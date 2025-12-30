package dev.everly.portfolio.guard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import dev.everly.portfolio.model.ChatResponse;

@Component
public final class GuardPipeline {
	private final List<GuardRule> rulesInPriorityOrder;
	private static final Logger log = LoggerFactory.getLogger(GuardPipeline.class);

	public GuardPipeline(List<GuardRule> rulesInPriorityOrder) {
		List<GuardRule> sorted = new ArrayList<>(Objects.requireNonNull(rulesInPriorityOrder));
		AnnotationAwareOrderComparator.sort(sorted);
		this.rulesInPriorityOrder = List.copyOf(sorted);
		log.info("Initialized GuardPipeline with {} rules.", this.rulesInPriorityOrder.size());
	}

	public ChatResponse evaluatePreOrNull(GuardContext context) {
		for (GuardRule rule : rulesInPriorityOrder) {
			if (!rule.supports(GuardStage.PRE_LLM)) {
				continue;
			}
			GuardAction action = rule.applyPre(context);
			if (action instanceof GuardAction.Block block) {
				return block.response();
			}
		}
		return null;
	}

	public String sanitizePost(GuardContext context, String modelOutput) {
		String sanitizedOutput = modelOutput == null ? "" : modelOutput;

		for (GuardRule rule : rulesInPriorityOrder) {
			if (!rule.supports(GuardStage.POST_LLM)) {
				continue;
			}
			GuardAction action = rule.applyPost(context, sanitizedOutput);
			if (action instanceof GuardAction.Sanitize sanitize) {
				sanitizedOutput = sanitize.sanitizedOutput();
			}
		}
		return sanitizedOutput;
	}
}
