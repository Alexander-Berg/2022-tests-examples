package com.yandex.mobile.realty.ui.presenter

import com.yandex.mobile.realty.RobolectricTest
import com.yandex.mobile.realty.domain.model.common.CommissioningStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * @author sorokinandrei on 8/13/21.
 */
@RunWith(RobolectricTestRunner::class)
class CommissioningStatusTest : RobolectricTest() {

    @Test
    fun testHandOver() {
        val status = CommissioningStatus.HAND_OVER
        assertEquals("Сдан", status.getShortDescription())
    }

    @Test
    fun testSuspended() {
        val status = CommissioningStatus.SUSPENDED
        assertEquals("Стройка заморожена", status.getShortDescription())
    }

    @Test
    fun testUnderConstruction() {
        val status = CommissioningStatus.UNDER_CONSTRUCTION
        assertEquals("Строится", status.getShortDescription())
    }

    @Test
    fun testUnderConstructionWithHandOver() {
        val status = CommissioningStatus.UNDER_CONSTRUCTIONS_WITH_HAND_OVER
        assertEquals("Строится, есть сданные", status.getShortDescription())
    }

    @Test
    fun testInProject() {
        val status = CommissioningStatus.IN_PROJECT
        assertEquals("В проекте", status.getShortDescription())
    }

    @Test
    fun testSoonAvailable() {
        val status = CommissioningStatus.SOON_AVAILABLE
        assertEquals("Скоро в продаже", status.getShortDescription())
    }
}
