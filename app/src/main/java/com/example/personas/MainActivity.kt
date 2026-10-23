package com.example.personas

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.enableEdgeToEdge
import com.example.personas.ui.PersonasApp
import com.example.personas.ui.theme.PersonasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.DKGRAY),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.DKGRAY)
        )
        val factory = (application as PersonasApplication).container.viewModelFactory
        setContent { PersonasTheme { PersonasApp(viewModel(factory = factory)) } }
    }
}
