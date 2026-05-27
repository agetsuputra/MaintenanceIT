package com.example.data.repository

import com.example.BuildConfig
import com.example.data.database.InventoryDao
import com.example.data.model.Asset
import com.example.data.model.Repair
import com.example.data.model.Maintenance
import com.example.data.model.AssetUpdateLog
import com.example.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import java.io.File

class ITRepository(private val dao: InventoryDao) {

    // Check if Firebase settings are valid and active
    val isFirebaseEnabled: Boolean by lazy {
        val apiKey = try { BuildConfig.FIREBASE_API_KEY } catch (e: Throwable) { "" }
        val appId = try { BuildConfig.FIREBASE_APPLICATION_ID } catch (e: Throwable) { "" }
        val projectId = try { BuildConfig.FIREBASE_PROJECT_ID } catch (e: Throwable) { "" }
        
        apiKey.isNotBlank() && apiKey != "YOUR_FIREBASE_API_KEY" &&
        appId.isNotBlank() && appId != "YOUR_FIREBASE_APPLICATION_ID" &&
        projectId.isNotBlank() && projectId != "YOUR_FIREBASE_PROJECT_ID"
    }

    private val firestore: FirebaseFirestore? by lazy {
        if (isFirebaseEnabled) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Throwable) {
                null
            }
        } else {
            null
        }
    }

    // Live Stream of Assets
    val allAssets: Flow<List<Asset>> = if (isFirebaseEnabled && firestore != null) {
        callbackFlow {
            val listener = firestore?.collection("assets")
                ?.orderBy("createdAt", Query.Direction.DESCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                Asset(
                                    inventoryNumber = doc.getString("inventoryNumber") ?: doc.id,
                                    name = doc.getString("name") ?: "",
                                    type = doc.getString("type") ?: "",
                                    location = doc.getString("location") ?: "",
                                    status = doc.getString("status") ?: "",
                                    description = doc.getString("description"),
                                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                    acquisitionDate = doc.getLong("acquisitionDate") ?: (doc.getLong("createdAt") ?: System.currentTimeMillis()),
                                    purchasePrice = doc.getDouble("purchasePrice")
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(list)
                    }
                }
            awaitClose { listener?.remove() }
        }
    } else {
        dao.getAllAssets()
    }

    // Live Stream of Repairs
    val allRepairs: Flow<List<Repair>> = if (isFirebaseEnabled && firestore != null) {
        callbackFlow {
            val listener = firestore?.collection("repairs")
                ?.orderBy("startTime", Query.Direction.DESCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                Repair(
                                    id = doc.getLong("id")?.toInt() ?: doc.id.hashCode(),
                                    inventoryNumber = doc.getString("inventoryNumber") ?: "",
                                    startTime = doc.getLong("startTime") ?: 0L,
                                    endTime = doc.get("endTime")?.toString()?.toLongOrNull(),
                                    problem = doc.getString("problem") ?: "",
                                    cause = doc.getString("cause") ?: "",
                                    actionTaken = doc.getString("actionTaken") ?: "",
                                    status = doc.getString("status") ?: "",
                                    holdReason = doc.getString("holdReason"),
                                    holdEstimate = doc.getString("holdEstimate"),
                                    photoBefore = doc.getString("photoBefore"),
                                    photoAfter = doc.getString("photoAfter"),
                                    photoUser = doc.getString("photoUser"),
                                    technician = doc.getString("technician") ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(list)
                    }
                }
            awaitClose { listener?.remove() }
        }
    } else {
        dao.getAllRepairs()
    }

    // Live Stream of Maintenances
    val allMaintenances: Flow<List<Maintenance>> = if (isFirebaseEnabled && firestore != null) {
        callbackFlow {
            val listener = firestore?.collection("maintenances")
                ?.orderBy("startTime", Query.Direction.DESCENDING)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                Maintenance(
                                    id = doc.getLong("id")?.toInt() ?: doc.id.hashCode(),
                                    inventoryNumber = doc.getString("inventoryNumber") ?: "",
                                    startTime = doc.getLong("startTime") ?: 0L,
                                    endTime = doc.get("endTime")?.toString()?.toLongOrNull(),
                                    actionTaken = doc.getString("actionTaken") ?: "",
                                    issuesFound = doc.getString("issuesFound") ?: "",
                                    result = doc.getString("result") ?: "",
                                    status = doc.getString("status") ?: "",
                                    photoBefore = doc.getString("photoBefore"),
                                    photoAfter = doc.getString("photoAfter"),
                                    photoUser = doc.getString("photoUser"),
                                    technician = doc.getString("technician") ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(list)
                    }
                }
            awaitClose { listener?.remove() }
        }
    } else {
        dao.getAllMaintenances()
    }

    // --- Asset Operations ---
    suspend fun getAssetByInventory(invNum: String): Asset? {
        if (isFirebaseEnabled && firestore != null) {
            return try {
                val task = firestore!!.collection("assets").document(invNum).get()
                val doc = com.google.android.gms.tasks.Tasks.await(task)
                if (doc.exists()) {
                    Asset(
                        inventoryNumber = doc.getString("inventoryNumber") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        type = doc.getString("type") ?: "",
                        location = doc.getString("location") ?: "",
                        status = doc.getString("status") ?: "",
                        description = doc.getString("description"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        acquisitionDate = doc.getLong("acquisitionDate") ?: (doc.getLong("createdAt") ?: System.currentTimeMillis()),
                        purchasePrice = doc.getDouble("purchasePrice")
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        } else {
            return dao.getAssetByInventory(invNum)
        }
    }

    suspend fun insertAsset(asset: Asset) {
        if (isFirebaseEnabled && firestore != null) {
            val data = hashMapOf(
                "inventoryNumber" to asset.inventoryNumber,
                "name" to asset.name,
                "type" to asset.type,
                "location" to asset.location,
                "status" to asset.status,
                "description" to asset.description,
                "createdAt" to asset.createdAt,
                "acquisitionDate" to asset.acquisitionDate,
                "purchasePrice" to asset.purchasePrice
            )
            firestore!!.collection("assets").document(asset.inventoryNumber).set(data)
        } else {
            dao.insertAsset(asset)
        }
    }

    suspend fun updateAsset(asset: Asset) {
        insertAsset(asset)
    }

    suspend fun deleteAsset(asset: Asset) {
        if (isFirebaseEnabled && firestore != null) {
            firestore!!.collection("assets").document(asset.inventoryNumber).delete()
        } else {
            dao.deleteAsset(asset)
        }
    }

    // --- Repair Operations ---
    fun getRepairsForAsset(invNum: String): Flow<List<Repair>> {
        return if (isFirebaseEnabled) {
            allRepairs.map { list -> list.filter { it.inventoryNumber == invNum } }
        } else {
            dao.getRepairsForAsset(invNum)
        }
    }

    fun getMaintenancesForAsset(invNum: String): Flow<List<Maintenance>> {
        return if (isFirebaseEnabled) {
            allMaintenances.map { list -> list.filter { it.inventoryNumber == invNum } }
        } else {
            dao.getMaintenancesForAsset(invNum)
        }
    }

    fun getRepairsBetween(start: Long, end: Long): Flow<List<Repair>> {
        return if (isFirebaseEnabled) {
            allRepairs.map { list -> list.filter { it.startTime in start..end } }
        } else {
            dao.getRepairsBetween(start, end)
        }
    }

    fun getMaintenancesBetween(start: Long, end: Long): Flow<List<Maintenance>> {
        return if (isFirebaseEnabled) {
            allMaintenances.map { list -> list.filter { it.startTime in start..end } }
        } else {
            dao.getMaintenancesBetween(start, end)
        }
    }

    suspend fun insertRepair(repair: Repair) {
        if (isFirebaseEnabled && firestore != null) {
            val finalId = if (repair.id == 0) (System.currentTimeMillis() % 10000000).toInt() else repair.id
            val data = hashMapOf(
                "id" to finalId.toLong(),
                "inventoryNumber" to repair.inventoryNumber,
                "startTime" to repair.startTime,
                "endTime" to repair.endTime,
                "problem" to repair.problem,
                "cause" to repair.cause,
                "actionTaken" to repair.actionTaken,
                "status" to repair.status,
                "holdReason" to repair.holdReason,
                "holdEstimate" to repair.holdEstimate,
                "photoBefore" to repair.photoBefore,
                "photoAfter" to repair.photoAfter,
                "photoUser" to repair.photoUser,
                "technician" to repair.technician
            )
            firestore!!.collection("repairs").document(finalId.toString()).set(data)
        } else {
            dao.insertRepair(repair)
        }
    }

    suspend fun updateRepair(repair: Repair) {
        insertRepair(repair)
    }

    suspend fun getRepairById(id: Int): Repair? {
        if (isFirebaseEnabled && firestore != null) {
            return try {
                val task = firestore!!.collection("repairs").document(id.toString()).get()
                val doc = com.google.android.gms.tasks.Tasks.await(task)
                if (doc.exists()) {
                    Repair(
                        id = doc.getLong("id")?.toInt() ?: id,
                        inventoryNumber = doc.getString("inventoryNumber") ?: "",
                        startTime = doc.getLong("startTime") ?: 0L,
                        endTime = doc.get("endTime")?.toString()?.toLongOrNull(),
                        problem = doc.getString("problem") ?: "",
                        cause = doc.getString("cause") ?: "",
                        actionTaken = doc.getString("actionTaken") ?: "",
                        status = doc.getString("status") ?: "",
                        holdReason = doc.getString("holdReason"),
                        holdEstimate = doc.getString("holdEstimate"),
                        photoBefore = doc.getString("photoBefore"),
                        photoAfter = doc.getString("photoAfter"),
                        photoUser = doc.getString("photoUser"),
                        technician = doc.getString("technician") ?: ""
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        } else {
            return dao.getRepairById(id)
        }
    }

    // --- Maintenance Operations ---
    suspend fun insertMaintenance(maintenance: Maintenance) {
        if (isFirebaseEnabled && firestore != null) {
            val finalId = if (maintenance.id == 0) (System.currentTimeMillis() % 10000000).toInt() else maintenance.id
            val data = hashMapOf(
                "id" to finalId.toLong(),
                "inventoryNumber" to maintenance.inventoryNumber,
                "startTime" to maintenance.startTime,
                "endTime" to maintenance.endTime,
                "actionTaken" to maintenance.actionTaken,
                "issuesFound" to maintenance.issuesFound,
                "result" to maintenance.result,
                "status" to maintenance.status,
                "photoBefore" to maintenance.photoBefore,
                "photoAfter" to maintenance.photoAfter,
                "photoUser" to maintenance.photoUser,
                "technician" to maintenance.technician
            )
            firestore!!.collection("maintenances").document(finalId.toString()).set(data)
        } else {
            dao.insertMaintenance(maintenance)
        }
    }

    suspend fun updateMaintenance(maintenance: Maintenance) {
        insertMaintenance(maintenance)
    }

    // --- Asset Update Log Operations ---
    fun getUpdateLogsForAsset(invNum: String): Flow<List<AssetUpdateLog>> {
        return if (isFirebaseEnabled && firestore != null) {
            callbackFlow {
                val listener = firestore?.collection("asset_updates")
                    ?.whereEqualTo("inventoryNumber", invNum)
                    ?.addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (snapshot != null) {
                            val list = snapshot.documents.mapNotNull { doc ->
                                try {
                                    AssetUpdateLog(
                                        id = doc.getLong("id")?.toInt() ?: doc.id.hashCode(),
                                        inventoryNumber = doc.getString("inventoryNumber") ?: "",
                                        updateTime = doc.getLong("updateTime") ?: 0L,
                                        oldLocation = doc.getString("oldLocation") ?: "",
                                        newLocation = doc.getString("newLocation") ?: "",
                                        oldStatus = doc.getString("oldStatus") ?: "",
                                        newStatus = doc.getString("newStatus") ?: "",
                                        oldDescription = doc.getString("oldDescription"),
                                        newDescription = doc.getString("newDescription"),
                                        reasonForPermanentDamage = doc.getString("reasonForPermanentDamage")
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            }.sortedByDescending { it.updateTime }
                            trySend(list)
                        }
                    }
                awaitClose { listener?.remove() }
            }
        } else {
            dao.getUpdateLogsForAsset(invNum)
        }
    }

    suspend fun insertAssetUpdateLog(log: AssetUpdateLog) {
        if (isFirebaseEnabled && firestore != null) {
            val finalId = if (log.id == 0) (System.currentTimeMillis() % 10000000).toInt() else log.id
            val data = hashMapOf(
                "id" to finalId.toLong(),
                "inventoryNumber" to log.inventoryNumber,
                "updateTime" to log.updateTime,
                "oldLocation" to log.oldLocation,
                "newLocation" to log.newLocation,
                "oldStatus" to log.oldStatus,
                "newStatus" to log.newStatus,
                "oldDescription" to log.oldDescription,
                "newDescription" to log.newDescription,
                "reasonForPermanentDamage" to log.reasonForPermanentDamage
            )
            firestore!!.collection("asset_updates").document(finalId.toString()).set(data)
        } else {
            dao.insertAssetUpdateLog(log)
        }
    }

    val allUpdateLogs: Flow<List<AssetUpdateLog>> = if (isFirebaseEnabled && firestore != null) {
        callbackFlow {
            val listener = firestore!!.collection("asset_updates")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                AssetUpdateLog(
                                    id = doc.getLong("id")?.toInt() ?: doc.id.hashCode(),
                                    inventoryNumber = doc.getString("inventoryNumber") ?: "",
                                    updateTime = doc.getLong("updateTime") ?: 0L,
                                    oldLocation = doc.getString("oldLocation") ?: "",
                                    newLocation = doc.getString("newLocation") ?: "",
                                    oldStatus = doc.getString("oldStatus") ?: "",
                                    newStatus = doc.getString("newStatus") ?: "",
                                    oldDescription = doc.getString("oldDescription"),
                                    newDescription = doc.getString("newDescription"),
                                    reasonForPermanentDamage = doc.getString("reasonForPermanentDamage")
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.sortedByDescending { it.updateTime }
                        trySend(list)
                    }
                }
            awaitClose { listener.remove() }
        }
    } else {
        dao.getAllUpdateLogs()
    }

    // --- User Operations ---
    val allUsers: Flow<List<User>> = dao.getAllUsers()

    suspend fun getUserByUsername(uname: String): User? {
        return dao.getUserByUsername(uname)
    }

    suspend fun insertUser(user: User) {
        dao.insertUser(user)
    }

    suspend fun updateUser(user: User) {
        dao.updateUser(user)
    }

    suspend fun deleteUser(user: User) {
        dao.deleteUser(user)
    }

    // --- Category Operations ---
    val allCategories: Flow<List<com.example.data.model.Category>> = dao.getAllCategories()

    suspend fun getCategoryByName(catName: String): com.example.data.model.Category? {
        return dao.getCategoryByName(catName)
    }

    suspend fun insertCategory(category: com.example.data.model.Category) {
        dao.insertCategory(category)
    }

    suspend fun updateCategory(category: com.example.data.model.Category) {
        dao.updateCategory(category)
    }

    suspend fun deleteCategory(category: com.example.data.model.Category) {
        dao.deleteCategory(category)
    }
}
