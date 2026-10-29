package com.gandhi.simplecallblocker.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.gandhi.simplecallblocker.domain.BlockingSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.repository() = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(tmp.root, "settings.preferences_pb")
        },
    )

    @Test
    fun `sem nada salvo usa os padroes`() = runTest {
        assertEquals(BlockingSettings(), repository().settings.first())
    }

    @Test
    fun `alteracoes sao persistidas`() = runTest {
        val repo = repository()
        repo.setBlockingEnabled(false)
        repo.setBlockHidden(true)
        repo.setBlockUnknown(true)
        repo.setNotifyOnBlock(false)
        repo.setSkipCallLog(true)

        assertEquals(
            BlockingSettings(
                blockingEnabled = false,
                blockHidden = true,
                blockUnknown = true,
                notifyOnBlock = false,
                skipCallLog = true,
            ),
            repo.settings.first(),
        )
    }

    @Test
    fun `pais e salvo em maiusculas e null volta ao automatico`() = runTest {
        val repo = repository()
        repo.setCountryIso("pt")
        assertEquals("PT", repo.settings.first().countryIso)

        repo.setCountryIso(null)
        assertNull(repo.settings.first().countryIso)
    }
}
