package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Booking
import com.example.data.model.RentalItem
import com.example.data.model.Review
import com.example.data.model.User
import com.example.data.model.ChatMessage
import com.example.data.model.Dispute
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalPayApp(viewModel: RentalViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val myRentals by viewModel.myRentals.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val selectedReviews by viewModel.selectedReviews.collectAsStateWithLifecycle()

    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val disputes by viewModel.disputes.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf("explore") }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showAddListingDialog by remember { mutableStateOf(false) }
    var activeItemForRatingDialog by remember { mutableStateOf<Booking?>(null) }
    var activeRenterForRatingDialog by remember { mutableStateOf<Booking?>(null) }

    var activeChatUserId by remember { mutableStateOf<Long?>(null) }
    var activeChatUserName by remember { mutableStateOf<String>("") }
    var showDisputeCenter by remember { mutableStateOf(false) }
    var fileDisputeForBooking by remember { mutableStateOf<Booking?>(null) }

    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    // Observe snackbar messages from ViewModel
    LaunchedEffect(key1 = true) {
        viewModel.uiMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (currentUser == null) {
        LoginScreen(
            allUsers = allUsers,
            onLoginUser = { viewModel.login(it) },
            onRegisterAndLogin = { name, email, phone, community ->
                viewModel.registerAndLogin(name, email, phone, community)
            }
        )
    } else {
        Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Rental Pay Shield",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Rental Pay",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(SuccessGreen, shape = CircleShape)
                                )
                                Text(
                                    text = "Community Verified",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    currentUser?.let { user ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            onClick = { currentTab = "profile" },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wallet,
                                    contentDescription = "Wallet",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$${String.format("%.2f", user.walletBalance)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentTab == "explore",
                    onClick = { currentTab = "explore" },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Explore") },
                    label = { Text("Explore") },
                    modifier = Modifier.testTag("nav_tab_explore")
                )
                NavigationBarItem(
                    selected = currentTab == "listings",
                    onClick = { currentTab = "listings" },
                    icon = { 
                        BadgedBox(
                            badge = {
                                val pendingCount = incomingRequests.count { it.status == "PENDING" }
                                if (pendingCount > 0) {
                                    Badge { Text(pendingCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Give on Rent")
                        }
                    },
                    label = { Text("Rent Out") },
                    modifier = Modifier.testTag("nav_tab_listings")
                )
                NavigationBarItem(
                    selected = currentTab == "rentals",
                    onClick = { currentTab = "rentals" },
                    icon = { 
                        BadgedBox(
                            badge = {
                                val activeCount = myRentals.count { it.status == "ACTIVE" }
                                if (activeCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(activeCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "My Rentals")
                        }
                    },
                    label = { Text("My Rentals") },
                    modifier = Modifier.testTag("nav_tab_rentals")
                )
                NavigationBarItem(
                    selected = currentTab == "inbox",
                    onClick = { 
                        currentTab = "inbox" 
                        activeChatUserId = null
                    },
                    icon = { 
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) { Text(unreadCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Inbox")
                        }
                    },
                    label = { Text("Inbox") },
                    modifier = Modifier.testTag("nav_tab_inbox")
                )
                NavigationBarItem(
                    selected = currentTab == "profile",
                    onClick = { currentTab = "profile" },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile & Trust") },
                    label = { Text("Profile") },
                    modifier = Modifier.testTag("nav_tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "explore" -> ExploreScreen(
                    items = items,
                    onItemClick = { item ->
                        viewModel.selectItem(item)
                        showDetailSheet = true
                    }
                )
                "listings" -> MyListingsScreen(
                    items = items.filter { currentUser != null && it.ownerId == currentUser?.id },
                    incomingRequests = incomingRequests,
                    onAddNewListingClick = { showAddListingDialog = true },
                    onApproveRequest = { viewModel.approveBooking(it) },
                    onDeclineRequest = { viewModel.rejectBooking(it) },
                    onReviewRenterClick = { activeRenterForRatingDialog = it },
                    onReportDispute = { booking -> fileDisputeForBooking = booking }
                )
                "rentals" -> MyRentalsScreen(
                    bookings = myRentals,
                    onReturnItem = { viewModel.completeBooking(it) },
                    onLeaveReview = { activeItemForRatingDialog = it },
                    onReportDispute = { booking -> fileDisputeForBooking = booking }
                )
                "inbox" -> {
                    val messages by viewModel.chatMessages.collectAsStateWithLifecycle(initialValue = emptyList())
                    InboxScreen(
                        messages = messages,
                        currentUser = currentUser ?: User(name = "Me", email = "", phone = "", communityName = ""),
                        activeChatUserId = activeChatUserId,
                        activeChatUserName = activeChatUserName,
                        onSendMessage = { receiverId, text -> viewModel.sendChatMessage(receiverId, activeChatUserName, text) },
                        onBackToInbox = {
                            activeChatUserId = null
                            activeChatUserName = ""
                        },
                        onChatSelect = { partnerId, partnerName ->
                            activeChatUserId = partnerId
                            activeChatUserName = partnerName
                        }
                    )
                }
                "profile" -> ProfileWalletScreen(
                    user = currentUser ?: User(name = "Loading", email = "", phone = "", communityName = ""),
                    reviewsReceived = selectedReviews,
                    onAddFunds = { viewModel.depositFunds(it) },
                    onVerifyProfile = { idType, idNum, idPhoto, google, facebook, twitter, linkedin ->
                        viewModel.verifyProfile(idType, idNum, idPhoto, google, facebook, twitter, linkedin)
                    },
                    onResetVerification = { viewModel.resetVerification() },
                    onLoadUserReviews = { viewModel.selectUserReviews(currentUser?.id ?: 1L) },
                    onOpenDisputeCenter = { showDisputeCenter = true },
                    onLogout = { viewModel.logout() }
                )
            }

            // Bottom Sheet for Item Details
            if (showDetailSheet && selectedItem != null) {
                ItemDetailBottomSheet(
                    item = selectedItem!!,
                    reviews = selectedReviews,
                    currentUser = currentUser,
                    onDismiss = { showDetailSheet = false },
                    onRentClick = { days ->
                        viewModel.rentItem(selectedItem!!.id, days)
                        showDetailSheet = false
                    },
                    onGoToWallet = {
                        showDetailSheet = false
                        currentTab = "profile"
                    },
                    onChatClick = { partnerId, partnerName ->
                        showDetailSheet = false
                        activeChatUserId = partnerId
                        activeChatUserName = partnerName
                        currentTab = "inbox"
                    }
                )
            }

            // Dialog for Listing New Item
            if (showAddListingDialog) {
                AddListingDialog(
                    onDismiss = { showAddListingDialog = false },
                    onConfirm = { title, desc, category, price, deposit, condition ->
                        viewModel.listItemForRent(title, desc, category, price, deposit, condition)
                        showAddListingDialog = false
                    }
                )
            }

            // Trust Rating Dialog for Item (Renter rating Item & Owner)
            activeItemForRatingDialog?.let { booking ->
                TrustReviewDialog(
                    title = "Rate your Rental of '${booking.itemTitle}'",
                    subtitle = "Provide community feedback to build trust for this item & owner.",
                    onSubmit = { rating, comment ->
                        // Rate the Item
                        viewModel.submitReview(booking.itemId, "ITEM", rating, comment)
                        // Also rate the Owner user
                        viewModel.submitReview(booking.ownerId, "USER", rating, "Renter review: $comment")
                        
                        activeItemForRatingDialog = null
                    },
                    onDismiss = { activeItemForRatingDialog = null }
                )
            }

            // Trust Rating Dialog for Renter (Owner rating Renter)
            activeRenterForRatingDialog?.let { booking ->
                TrustReviewDialog(
                    title = "Rate Borrower '${booking.renterName}'",
                    subtitle = "Verify their care of item and timeliness.",
                    onSubmit = { rating, comment ->
                        viewModel.submitReview(booking.renterId, "USER", rating, comment)
                        activeRenterForRatingDialog = null
                    },
                    onDismiss = { activeRenterForRatingDialog = null }
                )
            }

            // Dispute Center Dialog Overlay
            if (showDisputeCenter) {
                val disputes by viewModel.disputes.collectAsStateWithLifecycle(initialValue = emptyList())
                DisputeCenterDialog(
                    disputes = disputes,
                    onDismiss = { showDisputeCenter = false },
                    onResolveDispute = { disputeId, resolution ->
                        viewModel.resolveDispute(disputeId, resolution, adminNotes = "Mediated by Community Panel")
                    }
                )
            }

            // File New Dispute Dialog
            fileDisputeForBooking?.let { booking ->
                FileDisputeDialog(
                    booking = booking,
                    currentUser = currentUser ?: User(name = "", email = "", phone = "", communityName = ""),
                    onDismiss = { fileDisputeForBooking = null },
                    onConfirm = { reason, evidence ->
                        val againstId = if (currentUser?.id == booking.renterId) booking.ownerId else booking.renterId
                        val againstName = if (currentUser?.id == booking.renterId) booking.ownerName else booking.renterName
                        viewModel.fileDispute(
                            bookingId = booking.id,
                            itemTitle = booking.itemTitle,
                            againstId = againstId,
                            againstName = againstName,
                            reason = reason,
                            description = "Dispute filed for booking #${booking.id} - $reason. Evidence notes: $evidence",
                            evidenceUrl = evidence
                        )
                        fileDisputeForBooking = null
                        showDisputeCenter = true
                    }
                )
            }
        }
    }
    }
}

// ==========================================
// 1. EXPLORE SCREEN (BROWSE ITEMS)
// ==========================================
@Composable
fun ExploreScreen(
    items: List<RentalItem>,
    onItemClick: (RentalItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Advanced search filters state
    var filterLocation by remember { mutableStateOf("") }
    var filterMinPrice by remember { mutableStateOf("") }
    var filterMaxPrice by remember { mutableStateOf("") }
    var filterMinRating by remember { mutableStateOf(0.0f) }
    var filterAvailableOnly by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Popular categories as requested: Electronics, Tools, Vehicles, Party Supplies, Outdoors, Sports, etc.
    val categories = listOf("All", "Electronics", "Tools", "Vehicles", "Party Supplies", "Outdoors", "Sports")

    val filteredItems = items.filter { item ->
        val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
        
        val matchesCategory = selectedCategory == "All" || 
                item.category.equals(selectedCategory, ignoreCase = true) ||
                (selectedCategory == "Party Supplies" && item.category.equals("Party", ignoreCase = true))

        val matchesLocation = filterLocation.isBlank() || item.location.contains(filterLocation, ignoreCase = true)
        
        val minPrice = filterMinPrice.toDoubleOrNull() ?: 0.0
        val maxPrice = filterMaxPrice.toDoubleOrNull() ?: Double.MAX_VALUE
        val matchesPrice = item.pricePerDay >= minPrice && item.pricePerDay <= maxPrice

        val matchesRating = item.ownerRating >= filterMinRating

        val matchesAvailability = !filterAvailableOnly || item.isAvailable

        matchesSearch && matchesCategory && matchesLocation && matchesPrice && matchesRating && matchesAvailability
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Welcome and Headline
        Text(
            text = "Rent local. Rent secure.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Search Bar with Advanced Filters Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tools, electronics, tents...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("explore_search_input"),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            // Filter Button with Badge showing count of active filters
            val activeFiltersCount = (if (filterLocation.isNotBlank()) 1 else 0) +
                    (if (filterMinPrice.isNotBlank() || filterMaxPrice.isNotBlank()) 1 else 0) +
                    (if (filterMinRating > 0.0f) 1 else 0) +
                    (if (filterAvailableOnly) 1 else 0)

            BadgedBox(
                badge = {
                    if (activeFiltersCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(text = activeFiltersCount.toString())
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = CircleShape)
                        .testTag("btn_advanced_filters")
                ) {
                    Icon(imageVector = Icons.Default.FilterList, contentDescription = "Advanced Filters")
                }
            }
        }

        // Advanced Filters Dialog
        if (showFilterDialog) {
            Dialog(onDismissRequest = { showFilterDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advanced Search Filters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showFilterDialog = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        // Location Filter
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Location / Community", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = filterLocation,
                                onValueChange = { filterLocation = it },
                                placeholder = { Text("e.g. Maple Valley, Oakridge") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Price Filter
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Price Range ($ per day)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = filterMinPrice,
                                    onValueChange = { filterMinPrice = it },
                                    label = { Text("Min") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = filterMaxPrice,
                                    onValueChange = { filterMaxPrice = it },
                                    label = { Text("Max") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }

                        // Rating Filter
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Minimum Owner Rating", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0.0f, 3.0f, 4.0f, 4.5f).forEach { rating ->
                                    val isSelected = filterMinRating == rating
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { filterMinRating = rating },
                                        label = {
                                            Text(if (rating == 0.0f) "Any" else "⭐ $rating+")
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Availability Filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Currently Available Only", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = "Hide items that are currently active on rent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = filterAvailableOnly,
                                onCheckedChange = { filterAvailableOnly = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Reset and Apply Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    filterLocation = ""
                                    filterMinPrice = ""
                                    filterMaxPrice = ""
                                    filterMinRating = 0.0f
                                    filterAvailableOnly = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset All")
                            }
                            Button(
                                onClick = { showFilterDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Apply Filters")
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Category Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Surface(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("category_chip_$category")
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Trust Banner / Card (Sleek Escrow)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = TrustPurpleBg
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(TrustPurpleContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Verified Escrow",
                        tint = TrustPurpleText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "SECURE ESCROW PAYMENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TrustPurpleText,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Your funds are held safely until the item is returned.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrustPurpleSubText
                    )
                }
            }
        }

        // Listings List / Grid
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "No items",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "No items listed in this category yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Be the first to rent out something of yours!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { item ->
                    ItemCard(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
fun ItemCard(
    item: RentalItem,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else if (isHovered) 1.03f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "item_scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else if (isHovered) 8.dp else 2.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "item_elevation"
    )
    val borderColor = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(245.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .testTag("item_card_${item.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Category Icon Splash Box (Alternative to loaded images)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = getCategoryIcon(item.category),
                        contentDescription = item.category,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        letterSpacing = 0.5.sp
                    )
                }

                // Star Rating Overlay (from HTML bento template top-left)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = GoldStar,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format("%.1f", item.ownerRating),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Available Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = if (item.isAvailable) SuccessGreen else MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (item.isAvailable) "Available" else "Rented",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Content Description Text
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.ownerName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.ownerIsVerified) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Owner",
                                tint = SuccessGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Daily Rent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$${String.format("%.0f", item.pricePerDay)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "/day",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                    
                    // Bento style action button inside the card bottom right
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(10.dp))
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Rent",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. MY LISTINGS SCREEN (RENT OUT / GIVE ON RENT)
// ==========================================
@Composable
fun MyListingsScreen(
    items: List<RentalItem>,
    incomingRequests: List<Booking>,
    onAddNewListingClick: () -> Unit,
    onApproveRequest: (Long) -> Unit,
    onDeclineRequest: (Long) -> Unit,
    onReviewRenterClick: (Booking) -> Unit,
    onReportDispute: (Booking) -> Unit
) {
    var activeSubTab by remember { mutableStateOf("listings") } // listings or requests

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lend Your Items",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Help your neighbors and earn secure income",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onAddNewListingClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_list_new_item")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("List Item")
            }
        }

        // Sub-Tab Switcher (My active listings, Incoming rental requests)
        TabRow(
            selectedTabIndex = if (activeSubTab == "listings") 0 else 1,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = activeSubTab == "listings",
                onClick = { activeSubTab = "listings" },
                text = { Text("My Active Listings (${items.size})") },
                modifier = Modifier.testTag("tab_active_listings")
            )
            Tab(
                selected = activeSubTab == "requests",
                onClick = { activeSubTab = "requests" },
                text = { 
                    val pendingCount = incomingRequests.count { it.status == "PENDING" }
                    Text(
                        text = "Rent Requests" + if (pendingCount > 0) " ($pendingCount)" else "",
                        fontWeight = if (pendingCount > 0) FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                modifier = Modifier.testTag("tab_incoming_requests")
            )
        }

        if (activeSubTab == "listings") {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "No items",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "You haven't listed any items for rent yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Click 'List Item' above to share your tools or gear!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(item.category),
                                        contentDescription = item.category,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$${String.format("%.0f", item.pricePerDay)}/day",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Deposit: $${String.format("%.0f", item.securityDeposit)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Badge(
                                    containerColor = if (item.isAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = if (item.isAvailable) "Active Available" else "Currently Rented",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Incoming Requests List
            if (incomingRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "No requests",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "No rental requests received yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "When other community users want to rent your items, requests will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(incomingRequests) { request ->
                        IncomingRequestCard(
                            booking = request,
                            onApprove = { onApproveRequest(request.id) },
                            onDecline = { onDeclineRequest(request.id) },
                            onReviewRenter = { onReviewRenterClick(request) },
                            onReportDispute = { onReportDispute(request) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomingRequestCard(
    booking: Booking,
    onApprove: () -> Unit,
    onDecline: () -> Unit,
    onReviewRenter: () -> Unit,
    onReportDispute: () -> Unit
) {
    val durationDays = maxOf(1, (booking.endDate - booking.startDate) / (24 * 60 * 60 * 1000L))
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.itemTitle,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Borrower: ${booking.renterName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (booking.status) {
                        "PENDING" -> MaterialTheme.colorScheme.primaryContainer
                        "ACTIVE" -> MaterialTheme.colorScheme.secondaryContainer
                        "COMPLETED" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = booking.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Renting Period", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$durationDays Days", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Rental Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$${String.format("%.2f", booking.pricePerDay * durationDays)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Security Deposit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$${String.format("%.2f", booking.securityDeposit)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (booking.status == "PENDING") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Decline")
                    }
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve & Pay")
                    }
                }
            } else if (booking.status == "COMPLETED") {
                Button(
                    onClick = onReviewRenter,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Submit Borrower Trust Rating")
                }
            }

            if (booking.status == "ACTIVE" || booking.status == "COMPLETED") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onReportDispute,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report Issue / File Dispute", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. MY RENTALS SCREEN (TAKE ON RENT / MY REQUESTS)
// ==========================================
@Composable
fun MyRentalsScreen(
    bookings: List<Booking>,
    onReturnItem: (Long) -> Unit,
    onLeaveReview: (Booking) -> Unit,
    onReportDispute: (Booking) -> Unit
) {
    var rentalSubTab by remember { mutableStateOf("active") } // active vs past/pending

    val activeRentals = bookings.filter { it.status == "ACTIVE" }
    val otherRentals = bookings.filter { it.status != "ACTIVE" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "My Borrowed Items",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Keep track of active rentals and trust ratings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TabRow(
            selectedTabIndex = if (rentalSubTab == "active") 0 else 1,
            containerColor = Color.Transparent
        ) {
            Tab(
                selected = rentalSubTab == "active",
                onClick = { rentalSubTab = "active" },
                text = { Text("Active Rentals (${activeRentals.size})") },
                modifier = Modifier.testTag("tab_rentals_active")
            )
            Tab(
                selected = rentalSubTab == "history",
                onClick = { rentalSubTab = "history" },
                text = { Text("Past & Pending (${otherRentals.size})") },
                modifier = Modifier.testTag("tab_rentals_history")
            )
        }

        val displayList = if (rentalSubTab == "active") activeRentals else otherRentals

        if (displayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "No rentals",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (rentalSubTab == "active") "No items currently rented." else "No pending requests or past rentals.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Browse the Explore feed to find items to rent locally!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayList) { booking ->
                    RentedItemCard(
                        booking = booking,
                        onReturn = { onReturnItem(booking.id) },
                        onReview = { onLeaveReview(booking) },
                        onReportDispute = { onReportDispute(booking) }
                    )
                }
            }
        }
    }
}

@Composable
fun RentedItemCard(
    booking: Booking,
    onReturn: () -> Unit,
    onReview: () -> Unit,
    onReportDispute: () -> Unit
) {
    val durationDays = maxOf(1, (booking.endDate - booking.startDate) / (24 * 60 * 60 * 1000L))
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = booking.itemTitle,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Owner: ${booking.ownerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Badge with dynamic colors
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (booking.status) {
                        "PENDING" -> MaterialTheme.colorScheme.primaryContainer
                        "ACTIVE" -> MaterialTheme.colorScheme.secondaryContainer
                        "COMPLETED" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(
                        text = when (booking.status) {
                            "PENDING" -> "Waiting Approval"
                            "ACTIVE" -> "Renting Out (In Use)"
                            "COMPLETED" -> "Completed"
                            else -> "Cancelled"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (booking.status) {
                            "PENDING" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "ACTIVE" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "COMPLETED" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Price/Day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$${String.format("%.0f", booking.pricePerDay)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Rented Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$durationDays Days", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Total Charged", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "$${String.format("%.2f", booking.totalCost)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Secure Payment Held Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = when (booking.status) {
                            "PENDING" -> "Payment verified. Funds are held and won't charge unless approved."
                            "ACTIVE" -> "Deposit of $${String.format("%.0f", booking.securityDeposit)} held in Escrow."
                            "COMPLETED" -> "Completed. Security deposit of $${String.format("%.0f", booking.securityDeposit)} safely refunded!"
                            else -> "Cancelled. No funds were charged."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Buttons
            if (booking.status == "ACTIVE") {
                Button(
                    onClick = onReturn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Return Item (Instant Refund Deposit)")
                }
            } else if (booking.status == "COMPLETED") {
                Button(
                    onClick = onReview,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Leave Community Trust Review")
                }
            }

            if (booking.status == "ACTIVE" || booking.status == "COMPLETED") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onReportDispute,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Report Issue / File Dispute", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. PROFILE & SECURE WALLET SCREEN
// ==========================================
@Composable
fun ProfileWalletScreen(
    user: User,
    reviewsReceived: List<Review>,
    onAddFunds: (Double) -> Unit,
    onVerifyProfile: (String, String, String, Boolean, Boolean, Boolean, Boolean) -> Unit,
    onResetVerification: () -> Unit,
    onLoadUserReviews: () -> Unit,
    onOpenDisputeCenter: () -> Unit,
    onLogout: () -> Unit
) {
    var showVerificationForm by remember { mutableStateOf(false) }
    var verificationStep by remember { mutableStateOf(1) } // 1: ID Details, 2: ID Photo, 3: Social Links, 4: Final Processing
    var idType by remember { mutableStateOf("Driver's License") }
    var idNumber by remember { mutableStateOf("") }
    var idPhotoPath by remember { mutableStateOf("") }
    var isScanningPhoto by remember { mutableStateOf(false) }
    var linkedGoogle by remember { mutableStateOf(false) }
    var linkedFacebook by remember { mutableStateOf(false) }
    var linkedTwitter by remember { mutableStateOf(false) }
    var linkedLinkedIn by remember { mutableStateOf(false) }
    var isVerifyingProcessing by remember { mutableStateOf(false) }
    var verificationProgressStep by remember { mutableIntStateOf(0) }
    var showSocialDialogFor by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Trigger loading of user reviews on profile entry
    LaunchedEffect(key1 = user.id) {
        onLoadUserReviews()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Profile Core Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (user.name.isNotEmpty()) user.name.first().uppercase() else "U",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Small floating verified check on avatar
                    if (user.isVerified) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFFBF953F), CircleShape)
                                .align(Alignment.BottomEnd)
                                .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (user.isVerified) Icons.Default.VerifiedUser else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (user.isVerified) Color(0xFFBF953F) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (user.isVerified) "Verified Community Gold Member" else "Unverified Account",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (user.isVerified) Color(0xFFBF953F) else MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⭐ ${String.format("%.1f", user.rating)} (${user.ratingsCount} Ratings)", fontWeight = FontWeight.Bold)
                    Text(text = "📍 ${user.communityName}", style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    text = user.email + "  •  " + user.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .testTag("btn_logout")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Log Out",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Out", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Identity Verification Banner / Form
        if (!user.isVerified) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trust & Identity Verification",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Text(
                        text = "To build trust in the neighborhood, list valuable tools, or hire items instantly without security deposit locks, verify your identity safely in our encrypted system.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!showVerificationForm) {
                        Button(
                            onClick = { showVerificationForm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Start Verification Journey")
                        }
                    } else {
                        // Horizontal Stepper Progress
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Details", "Photo", "Socials", "Review").forEachIndexed { index, stepName ->
                                val stepNum = index + 1
                                val isActive = stepNum == verificationStep
                                val isCompleted = stepNum < verificationStep
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(
                                                color = if (isActive) MaterialTheme.colorScheme.primary 
                                                        else if (isCompleted) Color(0xFF1B5E20)
                                                        else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        } else {
                                            Text(
                                                text = stepNum.toString(),
                                                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = stepName,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) MaterialTheme.colorScheme.primary 
                                                else if (isCompleted) Color(0xFF1B5E20)
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                
                                if (index < 3) {
                                    Spacer(
                                        modifier = Modifier
                                            .height(2.dp)
                                            .weight(0.1f)
                                            .background(
                                                if (isCompleted) Color(0xFF1B5E20) 
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Stepper Content
                        when (verificationStep) {
                            1 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Select Document Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("Driver's License", "Passport", "National ID").forEach { type ->
                                            val selected = idType == type
                                            ElevatedFilterChip(
                                                selected = selected,
                                                onClick = { idType = type },
                                                label = { Text(type) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    OutlinedTextField(
                                        value = idNumber,
                                        onValueChange = { idNumber = it },
                                        label = { Text("Document ID / Registration Number") },
                                        placeholder = { Text("e.g. DL-2947294") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { showVerificationForm = false },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel")
                                        }
                                        Button(
                                            onClick = { verificationStep = 2 },
                                            modifier = Modifier.weight(1f),
                                            enabled = idNumber.trim().isNotBlank()
                                        ) {
                                            Text("Next: ID Photo")
                                        }
                                    }
                                }
                            }
                            2 -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Scan or Upload $idType",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.align(Alignment.Start)
                                    )

                                    if (idPhotoPath.isEmpty() && !isScanningPhoto) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .clickable {
                                                    coroutineScope.launch {
                                                        isScanningPhoto = true
                                                        delay(1800)
                                                        isScanningPhoto = false
                                                        idPhotoPath = "simulated_secure_${idType.lowercase().replace(" ", "_")}_hash.png"
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            border = BorderStroke(1.5.dp, Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Security,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("Tap to simulate Camera Document Scan", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                Text("Supported: JPG, PNG under 10MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    } else if (isScanningPhoto) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().height(140.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                                verticalArrangement = Arrangement.Center,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text("Reading Security Watermarks & MRZ data...", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    } else {
                                        // Display beautifully mock scanned card
                                        val idBrush = if (idType == "Passport") {
                                            Brush.linearGradient(colors = listOf(Color(0xFF141E30), Color(0xFF243B55)))
                                        } else if (idType == "National ID") {
                                            Brush.linearGradient(colors = listOf(Color(0xFF3F2B96), Color(0xFFA8C0FF)))
                                        } else {
                                            Brush.linearGradient(colors = listOf(Color(0xFF1D976C), Color(0xFF93F9B9)))
                                        }

                                        Card(
                                            modifier = Modifier.fillMaxWidth().height(150.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().background(idBrush).padding(12.dp)) {
                                                Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "OFFICIAL DIGITAL ${idType.uppercase()}",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    }
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(54.dp)
                                                                .background(Color.White.copy(alpha = 0.25f), CircleShape)
                                                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = user.name.first().toString().uppercase(),
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Black,
                                                                style = MaterialTheme.typography.titleLarge
                                                            )
                                                        }
                                                        Column {
                                                            Text(text = user.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                            Text(text = "ID: $idNumber", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                                                            Text(text = "HOLOGRAPHIC CHIP: ENGAGED", color = Color(0xFFE8F5E9), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                                        }
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("ISSUED BY NEIGHBOR APP", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                                                        Text("STATUS: SECURED & VERIFIED", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                        
                                        TextButton(
                                            onClick = { idPhotoPath = "" },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Reset and scan again")
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { verificationStep = 1 },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Back")
                                        }
                                        Button(
                                            onClick = { verificationStep = 3 },
                                            modifier = Modifier.weight(1f),
                                            enabled = idPhotoPath.isNotEmpty()
                                        ) {
                                            Text("Next: Socials")
                                        }
                                    }
                                }
                            }
                            3 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Link Social Platforms", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        "Multiply trust credentials by authenticating social handles. Each connected channel grants verified badges and boosts eligibility.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Google Connect Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF4285F4))
                                            Column {
                                                Text("Google Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                if (linkedGoogle) {
                                                    Text("@${user.name.lowercase().replace(" ", "")}_google", color = Color(0xFF1B5E20), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        if (linkedGoogle) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Linked", tint = Color(0xFF1B5E20))
                                        } else {
                                            TextButton(onClick = { showSocialDialogFor = "Google" }) {
                                                Text("Link")
                                            }
                                        }
                                    }

                                    // LinkedIn Connect Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0A66C2))
                                            Column {
                                                Text("LinkedIn Business", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                if (linkedLinkedIn) {
                                                    Text("linkedin.com/in/${user.name.lowercase().replace(" ", "-")}", color = Color(0xFF1B5E20), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        if (linkedLinkedIn) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Linked", tint = Color(0xFF1B5E20))
                                        } else {
                                            TextButton(onClick = { showSocialDialogFor = "LinkedIn" }) {
                                                Text("Link")
                                            }
                                        }
                                    }

                                    // Facebook Connect Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF1877F2))
                                            Column {
                                                Text("Facebook Social", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                if (linkedFacebook) {
                                                    Text("fb.com/${user.name.lowercase().replace(" ", "")}", color = Color(0xFF1B5E20), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        if (linkedFacebook) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Linked", tint = Color(0xFF1B5E20))
                                        } else {
                                            TextButton(onClick = { showSocialDialogFor = "Facebook" }) {
                                                Text("Link")
                                            }
                                        }
                                    }

                                    // Twitter Connect Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF1DA1F2))
                                            Column {
                                                Text("Twitter / X Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                if (linkedTwitter) {
                                                    Text("@${user.name.lowercase().replace(" ", "")}", color = Color(0xFF1B5E20), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                        if (linkedTwitter) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = "Linked", tint = Color(0xFF1B5E20))
                                        } else {
                                            TextButton(onClick = { showSocialDialogFor = "Twitter" }) {
                                                Text("Link")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { verificationStep = 2 },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Back")
                                        }
                                        Button(
                                            onClick = { verificationStep = 4 },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Next: Review")
                                        }
                                    }
                                }
                            }
                            4 -> {
                                if (isVerifyingProcessing) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                        Text(
                                            text = when (verificationProgressStep) {
                                                1 -> "Authenticating Document Holograms & Checksums..."
                                                2 -> "Matching facial landmarks to official database photo..."
                                                3 -> "Authenticating social network connections..."
                                                4 -> "Applying secure community cryptographic seal..."
                                                else -> "Initiating AI Secure Identity Audit..."
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        
                                        // Simple checklist showing live updates
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(
                                                    imageVector = if (verificationProgressStep >= 1) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = if (verificationProgressStep >= 1) Color(0xFF1B5E20) else MaterialTheme.colorScheme.outline
                                                )
                                                Text("Document checksum validation", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(
                                                    imageVector = if (verificationProgressStep >= 2) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = if (verificationProgressStep >= 2) Color(0xFF1B5E20) else MaterialTheme.colorScheme.outline
                                                )
                                                Text("Facial landmark verification check", style = MaterialTheme.typography.bodySmall)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(
                                                    imageVector = if (verificationProgressStep >= 3) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = if (verificationProgressStep >= 3) Color(0xFF1B5E20) else MaterialTheme.colorScheme.outline
                                                )
                                                Text("Social integrity lookup authentication", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Final Verification Review", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("• Document: $idType", style = MaterialTheme.typography.bodySmall)
                                            Text("• Document No: $idNumber", style = MaterialTheme.typography.bodySmall)
                                            Text("• ID Scanned: Secure image signature verified", style = MaterialTheme.typography.bodySmall)
                                            
                                            val listSocials = mutableListOf<String>()
                                            if (linkedGoogle) listSocials.add("Google")
                                            if (linkedLinkedIn) listSocials.add("LinkedIn")
                                            if (linkedFacebook) listSocials.add("Facebook")
                                            if (linkedTwitter) listSocials.add("Twitter/X")
                                            
                                            val socialSummary = if (listSocials.isNotEmpty()) listSocials.joinToString(", ") else "None"
                                            Text("• Linked Channels: $socialSummary", style = MaterialTheme.typography.bodySmall)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = { verificationStep = 3 },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Back")
                                            }
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        isVerifyingProcessing = true
                                                        verificationProgressStep = 1
                                                        delay(1200)
                                                        verificationProgressStep = 2
                                                        delay(1200)
                                                        verificationProgressStep = 3
                                                        delay(1200)
                                                        verificationProgressStep = 4
                                                        delay(1000)
                                                        isVerifyingProcessing = false
                                                        onVerifyProfile(idType, idNumber, idPhotoPath, linkedGoogle, linkedFacebook, linkedTwitter, linkedLinkedIn)
                                                        showVerificationForm = false
                                                        verificationStep = 1
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Confirm & Finalize")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // 2. Verified Community Trust Passport View
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, Color(0xFFBF953F)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFBF953F).copy(alpha = 0.08f),
                                    Color(0xFFFCF6BA).copy(alpha = 0.05f),
                                    Color(0xFFB38728).copy(alpha = 0.08f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified Gold Checkmark",
                                    tint = Color(0xFFBF953F),
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "COMMUNITY TRUST PASSPORT",
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFAA771C)
                                    )
                                    Text(
                                        text = "Verified on " + java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(if (user.verificationDate > 0L) user.verificationDate else System.currentTimeMillis())),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFBF953F), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "A+ TRUST",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFBF953F).copy(alpha = 0.2f))

                        Text(
                            text = "Your identity is certified secure. This passport unlocks immediate zero-deposit renting, unlimited tool hosting, and direct peer-to-peer trust-shield coverage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Display details
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Verified Credential", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${user.idType} (${user.idNumber.take(3)}*****${user.idNumber.takeLast(2)})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                            
                            val platformsList = mutableListOf<String>()
                            if (user.linkedGoogle) platformsList.add("Google")
                            if (user.linkedLinkedIn) platformsList.add("LinkedIn")
                            if (user.linkedFacebook) platformsList.add("Facebook")
                            if (user.linkedTwitter) platformsList.add("Twitter/X")
                            
                            if (platformsList.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Authenticated Channels", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(platformsList.joinToString(" • "), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = Color(0xFF1B5E20))
                                }
                            }
                        }

                        // Developer/Tester Tools - Reset Verification Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onResetVerification,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset Verification status (Dev Demo)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        // Render Mock Social Dialog
        showSocialDialogFor?.let { platform ->
            Dialog(onDismissRequest = { showSocialDialogFor = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val platformColor = when (platform) {
                            "Google" -> Color(0xFF4285F4)
                            "Facebook" -> Color(0xFF1877F2)
                            "Twitter" -> Color(0xFF1DA1F2)
                            "LinkedIn" -> Color(0xFF0A66C2)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        
                        Icon(
                            imageVector = when (platform) {
                                "Google" -> Icons.Default.AccountCircle
                                "LinkedIn" -> Icons.Default.Lock
                                else -> Icons.Default.AccountCircle
                            },
                            contentDescription = null,
                            tint = platformColor,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Text(
                            text = "Connect with $platform",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "RentalPay will securely connect with $platform to verify your identity. This will confirm your public name, verified email, and account status, adding high-trust points to your community profile.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        var isConnectingSocial by remember { mutableStateOf(false) }
                        val socialScope = rememberCoroutineScope()
                        
                        if (isConnectingSocial) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = platformColor, modifier = Modifier.size(24.dp))
                                Text("Establishing secure connection...", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { showSocialDialogFor = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = {
                                        socialScope.launch {
                                            isConnectingSocial = true
                                            delay(1200)
                                            isConnectingSocial = false
                                            when (platform) {
                                                "Google" -> linkedGoogle = true
                                                "Facebook" -> linkedFacebook = true
                                                "Twitter" -> linkedTwitter = true
                                                "LinkedIn" -> linkedLinkedIn = true
                                            }
                                            showSocialDialogFor = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = platformColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Authorize")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Secure Wallet Segment
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Secure Rental Pay Wallet",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Used for immediate rental charges & deposit escrow locks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Payment Escrow",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Available Wallet Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format("%.2f", user.walletBalance)}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Add Simulated Funds buttons
                Text(
                    text = "Refill Simulated Funds",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50.0, 100.0, 250.0).forEach { amount ->
                        Button(
                            onClick = { onAddFunds(amount) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_add_funds_$amount"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("+$${amount.toInt()}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3.5. Dispute Resolution Center Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
            modifier = Modifier
                .clickable { onOpenDisputeCenter() }
                .testTag("btn_dispute_center")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(32.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dispute Resolution Center",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Report damaged items, delayed returns, or view active claims and community mediations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // 4. Community Reviews / Trust Log
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "My Trust Logs & Reviews",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            if (reviewsReceived.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No community trust reviews received yet. Build ratings by borrowing and lending items with your local neighbors!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                reviewsReceived.forEach { review ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = review.reviewerName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "⭐ ".repeat(review.rating),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                text = review.comment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. ITEM DETAIL MODAL BOTTOM SHEET
// ==========================================
@Composable
fun ItemDetailBottomSheet(
    item: RentalItem,
    reviews: List<Review>,
    currentUser: User?,
    onDismiss: () -> Unit,
    onRentClick: (Int) -> Unit,
    onGoToWallet: () -> Unit,
    onChatClick: (Long, String) -> Unit
) {
    var rentalDays by remember { mutableIntStateOf(1) }

    val daysPrice = item.pricePerDay * rentalDays
    val securityDeposit = item.securityDeposit
    val localServiceFee = Math.round((daysPrice * 0.05) * 100.0) / 100.0
    val communityTrustFee = 1.50
    val totalCost = daysPrice + securityDeposit + localServiceFee + communityTrustFee

    val canAfford = currentUser != null && currentUser.walletBalance >= totalCost

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header (Category + Title + Dismiss)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Title
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                // Owner Trust Badge
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.ownerName.first().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = item.ownerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                if (item.ownerIsVerified) {
                                    Icon(Icons.Default.Verified, contentDescription = "Verified Owner", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(text = "Rating: ⭐ ${String.format("%.1f", item.ownerRating)}  •  📍 ${item.location}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (item.ownerId != currentUser?.id) {
                            OutlinedButton(
                                onClick = { onChatClick(item.ownerId, item.ownerName) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_chat_with_owner")
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Chat", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Description Block
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Item Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Specifications (Condition, Local Neighborhood)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Condition", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = item.condition, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Community Location", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = item.location, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // 2. Booking Picker Row
                if (item.isAvailable && item.ownerId != currentUser?.id) {
                    Divider()
                    Text(text = "Set Rental Period", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Number of Days:", style = MaterialTheme.typography.bodyMedium)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedIconButton(
                                onClick = { if (rentalDays > 1) rentalDays-- },
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = "$rentalDays Days",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedIconButton(
                                onClick = { if (rentalDays < 14) rentalDays++ },
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }

                    // Dynamic Price Estimator Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Dynamic Cost Estimator",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$rentalDays days select",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

                            // Base Rental
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Base Rental ($${String.format("%.0f", item.pricePerDay)} × $rentalDays days)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(text = "$${String.format("%.2f", daysPrice)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }

                            // Refundable Security Deposit
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF4CAF50))
                                    Text(text = "Security Deposit (Refundable)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE8F5E9),
                                        modifier = Modifier.padding(end = 2.dp)
                                    ) {
                                        Text(
                                            text = "Escrow held",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Text(text = "$${String.format("%.2f", securityDeposit)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Local Service Fee
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Local Service Fee (5%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(text = "$${String.format("%.2f", localServiceFee)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }

                            // Community Trust Flat Fee
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Community Trust Support Fee", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(text = "$${String.format("%.2f", communityTrustFee)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }

                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), thickness = 1.dp)

                            // Total
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Total Secure Escrow Lock", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(text = "Fully escrow protected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                                Text(
                                    text = "$${String.format("%.2f", totalCost)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Check afford and rent action
                    if (canAfford) {
                        Button(
                            onClick = { onRentClick(rentalDays) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_request_rental"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Request Rental & Freeze Payment")
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Insufficient Funds: You need $${String.format("%.2f", totalCost)} but only have $${String.format("%.2f", currentUser?.walletBalance ?: 0.0)}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Button(
                                onClick = onGoToWallet,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Wallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Go to Wallet to Deposit Cash")
                            }
                        }
                    }
                } else if (item.ownerId == currentUser?.id) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "This is your listing. Manage incoming requests from borrowers directly on the 'Rent Out' tab.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (!item.isAvailable) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "This item is currently active on rent with a neighbor and is temporarily unavailable.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Reviews Section
                Divider()
                Text(text = "Item & Owner Community Reviews", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                if (reviews.isEmpty()) {
                    Text(
                        text = "No reviews for this item yet. Be the first to rate it after renting!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    reviews.forEach { r ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = r.reviewerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(text = "⭐ ".repeat(r.rating), fontSize = 10.sp)
                                }
                                Text(text = r.comment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. ADD LISTING DIALOG
// ==========================================
@Composable
fun AddListingDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tools") }
    var price by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Excellent") }

    val categories = listOf("Tools", "Electronics", "Outdoors", "Sports", "Party")
    val conditions = listOf("New", "Like New", "Excellent", "Good", "Fair")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Lend an Item",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What are you renting out?") },
                    placeholder = { Text("e.g. Cordless Drill, Karaoke Machine") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Instructions") },
                    placeholder = { Text("Describe details, accessories included, and condition info...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                // Category selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Category", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                // Condition selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Condition", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(conditions) { cond ->
                            FilterChip(
                                selected = condition == cond,
                                onClick = { condition = cond },
                                label = { Text(cond) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Rent / Day ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = deposit,
                        onValueChange = { deposit = it },
                        label = { Text("Security Deposit ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val priceVal = price.toDoubleOrNull() ?: 0.0
                            val depositVal = deposit.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && priceVal > 0.0) {
                                onConfirm(title, description, category, priceVal, depositVal, condition)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = title.isNotBlank() && price.isNotBlank()
                    ) {
                        Text("List Item Now")
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. TRUST RATING / REVIEW DIALOG
// ==========================================
@Composable
fun TrustReviewDialog(
    title: String,
    subtitle: String,
    onSubmit: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Stars rating selection
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    for (star in 1..5) {
                        IconButton(
                            onClick = { rating = star },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                                contentDescription = "$star Stars",
                                tint = if (star <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment") },
                    placeholder = { Text("Share your honest experience (returns, care, communication)...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Skip")
                    }
                    Button(
                        onClick = { onSubmit(rating, comment) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = comment.isNotBlank()
                    ) {
                        Text("Submit Review")
                    }
                }
            }
        }
    }
}

// Helper: Maps categories to beautiful Vector Icons
fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "tools" -> Icons.Default.Build
        "electronics" -> Icons.Default.Tv
        "outdoors" -> Icons.Default.Terrain
        "sports" -> Icons.Default.SportsBasketball
        "party" -> Icons.Default.Celebration
        else -> Icons.Default.Category
    }
}

@Composable
fun InboxScreen(
    messages: List<ChatMessage>,
    currentUser: User,
    activeChatUserId: Long?,
    activeChatUserName: String?,
    onSendMessage: (Long, String) -> Unit,
    onBackToInbox: () -> Unit,
    onChatSelect: (Long, String) -> Unit
) {
    AnimatedContent(
        targetState = activeChatUserId,
        transitionSpec = {
            if (targetState != null) {
                // Chat details sliding in from the right, inbox sliding out to the left
                slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
            } else {
                // Chat details sliding out to the right, inbox sliding in from the left
                slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            }
        },
        label = "ChatDrawerTransition",
        modifier = Modifier.fillMaxSize()
    ) { targetChatUserId ->
        if (targetChatUserId != null) {
            // Chat Details Screen
            val conversationMessages = messages.filter {
                (it.senderId == currentUser.id && it.receiverId == targetChatUserId) ||
                (it.senderId == targetChatUserId && it.receiverId == currentUser.id)
            }.sortedBy { it.timestamp }

            var messageText by remember { mutableStateOf("") }
            val listState = rememberLazyListState()

            // Auto-scroll to bottom when new messages arrive
            LaunchedEffect(key1 = conversationMessages.size) {
                if (conversationMessages.isNotEmpty()) {
                    listState.animateScrollToItem(conversationMessages.size - 1)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header
                Surface(
                    tonalElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = onBackToInbox) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (activeChatUserName ?: "User").first().uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeChatUserName ?: "Local Neighbor",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF4CAF50), shape = CircleShape)
                                )
                                Text(
                                    text = "Verified Member",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Message list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(conversationMessages) { msg ->
                        val isMe = msg.senderId == currentUser.id
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            Surface(
                                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 2.dp,
                                    bottomEnd = if (isMe) 2.dp else 16.dp
                                ),
                                tonalElevation = 1.dp,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.messageText,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Status indicator or timestamp
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                            ) {
                                Text(
                                    text = "Just now",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                if (isMe) {
                                    Icon(
                                        imageVector = if (msg.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                        contentDescription = if (msg.isRead) "Read" else "Sent",
                                        tint = if (msg.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom bar with Input
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Write a message...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_chat_message"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            trailingIcon = {
                                if (messageText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            onSendMessage(targetChatUserId, messageText)
                                            messageText = ""
                                        },
                                        modifier = Modifier.testTag("btn_send_chat_message")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Inbox List of Conversations
            val grouped = messages.groupBy {
                if (it.senderId == currentUser.id) it.receiverId else it.senderId
            }

            val knownNames = mapOf(
                1L to "Alice (Tools Lender)",
                2L to "Bob (Drill Renter)",
                3L to "Marcus (Verified Power-User)",
                4L to "Dave (Party Gear)",
                5L to "Me"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Local Messages",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Coordinate rentals and build neighborly trust",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (grouped.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "No chats",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Your inbox is empty",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Browse listings and chat with landlords/borrowers to start renting!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(grouped.keys.toList()) { partnerId ->
                            val conversation = grouped[partnerId] ?: emptyList()
                            val lastMsg = conversation.maxByOrNull { it.timestamp }
                            val partnerName = knownNames[partnerId] ?: "Neighbor #$partnerId"
                            val unreadCount = conversation.count { !it.isRead && it.receiverId == currentUser.id }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChatSelect(partnerId, partnerName)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = partnerName.first().uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = partnerName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "Recent",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = lastMsg?.messageText ?: "No messages yet",
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(MaterialTheme.colorScheme.error, shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = unreadCount.toString(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisputeCenterDialog(
    disputes: List<Dispute>,
    onDismiss: () -> Unit,
    onResolveDispute: (Long, String) -> Unit
) {
    var selectedDispute by remember { mutableStateOf<Dispute?>(null) }
    var resolutionText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (selectedDispute != null) "Mediation Details" else "Community Dispute Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = {
                        if (selectedDispute != null) {
                            selectedDispute = null
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(
                            imageVector = if (selectedDispute != null) Icons.Default.ArrowBack else Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                if (selectedDispute == null) {
                    // Dispute List Mode
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Neutral Escrow & Mediation Tracker",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "We hold security deposits in secure Escrow during active disputes. View reports filed by you or other members below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (disputes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp))
                                    Text("Zero Active Disputes!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text("All local community transactions are fully verified and trusted.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(disputes) { dispute ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedDispute = dispute }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Claim ID: #${dispute.id}",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = when (dispute.status) {
                                                        "OPEN" -> MaterialTheme.colorScheme.errorContainer
                                                        "MEDIATING" -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> Color(0xFFE8F5E9)
                                                    }
                                                ) {
                                                    Text(
                                                        text = dispute.status,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (dispute.status) {
                                                            "OPEN" -> MaterialTheme.colorScheme.error
                                                            "MEDIATING" -> MaterialTheme.colorScheme.primary
                                                            else -> Color(0xFF2E7D32)
                                                        }
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "Issue: ${dispute.reason}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (dispute.outcome.isNotBlank()) {
                                                Text(
                                                    text = "Outcome: ${dispute.outcome}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Dispute Details Mode
                    val claim = selectedDispute!!
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Claim Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "Booking Reference: #${claim.bookingId}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Reporter ID: #${claim.reporterId}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Status: ${claim.status}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Reason Filed:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text(text = claim.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Text(
                            text = "Evidence & Logs",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FilePresent, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(text = "Submitted Evidence Path", style = MaterialTheme.typography.labelSmall)
                                    Text(text = claim.evidenceUrl, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        if (claim.status != "RESOLVED") {
                            Divider()
                            Text(
                                text = "Neutral Administrator Resolution Panel",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Propose a resolution outcome to resolve this dispute and release escrow funds. Common options: Refund 100% deposit to borrower, or pay landlord 100% compensation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = resolutionText,
                                onValueChange = { resolutionText = it },
                                label = { Text("Resolution Statement") },
                                placeholder = { Text("e.g., Refunded 100% of deposit to renter as item was reported broken on delivery.") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 4
                            )

                            Button(
                                onClick = {
                                    onResolveDispute(claim.id, resolutionText)
                                    selectedDispute = claim.copy(status = "RESOLVED", outcome = resolutionText)
                                    resolutionText = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = resolutionText.isNotBlank()
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enforce & Resolve Mediation")
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                        Text(text = "Dispute Resolved Successfully", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Mediated Outcome: ${claim.outcome}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileDisputeDialog(
    booking: Booking,
    currentUser: User,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var evidencePath by remember { mutableStateOf("") }
    
    val reasons = listOf(
        "Item arrived damaged / inoperable",
        "Renter delayed item return",
        "Wrong item provided / missing parts",
        "Host did not cooperate with handover"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            text = "Report Rental Issue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Provide details and photographic evidence below. Our support mediators will audit the Escrow funds freeze and contact both parties.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Booking card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "Item: ${booking.itemTitle}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Rented From: ${booking.ownerName}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Total Secure Escrow: $${String.format("%.2f", booking.totalCost)}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Reasons selection
                Text(
                    text = "Select Dispute Category",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    reasons.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reason = cat }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = reason == cat,
                                onClick = { reason = cat },
                                modifier = Modifier.testTag("radio_dispute_$cat")
                            )
                            Text(text = cat, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Evidence notes
                OutlinedTextField(
                    value = evidencePath,
                    onValueChange = { evidencePath = it },
                    label = { Text("Evidence Notes / Photographic Proof Path") },
                    placeholder = { Text("e.g., photo_evidence_scratches.png - Renter did not clean the lawnmower bag.") },
                    modifier = Modifier.fillMaxWidth().testTag("input_dispute_evidence"),
                    maxLines = 4
                )

                // Submit / Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onConfirm(reason, evidencePath) },
                        modifier = Modifier.weight(1.5f).testTag("btn_submit_dispute"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        enabled = reason.isNotBlank() && evidencePath.isNotBlank()
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Freeze & Submit")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    allUsers: List<User>,
    onLoginUser: (User) -> Unit,
    onRegisterAndLogin: (String, String, String, String) -> Unit
) {
    var isSignInMode by remember { mutableStateOf(true) }
    
    // Create Account form fields
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regCommunity by remember { mutableStateOf("Oakridge Estates") }

    // Custom sign-in with typed email
    var signInEmail by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Logo & Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Rental Pay Shield Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Rental Pay",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Secure Peer-to-Peer Neighborhood Rentals",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Mode Selector (Tab Control)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { isSignInMode = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSignInMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                        contentColor = if (isSignInMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("login_tab_signin"),
                    shape = RoundedCornerShape(8.dp),
                    elevation = if (isSignInMode) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation()
                ) {
                    Text("Sign In", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isSignInMode = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isSignInMode) MaterialTheme.colorScheme.surface else Color.Transparent,
                        contentColor = if (!isSignInMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("login_tab_register"),
                    shape = RoundedCornerShape(8.dp),
                    elevation = if (!isSignInMode) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else ButtonDefaults.buttonElevation()
                ) {
                    Text("New Account", fontWeight = FontWeight.Bold)
                }
            }

            if (isSignInMode) {
                // Sign In Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Choose an account to start:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Render existing users beautifully
                    if (allUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            allUsers.forEach { user ->
                                Card(
                                    onClick = { onLoginUser(user) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("user_card_${user.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (user.name.isNotEmpty()) user.name.first().uppercase() else "U",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = user.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (user.isVerified) {
                                                    Icon(
                                                        imageVector = Icons.Default.Verified,
                                                        contentDescription = "Verified",
                                                        tint = Color(0xFFBF953F),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "📍 ${user.communityName}  •  ⭐ ${user.rating}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Sign In",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Or sign in using email:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    OutlinedTextField(
                        value = signInEmail,
                        onValueChange = { signInEmail = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("email@example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signin_email_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            val matched = allUsers.find { it.email.equals(signInEmail.trim(), ignoreCase = true) }
                            if (matched != null) {
                                onLoginUser(matched)
                            } else if (signInEmail.isNotBlank()) {
                                onRegisterAndLogin(
                                    signInEmail.substringBefore("@").replace(".", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                    signInEmail.trim(),
                                    "555-0100",
                                    "Oakridge Estates"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signin_email_button"),
                        shape = RoundedCornerShape(10.dp),
                        enabled = signInEmail.isNotBlank()
                    ) {
                        Text("Sign In with Email")
                    }
                }
            } else {
                // Register Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Create a new profile:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    OutlinedTextField(
                        value = regName,
                        onValueChange = { regName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("John Doe") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_name_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("john@example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_email_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = regPhone,
                        onValueChange = { regPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("555-0199") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_phone_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = regCommunity,
                        onValueChange = { regCommunity = it },
                        label = { Text("Neighborhood / Community") },
                        placeholder = { Text("Oakridge Estates") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_community_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = { onRegisterAndLogin(regName, regEmail, regPhone, regCommunity) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_submit_button"),
                        shape = RoundedCornerShape(10.dp),
                        enabled = regName.isNotBlank() && regEmail.isNotBlank()
                    ) {
                        Text("Register & Enter Marketplace")
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

