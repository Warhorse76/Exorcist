package com.example.exorcist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.exorcist.data.dao.ForensicLogDao
import com.example.exorcist.data.entity.ForensicLog
import net.sqlcipher.database.SupportFactory

@Database(entities = [ForensicLog::class], version = 1, exportSchema = false)
abstract class ExorcistDatabase : RoomDatabase() {
    abstract fun forensicLogDao(): ForensicLogDao

    companion object {
        @Volatile
        private var INSTANCE: ExorcistDatabase? = null

        // In production, this should be retrieved from Keystore
        private val PASSPHRASE = "exorcist_vault_seed_aes256".toByteArray()

        fun getDatabase(context: Context): ExorcistDatabase {
            return INSTANCE ?: synchronized(this) {
                val factory = SupportFactory(PASSPHRASE)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExorcistDatabase::class.java,
                    "exorcist_forensics.db"
                )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
