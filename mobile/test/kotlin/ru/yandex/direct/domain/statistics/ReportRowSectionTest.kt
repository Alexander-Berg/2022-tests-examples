// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.domain.statistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ReportRowSectionTest(
        private val firstSection: String?,
        private val firstSectionExtra: String?,
        private val firstSectionId: Long,
        private val secondSection: String?,
        private val secondSectionExtra: String?,
        private val secondSectionId: Long,
        private val areInOneSection: Boolean
) : BaseReportRowTest() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<out Any?>> {
            val noId = ReportRow.SECTION_ID_EMPTY
            return listOf(

                    // Part One: in this part section ids are absent on both rows.
                    // In this case rows are said to be in one section, iff:
                    // 1. At least one section is not null
                    // 2. Section and section extra are the same for both rows

                    // Part One A: section == null.
                    arrayOf(null, null, noId,
                            null, null, noId,
                            false),

                    arrayOf(null, "description", noId,
                            null, "description", noId,
                            false),

                    arrayOf(null, "description 1", noId,
                            null, "description 2", noId,
                            false),

                    // Part One B: sections are equal.
                    arrayOf("title", null, noId,
                            "title", null, noId,
                            true),

                    arrayOf("title", "description", noId,
                            "title", "description", noId,
                            true),

                    arrayOf("title", "description 1", noId,
                            "title", "description 2", noId,
                            false),

                    // Part One C: sections are not equal.
                    arrayOf("title 1", null, noId,
                            "title 2", null, noId,
                            false),

                    arrayOf("title 1", "description", noId,
                            "title 2", "description", noId,
                            false),

                    arrayOf("title 1", "description 1", noId,
                            "title 2", "description 2", noId,
                            false),

                    // Part Two: Ids are equal.
                    // In this case rows are said to be in one section, iff section and section extra are the same for both rows

                    // Part Two A: section == null.
                    arrayOf(null, null, 0,
                            null, null, 0,
                            true),

                    arrayOf(null, "description", 0,
                            null, "description", 0,
                            true),

                    arrayOf(null, "description 1", 0,
                            null, "description 2", 0,
                            false),

                    // Part Two B: sections are equal.
                    arrayOf("title", null, 0,
                            "title", null, 0,
                            true),

                    arrayOf("title", "description", 0,
                            "title", "description", 0,
                            true),

                    arrayOf("title", "description 1", 0,
                            "title", "description 2", 0,
                            false),

                    // Part Two C: sections are not equal.
                    arrayOf("title 1", null, 0,
                            "title 2", null, 0,
                            false),

                    arrayOf("title 1", "description", 0,
                            "title 2", "description", 0,
                            false),

                    arrayOf("title 1", "description 1", 0,
                            "title 2", "description 2", 0,
                            false),

                    // Part Three: Ids are NOT equal.
                    // In this case we also trust only ids and do not check section and section extra.
                    // Rows with different ids cannot be in one section.

                    // Part Three A: section == null.
                    arrayOf(null, null, 0,
                            null, null, 1,
                            false),

                    arrayOf(null, "description", 0,
                            null, "description", 1,
                            false),

                    arrayOf(null, "description 1", 0,
                            null, "description 2", 1,
                            false),

                    // Part Three B: sections are equal.
                    arrayOf("title", null, 0,
                            "title", null, 1,
                            false),

                    arrayOf("title", "description", 0,
                            "title", "description", 1,
                            false),

                    arrayOf("title", "description 1", 0,
                            "title", "description 2", 1,
                            false),

                    // Part Three C: sections are not equal.
                    arrayOf("title 1", null, 0,
                            "title 2", null, 1,
                            false),

                    arrayOf("title 1", "description", 0,
                            "title 2", "description", 1,
                            false),

                    arrayOf("title 1", "description 1", 0,
                            "title 2", "description 2", 1,
                            false)
            )
        }
    }

    @Test
    fun isInOneSectionWith_worksCorrectly() {
        val first = row(mTimestamp, firstSection, firstSectionExtra, firstSectionId)
        val second = row(mTimestamp, secondSection, secondSectionExtra, secondSectionId)
        assertThat(first.isInOneSectionWith(second)).isEqualTo(areInOneSection)
    }
}