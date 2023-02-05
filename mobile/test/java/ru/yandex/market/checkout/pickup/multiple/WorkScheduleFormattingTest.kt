package ru.yandex.market.checkout.pickup.multiple

import android.os.Build
import dagger.MembersInjector
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.clean.data.mapper.WorkScheduleMapper
import ru.yandex.market.clean.data.model.dto.BreakIntervalDto
import ru.yandex.market.clean.data.model.dto.OpenHoursDto
import ru.yandex.market.di.TestScope
import javax.inject.Inject

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class WorkScheduleFormattingTest(
    private val input: List<OpenHoursDto>?,
    private val expectedOutput: List<WorkScheduleVo>
) {

    @Inject
    lateinit var workScheduleMapper: WorkScheduleMapper

    @Inject
    lateinit var workScheduleFormatter: WorkScheduleFormatter

    @Before
    fun setUp() {
        DaggerWorkScheduleFormattingTest_Component.builder()
            .testComponent(TestApplication.instance.component)
            .build()
            .injectMembers(this)
    }

    @dagger.Component(dependencies = [TestComponent::class])
    @TestScope
    interface Component : MembersInjector<WorkScheduleFormattingTest>

    @Test
    fun `Maps working schedule as expected`() {
        val workSchedule = workScheduleMapper.map(input).orThrow
        val formatted = workScheduleFormatter.formatWorkSchedule(workSchedule)
        assertThat(formatted).isEqualTo(expectedOutput)
    }

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun parameters(): Iterable<Array<*>> = listOf(

            // 0
            arrayOf(
                listOf(
                    OpenHoursDto.testBuilder()
                        .startDay("1")
                        .endDay("7")
                        .startTime("10:00")
                        .endTime("20:45")
                        .breaks(
                            listOf(
                                BreakIntervalDto.testBuilder()
                                    .startTime("13:30")
                                    .endTime("14:30")
                                    .build()
                            )
                        )
                        .build()
                ),
                listOf(
                    WorkScheduleVo(
                        "Ежедневно",
                        "10:00\u200A\u2013\u200A20:45, перерыв\u00A013:30\u200A\u2013\u200A14:30"
                    )
                )
            ),

            // 1
            arrayOf(
                listOf(
                    OpenHoursDto.testBuilder()
                        .startDay("1")
                        .endDay("4")
                        .startTime("10:00")
                        .endTime("20:45")
                        .breaks(emptyList())
                        .build()
                ),
                listOf(
                    WorkScheduleVo("Понедельник", "10:00\u200A\u2013\u200A20:45"),
                    WorkScheduleVo("Вторник", "10:00\u200A\u2013\u200A20:45"),
                    WorkScheduleVo("Среда", "10:00\u200A\u2013\u200A20:45"),
                    WorkScheduleVo("Четверг", "10:00\u200A\u2013\u200A20:45"),
                    WorkScheduleVo("Пятница", "Выходной"),
                    WorkScheduleVo("Суббота", "Выходной"),
                    WorkScheduleVo("Воскресенье", "Выходной")
                )
            ),

            // 2
            arrayOf(
                listOf(
                    OpenHoursDto.testBuilder()
                        .startDay("1")
                        .endDay("7")
                        .startTime("00:00")
                        .endTime("24:00")
                        .breaks(emptyList())
                        .build()
                ),
                listOf(
                    WorkScheduleVo("Ежедневно", "Круглосуточно")
                )
            ),

            // 3
            arrayOf(
                listOf(
                    OpenHoursDto.testBuilder()
                        .startDay("1")
                        .endDay("5")
                        .startTime("10:00")
                        .endTime("20:00")
                        .breaks(emptyList())
                        .build()
                ),
                listOf(
                    WorkScheduleVo("Будни", "10:00\u200A\u2013\u200A20:00"),
                    WorkScheduleVo("Суббота", "Выходной"),
                    WorkScheduleVo("Воскресенье", "Выходной")
                )
            ),

            // 4
            arrayOf(
                listOf(
                    OpenHoursDto.testBuilder()
                        .startDay("1")
                        .endDay("5")
                        .startTime("10:00")
                        .endTime("20:00")
                        .breaks(emptyList())
                        .build(),

                    OpenHoursDto.testBuilder()
                        .startDay("6")
                        .endDay("6")
                        .startTime("10:00")
                        .endTime("18:00")
                        .breaks(emptyList())
                        .build()
                ),
                listOf(
                    WorkScheduleVo("Будни", "10:00\u200A\u2013\u200A20:00"),
                    WorkScheduleVo("Суббота", "10:00\u200A\u2013\u200A18:00"),
                    WorkScheduleVo("Воскресенье", "Выходной")
                )
            ),

            // 5
            arrayOf(
                listOf(
                    OpenHoursDto.testBuilder()
                        .startDay("1")
                        .endDay("5")
                        .startTime("10:00")
                        .endTime("20:00")
                        .breaks(emptyList())
                        .build(),

                    OpenHoursDto.testBuilder()
                        .startDay("6")
                        .endDay("6")
                        .startTime("10:00")
                        .endTime("18:00")
                        .breaks(emptyList())
                        .build(),

                    OpenHoursDto.testBuilder()
                        .startDay("7")
                        .endDay("7")
                        .startTime("10:00")
                        .endTime("18:00")
                        .breaks(emptyList())
                        .build()
                ),
                listOf(
                    WorkScheduleVo("Будни", "10:00\u200A\u2013\u200A20:00"),
                    WorkScheduleVo("Суббота", "10:00\u200A\u2013\u200A18:00"),
                    WorkScheduleVo("Воскресенье", "10:00\u200A\u2013\u200A18:00")
                )
            ),

            // 6
            arrayOf(
                listOf(
                    OpenHoursDto.testBuilder()
                        .startDay("1")
                        .endDay("1")
                        .startTime("10:00")
                        .endTime("20:00")
                        .breaks(emptyList())
                        .build(),

                    OpenHoursDto.testBuilder()
                        .startDay("2")
                        .endDay("7")
                        .startTime("10:00")
                        .endTime("21:00")
                        .breaks(emptyList())
                        .build()
                ),
                listOf(
                    WorkScheduleVo("Понедельник", "10:00\u200A\u2013\u200A20:00"),
                    WorkScheduleVo("Вторник", "10:00\u200A\u2013\u200A21:00"),
                    WorkScheduleVo("Среда", "10:00\u200A\u2013\u200A21:00"),
                    WorkScheduleVo("Четверг", "10:00\u200A\u2013\u200A21:00"),
                    WorkScheduleVo("Пятница", "10:00\u200A\u2013\u200A21:00"),
                    WorkScheduleVo("Суббота", "10:00\u200A\u2013\u200A21:00"),
                    WorkScheduleVo("Воскресенье", "10:00\u200A\u2013\u200A21:00")
                )
            ),

            // 7
            arrayOf(
                listOf(
                    OpenHoursDto.testBuilder()
                        .startDay("1")
                        .endDay("7")
                        .startTime("10:00")
                        .endTime("23:59")
                        .breaks(emptyList())
                        .build()
                ),
                listOf(
                    WorkScheduleVo("Ежедневно", "10:00\u200A\u2013\u200A24:00")
                )
            )
        )
    }
}