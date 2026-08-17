package org.chemie_lernen_org.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class QuickAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTopics: () -> Unit,
    onOpenCalculators: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenUrl: (String, String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🧪 Chemie Lernen",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Interaktive Chemie — kostenlos & ohne Tracking",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(24.dp))

        // Quick actions grid
        val actions = listOf(
            QuickAction(Icons.Default.MenuBook, "Themenbereiche", "12 Themen der Chemie") { onOpenTopics() },
            QuickAction(Icons.Default.Calculate, "Rechner", "Molare Masse, pH, Stöchiometrie…") { onOpenCalculators() },
            QuickAction(Icons.Default.VideoLibrary, "Lernvideos", "Zig\u2019s Chemistry 42") { onOpenVideos() },
            QuickAction(Icons.Default.School, "Wissensnetz", "Wissensgraph der Chemie") {
                onOpenUrl("https://chemie-lernen.org/wissensnetz/", "Wissensnetz")
            },
        )

        LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(actions) { action ->
                Card(
                    onClick = action.onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(12.dp))
                        Icon(action.icon, contentDescription = null, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = action.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = action.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Attribution
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🧬 Über diese App",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Dies ist eine inoffizielle App für chemie-lernen.org, " +
                            "die kostenlose Lernplattform für Chemie. Alle Inhalte " +
                            "stammen von Tobias Weiß und dem Team hinter chemie-lernen.org. " +
                            "Die App bietet eine mobile-optimierte Ansicht und " +
                            "keine Werbung, kein Tracking.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
