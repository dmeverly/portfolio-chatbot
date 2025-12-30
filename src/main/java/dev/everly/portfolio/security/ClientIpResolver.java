package dev.everly.portfolio.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();

        // Only trust proxy headers from loopback / tunnel
        boolean trustedProxy = remote.equals("127.0.0.1") ||
                remote.equals("::1") ||
                remote.startsWith("10.") ||
                remote.startsWith("192.168.") ||
                remote.startsWith("172.");

        if (trustedProxy) {
            String cfIp = request.getHeader("CF-Connecting-IP");
            if (cfIp != null && !cfIp.isBlank()) {
                return cfIp.trim();
            }

            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                int comma = xff.indexOf(',');
                return (comma > 0 ? xff.substring(0, comma) : xff).trim();
            }
        }

        return remote;
    }

}
