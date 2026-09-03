package ai.chemistry_learning_org.app.web

/**
 * Pure decision core for WebViewClient.shouldOverrideUrlLoading — no
 * Android dependencies (scheme/host are parsed manually so the policy is
 * fully unit-testable on the JVM).
 *
 * Outcomes:
 *  - [Decision.ALLOW]          load inside the in-app WebView (https on the
 *                              whitelisted host only)
 *  - [Decision.OPEN_EXTERNAL]  hand to the OS via ACTION_VIEW (mailto:,
 *                              tel:, geo:, sms:, intent: — previously these
 *                              were silently swallowed)
 *  - [Decision.BLOCK]          ignore the navigation entirely (foreign
 *                              https hosts, insecure http, script/data/
 *                              file/about URIs, malformed input)
 *
 * Fails closed: anything unrecognized is BLOCK.
 */
object WebViewNavPolicy {

    enum class Decision { ALLOW, OPEN_EXTERNAL, BLOCK }

    private val externalSchemes = setOf("mailto", "tel", "geo", "sms", "intent")

    fun decide(uriString: String?): Decision {
        if (uriString.isNullOrBlank()) return Decision.BLOCK
        val trimmed = uriString.trim()
        val scheme = trimmed.substringBefore(':', "").lowercase()
        val afterScheme = trimmed.substring(scheme.length + 1)

        return when {
            scheme == "https" -> {
                if (WebUrlPolicy.isAllowedHost(extractHost(afterScheme))) Decision.ALLOW
                else Decision.BLOCK
            }
            scheme in externalSchemes -> Decision.OPEN_EXTERNAL
            else -> Decision.BLOCK
        }
    }

    /**
     * Extracts the host from the authority part (everything after
     * "scheme://", before the first '/', '?', or '#'), stripping any
     * userinfo ("user@host" -> "host"). Returns null if absent.
     */
    private fun extractHost(afterScheme: String): String? {
        val noLeadingSlashes = afterScheme.trimStart('/')
        // single-arg substringBefore returns `this` when the delimiter is
        // absent — safe to chain (a restoring `missingDelimiterValue` here
        // would resurrect stripped path/query characters and corrupt the host)
        val authority = noLeadingSlashes.substringBefore('/').substringBefore('?').substringBefore('#')
        val host = authority.substringAfterLast('@')
        return host.ifBlank { null }
    }
}
