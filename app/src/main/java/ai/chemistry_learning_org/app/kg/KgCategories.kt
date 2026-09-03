package ai.chemistry_learning_org.app.kg

import ai.chemistry_learning_org.R

/**
 * Category metadata for the knowledge graph. Pure JVM: returns resource
 * IDs and hex colors; the composable layer maps them to strings/Color.
 *
 * Colors mirror the web legend (kg-legend.css) for brand consistency.
 */
object KgCategories {

    data class Meta(val labelRes: Int, val colorHex: Long)

    private val known = mapOf(
        "stoff" to Meta(R.string.cat_stoff, 0xFF667EEA),
        "konzept" to Meta(R.string.cat_konzept, 0xFF45B7D1),
        "reaktion" to Meta(R.string.cat_reaktion, 0xFF4ECDC4),
        "methode" to Meta(R.string.cat_methode, 0xFFF093FB),
        "person" to Meta(R.string.cat_person, 0xFFFF9A76),
        "didaktik" to Meta(R.string.cat_didaktik, 0xFFA8E6CF),
        "lehrplan" to Meta(R.string.cat_lehrplan, 0xFF9B59B6),
        "kmk" to Meta(R.string.cat_kmk, 0xFF2E7D32),
        "quelle" to Meta(R.string.cat_quelle, 0xFF8D99AE),
    )

    private val fallback = Meta(R.string.cat_other, 0xFF90A4AE)

    fun meta(category: String?): Meta =
        known[category?.trim()?.lowercase()] ?: fallback

    /** All category ids that appear in the API (for the filter chips). */
    val chipOrder: List<String> = listOf(
        "stoff", "konzept", "reaktion", "methode", "person", "quelle", "lehrplan", "kmk",
    )

    fun labelRes(category: String): Int = meta(category).labelRes

    fun colorHex(category: String?): Long = meta(category).colorHex

    /**
     * Pure view-model for the egocentric detail: the selected entity in the
     * center and its related entities sorted by their own relation count
     * (strongest connections first — we only know the count of the center,
     * so ties keep API order which is strength-sorted server-side).
     */
    data class Center(
        val entity: KgEntity,
        val related: List<KgRelation>,
        val articles: List<KgArticle>,
    )

    fun buildCenter(entity: KgEntity, allArticles: List<KgArticle>): Center {
        val centerName = entity.name.lowercase()
        val related = entity.related
            .sortedByDescending { it.name.length } // stable, deterministic order
        val articles = allArticles.filter { article ->
            article.entities.any { it.lowercase() == centerName }
        }
        return Center(entity, related, articles)
    }
}
