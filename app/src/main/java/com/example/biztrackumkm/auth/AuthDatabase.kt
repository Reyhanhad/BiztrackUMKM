package com.example.biztrackumkm.auth

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [OwnerAccount::class],
    version = 1,
    exportSchema = true
)
abstract class AuthDatabase : RoomDatabase() {

    abstract fun ownerDao(): OwnerDao

    companion object {

        @Volatile
        private var INSTANCE: AuthDatabase? = null

        fun getDatabase(context: Context): AuthDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuthDatabase::class.java,
                    "biztrack_auth_database"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
