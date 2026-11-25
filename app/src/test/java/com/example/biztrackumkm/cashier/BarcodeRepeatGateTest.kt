package com.example.biztrackumkm.cashier

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeRepeatGateTest {
    @Test fun heldBarcodeNeedsToLeaveTheCameraBeforeItCountsAgain() {
        val gate = BarcodeRepeatGate()
        assertEquals("001234", gate.acceptFrame("001234", 0))
        assertNull(gate.acceptFrame("001234", 3_000))
        assertNull(gate.acceptFrame(null, 3_100))
        assertNull(gate.acceptFrame("001234", 3_200))

        assertNull(gate.acceptFrame(null, 3_300))
        assertNull(gate.acceptFrame(null, 3_500))
        assertEquals("001234", gate.acceptFrame("001234", 3_600))
        assertNull(gate.acceptFrame("001234", 3_650))
    }

    @Test fun differentBarcodeDoesNotWaitForACooldown() {
        val gate = BarcodeRepeatGate()
        assertEquals("001234", gate.acceptFrame("001234", 0))
        assertEquals("1234", gate.acceptFrame("1234", 33))
        assertNull(gate.acceptFrame("1234", 66))
    }
}
