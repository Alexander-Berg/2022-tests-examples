package ru.yandex.yandexmaps.permissions

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import ru.yandex.yandexmaps.common.preferences.Preference
import ru.yandex.yandexmaps.common.preferences.PreferencesFactory
import ru.yandex.yandexmaps.common.utils.RequestCodes
import ru.yandex.yandexmaps.common.utils.activity.starter.ActivityStarter
import ru.yandex.yandexmaps.common.utils.extensions.rx.justObservable2
import ru.yandex.yandexmaps.permissions.api.PermissionsManager
import ru.yandex.yandexmaps.permissions.api.data.PermissionEventType
import ru.yandex.yandexmaps.permissions.api.data.PermissionsReason
import ru.yandex.yandexmaps.permissions.api.data.PermissionsRequest
import ru.yandex.yandexmaps.permissions.api.data.PermissionsRequest.Companion.unimportant
import ru.yandex.yandexmaps.permissions.api.data.SettingsPermissionsRequest
import ru.yandex.yandexmaps.permissions.internal.PendingPermissionsHolder
import ru.yandex.yandexmaps.permissions.internal.PendingPermissionsHolderImpl
import ru.yandex.yandexmaps.permissions.internal.PermissionsActions
import ru.yandex.yandexmaps.permissions.internal.PermissionsManagerImpl
import ru.yandex.yandexmaps.permissions.internal.SettingsPermissionsActions
import java.util.Arrays

internal class PermissionsManagerTest {

    private lateinit var manager: PermissionsManager
    private val pendingPermissionsHolder: PendingPermissionsHolder = PendingPermissionsHolderImpl()

    private val PERMISSIONS = Arrays.asList(
        "test.permission.FIRST",
        "test.permission.SECOND",
        "test.permission.THIRD"
    )

    @Mock
    lateinit var preferencesFactory: PreferencesFactory

    @Mock
    lateinit var preference: Preference<Boolean>

    @Mock
    lateinit var actions: PermissionsActions

    @Mock
    lateinit var activityStarter: ActivityStarter

    @Mock
    lateinit var settingsActions: SettingsPermissionsActions

    lateinit var mocksCloseable: AutoCloseable

    @Before
    fun setUp() {
        mocksCloseable = MockitoAnnotations.openMocks(this)
        whenever(actions.pendingPermissionsHolder()).thenReturn(pendingPermissionsHolder)
        whenever(preferencesFactory.boolean(any(), eq(false))).thenReturn(preference)
        manager = PermissionsManagerImpl(preferencesFactory, actions, activityStarter, settingsActions)
    }

    @After
    fun tearDown() {
        mocksCloseable.close()
    }

    @Test
    fun systemUiShownOnFirstRequest() {
        whenever(preference.changes).thenReturn(false.justObservable2())
        whenever(actions.shouldShowRequestPermissionsRationale(any())).thenReturn(false)
        Observable.just(Unit)
            .compose(manager.ensure(unimportant("test.key", PERMISSIONS), PermissionsReason.START_UP))
            .subscribe()
        verify(actions).systemRequest(PERMISSIONS, PermissionsReason.START_UP, PermissionEventType.SYSTEM)
    }

    @Test
    fun rationaleShownBeforeSecondRequest() {
        whenever(preference.changes).thenReturn(true.justObservable2())
        whenever(actions.shouldShowRequestPermissionsRationale(any())).thenReturn(true)
        Observable.just(Unit)
            .compose(
                manager.ensure(
                    PermissionsRequest(
                        permissions = PERMISSIONS,
                        rationaleTitleId = 1,
                        rationaleTextId = 2,
                        rationaleDrawableId = 3,
                        settingsTitleId = 4,
                        settingsTextId = 5,
                        settingsDrawableId = 6,
                        key = "test.key"
                    ),
                    PermissionsReason.START_UP
                )
            )
            .subscribe()
        verify(actions).rationaleRequest(PERMISSIONS, 1, 2, 3, PermissionsReason.START_UP)
    }

    @Test
    fun notShowRationaleBeforeSecondUnimportantRequest() {
        whenever(preference.changes).thenReturn(true.justObservable2())
        whenever(actions.shouldShowRequestPermissionsRationale(any())).thenReturn(true)
        Observable.just(Unit)
            .compose(manager.ensure(unimportant("test.key", PERMISSIONS), PermissionsReason.START_UP))
            .subscribe()
        verify(actions, times(0)).rationaleRequest(eq(PERMISSIONS), any(), any(), any(), any())
    }

    @Test
    fun settingsRequestShownAsLastChance() {
        whenever(preference.changes).thenReturn(true.justObservable2())
        whenever(actions.shouldShowRequestPermissionsRationale(any())).thenReturn(false)
        Observable.just(Unit)
            .compose(
                manager.ensure(
                    PermissionsRequest(
                        permissions = PERMISSIONS,
                        rationaleTitleId = 1,
                        rationaleTextId = 2,
                        rationaleDrawableId = 3,
                        settingsTitleId = 4,
                        settingsTextId = 5,
                        settingsDrawableId = 6,
                        key = "test.key"
                    ),
                    PermissionsReason.START_UP
                )
            )
            .subscribe()
        verify(actions).settingsRequest(PERMISSIONS, 4, 5, 6, PermissionsReason.START_UP)
    }

    @Test
    fun notShowSettingsRequestAtLastChanceUnimportantRequest() {
        whenever(preference.changes).thenReturn(true.justObservable2())
        whenever(actions.shouldShowRequestPermissionsRationale(any())).thenReturn(false)
        Observable.just(Unit)
            .compose(manager.ensure(unimportant("test.key", PERMISSIONS), PermissionsReason.START_UP))
            .subscribe()
        verify(actions, times(0)).settingsRequest(any(), any(), any(), any(), any())
    }

    @Test
    fun rationaleShownBeforeSettingsRequest() {
        whenever(settingsActions.requestSettingsPermission()).thenReturn(Observable.just(true))
        whenever(activityStarter.forResult(any(), any()))
            .thenReturn(ObservableTransformer { Observable.empty() })
        whenever(activityStarter.hasPending(any())).thenReturn(Single.just(false))
        Observable.just(Unit)
            .compose(
                manager.ensureSetting(
                    SettingsPermissionsRequest(
                        requestCode = RequestCodes.Rx.AON_SETUP,
                        titleId = 1,
                        textId = 2,
                        drawableId = 3,
                        startActivityRequest = mock(),
                    ),
                    PermissionsReason.AON_SETTINGS
                )
            )
            .subscribe()
        verify(settingsActions).requestSettingsPermission()
        verify(settingsActions).showSettingsPermissionRationale(any(), any(), any(), any())
    }
}
