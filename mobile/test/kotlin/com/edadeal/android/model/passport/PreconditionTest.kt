package com.edadeal.android.model.passport

import com.edadeal.android.helpers.StatefulMock
import com.edadeal.android.metrics.MigrationDescription
import com.edadeal.android.metrics.MigrationException
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Test
import org.mockito.ArgumentMatchers

class PreconditionTest : MigrationDelegateTest() {
    @Test
    fun `Precondition failed when auth method == AM and me and device got exception`() {
        val observer = delegate.startUp(meLoaded = false, deviceLoaded = false).test()
        observer.assertDelegateError(MigrationException(MigrationDescription.PreconditionFailure))
    }

    @Test
    fun `Precondition failed when auth method == AM and device got exception`() {
        val observer = delegate.startUp(meLoaded = true, deviceLoaded = false).test()
        observer.assertDelegateError(MigrationException(MigrationDescription.PreconditionFailure))
    }

    @Test
    fun `Precondition failed when auth method == AM and me got exception`() {
        val observer = delegate.startUp(meLoaded = false, deviceLoaded = true).test()
        observer.assertDelegateError(MigrationException(MigrationDescription.PreconditionFailure))
    }

    @Test
    fun `Precondition failed when auth method == AM and user is authorized via passport`() {
        whenever(prefs.isAuthorized).then { true }
        whenever(passportApi.getCurrentUid()).then { NOT_EMPTY_PASSPORT_UID }

        val observer = delegate.startUp(meLoaded = true, deviceLoaded = true).test()
        observer.assertDelegateError(MigrationException(MigrationDescription.PreconditionFailure))
    }

    @Test
    fun `amAllowed doesn't change when auth method == AM and migration get error (amAllowed == false)`() {
        val amAllowed = StatefulMock(false, { prefs::amAllowed.set(ArgumentMatchers.anyBoolean()) }, { prefs.amAllowed })
        delegate.startUp(meLoaded = false, deviceLoaded = false).test()
        assert(!amAllowed.value)
    }

    @Test
    fun `amAllowed doesn't change when auth method == AM and migration get error (amAllowed == true)`() {
        val amAllowed = StatefulMock(true, { prefs::amAllowed.set(ArgumentMatchers.anyBoolean()) }, { prefs.amAllowed })
        delegate.startUp(meLoaded = false, deviceLoaded = false).test()
        assert(amAllowed.value)
    }
}
