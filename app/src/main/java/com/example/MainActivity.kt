package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.database.InventoryDatabase
import com.example.data.repository.ITRepository
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ITViewModel
import com.example.ui.viewmodel.ITViewModelFactory

class MainActivity : FragmentActivity() {

    private val database by lazy { InventoryDatabase.getDatabase(this) }
    private val repository by lazy { ITRepository(database.inventoryDao()) }
    
    // Instantiate ViewModel with custom factory for injection
    private val viewModel: ITViewModel by viewModels {
        ITViewModelFactory(repository)
    }

    private fun tryInitializeFirebase() {
        val apiKey = try { BuildConfig.FIREBASE_API_KEY } catch (e: Throwable) { "" }
        val appId = try { BuildConfig.FIREBASE_APPLICATION_ID } catch (e: Throwable) { "" }
        val projectId = try { BuildConfig.FIREBASE_PROJECT_ID } catch (e: Throwable) { "" }
        
        val isValid = apiKey.isNotBlank() && apiKey != "YOUR_FIREBASE_API_KEY" &&
                appId.isNotBlank() && appId != "YOUR_FIREBASE_APPLICATION_ID" &&
                projectId.isNotBlank() && projectId != "YOUR_FIREBASE_PROJECT_ID"
                
        if (isValid) {
            try {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        tryInitializeFirebase()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                MainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
