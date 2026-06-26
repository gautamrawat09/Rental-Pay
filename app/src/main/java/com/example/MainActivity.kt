package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.RentalRepository
import com.example.ui.RentalPayApp
import com.example.ui.RentalViewModel
import com.example.ui.RentalViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        // Initialize database, repository, and ViewModel
        val context = this
        val database = AppDatabase.getDatabase(context)
        val repository = RentalRepository(
          userDao = database.userDao(),
          itemDao = database.rentalItemDao(),
          bookingDao = database.bookingDao(),
          reviewDao = database.reviewDao(),
          chatMessageDao = database.chatMessageDao(),
          disputeDao = database.disputeDao()
        )
        val factory = RentalViewModelFactory(repository)
        val viewModel: RentalViewModel = viewModel(factory = factory)

        RentalPayApp(viewModel = viewModel)
      }
    }
  }
}
