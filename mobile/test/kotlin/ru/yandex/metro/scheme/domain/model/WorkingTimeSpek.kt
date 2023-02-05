package ru.yandex.metro.scheme.domain.model

import com.yandex.metrokit.DayTime
import com.yandex.metrokit.TimeZone
import com.yandex.metrokit.scheme.data.LocalWorkingTime
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.joda.time.LocalTime
import ru.yandex.metro.ClassSpek
import com.yandex.metrokit.scheme.data.WorkingTime as NativeWorkingTime

class WorkingTimeSpek : ClassSpek(WorkingTime::class.java, {

    context("opening is before closing") {
        val native = LocalWorkingTime(
                NativeWorkingTime(
                        DayTime(7, 0, 0),
                        DayTime(21, 0, 0)

                ),
                TimeZone("MSK")
        )

        describe("inside the interval") {
            val now = LocalTime(12, 30)

            it("should be working") {
                WorkingTimeByNative(native).isWorkingAt(now).shouldBeTrue()
            }
        }

        describe("outside of the time interval") {
            val now = LocalTime.MIDNIGHT

            it("should be NOT working") {
                WorkingTimeByNative(native).isWorkingAt(now).shouldBeFalse()
            }
        }
    }

    context("closing is before opening") {
        val native = LocalWorkingTime(
                NativeWorkingTime(
                        DayTime(7, 0, 0),
                        DayTime(2, 0, 0)

                ),
                TimeZone("MSK")
        )

        describe("moment before closing") {
            val now = LocalTime(1, 30)

            it("should be working") {
                WorkingTimeByNative(native).isWorkingAt(now).shouldBeTrue()
            }
        }

        describe("moment after opening") {
            val now = LocalTime(8, 0, 0)

            it("should be working") {
                WorkingTimeByNative(native).isWorkingAt(now).shouldBeTrue()
            }
        }

        describe("moment between closing and opening") {
            val now = LocalTime(4, 0, 0)

            it("should be NOT working") {
                WorkingTimeByNative(native).isWorkingAt(now).shouldBeFalse()
            }
        }
    }
})
