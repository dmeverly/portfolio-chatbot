package dev.everly.portfolio.guard;

import dev.everly.portfolio.model.ChatRequest;

public final class GuardContext {

	private final String userInput;

	private GuardContext(String userInput) {
		this.userInput = userInput == null ? "" : userInput;
	}

	public static GuardContext from(ChatRequest request) {
		return new GuardContext(request.getUserQuery());
	}

	public String userQuery() {
		return userInput;
	}
}
