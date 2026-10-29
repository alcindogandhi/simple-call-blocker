package com.gandhi.simplecallblocker.data

import com.gandhi.simplecallblocker.data.db.BlockedCallDao
import com.gandhi.simplecallblocker.domain.BlockedCall
import com.gandhi.simplecallblocker.domain.CallDecision
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: BlockedCallDao) {

    val all: Flow<List<BlockedCall>> = dao.observeAll()

    fun recent(limit: Int): Flow<List<BlockedCall>> = dao.observeRecent(limit)

    fun countSince(timestamp: Long): Flow<Int> = dao.observeCountSince(timestamp)

    suspend fun record(
        number: String?,
        decision: CallDecision.Block,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        val call = BlockedCall(number = number, timestamp = timestamp, reason = decision.reason, ruleId = decision.ruleId)
        dao.insertAndTrim(call, MAX_ENTRIES)
    }

    suspend fun clear() = dao.clear()

    companion object {
        const val MAX_ENTRIES = 500
    }
}
