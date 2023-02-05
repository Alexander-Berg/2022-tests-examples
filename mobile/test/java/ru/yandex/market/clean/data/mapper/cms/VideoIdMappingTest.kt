package ru.yandex.market.clean.data.mapper.cms

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.TestApplication
import ru.yandex.market.common.toxin.app.scopes.appScope
import ru.yandex.market.common.toxin.app.scopes.coreScope
import ru.yandex.market.data.videoid.mapper.VideoIdMapper
import ru.yandex.market.di.toxin.setupToxinAppScope
import ru.yandex.market.di.toxin.setupToxinCoreScope
import ru.yandex.market.safe.Safe
import toxin.Component

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class VideoIdMappingTest(
    private val url: String,
    private val expectedResult: Safe<String>?
) {
    lateinit var mapper: VideoIdMapper

    @Before
    fun setUp() {
        setupToxinCoreScope(TestApplication.instance)
        setupToxinAppScope(coreScope = coreScope)
        mapper = VideoMappingComponent().videoIdMapper()
    }

    @Test
    fun `Check mapping works as expected`() {
        if (expectedResult != null) {
            assertThat(mapper.map(url)).isEqualTo(expectedResult)
        } else {
            assertThat(mapper.map(url)).matches { it.ignoreError() == null }
        }
    }

    class VideoMappingComponent : Component(appScope) {

        fun videoIdMapper() = auto<VideoIdMapper>()
    }

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun parameters(): Iterable<Array<*>> = listOf(
            arrayOf("", null),
            arrayOf("https://www.yandex.ru", null),
            arrayOf(
                "https://www.youtube.com/watch?v=p__QLSU1fBg",
                Safe.invoke { "p__QLSU1fBg" }
            ),
            arrayOf(
                "https://WWW.YOUTUBE.COM/WATCH?v=p__QLSU1fBg",
                Safe.invoke { "p__QLSU1fBg" }
            ),
            arrayOf(
                "https://www.ssl.youtube.com/watch?v=p__QLSU1fBg",
                Safe.invoke { "p__QLSU1fBg" }
            ),
            arrayOf(
                "https://www.youtu.be/watch?v=p__QLSU1fBg",
                Safe.invoke { "p__QLSU1fBg" }
            ),
            arrayOf(
                "https://www.youtube.com/embed/p__QLSU1fBg",
                Safe.invoke { "p__QLSU1fBg" }
            ),
            arrayOf(
                "https://www.youtu.be/embed/p__QLSU1fBg",
                Safe.invoke { "p__QLSU1fBg" }
            ),
            arrayOf("https://www.youtube.com/watch?p=p__QLSU1fBg", null),
            arrayOf("https://www.youtube.com/watch?v=p__QLSU1fBg&v=1234", null),
            arrayOf("https://www.youtu.be/embed/embed/p__QLSU1fBg", null)
        )
    }
}
