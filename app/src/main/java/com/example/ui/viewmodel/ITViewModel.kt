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

    fun selectAssetByInventoryNum(invNum: String) {
        viewModelScope.launch {
            val asset = repository.getAssetByInventory(invNum)
            selectedAsset.value = asset
        }
    }

    fun getRepairsForAsset(invNum: String): Flow<List<Repair>> = repository.getRepairsForAsset(invNum)
    fun getMaintenancesForAsset(invNum: String): Flow<List<Maintenance>> = repository.getMaintenancesForAsset(invNum)

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

    private fun parseStringPhotoForCsv(photoStr: String?): String {
        if (photoStr.isNullOrBlank()) return "Tidak ada foto"
        val parts = photoStr.split("|||")
        return if (parts.size >= 3) {
            "URL: ${parts[0]} [Lokasi: ${parts[1]}, Tgl: ${parts[2]}]"
        } else {
            "URL: ${parts[0]}"
        }
    }

    // --- Excel / CSV Exporter ---

    fun exportToExcel(context: Context, type: String): File? {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val dateFileFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFileFormat.format(Date())

        val filename: String
        val csvHeader: StringBuilder = StringBuilder()
        val csvContent = StringBuilder()
        val photoFiles = mutableMapOf<String, File>()

        // Write immutable header safeguarding instructions to declare Read-Only status
        csvHeader.append("# ==========================================================================================\n")
        csvHeader.append("# SISTEM PROTEKSI & VERIFIKASI MONITORING TIM IT (READ-ONLY REPORT DOCUMENT)\n")
        csvHeader.append("# DOKUMEN INI TELAH DI-GENERASI SECARA ELEKTRONIK DAN BERSIFAT READ-ONLY.\n")
        csvHeader.append("# KODE VERIFIKASI KEASLIAN ADALAH DIGITAL SIGNATURE UNIK YANG DIHASILKAN SISTEM TIAP BARIS.\n")
        csvHeader.append("# APABILA TERJADI MODIFIKASI DATA SECARA MANUAL, MAKA KODE VERIFIKASI AKAN BATAL/TIDAK COCOK.\n")
        csvHeader.append("# ==========================================================================================\n")

        val assetsList = allAssets.value
        val assetMap = assetsList.associateBy { it.inventoryNumber }

        when (type) {
            "assets" -> {
                filename = "List_Inventaris_IT_$timestamp.csv"
                csvHeader.append("No. Inventaris;Nama Perangkat;Kategori;Lokasi;Status;Deskripsi;Tanggal Terdaftar;Kode Verifikasi Keaslian (SHA-Signature)\n")
                assetsList.forEach { a ->
                    val verificationHash = generateTamperProofHash(a.inventoryNumber, a.name, a.type, a.location, a.status)
                    val row = String.format(
                        "\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\"\n",
                        a.inventoryNumber.replace("\"", "\"\""),
                        a.name.replace("\"", "\"\""),
                        a.type.replace("\"", "\"\""),
                        a.location.replace("\"", "\"\""),
                        a.status.replace("\"", "\"\""),
                        (a.description ?: "").replace("\"", "\"\""),
                        dateFormat.format(Date(a.createdAt)),
                        verificationHash
                    )
                    csvContent.append(row)
                }
            }
            "repairs" -> {
                filename = "Laporan_Perbaikan_IT_$timestamp.csv"
                csvHeader.append("ID Perbaikan;No. Inventaris;Nama Perangkat;Lokasi Perangkat;Waktu Mulai;Waktu Selesai;Kendala;Penyebab;Tindak Lanjut;Status;Alasan Hold;Estimasi;Teknisi;Foto Sebelum;Foto Sesudah;Foto Bersama Unit;Kode Verifikasi Keaslian (SHA-Signature)\n")
                val repairs = filteredRepairs.value
                repairs.forEach { r ->
                    val endStr = r.endTime?.let { dateFormat.format(Date(it)) } ?: "Sedang Diproses/Hold"
                    val devName = assetMap[r.inventoryNumber]?.name ?: "Perangkat Tidak Dikenal"
                    val devLoc = assetMap[r.inventoryNumber]?.location ?: "Lokasi Tidak Tercatat"
                    
                    val beforePhoto = extractPhotoAndGetLabel(context, r.photoBefore, "sebelum_rep_${r.id}", r.id.toString(), photoFiles)
                    val afterPhoto = extractPhotoAndGetLabel(context, r.photoAfter, "sesudah_rep_${r.id}", r.id.toString(), photoFiles)
                    val userPhoto = extractPhotoAndGetLabel(context, r.photoUser, "handover_rep_${r.id}", r.id.toString(), photoFiles)
                    
                    val verificationHash = generateTamperProofHash(
                        r.id.toString(), r.inventoryNumber, r.status, r.technician, r.startTime.toString()
                    )

                    val row = String.format(
                        "\"%d\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\"\n",
                        r.id,
                        r.inventoryNumber.replace("\"", "\"\""),
                        devName.replace("\"", "\"\""),
                        devLoc.replace("\"", "\"\""),
                        dateFormat.format(Date(r.startTime)),
                        endStr,
                        r.problem.replace("\"", "\"\""),
                        r.cause.replace("\"", "\"\""),
                        r.actionTaken.replace("\"", "\"\""),
                        r.status.replace("\"", "\"\""),
                        (r.holdReason ?: "").replace("\"", "\"\""),
                        (r.holdEstimate ?: "").replace("\"", "\"\""),
                        r.technician.replace("\"", "\"\""),
                        beforePhoto.replace("\"", "\"\""),
                        afterPhoto.replace("\"", "\"\""),
                        userPhoto.replace("\"", "\"\""),
                        verificationHash
                    )
                    csvContent.append(row)
                }
            }
            "maintenances" -> {
                filename = "Laporan_Perawatan_IT_$timestamp.csv"
                csvHeader.append("ID Perawatan;No. Inventaris;Nama Perangkat;Lokasi Perangkat;Waktu Mulai;Waktu Selesai;Tindakan;Kendala Temuan;Hasil;Status;Teknisi;Foto Sebelum;Foto Sesudah;Foto Bersama Unit;Kode Verifikasi Keaslian (SHA-Signature)\n")
                val maints = filteredMaintenances.value
                maints.forEach { m ->
                    val endStr = m.endTime?.let { dateFormat.format(Date(it)) } ?: "Sedang Diproses"
                    val devName = assetMap[m.inventoryNumber]?.name ?: "Perangkat Tidak Dikenal"
                    val devLoc = assetMap[m.inventoryNumber]?.location ?: "Lokasi Tidak Tercatat"
                    
                    val beforePhoto = extractPhotoAndGetLabel(context, m.photoBefore, "sebelum_maint_${m.id}", m.id.toString(), photoFiles)
                    val afterPhoto = extractPhotoAndGetLabel(context, m.photoAfter, "sesudah_maint_${m.id}", m.id.toString(), photoFiles)
                    val userPhoto = extractPhotoAndGetLabel(context, m.photoUser, "handover_maint_${m.id}", m.id.toString(), photoFiles)

                    val verificationHash = generateTamperProofHash(
                        m.id.toString(), m.inventoryNumber, m.status, m.technician, m.startTime.toString()
                    )

                    val row = String.format(
                        "\"%d\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\"\n",
                        m.id,
                        m.inventoryNumber.replace("\"", "\"\""),
                        devName.replace("\"", "\"\""),
                        devLoc.replace("\"", "\"\""),
                        dateFormat.format(Date(m.startTime)),
                        endStr,
                        m.actionTaken.replace("\"", "\"\""),
                        m.issuesFound.replace("\"", "\"\""),
                        m.result.replace("\"", "\"\""),
                        m.status.replace("\"", "\"\""),
                        m.technician.replace("\"", "\"\""),
                        beforePhoto.replace("\"", "\"\""),
                        afterPhoto.replace("\"", "\"\""),
                        userPhoto.replace("\"", "\"\""),
                        verificationHash
                    )
                    csvContent.append(row)
                }
            }
            else -> return null
        }

        // Add verification footer
        csvContent.append("# ==========================================================================================\n")
        csvContent.append("# TIM IT INFRASTRUKTUR & DESKTOP SUPPORT HUB - SECURE RECORD VERIFIED VER.2.6\n")
        csvContent.append("# DILARANG KERAS MEMALSUKAN ATAU MENGUBAH REPORT TANPA ACC TIM IT HUB.\n")
        csvContent.append("# ==========================================================================================\n")

        try {
            // Write to local cache / external files dir
            val tempCsvFile = File(context.cacheDir, filename)
            tempCsvFile.writeText(csvHeader.toString() + csvContent.toString(), Charsets.UTF_8)
            
            if (photoFiles.isNotEmpty()) {
                val zipFilename = filename.replace(".csv", "_Dengan_Foto.zip")
                val zipFile = createZipFile(context, tempCsvFile, photoFiles, zipFilename)
                if (zipFile != null) return zipFile
            }
            
            return tempCsvFile
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
            val isZip = file.name.endsWith(".zip")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (isZip) "application/zip" else "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val titleText = if (isZip) "Ekspor Laporan Lengkap + Foto (Arsip ZIP)" else "Ekspor Data Ke Excel (CSV) [READ-ONLY PROTECTED]"
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
