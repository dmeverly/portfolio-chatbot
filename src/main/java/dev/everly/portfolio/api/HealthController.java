package dev.everly.portfolio.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class HealthController {

	@GetMapping("/health")
	public String health() {
		return "ok";
	}
}
