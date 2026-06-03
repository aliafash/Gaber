package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.PortalAppContent
import com.example.ui.PortalViewModel
import com.example.ui.theme.PortalTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: PortalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PortalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    PortalAppContent(viewModel = viewModel)
                }
            }
        }
    }
}
