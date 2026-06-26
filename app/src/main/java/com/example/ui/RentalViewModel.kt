package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Booking
import com.example.data.model.RentalItem
import com.example.data.model.Review
import com.example.data.model.User
import com.example.data.model.ChatMessage
import com.example.data.model.Dispute
import com.example.data.repository.RentalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class RentalViewModel(private val repository: RentalRepository) : ViewModel() {

    // Current Logged-in User (starts null)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // All registered users
    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available community items
    val items: StateFlow<List<RentalItem>> = repository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookings where user is the renter
    val myRentals: StateFlow<List<Booking>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getBookingsByRenter(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookings where user is the owner
    val incomingRequests: StateFlow<List<Booking>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getBookingsByOwner(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Item Detail
    private val _selectedItem = MutableStateFlow<RentalItem?>(null)
    val selectedItem: StateFlow<RentalItem?> = _selectedItem.asStateFlow()

    // Reviews for the selected item or user
    private val _selectedReviews = MutableStateFlow<List<Review>>(emptyList())
    val selectedReviews: StateFlow<List<Review>> = _selectedReviews.asStateFlow()

    // Chat messages involving current user
    val chatMessages: StateFlow<List<ChatMessage>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getAllMessagesForUser(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unread messages count for current user
    val unreadCount: StateFlow<Int> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getUnreadMessageCount(user.id) else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Disputes filed by or against current user
    val disputes: StateFlow<List<Dispute>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getDisputesForUser(user.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback Message
    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            // First, prepopulate if database is empty
            repository.prepopulateIfEmpty()
        }
    }

    fun login(user: User) {
        viewModelScope.launch {
            _currentUser.value = user
            _uiMessage.emit("Welcome back, ${user.name}!")
        }
    }

    fun logout() {
        _currentUser.value = null
        viewModelScope.launch {
            _uiMessage.emit("Logged out successfully.")
        }
    }

    fun registerAndLogin(name: String, email: String, phone: String, communityName: String) {
        viewModelScope.launch {
            if (name.isBlank() || email.isBlank()) {
                _uiMessage.emit("Please enter both Name and Email.")
                return@launch
            }
            val existing = allUsers.value.find { it.email.equals(email, ignoreCase = true) }
            if (existing != null) {
                _currentUser.value = existing
                _uiMessage.emit("Email already registered. Welcome back, ${existing.name}!")
                return@launch
            }

            val newUser = User(
                name = name,
                email = email,
                phone = phone,
                communityName = if (communityName.isBlank()) "Oakridge Estates" else communityName,
                walletBalance = 250.0, // generous starting balance for demoing renting!
                isVerified = false,
                idType = "",
                idNumber = ""
            )
            val newId = repository.insertUser(newUser)
            val createdUser = newUser.copy(id = newId)
            _currentUser.value = createdUser
            _uiMessage.emit("Account created! Logged in as ${createdUser.name}")
        }
    }

    fun selectItem(item: RentalItem) {
        _selectedItem.value = item
        viewModelScope.launch {
            repository.getReviewsForTarget(item.id, "ITEM")
                .collect { _selectedReviews.value = it }
        }
    }

    fun selectUserReviews(userId: Long) {
        viewModelScope.launch {
            repository.getReviewsForTarget(userId, "USER")
                .collect { _selectedReviews.value = it }
        }
    }

    /**
     * List a new item for rent (Give on Rent)
     */
    fun listItemForRent(
        title: String,
        description: String,
        category: String,
        pricePerDay: Double,
        securityDeposit: Double,
        condition: String
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                _uiMessage.emit("Unable to list item: user not loaded.")
                return@launch
            }

            val item = RentalItem(
                title = title,
                description = description,
                category = category,
                pricePerDay = pricePerDay,
                securityDeposit = securityDeposit,
                ownerId = user.id,
                ownerName = user.name,
                ownerIsVerified = user.isVerified,
                ownerRating = user.rating,
                isAvailable = true,
                location = user.communityName,
                condition = condition
            )

            repository.insertItem(item)
            _uiMessage.emit("Successfully listed '$title' for rent in ${user.communityName}!")
        }
    }

    /**
     * Request to Rent an Item
     */
    fun rentItem(itemId: Long, days: Int) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                _uiMessage.emit("Unable to request: user not loaded")
                return@launch
            }

            val startDate = System.currentTimeMillis()
            val endDate = startDate + (days * 24 * 60 * 60 * 1000L)

            val result = repository.requestBooking(itemId, user.id, startDate, endDate)
            result.onSuccess {
                _uiMessage.emit("Rental request submitted! Waiting for owner's approval.")
            }
            result.onFailure { error ->
                _uiMessage.emit(error.message ?: "Failed to submit rental request")
            }
        }
    }

    /**
     * Approve Booking Request (Owner action)
     */
    fun approveBooking(bookingId: Long) {
        viewModelScope.launch {
            val result = repository.approveBooking(bookingId)
            result.onSuccess {
                _uiMessage.emit("Booking approved! Funds securely transferred and held in escrow.")
            }
            result.onFailure { error ->
                _uiMessage.emit(error.message ?: "Failed to approve booking")
            }
        }
    }

    /**
     * Decline/Reject Booking Request (Owner action)
     */
    fun rejectBooking(bookingId: Long) {
        viewModelScope.launch {
            val result = repository.rejectBooking(bookingId)
            result.onSuccess {
                _uiMessage.emit("Booking request declined.")
            }
            result.onFailure { error ->
                _uiMessage.emit(error.message ?: "Failed to decline booking")
            }
        }
    }

    /**
     * Complete Booking (Item Returned)
     */
    fun completeBooking(bookingId: Long) {
        viewModelScope.launch {
            val result = repository.completeBooking(bookingId)
            result.onSuccess {
                _uiMessage.emit("Rental completed! Security deposit safely refunded to the renter.")
            }
            result.onFailure { error ->
                _uiMessage.emit(error.message ?: "Failed to complete rental")
            }
        }
    }

    /**
     * Deposit Simulated Cash
     */
    fun depositFunds(amount: Double) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val result = repository.depositFunds(user.id, amount)
            result.onSuccess {
                _uiMessage.emit("Successfully deposited $${String.format("%.2f", amount)} into your secure wallet.")
            }
            result.onFailure {
                _uiMessage.emit("Deposit failed.")
            }
        }
    }

    /**
     * Complete Profile Verification
     */
    fun verifyProfile(
        idType: String,
        idNumber: String,
        idPhotoPath: String,
        linkedGoogle: Boolean,
        linkedFacebook: Boolean,
        linkedTwitter: Boolean,
        linkedLinkedIn: Boolean
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            if (idType.isBlank() || idNumber.isBlank()) {
                _uiMessage.emit("Please enter valid document details.")
                return@launch
            }

            val updatedUser = user.copy(
                isVerified = true,
                idType = idType,
                idNumber = idNumber,
                idPhotoPath = idPhotoPath,
                linkedGoogle = linkedGoogle,
                linkedFacebook = linkedFacebook,
                linkedTwitter = linkedTwitter,
                linkedLinkedIn = linkedLinkedIn,
                verificationDate = System.currentTimeMillis()
            )
            repository.updateUser(updatedUser)
            _uiMessage.emit("Identity verified! You now have a verified community profile badge.")
        }
    }

    /**
     * Reset Verification Status for testing purposes
     */
    fun resetVerification() {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val updatedUser = user.copy(
                isVerified = false,
                idType = "",
                idNumber = "",
                idPhotoPath = "",
                linkedGoogle = false,
                linkedFacebook = false,
                linkedTwitter = false,
                linkedLinkedIn = false,
                verificationDate = 0L
            )
            repository.updateUser(updatedUser)
            _uiMessage.emit("Verification reset! You can now verify again.")
        }
    }

    /**
     * Submit Trust Review for Peer / Item
     */
    fun submitReview(targetId: Long, targetType: String, rating: Int, comment: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            if (comment.isBlank()) {
                _uiMessage.emit("Please write a short comment to support your rating.")
                return@launch
            }

            val review = Review(
                targetId = targetId,
                targetType = targetType,
                reviewerId = user.id,
                reviewerName = user.name,
                rating = rating,
                comment = comment
            )

            repository.submitReview(review)
            
            // Update selected reviews list if active
            val currentSelected = _selectedItem.value
            if (targetType == "ITEM" && currentSelected != null && currentSelected.id == targetId) {
                repository.getReviewsForTarget(targetId, "ITEM").collect { _selectedReviews.value = it }
            } else if (targetType == "USER") {
                repository.getReviewsForTarget(targetId, "USER").collect { _selectedReviews.value = it }
            }

            _uiMessage.emit("Trust review submitted successfully! Thank you for building community trust.")
        }
    }

    /**
     * Send in-app Chat message
     */
    fun sendChatMessage(receiverId: Long, receiverName: String, messageText: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            if (messageText.isBlank()) return@launch
            repository.sendChatMessage(
                senderId = user.id,
                senderName = user.name,
                receiverId = receiverId,
                receiverName = receiverName,
                text = messageText
            )
        }
    }

    /**
     * Mark chat messages between user and another user as read
     */
    fun markMessagesAsRead(senderId: Long) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.markMessagesAsRead(senderId = senderId, receiverId = user.id)
        }
    }

    /**
     * File a new dispute
     */
    fun fileDispute(
        bookingId: Long,
        itemTitle: String,
        againstId: Long,
        againstName: String,
        reason: String,
        description: String,
        evidenceUrl: String = ""
    ) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            if (description.isBlank()) {
                _uiMessage.emit("Please describe the dispute details.")
                return@launch
            }
            repository.fileDispute(
                bookingId = bookingId,
                itemTitle = itemTitle,
                reporterId = user.id,
                reporterName = user.name,
                againstId = againstId,
                againstName = againstName,
                reason = reason,
                description = description,
                evidenceUrl = evidenceUrl
            )
            _uiMessage.emit("Dispute reported successfully. Our neutral community admins have been notified to review and mediate.")
        }
    }

    /**
     * Mediate / Resolve Dispute (Admin mock action)
     */
    fun resolveDispute(disputeId: Long, outcome: String, adminNotes: String, status: String = "RESOLVED") {
        viewModelScope.launch {
            val dispute = repository.getDisputeById(disputeId) ?: return@launch
            val updated = dispute.copy(
                status = status,
                outcome = outcome,
                adminNotes = adminNotes,
                timestamp = System.currentTimeMillis()
            )
            repository.updateDispute(updated)
            _uiMessage.emit("Dispute status updated to $status with outcome: '$outcome'")
        }
    }
}
