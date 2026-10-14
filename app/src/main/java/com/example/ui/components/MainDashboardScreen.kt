package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveNokos
import com.example.model.CountryItem
import com.example.model.NokosStatus
import com.example.model.ServiceProduct
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardHover
import com.example.ui.theme.DarkCardStroke
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BrandIcon
import com.example.viewmodel.NokosUiState

@Composable
fun MainDashboardScreen(
    uiState: NokosUiState,
    onSelectCountry: (CountryItem) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOrderService: (ServiceProduct) -> Unit,
    onCancelOrder: (String) -> Unit,
    onFinishOrder: (String) -> Unit,
    onSimulateSms: (String) -> Unit,
    onDismissOrder: (String) -> Unit,
    onRefreshBalance: () -> Unit,
    onCopyOtp: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("Semua") }
    var showCountryDropdown by remember { mutableStateOf(false) }
    var serviceToConfirm by remember { mutableStateOf<ServiceProduct?>(null) }

    val categories = listOf("Semua", "Messaging", "Social", "E-Commerce", "Fintech", "Transport", "AI & Tech")

    // Filtered services
    val filteredServices = remember(uiState.catalogServices, uiState.searchQuery, selectedCategory) {
        uiState.catalogServices.filter { service ->
            val matchesSearch = uiState.searchQuery.isBlank() ||
                    service.name.contains(uiState.searchQuery, ignoreCase = true) ||
                    service.category.contains(uiState.searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "Semua" ||
                    service.category.equals(selectedCategory, ignoreCase = true) ||
                    (selectedCategory == "Messaging" && (service.id == "wa" || service.id == "tg")) ||
                    (selectedCategory == "Social" && (service.id == "tt" || service.id == "ig" || service.id == "tw")) ||
                    (selectedCategory == "E-Commerce" && service.id == "sp") ||
                    (selectedCategory == "Fintech" && service.id == "dn") ||
                    (selectedCategory == "Transport" && (service.id == "gj" || service.id == "gb")) ||
                    (selectedCategory == "AI & Tech" && (service.id == "oa" || service.id == "ds"))
            matchesSearch && matchesCategory
        }
    }

    // Split orders into received OTPs and pending waiting orders
    val receivedOtpOrders = remember(uiState.activeOrders) {
        uiState.activeOrders.filter { it.status == NokosStatus.RECEIVED_OTP }
    }
    val waitingOrders = remember(uiState.activeOrders) {
        uiState.activeOrders.filter { it.status == NokosStatus.WAITING_SMS }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Live Telemetry & Quick Metric Cards
        item {
            TelemetryDashboardCards(
                waitingCount = waitingOrders.size,
                receivedCount = receivedOtpOrders.size,
                balance = uiState.balance,
                currency = uiState.currency,
                isPolling = uiState.isPolling,
                onRefreshBalance = onRefreshBalance
            )
        }

        // 2. Real-Time Received OTP Messages (PROMINENT REALTIME LIST)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberEmerald.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                tint = CyberEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Pesan OTP Diterima (Real-Time)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Kode verifikasi diekstrak otomatis & diamankan AES-256",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                if (receivedOtpOrders.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = CyberEmerald
                    ) {
                        Text(
                            text = "${receivedOtpOrders.size} Baru",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = DarkCard,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (receivedOtpOrders.isNotEmpty()) {
            items(receivedOtpOrders, key = { it.id }) { order ->
                ReceivedOtpCard(
                    order = order,
                    onCopyOtp = onCopyOtp,
                    onFinish = { onFinishOrder(order.id) },
                    onDelete = { onDismissOrder(order.id) }
                )
            }
        } else {
            // Empty state for received OTPs with quick simulated test
            item {
                Surface(
                    color = DarkCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DarkCardStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isPolling) CyberCyan else CyberEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Menunggu SMS Masuk...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Saat ada SMS verifikasi tiba di nomor virtual Anda, kode OTP akan muncul seketika di panel ini dengan tombol salin otomatis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        // If user has waiting orders, offer quick simulate button
                        if (waitingOrders.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val firstWaiting = waitingOrders.first()
                            Button(
                                onClick = { onSimulateSms(firstWaiting.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = DarkCard
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("quick_simulate_otp_btn")
                            ) {
                                Text("⚡ Kirim Uji SMS ke ${firstWaiting.serviceName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Active Pending Orders (Waiting for SMS)
        if (waitingOrders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nomor Aktif Menunggu SMS (${waitingOrders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            items(waitingOrders, key = { it.id }) { order ->
                ActiveOrderCard(
                    order = order,
                    onCancel = { onCancelOrder(order.id) },
                    onFinish = { onFinishOrder(order.id) },
                    onSimulateSms = { onSimulateSms(order.id) },
                    onDismiss = { onDismissOrder(order.id) },
                    onCopyOtp = onCopyOtp
                )
            }
        }

        // 4. Available SMS Services Section (CATALOG & SELECTION)
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section Title + Country Selector Dropdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Layanan SMS Tersedia",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pilih platform untuk nomor virtual baru",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    // Country Dropdown Button
                    Box {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkCard,
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clickable { showCountryDropdown = true }
                                .testTag("country_select_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.selectedCountry?.flagEmoji ?: "🇮🇩",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = uiState.selectedCountry?.name ?: "Indonesia",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Pilih Negara",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showCountryDropdown,
                            onDismissRequest = { showCountryDropdown = false },
                            modifier = Modifier.background(DarkCard)
                        ) {
                            uiState.countries.forEach { country ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = country.flagEmoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${country.name} (${country.phonePrefix})",
                                                color = TextPrimary
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectCountry(country)
                                        showCountryDropdown = false
                                    },
                                    trailingIcon = if (country.id == uiState.selectedCountry?.id) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Terpilih",
                                                tint = CyberCyan
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Cari layanan (WhatsApp, Telegram, Google...)", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardStroke,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan,
                                selectedLabelColor = DarkCard,
                                containerColor = DarkCard,
                                labelColor = TextSecondary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selectedCategory == cat) CyberCyan else DarkCardStroke
                            )
                        )
                    }
                }
            }
        }

        // 5. Service Grid Cards
        items(filteredServices.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (service in pair) {
                    ServiceGridItem(
                        service = service,
                        countryPrefix = uiState.selectedCountry?.phonePrefix ?: "+62",
                        onClick = { serviceToConfirm = service },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    // Order Confirmation Dialog
    if (serviceToConfirm != null) {
        val s = serviceToConfirm!!
        val country = uiState.selectedCountry ?: uiState.countries.firstOrNull()
        AlertDialog(
            onDismissRequest = { serviceToConfirm = null },
            containerColor = DarkCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandIcon(
                        serviceId = s.id,
                        serviceName = s.name,
                        fallbackEmoji = s.iconEmoji,
                        size = 28.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Konfirmasi Pesanan",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Pesan nomor virtual baru untuk verifikasi:",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = DarkCardHover,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkCardStroke),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = s.name,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                                Text(
                                    text = "$${s.price} USD",
                                    fontWeight = FontWeight.Bold,
                                    color = CyberEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Negara: ${country?.name ?: "Indonesia"} (${country?.phonePrefix ?: "+62"})",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Masa Sewa: 15 Menit • Garansi 100% Refund Otomatis",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val svc = s
                        serviceToConfirm = null
                        onOrderService(svc)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DarkCard
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("submit_order_dialog_btn")
                ) {
                    if (uiState.isOrdering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = DarkCard,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Beli ($${s.price})", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { serviceToConfirm = null },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun TelemetryDashboardCards(
    waitingCount: Int,
    receivedCount: Int,
    balance: Double,
    currency: String,
    isPolling: Boolean,
    onRefreshBalance: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: OTP Diterima
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OTP MASUK",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = CyberEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        tint = CyberEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$receivedCount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Terekam di Vault",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        // Card 2: Menunggu SMS (Waiting)
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (waitingCount > 0) CyberAmber else DarkCardStroke)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MENUNGGU SMS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (waitingCount > 0) CyberAmber else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = if (waitingCount > 0) CyberAmber else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$waitingCount",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = if (isPolling) "Polling aktif 4s" else "Siaga",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isPolling) CyberCyan else TextMuted
                )
            }
        }
    }
}

@Composable
fun ServiceGridItem(
    service: ServiceProduct,
    countryPrefix: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("service_grid_item_${service.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkCardStroke)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkCardHover,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BrandIcon(
                            serviceId = service.id,
                            serviceName = service.name,
                            fallbackEmoji = service.iconEmoji,
                            size = 24.dp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CyberEmerald.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$${service.price}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberEmerald,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = service.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )

            Text(
                text = "${service.category} • Stok: ${service.stockCount}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan.copy(alpha = 0.15f),
                    contentColor = CyberCyan
                ),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            ) {
                Text(
                    text = "Pesan Nomor",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
