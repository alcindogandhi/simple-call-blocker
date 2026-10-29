package com.gandhi.simplecallblocker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gandhi.simplecallblocker.domain.BlockRule
import com.gandhi.simplecallblocker.domain.BlockedCall

@Database(entities = [BlockRule::class, BlockedCall::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockRuleDao(): BlockRuleDao
    abstract fun blockedCallDao(): BlockedCallDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "call_blocker.db").build()
    }
}
