package com.edadeal.android.model.ads

import com.edadeal.android.dto.Promo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class AdPositionTest(
    private val position: Promo.Position,
    private val itemsCount: Int,
    private val offsets: List<Int>
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Any> = listOf(
            arrayOf(
                PromoBuilder().offset(1).build(),
                1,
                listOf(1)
            ),
            arrayOf(
                PromoBuilder().repeat(start = 1, step = 2, end = 10).build(),
                3,
                listOf(1, 3)
            ),
            arrayOf(
                PromoBuilder().repeat(start = 100, step = 1, end = 0).build(),
                100,
                emptyList<Int>()
            ),
            arrayOf(
                PromoBuilder().repeat(start = 0, step = 3, end = 50).dynamic().build(),
                10,
                listOf(0, 3)
            ),
            arrayOf(
                PromoBuilder().repeat(start = 50, step = 0, end = 100).dynamic().build(),
                4,
                listOf(2)
            )
        )
    }

    @Test
    fun `promo position should have correct offsets`() {
        val positionOffsets = AdPosition.from(position)
            ?.offsets(itemsCount)
            ?.toList()
            .orEmpty()

        assertEquals(offsets, positionOffsets)
    }

    class PromoBuilder {

        private var offset = 0
        private var isDynamic = false
        private var repeat: Promo.Position.Repeat? = null

        fun dynamic(): PromoBuilder = apply { isDynamic = true }
        fun offset(value: Int): PromoBuilder = apply { offset = value }

        fun repeat(
            start: Int? = null,
            end: Int? = null,
            step: Int? = null
        ): PromoBuilder {
            repeat = Promo.Position.Repeat(start = start, end = end, step = step)
            return this
        }

        fun build() = Promo.Position(
            offset = offset,
            repeat = repeat,
            dynamicOffset = isDynamic
        )
    }
}
