package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.StaticLayout
import android.text.TextPaint
import android.text.Layout
import android.util.Base64
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

    val allCategories: StateFlow<List<com.example.data.model.Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allUpdateLogs: StateFlow<List<com.example.data.model.AssetUpdateLog>> = repository.allUpdateLogs.stateIn(
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

            // Seed default categories if empty
            val categories = repository.allCategories.first()
            if (categories.isEmpty()) {
                val defaultCategories = listOf(
                    com.example.data.model.Category(
                        name = "Laptop",
                        guidelines = listOf(
                            "Cek kesehatan baterai (Battery Health)",
                            "Perbarui sistem operasi & security patch",
                            "Bersihkan kipas & ganti thermal paste if > 1 year old"
                        ).joinToString("||~||")
                    ),
                    com.example.data.model.Category(
                        name = "PC Desktop",
                        guidelines = listOf(
                            "Bersihkan debu casing luar dan dalam",
                            "Cek suhu CPU idle dan load",
                            "Verifikasi kestabilan tegangan PSU"
                        ).joinToString("||~||")
                    ),
                    com.example.data.model.Category(
                        name = "Printer",
                        guidelines = listOf(
                            "Lakukan print head nozzle check",
                            "Bersihkan roller penarik kertas dari residu",
                            "Ganti tinta/toner jika di bawah 20%"
                        ).joinToString("||~||")
                    ),
                    com.example.data.model.Category(
                        name = "Network Device",
                        guidelines = listOf(
                            "Backup file konfigurasi router/switch",
                            "Cek status keaktifan port ethernet",
                            "Verifikasi suhu perangkat dalam rack mount"
                        ).joinToString("||~||")
                    ),
                    com.example.data.model.Category(
                        name = "Server",
                        guidelines = listOf(
                            "Cek utilitas CPU dan sisa kapasitas RAM",
                            "Verifikasi log error sistem operasi",
                            "Pastikan temperatur server room tetap sejuk"
                        ).joinToString("||~||")
                    ),
                    com.example.data.model.Category(
                        name = "Lainnya",
                        guidelines = listOf(
                            "Periksa kondisi fisik perangkat",
                            "Pastikan perkabelan rapi dan aman"
                        ).joinToString("||~||")
                    )
                )
                defaultCategories.forEach { repository.insertCategory(it) }
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

    fun saveCategory(category: com.example.data.model.Category, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.insertCategory(category)
            onComplete()
        }
    }

    fun deleteCategory(category: com.example.data.model.Category, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteCategory(category)
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

    private fun decodeBase64ToBitmap(base64Str: String?): Bitmap? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            val clean = if (base64Str.startsWith("data:image")) {
                base64Str.substringAfter("base64,")
            } else {
                base64Str
            }
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    class PdfPageWriter {
        val document = PdfDocument()
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var pageNumber = 0
        var currentY = 0f
        val margin = 40f
        val bottomLimit = 842f - 60f

        fun newPage(title: String, subtitle: String) {
            if (currentPage != null) {
                document.finishPage(currentPage)
            }
            pageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
            val page = document.startPage(pageInfo)
            currentPage = page
            canvas = page.canvas
            currentY = margin

            canvas?.let { c ->
                val bandPaint = Paint().apply {
                    color = 0xFF0D47A1.toInt()
                    style = Paint.Style.FILL
                }
                c.drawRect(40f, 30f, 555f, 55f, bandPaint)

                val titlePaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 10f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                c.drawText(title.uppercase(), 50f, 47f, titlePaint)

                val textPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 8f
                    isAntiAlias = true
                }
                c.drawText("$subtitle | Halaman $pageNumber", 40f, 72f, textPaint)

                val linePaint = Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 1f
                }
                c.drawLine(40f, 78f, 555f, 78f, linePaint)
            }
            currentY = 95f
        }

        fun saveAndClose(context: Context, filename: String): File? {
            if (currentPage != null) {
                document.finishPage(currentPage)
            }
            return try {
                val file = File(context.cacheDir, filename)
                val fos = java.io.FileOutputStream(file)
                document.writeTo(fos)
                fos.close()
                document.close()
                file
            } catch (e: Exception) {
                e.printStackTrace()
                document.close()
                null
            }
        }
    }

    private fun drawAssetRow(
        writer: PdfPageWriter,
        cells: List<String>,
        widths: List<Float>,
        isHeader: Boolean = false
    ) {
        if (writer.canvas == null) return
        val canvas = writer.canvas!!

        val textPaint = TextPaint().apply {
            color = if (isHeader) Color.WHITE else Color.BLACK
            textSize = 7.5f
            isAntiAlias = true
            if (isHeader) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val layouts = cells.mapIndexed { idx, t ->
            val w = widths[idx]
            val wrapW = (w - 10f).coerceAtLeast(10f).toInt()
            StaticLayout(
                t, textPaint, wrapW, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false
            )
        }

        val maxTextHeight = layouts.maxOfOrNull { it.height } ?: 0
        val rowHeight = maxTextHeight + 12f

        if (writer.currentY + rowHeight > writer.bottomLimit) {
            writer.newPage("LIST INVENTARIS ASET TIM IT SUPPORT", "Dokumen Resmi Aset IT Hub")
            drawAssetRow(writer, listOf("No. Inventaris", "Nama Perangkat", "Kategori", "Lokasi", "Status", "Deskripsi"), widths, true)
        }

        val canvasNow = writer.canvas!!

        val bgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = if (isHeader) 0xFF0D47A1.toInt() else Color.WHITE
        }
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = 0xFFCCCCCC.toInt()
            strokeWidth = 0.5f
        }

        canvasNow.drawRect(40f, writer.currentY, 555f, writer.currentY + rowHeight, bgPaint)

        var currX = 40f
        layouts.forEachIndexed { idx, layout ->
            val w = widths[idx]
            canvasNow.drawRect(currX, writer.currentY, currX + w, writer.currentY + rowHeight, borderPaint)

            canvasNow.save()
            canvasNow.translate(currX + 5f, writer.currentY + 6f)
            layout.draw(canvasNow)
            canvasNow.restore()

            currX += w
        }

        writer.currentY += rowHeight
    }

    private fun drawRepairCard(
        writer: PdfPageWriter,
        r: Repair,
        assetName: String,
        dateFormat: SimpleDateFormat
    ) {
        val cardHeight = 135f
        if (writer.currentY + cardHeight > writer.bottomLimit) {
            writer.newPage("LAPORAN PERBAIKAN TIM IT SUPPORT", "Dokumen Resmi Perbaikan Aset")
        }

        val canvas = writer.canvas!!
        val y = writer.currentY

        val fillPaint = Paint().apply {
            color = 0xFFFCFCFC.toInt()
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = 0xFFE0E0E0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val rect = RectF(40f, y, 555f, y + cardHeight - 10f)
        canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val titleBgPaint = Paint().apply {
            color = 0xFFECEFF1.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, y, 555f, y + 24f), 6f, 6f, titleBgPaint)
        canvas.drawRect(40f, y + 15f, 555f, y + 24f, titleBgPaint)

        val labelPaint = Paint().apply {
            color = 0xFF37474F.toInt()
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val txt = "PERBAIKAN #${r.id} - ASET: ${r.inventoryNumber} (${assetName.uppercase()})"
        canvas.drawText(txt, 48f, y + 15f, labelPaint)

        val statusColor = when (r.status) {
            "Selesai & Terverifikasi" -> 0xFF2E7D32.toInt()
            "Dalam Pengerjaan" -> 0xFF1565C0.toInt()
            "Hold" -> 0xFFC62828.toInt()
            else -> 0xFF37474F.toInt()
        }
        val statusBgPaint = Paint().apply {
            color = statusColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(460f, y + 4f, 545f, y + 20f), 3f, 3f, statusBgPaint)

        val statusTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(r.status, 502.5f, y + 14f, statusTextPaint)

        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 7.5f
            isAntiAlias = true
        }
        val keyPaint = TextPaint().apply {
            color = Color.GRAY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var textY = y + 36f
        fun drawField(label: String, value: String) {
            canvas.drawText(label, 48f, textY, keyPaint)
            val wrapLayout = StaticLayout(
                value, textPaint, 175, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false
            )
            canvas.save()
            canvas.translate(110f, textY - 7f)
            wrapLayout.draw(canvas)
            canvas.restore()
            textY += wrapLayout.height + 3f
        }

        val startStr = dateFormat.format(Date(r.startTime))
        val endStr = r.endTime?.let { dateFormat.format(Date(it)) } ?: "Belum Selesai"

        drawField("Waktu Mulai:", startStr)
        drawField("Waktu Selesai:", endStr)
        drawField("Kendala:", r.problem)
        drawField("Penyebab:", r.cause)
        drawField("Tindakan:", r.actionTaken)
        drawField("Teknisi:", r.technician)
        if (!r.holdReason.isNullOrBlank()) {
            drawField("Alasan Hold:", "${r.holdReason} (Est: ${r.holdEstimate ?: "-"})")
        }

        fun drawPhoto(photoStr: String?, label: String, photoX: Float, photoY: Float) {
            val rectBg = RectF(photoX, photoY, photoX + 75f, photoY + 50f)
            val rectPaint = Paint().apply {
                color = 0xFFF5F5F5.toInt()
                style = Paint.Style.FILL
            }
            val strokePaint = Paint().apply {
                color = 0xFFE0E0E0.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            canvas.drawRect(rectBg, rectPaint)
            canvas.drawRect(rectBg, strokePaint)

            val lblPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 6f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(label, photoX + 37.5f, photoY + 62f, lblPaint)

            val (urlPart, locPart, datePart) = parseWatermarkedPhoto(photoStr)
            val imageBitmap = decodeBase64ToBitmap(urlPart)
            if (imageBitmap != null) {
                val src = Rect(0, 0, imageBitmap.width, imageBitmap.height)
                val dst = Rect((photoX + 0.5f).toInt(), (photoY + 0.5f).toInt(), (photoX + 74.5f).toInt(), (photoY + 49.5f).toInt())
                canvas.drawBitmap(imageBitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))

                val txt = "${locPart.take(16)}, ${datePart.take(10)}"
                val overlayPaint = Paint().apply {
                    color = 0xAA000000.toInt()
                    style = Paint.Style.FILL
                }
                canvas.drawRect(RectF(photoX + 0.5f, photoY + 40.5f, photoX + 74.5f, photoY + 49.5f), overlayPaint)
                val wmTxtPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 3f
                    isAntiAlias = true
                }
                canvas.drawText(txt, photoX + 2.5f, photoY + 47f, wmTxtPaint)
            } else {
                val emptyPaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 6f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("No Photo", photoX + 37.5f, photoY + 28f, emptyPaint)
            }
        }

        drawPhoto(r.photoBefore, "SEBELUM", 300f, y + 42f)
        drawPhoto(r.photoAfter, "SESUDAH", 385f, y + 42f)
        drawPhoto(r.photoUser, "PENERIMA", 470f, y + 42f)

        writer.currentY += cardHeight
    }

    private fun drawMaintenanceCard(
        writer: PdfPageWriter,
        m: Maintenance,
        assetName: String,
        dateFormat: SimpleDateFormat
    ) {
        val cardHeight = 135f
        if (writer.currentY + cardHeight > writer.bottomLimit) {
            writer.newPage("LAPORAN PERAWATAN TIM IT SUPPORT", "Dokumen Resmi Perawatan Rutin")
        }

        val canvas = writer.canvas!!
        val y = writer.currentY

        val fillPaint = Paint().apply {
            color = 0xFFFCFCFC.toInt()
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = 0xFFE0E0E0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val rect = RectF(40f, y, 555f, y + cardHeight - 10f)
        canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
        canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

        val titleBgPaint = Paint().apply {
            color = 0xFFE8F5E9.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(40f, y, 555f, y + 24f), 6f, 6f, titleBgPaint)
        canvas.drawRect(40f, y + 15f, 555f, y + 24f, titleBgPaint)

        val labelPaint = Paint().apply {
            color = 0xFF2E7D32.toInt()
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val txt = "PERAWATAN #${m.id} - ASET: ${m.inventoryNumber} (${assetName.uppercase()})"
        canvas.drawText(txt, 48f, y + 15f, labelPaint)

        val statusColor = when (m.status) {
            "Selesai" -> 0xFF2E7D32.toInt()
            "Selesai & Terverifikasi" -> 0xFF2E7D32.toInt()
            "Dalam Pengerjaan" -> 0xFF1565C0.toInt()
            else -> 0xFF37474F.toInt()
        }
        val statusBgPaint = Paint().apply {
            color = statusColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(460f, y + 4f, 545f, y + 20f), 3f, 3f, statusBgPaint)

        val statusTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(m.status, 502.5f, y + 14f, statusTextPaint)

        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 7.5f
            isAntiAlias = true
        }
        val keyPaint = TextPaint().apply {
            color = Color.GRAY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var textY = y + 36f
        fun drawField(label: String, value: String) {
            canvas.drawText(label, 48f, textY, keyPaint)
            val wrapLayout = StaticLayout(
                value, textPaint, 175, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false
            )
            canvas.save()
            canvas.translate(110f, textY - 7f)
            wrapLayout.draw(canvas)
            canvas.restore()
            textY += wrapLayout.height + 3f
        }

        val startStr = dateFormat.format(Date(m.startTime))
        val endStr = m.endTime?.let { dateFormat.format(Date(it)) } ?: "Belum Selesai"

        drawField("Waktu Mulai:", startStr)
        drawField("Waktu Selesai:", endStr)
        drawField("Tindakan:", m.actionTaken)
        drawField("Temuan Kendala:", m.issuesFound)
        drawField("Hasil Akhir:", m.result)
        drawField("Teknisi:", m.technician)

        fun drawPhoto(photoStr: String?, label: String, photoX: Float, photoY: Float) {
            val rectBg = RectF(photoX, photoY, photoX + 75f, photoY + 50f)
            val rectPaint = Paint().apply {
                color = 0xFFF5F5F5.toInt()
                style = Paint.Style.FILL
            }
            val strokePaint = Paint().apply {
                color = 0xFFE0E0E0.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            canvas.drawRect(rectBg, rectPaint)
            canvas.drawRect(rectBg, strokePaint)

            val lblPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 6f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(label, photoX + 37.5f, photoY + 62f, lblPaint)

            val (urlPart, locPart, datePart) = parseWatermarkedPhoto(photoStr)
            val imageBitmap = decodeBase64ToBitmap(urlPart)
            if (imageBitmap != null) {
                val src = Rect(0, 0, imageBitmap.width, imageBitmap.height)
                val dst = Rect((photoX + 0.5f).toInt(), (photoY + 0.5f).toInt(), (photoX + 74.5f).toInt(), (photoY + 49.5f).toInt())
                canvas.drawBitmap(imageBitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))

                val txt = "${locPart.take(16)}, ${datePart.take(10)}"
                val overlayPaint = Paint().apply {
                    color = 0xAA000000.toInt()
                    style = Paint.Style.FILL
                }
                canvas.drawRect(RectF(photoX + 0.5f, photoY + 40.5f, photoX + 74.5f, photoY + 49.5f), overlayPaint)
                val wmTxtPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 3f
                    isAntiAlias = true
                }
                canvas.drawText(txt, photoX + 2.5f, photoY + 47f, wmTxtPaint)
            } else {
                val emptyPaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 6f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("No Photo", photoX + 37.5f, photoY + 28f, emptyPaint)
            }
        }

        drawPhoto(m.photoBefore, "SEBELUM", 300f, y + 42f)
        drawPhoto(m.photoAfter, "SESUDAH", 385f, y + 42f)
        drawPhoto(m.photoUser, "PENERIMA", 470f, y + 42f)

        writer.currentY += cardHeight
    }

    fun exportToPdf(context: Context, type: String): File? {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val dateFileFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFileFormat.format(Date())

        val writer = PdfPageWriter()
        val assetsList = allAssets.value
        val assetMap = assetsList.associateBy { it.inventoryNumber }

        return when (type) {
            "assets" -> {
                val filename = "List_Inventaris_IT_$timestamp.pdf"
                writer.newPage("LIST INVENTARIS ASET TIM IT SUPPORT", "Sistem Inventaris IT Hub - Resmi")

                val colWidths = listOf(85f, 115f, 85f, 85f, 55f, 90f)
                val headers = listOf("No. Inventaris", "Nama Perangkat", "Kategori", "Lokasi", "Status", "Deskripsi")
                drawAssetRow(writer, headers, colWidths, isHeader = true)

                assetsList.forEach { a ->
                    val rowCells = listOf(
                        a.inventoryNumber,
                        a.name,
                        a.type,
                        a.location,
                        a.status,
                        a.description ?: ""
                    )
                    drawAssetRow(writer, rowCells, colWidths, isHeader = false)
                }
                writer.saveAndClose(context, filename)
            }
            "repairs" -> {
                val filename = "Laporan_Perbaikan_IT_$timestamp.pdf"
                writer.newPage("LAPORAN PERBAIKAN TIM IT SUPPORT", "Hanya Laporan Terverifikasi & Selesai")

                val repairs = filteredRepairs.value.filter { it.status == "Selesai & Terverifikasi" }
                repairs.forEach { r ->
                    val assetName = assetMap[r.inventoryNumber]?.name ?: "Aset Tidak Dikenal"
                    drawRepairCard(writer, r, assetName, dateFormat)
                }
                writer.saveAndClose(context, filename)
            }
            "maintenances" -> {
                val filename = "Laporan_Perawatan_IT_$timestamp.pdf"
                writer.newPage("LAPORAN PERAWATAN RUTIN TIM IT SUPPORT", "Hanya Laporan Terverifikasi & Selesai")

                val maints = filteredMaintenances.value.filter { it.status == "Selesai & Terverifikasi" }
                maints.forEach { m ->
                    val assetName = assetMap[m.inventoryNumber]?.name ?: "Aset Tidak Dikenal"
                    drawMaintenanceCard(writer, m, assetName, dateFormat)
                }
                writer.saveAndClose(context, filename)
            }
            else -> null
        }
    }

    fun exportAllLogsToExcel(context: Context): File? {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val dateFileFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFileFormat.format(Date())
        val filename = "Log_Sistem_Lengkap_IT_$timestamp.xlsx"

        val assets = allAssets.value
        val repairs = allRepairs.value
        val maintenances = allMaintenances.value
        val logs = allUpdateLogs.value
        
        val assetNameMap = assets.associateBy { it.inventoryNumber }

        fun colName(colIndex: Int): String {
            var temp = colIndex
            var colName = ""
            while (temp >= 0) {
                colName = ('A' + (temp % 26)).toString() + colName
                temp = (temp / 26) - 1
            }
            return colName
        }

        fun escapeXml(str: String?): String {
            if (str == null) return ""
            return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")
        }

        class SheetWriter {
            private val sb = java.lang.StringBuilder()
            private var rowCount = 0

            fun startSheet(colsWidths: List<Int>) {
                sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">\n")
                sb.append("  <cols>\n")
                colsWidths.forEachIndexed { idx, w ->
                    sb.append("    <col min=\"${idx + 1}\" max=\"${idx + 1}\" width=\"$w\" customWidth=\"1\"/>\n")
                }
                sb.append("  </cols>\n")
                sb.append("  <sheetData>\n")
            }

            fun writeRow(height: Int, cells: List<Any?>, styleIndices: List<Int> = emptyList()) {
                rowCount++
                sb.append("    <row r=\"$rowCount\" ht=\"$height\" customHeight=\"1\">\n")
                cells.forEachIndexed { colIdx, cellVal ->
                    val colNameStr = colName(colIdx)
                    val ref = "$colNameStr$rowCount"
                    val styleIdx = if (styleIndices.size > colIdx) styleIndices[colIdx] else (if (styleIndices.isNotEmpty()) styleIndices[0] else 0)

                    if (cellVal == null) {
                        // Empty cell
                    } else if (cellVal is Number) {
                        sb.append("      <c r=\"$ref\" s=\"$styleIdx\"><v>$cellVal</v></c>\n")
                    } else {
                        val str = cellVal.toString()
                        sb.append("      <c r=\"$ref\" s=\"$styleIdx\" t=\"inlineStr\"><is><t>${escapeXml(str)}</t></is></c>\n")
                    }
                }
                sb.append("    </row>\n")
            }
            
            fun writeEmptyRow() {
                rowCount++
                sb.append("    <row r=\"$rowCount\"/>\n")
            }

            fun endSheet(protect: Boolean = true) {
                sb.append("  </sheetData>\n")
                if (protect) {
                    sb.append("  <sheetProtection sheet=\"true\" objects=\"true\" scenarios=\"true\" selectLockedCells=\"true\" selectUnlockedCells=\"true\"/>\n")
                }
                sb.append("</worksheet>\n")
            }

            override fun toString() = sb.toString()
        }

        // SHEET 1: Inventaris & Logs
        val sheet1 = SheetWriter()
        sheet1.startSheet(listOf(18, 22, 18, 18, 18, 25, 20))
        sheet1.writeRow(28, listOf("DAFTAR INVENTARIS ASET Tim IT Support", null, null, null, null, null, null), listOf(2))
        sheet1.writeRow(18, listOf("Exported: ${dateFormat.format(Date())} | Total Aset: ${assets.size}", null, null, null, null, null, null), listOf(3))
        sheet1.writeEmptyRow()
        sheet1.writeRow(22, listOf("No. Inventaris", "Nama Perangkat", "Kategori", "Lokasi", "Status", "Deskripsi", "Tanggal Registrasi"), listOf(1))
        assets.forEach { a ->
            sheet1.writeRow(20, listOf(
                a.inventoryNumber,
                a.name,
                a.type,
                a.location,
                a.status,
                a.description ?: "",
                dateFormat.format(Date(a.createdAt))
            ), listOf(0))
        }
        sheet1.endSheet(protect = true)

        // SHEET 2: Perbaikan
        val sheet2 = SheetWriter()
        sheet2.startSheet(listOf(8, 18, 22, 20, 20, 25, 25, 25, 15, 25, 18))
        sheet2.writeRow(28, listOf("DAFTAR RIWAYAT PERBAIKAN ASET (SEMUA STATUS)", null, null, null, null, null, null, null, null, null, null), listOf(2))
        sheet2.writeEmptyRow()
        sheet2.writeRow(22, listOf("ID", "No. Inventaris", "Nama Perangkat", "Waktu Mulai", "Waktu Selesai", "Kendala", "Penyebab", "Tindakan", "Status", "Alasan Hold / Estimasi", "Teknisi"), listOf(1))
        repairs.forEach { r ->
            val devName = assetNameMap[r.inventoryNumber]?.name ?: "Aset Tidak Dikenal"
            val endStr = r.endTime?.let { dateFormat.format(Date(it)) } ?: "Proses"
            val holdStr = if (!r.holdReason.isNullOrBlank()) "${r.holdReason} (Est: ${r.holdEstimate ?: "-"})" else ""
            sheet2.writeRow(22, listOf(
                r.id,
                r.inventoryNumber,
                devName,
                dateFormat.format(Date(r.startTime)),
                endStr,
                r.problem,
                r.cause,
                r.actionTaken,
                r.status,
                holdStr,
                r.technician
            ), listOf(0))
        }
        sheet2.endSheet(protect = true)

        // SHEET 3: Perawatan
        val sheet3 = SheetWriter()
        sheet3.startSheet(listOf(8, 18, 22, 20, 20, 25, 25, 25, 15, 18))
        sheet3.writeRow(28, listOf("DAFTAR RIWAYAT PERAWATAN RUTIN ASET (SEMUA STATUS)", null, null, null, null, null, null, null, null, null), listOf(2))
        sheet3.writeEmptyRow()
        sheet3.writeRow(22, listOf("ID", "No. Inventaris", "Nama Perangkat", "Waktu Mulai", "Waktu Selesai", "Tindakan Dilakukan", "Kendala Ditemukan", "Hasil / Rekomendasi", "Status", "Teknisi"), listOf(1))
        maintenances.forEach { m ->
            val devName = assetNameMap[m.inventoryNumber]?.name ?: "Aset Tidak Dikenal"
            val endStr = m.endTime?.let { dateFormat.format(Date(it)) } ?: "Proses"
            sheet3.writeRow(22, listOf(
                m.id,
                m.inventoryNumber,
                devName,
                dateFormat.format(Date(m.startTime)),
                endStr,
                m.actionTaken,
                m.issuesFound,
                m.result,
                m.status,
                m.technician
            ), listOf(0))
        }
        sheet3.endSheet(protect = true)

        // SHEET 4: Riwayat Aset (Log Update)
        val sheet4 = SheetWriter()
        sheet4.startSheet(listOf(22, 18, 25, 25, 25, 25, 25))
        sheet4.writeRow(28, listOf("RIWAYAT PERUBAHAN & LOG UPDATE INVENTARIS", null, null, null, null, null, null), listOf(2))
        sheet4.writeEmptyRow()
        sheet4.writeRow(22, listOf("Waktu Update", "No. Inventaris", "Lokasi Lama > Baru", "Status Lama > Baru", "Komentar Lama", "Komentar Baru", "Sebab Rusak Permanen"), listOf(1))
        logs.forEach { l ->
            sheet4.writeRow(22, listOf(
                dateFormat.format(Date(l.updateTime)),
                l.inventoryNumber,
                "${l.oldLocation} > ${l.newLocation}",
                "${l.oldStatus} > ${l.newStatus}",
                l.oldDescription ?: "",
                l.newDescription ?: "",
                l.reasonForPermanentDamage ?: ""
            ), listOf(0))
        }
        sheet4.endSheet(protect = true)

        val contentTypesXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
              <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
            </Types>
        """.trimIndent()

        val relsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()

        val workbookXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                <sheet name="Inventaris" sheetId="1" r:id="rId1"/>
                <sheet name="Perbaikan" sheetId="2" r:id="rId2"/>
                <sheet name="Perawatan" sheetId="3" r:id="rId3"/>
                <sheet name="Riwayat Aset" sheetId="4" r:id="rId4"/>
              </sheets>
            </workbook>
         """.trimIndent()

        val workbookRelsXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
              <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
              <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
              <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
              <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
            </Relationships>
        """.trimIndent()

        val stylesXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <fonts count="4">
                <font><fontSize val="11"/><color rgb="000000"/><name val="Segoe UI"/></font>
                <font><b/><fontSize val="14"/><color rgb="01579B"/><name val="Segoe UI"/></font>
                <font><b/><fontSize val="11"/><color rgb="FFFFFFFF"/><name val="Segoe UI"/></font>
                <font><b/><fontSize val="11"/><color rgb="01579B"/><name val="Segoe UI"/></font>
              </fonts>
              <fills count="4">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
                <fill>
                  <patternFill patternType="solid">
                     <fgColor rgb="FF0288D1"/>
                     <bgColor rgb="FF0288D1"/>
                  </patternFill>
                </fill>
                <fill>
                  <patternFill patternType="solid">
                     <fgColor rgb="FFE1F5FE"/>
                     <bgColor rgb="FFE1F5FE"/>
                  </patternFill>
                </fill>
              </fills>
              <borders count="1">
                <border><left/><right/><top/><bottom/></border>
              </borders>
              <cellStyleXfs count="1">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
              </cellStyleXfs>
              <cellXfs count="4">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="0" fontId="2" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1">
                  <alignment horizontal="center" vertical="center" wrapText="1"/>
                </xf>
                <xf numFmtId="0" fontId="1" fillId="3" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1">
                  <alignment horizontal="left" vertical="center"/>
                </xf>
                <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
              </cellXfs>
              <cellStyles count="1">
                <cellStyle name="Normal" xfId="0" builtinId="0"/>
              </cellStyles>
            </styleSheet>
        """.trimIndent()

        return try {
            val file = File(context.cacheDir, filename)
            java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(java.io.FileOutputStream(file))).use { zos ->
                fun addEntry(name: String, content: String) {
                    zos.putNextEntry(java.util.zip.ZipEntry(name))
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
                addEntry("[Content_Types].xml", contentTypesXml)
                addEntry("_rels/.rels", relsXml)
                addEntry("xl/workbook.xml", workbookXml)
                addEntry("xl/_rels/workbook.xml.rels", workbookRelsXml)
                addEntry("xl/styles.xml", stylesXml)
                addEntry("xl/worksheets/sheet1.xml", sheet1.toString())
                addEntry("xl/worksheets/sheet2.xml", sheet2.toString())
                addEntry("xl/worksheets/sheet3.xml", sheet3.toString())
                addEntry("xl/worksheets/sheet4.xml", sheet4.toString())
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareExportFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val isPdf = file.name.endsWith(".pdf")
            val isXls = file.name.endsWith(".xls") || file.name.endsWith(".xlsx")
            val isZip = file.name.endsWith(".zip")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    isPdf -> "application/pdf"
                    isXls -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    isZip -> "application/zip"
                    else -> "text/comma-separated-values"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val titleText = when {
                isPdf -> "Bagikan Laporan Resmi PDF"
                isXls -> "Bagikan Log Sistem Lengkap (Excel)"
                isZip -> "Ekspor Laporan Lengkap + Foto (Arsip ZIP)"
                else -> "Ekspor Data [READ-ONLY]"
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
