package ru.yandex.yandexmaps.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.yandexmaps.common.mapkit.debug.dumpThis
import ru.yandex.yandexmaps.utils.DumpUtilTest.GheoObject
import ru.yandex.yandexmaps.utils.DumpUtilTest.Phoint

class DumpUtilTest {

    @Test
    fun shouldDumpSimpleGheoObjectInSpecificFormat() {
        assertThat(gheoObject.dumpThis()).isEqualTo(gheoObjectDump)
    }

    @Test
    fun shouldDumpListOfMapAndPairOfPairsInSpecificFormat() {
        assertThat(listOfMapAndPair.dumpThis()).isEqualTo(listOfMapAndPairDump)
    }

    class Phoint(val lat: Double, val lon: Double)
    class GheoObject(val name: String, val description: String, val phoints: List<Phoint>, val type: GheoObjectType)
    enum class GheoObjectType { BUSINESS, TOPONYM }
}

private val gheoObject = GheoObject(
    "Moskva river",
    "It rises about 140 km (90 mi) west of Moscow, and flows roughly east through the Smolensk and Moscow Oblasts, passing through central Moscow.",
    listOf(
        Phoint(44.22, 123.33),
        Phoint(44.22, 123.33),
        Phoint(44.42, 123.33),
        Phoint(44.22, 123.33),
        Phoint(44.22, 123.33)
    ),
    DumpUtilTest.GheoObjectType.TOPONYM
)

private const val gheoObjectDump =
"""GheoObject(
    description = "It rises about 140 km (90 mi) west of Moscow, and flows roughly east through the Smolensk and Moscow Oblasts, passing through central Moscow."
    name = "Moskva river"
    phoints = [
        - Phoint(
            lat = 44.22
            lon = 123.33
        )
        - Phoint(
            lat = 44.22
            lon = 123.33
        )
        - Phoint(
            lat = 44.42
            lon = 123.33
        )
        - Phoint(
            lat = 44.22
            lon = 123.33
        )
        - Phoint(
            lat = 44.22
            lon = 123.33
        )
    ]
    type = TOPONYM
)
"""

private val listOfMapAndPair = listOf(
    mapOf("one" to 1, "two" to 2, "three" to 3),
    1 to 2 to 3
)

private const val listOfMapAndPairDump =
"""[
    - {
        one: 1
        two: 2
        three: 3
    }
    - Pair(
        first = Pair(
            first = 1
            second = 2
        )
        second = 3
    )
]
"""
