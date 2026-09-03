package ai.chemistry_learning_org.app.ui.calculators

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

internal data class Calculator(@StringRes val titleRes: Int, val url: String, val icon: String)

internal val calculators = listOf(
    Calculator(R.string.calc_molar_mass, "/molar-masse-rechner/", "\u2697"),
    Calculator(R.string.calc_ph, "/ph-rechner/", "\u26a1"),
    Calculator(R.string.calc_stoichiometry, "/reaktionsgleichungen-ausgleichen/", "\u2697"),
    Calculator(R.string.calc_gas_laws, "/gasgesetz-rechner/", "\u2702"),
    Calculator(R.string.calc_concentration, "/konzentration-rechner/", "\u2697"),
    Calculator(R.string.calc_density, "/dichte-rechner/", "\u2696"),
    Calculator(R.string.calc_dilution, "/verduennungsrechner/", "\ud83d\udca1"),
    Calculator(R.string.calc_solubility, "/loeslichkeitsprodukt-rechner/", "\ud83d\udcdd"),
    Calculator(R.string.calc_combustion, "/verbrennungsrechner/", "\u2697"),
    Calculator(R.string.calc_redox, "/redox-potenzial-rechner/", "\u26a1"),
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
                title = { Text(stringResource(R.string.calculators_title)) },
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
            items(calculators) { calculator ->
                val title = stringResource(calculator.titleRes)
                Card(
                    onClick = { onOpenUrl("https://chemie-lernen.org${calculator.url}", title) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = calculator.icon, fontSize = MaterialTheme.typography.headlineMedium.fontSize)
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
