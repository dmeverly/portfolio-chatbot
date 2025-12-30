package dev.everly.portfolio.rag;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public final class ProjectAllowlist {

	private final List<String> projects;

	public ProjectAllowlist(InstructionBundle instructionBundle) {
		Objects.requireNonNull(instructionBundle, "instructionBundle");

		String raw = instructionBundle.text(InstructionBundle.InstructionKey.PROJECTS);

		this.projects = raw.lines().map(String::trim).filter(s -> !s.isBlank()).filter(s -> !s.startsWith("#"))
				.toList();
	}

	public List<String> projects() {
		return projects;
	}

	public boolean isAllowlisted(String name) {
		if (name == null || name.isBlank()) {
			return false;
		}
		return projects.stream().anyMatch(p -> p.equalsIgnoreCase(name.trim()));
	}

	public String formattedList() {
		return String.join("\n", projects.stream().map(p -> "- " + p).toList());
	}

	public boolean mentionsAnyAllowlistedProject(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		String lower = text.toLowerCase(java.util.Locale.ROOT);
		return projects.stream().anyMatch(p -> lower.contains(p.toLowerCase(java.util.Locale.ROOT)));
	}

}
