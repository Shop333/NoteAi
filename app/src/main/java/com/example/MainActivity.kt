package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.repository.WorkspaceRepository
import com.example.ui.screens.WorkspaceDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WorkspaceViewModel

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: WorkspaceRepository
    private lateinit var viewModel: WorkspaceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SQLite Room Local Database with automatic migrations support
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "thinkspace_ai_db"
        ).fallbackToDestructiveMigration().build()

        // Setup repository layer with local DB DAO access interfaces
        repository = WorkspaceRepository(
            noteDao = database.noteDao(),
            chatDao = database.chatDao()
        )

        // Initialize state view-model with manual construction injection
        viewModel = WorkspaceViewModel(repository)

        enableEdgeToEdge()
        setContent {
            // Memeriksa mode tema yang dipilih oleh pengguna di ViewModel
            val isDark = when (viewModel.themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WorkspaceDashboard(viewModel = viewModel)
                }
            }
        }
    }
}

