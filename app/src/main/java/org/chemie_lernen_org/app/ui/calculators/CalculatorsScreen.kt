package org.chemie_lernen_org.app.ui.calculators

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
import androidx.compose.ui.draw.rotate

private val calculators = listOf(
    "\u2697" to "Molare Masse" to "/molar-masse-rechner/",
    "\u26a1" to "pH-Rechner" to "/ph-rechner/",
    "\u2697" to "St\u00f6chiometrie" to "/reaktionsgleichungen-ausgleichen/",
    "\u2702" to "Gasgesetze" to "/gasgesetz-rechner/",
    "\u2697" to "Konzentration" to "/konzentration-rechner/",
    "\u2696" to "Dichte" to "/dichte-rechner/",
    "\ud83d\udca1" to "Verd\u00fcnnt" to "/verduennungsrechner/",
    "\ud83d\udcdd" to "L\u00f6slichkeit" to "/loeslichkeitsprodukt-rechner/",
    "\u2697" to "Verbrennung" to "/verbrennungsrechner/",
    "\u26a1" to "Redox-Potenzial" to "/redox-potenzial-rechner/",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsScreen(
    onBack: () -> Unit,
    onOpenUrl: (String, String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rechner") },
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
            items(calculators) { (icon, inner) ->
                val (title, url) = inner
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
