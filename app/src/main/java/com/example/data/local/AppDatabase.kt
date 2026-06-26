package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Booking
import com.example.data.model.RentalItem
import com.example.data.model.Review
import com.example.data.model.User
import com.example.data.model.ChatMessage
import com.example.data.model.Dispute

@Database(
    entities = [User::class, RentalItem::class, Booking::class, Review::class, ChatMessage::class, Dispute::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun rentalItemDao(): RentalItemDao
    abstract fun bookingDao(): BookingDao
    abstract fun reviewDao(): ReviewDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun disputeDao(): DisputeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rental_pay_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
