package dev.everly.portfolio.rag;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public final class EmployerAllowlist {

	private final Set<String> employers;

	public EmployerAllowlist(InstructionBundle instructionBundle) {
		Objects.requireNonNull(instructionBundle, "instructionBundle");

		String raw = instructionBundle.text(InstructionBundle.InstructionKey.EMPLOYERS);
		this.employers = Set.copyOf(parseEmployers(raw));
	}

	private static Set<String> parseEmployers(String raw) {
		Set<String> out = new LinkedHashSet<>();
		if (raw == null || raw.isBlank()) {
			return out;
		}

		for (String line : raw.split("\\R")) {
			String trimmed = line.trim();
			if (!trimmed.startsWith("- ")) {
				continue;
			}

			String afterDash = trimmed.substring(2).trim();
			String employer = afterDash.split("—")[0].trim();

			if (!employer.isBlank()) {
				out.add(employer);
			}
		}

		return out;
	}

	public Set<String> employers() {
		return employers;
	}

	public boolean isAllowlisted(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		String s = text.toLowerCase(Locale.ROOT);
		return employers.stream().anyMatch(e -> s.contains(e.toLowerCase(Locale.ROOT)));
	}
}
