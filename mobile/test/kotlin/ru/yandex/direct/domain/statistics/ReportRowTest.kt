// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.domain.statistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.domain.daterange.Duration
import java.util.*

class ReportRowTest : BaseReportRowTest() {
    @Test
    fun gluedTogetherWith_shouldSaveRowData() {
        val section = "1"
        val sectionId = 1L
        val sectionExtra = "2"
        val first = row(mTimestamp, section, sectionExtra, sectionId)
        val second = row(mTimestamp, section, sectionExtra, sectionId)
        val glued = first.gluedTogetherWith(second)
        assertThat(glued as Any)
                .isEqualToComparingOnlyGivenFields(first, "mTimestamp", "mSectionCriteria",
                        "mSectionCriteriaExtra", "mReportUuid", "mSectionCriteriaId")
    }

    @Test
    fun isAbleToGlueTogether_true() {
        testGlueTogether(
                mTimestamp, "1", "1", 1,
                mTimestamp, "1", "1", 1,
                true
        )
    }

    @Test
    fun isAbleToGlueTogether_false_whenDifferentTimestamp() {
        testGlueTogether(
                mTimestamp, "1", "1", 1,
                Duration.millis(1).addTo(mTimestamp), "1", "1", 1,
                false
        )
    }

    @Test
    fun isAbleToGlueTogether_false_whenDifferentSection() {
        testGlueTogether(
                mTimestamp, "1", "1", 1,
                mTimestamp, "2", "1", 1,
                false
        )
    }

    @Test
    fun isAbleToGlueTogether_false_whenDifferentSectionExtra() {
        testGlueTogether(
                mTimestamp, "1", "1", 1,
                mTimestamp, "1", "2", 1,
                false
        )
    }

    @Test
    fun isAbleToGlueTogether_false_whenDifferentSectionId() {
        testGlueTogether(
                mTimestamp, "1", "1", 1,
                mTimestamp, "1", "1", 2,
                false
        )
    }

    private fun testGlueTogether(
            firstTimestamp: Date, firstSection: String, firstSectionExtra: String, firstSectionId: Long,
            secondTimestamp: Date, secondSection: String, secondSectionExtra: String, secondSectionId: Long,
            shouldGlue: Boolean) {
        val first = row(firstTimestamp, firstSection, firstSectionExtra, firstSectionId)
        val second = row(secondTimestamp, secondSection, secondSectionExtra, secondSectionId)
        assertThat(first.isAbleToGlueTogetherWith(second)).isEqualTo(shouldGlue)
    }
}