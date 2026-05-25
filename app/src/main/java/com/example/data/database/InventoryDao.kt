package com.example.data.database

import androidx.room.*
import com.example.data.model.Asset
import com.example.data.model.Repair
import com.example.data.model.Maintenance
import com.example.data.model.AssetUpdateLog
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    // --- Asset Queries ---
    @Query("SELECT * FROM assets ORDER BY createdAt DESC")
    fun getAllAssets(): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE inventoryNumber = :invNum LIMIT 1")
    suspend fun getAssetByInventory(invNum: String): Asset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: Asset)

    @Update
    suspend fun updateAsset(asset: Asset)

    @Delete
    suspend fun deleteAsset(asset: Asset)

    // --- Repair Queries ---
    @Query("SELECT * FROM repairs ORDER BY startTime DESC")
    fun getAllRepairs(): Flow<List<Repair>>

    @Query("SELECT * FROM repairs WHERE inventoryNumber = :invNum ORDER BY startTime DESC")
    fun getRepairsForAsset(invNum: String): Flow<List<Repair>>

    @Query("SELECT * FROM repairs WHERE startTime >= :start AND startTime <= :end ORDER BY startTime DESC")
    fun getRepairsBetween(start: Long, end: Long): Flow<List<Repair>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepair(repair: Repair)

    @Update
    suspend fun updateRepair(repair: Repair)

    @Query("SELECT * FROM repairs WHERE id = :id LIMIT 1")
    suspend fun getRepairById(id: Int): Repair?

    // --- Maintenance Queries ---
    @Query("SELECT * FROM maintenances ORDER BY startTime DESC")
    fun getAllMaintenances(): Flow<List<Maintenance>>

    @Query("SELECT * FROM maintenances WHERE inventoryNumber = :invNum ORDER BY startTime DESC")
    fun getMaintenancesForAsset(invNum: String): Flow<List<Maintenance>>

    @Query("SELECT * FROM maintenances WHERE startTime >= :start AND startTime <= :end ORDER BY startTime DESC")
    fun getMaintenancesBetween(start: Long, end: Long): Flow<List<Maintenance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(maintenance: Maintenance)

    @Update
    suspend fun updateMaintenance(maintenance: Maintenance)

    // --- Asset Update Log Queries ---
    @Query("SELECT * FROM asset_updates WHERE inventoryNumber = :invNum ORDER BY updateTime DESC")
    fun getUpdateLogsForAsset(invNum: String): Flow<List<AssetUpdateLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetUpdateLog(log: AssetUpdateLog)

    // --- User Queries ---
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :uname LIMIT 1")
    suspend fun getUserByUsername(uname: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}
