package com.example.ruta705

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.ruta705.core.navigation.RutaNavGraph
import com.example.ruta705.ui.theme.Ruta705Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Ruta705Theme {
                RutaNavGraph(rememberNavController())
            }
        }
    }
}       
