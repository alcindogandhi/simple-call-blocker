package com.gandhi.simplecallblocker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gandhi.simplecallblocker.domain.BlockRule
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {

    @Query("SELECT * FROM block_rules ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<BlockRule>>

    /** Retorna -1 se já existir uma regra com o mesmo tipo e padrão. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(rule: BlockRule): Long

    @Query("UPDATE block_rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM block_rules WHERE id = :id")
    suspend fun delete(id: Long)
}
