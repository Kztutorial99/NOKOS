package com.example.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

object BrandIconHelper {

    fun getDrawableRes(serviceId: String, serviceName: String): Int? {
        val id = serviceId.lowercase().trim()
        val name = serviceName.lowercase().trim()

        return when {
            id == "wa" || name.contains("whatsapp") -> R.drawable.ic_brand_whatsapp
            id == "tg" || name.contains("telegram") -> R.drawable.ic_brand_telegram
            id == "go" || name.contains("google") || name.contains("gmail") -> R.drawable.ic_brand_google
            id == "tt" || name.contains("tiktok") -> R.drawable.ic_brand_tiktok
            id == "sp" || name.contains("shopee") -> R.drawable.ic_brand_shopee
            id == "ig" || name.contains("instagram") -> R.drawable.ic_brand_instagram
            id == "oa" || name.contains("openai") || name.contains("chatgpt") || name.contains("gpt") -> R.drawable.ic_brand_openai
            id == "gj" || name.contains("gojek") -> R.drawable.ic_brand_gojek
            id == "dn" || name.contains("dana") -> R.drawable.ic_brand_dana
            id == "tw" || id == "x" || name.contains("twitter") || name == "x" -> R.drawable.ic_brand_twitter_x
            id == "ds" || name.contains("discord") -> R.drawable.ic_brand_discord
            id == "gb" || name.contains("grab") -> R.drawable.ic_brand_grab
            else -> null
        }
    }
}

@Composable
fun BrandIcon(
    serviceId: String,
    serviceName: String,
    fallbackEmoji: String = "💬",
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val resId = BrandIconHelper.getDrawableRes(serviceId, serviceName)
    if (resId != null) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = serviceName,
            modifier = modifier.size(size)
        )
    } else {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(text = fallbackEmoji, fontSize = (size.value * 0.75f).sp)
        }
    }
}
