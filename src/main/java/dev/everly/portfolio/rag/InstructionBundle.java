package dev.everly.portfolio.rag;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public final class InstructionBundle {

	private final String privateDir;
	private final InstructionTextLoader loader;
	private Map<InstructionKey, String> texts;

	public InstructionBundle(@Value("${portfolio.privateDir:}") String privateDir, ResourceLoader resourceLoader) {
		this.privateDir = privateDir == null ? "" : privateDir.trim();
		this.loader = new InstructionTextLoader(resourceLoader);
	}

	@PostConstruct
	void loadAll() {
		this.texts = Map.ofEntries(entry(InstructionKey.VOICE), entry(InstructionKey.GUARDS),
				entry(InstructionKey.FACTS), entry(InstructionKey.EMPLOYERS), entry(InstructionKey.PROJECTS),
				entry(InstructionKey.DEGREES), entry(InstructionKey.CERTIFICATIONS), entry(InstructionKey.CONTACT));
	}

	private Map.Entry<InstructionKey, String> entry(InstructionKey key) {
		String primary = privateDir.isBlank() ? "" : "file:" + privateDir + "/instructions/" + key.privateFileName();

		String fallback = "classpath:" + key.publicClasspathFallback();

		String text = loader.loadOrThrow(primary, fallback, key.name().toLowerCase());
		System.out.println("Loading " + key + " primary=" + primary + " fallback=" + fallback);

		return Map.entry(key, text);
	}

	public String text(InstructionKey key) {
		Objects.requireNonNull(key, "key");
		return texts.getOrDefault(key, "");
	}

	public enum InstructionKey {
		VOICE("voice.md", "voice.public.md"), GUARDS("guards.md", "guards.public.md"),
		FACTS("facts.md", "facts.public.md"), EMPLOYERS("employers.md", "employers.public.md"),
		PROJECTS("projects.md", "projects.public.md"), DEGREES("degrees.md", "degrees.public.md"),
		CERTIFICATIONS("certifications.md", "certifications.public.md"), CONTACT("contact.md", "contact.public.md");

		private final String privateFileName;
		private final String publicClasspathFallback;

		InstructionKey(String privateFileName, String publicClasspathFallback) {
			this.privateFileName = privateFileName;
			this.publicClasspathFallback = publicClasspathFallback;
		}

		public String privateFileName() {
			return privateFileName;
		}

		public String publicClasspathFallback() {
			return publicClasspathFallback;
		}
	}
}
