package dev.everly.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import dev.everly.portfolio.rag.InstructionTextLoader;

@Configuration
public class RagConfig {

    @Bean
    public InstructionTextLoader instructionTextLoader(ResourceLoader resourceLoader) {
        return new InstructionTextLoader(resourceLoader);
    }
}
