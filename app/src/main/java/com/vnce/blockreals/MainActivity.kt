package com.vnce.blockreals

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vnce.blockreals.ui.theme.StopReelsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StopReelsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StopReelsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun StopReelsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(ReelsAccessibilityService.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var debugMode by remember {
        mutableStateOf(prefs.getBoolean(ReelsAccessibilityService.KEY_DEBUG_MODE, false))
    }
    var serviceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Block Reels", style = MaterialTheme.typography.headlineMedium)

        Text(
            text = if (serviceEnabled) {
                "Accessibility service: ON — Reels will be blocked in Instagram."
            } else {
                "Accessibility service: OFF — turn it on below to start blocking Reels."
            }
        )

        Button(onClick = {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }) {
            Text("Open Accessibility Settings")
        }

        Button(onClick = { serviceEnabled = isAccessibilityServiceEnabled(context) }) {
            Text("Refresh status")
        }

//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween,
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Column {
//                Text("Debug mode")
//                Text(
//                    text = "Logs what it sees instead of blocking, so you can tune " +
//                            "matching in Logcat (tag: ReelsBlocker).",
//                    style = MaterialTheme.typography.bodySmall
//                )
//            }
//            Switch(
//                checked = debugMode,
//                onCheckedChange = {
//                    debugMode = it
//                    prefs.edit().putBoolean(ReelsAccessibilityService.KEY_DEBUG_MODE, it).apply()
//                }
//            )
//        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponent = "${context.packageName}/${ReelsAccessibilityService::class.java.name}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
}