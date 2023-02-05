package com.yandex.sync.lib.entity

import org.junit.Assert.assertEquals
import org.junit.Test


class AttendeeEntityTest {

    @Test
    fun `filled and empty name`() {
        val entity = AttendeeEntity(
                name = "Svyatoslav",
                email = "svyatoslavdp@yandex-team.ru",
                status = AttendeeEntity.Status.ACCEPTED,
                type = "UNKNOWN",
                isOrganizer = false
        )
        val attendeeICal = entity.toAttendeeICal()
        val organizerICal = entity.toOrganizerICal()
        assertEquals("Svyatoslav", attendeeICal.commonName)
        assertEquals("Svyatoslav", organizerICal.commonName)


        val entityEmpty = AttendeeEntity(
                name = "",
                email = "svyatoslavdp@yandex-team.ru",
                status = AttendeeEntity.Status.ACCEPTED,
                type = "UNKNOWN",
                isOrganizer = false
        )
        val attendeeICalEmpty = entityEmpty.toAttendeeICal()
        val organizerICalEmpty = entityEmpty.toOrganizerICal()
        assertEquals("svyatoslavdp@yandex-team.ru", attendeeICalEmpty.commonName)
        assertEquals("svyatoslavdp@yandex-team.ru", organizerICalEmpty.commonName)
    }

}