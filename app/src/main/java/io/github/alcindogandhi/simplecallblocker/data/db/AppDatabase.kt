package io.github.alcindogandhi.simplecallblocker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.alcindogandhi.simplecallblocker.domain.BlockRule
import io.github.alcindogandhi.simplecallblocker.domain.BlockedCall

@Database(entities = [BlockRule::class, BlockedCall::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockRuleDao(): BlockRuleDao
    abstract fun blockedCallDao(): BlockedCallDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "call_blocker.db").build()
    }
}
