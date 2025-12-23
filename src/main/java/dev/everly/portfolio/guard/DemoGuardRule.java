package dev.everly.portfolio.guard;

import org.springframework.stereotype.Component;

@Component
public final class DemoGuardRule implements GuardRule {
	@Override
	public String ruleName() {
		return "demo";
	}

	@Override
	public boolean supports(GuardStage stage) {
		return false;
	}

	@Override
	public GuardAction applyPre(GuardContext context) {
		return GuardAction.pass();
	}

	@Override
	public GuardAction applyPost(GuardContext context, String modelOutput) {
		return GuardAction.pass();
	}
}
