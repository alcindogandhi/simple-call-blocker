package com.simplecallblocker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.simplecallblocker.domain.BlockedCall
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BlockedCallDao {

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC, id DESC")
    abstract fun observeAll(): Flow<List<BlockedCall>>

    @Query("SELECT * FROM blocked_calls ORDER BY timestamp DESC, id DESC LIMIT :limit")
    abstract fun observeRecent(limit: Int): Flow<List<BlockedCall>>

    @Query("SELECT COUNT(*) FROM blocked_calls WHERE timestamp >= :since")
    abstract fun observeCountSince(since: Long): Flow<Int>

    @Insert
    abstract suspend fun insert(call: BlockedCall): Long

    @Query(
        """DELETE FROM blocked_calls WHERE id NOT IN
           (SELECT id FROM blocked_calls ORDER BY timestamp DESC, id DESC LIMIT :keep)"""
    )
    abstract suspend fun trimTo(keep: Int)

    /** Insere e descarta os registros mais antigos além de [keep]. */
    @Transaction
    open suspend fun insertAndTrim(call: BlockedCall, keep: Int) {
        insert(call)
        trimTo(keep)
    }

    @Query("DELETE FROM blocked_calls")
    abstract suspend fun clear()
}
