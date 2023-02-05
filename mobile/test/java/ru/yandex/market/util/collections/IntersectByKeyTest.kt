package ru.yandex.market.util.collections

import org.junit.Assert
import org.junit.Test
import ru.yandex.market.utils.intersectByKey

class IntersectByKeyTest {

    data class Scientist(
        val name: String,
        val science: String
    )

    private val keyProvider: (Scientist) -> String = { it.science }
    private val collapseFunction: (List<Scientist>) -> Scientist = { scientists ->
        val newName = scientists.map { it.name }
            .joinToString(separator = "/") { it }
        scientists.first().copy(
            name = newName
        )
    }

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

        val result = intersectByKey(lists, keyProvider, collapseFunction)

        Assert.assertEquals(emptyList<Scientist>(), result)
    }

    @Test
    fun `Result for different collection item keys`() {
        val listA = listOf(GAUSS, DESCARTES, EULER)
        val listB = listOf(NEWTON, EINSTEIN, MAXWELL)
        val listC = listOf(AVOGADRO, BOR, VERNADSKY)
        val lists = listOf(listA, listB, listC)

        val result = intersectByKey(lists, keyProvider, collapseFunction)

        Assert.assertEquals(
            emptyList<Scientist>(),
            result
        )
    }

    @Test
    fun `Result for same collection item keys`() {
        val listA = listOf(GAUSS, NEWTON, AVOGADRO)
        val listB = listOf(DESCARTES, EINSTEIN, BOR)
        val listC = listOf(EULER, MAXWELL, VERNADSKY)
        val lists = listOf(listA, listB, listC)

        val result = intersectByKey(lists, keyProvider, collapseFunction)

        Assert.assertEquals(
            listOf(
                Scientist("Avogadro/Bor/Vernadsky", "chemistry"),
                Scientist("Newton/Einstein/Maxwell", "physics"),
                Scientist("Gauss/Descartes/Euler", "math")
            ),
            result
        )
    }

    @Test
    fun `Result for two collection item keys, without intersect with third`() {
        val listA = listOf(GAUSS, EINSTEIN, EULER)
        val listB = listOf(AVOGADRO, VERNADSKY)
        val listC = listOf(NEWTON, DESCARTES, MAXWELL)
        val lists = listOf(listA, listB, listC)

        val result = intersectByKey(lists, keyProvider, collapseFunction)

        Assert.assertEquals(
            emptyList<Scientist>(),
            result
        )
    }

}