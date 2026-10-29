package com.simplecallblocker.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import com.simplecallblocker.AppContainer
import com.simplecallblocker.appContainer
import com.simplecallblocker.domain.BlockingSettings
import com.simplecallblocker.domain.CallDecision
import com.simplecallblocker.domain.CallDecisionEngine
import com.simplecallblocker.domain.CallInfo
import com.simplecallblocker.domain.NumberNormalizer
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

            respondToCall(callDetails, blockResponse(screening.settings))
            onBlocked(container, call, decision, screening)
        }
    }

    private suspend fun screen(container: AppContainer, call: CallInfo): Screening {
        val settings = container.settingsRepository.settings.first()
        val rules = container.rulesRepository.current()
        val normalizer = container.normalizerFor(settings)
        val contacts = ContactsLookup(this)
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

    private fun blockResponse(settings: BlockingSettings): CallResponse =
        CallResponse.Builder()
            .setDisallowCall(true)
            .setRejectCall(true)
            .setSkipCallLog(settings.skipCallLog)
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
