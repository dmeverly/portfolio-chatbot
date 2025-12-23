package dev.everly.portfolio.guard;

public interface GuardRule {

	String ruleName();

	boolean supports(GuardStage stage);

	GuardAction applyPre(GuardContext context);

	GuardAction applyPost(GuardContext context, String modelOutput);
}
