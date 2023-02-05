package ru.yandex.yandexmaps.utils

import android.view.View
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import io.reactivex.Observable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexmaps.common.conductor.BaseController
import ru.yandex.yandexmaps.common.kotterknife.KotterKnifeException
import utils.mock
import kotlin.IllegalStateException
import io.reactivex.exceptions.OnErrorNotImplementedException as Rx2OnErrorNotImplementedException

class UnwrappingExceptionHandlerTest {

    private lateinit var defaultHandler: Thread.UncaughtExceptionHandler
    private lateinit var unwrapper: UnwrappingExceptionHandler
    private lateinit var testController: TestController
    private val thread = mock<Thread>()
    private val innerThrowable: Throwable = RuntimeException()

    private var systemHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setup() {
        systemHandler = Thread.getDefaultUncaughtExceptionHandler()
        defaultHandler = mock()
        unwrapper = UnwrappingExceptionHandler(defaultHandler)
        testController = TestController()
    }

    @After
    fun teardown() {
        Thread.setDefaultUncaughtExceptionHandler(systemHandler)
    }

    @Test
    fun unwrapsRx2OnErrorNotImplemented() {
        unwrapper.uncaughtException(thread, Rx2OnErrorNotImplementedException(innerThrowable))

        verify(defaultHandler).uncaughtException(eq(thread), eq(innerThrowable))
        verifyNoMoreInteractions(defaultHandler)
    }

    @Test
    fun unwrapIllegalStateExceptionCausedByRx2OnErrorNotImplemented() {
        unwrapper.uncaughtException(thread, IllegalStateException(Rx2OnErrorNotImplementedException(innerThrowable)))

        verify(defaultHandler).uncaughtException(eq(thread), eq(innerThrowable))
        verifyNoMoreInteractions(defaultHandler)
    }

    @Test
    fun doNotTouchIllegalStateExceptionNotCausedByOnErrorNotImplementedRx() {
        val illegalStateException = IllegalStateException(innerThrowable)
        unwrapper.uncaughtException(thread, illegalStateException)

        verify(defaultHandler).uncaughtException(eq(thread), eq(illegalStateException))
        verifyNoMoreInteractions(defaultHandler)
    }

    @Test
    fun cropKotterKnifeException() {
        try {
            testController.someView
        } catch (e: KotterKnifeException) {
            unwrapper.uncaughtException(thread, e)
        }

        val captor = argumentCaptor<Throwable>()
        verify(defaultHandler).uncaughtException(eq(thread), captor.capture())
        verifyNoMoreInteractions(defaultHandler)

        assertEquals(TestController::class.java.name, captor.firstValue.stackTrace.first().className)
    }

    @Test
    fun doNotTouchOtherException() {
        val originalException = RuntimeException(innerThrowable)
        unwrapper.uncaughtException(thread, originalException)

        verify(defaultHandler).uncaughtException(eq(thread), eq(originalException))
        verifyNoMoreInteractions(defaultHandler)
    }

    @Test
    fun originalExceptionHandled_when_ISE_getCauseThrows() {
        val originalException = object : IllegalStateException() {
            override val cause: Throwable? get() = throw RuntimeException()
        }

        unwrapper.uncaughtException(thread, originalException)

        verify(defaultHandler).uncaughtException(eq(thread), eq(originalException))
        verifyNoMoreInteractions(defaultHandler)
    }

    @Test
    fun installedHandlerUnwrapRxException() {
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
        UnwrappingExceptionHandler.enable()

        val expectedException = RuntimeException("Hi!")

        Observable.just(0)
            .doOnNext { throw expectedException }
            .subscribe()

        verify(defaultHandler).uncaughtException(eq(Thread.currentThread()), eq(expectedException))
        verifyNoMoreInteractions(defaultHandler)
    }
}

private class TestController : BaseController() {
    val someView by bind<View>(1)
    override fun performInjection() {
    }
}
