package ru.yandex.market.util.collections

import org.junit.Assert
import org.junit.Test
import ru.yandex.market.utils.flattenGroupByDistinctKey

class FlattenGroupByDistinctKeyTest {

    data class Scientist(
        val name: String,
        val science: String
    )

    private val GAUSS = Scientist("Gauss", "math")
    private val DESCARTES = Scientist("Descartes", "math")
    private val EULER = Scientist("Euler", "math")

    private val NEWTON = Scientist("Newton", "physics")
    private val EINSTEIN = Scientist("Einstein", "physics")
    private val MAXWELL = Scientist("Maxwell", "physics")

    private val AVOGADRO = Scientist("Avogadro", "chemistry")
    private val BOR = Scientist("Bor", "chemistry")
    private val VERNADSKY = Scientist("Vernadsky", "chemistry")

    @Test
    fun `Empty result for empty collections`() {
        val listA = emptyList<Scientist>()
        val listB = emptyList<Scientist>()
        val listC = emptyList<Scientist>()
        val lists = listOf(listA, listB, listC)

        val result = flattenGroupByDistinctKey(lists, { it.science })

        Assert.assertEquals(emptyMap<Any, Any>(), result)
    }

    @Test
    fun `Result for different collection item keys`() {
        val listA = listOf(GAUSS, DESCARTES, EULER)
        val listB = listOf(NEWTON, EINSTEIN, MAXWELL)
        val listC = listOf(AVOGADRO, BOR, VERNADSKY)
        val lists = listOf(listA, listB, listC)

        val result = flattenGroupByDistinctKey(lists, { it.science })

        Assert.assertEquals(
            mapOf(
                "math" to listOf(GAUSS),
                "physics" to listOf(NEWTON),
                "chemistry" to listOf(AVOGADRO)
            ),
            result
        )
    }

    @Test
    fun `Result for same collection item keys`() {
        val listA = listOf(GAUSS, NEWTON, AVOGADRO)
        val listB = listOf(DESCARTES, EINSTEIN, BOR)
        val listC = listOf(EULER, MAXWELL, VERNADSKY)
        val lists = listOf(listA, listB, listC)

        val result = flattenGroupByDistinctKey(lists, { it.science })

        Assert.assertEquals(
            mapOf(
                "math" to listOf(GAUSS, DESCARTES, EULER),
                "physics" to listOf(NEWTON, EINSTEIN, MAXWELL),
                "chemistry" to listOf(AVOGADRO, BOR, VERNADSKY)
            ),
            result
        )
    }

    @Test
    fun `Result for two collection item keys`() {
        val listA = listOf(GAUSS, EINSTEIN, EULER)
        val listB = listOf(AVOGADRO, VERNADSKY)
        val listC = listOf(NEWTON, DESCARTES, MAXWELL)
        val lists = listOf(listA, listB, listC)

        val result = flattenGroupByDistinctKey(lists, { it.science })

        Assert.assertEquals(
            mapOf(
                "math" to listOf(GAUSS, DESCARTES),
                "physics" to listOf(EINSTEIN, NEWTON),
                "chemistry" to listOf(AVOGADRO)
            ),
            result
        )
    }

}