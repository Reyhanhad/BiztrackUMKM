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
        LIMIT 1
    """)
    suspend fun getFirstOwner(): OwnerAccount?

    @Query("SELECT COUNT(*) FROM owner_accounts")
    suspend fun getOwnerCount(): Int
}
