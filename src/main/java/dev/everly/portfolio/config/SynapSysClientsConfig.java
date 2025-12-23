package dev.everly.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.everly.synapsys.llm.gemini.GeminiClient;

@Configuration
public class SynapSysClientsConfig {

	@Bean
	public GeminiClient geminiDefault(ObjectMapper objectMapper) {
		return new GeminiClient("gemini-default", "Google Gemini Pro Client", objectMapper);
	}
}
