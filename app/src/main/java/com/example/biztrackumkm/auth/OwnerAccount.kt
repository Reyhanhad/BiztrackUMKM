package com.example.biztrackumkm.auth

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "owner_accounts",
    indices = [
        Index(value = ["username"], unique = true)
    ]
)
data class OwnerAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val username: String,

    val passwordHash: String,

    val passwordSalt: String,

    val role: String = "OWNER",

    val createdAt: Long = System.currentTimeMillis()
)
