package dev.everly.portfolio.rag;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public final class CertificationAllowlist {

	private final List<String> certifications;

	public CertificationAllowlist(InstructionBundle instructionBundle) {
		Objects.requireNonNull(instructionBundle, "instructionBundle");

		String raw = instructionBundle.text(InstructionBundle.InstructionKey.CERTIFICATIONS);

		this.certifications = raw.lines().map(String::trim).filter(s -> !s.isBlank()).filter(s -> !s.startsWith("#"))
				.toList();
	}

	public List<String> certifications() {
		return certifications;
	}

	public String formattedList() {
		return String.join("\n", certifications.stream().map(c -> "- " + c).toList());
	}
}
