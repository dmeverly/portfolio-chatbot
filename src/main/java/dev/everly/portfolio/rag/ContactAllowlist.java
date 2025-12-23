package dev.everly.portfolio.rag;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public final class ContactAllowlist {

	private final Map<String, String> entries;

	public ContactAllowlist(InstructionBundle instructionBundle) {
		Objects.requireNonNull(instructionBundle, "instructionBundle");

		String raw = instructionBundle.text(InstructionBundle.InstructionKey.CONTACT);
		this.entries = Map.copyOf(parse(raw));

		requireKey("email");
		requireKey("github");
		requireKey("linkedin");
		requireKey("primary portfolio");
	}

	private static Map<String, String> parse(String raw) {
		Map<String, String> map = new LinkedHashMap<>();
		if (raw == null || raw.isBlank()) {
			return map;
		}

		for (String line : raw.split("\\R")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}

			int colon = trimmed.indexOf(':');
			if (colon <= 0) {
				continue;
			}

			String key = trimmed.substring(0, colon).trim().toLowerCase(Locale.ROOT);
			String value = trimmed.substring(colon + 1).trim();

			if (!key.isEmpty() && !value.isEmpty()) {
				map.put(key, value);
			}
		}

		return map;
	}

	private void requireKey(String normalizedKey) {
		String v = entries.get(normalizedKey);
		if (v == null || v.isBlank()) {
			throw new IllegalStateException("contact.md missing required key: " + normalizedKey + " (available keys: "
					+ entries.keySet() + ")");
		}
	}

	public Map<String, String> entries() {
		return entries;
	}

	public String primaryPortfolio() {
		return entries.getOrDefault("primary portfolio", "");
	}

	public String alternatePortfolio() {
		return entries.getOrDefault("alternate portfolio", "");
	}

	public String github() {
		return entries.getOrDefault("github", "");
	}

	public String linkedin() {
		return entries.getOrDefault("linkedin", "");
	}

	public String email() {
		return entries.getOrDefault("email", "");
	}
}
