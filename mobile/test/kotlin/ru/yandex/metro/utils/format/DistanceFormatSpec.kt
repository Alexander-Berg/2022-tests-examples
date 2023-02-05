package ru.yandex.metro.utils.format

import android.content.res.Resources
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.mockito.ArgumentCaptor
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import ru.yandex.metro.R
import ru.yandex.metro.common.domain.model.Distance
import ru.yandex.metro.common.domain.model.DistanceUnit

class DistanceFormatSpec : Spek({
    val resources = mock<Resources> {
        val quantityCaptor = ArgumentCaptor.forClass(Any::class.java)
        on { getString(eq(R.string.format_distance_m), quantityCaptor.capture()) } doAnswer { formatMeters(quantityCaptor.value) }
        on { getString(eq(R.string.format_distance_km), quantityCaptor.capture()) } doAnswer { formatKilometers(quantityCaptor.value) }
    }

    describe("distance formatter") {
        context("input < 0 m") {
            it("should throw") {
                val throwingFunction: () -> Unit = { formatDistance(resources, Distance(-0.1, DistanceUnit.METER)) }
                throwingFunction shouldThrow IllegalArgumentException::class
            }
        }
        context("input in range [0, 10) m") {
            it("should output rounded integer meters quantity") {
                formatDistance(resources, Distance(0.0, DistanceUnit.METER)) shouldEqual formatMeters(0)
                formatDistance(resources, Distance(1.4, DistanceUnit.METER)) shouldEqual formatMeters(1)
                formatDistance(resources, Distance(1.5, DistanceUnit.METER)) shouldEqual formatMeters(2)
                formatDistance(resources, Distance(9.4, DistanceUnit.METER)) shouldEqual formatMeters(9)
            }
        }
        context("input in range [10, 995) m") {
            it("should output rounded integer multiple of 10 meters quantity") {
                formatDistance(resources, Distance(9.5, DistanceUnit.METER)) shouldEqual formatMeters(10)
                formatDistance(resources, Distance(14.0, DistanceUnit.METER)) shouldEqual formatMeters(10)
                formatDistance(resources, Distance(15.0, DistanceUnit.METER)) shouldEqual formatMeters(20)
                formatDistance(resources, Distance(994.0, DistanceUnit.METER)) shouldEqual formatMeters(990)
            }
        }
        context("input in range [995, 9950) m") {
            it("should output rounded floating with one digit after dot kilometers quantity") {
                formatDistance(resources, Distance(995.0, DistanceUnit.METER)) shouldEqual formatKilometers(1.0)
                formatDistance(resources, Distance(1000.0, DistanceUnit.METER)) shouldEqual formatKilometers(1.0)
                formatDistance(resources, Distance(1050.0, DistanceUnit.METER)) shouldEqual formatKilometers(1.1)
                formatDistance(resources, Distance(9949.0, DistanceUnit.METER)) shouldEqual formatKilometers(9.9)
            }
        }
        context("input >= 9950 m") {
            it("should output rounded integer kilometers quantity") {
                formatDistance(resources, Distance(9950.0, DistanceUnit.METER)) shouldEqual formatKilometers(10)
                formatDistance(resources, Distance(12345.0, DistanceUnit.METER)) shouldEqual formatKilometers(12)
            }
        }
    }
})

private fun formatMeters(value: Any) = "$value m"
private fun formatKilometers(value: Any) = "$value km"
