package com.example.biztrackumkm.auth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OwnerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOwner(owner: OwnerAccount): Long

    @Query("""
        SELECT * FROM owner_accounts
        WHERE username = :username
        LIMIT 1
    """)
    suspend fun getOwnerByUsername(
        username: String
    ): OwnerAccount?

    @Query("""
        SELECT * FROM owner_accounts
        WHERE role = 'OWNER'
        LIMIT 1
    """)
    suspend fun getFirstOwner(): OwnerAccount?

    @Query("SELECT COUNT(*) FROM owner_accounts WHERE role = 'OWNER'")
    suspend fun getOwnerCount(): Int

    @Query("SELECT COUNT(*) FROM owner_accounts WHERE username = :username")
    suspend fun countByUsername(username: String): Int

    @Query("SELECT * FROM owner_accounts WHERE role = 'CASHIER' ORDER BY id DESC")
    suspend fun getCashierAccounts(): List<OwnerAccount>
}
