package dev.everly.portfolio.guard.rules;

import java.util.Locale;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import dev.everly.portfolio.guard.GuardAction;
import dev.everly.portfolio.guard.GuardContext;
import dev.everly.portfolio.guard.GuardRule;
import dev.everly.portfolio.guard.GuardStage;
import dev.everly.portfolio.model.ChatResponse;
import dev.everly.portfolio.rag.ContactAllowlist;

@Component
@Order(30)
public final class ContactRequestGuardRule implements GuardRule {

	private final ContactAllowlist allowlist;

	public ContactRequestGuardRule(ContactAllowlist allowlist) {
		this.allowlist = allowlist;
	}

	@Override
	public String ruleName() {
		return "contact-request";
	}

	@Override
	public boolean supports(GuardStage stage) {
		return stage == GuardStage.PRE_LLM;
	}

	@Override
	public GuardAction applyPre(GuardContext context) {
		String q = context.userQuery();
		if (q == null || q.isBlank()) {
			return GuardAction.pass();
		}

		String s = q.toLowerCase(Locale.ROOT);

		boolean isAskingForYourContact = s.contains("your email") || s.contains("email address") || s.contains("email")
				|| s.contains("your github") || s.contains("github") || s.contains("your linkedin")
				|| s.contains("linkedin") || s.contains("your portfolio") || s.contains("portfolio website")
				|| s.contains("your website") || s.contains("website for you") || s.contains("contact")
				|| s.contains("reach you") || s.contains("how can i contact");

		if (!isAskingForYourContact) {
			return GuardAction.pass();
		}

		if (s.contains("email")) {
			return GuardAction.block(new ChatResponse(allowlist.email()));
		}
		if (s.contains("github")) {
			return GuardAction.block(new ChatResponse(allowlist.github()));
		}
		if (s.contains("linkedin")) {
			return GuardAction.block(new ChatResponse(allowlist.linkedin()));
		}
		if (s.contains("portfolio") || s.contains("website")) {
			return GuardAction.block(new ChatResponse(allowlist.primaryPortfolio()));
		}

		return GuardAction.pass();
	}

	@Override
	public GuardAction applyPost(GuardContext context, String modelOutput) {
		return GuardAction.pass();
	}
}
