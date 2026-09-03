package ai.chemistry_learning_org.app.ui.topics

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.rotate
import ai.chemistry_learning_org.R
import ai.chemistry_learning_org.app.web.SiteUrls

internal data class Topic(@StringRes val titleRes: Int, val url: String, val icon: String)

internal val topics = listOf(
    Topic(R.string.topic_introduction, "/themenbereiche/einfuehrung-chemie/", "\u2728"),
    Topic(R.string.topic_matter, "/themenbereiche/aufbau-materie/", "\u2697\ufe0f"),
    Topic(R.string.topic_inorganic, "/themenbereiche/anorganische-verbindungen/", "\u269b\ufe0f"),
    Topic(R.string.topic_energetics, "/themenbereiche/energetik/", "\u23f0\ufe0f"),
    Topic(R.string.topic_redox, "/themenbereiche/redox-elektrochemie/", "\u26a1"),
    Topic(R.string.topic_acids, "/themenbereiche/saeuren-basen/", "\u2697\ufe0f"),
    Topic(R.string.topic_equilibrium, "/themenbereiche/gleichgewicht-geschwindigkeit/", "\u2697\ufe0f"),
    Topic(R.string.topic_analytical, "/themenbereiche/analytische-methoden/", "\u269b\ufe0f"),
    Topic(R.string.topic_fuels, "/themenbereiche/erdoel-organische-stoffklassen/", "\u269b\ufe0f"),
    Topic(R.string.topic_products, "/themenbereiche/produkte-organisch/", "\u269b\ufe0f"),
    Topic(R.string.topic_biochemistry, "/themenbereiche/biochemie/", "\u269b\ufe0f"),
    Topic(R.string.topic_reactions, "/themenbereiche/reaktionstypen-organisch/", "\u269b\ufe0f"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(
    onBack: () -> Unit,
    onOpenUrl: (String, String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.topics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(topics) { topic ->
                val title = stringResource(topic.titleRes)
                Card(
                    onClick = { onOpenUrl(SiteUrls.resolve(topic.url), title) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = topic.icon, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.open),
                            modifier = Modifier.rotate(180f),
                        )
                    }
                }
            }
        }
    }
}
