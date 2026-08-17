package org.chemie_lernen_org.app.ui.topics

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val themenbereiche = listOf(
    "\u2728" to "Einf\u00fchrung in die Chemie" to "/themenbereiche/einfuehrung-chemie/",
    "\u2697\ufe0f" to "Aufbau der Materie" to "/themenbereiche/aufbau-materie/",
    "\u269b\ufe0f" to "Anorganische Verbindungen" to "/themenbereiche/anorganische-verbindungen/",
    "\u23f0\ufe0f" to "Energetik" to "/themenbereiche/energetik/",
    "\u26a1" to "Elektrochemie & Redox" to "/themenbereiche/redox-elektrochemie/",
    "\u2697\ufe0f" to "S\u00e4ure & Basen" to "/themenbereiche/saeuren-basen/",
    "\u2697\ufe0f" to "Gleichgewicht & Kinetik" to "/themenbereiche/gleichgewicht-geschwindigkeit/",
    "\u269b\ufe0f" to "Analytische Methoden" to "/themenbereiche/analytische-methoden/",
    "\u269b\ufe0f" to "Erdoel & Organik" to "/themenbereiche/erdoel-organische-stoffklassen/",
    "\u269b\ufe0f" to "Produkte Organisch" to "/themenbereiche/produkte-organisch/",
    "\u269b\ufe0f" to "Biochemie" to "/themenbereiche/biochemie/",
    "\u269b\ufe0f" to "Reaktionstypen Organik" to "/themenbereiche/reaktionstypen-organisch/",
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
                title = { Text("Themenbereiche") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zur\u00fcck")
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
            items(themenbereiche) { (triple) ->
                val (icon, title, url) = triple
                Card(
                    onClick = { onOpenUrl("https://chemie-lernen.org$url", title) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = icon, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
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
                            contentDescription = "\u00d6ffnen",
                            modifier = Modifier.rotate(180f),
                        )
                    }
                }
            }
        }
    }
}
