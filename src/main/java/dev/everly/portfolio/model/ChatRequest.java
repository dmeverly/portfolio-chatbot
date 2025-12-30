package dev.everly.portfolio.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(String userQuery) {
	@NotBlank @Size(max = 4000)
	public String getUserQuery() {
		return userQuery;
	}
}
