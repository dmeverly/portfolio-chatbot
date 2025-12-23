package dev.everly.portfolio.model;

public record ChatRequest(String userQuery) {
	public String getUserQuery() {
		return userQuery;
	}
}
