package dev.everly.portfolio.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.everly.portfolio.guard.GuardContext;
import dev.everly.portfolio.guard.GuardPipeline;
import dev.everly.portfolio.model.ChatRequest;
import dev.everly.portfolio.model.ChatResponse;
import dev.everly.portfolio.rag.InstructionBundle;
import dev.everly.portfolio.rag.Retriever;
import dev.everly.synapsys.agents.GeminiAgent;
import dev.everly.synapsys.api.*;

@Service
public class PortfolioChatbotAgent implements Agent<ChatRequest, ChatResponse> {

	private static final Logger log = LoggerFactory.getLogger(PortfolioChatbotAgent.class);
	private final Retriever retriever;
	private final ObjectMapper objectMapper;
	private final InstructionBundle instructions;
	private final GuardPipeline guardPipeline;
	private volatile GeminiAgent<ChatRequest, String, ChatResponse> geminiAgent;

	public PortfolioChatbotAgent(Retriever retriever, ObjectMapper objectMapper, InstructionBundle instructions,
			GuardPipeline guardPipeline) {
		this.retriever = Objects.requireNonNull(retriever, "retriever");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
		this.instructions = Objects.requireNonNull(instructions, "instructions");
		this.guardPipeline = Objects.requireNonNull(guardPipeline, "guardPipeline");
	}

	@Override
	public ChatResponse execute(ChatRequest request) {
		GuardContext guardContext = GuardContext.from(request);

		ChatResponse blocked = guardPipeline.evaluatePreOrNull(guardContext);
		if (blocked != null) {
			return blocked;
		}

		GeminiAgent<ChatRequest, String, ChatResponse> agentInstance = geminiAgent;
		if (agentInstance == null) {
			synchronized (this) {
				agentInstance = geminiAgent;
				if (agentInstance == null) {
					log.info("Initializing GeminiAgent lazily - SynapSys started.");
					geminiAgent = agentInstance = buildGeminiAgent();
				}
			}
		}

		ChatResponse rawResponse = agentInstance.execute(request);
		String sanitized = guardPipeline.sanitizePost(guardContext, rawResponse.getResponse());
		return new ChatResponse(sanitized);
	}

	private GeminiAgent<ChatRequest, String, ChatResponse> buildGeminiAgent() {

		Prompt<ChatRequest, List<MessageModel>> ragPromptGenerator = input -> {
			log.debug("Generating RAG prompt for query: '{}'", input.getUserQuery());

			List<String> retrievedContexts = retriever.retrieve(input.getUserQuery(), 3);

			StringBuilder retrievedContextSection = new StringBuilder();
			if (!retrievedContexts.isEmpty()) {
				retrievedContextSection.append("\n\nRelevant background (use only if helpful):\n\n");
				for (int i = 0; i < retrievedContexts.size(); i++) {
					retrievedContextSection.append("Context ").append(i + 1).append(":\n")
							.append(retrievedContexts.get(i)).append("\n\n---\n\n");
				}
			}

			String combinedSystemText = instructions.text(InstructionBundle.InstructionKey.VOICE) + "\n\n"
					+ instructions.text(InstructionBundle.InstructionKey.GUARDS) + "\n\n"
					+ instructions.text(InstructionBundle.InstructionKey.FACTS) + "\n\n"
					+ instructions.text(InstructionBundle.InstructionKey.EMPLOYERS) + "\n\n"
					+ instructions.text(InstructionBundle.InstructionKey.PROJECTS) + "\n\n"
					+ instructions.text(InstructionBundle.InstructionKey.DEGREES) + "\n\n"
					+ instructions.text(InstructionBundle.InstructionKey.CERTIFICATIONS) + "\n\n"
					+ instructions.text(InstructionBundle.InstructionKey.CONTACT) + retrievedContextSection;

			List<MessageModel> messages = new ArrayList<>();
			messages.add(new MessageModel("system", combinedSystemText));
			messages.add(new MessageModel("user", input.getUserQuery()));
			return messages;
		};

		Parser<String, String> plainTextParser = raw -> raw == null ? "" : raw.trim();

		Validator<String, ValidationResult> nonEmptyValidator = parsed -> {
			if (parsed == null || parsed.isBlank()) {
				return ValidationResult.fail("LLM response cannot be empty.");
			}
			return ValidationResult.success();
		};

		PostProcessor<String, ChatResponse> responsePostProcessor = parsed -> new ChatResponse(parsed);

		return new GeminiAgent<ChatRequest, String, ChatResponse>().withLlmClientId("gemini-default")
				.withPromptGenerator(ragPromptGenerator).withParser(plainTextParser).withValidator(nonEmptyValidator)
				.withPostProcessor(responsePostProcessor).withJsonMode(false).withTemperature(0.7).withMaxTokens(200)
				.withMaxParsingAttempts(1).withMaxValidationAttempts(1).withObjectMapper(objectMapper).build();
	}
}
