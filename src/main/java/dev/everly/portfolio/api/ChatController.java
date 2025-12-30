package dev.everly.portfolio.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.everly.portfolio.agent.PortfolioChatbotAgent;
import dev.everly.portfolio.model.ChatRequest;
import dev.everly.portfolio.model.ChatResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

	private final PortfolioChatbotAgent agent;

	public ChatController(PortfolioChatbotAgent agent) {
		this.agent = agent;
	}

	@PostMapping
	public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
		return agent.execute(request);
	}
}
