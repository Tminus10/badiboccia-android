package swiss.reutener.boccia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import swiss.reutener.boccia.app.AppModel
import swiss.reutener.boccia.app.BocciaApp
import swiss.reutener.boccia.app.LocalAppModel
import swiss.reutener.boccia.ui.theme.BocciaTheme

class MainActivity : ComponentActivity() {
    private val model: AppModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark only, like the iOS app: light status bar icons on the blue gradient.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            BocciaTheme {
                CompositionLocalProvider(LocalAppModel provides model) {
                    BocciaApp()
                }
            }
        }
    }
}
