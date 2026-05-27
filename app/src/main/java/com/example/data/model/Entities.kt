package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey val inventoryNumber: String,
    val name: String,
    val type: String,
    val location: String,
    val status: String, // e.g. "Aktif", "Rusak", "Hold", "Dalam Pengerjaan"
    val description: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val acquisitionDate: Long = System.currentTimeMillis(),
    val purchasePrice: Double? = null
)

@Entity(tableName = "repairs")
data class Repair(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val inventoryNumber: String,
    val startTime: Long,
    val endTime: Long?,
    val problem: String,
    val cause: String,
    val actionTaken: String,
    val status: String, // "Dalam Pengerjaan", "Hold", or "Selesai"
    val holdReason: String?,
    val holdEstimate: String?,
    val photoBefore: String?, // Watermarked path/description
    val photoAfter: String?,  // Watermarked path/description
    val photoUser: String?,   // Watermarked path/description of handover with user
    val technician: String
)

@Entity(tableName = "maintenances")
data class Maintenance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val inventoryNumber: String,
    val startTime: Long,
    val endTime: Long?,
    val actionTaken: String,
    val issuesFound: String,
    val result: String,
    val status: String, // "Dalam Pengerjaan" or "Selesai", now also "Hold"
    val holdReason: String? = null,
    val holdEstimate: String? = null,
    val photoBefore: String?, // Watermarked
    val photoAfter: String?,  // Watermarked
    val photoUser: String?,   // Watermarked handover
    val technician: String
)

@Entity(tableName = "asset_updates")
data class AssetUpdateLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val inventoryNumber: String,
    val updateTime: Long,
    val oldLocation: String,
    val newLocation: String,
    val oldStatus: String,
    val newStatus: String,
    val oldDescription: String?,
    val newDescription: String?,
    val reasonForPermanentDamage: String? = null
)

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String, // lowercase, unique identifier
    val name: String,
    val pin: String,
    val role: String, // "Kepala Unit IT" or "Staff IT"
    val isBiometricEnabled: Boolean = false
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val name: String, // unique category name
    val guidelines: String // Dynamic items joined by "||~||"
)

