package ru.auto.ara.core.rules.di

import org.junit.rules.ExternalResource
import ru.auto.data.repository.ISystemInfoRepository

object TestSystemInfoRepository : ISystemInfoRepository {
    var areNotificationsEnabledProvider: () -> Boolean = { true }
    var areOverlaysEnabledProvider: () -> Boolean = { true }

    override fun areNotificationsEnabled(): Boolean = areNotificationsEnabledProvider()
    override fun areOverlaysEnabled(): Boolean = areOverlaysEnabledProvider()
}


class NotificationsEnabledRule(var notificationsEnabled: () -> Boolean, var overlaysEnabled: () -> Boolean) : ExternalResource() {
    override fun before() {
        TestSystemInfoRepository.areNotificationsEnabledProvider = notificationsEnabled
        TestSystemInfoRepository.areOverlaysEnabledProvider = overlaysEnabled
    }
}
