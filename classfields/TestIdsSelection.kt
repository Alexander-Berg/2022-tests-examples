package com.yandex.mobile.realty.ui.abt.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * @author misha-kozlov on 12.11.2020
 */
class TestIdsSelection(testIds: Map<String, Boolean>) {

    private val mutableState = MutableStateFlow(testIds)
    val state: StateFlow<Map<String, Boolean>>
        get() = mutableState

    fun getTestIds(): Map<String, Boolean> {
        return mutableState.value
    }

    fun toggle(testId: String) {
        val current = mutableState.value
        val checked = current[testId]
        if (checked != null) {
            mutableState.value = current + Pair(testId, !checked)
        }
    }

    fun add(testId: String) {
        val current = mutableState.value
        mutableState.value = current + Pair(testId, true)
    }

    fun clear() {
        mutableState.value = emptyMap()
    }
}
