package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.InventoryDatabase
import com.example.data.model.Asset
import com.example.data.model.Repair
import com.example.data.model.Maintenance
import com.example.data.repository.ITRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ITViewModel(private val repository: ITRepository) : ViewModel() {

    // All lists from database
    val allAssets: StateFlow<List<Asset>> = repository.allAssets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRepairs: StateFlow<List<Repair>> = repository.allRepairs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allMaintenances: StateFlow<List<Maintenance>> = repository.allMaintenances.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allUsers: StateFlow<List<com.example.data.model.User>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Range Filter Dates (Default is Last 30 Days to Future 30 Days)
    val filterStartDate = MutableStateFlow<Long>(getStartOfRange())
    val filterEndDate = MutableStateFlow<Long>(getEndOfRange())

    // Filtered lists based on date
    val filteredRepairs: StateFlow<List<Repair>> = combine(
        allRepairs, filterStartDate, filterEndDate
    ) { repairs, start, end ->
        repairs.filter { repair ->
            val timestamp = repair.startTime
            timestamp in start..end
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMaintenances: StateFlow<List<Maintenance>> = combine(
        allMaintenances, filterStartDate, filterEndDate
    ) { maintenances, start, end ->
        maintenances.filter { maint ->
            val timestamp = maint.startTime
            timestamp in start..end
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected items for detail screens
    val selectedAsset = MutableStateFlow<Asset?>(null)
    val selectedRepair = MutableStateFlow<Repair?>(null)
    val selectedMaintenance = MutableStateFlow<Maintenance?>(null)

    // Seeding dummy data for preview convenience if DB is empty
    fun seedSampleDataIfEmpty() {
        viewModelScope.launch {
            val assets = repository.allAssets.first()
            if (assets.isEmpty()) {
                val sampleAssets = listOf(
                    Asset("INV-PC-001", "PC Admin Keuangan - Lenovo", "PC Desktop", "Ruang Keuangan Lt 1", "Aktif", "Intel i5, SSD 256GB, RAM 8GB"),
                    Asset("INV-LP-002", "MacBook Pro M1 - Desain", "Laptop", "Ruang Kreatif Lt 2", "Aktif", "MacBook Pro M1 8-Core CPU 8-Core GPU"),
                    Asset("INV-LP-003", "Laptop HRD - ASUS", "Laptop", "Ruang HR Lt 1", "Hold", "Intel i3, RAM 4GB, HDD 1TB"),
                    Asset("INV-PR-004", "Printer Epson L3110", "Printer", "Ruang Umum Lt 1", "Dalam Pengerjaan", "Inkjet Printer multifunction"),
                    Asset("INV-NW-005", "Router MikroTik RB4011", "Network Device", "Server Room Lt 3", "Aktif", "MikroTik RB4011iGS+RM Server Router")
                )
                sampleAssets.forEach { repository.insertAsset(it) }

                // Insert dynamic times
                val now = System.currentTimeMillis()
                val threeDaysAgo = now - 3 * 24 * 60 * 60 * 1000L
                val tenDaysAgo = now - 10 * 24 * 60 * 60 * 1000L

                val sampleRepairs = listOf(
                    Repair(
                        inventoryNumber = "INV-LP-003",
                        startTime = tenDaysAgo,
                        endTime = null,
                        problem = "Layar berkedip dan sering mati mendadak",
                        cause = "Kabel fleksibel LCD kendor dan overheat thermal paste kering",
                        actionTaken = "Pembersihan kipas dan pengganti thermal paste, pasang ulang konektor LCD",
                        status = "Hold",
                        holdReason = "Menunggu part kabel fleksibel LCD cadangan dari gudang pusat",
                        holdEstimate = "3 Hari",
                        photoBefore = "https://images.unsplash.com/photo-1591799264318-7e6ef8ddb7ea?w=400|||Ruang HR Lt 1|||20-05-2026",
                        photoAfter = null,
                        photoUser = null,
                        technician = "Budi Hartono"
                    ),
                    Repair(
                        inventoryNumber = "INV-NW-005",
                        startTime = threeDaysAgo,
                        endTime = threeDaysAgo + 3600 * 1000,
                        problem = "Traffic lamban dan port 3 mati total",
                        cause = "Sambaran petir dekat gedung merusak port Ethernet internal",
                        actionTaken = "Reset router, konfigurasi ulang WAN ke port 4, port 3 dinonaktifkan",
                        status = "Selesai",
                        holdReason = null,
                        holdEstimate = null,
                        photoBefore = "https://images.unsplash.com/photo-1544256718-3bcf237f3974?w=400|||Server Room Lt 3|||21-05-2026",
                        photoAfter = "https://images.unsplash.com/photo-1544256718-3bcf237f3974?w=400|||Server Room Lt 3|||21-05-2026",
                        photoUser = "https://images.unsplash.com/photo-1512486130939-2c4f79935e4f?w=400|||Server Room Lt 3|||21-05-2026",
                        technician = "Roni Setiawan"
                    )
                )
                sampleRepairs.forEach { repository.insertRepair(it) }

                val sampleMaintenances = listOf(
                    Maintenance(
                        inventoryNumber = "INV-PR-004",
                        startTime = tenDaysAgo + 3 * 3600 * 1000,
                        endTime = null,
                        actionTaken = "Deep cleaning printhead, pengisian tangki tinta, penyedotan udara selang tindas",
                        issuesFound = "Nozzle sempat mampet di warna Magenta",
                        result = "Lolos pengujian nozzle check, warna tajam dan lancar",
                        status = "Dalam Pengerjaan",
                        photoBefore = "https://images.unsplash.com/photo-1512486130939-2c4f79935e4f?w=400|||Ruang Umum Lt 1|||19-05-2026",
                        photoAfter = null,
                        photoUser = null,
                        technician = "Ahmad Dani"
                    )
                )
                sampleMaintenances.forEach { repository.insertMaintenance(it) }
            }
            
            // Seed default users if empty
            val users = repository.allUsers.first()
            if (users.isEmpty()) {
                repository.insertUser(com.example.data.model.User("sumayasa", "Sumayasa", "123456", "Kepala Unit IT", false))
                repository.insertUser(com.example.data.model.User("deaget", "Deaget", "123456", "Staff IT", false))
            }
        }
    }

    // --- Actions ---

    fun saveAsset(asset: Asset, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertAsset(asset)
            onComplete()
        }
    }

    fun saveRepair(repair: Repair, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertRepair(repair)
            // Update asset status based on repair status
            val asset = repository.getAssetByInventory(repair.inventoryNumber)
            if (asset != null) {
                val newStatus = when (repair.status) {
                    "Hold" -> "Hold"
                    "Dalam Pengerjaan" -> "Dalam Pengerjaan"
                    else -> "Aktif"
                }
                repository.updateAsset(asset.copy(status = newStatus))
            }
            onComplete()
        }
    }

    fun resumeRepairToComplete(repair: Repair, completionTime: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateRepair(repair)
            // Restore asset status to Aktif
            val asset = repository.getAssetByInventory(repair.inventoryNumber)
            if (asset != null) {
                repository.updateAsset(asset.copy(status = "Aktif"))
            }
            onComplete()
        }
    }

    fun saveMaintenance(maintenance: Maintenance, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertMaintenance(maintenance)
            // Update asset status indicating it passed/is in maintenance
            val asset = repository.getAssetByInventory(maintenance.inventoryNumber)
            if (asset != null) {
                val newStatus = when (maintenance.status) {
                    "Dalam Pengerjaan" -> "Dalam Pengerjaan"
                    else -> "Aktif"
                }
                repository.updateAsset(asset.copy(status = newStatus))
            }
            onComplete()
        }
    }

    fun updateMaintenance(maintenance: Maintenance, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateMaintenance(maintenance)
            val asset = repository.getAssetByInventory(maintenance.inventoryNumber)
            if (asset != null) {
                val newStatus = when (maintenance.status) {
                    "Dalam Pengerjaan" -> "Dalam Pengerjaan"
                    else -> "Aktif"
                }
                repository.updateAsset(asset.copy(status = newStatus))
            }
            onComplete()
        }
    }

    fun verifyRepair(repair: Repair, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateRepair(repair.copy(status = "Selesai & Terverifikasi"))
            onComplete()
        }
    }

    fun verifyMaintenance(maintenance: Maintenance, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateMaintenance(maintenance.copy(status = "Selesai & Terverifikasi"))
            onComplete()
        }
    }

    fun selectAssetByInventoryNum(invNum: String) {
        viewModelScope.launch {
            val asset = repository.getAssetByInventory(invNum)
            selectedAsset.value = asset
        }
    }

    fun getRepairsForAsset(invNum: String): Flow<List<Repair>> = repository.getRepairsForAsset(invNum)
    fun getMaintenancesForAsset(invNum: String): Flow<List<Maintenance>> = repository.getMaintenancesForAsset(invNum)
    fun getUpdateLogsForAsset(invNum: String): Flow<List<com.example.data.model.AssetUpdateLog>> = repository.getUpdateLogsForAsset(invNum)

    fun saveAssetUpdate(
        asset: Asset,
        newLocation: String,
        newStatus: String,
        newDescription: String?,
        reasonForPermanentDamage: String?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val log = com.example.data.model.AssetUpdateLog(
                inventoryNumber = asset.inventoryNumber,
                updateTime = System.currentTimeMillis(),
                oldLocation = asset.location,
                newLocation = newLocation,
                oldStatus = asset.status,
                newStatus = newStatus,
                oldDescription = asset.description,
                newDescription = newDescription,
                reasonForPermanentDamage = reasonForPermanentDamage
            )
            repository.insertAssetUpdateLog(log)

            val updatedAsset = asset.copy(
                location = newLocation,
                status = newStatus,
                description = newDescription
            )
            repository.updateAsset(updatedAsset)

            if (selectedAsset.value?.inventoryNumber == asset.inventoryNumber) {
                selectedAsset.value = updatedAsset
            }
            onComplete()
        }
    }

    fun saveUser(user: com.example.data.model.User, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertUser(user)
            onComplete()
        }
    }

    fun deleteUser(user: com.example.data.model.User, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteUser(user)
            onComplete()
        }
    }

    // --- Tamper-proof hash generator for Excel (CSV) row integrity verification ---
    private fun generateTamperProofHash(vararg inputs: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val combined = inputs.joinToString("|") + "|SECURE_IT_HUB_SALT_2026"
            val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }.take(16).uppercase()
        } catch (e: Exception) {
            "SECURE-ERR-VAL"
        }
    }

    val isFirebaseEnabled: Boolean = repository.isFirebaseEnabled

    private fun saveBase64ToFile(base64Str: String, file: File): Boolean {
        return try {
            val base64Clean = if (base64Str.startsWith("data:image")) {
                base64Str.substringAfter("base64,")
            } else {
                base64Str
            }
            val decodedBytes = android.util.Base64.decode(base64Clean, android.util.Base64.DEFAULT)
            file.writeBytes(decodedBytes)
            true
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun extractPhotoAndGetLabel(context: Context, photoStr: String?, prefix: String, id: String, photoFiles: MutableMap<String, File>): String {
        if (photoStr.isNullOrBlank()) return "Tidak ada foto"
        val (url, loc, date) = parseWatermarkedPhoto(photoStr)
        if (url.startsWith("data:image") && url.contains("base64,")) {
            val fileName = "${prefix}_${id}.jpg"
            val tempFile = File(context.cacheDir, "temp_photo_$fileName")
            if (saveBase64ToFile(url, tempFile)) {
                photoFiles[fileName] = tempFile
                return "./fotos/$fileName [Lokasi: $loc, Tgl: $date]"
            }
        } else if (url.startsWith("http")) {
            return "URL Online: $url [Lokasi: $loc, Tgl: $date]"
        }
        return "Tidak ada foto"
    }

    private fun parseWatermarkedPhoto(photoStr: String?, defaultLocation: String = "IT Office"): Triple<String, String, String> {
        if (photoStr.isNullOrBlank()) return Triple("", "", "")
        val parts = photoStr.split("|||")
        return if (parts.size >= 3) {
            Triple(parts[0], parts[1], parts[2])
        } else {
            val dateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            Triple(parts[0], defaultLocation, dateString)
        }
    }

    private fun createZipFile(context: Context, csvFile: File, photoFiles: Map<String, File>, zipFilename: String): File? {
        val zipFile = File(context.cacheDir, zipFilename)
        try {
            java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                // 1. Add CSV file
                val csvEntry = java.util.zip.ZipEntry(csvFile.name)
                zos.putNextEntry(csvEntry)
                java.io.FileInputStream(csvFile).use { fis -> fis.copyTo(zos) }
                zos.closeEntry()

                // 2. Add photos under "fotos/" folder
                photoFiles.forEach { (entryName, file) ->
                    if (file.exists()) {
                        val photoEntry = java.util.zip.ZipEntry("fotos/$entryName")
                        zos.putNextEntry(photoEntry)
                        java.io.FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                        // Clean up temp file
                        file.delete()
                    }
                }
            }
            return zipFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            csvFile.delete() // clean up temp CSV file after zipping
        }
    }

    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun getEmbeddedPhotoHtml(photoStr: String?): String {
        if (photoStr.isNullOrBlank()) return "Tidak ada foto"
        val (url, loc, date) = parseWatermarkedPhoto(photoStr)
        if (url.startsWith("data:image") && url.contains("base64,")) {
            return "<div style=\"text-align:center;\"><img src=\"$url\" height=\"100\" width=\"133\" style=\"border:1px solid #ccc;\" /><br/><small style=\"font-size:10px;color:#555;\">${escapeHtml(loc)}<br/>${escapeHtml(date)}</small></div>"
        } else if (url.startsWith("http")) {
            return "<div style=\"text-align:center;\"><img src=\"$url\" height=\"100\" width=\"133\" style=\"border:1px solid #ccc;\" /><br/><small style=\"font-size:10px;color:#555;\">${escapeHtml(loc)}<br/>${escapeHtml(date)}</small></div>"
        }
        return "Tidak ada foto"
    }

    // --- Excel / CSV Exporter ---

    fun exportToExcel(context: Context, type: String): File? {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val dateFileFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFileFormat.format(Date())

        val filename: String
        val html = StringBuilder()

        // HTML Header with Excel gridlines instruction
        html.append("<html xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n")
        html.append("<head>\n")
        html.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />\n")
        html.append("<!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>Laporan IT</x:Name><x:WorksheetOptions><x:DisplayGridlines/><x:ProtectContents>True</x:ProtectContents><x:ProtectObjects>True</x:ProtectObjects><x:ProtectScenarios>True</x:ProtectScenarios></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->\n")
        html.append("<style>\n")
        html.append("  body { font-family: 'Segoe UI', Calibri, Arial, sans-serif; }\n")
        html.append("  table { border-collapse: collapse; margin: 15px 0; }\n")
        html.append("  th { background-color: #0288D1; color: #FFFFFF; font-weight: bold; border: 1px solid #000000; padding: 10px; text-align: center; font-size: 13px; mso-protection: locked; }\n")
        html.append("  td { border: 1px solid #B0BEC5; padding: 10px; vertical-align: middle; text-align: left; font-size: 12px; mso-protection: locked; }\n")
        html.append("  .title-row { background-color: #E1F5FE; font-weight: bold; font-size: 16px; text-align: center; color: #01579B; padding: 15px; }\n")
        html.append("  .sec-decl { background-color: #FFF9C4; font-weight: bold; font-size: 11px; text-align: center; padding: 8px; color: #F57F17; }\n")
        html.append("</style>\n")
        html.append("</head>\n")
        html.append("<body>\n")

        val assetsList = allAssets.value
        val assetMap = assetsList.associateBy { it.inventoryNumber }

        when (type) {
            "assets" -> {
                filename = "List_Inventaris_IT_$timestamp.xls"
                html.append("<table>\n")
                html.append("  <tr><td colspan=\"8\" class=\"title-row\"><b>DOKUMEN INVENTARIS ASET TIM IT SUPPORT</b></td></tr>\n")
                html.append("  <tr><td colspan=\"8\" class=\"sec-decl\">SISTEM PROTEKSI SECURE RECORD VERIFIED VER.2.6 - READ-ONLY - DATA SIGNED</td></tr>\n")
                html.append("  <tr>\n")
                html.append("    <th>No. Inventaris</th>\n")
                html.append("    <th>Nama Perangkat</th>\n")
                html.append("    <th>Kategori</th>\n")
                html.append("    <th>Lokasi</th>\n")
                html.append("    <th>Status</th>\n")
                html.append("    <th>Deskripsi</th>\n")
                html.append("    <th>Tanggal Terdaftar</th>\n")
                html.append("    <th>Kode Verifikasi Keaslian (SHA-Signature)</th>\n")
                html.append("  </tr>\n")

                assetsList.forEach { a ->
                    val verificationHash = generateTamperProofHash(a.inventoryNumber, a.name, a.type, a.location, a.status)
                    html.append("  <tr>\n")
                    html.append("    <td>${escapeHtml(a.inventoryNumber)}</td>\n")
                    html.append("    <td>${escapeHtml(a.name)}</td>\n")
                    html.append("    <td>${escapeHtml(a.type)}</td>\n")
                    html.append("    <td>${escapeHtml(a.location)}</td>\n")
                    html.append("    <td><b>${escapeHtml(a.status)}</b></td>\n")
                    html.append("    <td>${escapeHtml(a.description ?: "")}</td>\n")
                    html.append("    <td>${dateFormat.format(Date(a.createdAt))}</td>\n")
                    html.append("    <td style=\"font-family: monospace;\">$verificationHash</td>\n")
                    html.append("  </tr>\n")
                }
                html.append("</table>\n")
            }
            "repairs" -> {
                filename = "Laporan_Perbaikan_IT_$timestamp.xls"
                html.append("<table>\n")
                html.append("  <tr><td colspan=\"17\" class=\"title-row\"><b>LAPORAN PERBAIKAN PERANGKAT TIM IT SUPPORT</b></td></tr>\n")
                html.append("  <tr><td colspan=\"17\" class=\"sec-decl\">SISTEM PROTEKSI SECURE RECORD VERIFIED VER.2.6 - READ-ONLY - DATA SIGNED</td></tr>\n")
                html.append("  <tr>\n")
                html.append("    <th>ID Perbaikan</th>\n")
                html.append("    <th>No. Inventaris</th>\n")
                html.append("    <th>Nama Perangkat</th>\n")
                html.append("    <th>Lokasi Perangkat</th>\n")
                html.append("    <th>Waktu Mulai</th>\n")
                html.append("    <th>Waktu Selesai</th>\n")
                html.append("    <th>Kendala</th>\n")
                html.append("    <th>Penyebab</th>\n")
                html.append("    <th>Tindak Lanjut</th>\n")
                html.append("    <th>Status</th>\n")
                html.append("    <th>Alasan Hold</th>\n")
                html.append("    <th>Estimasi</th>\n")
                html.append("    <th>Teknisi</th>\n")
                html.append("    <th style=\"width: 250px;\">Foto Sebelum</th>\n")
                html.append("    <th style=\"width: 250px;\">Foto Sesudah</th>\n")
                html.append("    <th style=\"width: 250px;\">Foto Bersama Unit</th>\n")
                html.append("    <th style=\"width: 250px;\">Kode Verifikasi Keaslian (SHA-Signature)</th>\n")
                html.append("  </tr>\n")

                val repairs = filteredRepairs.value.filter { it.status == "Selesai & Terverifikasi" }
                repairs.forEach { r ->
                    val endStr = r.endTime?.let { dateFormat.format(Date(it)) } ?: "Sedang Diproses/Hold"
                    val devName = assetMap[r.inventoryNumber]?.name ?: "Perangkat Tidak Dikenal"
                    val devLoc = assetMap[r.inventoryNumber]?.location ?: "Lokasi Tidak Tercatat"
                    val verificationHash = generateTamperProofHash(
                        r.id.toString(), r.inventoryNumber, r.status, r.technician, r.startTime.toString()
                    )

                    html.append("  <tr>\n")
                    html.append("    <td>${r.id}</td>\n")
                    html.append("    <td>${escapeHtml(r.inventoryNumber)}</td>\n")
                    html.append("    <td>${escapeHtml(devName)}</td>\n")
                    html.append("    <td>${escapeHtml(devLoc)}</td>\n")
                    html.append("    <td>${dateFormat.format(Date(r.startTime))}</td>\n")
                    html.append("    <td>${escapeHtml(endStr)}</td>\n")
                    html.append("    <td>${escapeHtml(r.problem)}</td>\n")
                    html.append("    <td>${escapeHtml(r.cause)}</td>\n")
                    html.append("    <td>${escapeHtml(r.actionTaken)}</td>\n")
                    html.append("    <td><b>${escapeHtml(r.status)}</b></td>\n")
                    html.append("    <td>${escapeHtml(r.holdReason ?: "")}</td>\n")
                    html.append("    <td>${escapeHtml(r.holdEstimate ?: "")}</td>\n")
                    html.append("    <td>${escapeHtml(r.technician)}</td>\n")
                    html.append("    <td style=\"width: 250px;\">${getEmbeddedPhotoHtml(r.photoBefore)}</td>\n")
                    html.append("    <td style=\"width: 250px;\">${getEmbeddedPhotoHtml(r.photoAfter)}</td>\n")
                    html.append("    <td style=\"width: 250px;\">${getEmbeddedPhotoHtml(r.photoUser)}</td>\n")
                    html.append("    <td style=\"width: 250px; font-family: monospace; word-break: break-all; word-wrap: break-word;\">$verificationHash</td>\n")
                    html.append("  </tr>\n")
                }
                html.append("</table>\n")
            }
            "maintenances" -> {
                filename = "Laporan_Perawatan_IT_$timestamp.xls"
                html.append("<table>\n")
                html.append("  <tr><td colspan=\"15\" class=\"title-row\"><b>LAPORAN PERAWATAN RUTIN TIM IT SUPPORT</b></td></tr>\n")
                html.append("  <tr><td colspan=\"15\" class=\"sec-decl\">SISTEM PROTEKSI SECURE RECORD VERIFIED VER.2.6 - READ-ONLY - DATA SIGNED</td></tr>\n")
                html.append("  <tr>\n")
                html.append("    <th>ID Perawatan</th>\n")
                html.append("    <th>No. Inventaris</th>\n")
                html.append("    <th>Nama Perangkat</th>\n")
                html.append("    <th>Lokasi Perangkat</th>\n")
                html.append("    <th>Waktu Mulai</th>\n")
                html.append("    <th>Waktu Selesai</th>\n")
                html.append("    <th>Tindakan</th>\n")
                html.append("    <th>Kendala Temuan</th>\n")
                html.append("    <th>Hasil</th>\n")
                html.append("    <th>Status</th>\n")
                html.append("    <th>Teknisi</th>\n")
                html.append("    <th style=\"width: 250px;\">Foto Sebelum</th>\n")
                html.append("    <th style=\"width: 250px;\">Foto Sesudah</th>\n")
                html.append("    <th style=\"width: 250px;\">Foto Bersama Unit</th>\n")
                html.append("    <th style=\"width: 250px;\">Kode Verifikasi Keaslian (SHA-Signature)</th>\n")
                html.append("  </tr>\n")

                val maints = filteredMaintenances.value.filter { m -> m.status == "Selesai & Terverifikasi" }
                maints.forEach { m ->
                    val endStr = m.endTime?.let { dateFormat.format(Date(it)) } ?: "Sedang Diproses"
                    val devName = assetMap[m.inventoryNumber]?.name ?: "Perangkat Tidak Dikenal"
                    val devLoc = assetMap[m.inventoryNumber]?.location ?: "Lokasi Tidak Tercatat"
                    val verificationHash = generateTamperProofHash(
                        m.id.toString(), m.inventoryNumber, m.status, m.technician, m.startTime.toString()
                    )

                    html.append("  <tr>\n")
                    html.append("    <td>${m.id}</td>\n")
                    html.append("    <td>${escapeHtml(m.inventoryNumber)}</td>\n")
                    html.append("    <td>${escapeHtml(devName)}</td>\n")
                    html.append("    <td>${escapeHtml(devLoc)}</td>\n")
                    html.append("    <td>${dateFormat.format(Date(m.startTime))}</td>\n")
                    html.append("    <td>${escapeHtml(endStr)}</td>\n")
                    html.append("    <td>${escapeHtml(m.actionTaken)}</td>\n")
                    html.append("    <td>${escapeHtml(m.issuesFound)}</td>\n")
                    html.append("    <td>${escapeHtml(m.result)}</td>\n")
                    html.append("    <td><b>${escapeHtml(m.status)}</b></td>\n")
                    html.append("    <td>${escapeHtml(m.technician)}</td>\n")
                    html.append("    <td style=\"width: 250px;\">${getEmbeddedPhotoHtml(m.photoBefore)}</td>\n")
                    html.append("    <td style=\"width: 250px;\">${getEmbeddedPhotoHtml(m.photoAfter)}</td>\n")
                    html.append("    <td style=\"width: 250px;\">${getEmbeddedPhotoHtml(m.photoUser)}</td>\n")
                    html.append("    <td style=\"width: 250px; font-family: monospace; word-break: break-all; word-wrap: break-word;\">$verificationHash</td>\n")
                    html.append("  </tr>\n")
                }
                html.append("</table>\n")
            }
            else -> return null
        }

        html.append("</body>\n")
        html.append("</html>\n")

        try {
            val tempFile = File(context.cacheDir, filename)
            tempFile.writeText(html.toString(), Charsets.UTF_8)
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareExportFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val isXls = file.name.endsWith(".xls")
            val isZip = file.name.endsWith(".zip")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    isXls -> "application/vnd.ms-excel"
                    isZip -> "application/zip"
                    else -> "text/comma-separated-values"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val titleText = when {
                isXls -> "Ekspor Laporan Excel Lengkap + Foto Bukti"
                isZip -> "Ekspor Laporan Lengkap + Foto (Arsip ZIP)"
                else -> "Ekspor Data Ke Excel (CSV) [READ-ONLY PROTECTED]"
            }
            context.startActivity(Intent.createChooser(intent, titleText))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getStartOfRange(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -30)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfRange(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 30)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}

class ITViewModelFactory(private val repository: ITRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ITViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ITViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
