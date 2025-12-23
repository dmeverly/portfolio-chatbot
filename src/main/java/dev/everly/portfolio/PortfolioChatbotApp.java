package dev.everly.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

@SpringBootApplication
public class PortfolioChatbotApp {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		for (DotenvEntry e : dotenv.entries()) {
			if (System.getProperty(e.getKey()) == null) {
				System.setProperty(e.getKey(), e.getValue());
			}
		}

		SpringApplication.run(PortfolioChatbotApp.class, args);
	}
}
