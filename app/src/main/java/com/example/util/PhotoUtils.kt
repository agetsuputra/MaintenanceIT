package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MockPhoto(val title: String, val url: String, val category: String)

val MOCK_PHOTOS = listOf(
    MockPhoto("Kerusakan LCD", "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=400", "Layar"),
    MockPhoto("Mainboard Berdebu", "https://images.unsplash.com/photo-1544256718-3bcf237f3974?w=400", "Hub/PC"),
    MockPhoto("Pembersihan Switch", "https://images.unsplash.com/photo-1512486130939-2c4f79935e4f?w=400", "Jaringan"),
    MockPhoto("Uji Printer Test Page", "https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6?w=400", "Printer"),
    MockPhoto("Instalasi Bersih OS", "https://images.unsplash.com/photo-1531403009284-440f080d1e12?w=400", "Sistem"),
    MockPhoto("Kabel Manajemen Rapi", "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=400", "Server")
)

fun parseWatermarkedPhoto(photoStr: String?, defaultLocation: String = "IT Office"): Triple<String, String, String> {
    if (photoStr.isNullOrBlank()) return Triple("", "", "")
    val parts = photoStr.split("|||")
    return if (parts.size >= 3) {
        Triple(parts[0], parts[1], parts[2])
    } else {
        val dateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        Triple(parts[0], defaultLocation, dateString)
    }
}
