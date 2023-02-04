package ru.yandex.intranet.d.util

import com.google.common.collect.Streams
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.intranet.d.model.units.UnitModel
import ru.yandex.intranet.d.model.usage.HistogramBin
import ru.yandex.intranet.d.services.usage.accumulate
import ru.yandex.intranet.d.services.usage.accumulateTimeSeries
import ru.yandex.intranet.d.services.usage.convertTimeSeries
import ru.yandex.intranet.d.services.usage.histogram
import ru.yandex.intranet.d.services.usage.mean
import ru.yandex.intranet.d.services.usage.minMedianMax
import ru.yandex.intranet.d.services.usage.roundToIntegerHalfUp
import ru.yandex.intranet.d.services.usage.sumTimeSeries
import ru.yandex.intranet.d.services.usage.variance
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

/**
 * Numerical utils test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
class NumericalUtilsTest {

    @Test
    fun testConvert() {
        val fromUnit = UnitModel.builder()
            .id("fromUnitId")
            .key("fromUnitKey")
            .shortNameSingularEn("from unit")
            .shortNamePluralEn("from units")
            .longNameSingularEn("from unit")
            .longNamePluralEn("from units")
            .base(10)
            .power(0)
            .deleted(false)
            .build()
        val toUnit = UnitModel.builder()
            .id("toUnitId")
            .key("toUnitKey")
            .shortNameSingularEn("to unit")
            .shortNamePluralEn("to units")
            .longNameSingularEn("to unit")
            .longNamePluralEn("to units")
            .base(10)
            .power(-3)
            .deleted(false)
            .build()
        val actual = convertTimeSeries(mapOf(
            1L to BigDecimal.valueOf(1),
            2L to BigDecimal.valueOf(2),
            3L to BigDecimal.valueOf(3)), fromUnit, toUnit)
        Assertions.assertEquals(mapOf(
            1L to BigInteger.valueOf(1000),
            2L to BigInteger.valueOf(2000),
            3L to BigInteger.valueOf(3000)), actual)
    }

    @Test
    fun testSum() {
        Assertions.assertEquals(
            mapOf(
                1L to BigInteger.valueOf(2),
                2L to BigInteger.valueOf(4),
                3L to BigInteger.valueOf(6)
            ),
            sumTimeSeries(listOf(
            mapOf(
                1L to BigInteger.valueOf(1),
                2L to BigInteger.valueOf(2),
                3L to BigInteger.valueOf(3)
            ),
            mapOf(
                1L to BigInteger.valueOf(1),
                2L to BigInteger.valueOf(2),
                3L to BigInteger.valueOf(3)
            )))
        )
        Assertions.assertEquals(
            mapOf(
                1L to BigInteger.valueOf(1),
                2L to BigInteger.valueOf(2),
                3L to BigInteger.valueOf(3),
                4L to BigInteger.valueOf(4),
                5L to BigInteger.valueOf(5),
                6L to BigInteger.valueOf(6)
            ),
            sumTimeSeries(listOf(
                mapOf(
                    1L to BigInteger.valueOf(1),
                    2L to BigInteger.valueOf(2),
                    3L to BigInteger.valueOf(3)
                ),
                mapOf(
                    4L to BigInteger.valueOf(4),
                    5L to BigInteger.valueOf(5),
                    6L to BigInteger.valueOf(6)
                )
            ))
        )
        Assertions.assertEquals(
            mapOf(
                1L to BigInteger.valueOf(2),
                2L to BigInteger.valueOf(4),
                3L to BigInteger.valueOf(6)
            ),
            sumTimeSeries(
                mapOf(
                    1L to BigInteger.valueOf(1),
                    2L to BigInteger.valueOf(2),
                    3L to BigInteger.valueOf(3)
                ),
                mapOf(
                    1L to BigInteger.valueOf(1),
                    2L to BigInteger.valueOf(2),
                    3L to BigInteger.valueOf(3)
                ))
        )
        Assertions.assertEquals(
            mapOf(
                1L to BigInteger.valueOf(1),
                2L to BigInteger.valueOf(2),
                3L to BigInteger.valueOf(3),
                4L to BigInteger.valueOf(4),
                5L to BigInteger.valueOf(5),
                6L to BigInteger.valueOf(6)
            ),
            sumTimeSeries(
                mapOf(
                    1L to BigInteger.valueOf(1),
                    2L to BigInteger.valueOf(2),
                    3L to BigInteger.valueOf(3)
                ),
                mapOf(
                    4L to BigInteger.valueOf(4),
                    5L to BigInteger.valueOf(5),
                    6L to BigInteger.valueOf(6)
                )
            )
        )
    }

    @Test
    fun testAccumulateTimeSeries() {
        val accumulatorOne = mutableMapOf(
            1L to BigInteger.valueOf(1),
            2L to BigInteger.valueOf(2),
            3L to BigInteger.valueOf(3)
        )
        accumulateTimeSeries(
            accumulatorOne,
            mapOf(
                1L to BigInteger.valueOf(1),
                2L to BigInteger.valueOf(2),
                3L to BigInteger.valueOf(3)
            )
        )
        Assertions.assertEquals(
            mapOf(
                1L to BigInteger.valueOf(2),
                2L to BigInteger.valueOf(4),
                3L to BigInteger.valueOf(6)
            ),
            accumulatorOne
        )
        val accumulatorTwo = mutableMapOf(
            1L to BigInteger.valueOf(1),
            2L to BigInteger.valueOf(2),
            3L to BigInteger.valueOf(3)
        )
        accumulateTimeSeries(
            accumulatorTwo,
            mapOf(
                4L to BigInteger.valueOf(4),
                5L to BigInteger.valueOf(5),
                6L to BigInteger.valueOf(6)
            )
        )
        Assertions.assertEquals(
            mapOf(
                1L to BigInteger.valueOf(1),
                2L to BigInteger.valueOf(2),
                3L to BigInteger.valueOf(3),
                4L to BigInteger.valueOf(4),
                5L to BigInteger.valueOf(5),
                6L to BigInteger.valueOf(6)
            ),
            accumulatorTwo
        )
    }

    @Test
    fun testRound() {
        Assertions.assertEquals(BigInteger.valueOf(0), roundToIntegerHalfUp(BigDecimal.valueOf(0.0)))
        Assertions.assertEquals(BigInteger.valueOf(0), roundToIntegerHalfUp(BigDecimal.valueOf(0.1)))
        Assertions.assertEquals(BigInteger.valueOf(1), roundToIntegerHalfUp(BigDecimal.valueOf(0.5)))
        Assertions.assertEquals(BigInteger.valueOf(1), roundToIntegerHalfUp(BigDecimal.valueOf(0.9)))
        Assertions.assertEquals(BigInteger.valueOf(1), roundToIntegerHalfUp(BigDecimal.valueOf(1.0)))
    }

    @Test
    fun testMean() {
        Assertions.assertTrue(BigDecimal.valueOf(2).compareTo(
            mean(listOf(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3)))) == 0)
    }

    @Test
    fun testVariance() {
        Assertions.assertTrue(BigDecimal.valueOf(0).compareTo(variance(listOf(), mean(listOf()))) == 0)
        val values = listOf(BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3))
        Assertions.assertTrue(BigDecimal.valueOf(1).compareTo(variance(values, mean(values))) == 0)
    }

    @Test
    fun testMinMedianMax() {
        Assertions.assertEquals(Triple(BigInteger.valueOf(1L), BigDecimal.valueOf(2L), BigInteger.valueOf(3L)),
            minMedianMax(listOf(BigInteger.valueOf(1L), BigInteger.valueOf(2L), BigInteger.valueOf(3L))))
        Assertions.assertEquals(Triple(BigInteger.valueOf(1L), BigDecimal.valueOf(2.5).setScale(34),
            BigInteger.valueOf(4L)), minMedianMax(listOf(BigInteger.valueOf(1L), BigInteger.valueOf(2L),
            BigInteger.valueOf(3L), BigInteger.valueOf(4L))))
        Assertions.assertEquals(Triple(BigInteger.valueOf(1L), BigDecimal.valueOf(1L), BigInteger.valueOf(1L)),
            minMedianMax(listOf(BigInteger.valueOf(1L))))
    }

    @Test
    fun testHistogram() {
        Assertions.assertEquals(listOf(HistogramBin(BigInteger.valueOf(1L), BigInteger.valueOf(1L), 1)),
            histogram(listOf(BigInteger.valueOf(1L)), BigInteger.valueOf(1L), BigInteger.valueOf(1L)))
        Assertions.assertEquals(listOf(HistogramBin(BigInteger.valueOf(1L), BigInteger.valueOf(1L), 2)),
            histogram(listOf(BigInteger.valueOf(1L), BigInteger.valueOf(1L)), BigInteger.valueOf(1L),
                BigInteger.valueOf(1L)))
        Assertions.assertEquals(listOf(HistogramBin(BigInteger.valueOf(1L), BigInteger.valueOf(2L), 1),
            HistogramBin(BigInteger.valueOf(2L), BigInteger.valueOf(3L), 1)),
            histogram(listOf(BigInteger.valueOf(1L), BigInteger.valueOf(2L)), BigInteger.valueOf(1L),
                BigInteger.valueOf(2L)))
        val valuesOne = Streams.concat(Collections.nCopies(1000, BigInteger.valueOf(1L)).stream(),
            Collections.nCopies(1000, BigInteger.valueOf(2L)).stream()).toList()
        Assertions.assertEquals(listOf(HistogramBin(BigInteger.valueOf(1L), BigInteger.valueOf(2L), 1000),
            HistogramBin(BigInteger.valueOf(2L), BigInteger.valueOf(3L), 1000)),
            histogram(valuesOne, BigInteger.valueOf(1L), BigInteger.valueOf(2L)))
        val valuesTwo = listOf(BigInteger.valueOf(1L), BigInteger.valueOf(2L), BigInteger.valueOf(3L),
            BigInteger.valueOf(4L), BigInteger.valueOf(5L), BigInteger.valueOf(6L), BigInteger.valueOf(7L),
            BigInteger.valueOf(8L))
        Assertions.assertEquals(listOf(HistogramBin(BigInteger.valueOf(1L), BigInteger.valueOf(3L), 2),
            HistogramBin(BigInteger.valueOf(3L), BigInteger.valueOf(5L), 2),
            HistogramBin(BigInteger.valueOf(5L), BigInteger.valueOf(7L), 2),
            HistogramBin(BigInteger.valueOf(7L), BigInteger.valueOf(9L), 2)),
            histogram(valuesTwo, BigInteger.valueOf(1L), BigInteger.valueOf(8L)))

    }

    @Test
    fun testAccumulate() {
        Assertions.assertEquals(Pair(BigDecimal.ZERO, 0L), accumulate(mapOf(), 300))
        Assertions.assertEquals(Pair(BigDecimal.valueOf(600), 300L),
            accumulate(mapOf(0L to BigInteger.valueOf(2)), 300))
        Assertions.assertEquals(Pair(BigDecimal.valueOf(600).setScale(34), 300L),
            accumulate(mapOf(0L to BigInteger.valueOf(2), 300L to BigInteger.valueOf(2)), 300))
        Assertions.assertEquals(Pair(BigDecimal.valueOf(1200).setScale(34), 600L),
            accumulate(mapOf(0L to BigInteger.valueOf(2), 300L to BigInteger.valueOf(2),
                600L to BigInteger.valueOf(2)), 300))
        Assertions.assertEquals(Pair(BigDecimal.valueOf(1200).setScale(34), 600L),
            accumulate(mapOf(0L to BigInteger.valueOf(1), 300L to BigInteger.valueOf(2),
                600L to BigInteger.valueOf(3)), 300))
        Assertions.assertEquals(Pair(BigDecimal.valueOf(3300).setScale(34), 1500L),
            accumulate(mapOf(0L to BigInteger.valueOf(1), 300L to BigInteger.valueOf(2),
                600L to BigInteger.valueOf(3), 900L to BigInteger.valueOf(3), 1200L to BigInteger.valueOf(2),
                1500L to BigInteger.valueOf(1)), 300))
        Assertions.assertEquals(Pair(BigDecimal.valueOf(1200), 600L),
            accumulate(mapOf(0L to BigInteger.valueOf(2), 600L to BigInteger.valueOf(2)), 300))
        Assertions.assertEquals(Pair(BigDecimal.valueOf(1000).setScale(34), 500L),
            accumulate(mapOf(0L to BigInteger.valueOf(2), 500L to BigInteger.valueOf(2)), 300))
        Assertions.assertEquals(Pair(BigDecimal.valueOf(400).setScale(34), 200L),
            accumulate(mapOf(0L to BigInteger.valueOf(2), 200L to BigInteger.valueOf(2)), 300))
        Assertions.assertEquals(Pair(BigDecimal.valueOf(1200).setScale(34), 600L),
            accumulate(mapOf(0L to BigInteger.valueOf(1), 300L to BigInteger.valueOf(2),
                900L to BigInteger.valueOf(3), 1200L to BigInteger.valueOf(2)), 300))
    }

}
