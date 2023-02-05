package ru.yandex.market.internal

import android.os.Build
import dagger.MembersInjector
import io.reactivex.exceptions.CompositeException
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.common.preferences.CommonPreferences
import ru.yandex.market.di.TestScope
import ru.yandex.market.rx.schedulers.YSchedulers
import javax.inject.Inject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class PreferencesDataStoreTest {

    @Inject
    lateinit var dataStore: PreferencesDataStore

    @Inject
    lateinit var commonPreferences: CommonPreferences

    @Before
    fun setUp() {
        YSchedulers.setTestMode()
        DaggerPreferencesDataStoreTest_Component.builder()
            .testComponent(TestApplication.instance.component)
            .build()
            .injectMembers(this)
    }

    @Test
    fun `Throws exception when gson adapter failed to deserialize delivery locality`() {
        commonPreferences.preferences.edit().putString(PreferencesDataStore.KEY_SELECTED_LOCALITY, "{}").commit()
        dataStore.selectedDeliveryLocalityStream
            .test()
            .assertError(CompositeException::class.java)
    }

    @dagger.Component(dependencies = [TestComponent::class])
    @TestScope
    interface Component : MembersInjector<PreferencesDataStoreTest>
}