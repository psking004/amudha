package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [UserProfile::class, DataBrokerItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "selfcheck_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            getDatabase(context, scope).appDao().insertBrokers(
                                listOf(
                                    DataBrokerItem(
                                        id = "spokeo",
                                        name = "Spokeo",
                                        optOutUrl = "https://www.spokeo.com/optout",
                                        isListed = false,
                                        notes = "Aggregates social networks, phone numbers, and public records."
                                    ),
                                    DataBrokerItem(
                                        id = "whitepages",
                                        name = "Whitepages",
                                        optOutUrl = "https://www.whitepages.com/suppression-requests",
                                        isListed = false,
                                        notes = "Displays contact information, home addresses, and background summaries."
                                    ),
                                    DataBrokerItem(
                                        id = "beenverified",
                                        name = "BeenVerified",
                                        optOutUrl = "https://www.beenverified.com/app/optout/search",
                                        isListed = false,
                                        notes = "Provides detailed public records, phone lookups, and property deeds."
                                    )
                                )
                            )
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
