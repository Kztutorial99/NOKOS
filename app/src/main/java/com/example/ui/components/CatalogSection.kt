package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CountryItem
import com.example.model.ServiceProduct
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardHover
import com.example.ui.theme.DarkCardStroke
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BrandIcon

@Composable
fun CatalogSection(
    countries: List<CountryItem>,
    selectedCountry: CountryItem?,
    catalogServices: List<ServiceProduct>,
    searchQuery: String,
    isOrdering: Boolean,
    onSelectCountry: (CountryItem) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOrderService: (ServiceProduct) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCountryMenu by remember { mutableStateOf(false) }
    var serviceToConfirm by remember { mutableStateOf<ServiceProduct?>(null) }

    val filteredServices = remember(catalogServices, searchQuery) {
        if (searchQuery.isBlank()) {
            catalogServices
        } else {
            catalogServices.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Title & Country Picker Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Beli Nomor Virtual (Nokos)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Pilih layanan populer untuk verifikasi OTP otomatis",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            // Country Selector Button
            Box {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkCard,
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { showCountryMenu = true }
                        .testTag("country_picker_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCountry?.flagEmoji ?: "🌐",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = selectedCountry?.name ?: "Pilih Negara",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Pilih Negara",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showCountryMenu,
                    onDismissRequest = { showCountryMenu = false },
                    modifier = Modifier.background(DarkCard)
                ) {
                    countries.forEach { country ->
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
                                showCountryMenu = false
                            },
                            trailingIcon = if (country.id == selectedCountry?.id) {
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
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Cari layanan (WhatsApp, Telegram, Google...)", color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari",
                    tint = CyberCyan
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_service_input"),
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

        Spacer(modifier = Modifier.height(14.dp))

        // Horizontal Quick-Picks Carousel
        Text(
            text = "Layanan Terpopuler",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 6.dp)
        ) {
            items(filteredServices) { service ->
                PopularServiceChip(
                    service = service,
                    onClick = { serviceToConfirm = service }
                )
            }
        }
    }

    // Confirmation Order Dialog
    if (serviceToConfirm != null) {
        val s = serviceToConfirm!!
        val country = selectedCountry ?: countries.firstOrNull()
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
                        text = "Konfirmasi Beli Nomor",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Apakah Anda ingin memesan nomor virtual baru untuk:",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = DarkCardHover,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DarkCardStroke),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Layanan: ${s.name}",
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                            Text(
                                text = "Negara: ${country?.name ?: "Indonesia"} (${country?.phonePrefix ?: "+62"})",
                                color = TextPrimary
                            )
                            Text(
                                text = "Biaya Sewa: $${s.price} USD",
                                fontWeight = FontWeight.Bold,
                                color = CyberEmerald
                            )
                            Text(
                                text = "Masa Aktif: 15 Menit (Otomatis Refund jika SMS tidak sampai)",
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
                    modifier = Modifier.testTag("confirm_order_dialog_btn")
                ) {
                    if (isOrdering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = DarkCard,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Beli Sekarang ($${s.price})", fontWeight = FontWeight.Bold)
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
fun PopularServiceChip(
    service: ServiceProduct,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
            .testTag("service_chip_${service.id}"),
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
                BrandIcon(
                    serviceId = service.id,
                    serviceName = service.name,
                    fallbackEmoji = service.iconEmoji,
                    size = 26.dp
                )
                Text(
                    text = "$${service.price}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberEmerald
                )
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
                text = "Tersedia: ${service.stockCount}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
