package dev.everly.portfolio.rag;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class InMemoryRetriever implements Retriever {
	private static final Logger log = LoggerFactory.getLogger(InMemoryRetriever.class);
	private final InstructionBundle instructionBundle;
	private List<String> portfolioDocuments;

	public InMemoryRetriever(InstructionBundle instructionBundle) {
		this.instructionBundle = instructionBundle;
	}

	private static String loadResource(String name) {
		try {
			return new String(new ClassPathResource(name).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load " + name, e);
		}
	}

	@PostConstruct
	public void loadPortfolioData() {
		String rawFacts = instructionBundle.text(InstructionBundle.InstructionKey.FACTS);

		this.portfolioDocuments = Arrays.stream(rawFacts.split("(?m)^---\\s*$")).map(String::trim)
				.filter(s -> !s.isBlank()).collect(Collectors.toList());

		log.info("Loaded {} fact chunks from facts.md", portfolioDocuments.size());
	}

	@Override
	public List<String> retrieve(String query, int topK) {
		log.debug("Retrieving context for query: '{}'", query);

		List<String> queryWords = Arrays.stream(query.toLowerCase().split("\\W+")).filter(word -> !word.isEmpty())
				.distinct().collect(Collectors.toList());

		if (queryWords.isEmpty()) {
			return List.of();
		}

		return portfolioDocuments.stream().filter(doc -> {
			String lower = doc.toLowerCase();
			return queryWords.stream().anyMatch(lower::contains);
		}).limit(topK).collect(Collectors.toList());
	}
}
