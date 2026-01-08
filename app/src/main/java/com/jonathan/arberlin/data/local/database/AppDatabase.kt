package com.jonathan.arberlin.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jonathan.arberlin.data.local.entity.Discovery
import com.jonathan.arberlin.data.local.entity.PointOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Database(entities = [PointOfInterest::class, Discovery::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun poiDao(): PoiDao

    companion object {
        @Volatile
        private var databaseInstance: AppDatabase? = null

        fun getDatabaseInstance(mContext: Context): AppDatabase =
            databaseInstance ?: synchronized(this) {
                databaseInstance ?: buildDatabaseInstance(mContext).also {
                    databaseInstance = it
                }
            }

        private fun buildDatabaseInstance(context: Context) =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ar_berlin_db")
                .fallbackToDestructiveMigration()
                .addCallback(object: Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getDatabaseInstance(context).poiDao().insertAll(InitialData.poiList)
                        }
                    }
                })
                .build()
    }
}