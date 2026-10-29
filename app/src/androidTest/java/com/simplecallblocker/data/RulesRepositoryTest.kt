package com.simplecallblocker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.simplecallblocker.data.db.AppDatabase
import com.simplecallblocker.domain.NumberNormalizer
import com.simplecallblocker.domain.RuleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RulesRepositoryTest {

    private lateinit var db: AppDatabase
    private var region = "BR"

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun TestScope.repository() =
        RulesRepository(db.blockRuleDao(), backgroundScope) { NumberNormalizer(region) }

    @Test
    fun regraExataEhSalvaNormalizada() = runTest {
        val repo = repository()
        val result = repo.add(RuleType.EXACT, "(11) 99999-0000", label = "  Chato  ")

        assertTrue(result is AddRuleResult.Added)
        val rule = repo.current().single()
        assertEquals("+5511999990000", rule.pattern)
        assertEquals("Chato", rule.label)
    }

    @Test
    fun mesmoNumeroEmOutroFormatoEhDuplicado() = runTest {
        val repo = repository()
        repo.add(RuleType.EXACT, "11999990000")

        assertEquals(AddRuleResult.Duplicate, repo.add(RuleType.EXACT, "+55 11 99999-0000"))
        assertEquals(1, repo.current().size)
    }

    @Test
    fun mesmoPadraoComTipoDiferenteNaoEhDuplicado() = runTest {
        val repo = repository()
        repo.add(RuleType.EXACT, "4004")

        assertTrue(repo.add(RuleType.PREFIX, "4004") is AddRuleResult.Added)
    }

    @Test
    fun entradaSemDigitosEhInvalida() = runTest {
        assertEquals(AddRuleResult.Invalid, repository().add(RuleType.PREFIX, "abc"))
    }

    @Test
    fun paisAtualEhUsadoAoNormalizar() = runTest {
        val repo = repository()
        region = "PT"
        repo.add(RuleType.EXACT, "912345678")

        assertEquals("+351912345678", repo.current().single().pattern)
    }

    @Test
    fun desativarEExcluir() = runTest {
        val repo = repository()
        val id = (repo.add(RuleType.PREFIX, "0303") as AddRuleResult.Added).id

        repo.setEnabled(id, false)
        assertFalse(repo.rules.first { rules -> rules.none { it.enabled } }.single().enabled)

        repo.delete(id)
        assertTrue(repo.rules.first { it.isEmpty() }.isEmpty())
    }
}
