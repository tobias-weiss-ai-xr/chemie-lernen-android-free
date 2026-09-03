package ai.chemistry_learning_org.app.web

/**
 * Central builder for chemie-lernen.org URLs.
 *
 * Single source of truth for every platform link the app opens, so the
 * host can never drift between screens and every produced URL is
 * guaranteed to pass [WebUrlPolicy].
 */
object SiteUrls {

    const val BASE = "https://${WebUrlPolicy.ALLOWED_HOST}"

    /** Appends [path] to [BASE] tolerating missing or duplicated slashes. */
    fun resolve(path: String): String {
        val normalized = path.trim().trimStart('/')
        if (normalized.isEmpty()) return BASE
        return "$BASE/$normalized"
    }

    fun home(): String = BASE
    fun videos(): String = resolve("/lernvideos/")
    fun knowledgeGraph(): String = resolve("/wissensnetz/")
    fun privacy(): String = resolve("/datenschutz/")
    fun imprint(): String = resolve("/impressum/")
}
