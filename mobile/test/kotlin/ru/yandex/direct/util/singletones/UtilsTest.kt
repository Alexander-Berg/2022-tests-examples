package ru.yandex.direct.util.singletones

import org.assertj.core.api.Assertions
import org.junit.Test

class UtilsTest {
    @Test
    fun comparing_shouldWork() {
        val first = 1 to 0
        val second = 0 to 0

        val comparatorX = Utils.comparing<Pair<Int, Int>, Int> { it.first }
        Assertions.assertThat(comparatorX.compare(first, second)).isEqualTo(1)
        Assertions.assertThat(comparatorX.compare(second, first)).isEqualTo(-1)

        val comparatorY = Utils.comparing<Pair<Int, Int>, Int> { it.second }
        Assertions.assertThat(comparatorY.compare(first, second)).isEqualTo(0)
        Assertions.assertThat(comparatorY.compare(second, first)).isEqualTo(0)
    }
}
