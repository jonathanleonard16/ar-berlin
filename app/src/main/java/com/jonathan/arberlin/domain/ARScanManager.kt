package com.jonathan.arberlin.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ARScanManager {


    private val SCAN_DURATION = 1500
    private var currentTargetId: Long? = null
    private var gazeStartTime: Long = 0L
    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress = _scanProgress.asStateFlow()

    fun processFrame(hitPoiId: Long?): Boolean {
        if (hitPoiId == null) {
            resetScan()
            return false
        }

        if (hitPoiId != currentTargetId) {
            currentTargetId = hitPoiId
            gazeStartTime = System.currentTimeMillis()
            _scanProgress.value = 0f
            return false
        }

        val elapsed = System.currentTimeMillis() - gazeStartTime
        val progress = (elapsed / SCAN_DURATION.toFloat()).coerceAtMost(1f)
        _scanProgress.value = progress

        if (elapsed >= SCAN_DURATION) {
            resetScan()
            return true
        }

        return false
    }

    private fun resetScan() {
        currentTargetId = null
        gazeStartTime = 0L
        _scanProgress.value = 0f
    }
}