package com.example.biztrackumkm.cashier

private const val REARM_AFTER_MISSING_MS = 300L

internal class BarcodeRepeatGate {
    private var lastCode: String? = null
    private var missingSince: Long? = null

    /** Ignore brief gaps in detection while a package is still held in front of the camera. */
    fun acceptFrame(code: String?, elapsedMillis: Long): String? {
        if (code == null) {
            if (missingSince == null) missingSince = elapsedMillis
            return null
        }

        val since = missingSince
        missingSince = null
        if (code == lastCode && (since == null || elapsedMillis - since < REARM_AFTER_MISSING_MS)) return null
        lastCode = code
        return code
    }
}
