package ru.yandex.yandexbus.inhouse

import android.content.Context
import androidx.annotation.CallSuper
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.stubbing.Stubber
import rx.android.plugins.RxAndroidPlugins
import rx.android.plugins.RxAndroidSchedulersHook
import rx.schedulers.Schedulers

fun <T> whenever(methodCall: T) = Mockito.`when`(methodCall)!!
fun <T> Stubber.whenever(methodCall: T) = `when`(methodCall)!!

// Workaround for mocking classes written on Kotlin
// All the Mockito's argument matchers return null which can't be passed as NonNull arguments
fun <T> any(): T {
    return Mockito.any<T>()
}

fun <T> eq(t: T): T {
    Mockito.eq<T>(t)
    @Suppress("UNCHECKED_CAST")
    return null as T
}

fun <T> capture(captor: ArgumentCaptor<*>): T {
    captor.capture()
    @Suppress("UNCHECKED_CAST")
    return null as T
}

@RunWith(TestsRunner::class)
abstract class BaseTest {

    protected val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @CallSuper
    @Before
    open fun setUp() {
        MockitoAnnotations.initMocks(this)

        //Because Robolectric doesn't know about AndroidSchedulers.mainThread()
        RxAndroidPlugins.getInstance().registerSchedulersHook(object : RxAndroidSchedulersHook() {
            override fun getMainThreadScheduler() = Schedulers.immediate()
        })
    }

    @CallSuper
    @After
    open fun tearDown() {
        RxAndroidPlugins.getInstance().reset()
    }
}
