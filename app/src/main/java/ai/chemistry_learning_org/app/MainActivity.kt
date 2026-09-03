package ai.chemistry_learning_org.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ai.chemistry_learning_org.app.ui.theme.ChemieLernenTheme
import ai.chemistry_learning_org.app.ui.navigation.ChemieNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChemieLernenTheme {
                ChemieNavHost()
            }
        }
    }
}
