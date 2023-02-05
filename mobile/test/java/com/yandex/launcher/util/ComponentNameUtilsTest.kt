package com.yandex.launcher.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.yandex.launcher.common.util.toComponentName
import com.yandex.launcher.BaseRobolectricTest
import org.junit.Test

class ComponentNameUtilsTest : BaseRobolectricTest() {
    @Test
    fun `toComponentName returns correct componentName`() {
        assertThat(toComponentName("pack1", "class1"), equalTo("{pack1/class1}"))
    }
}