package com.rhodes.algae

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rhodes.algae.ui.screens.MainScreen
import com.rhodes.algae.ui.theme.AlgaeTheme
import com.rhodes.algae.viewmodel.TrainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlgaeTheme {
                val vm: TrainViewModel = viewModel()
                vm.loadData()
                MainScreen(vm)
            }
        }
    }
}
