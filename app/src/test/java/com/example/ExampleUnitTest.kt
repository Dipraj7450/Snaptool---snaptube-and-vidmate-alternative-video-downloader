package com.example

import com.example.data.crypto.VaultCrypto
import com.example.ui.i18n.StringsProvider
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testVaultPinHashingAndVerification() {
        val pin = "1234"
        val hash = VaultCrypto.hashPin(pin)
        assertTrue(VaultCrypto.verifyPin("1234", hash))
        assertFalse(VaultCrypto.verifyPin("4321", hash))
    }

    @Test
    fun testMultiLanguageStrings() {
        val en = StringsProvider.getStrings("en")
        assertEquals("SnapLoad", en.appName)
        assertEquals("Browser", en.navBrowser)

        val es = StringsProvider.getStrings("es")
        assertEquals("Navegador", es.navBrowser)

        val hi = StringsProvider.getStrings("hi")
        assertEquals("ब्राउज़र", hi.navBrowser)
    }
}
