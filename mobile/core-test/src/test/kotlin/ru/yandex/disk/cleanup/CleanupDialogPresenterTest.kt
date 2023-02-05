package ru.yandex.disk.cleanup

import android.os.Bundle
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import ru.yandex.disk.cleanup.command.CheckForCleanupCommandRequest
import ru.yandex.disk.cleanup.command.StartCleanupCommandRequest
import ru.yandex.disk.event.DiskEvents
import ru.yandex.disk.event.EventSource
import ru.yandex.disk.provider.DiskContentProviderTest
import ru.yandex.disk.service.CommandScheduler
import ru.yandex.disk.service.CommandStarter
import ru.yandex.disk.storage.DocumentsTreeManager
import ru.yandex.disk.upload.UploadQueue
import ru.yandex.disk.util.SystemClock
import rx.plugins.RxJavaHooks
import rx.schedulers.Schedulers


class CleanupDialogPresenterTest : DiskContentProviderTest() {

    companion object {
        const val DEFAULT_TITLE = "defaultTitle"
        const val CALCULATING_MESSAGE = "calculatingMessage"
        const val CANCEL_BTN_TEXT = "cancelBtnText"
        const val CLOSE_BTN_TEXT = "closeBtnText"
        const val NOTHING_TO_CLEAN_MESSAGE = "nothingToCleanMessage"
        const val WAIT_FOR_AUTOUPLOAD_MESSAGE = "waitForAutouploadMessage"
        const val CLEAN_BTN_TEXT = "cleanBtnText"
        const val CLEAN_MESSAGE_TEXT = "cleanMessageText"
        const val NO_OLD_FILES_MESSAGE = "noOldFilesMessage"
        const val CLEAN_TITLE_TEXT = "cleanTitleText_"
    }

    private lateinit var nativeApiMock: CleanupDialogNativeApi
    private val uploadQueue: UploadQueue = mock(UploadQueue::class.java)
    private val commandStarter: CommandStarter = mock(CommandStarter::class.java)
    private val commandScheduler: CommandScheduler = mock(CommandScheduler::class.java)
    private val eventSource: EventSource = mock(EventSource::class.java)
    private val view: CleanupDialogView = mock(CleanupDialogView::class.java)
    private val documentsTreeManager = mock(DocumentsTreeManager::class.java)
    private val cleanupPolicy = CleanupPolicy(SystemClock.REAL)
    private val calculator = mock(CleanupSizeCalculator::class.java)
    private lateinit var presenter: CleanupDialogPresenter

    override fun setUp() {
        super.setUp()
        nativeApiMock = mock(CleanupDialogNativeApi::class.java).apply {
            whenever(defaultTitle).thenReturn(DEFAULT_TITLE)
            whenever(calculatingMessage).thenReturn(CALCULATING_MESSAGE)
            whenever(cancelBtnText).thenReturn(CANCEL_BTN_TEXT)
            whenever(closeBtnText).thenReturn(CLOSE_BTN_TEXT)
            whenever(nothingToCleanMessage).thenReturn(NOTHING_TO_CLEAN_MESSAGE)
            whenever(waitForAutouploadMessage).thenReturn(WAIT_FOR_AUTOUPLOAD_MESSAGE)
            whenever(cleanBtnText).thenReturn(CLEAN_BTN_TEXT)
            whenever(cleanMessageText).thenReturn(CLEAN_MESSAGE_TEXT)
            whenever(noOldFilesMessage).thenReturn(NO_OLD_FILES_MESSAGE)
            whenever(getCleanTitleText(anyLong())).thenAnswer { answer -> CLEAN_TITLE_TEXT + answer.getArgument<Long?>(0) }
        }

        presenter = createPresenter(false)

        RxJavaHooks.setOnIOScheduler({Schedulers.immediate()})
    }

    private fun createPresenter(fromPush : Boolean) : CleanupDialogPresenter {
        return CleanupDialogPresenter(eventSource, uploadQueue, commandStarter, commandScheduler,
                cleanupPolicy, documentsTreeManager,
                calculator, Schedulers.immediate(), nativeApiMock, fromPush)
    }

    override fun tearDown() {
        RxJavaHooks.reset()
        super.tearDown()
    }

    @Test
    fun `should show calculation`() {
        val presenter = spy(presenter)
        doNothing().whenever(presenter).requestLocalFilesSize()
        presenter.attach(view, null)
        verify(eventSource).registerListener(eq(presenter))
        verify(view).setTitle(eq(DEFAULT_TITLE))
        verify(view).setMessage(eq(CALCULATING_MESSAGE))
        verify(view).showNegativeButton(eq(CANCEL_BTN_TEXT))
        verify(view).hidePositiveButton()
        verify(view).hideExcludeRecent()
    }

