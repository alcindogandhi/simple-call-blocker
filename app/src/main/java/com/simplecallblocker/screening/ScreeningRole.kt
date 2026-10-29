package com.simplecallblocker.screening

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent

enum class ScreeningRoleStatus { HELD, NOT_HELD, UNAVAILABLE }

/**
 * Papel ROLE_CALL_SCREENING ("app de identificação de chamadas e spam"). Sem ele o sistema
 * não consulta o [BlockerScreeningService].
 */
class ScreeningRole(context: Context) {

    private val roleManager = context.getSystemService(RoleManager::class.java)

    fun status(): ScreeningRoleStatus = when {
        roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) ->
            ScreeningRoleStatus.UNAVAILABLE
        roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) -> ScreeningRoleStatus.HELD
        else -> ScreeningRoleStatus.NOT_HELD
    }

    fun requestIntent(): Intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
}
