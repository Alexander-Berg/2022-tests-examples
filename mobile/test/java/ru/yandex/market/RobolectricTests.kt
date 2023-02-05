package ru.yandex.market

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RobolectricTests {

    @Test
    fun `Correct application class being used for tests`() {
        assertThat(ApplicationProvider.getApplicationContext() as Context).isInstanceOf(TestApplication::class.java)
    }
}