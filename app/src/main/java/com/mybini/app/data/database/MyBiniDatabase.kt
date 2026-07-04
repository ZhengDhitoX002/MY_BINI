package com.mybini.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [DownloadItem::class, VaultItem::class], version = 1, exportSchema = false)
abstract class MyBiniDatabase : RoomDatabase() {
    
    abstract fun downloadDao(): DownloadDao
    abstract fun vaultDao(): VaultDao
    
    companion object {
        @Volatile
        private var INSTANCE: MyBiniDatabase? = null

        fun getDatabase(context: Context, passphrase: ByteArray, dbName: String): MyBiniDatabase {
            return INSTANCE ?: synchronized(this) {
                // Inisialisasi SQLCipher native libraries bawaan net.zetetic
                System.loadLibrary("sqlcipher")
                
                val factory = SupportOpenHelperFactory(passphrase)
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyBiniDatabase::class.java,
                    dbName
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
