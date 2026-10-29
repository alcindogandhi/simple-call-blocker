package io.github.alcindogandhi.simplecallblocker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alcindogandhi.simplecallblocker.data.db.AppDatabase
import io.github.alcindogandhi.simplecallblocker.domain.BlockReason
import io.github.alcindogandhi.simplecallblocker.domain.BlockedCall
import io.github.alcindogandhi.simplecallblocker.domain.CallDecision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: HistoryRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        repo = HistoryRepository(db.blockedCallDao())
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun registraComMotivoERegra() = runTest {
        repo.record("+5511999990000", CallDecision.Block(BlockReason.RULE_EXACT, ruleId = 7), timestamp = 1_000)
        repo.record(null, CallDecision.Block(BlockReason.HIDDEN), timestamp = 2_000)

        val calls = repo.all.first()
        assertEquals(listOf(2_000L, 1_000L), calls.map { it.timestamp })
        assertEquals(BlockedCall(id = calls[1].id, number = "+5511999990000", timestamp = 1_000,
            reason = BlockReason.RULE_EXACT, ruleId = 7), calls[1])
        assertEquals(null, calls[0].number)
    }

    @Test
    fun recentesEContagemDesde() = runTest {
        (1..6).forEach { repo.record("n$it", CallDecision.Block(BlockReason.UNKNOWN), timestamp = it * 1_000L) }

        assertEquals(listOf("n6", "n5", "n4"), repo.recent(3).first().map { it.number })
        assertEquals(3, repo.countSince(4_000).first())
    }

    @Test
    fun mantemSoOsUltimosRegistros() = runTest {
        val total = HistoryRepository.MAX_ENTRIES + 5
        (1..total).forEach { repo.record("n$it", CallDecision.Block(BlockReason.UNKNOWN), timestamp = it.toLong()) }

        val calls = repo.all.first()
        assertEquals(HistoryRepository.MAX_ENTRIES, calls.size)
        assertEquals("n$total", calls.first().number)
        assertEquals("n6", calls.last().number)
    }

    @Test
    fun limpar() = runTest {
        repo.record("n1", CallDecision.Block(BlockReason.UNKNOWN))
        repo.clear()
        assertTrue(repo.all.first().isEmpty())
    }
}
