package com.gandhi.simplecallblocker

import android.app.Application
import android.content.Context

class SimpleCallBlockerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Injeção de dependências manual. Repositórios, banco de dados e configurações
 * serão expostos aqui a partir da etapa 3.
 */
class AppContainer(private val context: Context)

val Context.appContainer: AppContainer
    get() = (applicationContext as SimpleCallBlockerApp).container
