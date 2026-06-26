package com.example.data.local

import androidx.room.*
import com.example.data.model.Booking
import com.example.data.model.RentalItem
import com.example.data.model.Review
import com.example.data.model.User
import com.example.data.model.ChatMessage
import com.example.data.model.Dispute
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserByIdSync(id: Long): User?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)
}

@Dao
interface RentalItemDao {
    @Query("SELECT * FROM rental_items ORDER BY id DESC")
    fun getAllItems(): Flow<List<RentalItem>>

    @Query("SELECT * FROM rental_items WHERE id = :id")
    fun getItemById(id: Long): Flow<RentalItem?>

    @Query("SELECT * FROM rental_items WHERE id = :id")
    suspend fun getItemByIdSync(id: Long): RentalItem?

    @Query("SELECT * FROM rental_items WHERE ownerId = :ownerId ORDER BY id DESC")
    fun getItemsByOwner(ownerId: Long): Flow<List<RentalItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: RentalItem): Long

    @Update
    suspend fun updateItem(item: RentalItem)

    @Delete
    suspend fun deleteItem(item: RentalItem)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY timestamp DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    fun getBookingById(id: Long): Flow<Booking?>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingByIdSync(id: Long): Booking?

    @Query("SELECT * FROM bookings WHERE renterId = :renterId ORDER BY timestamp DESC")
    fun getBookingsByRenter(renterId: Long): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE ownerId = :ownerId ORDER BY timestamp DESC")
    fun getBookingsByOwner(ownerId: Long): Flow<List<Booking>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking): Long

    @Update
    suspend fun updateBooking(booking: Booking)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE targetId = :targetId AND targetType = :targetType ORDER BY timestamp DESC")
    fun getReviewsForTarget(targetId: Long, targetType: String): Flow<List<Review>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review): Long
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE (senderId = :userA AND receiverId = :userB) OR (senderId = :userB AND receiverId = :userA) ORDER BY timestamp ASC")
    fun getMessagesBetweenUsers(userA: Long, userB: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE receiverId = :userId ORDER BY timestamp DESC")
    fun getAllReceivedMessages(userId: Long): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE senderId = :userId OR receiverId = :userId ORDER BY timestamp DESC")
    fun getAllMessagesForUser(userId: Long): Flow<List<ChatMessage>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE receiverId = :userId AND isRead = 0")
    fun getUnreadMessageCount(userId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("UPDATE chat_messages SET isRead = 1 WHERE senderId = :senderId AND receiverId = :receiverId")
    suspend fun markMessagesAsRead(senderId: Long, receiverId: Long)
}

@Dao
interface DisputeDao {
    @Query("SELECT * FROM disputes ORDER BY timestamp DESC")
    fun getAllDisputes(): Flow<List<Dispute>>

    @Query("SELECT * FROM disputes WHERE reporterId = :userId OR againstId = :userId ORDER BY timestamp DESC")
    fun getDisputesForUser(userId: Long): Flow<List<Dispute>>

    @Query("SELECT * FROM disputes WHERE id = :id")
    suspend fun getDisputeById(id: Long): Dispute?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispute(dispute: Dispute): Long

    @Update
    suspend fun updateDispute(dispute: Dispute)
}
