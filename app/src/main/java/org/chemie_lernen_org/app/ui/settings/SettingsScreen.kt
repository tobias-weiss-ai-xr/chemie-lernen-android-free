package org.chemie_lernen_org.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.fontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenUrl: (String, String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mehr") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zur\u00fcck")
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "\u2139\uFE0F Info",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall)
                    Text("App-ID: org.chemie_lernen_org.free", style = MaterialTheme.typography.bodySmall)
                    Text("Lizenz: Apache-2.0", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Die Inhalte stammen von chemie-lernen.org (Tobias Wei\u00df). " +
                            "Die App ist ein Open-Source-Wrapper und bietet " +
                            "keine eigenen Inhalte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "\uD83D\uDD17 Links",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingLink("Website", "https://chemie-lernen.org") {
                        onOpenUrl("https://chemie-lernen.org", "Chemie Lernen")
                    }
                    SettingLink("Impressum", "https://chemie-lernen.org/impressum/") {
                        onOpenUrl("https://chemie-lernen.org/impressum/", "Impressum")
                    }
                    SettingLink("Datenschutz", "https://chemie-lernen.org/datenschutz/") {
                        onOpenUrl("https://chemie-lernen.org/datenschutz/", "Datenschutz")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingLink(
    label: String,
    url: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.align(Alignment.Start),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