    @Test
    fun `should show nothing to clean`() {
        whenever(uploadQueue.anyFileAutouploaded()).thenReturn(true)
        whenever(calculator.calculate(anyLong())).thenReturn(CleanupSize())
        presenter.attach(view, null)
        verify(view, times(2)).setTitle(eq(DEFAULT_TITLE))
        verify(view).setMessage(eq(NOTHING_TO_CLEAN_MESSAGE))
        verify(view).showNegativeButton(eq(CLOSE_BTN_TEXT))
        verify(view, times(2)).hidePositiveButton()
        verify(view, times(2)).hideExcludeRecent()
    }

    @Test
    fun `should show wait for upload`() {
        whenever(calculator.calculate(anyLong())).thenReturn(CleanupSize())
        whenever(uploadQueue.anyFileAutouploaded()).thenReturn(false)
        presenter.attach(view, null)
        verify(view, times(2)).setTitle(eq(DEFAULT_TITLE))
        verify(view).setMessage(eq(WAIT_FOR_AUTOUPLOAD_MESSAGE))
        verify(view).showNegativeButton(eq(CLOSE_BTN_TEXT))
        verify(view, times(2)).hidePositiveButton()
        verify(view, times(2)).hideExcludeRecent()
    }

    @Test
    fun `should show clean old`() {
        whenever(calculator.calculate(anyLong())).thenReturn(CleanupSize(allUploadedFilesSize = 2048, oldUploadedFilesSize = 1024))
        presenter.attach(view, null)
        verify(view).setTitle(eq(CLEAN_TITLE_TEXT + 1024))
        verify(view).setMessage(eq(CLEAN_MESSAGE_TEXT))

        verify(view, times(2)).showNegativeButton(eq(CANCEL_BTN_TEXT))
        verify(view).showPositiveButton(eq(CLEAN_BTN_TEXT))

        verify(view).showExcludeRecent(eq(true))

        whenever(calculator.calculate(anyLong())).thenReturn(CleanupSize(oldUploadedFilesSize = 4000))
        val event = mock(DiskEvents.FileUploadSucceeded::class.java)
        whenever(event.isFromAutoupload).thenReturn(true)
        presenter.on(event)
        verify(view).setTitle(eq(CLEAN_TITLE_TEXT + 4000))
    }

    @Test
    fun `should show clean all`() {
        whenever(calculator.calculate(anyLong())).thenReturn(CleanupSize(allUploadedFilesSize = 2048))
        presenter.attach(view, null)
        verify(view).setTitle(eq(CLEAN_TITLE_TEXT + 2048))
        verify(view).setMessage(eq(CLEAN_MESSAGE_TEXT))

        verify(view, times(2)).showNegativeButton(eq(CANCEL_BTN_TEXT))
        verify(view).showPositiveButton(eq(CLEAN_BTN_TEXT))

        verify(view).showExcludeRecent(eq(false))

        presenter.excludeRecentUpdated(true)
        verify(view).setMessage(eq(NO_OLD_FILES_MESSAGE))
        verify(view, times(2)).hidePositiveButton()
        verify(view).showExcludeRecent(eq(true))
    }

    @Test
    fun `should restore state`() {
        whenever(calculator.calculate(anyLong())).thenReturn(CleanupSize(allUploadedFilesSize = 2048, oldUploadedFilesSize = 1024))
        presenter.attach(view, null)
        val savedState = Bundle()
        presenter.saveState(savedState)
        presenter.detach()

        reset(view)
        presenter = createPresenter(false)
        presenter.attach(view, savedState)

        verify(view, never()).setTitle(eq(DEFAULT_TITLE))
        verify(view, never()).setMessage(eq(CALCULATING_MESSAGE))
        verify(view).setMessage(eq(CLEAN_MESSAGE_TEXT))
        verify(view).setTitle(eq(CLEAN_TITLE_TEXT + 1024))
        verify(view).showExcludeRecent(eq(true))
    }

    @Test
    fun `should not schedule check`() {
        presenter.attach(view, null)
        presenter.scheduleCheckForCleanup()
        verify(commandScheduler, never()).scheduleAt(any<CheckForCleanupCommandRequest>(), anyLong())
    }

    @Test
    fun `should schedule check`() {
        val presenter = createPresenter(true)
        presenter.attach(view, null)
        presenter.scheduleCheckForCleanup()
        verify(commandScheduler).scheduleAt(any<CheckForCleanupCommandRequest>(), anyLong())
    }

    @Test
    fun `should start cleanup`() {
        presenter.attach(view, null)
        presenter.startCleanup()
        verify(commandStarter).start(any<StartCleanupCommandRequest>())
    }

    @Test
    fun `should detach`() {
        presenter.attach(view, null)
        presenter.detach()
        verify(eventSource).unregisterListener(presenter)
    }
}
