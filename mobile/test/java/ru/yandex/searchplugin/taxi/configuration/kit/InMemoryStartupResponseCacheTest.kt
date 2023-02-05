/*
 * This file is a part of the Yandex Search for Android project.
 *
 * (C) Copyright 2020. Yandex, LLC. All rights reserved.
 *
 * Author: Anton Rychagov <arychagov@yandex-team.ru>
 */

package ru.yandex.searchplugin.taxi.configuration.kit

import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryStartupResponseCacheTest {

    private val underTest = InMemoryStartupResponseCache()

    @Test
    fun saveAndGet() {
        // arrange
        val response = mock<StartupResponse>()
        val validUntil = Long.MAX_VALUE

        // act
        underTest.save(response)

        // assert
        assertEquals(response, underTest.get(validUntil)!!.startupResponse)
    }


    @Test
    fun saveAndGet_ignoresTimeBoundaries() {
        // arrange
        val response = mock<StartupResponse>()
        val validUntil = Long.MAX_VALUE

        // act
        underTest.save(response)

        // assert
        assertEquals(response, underTest.get(validUntil)!!.startupResponse)
    }
}
