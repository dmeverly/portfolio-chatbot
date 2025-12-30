package dev.everly.portfolio.rag;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

public final class InstructionTextLoader {

	private final ResourceLoader resourceLoader;

	public InstructionTextLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader");
	}

	public Loaded loadOrThrow(String primaryLocation, String fallbackLocation, String logicalName) {
		Loaded loaded = load(primaryLocation);
		if (loaded != null && !loaded.text().isBlank()) {
			return loaded.trimmed();
		}

		loaded = load(fallbackLocation);
		if (loaded != null && !loaded.text().isBlank()) {
			return loaded.trimmed();
		}

		throw new IllegalStateException("Failed to load " + logicalName + " from primary [" + primaryLocation
				+ "] or fallback [" + fallbackLocation + "]");
	}

	private Loaded load(String location) {
		if (location == null || location.isBlank()) {
			return null;
		}

		try {
			Resource r = resourceLoader.getResource(location);
			if (!r.exists()) {
				return null;
			}
			byte[] bytes = StreamUtils.copyToByteArray(r.getInputStream());
			String text = new String(bytes, StandardCharsets.UTF_8);
			return new Loaded(location, text);
		} catch (Exception e) {
			return null;
		}
	}

	public record Loaded(String sourceLocation, String text) {
		public Loaded trimmed() {
			return new Loaded(sourceLocation, text == null ? "" : text.trim());
		}
	}
}
