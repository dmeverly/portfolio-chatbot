package dev.everly.portfolio.guard;

import dev.everly.portfolio.model.ChatResponse;

public sealed interface GuardAction permits GuardAction.Pass, GuardAction.Block, GuardAction.Sanitize {

	static GuardAction pass() {
		return new Pass();
	}

	static GuardAction block(ChatResponse response) {
		return new Block(response);
	}

	static GuardAction sanitize(String sanitizedOutput) {
		return new Sanitize(sanitizedOutput);
	}

	record Pass() implements GuardAction {
	}

	record Block(ChatResponse response) implements GuardAction {
	}

	record Sanitize(String sanitizedOutput) implements GuardAction {
	}
}
