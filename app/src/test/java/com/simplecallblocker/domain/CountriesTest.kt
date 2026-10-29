package com.simplecallblocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CountriesTest {

    private val ptBr = Locale.forLanguageTag("pt-BR")

    @Test
    fun `lista inclui o Brasil com o DDI`() {
        val brasil = availableCountries(ptBr).single { it.iso == "BR" }
        assertEquals(Country("BR", "Brasil", 55), brasil)
    }

    @Test
    fun `lista esta em ordem alfabetica ignorando acentos`() {
        val names = availableCountries(ptBr).map { it.name }
        assertTrue(names.indexOf("África do Sul") < names.indexOf("Alemanha"))
    }

    @Test
    fun `nome do pais no idioma pedido`() {
        assertEquals("Portugal", countryName("PT", ptBr))
        assertEquals("Germany", countryName("DE", Locale.ENGLISH))
    }
}
