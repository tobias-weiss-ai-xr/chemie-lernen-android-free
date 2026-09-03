package ai.chemistry_learning_org.app.kg

/**
 * Immutable models for the knowledge graph (Wissensnetz), served by the
 * platform kg-data API (Neo4j backend).
 */
data class KgRelation(
    val name: String,
    val relType: String,
    val category: String?,
)

data class KgEntity(
    val name: String,
    val category: String?,
    val description: String?,
    val relationCount: Int,
    val related: List<KgRelation>,
)

data class KgArticle(
    val title: String,
    val url: String,
    val entities: List<String>,
)

data class KgPage(
    val entities: List<KgEntity>,
    val articles: List<KgArticle>,
    val total: Int,
    val offset: Int,
    val limit: Int,
)

data class KgSnapshot(
    val entities: List<KgEntity>,
    val articles: List<KgArticle>,
) {
    companion object {
        fun fromPages(pages: List<KgPage>): KgSnapshot = KgSnapshot(
            entities = pages.flatMap { it.entities },
            articles = pages.flatMap { it.articles },
        )
    }
}
