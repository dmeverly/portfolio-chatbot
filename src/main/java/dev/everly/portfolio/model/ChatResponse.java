package dev.everly.portfolio.model;

public record ChatResponse(String response) {

	public String getResponse() {
		return response;
	}
}
