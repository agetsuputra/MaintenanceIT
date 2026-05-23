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
    val createdAt: Long = System.currentTimeMillis()
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
    val status: String, // "Dalam Pengerjaan" or "Selesai"
    val photoBefore: String?, // Watermarked
    val photoAfter: String?,  // Watermarked
    val photoUser: String?,   // Watermarked handover
    val technician: String
)
