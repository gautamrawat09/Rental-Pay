package com.example.data.repository

import com.example.data.local.BookingDao
import com.example.data.local.RentalItemDao
import com.example.data.local.ReviewDao
import com.example.data.local.UserDao
import com.example.data.local.ChatMessageDao
import com.example.data.local.DisputeDao
import com.example.data.model.Booking
import com.example.data.model.RentalItem
import com.example.data.model.Review
import com.example.data.model.User
import com.example.data.model.ChatMessage
import com.example.data.model.Dispute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class RentalRepository(
    private val userDao: UserDao,
    private val itemDao: RentalItemDao,
    private val bookingDao: BookingDao,
    private val reviewDao: ReviewDao,
    private val chatMessageDao: ChatMessageDao,
    private val disputeDao: DisputeDao
) {
    // Chat messages
    fun getMessagesBetweenUsers(userA: Long, userB: Long): Flow<List<ChatMessage>> =
        chatMessageDao.getMessagesBetweenUsers(userA, userB)

    fun getAllReceivedMessages(userId: Long): Flow<List<ChatMessage>> =
        chatMessageDao.getAllReceivedMessages(userId)

    fun getAllMessagesForUser(userId: Long): Flow<List<ChatMessage>> =
        chatMessageDao.getAllMessagesForUser(userId)

    fun getUnreadMessageCount(userId: Long): Flow<Int> =
        chatMessageDao.getUnreadMessageCount(userId)

    suspend fun sendChatMessage(senderId: Long, senderName: String, receiverId: Long, receiverName: String, text: String): Long {
        val msg = ChatMessage(
            senderId = senderId,
            senderName = senderName,
            receiverId = receiverId,
            receiverName = receiverName,
            messageText = text
        )
        return chatMessageDao.insertMessage(msg)
    }

    suspend fun markMessagesAsRead(senderId: Long, receiverId: Long) {
        chatMessageDao.markMessagesAsRead(senderId, receiverId)
    }

    // Disputes
    fun getAllDisputes(): Flow<List<Dispute>> = disputeDao.getAllDisputes()
    
    fun getDisputesForUser(userId: Long): Flow<List<Dispute>> = disputeDao.getDisputesForUser(userId)

    suspend fun fileDispute(
        bookingId: Long,
        itemTitle: String,
        reporterId: Long,
        reporterName: String,
        againstId: Long,
        againstName: String,
        reason: String,
        description: String,
        evidenceUrl: String = ""
    ): Long {
        val dispute = Dispute(
            bookingId = bookingId,
            itemTitle = itemTitle,
            reporterId = reporterId,
            reporterName = reporterName,
            againstId = againstId,
            againstName = againstName,
            reason = reason,
            description = description,
            evidenceUrl = evidenceUrl,
            status = "OPEN"
        )
        return disputeDao.insertDispute(dispute)
    }

    suspend fun updateDispute(dispute: Dispute) = disputeDao.updateDispute(dispute)

    suspend fun getDisputeById(id: Long): Dispute? = disputeDao.getDisputeById(id)

    // Users
    fun getUserById(id: Long): Flow<User?> = userDao.getUserById(id)
    suspend fun getUserByIdSync(id: Long): User? = userDao.getUserByIdSync(id)
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
    suspend fun insertUser(user: User): Long = userDao.insertUser(user)
    suspend fun updateUser(user: User) = userDao.updateUser(user)

    // Items
    fun getAllItems(): Flow<List<RentalItem>> = itemDao.getAllItems()
    fun getItemById(id: Long): Flow<RentalItem?> = itemDao.getItemById(id)
    fun getItemsByOwner(ownerId: Long): Flow<List<RentalItem>> = itemDao.getItemsByOwner(ownerId)
    suspend fun insertItem(item: RentalItem): Long = itemDao.insertItem(item)
    suspend fun updateItem(item: RentalItem) = itemDao.updateItem(item)
    suspend fun deleteItem(item: RentalItem) = itemDao.deleteItem(item)

    // Bookings
    fun getAllBookings(): Flow<List<Booking>> = bookingDao.getAllBookings()
    fun getBookingById(id: Long): Flow<Booking?> = bookingDao.getBookingById(id)
    fun getBookingsByRenter(renterId: Long): Flow<List<Booking>> = bookingDao.getBookingsByRenter(renterId)
    fun getBookingsByOwner(ownerId: Long): Flow<List<Booking>> = bookingDao.getBookingsByOwner(ownerId)
    suspend fun updateBooking(booking: Booking) = bookingDao.updateBooking(booking)

    // Reviews
    fun getReviewsForTarget(targetId: Long, targetType: String): Flow<List<Review>> =
        reviewDao.getReviewsForTarget(targetId, targetType)

    /**
     * Submit a trust review for a user or item.
     * Recalculates the target user's ratings and saves the review.
     */
    suspend fun submitReview(review: Review) {
        reviewDao.insertReview(review)
        
        if (review.targetType == "USER") {
            val user = userDao.getUserByIdSync(review.targetId)
            if (user != null) {
                val currentRatingSum = user.rating * user.ratingsCount
                val newCount = user.ratingsCount + 1
                val newAverage = (currentRatingSum + review.rating) / newCount
                userDao.updateUser(
                    user.copy(
                        rating = newAverage,
                        ratingsCount = newCount
                    )
                )
            }
        }
    }

    /**
     * Requests a rental booking.
     * Checks if the renter has sufficient wallet balance for rental price + security deposit.
     * If yes, creates a pending booking.
     */
    suspend fun requestBooking(
        itemId: Long,
        renterId: Long,
        startDate: Long,
        endDate: Long
    ): Result<Long> {
        val item = itemDao.getItemByIdSync(itemId) ?: return Result.failure(Exception("Item not found"))
        val renter = userDao.getUserByIdSync(renterId) ?: return Result.failure(Exception("Renter profile not found"))

        if (!item.isAvailable) {
            return Result.failure(Exception("Item is currently not available for rent"))
        }
        if (item.ownerId == renterId) {
            return Result.failure(Exception("You cannot rent your own item"))
        }

        val days = maxOf(1L, (endDate - startDate) / (24 * 60 * 60 * 1000))
        val daysPrice = item.pricePerDay * days
        val serviceFee = Math.round((daysPrice * 0.05 + 1.50) * 100.0) / 100.0
        val totalCost = daysPrice + item.securityDeposit + serviceFee

        if (renter.walletBalance < totalCost) {
            return Result.failure(Exception("Insufficient wallet balance. Total cost ($$totalCost) exceeds balance ($${renter.walletBalance}). Please add funds in Profile."))
        }

        val booking = Booking(
            itemId = item.id,
            itemTitle = item.title,
            itemCategory = item.category,
            pricePerDay = item.pricePerDay,
            securityDeposit = item.securityDeposit,
            ownerId = item.ownerId,
            ownerName = item.ownerName,
            renterId = renter.id,
            renterName = renter.name,
            startDate = startDate,
            endDate = endDate,
            totalCost = totalCost,
            status = "PENDING",
            isPaid = false
        )

        val bookingId = bookingDao.insertBooking(booking)
        return Result.success(bookingId)
    }

    /**
     * Approves an incoming booking request.
     * Performs secure wallet fund transfer (simulated, verified).
     */
    suspend fun approveBooking(bookingId: Long): Result<Boolean> {
        val booking = bookingDao.getBookingByIdSync(bookingId) ?: return Result.failure(Exception("Booking not found"))
        if (booking.status != "PENDING") {
            return Result.failure(Exception("Booking is not in PENDING status"))
        }

        val renter = userDao.getUserByIdSync(booking.renterId) ?: return Result.failure(Exception("Renter profile not found"))
        val owner = userDao.getUserByIdSync(booking.ownerId) ?: return Result.failure(Exception("Owner profile not found"))

        if (renter.walletBalance < booking.totalCost) {
            // Cancel booking if renter can no longer afford it
            bookingDao.updateBooking(booking.copy(status = "CANCELLED"))
            return Result.failure(Exception("Renter has insufficient wallet balance. Request cancelled."))
        }

        // Deduct from renter, add to owner (with security deposit held on blockchain/smart contract escrow simulation)
        // Owner receives the rent + deposit, but the local platform fee is securely retained by the network.
        val days = maxOf(1L, (booking.endDate - booking.startDate) / (24 * 60 * 60 * 1000))
        val daysPrice = booking.pricePerDay * days
        val serviceFee = Math.round((daysPrice * 0.05 + 1.50) * 100.0) / 100.0

        val updatedRenter = renter.copy(walletBalance = renter.walletBalance - booking.totalCost)
        val updatedOwner = owner.copy(walletBalance = owner.walletBalance + (booking.totalCost - serviceFee))

        userDao.updateUser(updatedRenter)
        userDao.updateUser(updatedOwner)

        // Mark item as rented out (not available)
        val item = itemDao.getItemByIdSync(booking.itemId)
        if (item != null) {
            itemDao.updateItem(item.copy(isAvailable = false))
        }

        bookingDao.updateBooking(
            booking.copy(
                status = "ACTIVE",
                isPaid = true
            )
        )

        return Result.success(true)
    }

    /**
     * Rejects an incoming booking request.
     */
    suspend fun rejectBooking(bookingId: Long): Result<Boolean> {
        val booking = bookingDao.getBookingByIdSync(bookingId) ?: return Result.failure(Exception("Booking not found"))
        if (booking.status != "PENDING") {
            return Result.failure(Exception("Booking is not in PENDING status"))
        }

        bookingDao.updateBooking(booking.copy(status = "CANCELLED"))
        return Result.success(true)
    }

    /**
     * Complete a rental (item returned).
     * Releases security deposit back to the renter!
     */
    suspend fun completeBooking(bookingId: Long): Result<Boolean> {
        val booking = bookingDao.getBookingByIdSync(bookingId) ?: return Result.failure(Exception("Booking not found"))
        if (booking.status != "ACTIVE") {
            return Result.failure(Exception("Booking is not active"))
        }

        val renter = userDao.getUserByIdSync(booking.renterId) ?: return Result.failure(Exception("Renter profile not found"))
        val owner = userDao.getUserByIdSync(booking.ownerId) ?: return Result.failure(Exception("Owner profile not found"))

        // Release deposit: Renter gets the deposit back, Owner keeps the rental cost.
        val releaseAmount = booking.securityDeposit
        val updatedRenter = renter.copy(walletBalance = renter.walletBalance + releaseAmount)
        val updatedOwner = owner.copy(walletBalance = owner.walletBalance - releaseAmount) // Owner balance was credited full amount, now deposit goes back

        userDao.updateUser(updatedRenter)
        userDao.updateUser(updatedOwner)

        // Mark item as available again
        val item = itemDao.getItemByIdSync(booking.itemId)
        if (item != null) {
            itemDao.updateItem(item.copy(isAvailable = true))
        }

        bookingDao.updateBooking(booking.copy(status = "COMPLETED"))
        return Result.success(true)
    }

    /**
     * Add simulated funds to current user's wallet
     */
    suspend fun depositFunds(userId: Long, amount: Double): Result<User> {
        val user = userDao.getUserByIdSync(userId) ?: return Result.failure(Exception("User not found"))
        val updatedUser = user.copy(walletBalance = user.walletBalance + amount)
        userDao.updateUser(updatedUser)
        return Result.success(updatedUser)
    }

    /**
     * Prepopulates initial data if tables are empty.
     */
    suspend fun prepopulateIfEmpty() {
        val currentUsers = userDao.getAllUsers().first()
        if (currentUsers.isEmpty()) {
            // Insert primary user (id = 1)
            val primaryUser = User(
                id = 1,
                name = "Gautam Rawat",
                email = "rawatgautam10000@gmail.com",
                phone = "555-0101",
                communityName = "Oakridge Estates",
                walletBalance = 650.0,
                isVerified = false,
                idType = "",
                idNumber = ""
            )
            userDao.insertUser(primaryUser)

            // Insert peer community users
            val peerUsers = listOf(
                User(id = 2, name = "Sarah Jenkins", email = "sarah.j@oakridge.net", phone = "555-0122", communityName = "Oakridge Estates", walletBalance = 420.0, isVerified = true, idType = "Passport", idNumber = "US-P9384", rating = 4.9f, ratingsCount = 15),
                User(id = 3, name = "Marcus Chen", email = "m.chen@maple.org", phone = "555-0144", communityName = "Maple Valley", walletBalance = 580.0, isVerified = true, idType = "Driver's License", idNumber = "DL-28472", rating = 4.7f, ratingsCount = 8),
                User(id = 4, name = "Elena Rostova", email = "elena.r@glen.com", phone = "555-0155", communityName = "Oakridge Estates", walletBalance = 310.0, isVerified = false, rating = 4.8f, ratingsCount = 4)
            )
            for (user in peerUsers) {
                userDao.insertUser(user)
            }

            // Insert initial items
            val items = listOf(
                RentalItem(
                    id = 1,
                    title = "Heavy Duty Lawn Mower",
                    description = "Self-propelled gas-powered lawn mower. Perfect for medium to large lawns. Easy start mechanism. Comes with full tank of gas, please return clean and refilled.",
                    category = "Tools",
                    pricePerDay = 25.0,
                    securityDeposit = 50.0,
                    ownerId = 3,
                    ownerName = "Marcus Chen",
                    ownerIsVerified = true,
                    ownerRating = 4.7f,
                    isAvailable = true,
                    imageUrl = "",
                    location = "Maple Valley",
                    condition = "Very Good"
                ),
                RentalItem(
                    id = 2,
                    title = "4K Video Projector & Screen",
                    description = "3500 Lumens cinematic projector. Ideal for backyard movie nights or indoor presentations. Includes a 100-inch collapsible tripod screen and HDMI cords.",
                    category = "Electronics",
                    pricePerDay = 45.0,
                    securityDeposit = 100.0,
                    ownerId = 2,
                    ownerName = "Sarah Jenkins",
                    ownerIsVerified = true,
                    ownerRating = 4.9f,
                    isAvailable = true,
                    imageUrl = "",
                    location = "Oakridge Estates",
                    condition = "Like New"
                ),
                RentalItem(
                    id = 3,
                    title = "Two-Person Kayak",
                    description = "Rigid ocean/lake tandem kayak. Durable, steady, and comes with two lightweight paddles, two life jackets (size M & L), and roof rack straps for easy hauling.",
                    category = "Outdoors",
                    pricePerDay = 35.0,
                    securityDeposit = 80.0,
                    ownerId = 4,
                    ownerName = "Elena Rostova",
                    ownerIsVerified = false,
                    ownerRating = 4.8f,
                    isAvailable = true,
                    imageUrl = "",
                    location = "Oakridge Estates",
                    condition = "Good"
                ),
                RentalItem(
                    id = 4,
                    title = "Commercial Pressure Washer",
                    description = "3100 PSI gas pressure washer. Extremely effective at removing grime from concrete, decks, siding, and driveways. Multiple spray nozzles included.",
                    category = "Tools",
                    pricePerDay = 20.0,
                    securityDeposit = 40.0,
                    ownerId = 2,
                    ownerName = "Sarah Jenkins",
                    ownerIsVerified = true,
                    ownerRating = 4.9f,
                    isAvailable = true,
                    imageUrl = "",
                    location = "Oakridge Estates",
                    condition = "Excellent"
                ),
                RentalItem(
                    id = 5,
                    title = "Foldable Party Gazebo (10x20ft)",
                    description = "Waterproof pop-up canopy tent with side walls. Sets up in under 10 minutes (best with 2 people). Great for family gatherings, flea markets, or birthdays.",
                    category = "Party",
                    pricePerDay = 30.0,
                    securityDeposit = 60.0,
                    ownerId = 3,
                    ownerName = "Marcus Chen",
                    ownerIsVerified = true,
                    ownerRating = 4.7f,
                    isAvailable = true,
                    imageUrl = "",
                    location = "Maple Valley",
                    condition = "Very Good"
                )
            )
            for (item in items) {
                itemDao.insertItem(item)
            }

            // Insert initial Reviews
            val reviews = listOf(
                Review(targetId = 2, targetType = "USER", reviewerId = 3, reviewerName = "Marcus Chen", rating = 5, comment = "Sarah returned my gazebo in pristine condition and on time. Highly recommended borrower!"),
                Review(targetId = 3, targetType = "USER", reviewerId = 2, reviewerName = "Sarah Jenkins", rating = 5, comment = "Marcus was incredibly helpful. The lawn mower was clean and worked perfectly."),
                Review(targetId = 1, targetType = "ITEM", reviewerId = 2, reviewerName = "Sarah Jenkins", rating = 4, comment = "Great mower, starts on the first pull. Saved me a lot of money compared to hiring a landscaper.")
            )
            for (review in reviews) {
                reviewDao.insertReview(review)
            }

            // Insert initial ChatMessages
            val messages = listOf(
                ChatMessage(
                    senderId = 2,
                    senderName = "Sarah Jenkins",
                    receiverId = 1,
                    receiverName = "Gautam Rawat",
                    messageText = "Hi Gautam, is the 4K Video Projector still available for this Saturday?",
                    isRead = false,
                    timestamp = System.currentTimeMillis() - 3600000 // 1 hour ago
                ),
                ChatMessage(
                    senderId = 1,
                    senderName = "Gautam Rawat",
                    receiverId = 2,
                    receiverName = "Sarah Jenkins",
                    messageText = "Hi Sarah! Yes, it's fully available and clean. Let me know when you want to pick it up.",
                    isRead = true,
                    timestamp = System.currentTimeMillis() - 1800000 // 30 mins ago
                ),
                ChatMessage(
                    senderId = 2,
                    senderName = "Sarah Jenkins",
                    receiverId = 1,
                    receiverName = "Gautam Rawat",
                    messageText = "Awesome! I'll come by around 10:00 AM if that works for you?",
                    isRead = false,
                    timestamp = System.currentTimeMillis() - 900000 // 15 mins ago
                ),
                ChatMessage(
                    senderId = 3,
                    senderName = "Marcus Chen",
                    receiverId = 1,
                    receiverName = "Gautam Rawat",
                    messageText = "Hey Gautam, thanks for renting the lawn mower! It worked wonders on my lawn.",
                    isRead = true,
                    timestamp = System.currentTimeMillis() - 7200000 // 2 hours ago
                )
            )
            for (message in messages) {
                chatMessageDao.insertMessage(message)
            }

            // Insert initial Dispute
            val initialDispute = Dispute(
                bookingId = 1,
                itemTitle = "Heavy Duty Lawn Mower",
                reporterId = 1,
                reporterName = "Gautam Rawat",
                againstId = 3,
                againstName = "Marcus Chen",
                reason = "Damaged Item",
                description = "The mower was returned with a bent blade and the motor casing has a new deep crack.",
                status = "MEDIATION",
                evidenceUrl = "Blade bent at 30 degrees",
                adminNotes = "Admin verified the damage report. Under review.",
                timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
            )
            disputeDao.insertDispute(initialDispute)
        }
    }
}
