package dev.everly.portfolio.rag;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public final class DegreeAllowlist {

	private static final Pattern LINE = Pattern
			.compile("^(?<program>.+?)\\s+—\\s+(?<inst>.+?)(?:\\s*\\((?<status>[^)]+)\\))?$");

	private final List<DegreeEntry> entries;

	public DegreeAllowlist(InstructionBundle instructionBundle) {
		Objects.requireNonNull(instructionBundle, "instructionBundle");

		String raw = instructionBundle.text(InstructionBundle.InstructionKey.DEGREES);

		this.entries = raw.lines().map(String::trim).filter(s -> !s.isBlank()).filter(s -> !s.startsWith("#"))
				.map(DegreeAllowlist::parseLine).toList();
	}

	private static DegreeEntry parseLine(String line) {
		Matcher m = LINE.matcher(line);
		if (!m.matches()) {
			throw new IllegalStateException("degrees.md line invalid: " + line);
		}
		String program = m.group("program").trim();
		String institution = m.group("inst").trim();
		String status = m.group("status") == null ? "" : m.group("status").trim();
		return new DegreeEntry(program, institution, status);
	}

	public List<DegreeEntry> entries() {
		return entries;
	}

	public String formattedProgramsOnly() {
		return entries.stream().map(e -> "- " + e.program() + " — " + e.institution()).reduce("",
				(a, b) -> a + (a.isEmpty() ? "" : "\n") + b);
	}

	public String formattedWithYearsOrStatus() {
		return entries.stream().map(e -> {
			String suffix = e.status().isBlank() ? "" : " (" + e.status() + ")";
			return "- " + e.program() + " — " + e.institution() + suffix;
		}).reduce("", (a, b) -> a + (a.isEmpty() ? "" : "\n") + b);
	}

	public record DegreeEntry(String program, String institution, String status) {
	}
}
