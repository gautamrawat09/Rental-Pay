package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String,
    val communityName: String,
    val walletBalance: Double = 500.0, // Pre-funded with simulated cash for testing
    val isVerified: Boolean = false,
    val idType: String = "", // e.g. "Driver's License", "Passport"
    val idNumber: String = "",
    val rating: Float = 5.0f,
    val ratingsCount: Int = 1,
    val idPhotoPath: String = "",
    val linkedGoogle: Boolean = false,
    val linkedFacebook: Boolean = false,
    val linkedTwitter: Boolean = false,
    val linkedLinkedIn: Boolean = false,
    val verificationDate: Long = 0L
)

@Entity(tableName = "rental_items")
data class RentalItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // Tools, Electronics, Vehicles, Sports, Outdoors, Party, Clothing
    val pricePerDay: Double,
    val securityDeposit: Double,
    val ownerId: Long,
    val ownerName: String,
    val ownerIsVerified: Boolean = false,
    val ownerRating: Float = 5.0f,
    val isAvailable: Boolean = true,
    val imageUrl: String = "", // Used to load local assets or icons
    val location: String, // Local neighborhood / community
    val condition: String // New, Like New, Very Good, Good, Fair
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val itemTitle: String,
    val itemCategory: String,
    val pricePerDay: Double,
    val securityDeposit: Double,
    val ownerId: Long,
    val ownerName: String,
    val renterId: Long,
    val renterName: String,
    val startDate: Long, // Date in epoch millis
    val endDate: Long,   // Date in epoch millis
    val totalCost: Double,
    val status: String, // "PENDING" (Waiting Approval), "ACTIVE" (In Use), "COMPLETED" (Returned), "CANCELLED"
    val isPaid: Boolean = false,
    val isRenterRated: Boolean = false, // Renter rated the experience
    val isOwnerRated: Boolean = false,  // Owner rated the renter
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetId: Long, // ID of the user or item being reviewed
    val targetType: String, // "USER" or "ITEM"
    val reviewerId: Long,
    val reviewerName: String,
    val rating: Int, // 1 to 5
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderId: Long,
    val senderName: String,
    val receiverId: Long,
    val receiverName: String,
    val messageText: String,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "disputes")
data class Dispute(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: Long,
    val itemTitle: String,
    val reporterId: Long,
    val reporterName: String,
    val againstId: Long,
    val againstName: String,
    val reason: String, // "Damaged Item", "Late Return", "Not as Described", "Unfair Charge", "Other"
    val description: String,
    val evidenceUrl: String = "", // e.g. text or image reference
    val status: String = "OPEN", // "OPEN", "MEDIATION", "RESOLVED"
    val outcome: String = "", // Admin outcome description
    val adminNotes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

