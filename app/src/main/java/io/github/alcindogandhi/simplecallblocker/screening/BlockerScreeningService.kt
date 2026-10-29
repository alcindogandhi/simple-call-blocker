package io.github.alcindogandhi.simplecallblocker.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import io.github.alcindogandhi.simplecallblocker.AppContainer
import io.github.alcindogandhi.simplecallblocker.appContainer
import io.github.alcindogandhi.simplecallblocker.domain.BlockingSettings
import io.github.alcindogandhi.simplecallblocker.domain.CallDecision
import io.github.alcindogandhi.simplecallblocker.domain.CallDecisionEngine
import io.github.alcindogandhi.simplecallblocker.domain.CallInfo
import io.github.alcindogandhi.simplecallblocker.domain.NumberNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Chamado pelo sistema para cada chamada enquanto o app tiver o papel ROLE_CALL_SCREENING.
 * Falha aberta: erro ou demora resultam em chamada permitida.
 */
class BlockerScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val container = appContainer
        container.applicationScope.launch(Dispatchers.IO) {
            val call = callDetails.toCallInfo()
            val screening = try {
                withTimeoutOrNull(RESPONSE_TIMEOUT_MS) { screen(container, call) }
            } catch (e: Exception) {
                Log.w(TAG, "Falha na triagem; chamada permitida", e)
                null
            }

            val decision = screening?.decision
            if (screening == null || decision !is CallDecision.Block) {
                respondToCall(callDetails, CallResponse.Builder().build())
                return@launch
            }

            respondToCall(callDetails, blockResponse)
            onBlocked(container, call, decision, screening)
        }
    }

    private suspend fun screen(container: AppContainer, call: CallInfo): Screening {
        val contacts = ContactsLookup(this)
        // Sem acesso aos contatos "fora dos contatos" não tem efeito (é assim que a tela também o mostra),
        // nem arrasta o bloqueio de ocultos.
        val settings = container.settingsRepository.settings.first()
            .let { if (it.blockUnknown && !contacts.hasPermission()) it.copy(blockUnknown = false) else it }
        val rules = container.rulesRepository.current()
        val normalizer = container.normalizerFor(settings)
        val decision = CallDecisionEngine(normalizer).decide(call, rules, settings, contacts::isContact)
        return Screening(decision, settings, normalizer)
    }

    private suspend fun onBlocked(
        container: AppContainer,
        call: CallInfo,
        decision: CallDecision.Block,
        screening: Screening,
    ) {
        val number = call.rawNumber?.takeIf { call.isPresentationAllowed && it.isNotBlank() }
            ?.let(screening.normalizer::canonical)
        container.historyRepository.record(number, decision)
        if (screening.settings.notifyOnBlock) {
            BlockNotifier(this).notifyBlocked(number?.let(screening.normalizer::formatForDisplay), decision.reason)
        }
    }

    // Sem setSkipCallLog: a partir do Android 10 chamadas recusadas pela triagem são sempre
    // registradas como BLOCKED_TYPE no registro do sistema, então a opção não teria efeito.
    private val blockResponse: CallResponse = CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipNotification(true)
        .build()

    private fun Call.Details.toCallInfo() = CallInfo(
        rawNumber = handle?.schemeSpecificPart,
        isIncoming = callDirection == Call.Details.DIRECTION_INCOMING,
        isPresentationAllowed = handlePresentation == TelecomManager.PRESENTATION_ALLOWED,
    )

    private class Screening(
        val decision: CallDecision,
        val settings: BlockingSettings,
        val normalizer: NumberNormalizer,
    )

    private companion object {
        const val TAG = "BlockerScreening"

        // O sistema desiste da triagem após ~5 s; respondemos antes disso.
        const val RESPONSE_TIMEOUT_MS = 3_000L
    }
}
