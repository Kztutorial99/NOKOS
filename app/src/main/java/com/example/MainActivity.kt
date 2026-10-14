package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.NokosStatus
import com.example.ui.components.ActiveOrderCard
import com.example.ui.components.CatalogSection
import com.example.ui.components.MainDashboardScreen
import com.example.ui.components.NokosHeader
import com.example.ui.components.SettingsScreen
import com.example.ui.components.VaultScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardHover
import com.example.ui.theme.DarkCardStroke
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.NokosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                NokosApp()
            }
        }
    }
}

enum class MainTab {
    DASHBOARD,
    ACTIVE_ORDERS,
    VAULT,
    SETTINGS
}

@Composable
fun NokosApp(viewModel: NokosViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val onCopyOtpWithSnackbar: (String) -> Unit = { otpCode ->
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = "Kode OTP $otpCode berhasil disalin ke clipboard! 📋",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
        }
    }

    // Intercept back button to return to Dashboard tab
    BackHandler(enabled = currentTab != MainTab.DASHBOARD) {
        currentTab = MainTab.DASHBOARD
    }

    // Show toast / notification snackbars
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val waitingCount = remember(uiState.activeOrders) {
        uiState.activeOrders.count { it.status == NokosStatus.WAITING_SMS || it.status == NokosStatus.RECEIVED_OTP }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DarkCardHover,
                    contentColor = TextPrimary,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberEmerald),
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkCard,
                contentColor = TextSecondary,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // Tab 1: Dashboard
                NavigationBarItem(
                    selected = currentTab == MainTab.DASHBOARD,
                    onClick = { currentTab = MainTab.DASHBOARD },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Beranda", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBg,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // Tab 2: Active Orders / Inbox
                NavigationBarItem(
                    selected = currentTab == MainTab.ACTIVE_ORDERS,
                    onClick = { currentTab = MainTab.ACTIVE_ORDERS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (waitingCount > 0) {
                                    Badge(
                                        containerColor = CyberEmerald,
                                        contentColor = DarkBg
                                    ) {
                                        Text("$waitingCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentTab == MainTab.ACTIVE_ORDERS) Icons.Filled.Inbox else Icons.Outlined.Inbox,
                                contentDescription = "Pesanan Aktif"
                            )
                        }
                    },
                    label = { Text("Nomor Aktif", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBg,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_orders")
                )

                // Tab 3: Encrypted Vault
                NavigationBarItem(
                    selected = currentTab == MainTab.VAULT,
                    onClick = { currentTab = MainTab.VAULT },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.VAULT) Icons.Filled.Lock else Icons.Outlined.Lock,
                            contentDescription = "Vault Enkripsi"
                        )
                    },
                    label = { Text("Vault E2E", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBg,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_vault")
                )

                // Tab 4: Settings
                NavigationBarItem(
                    selected = currentTab == MainTab.SETTINGS,
                    onClick = { currentTab = MainTab.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Pengaturan"
                        )
                    },
                    label = { Text("Pengaturan", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DarkBg,
                        selectedTextColor = CyberCyan,
                        indicatorColor = CyberCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar
            NokosHeader(
                balance = uiState.balance,
                currency = uiState.currency,
                isLoadingBalance = uiState.isLoadingBalance,
                isDemoMode = uiState.isDemoMode,
                isPolling = uiState.isPolling,
                onRefreshBalance = { viewModel.refreshBalance() },
                onToggleDemoMode = { viewModel.setDemoMode(!uiState.isDemoMode) }
            )

            // Main Content Area based on selected Tab
            when (currentTab) {
                MainTab.DASHBOARD -> {
                    MainDashboardScreen(
                        uiState = uiState,
                        onSelectCountry = { viewModel.selectCountry(it) },
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onOrderService = { viewModel.orderNumber(it) },
                        onCancelOrder = { viewModel.cancelOrder(it) },
                        onFinishOrder = { viewModel.finishOrder(it) },
                        onSimulateSms = { viewModel.simulateIncomingSms(it) },
                        onDismissOrder = { viewModel.removeActiveOrder(it) },
                        onRefreshBalance = { viewModel.refreshBalance() }
                    )
                }

                MainTab.ACTIVE_ORDERS -> {
                    ActiveOrdersScreen(
                        orders = uiState.activeOrders,
                        onCancelOrder = { viewModel.cancelOrder(it) },
                        onFinishOrder = { viewModel.finishOrder(it) },
                        onSimulateSms = { viewModel.simulateIncomingSms(it) },
                        onDismissOrder = { viewModel.removeActiveOrder(it) },
                        onOrderNewClick = { currentTab = MainTab.DASHBOARD }
                    )
                }

                MainTab.VAULT -> {
                    VaultScreen(
                        records = uiState.decryptedVaultRecords,
                        keyFingerprint = uiState.keyFingerprint,
                        isVaultUnlocked = uiState.isVaultUnlocked,
                        hasVaultPin = uiState.hasVaultPin,
                        onUnlockVault = { viewModel.unlockVault(it) },
                        onLockVault = { viewModel.lockVault() },
                        onSetVaultPin = { viewModel.setVaultPin(it) },
                        onDeleteRecord = { viewModel.deleteVaultRecord(it) },
                        onShredAllData = { viewModel.shredAllData() }
                    )
                }

                MainTab.SETTINGS -> {
                    SettingsScreen(
                        maskedToken = uiState.maskedToken,
                        isDemoMode = uiState.isDemoMode,
                        hapticEnabled = uiState.hapticEnabled,
                        keyFingerprint = uiState.keyFingerprint,
                        onSaveToken = { viewModel.saveApiToken(it) },
                        onTestToken = { token, callback -> viewModel.testTokenConnection(token, callback) },
                        onToggleDemoMode = { viewModel.setDemoMode(it) },
                        onToggleHaptic = { viewModel.setHapticEnabled(it) }
                    )
                }
            }
        }
    }
}



@Composable
fun ActiveOrdersScreen(
    orders: List<com.example.model.ActiveNokos>,
    onCancelOrder: (String) -> Unit,
    onFinishOrder: (String) -> Unit,
    onSimulateSms: (String) -> Unit,
    onDismissOrder: (String) -> Unit,
    onOrderNewClick: () -> Unit
) {
    var selectedFilter by remember { mutableIntStateOf(0) }
    val filterLabels = listOf("Semua", "Menunggu SMS", "OTP Diterima", "Selesai / Batal")

    val filteredOrders = remember(orders, selectedFilter) {
        when (selectedFilter) {
            1 -> orders.filter { it.status == NokosStatus.WAITING_SMS }
            2 -> orders.filter { it.status == NokosStatus.RECEIVED_OTP }
            3 -> orders.filter { it.status == NokosStatus.COMPLETED || it.status == NokosStatus.CANCELED || it.status == NokosStatus.EXPIRED }
            else -> orders
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Daftar Nomor Aktif",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Pantau OTP yang sedang berjalan secara real-time",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Button(
                onClick = onOrderNewClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = DarkBg
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("order_new_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Beli Nomor", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterLabels.forEachIndexed { index, label ->
                FilterChip(
                    selected = selectedFilter == index,
                    onClick = { selectedFilter = index },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan,
                        selectedLabelColor = DarkBg,
                        containerColor = DarkCard,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selectedFilter == index) CyberCyan else DarkCardStroke
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredOrders.isEmpty()) {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DarkCardStroke),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak ada nomor pada filter ini",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pesan nomor virtual untuk menerima SMS OTP secara otomatis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onOrderNewClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = DarkBg
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Pesan Nomor Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    ActiveOrderCard(
                        order = order,
                        onCancel = { onCancelOrder(order.id) },
                        onFinish = { onFinishOrder(order.id) },
                        onSimulateSms = { onSimulateSms(order.id) },
                        onDismiss = { onDismissOrder(order.id) }
                    )
                }
            }
        }
    }
}
