package org.chemie_lernen_org.app.web

/**
 * Central policy for which hosts the in-app WebView may load.
 *
 * Kept pure (no Android dependencies) so it is unit-testable on the JVM.
 *
 * Security notes:
 *  - Only [ALLOWED_HOST] and its subdomains are allowed.
 *  - A suffix match guards against spoofed hosts like `chemie-lernen.org.evil.com`.
 *  - Matching is case-insensitive (hostnames are case-insensitive by spec).
 */
object WebUrlPolicy {

    const val ALLOWED_HOST = "chemie-lernen.org"

    /** True if [host] is the allowed domain or one of its subdomains. */
    fun isAllowedHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val normalized = host.trim().lowercase()
        return normalized == ALLOWED_HOST ||
            normalized.endsWith(".$ALLOWED_HOST", ignoreCase = true)
    }
}
