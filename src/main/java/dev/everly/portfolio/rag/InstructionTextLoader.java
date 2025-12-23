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

	public String loadOrThrow(String primaryLocation, String fallbackLocation, String logicalName) {
		String text = loadOrNull(primaryLocation);
		if (text != null && !text.isBlank()) {
			return text.trim();
		}

		text = loadOrNull(fallbackLocation);
		if (text != null && !text.isBlank()) {
			return text.trim();
		}

		throw new IllegalStateException("Failed to load " + logicalName + " from primary [" + primaryLocation
				+ "] or fallback [" + fallbackLocation + "]");
	}

	private String loadOrNull(String location) {
		if (location == null || location.isBlank()) {
			return null;
		}

		try {
			Resource r = resourceLoader.getResource(location);
			if (!r.exists()) {
				return null;
			}
			byte[] bytes = StreamUtils.copyToByteArray(r.getInputStream());
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (Exception e) {
			return null;
		}
	}
}
