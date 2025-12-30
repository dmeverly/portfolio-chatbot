package dev.everly.portfolio.rag;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public final class InstructionBundle {

    private static final Logger log = LoggerFactory.getLogger(InstructionBundle.class);

    private final Map<InstructionKey, String> texts;

    public InstructionBundle(Environment env, InstructionTextLoader loader) {
        Objects.requireNonNull(env, "env");
        Objects.requireNonNull(loader, "loader");

        boolean isPrivateProfile = Arrays.asList(env.getActiveProfiles()).contains("private");

        if (!isPrivateProfile) {
            this.texts = loadPublicOnly(loader);
            return;
        }

        String privateDir = env.getProperty("PORTFOLIO_PRIVATE_DIR", "").trim();
        if (privateDir.isEmpty()) {
            throw new IllegalStateException(
                "PORTFOLIO_PRIVATE_DIR is required when running with Spring profile 'private'. " +
                "Set it via .env or OS environment."
            );
        }

        this.texts = loadPrivateWithFallback(loader, privateDir);
    }

    public String text(InstructionKey key) {
        Objects.requireNonNull(key, "key");
        return texts.getOrDefault(key, "");
    }

    private static Map<InstructionKey, String> loadPublicOnly(InstructionTextLoader loader) {
        Map<InstructionKey, String> out = new EnumMap<>(InstructionKey.class);

        for (InstructionKey k : InstructionKey.values()) {
            InstructionTextLoader.Loaded loaded = loader.loadOrThrow(
                k.publicClasspathLocation(),
                null,
                k.logicalName()
            );
            out.put(k, loaded.text());
            log.info("InstructionBundle: Loaded {}", loaded.sourceLocation());
        }

        return out;
    }

    private static Map<InstructionKey, String> loadPrivateWithFallback(
            InstructionTextLoader loader,
            String privateDirRaw
    ) {
        Map<InstructionKey, String> out = new EnumMap<>(InstructionKey.class);

        String privateBase = "file:" + normalizeDir(privateDirRaw) + "/instructions/";

        for (InstructionKey k : InstructionKey.values()) {
            String primary = privateBase + k.privateFilename();
            String fallback = k.publicClasspathLocation();

            InstructionTextLoader.Loaded loaded = loader.loadOrThrow(primary, fallback, k.logicalName());
            out.put(k, loaded.text());
            log.info("InstructionBundle: Loaded {}", loaded.sourceLocation());
        }

        log.info("InstructionBundle: private profile active; primary base={}", privateBase);
        return out;
    }

    private static String normalizeDir(String dir) {
        return dir.replace('\\', '/').replaceAll("/+$", "");
    }

    public enum InstructionKey {
        VOICE("voice", "voice.md", "classpath:voice.public.md"),
        GUARDS("guards", "guards.md", "classpath:guards.public.md"),
        FACTS("facts", "facts.md", "classpath:facts.public.md"),
        EMPLOYERS("employers", "employers.md", "classpath:employers.public.md"),
        PROJECTS("projects", "projects.md", "classpath:projects.public.md"),
        DEGREES("degrees", "degrees.md", "classpath:degrees.public.md"),
        CERTIFICATIONS("certifications", "certifications.md", "classpath:certifications.public.md"),
        CONTACT("contact", "contact.md", "classpath:contact.public.md");

        private final String logicalName;
        private final String privateFilename;
        private final String publicClasspathLocation;

        InstructionKey(String logicalName, String privateFilename, String publicClasspathLocation) {
            this.logicalName = logicalName;
            this.privateFilename = privateFilename;
            this.publicClasspathLocation = publicClasspathLocation;
        }

        public String logicalName() {
            return logicalName;
        }

        public String privateFilename() {
            return privateFilename;
        }

        public String publicClasspathLocation() {
            return publicClasspathLocation;
        }
    }
}
