package io.github.alcindogandhi.simplecallblocker.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.alcindogandhi.simplecallblocker.appContainer
import io.github.alcindogandhi.simplecallblocker.domain.RuleType
import kotlinx.coroutines.launch

/**
 * Comandos de desenvolvimento via adb (só no build de debug):
 *
 * ```
 * adb shell am broadcast -a io.github.alcindogandhi.simplecallblocker.debug.ADD_RULE \
 *     -p io.github.alcindogandhi.simplecallblocker --es type PREFIX --es pattern 0303
 * adb shell am broadcast -a io.github.alcindogandhi.simplecallblocker.debug.SET \
 *     -p io.github.alcindogandhi.simplecallblocker --es key block_hidden --ez value true
 * adb shell am broadcast -a io.github.alcindogandhi.simplecallblocker.debug.SET \
 *     -p io.github.alcindogandhi.simplecallblocker --es key country --es value BR
 * ```
 * Chaves de SET: blocking_enabled, block_hidden, block_unknown, notify, country.
 */
class DebugCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = context.appContainer
        val pending = goAsync()
        container.applicationScope.launch {
            try {
                val result = when (intent.action) {
                    ACTION_ADD_RULE -> container.rulesRepository.add(
                        type = RuleType.valueOf(intent.getStringExtra("type") ?: "EXACT"),
                        input = intent.getStringExtra("pattern").orEmpty(),
                        label = intent.getStringExtra("label"),
                    )
                    ACTION_SET -> set(context, intent)
                    else -> "ação desconhecida"
                }
                Log.i(TAG, "${intent.action}: $result")
                pending.resultData = result.toString()
            } catch (e: Exception) {
                Log.e(TAG, "Falha no comando ${intent.action}", e)
                pending.resultData = "erro: ${e.message}"
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun set(context: Context, intent: Intent): String {
        val settings = context.appContainer.settingsRepository
        val value = intent.getBooleanExtra("value", false)
        when (val key = intent.getStringExtra("key")) {
            "blocking_enabled" -> settings.setBlockingEnabled(value)
            "block_hidden" -> settings.setBlockHidden(value)
            "block_unknown" -> settings.setBlockUnknown(value)
            "notify" -> settings.setNotifyOnBlock(value)
            "country" -> settings.setCountryIso(intent.getStringExtra("value"))
            else -> return "chave desconhecida: $key"
        }
        return "ok"
    }

    private companion object {
        const val TAG = "DebugCommand"
        const val ACTION_ADD_RULE = "io.github.alcindogandhi.simplecallblocker.debug.ADD_RULE"
        const val ACTION_SET = "io.github.alcindogandhi.simplecallblocker.debug.SET"
    }
}
