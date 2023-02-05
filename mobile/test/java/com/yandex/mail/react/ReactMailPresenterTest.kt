package com.yandex.mail.react

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Looper.getMainLooper
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.FutureTarget
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.mail.GlideRequest
import com.yandex.mail.R
import com.yandex.mail.attach.AttachmentUtils.getAttachPreviewFile
import com.yandex.mail.di.AccountComponent
import com.yandex.mail.entity.FolderType
import com.yandex.mail.glide.AttachImageParams
import com.yandex.mail.glide.BitmapWrapper
import com.yandex.mail.image.AvatarRequestBuilderProxy
import com.yandex.mail.image.ImageUtils.getAvatarFile
import com.yandex.mail.message_container.FolderContainer
import com.yandex.mail.metrica.MetricaConstns.EventMetrics.ThreadViewEventMetrics.EXTRA_THREADVIEW_CLOSING_TYPE
import com.yandex.mail.metrica.MetricaConstns.EventMetrics.ThreadViewEventMetrics.THREADVIEW_EXHIBITION
import com.yandex.mail.metrica.MetricaConstns.EventMetrics.ThreadViewEventMetrics.VALUE_THREADVIEW_CLOSING_TYPE_CLOSE
import com.yandex.mail.metrica.MetricaConstns.EventMetrics.ThreadViewEventMetrics.VALUE_THREADVIEW_CLOSING_TYPE_UPDATE
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.TARGET_HIERARCHICAL_ID
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.TARGET_MESSAGE_ID
import com.yandex.mail.metrica.MetricaConstns.RawMetrics.TARGET_THREAD_ID
import com.yandex.mail.metrica.MetricaConstns.ReactMetrics.REACT_TAP_ATTACH
import com.yandex.mail.model.AttachmentsModel
import com.yandex.mail.model.GeneralSettingsModel
import com.yandex.mail.model.MessageBodyDescriptor
import com.yandex.mail.model.MessagesModel
import com.yandex.mail.model.QuickReplyModel
import com.yandex.mail.notifications.NotificationsModel
import com.yandex.mail.proxy.BlockManager
import com.yandex.mail.proxy.MailDns.MAIL_DNS_TAG
import com.yandex.mail.react.ReactTestFactory.buildMessageWithIdWithBody
import com.yandex.mail.react.ReactTestFactory.buildMessageWithIdWithoutBody
import com.yandex.mail.react.entity.Attachment
import com.yandex.mail.react.entity.Avatar
import com.yandex.mail.react.entity.Fields
import com.yandex.mail.react.entity.ReactMessage
import com.yandex.mail.react.entity.ReactThread
import com.yandex.mail.react.entity.Recipient
import com.yandex.mail.react.model.LinkUnwrapper
import com.yandex.mail.react.model.MailModel
import com.yandex.mail.react.model.MessageBodyLoader
import com.yandex.mail.react.selection.ReactMailSelection
import com.yandex.mail.react.translator.ReactTranslatorsHolder
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.settings.AccountSettings
import com.yandex.mail.settings.GeneralSettings
import com.yandex.mail.settings.SwipeAction
import com.yandex.mail.storage.entities.EntitiesTestFactory
import com.yandex.mail.tools.Accounts
import com.yandex.mail.ui.presenters.presenter_commands.ArchiveCommand
import com.yandex.mail.ui.presenters.presenter_commands.ChangeSpamCommand
import com.yandex.mail.ui.presenters.presenter_commands.CommandConfig
import com.yandex.mail.ui.presenters.presenter_commands.CommandProcessor
import com.yandex.mail.ui.presenters.presenter_commands.DeleteCommand
import com.yandex.mail.ui.views.MailView
import com.yandex.mail.util.BaseIntegrationTest
import com.yandex.mail.util.CollectionUtil.unmodifiableSetOf
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper
import java.io.UnsupportedEncodingException
import java.util.Arrays.asList
import java.util.Optional
import java.util.concurrent.ExecutionException

@RunWith(IntegrationTestRunner::class)
class ReactMailPresenterTest : BaseIntegrationTest() {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    lateinit var presenterSettings: ReactMailPresenterSettings

    lateinit var selectionProvider: SelectionProvider

    lateinit var selection: ReactMailSelection

    lateinit var mailModel: MailModel

    lateinit var settingsModelMock: GeneralSettingsModel

    lateinit var accountSettingsModelMock: AccountSettings

    lateinit var presenter: ReactMailPresenter

    lateinit var commandProcessor: CommandProcessor

    lateinit var mailView: MailView

    lateinit var requestManager: RequestManager

    lateinit var messagesModelMock: MessagesModel

    lateinit var notificationsModelMock: NotificationsModel

    lateinit var quickReplyModelMock: QuickReplyModel

    lateinit var reactQuickReplyModelStateMock: ReactQuickReplyState

    private lateinit var avatarRequestBuilderProxy: TestAvatarRequestBuilderProxy

    private lateinit var reactTranslatorHolder: ReactTranslatorsHolder

    var localUid: Long = 2147483650

    var threadId: Long = 100

    @Before
    fun beforeEachTestCase() {
        val loginData = Accounts.testLoginData
        init(loginData)
        localUid = user.uid

        selectionProvider = mock(SelectionProvider::class.java)
        selection = mock(ReactMailSelection::class.java)
        mailModel = mock(MailModel::class.java)
        settingsModelMock = mock(GeneralSettingsModel::class.java)
        accountSettingsModelMock = mock(AccountSettings::class.java)
        requestManager = mock(RequestManager::class.java)
        avatarRequestBuilderProxy = spy(TestAvatarRequestBuilderProxy())
        commandProcessor = CommandProcessor()
        messagesModelMock = mock(MessagesModel::class.java)
        notificationsModelMock = mock(NotificationsModel::class.java)
        quickReplyModelMock = mock(QuickReplyModel::class.java)
        reactQuickReplyModelStateMock = mock(ReactQuickReplyState::class.java)

        presenterSettings = ReactMailPresenterSettings(
            10,
            5,
            Schedulers.trampoline(),
            Schedulers.trampoline(),
            Schedulers.trampoline(),
            Schedulers.trampoline()
        )

        initPresenter()

        verifyNoInteractions(mailModel, settingsModelMock)

        val settings = GeneralSettings(IntegrationTestRunner.app())
        settings.edit()
            .setSwipeAction(SwipeAction.ARCHIVE)
            .apply()

        whenever(settingsModelMock.generalSettings).thenReturn(settings)

        whenever(quickReplyModelMock.getQuickReplyValuesForReact(anyList())).thenReturn(Single.just(ReactQuickReplyStateValues.empty))
        whenever(selectionProvider.provide(anyLong(), anyLong(), anyLong(), anyInt())).thenReturn(selection)
        mailView = mock(MailView::class.java)
        whenever(mailView.uid).thenReturn(localUid)
        whenever(mailView.threadId).thenReturn(threadId)

        whenever(app.cacheDir).thenReturn(tmpFolder.root)

        val mockAccComponent = mock(AccountComponent::class.java)
        doReturn(mockAccComponent).whenever(app).getAccountComponent(anyLong())
        whenever(mockAccComponent.settings()).thenReturn(accountSettingsModelMock)
        whenever(mockAccComponent.isMailish).thenReturn(false)
        whenever(selection.observeSavingToDiskMids()).thenReturn(Observable.just(emptyList()))
        whenever(selection.networkLoadingTrigger).thenReturn(Observable.empty())
    }

    private fun initPresenter(scrollToExpanded: Boolean = false) {
        val messageBodyLoader = MessageBodyLoader(IntegrationTestRunner.app(), user.uid)
        reactTranslatorHolder = ReactTranslatorsHolder(messageBodyLoader)

        presenter = ReactMailPresenter(
            app,
            presenterSettings,
            selectionProvider,
            mailModel,
            settingsModelMock,
            commandProcessor,
            requestManager,
            avatarRequestBuilderProxy,
            metrica,
            mock(BlockManager::class.java),
            messagesModelMock,
            FolderContainer(1, FolderType.INBOX.serverType),
            translatorModel,
            messageBodyLoader,
            reactTranslatorHolder,
            quickReplyModelMock,
            reactQuickReplyModelStateMock,
            scrollToExpanded,
            notificationsModelMock
        )
    }

    @Test
    fun `onBindView verify behavior`() {
        whenever(mailView.uiEvents()).thenReturn(Observable.empty())

        presenter.onBindView(mailView)

        // Presenter should subscribe to GeneralSettings and send SwipeAction preference to the view
        verify(settingsModelMock).getGeneralSettings()
        verify(mailView).onDismissActionSelected(SwipeAction.ARCHIVE)

        // Presenter should at least make call to uiEvents, we will check that it reacts to the events later
        verify(mailView).uiEvents()
    }

    @Test
    fun `unwrapAndOpenLink should open regular link`() {
        whenever(mailView.uiEvents()).thenReturn(Observable.empty())
        val reactMailInfo = mock(ReactMailSelection::class.java)
        val linkUnwrapper = mock(LinkUnwrapper::class.java)
        whenever(reactMailInfo.networkLoadingTrigger).thenReturn(Observable.empty())
        whenever(
            selectionProvider.provide(
                anyLong(),
                anyLong(),
                anyLong(),
                anyInt()
            )
        ).thenReturn(reactMailInfo)
        whenever(reactMailInfo.linkUnwrapper).thenReturn(linkUnwrapper)
        presenter.onBindView(mailView)

        val safeLink = "yandex.ru"
        whenever(linkUnwrapper.unwrapLink(safeLink)).thenReturn(Single.just(safeLink))

        presenter.unwrapAndOpenLink(safeLink)
        verify(mailView).openLink(safeLink)
    }

    @Test
    fun `unwrapAndOpenLink should open safe link on untrusted`() {
        whenever(mailView.uiEvents()).thenReturn(Observable.empty())
        val reactMailInfo = mock(ReactMailSelection::class.java)
        val linkUnwrapper = mock(LinkUnwrapper::class.java)
        whenever(reactMailInfo.networkLoadingTrigger).thenReturn(Observable.empty())
        whenever(
            selectionProvider.provide(
                anyLong(),
                anyLong(),
                anyLong(),
                anyInt()
            )
        ).thenReturn(reactMailInfo)
        whenever(reactMailInfo.linkUnwrapper).thenReturn(linkUnwrapper)
        presenter.onBindView(mailView)

        val fraudLink = "mail.ru"
        val safeLink = "yandex.ru"
        whenever(linkUnwrapper.unwrapLink(fraudLink)).thenReturn(Single.just(safeLink))

        presenter.unwrapAndOpenLink(fraudLink)
        verify(mailView).openLink(safeLink)
    }

    @Test
    fun `unwrapAndOpenLink should send error to view`() {
        whenever(mailView.uiEvents()).thenReturn(Observable.empty())
        val reactMailInfo = mock(ReactMailSelection::class.java)
        val linkUnwrapper = mock(LinkUnwrapper::class.java)
        whenever(reactMailInfo.networkLoadingTrigger).thenReturn(Observable.empty())
        whenever(
            selectionProvider.provide(
                anyLong(),
                anyLong(),
                anyLong(),
                anyInt()
            )
        ).thenReturn(reactMailInfo)
        whenever(reactMailInfo.linkUnwrapper).thenReturn(linkUnwrapper)
        presenter.onBindView(mailView)

        val exception = AssertionError()
        whenever(linkUnwrapper.unwrapLink(any())).thenReturn(Single.error(exception))

        presenter.unwrapAndOpenLink("test")
        verify(mailView).onCanNotOpenLink(exception)
    }

    @Test
    fun `unwrapAndOpenLink should open calendar link`() {
        whenever(mailView.uiEvents()).thenReturn(Observable.empty())
        val reactMailInfo = mock(ReactMailSelection::class.java)
        val linkUnwrapper = mock(LinkUnwrapper::class.java)
        whenever(reactMailInfo.networkLoadingTrigger).thenReturn(Observable.empty())
        whenever(
            selectionProvider.provide(
                anyLong(),
                anyLong(),
                anyLong(),
                anyInt()
            )
        ).thenReturn(reactMailInfo)
        whenever(reactMailInfo.linkUnwrapper).thenReturn(linkUnwrapper)
        presenter.onBindView(mailView)

        val link = "https://calendar.yandex.ru/event/?uid=1&event_id=1&show_date=2020-05-30&view_type=week"
        whenever(linkUnwrapper.unwrapLink(link)).thenReturn(Single.just(link))

        presenter.unwrapAndOpenLink(link)
        verify(mailView).openLink(link)
    }

    @Test
    fun `subscribeToThread should subscribe after dom ready event and load initial messages`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(3)
        val threadMeta = ReactTestFactory.buildThreadMeta()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        // Presenter should receive ThreadMeta and send it to the view
        verify(mailView).onThreadMetaChanged(eq(threadMeta))

        val expectedAddMessages = ArrayList<ReactMessage>(messages.size)

        for (i in messages.indices) {
            expectedAddMessages.add(messages[i].toBuilder().collapsed(true).build())
        }

        // Presenter should receive initial list of messages and add them to the view
        verify(mailView).applyNewMessages(
            eq(expectedAddMessages),
            eq(emptyList<ReactMessage>()),
            eq(emptyList<ReactMessage>())
        )
    }

    @Test
    fun `subscribeToThread should send common error to view`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val error = IllegalStateException()
        whenever(selection.threadFlowable).thenReturn(Flowable.error(error))
        presenter.onBindView(mailView)

        // Presenter should deliver occurred error to the view
        verify(mailView).onCanNotLoadThread(error)
    }

    @Test
    fun `subscribeToThread should not send error to view when a thread is already shown`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(5)
        val threadMeta = ReactTestFactory.buildThreadMeta()
        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        val error = IllegalStateException()
        whenever(selection.threadFlowable).thenReturn(Flowable.error(error))

        verify(mailView, never()).onCanNotLoadThread(error)
    }

    @Test
    fun `subscribeToThread should send error of message body loading to view`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(5)
        // 3rd and 4th messages have problem with loading of the body
        messages[2] = messages[2].toBuilder().body(null).bodyLoadingError(IllegalStateException()).build()
        messages[3] = messages[3].toBuilder().body(null).bodyLoadingError(IllegalStateException()).build()
        val threadMeta = ReactTestFactory.buildThreadMeta()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))
        presenter.onBindView(mailView)

        // Presenter should notify selection that we no longer need full info for messages with bodyLoadingError
        val captor = argumentCaptor<Collection<Long>>()
        verify(selection).stopRequestingFullInfoForTheseMessages(captor.capture())
        assertThat(captor.lastValue).containsOnly(messages[2].messageId(), messages[3].messageId())

        // Presenter should notify view about problems with loading message(s) body
        verify(mailView).onCanNotLoadMessageBodies()

        val expectedAddMessages = ArrayList(messages)
        var i = 0
        val size = expectedAddMessages.size
        while (i < size) {
            expectedAddMessages[i] = expectedAddMessages[i].toBuilder().collapsed(true).build()
            i++
        }

        // And of course we should display messages anyway, even if some of them could not be loaded!
        verify(mailView).applyNewMessages(
            eq(expectedAddMessages),
            eq(emptyList<ReactMessage>()),
            eq(emptyList<ReactMessage>())
        )
    }

    @Test
    fun `subscribeToThread should update thread notifications`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(5)
        val threadMeta = ReactTestFactory.buildThreadMeta()
        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        verify(messagesModelMock, times(1)).resetMessageTimestamp(anyCollection())
        verify(notificationsModelMock, times(1)).updateNotifications(anyLong(), any())
    }

    @Test
    fun `uiEventActionLoadMessageBody should ask more messages from the selection`() {
        // Sending LOAD_MORE to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_LOAD_MORE, emptyList())))

        presenter.onBindView(mailView)

        // Should request more messages from selection
        verify(selection).loadMoreMessages(eq(presenterSettings.loadMoreCount))
    }

    @Test
    fun `uiEventActionLoadMessageBody should ask full info for message and trigger force reload`() {
        // Sending LOAD_MESSAGE_BODY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(
                Observable.just(
                    UiEvent(
                        UiEvent.ACTION_LOAD_MESSAGE_BODY,
                        listOf("123")
                    )
                ) // Requesting message body for mid = 123
            )

        presenter.onBindView(mailView)

        // Should request full info for message with mid 123
        verify(selection).requestFullInfoForTheseMessages(eq(listOf(123L)))
        // Should request full reload, required for case when message body was not
        // loaded before because of a error, but user can trigger manual ACTION_LOAD_MESSAGE_BODY
        verify(selection).forceReload()
    }

    @Test
    fun `uiEventAttachTapped should report to metrica`() {
        // Sending LOAD_MESSAGE_BODY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(
                Observable.just(
                    UiEvent(
                        UiEvent.ACTION_ATTACH_TAPPED,
                        listOf("image", "1", "hid")
                    )
                ) // Tap on attach with mid = 1 and hid = "hid"
            )

        presenter.onBindView(mailView)

        val expectedExtras = HashMap<String, Any>()
        expectedExtras[TARGET_MESSAGE_ID] = 1L
        expectedExtras[TARGET_HIERARCHICAL_ID] = "hid"
        metrica.assertLastEvent(REACT_TAP_ATTACH, expectedExtras)
    }

    @Test
    fun onDarkThemeForMessageClicked() {
        whenever(mailView.uiEvents())
            .thenReturn(
                Observable.just(
                    UiEvent(UiEvent.ACTION_MESSAGE_DARK_CHANGED, listOf("false", "1")),
                    UiEvent(UiEvent.ACTION_MESSAGE_DARK_CHANGED, listOf("false", "2")),
                    UiEvent(UiEvent.ACTION_MESSAGE_DARK_CHANGED, listOf("true", "2"))
                )
            )

        presenter.onBindView(mailView)

        verify(mailView).setForcedLightThemeMid(eq(2), eq(false))
        verify(mailView).setForcedLightThemeMid(eq(2), eq(false))
        verify(mailView).setForcedLightThemeMid(eq(2), eq(true))

        val state = Bundle()
        presenter.saveState(state)

        // reinit presenter
        initPresenter()

        presenter.restoreState(state)

        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))
        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(5)
        val threadMeta = ReactTestFactory.buildThreadMeta()
        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))
        presenter.onBindView(mailView)

        verify(mailView).setForcedLightThemeMids()
    }

    @Test
    fun shouldMarkAsReadUncollapsedUnreadMessages() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        whenever(mailModel.markAsRead(anyLong(), any())).thenReturn(Completable.complete())

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(4)
        val threadMeta = ReactTestFactory.buildThreadMeta()

        // First and last message will be read
        messages[0] = messages[0].toBuilder().read(true).build()
        messages[3] = messages[3].toBuilder().read(true).build()

        // 2nd message will be unread and uncollapsed
        messages[1] = messages[1].toBuilder().read(false).collapsed(false).body("test").build()

        // 3rd message will be unread BUT collapsed, it should not be marked as read automatically
        messages[2] = messages[2].toBuilder().read(false).collapsed(false).build()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        verify(mailView).onThreadMetaChanged(eq(threadMeta))

        val expectedAddMessages = ArrayList(messages)
        expectedAddMessages[0] = messages[0].toBuilder().collapsed(true).build()
        expectedAddMessages[2] = messages[2].toBuilder().collapsed(true).build()
        expectedAddMessages[3] = messages[3].toBuilder().collapsed(true).build()

        verify(mailView).applyNewMessages(
            eq(expectedAddMessages),
            eq(emptyList<ReactMessage>()),
            eq(emptyList<ReactMessage>())
        )

        // Mark as read should not be called until presenter is not resumed!
        //noinspection ResourceType,unchecked
        verify(mailModel, never()).markAsRead(anyLong(), anyCollection())

        // Resuming the presenter, should trigger invocation of mark as read for 2nd message
        presenter.onResume()

        // Only 2nd message should be marked as read!

        verify(mailModel).markAsRead(
            eq(localUid),
            eq(setOf(messages[1].messageId()))
        )
    }

    @Test
    fun shouldNotAutoMarkAsReadAlreadyMarkedMessages() {
        val mailView = mock(MailView::class.java)
        val uid: Long = user.uid

        whenever(mailView.uid).thenReturn(uid)

        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))
        whenever(mailModel.markAsRead(anyLong(), any())).thenReturn(Completable.complete())

        val messages1 = ReactTestFactory.buildListOfMessagesWithoutBodies(3)
        messages1[1] = messages1[1].toBuilder().read(false).collapsed(false).body("mark me as read!").build()

        val messages2 = ArrayList(messages1)
        messages2[1] = messages1[1].toBuilder().firstLine("New first line").build()

        val threadMeta = ReactTestFactory.buildThreadMeta()

        // Send exactly same messages list twice, then check that messages were marked as read only once
        val threadFlowable = PublishProcessor.create<ReactThread>()
        whenever(selection.threadFlowable).thenReturn(threadFlowable)

        presenter.onBindView(mailView)
        presenter.onResume()

        threadFlowable.onNext(ReactThread(messages1, threadMeta))
        threadFlowable.onNext(ReactThread(messages2, threadMeta))

        verify(mailView, times(2)).applyNewMessages(
            anyList(),
            anyList(),
            anyList()
        )

        // Presenter should auto mark messages as read only once!

        verify(mailModel).markAsRead(anyLong(), anyCollection())
    }

    @Test
    fun shouldNotAutoMarkAsReadAlreadyReadMessageWhichThenWasMarkedAsUnreadManually() {
        val mailView = mock(MailView::class.java)
        val uid: Long = user.uid
        whenever(mailView.uid).thenReturn(uid)

        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages1 = ReactTestFactory.buildListOfMessagesWithoutBodies(3)
        messages1[0] = messages1[0].toBuilder().read(true).body("Expand me!").build()

        val messages2 = ArrayList(messages1)
        // Emulation of manual marking as unread (real test case)
        messages2[0] = messages2[0].toBuilder().read(false).build()

        val threadMeta = ReactTestFactory.buildThreadMeta()
        val threadFlowable = PublishProcessor.create<ReactThread>()

        whenever(selection.threadFlowable).thenReturn(threadFlowable)

        presenter.onBindView(mailView)
        presenter.onResume()

        threadFlowable.onNext(ReactThread(messages1, threadMeta))
        threadFlowable.onNext(ReactThread(messages2, threadMeta))

        // Verify that auto mark as read was never done for such test case

        verify(mailModel, never()).markAsRead(anyLong(), anyCollection())
    }

    @Test
    fun `saveInstanceState restore instance state should request full info for previously un collapsed messages`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(3)
        // Uncollapsing only 2nd message
        messages[1] = messages[1].toBuilder().body("yo").collapsed(false).build()
        messages[0] = messages[0].toBuilder().collapsed(true).build()
        messages[2] = messages[2].toBuilder().collapsed(true).build()

        val threadMeta = ReactTestFactory.buildThreadMeta()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        // Presenter should receive ThreadMeta and send it to the view
        verify(mailView).onThreadMetaChanged(eq(threadMeta))

        // Presenter should receive initial list of messages and add them to the view
        verify(mailView).applyNewMessages(
            eq(messages),
            eq(emptyList<ReactMessage>()),
            eq(emptyList<ReactMessage>())
        )

        val state = Bundle()
        presenter.saveState(state)

        // New Presenter!
        beforeEachTestCase()
        presenter.restoreState(state)

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        // Send DOM_READY to the presenter
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))
        presenter.onBindView(mailView)

        // Presenter should request loading of all previously
        verify(selection).requestFullInfoForTheseMessages(eq(unmodifiableSetOf(messages[1].messageId())))

        // Presenter should receive NEW initial list of messages and add them to the view
        verify(mailView).applyNewMessages(
            eq(messages),
            eq(emptyList<ReactMessage>()),
            eq(emptyList<ReactMessage>())
        )
    }

    @Test
    fun `archiveAllMessagesInThread should no op if view null`() {
        presenter.archiveAllMessagesInThread()
        verify(mailModel, never()).archive(anyLong(), anyCollection())
    }

    @Test
    fun `archiveAllMessagesInThread should request all messages from thread and archive them`() {
        // Send DOM_READY to the presenter
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))
        whenever(selection.threadFlowable).thenReturn(Flowable.never())
        presenter.onBindView(mailView)

        val localMidsFromTheThread = asList(1L, 2L, 3L)
        whenever(messagesModelMock.getMidsByTids(anyList())).thenReturn(Single.just(localMidsFromTheThread))

        whenever(
            mailModel.archive(
                eq(localUid),
                eq(localMidsFromTheThread),
            )
        ).thenReturn(Completable.complete())

        presenter.archiveAllMessagesInThread()
        shadowOf(getMainLooper()).idle()
        ShadowLooper.runMainLooperToNextTask()
        verify(mailModel).archive(
            eq(localUid),
            eq(localMidsFromTheThread),
        )
    }

    @Test
    fun `archiveAllMessagesInThread should notify view about error`() {
        // Send DOM_READY to the presenter
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))
        whenever(selection.threadFlowable).thenReturn(Flowable.never())
        presenter.onBindView(mailView)

        val localMidsFromTheThread = asList(1L, 2L, 3L)
        whenever(selection.allMessageIdsFromThread).thenReturn(Single.just(localMidsFromTheThread))

        val error = RuntimeException()
        presenter.commandProcessorCallback
            .onError(
                ArchiveCommand(emptyList(), localMidsFromTheThread, mailModel, mock(CommandConfig::class.java)),
                error
            )
        verify(mailView).onCanNotArchiveMessages(eq(localMidsFromTheThread), any())
    }

    @Test
    fun `archiveMessages should no op if view null`() {
        presenter.archiveMessages(listOf(1L))
        verify(mailModel, never()).archive(anyLong(), anyCollection())
    }

    @Test
    fun `subscribeToThread should send message container to view if all messages in same folder`() {
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(3)
        val threadMeta = ReactTestFactory.buildThreadMeta().toBuilder().totalMessagesCount(1).build()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        val container = FolderContainer(messages[0].folderId, messages[0].folderType)
        verify(mailView).setMessageContainer(container)
    }

    @Test
    fun `subscribeToThread should not send message container to view if not all messages in same folder`() {
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(3)
        messages[1] = messages[1].toBuilder().folderId(messages[1].folderId + 1).build()
        val threadMeta = ReactTestFactory.buildThreadMeta().toBuilder().totalMessagesCount(2).build()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        val container = FolderContainer(messages[0].folderId, messages[0].folderType)
        verify(mailView, never()).setMessageContainer(container)
    }

    @Test
    fun `commandProcessorCallback should ignore archive`() {
        whenever(mailView.uiEvents()).thenReturn(Observable.empty())
        presenter.onBindView(mailView)
        reset(mailView)

        val command = mock(ArchiveCommand::class.java)
        presenter.commandProcessorCallback.onPrepare(command, 0)
        presenter.commandProcessorCallback.onCancel(command)

        verify(mailView, never()).onThreadMetaChanged(any())
        verify(mailView, never()).addMessageFilter(any())
        verify(mailView, never()).removeMessageFilter(any())
    }

    @Test
    fun `commandProcessorCallback should update view`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(4)
        messages[0] = messages[0].toBuilder().read(true).build()
        messages[1] = messages[1].toBuilder().read(false).build()
        messages[2] = messages[2].toBuilder().read(false).draft(true).build()
        messages[3] = messages[3].toBuilder().read(true).draft(true).build()

        val threadMeta = ReactTestFactory.threadMetaBuilder().draftsCount(2).totalMessagesCount(4).unreadMessagesCount(2).build()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        val command = mock(DeleteCommand::class.java)
        val emailIds = listOf(messages[1].messageId(), messages[2].messageId()) // two unread and one draft
        whenever(command.messageIds).thenReturn(emailIds)
        whenever(command.cancelable()).thenReturn(true)
        val newThreadMeta = ReactTestFactory.threadMetaBuilder().draftsCount(1).totalMessagesCount(2).unreadMessagesCount(0).build()

        presenter.commandProcessorCallback.onPrepare(command, 0)

        verify(mailView).addMessageFilter(emailIds)
        verify(mailView).onThreadMetaChanged(newThreadMeta)

        presenter.commandProcessorCallback.onCancel(command)

        verify(mailView).removeMessageFilter(listOf(messages[1], messages[2]))
        verify(mailView, times(2)).onThreadMetaChanged(threadMeta)
    }

    @Test
    fun `commandProcessorCallback should close view on all messages command if cancelable`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(4)
        val threadMeta = ReactTestFactory.threadMetaBuilder().totalMessagesCount(4).build()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        val command = mock(ChangeSpamCommand::class.java)
        val emailIds = messages.map { it.messageId() }
        whenever(command.cancelable()).thenReturn(true)
        whenever(command.messageIds).thenReturn(emailIds)

        presenter.commandProcessorCallback.onPrepare(command, 0)

        verify(mailView).requestHide()
    }

    @Test
    fun `commandProcessorCallback should close view on all messages command if archive`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(4)
        val threadMeta = ReactTestFactory.threadMetaBuilder().totalMessagesCount(4).build()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        val command = mock(ArchiveCommand::class.java)
        val emailIds = messages.map { it.messageId() }
        whenever(command.cancelable()).thenReturn(true)
        whenever(command.messageIds).thenReturn(emailIds)

        presenter.commandProcessorCallback.onPrepare(command, 0)

        verify(mailView).requestHide()
    }

    @Test
    fun `commandProcessorCallback should not close view on not all messages command if archive`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(1)
        val threadMeta = ReactTestFactory.threadMetaBuilder().totalMessagesCount(4).build()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        val command = mock(ArchiveCommand::class.java)
        val emailIds = messages.map { it.messageId() }
        whenever(command.cancelable()).thenReturn(true)
        whenever(command.messageIds).thenReturn(emailIds)

        presenter.commandProcessorCallback.onPrepare(command, 0)

        verify(mailView, never()).requestHide()
    }

    @Test
    fun `commandProcessorCallback should not close view on all messages command if not cancelable`() {
        // Sending DOM_READY to the presenter
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(4)
        val threadMeta = ReactTestFactory.threadMetaBuilder().totalMessagesCount(4).build()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        val command = mock(ChangeSpamCommand::class.java)
        val emailIds = messages.map { it.messageId() }
        whenever(command.cancelable()).thenReturn(false)
        whenever(command.messageIds).thenReturn(emailIds)

        presenter.commandProcessorCallback.onPrepare(command, 0)

        verify(mailView, never()).requestHide()
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun `commandProcessorCallback should reload avatars on cancel`() {
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        mockAvatarResources()
        val messages = createMessageWithAvatars()

        val threadMeta = ReactTestFactory.buildThreadMeta()
        val threadProcessor = PublishProcessor.create<ReactThread>()
        whenever(selection.threadFlowable).thenReturn(threadProcessor)

        presenter.onBindView(mailView)
        val thread = ReactThread(messages, threadMeta)
        threadProcessor.onNext(thread)
        verify(mailView).applyNewMessages(messages, emptyList(), emptyList())

        val fields = thread.messages[0]!!.toCcBcc
        verify(mailView).updateAvatar(
            fields.from[0].email,
            fields.from[0].avatar.toBuilder()
                .type(Avatar.TYPE_IMAGE)
                .imageUrl(Uri.fromFile(getAvatarFile(app, "from@email")).toString())
                .build()
        )
        verify(mailView).updateAvatar(any(), any())
        clearInvocations(mailView)

        // test caching
        threadProcessor.onNext(ReactThread(thread.messages, threadMeta))

        // from/to/cc/bcc from cache and 1 call to api(from)
        avatarRequestBuilderProxy.assertCalls(4, 1)

        val command = mock(DeleteCommand::class.java)
        val emailIds = messages.map { it.messageId() }
        whenever(command.cancelable()).thenReturn(true)
        whenever(command.messageIds).thenReturn(emailIds)

        presenter.commandProcessorCallback.onPrepare(command, 0)
        presenter.commandProcessorCallback.onCancel(command)

        verify(mailView).updateAvatar(
            fields.from[0].email,
            fields.from[0].avatar.toBuilder()
                .type(Avatar.TYPE_IMAGE)
                .imageUrl(Uri.fromFile(getAvatarFile(app, "from@email")).toString())
                .build()
        )
        verify(mailView).updateAvatar(any(), any())
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun `presenter should update messages with from avatars`() {
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        mockAvatarResources()
        val messages = createMessageWithAvatars()

        val threadMeta = ReactTestFactory.buildThreadMeta()
        val threadProcessor = PublishProcessor.create<ReactThread>()
        whenever(selection.threadFlowable).thenReturn(threadProcessor)

        presenter.onBindView(mailView)
        val thread = ReactThread(messages, threadMeta)
        threadProcessor.onNext(thread)
        verify(mailView).applyNewMessages(messages, emptyList(), emptyList())

        val fields = thread.messages[0]!!.toCcBcc
        verify(mailView).updateAvatar(
            fields.from[0].email,
            fields.from[0].avatar.toBuilder()
                .type(Avatar.TYPE_IMAGE)
                .imageUrl(Uri.fromFile(getAvatarFile(app, "from@email")).toString())
                .build()
        )
        verify(mailView).updateAvatar(any(), any())

        // test caching
        threadProcessor.onNext(ReactThread(thread.messages, threadMeta))

        // from/to/cc/bcc from cache and 1 call to api(from)
        avatarRequestBuilderProxy.assertCalls(4, 1)
    }

    @Test
    @Throws(UnsupportedEncodingException::class)
    fun `presenter should update messages with to cc bcc avatars on open header`() {
        val eventsSubject = PublishSubject.create<UiEvent>()
        whenever(mailView.uiEvents()).thenReturn(eventsSubject)

        mockAvatarResources()

        val messages = createMessageWithAvatars()

        val threadMeta = ReactTestFactory.buildThreadMeta()
        val threadProcessor = PublishProcessor.create<ReactThread>()
        whenever(selection.threadFlowable).thenReturn(threadProcessor)

        presenter.onBindView(mailView)
        eventsSubject.onNext(UiEvent(UiEvent.ACTION_DOM_READY, emptyList()))

        val thread = ReactThread(messages, threadMeta)
        threadProcessor.onNext(thread)
        verify(mailView).applyNewMessages(messages, emptyList(), emptyList())

        val fields = thread.messages[0].toCcBcc
        verify(mailView).updateAvatar(any(), any()) // called only ones for from avatars

        eventsSubject.onNext(
            UiEvent(
                UiEvent.ACTION_MESSAGE_HEADER_TOGGLED,
                listOf(null, thread.messages[0].messageId().toString())
            )
        )

        verify(mailView).updateAvatar(
            fields.to[0].email,
            fields.to[0].avatar.toBuilder()
                .type(Avatar.TYPE_IMAGE)
                .imageUrl(Uri.fromFile(getAvatarFile(app, "to@email")).toString())
                .build()
        )
        verify(mailView).updateAvatar(
            fields.cc[0].email,
            fields.cc[0].avatar.toBuilder()
                .type(Avatar.TYPE_IMAGE)
                .imageUrl(Uri.fromFile(getAvatarFile(app, "cc@email")).toString())
                .build()
        )
        verify(mailView).updateAvatar(
            fields.bcc[0].email,
            fields.bcc[0].avatar.toBuilder()
                .type(Avatar.TYPE_IMAGE)
                .imageUrl(Uri.fromFile(getAvatarFile(app, "bcc@email")).toString())
                .build()
        )

        // test caching
        threadProcessor.onNext(ReactThread(thread.messages, threadMeta))

        // from/to/cc/bcc from cache and from/to/cc/bcc from api
        avatarRequestBuilderProxy.assertCalls(4, 4)
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun `presenter should update messages with attachments`() {
        val eventsSubject = PublishSubject.create<UiEvent>()
        whenever(mailView.uiEvents()).thenReturn(eventsSubject)

        val resources = mock(Resources::class.java)
        val attachRequest = mock(GlideRequest::class.java, RETURNS_DEEP_STUBS)
        whenever(resources.getDimensionPixelSize(R.dimen.message_list_avatar_size)).thenReturn(1)
        whenever(app.resources).thenReturn(resources)
        val requestBuilder = mock(RequestBuilder::class.java)
        whenever(requestManager.asBitmap()).thenReturn(requestBuilder as RequestBuilder<Bitmap>)
        whenever(requestBuilder.load(any<AttachImageParams>())).thenReturn(attachRequest as GlideRequest<Bitmap>?)
        val request1 = mock<GlideRequest<Bitmap>>()
        val request2 = mock<FutureTarget<Bitmap>>()

        whenever(attachRequest!!.centerCrop()).thenReturn(request1)
        whenever(request1.submit(anyInt(), anyInt())).thenReturn(request2)
        whenever(request2.get()).thenReturn(createBitmap())

        avatarRequestBuilderProxy.local = BitmapWrapper(createBitmap())

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(2)

        val attachmentBuilders = testAttachmentBuilders(2)
        messages[0] = messages[0]
            .toBuilder()
            .collapsed(true)
            .attachments(attachmentBuilders.map { it.build() })
            .build()
        messages[1] = messages[1]
            .toBuilder()
            .attachments(listOf(testAttachmentBuilders(1)[0].build()))
            .collapsed(true)
            .build()

        val threadMeta = ReactTestFactory.buildThreadMeta()
        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)
        eventsSubject.onNext(UiEvent(UiEvent.ACTION_DOM_READY, emptyList()))

        verify(mailView).applyNewMessages(
            eq(messages),
            eq(emptyList<ReactMessage>()),
            eq(emptyList<ReactMessage>())
        )

        // first message
        eventsSubject.onNext(
            UiEvent(
                UiEvent.ACTION_ATTACHMENT_PREVIEW,
                listOf(
                    messages[0].messageId().toString(),
                    messages[0].attachments[0].hid,
                    messages[0].attachments[1].hid
                )
            )
        )
        val attachments1 = listOf(
            messages[0].attachments[0].withImage(
                Uri.fromFile(
                    getAttachPreviewFile(
                        app,
                        mailView.uid,
                        messages[0].attachments[0].hid,
                        messages[0].messageId(),
                        messages[0].attachments[0].name
                    )
                ).toString()
            ),
            messages[0].attachments[1].withImage(
                Uri.fromFile(
                    getAttachPreviewFile(
                        app,
                        mailView.uid,
                        messages[0].attachments[1].hid,
                        messages[0].messageId(),
                        messages[0].attachments[1].name
                    )
                ).toString()
            )
        )
        for (attachment in attachments1) {
            verify(mailView).extendAttachment(messages[0].messageId(), attachment)
        }
        verify(mailView, never()).extendAttachment(eq(messages[1].messageId()), any())

        // second message
        eventsSubject.onNext(
            UiEvent(
                UiEvent.ACTION_ATTACHMENT_PREVIEW,
                listOf(
                    messages[1].messageId().toString(),
                    messages[1].attachments[0].hid
                )
            )
        )
        val attachments2 = listOf(
            messages[1].attachments[0].withImage(
                Uri.fromFile(
                    getAttachPreviewFile(
                        app,
                        mailView.uid,
                        messages[1].attachments[0].hid,
                        messages[1].messageId(),
                        messages[1].attachments[0].name
                    )
                ).toString()
            )
        )
        for (attachment in attachments2) {
            verify(mailView).extendAttachment(messages[1].messageId(), attachment)
        }

        // test caching
        eventsSubject.onNext(
            UiEvent(
                UiEvent.ACTION_ATTACHMENT_PREVIEW,
                listOf(
                    messages[0].messageId().toString(),
                    messages[0].attachments[0].hid,
                    messages[0].attachments[1].hid
                )
            )
        )

        // 3 calls to api (other calls should take image from cache)
        // verify<DrawableTypeRequest<AttachImageParams>>(attachRequest, times(3)).asBitmap()
    }

    @Test
    fun `onMessageExpand sends exhibition event with update type`() {
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val threadProcessor = PublishProcessor.create<ReactThread>()
        whenever(selection.threadFlowable).thenReturn(threadProcessor)

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(2)
        messages[0] = messages[0].toBuilder().messageId(0).body("body1").collapsed(false).build()
        messages[1] = messages[1].toBuilder().messageId(1).collapsed(true).build()
        val threadMeta = ReactTestFactory.buildThreadMeta()

        presenter.onBindView(mailView)
        threadProcessor.onNext(ReactThread(messages, threadMeta))
        shadowOf(getMainLooper()).idle()

        metrica.assertEvent(MAIL_DNS_TAG + "load xlist")

        messages[1] = messages[1].toBuilder().messageId(1).body("body2").collapsed(false).build()
        threadProcessor.onNext(ReactThread(messages, threadMeta))
        shadowOf(getMainLooper()).idle()

        val lastEvent = metrica.lastEvent
        assertThat(lastEvent).isNotNull()
        assertThat(lastEvent!!.name).isEqualTo(THREADVIEW_EXHIBITION)
        val extras = lastEvent.attributes
        assertThat(extras).isNotNull
        assertThat(extras!![EXTRA_THREADVIEW_CLOSING_TYPE]).isEqualTo(VALUE_THREADVIEW_CLOSING_TYPE_UPDATE)
        assertThat(extras[TARGET_THREAD_ID]).isEqualTo(threadId.toString())
        assertThat(extras[TARGET_MESSAGE_ID]).isEqualTo("[0]") // sends previous state
    }

    @Test
    fun `onUnbindView sends exhibition event with close type`() {
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(3)
        messages[0] = messages[0].toBuilder().messageId(0).body("body1").collapsed(false).build()
        messages[1] = messages[1].toBuilder().messageId(1).body("body2").collapsed(false).build()
        messages[2] = messages[2].toBuilder().messageId(2).collapsed(true).build()

        val threadMeta = ReactTestFactory.buildThreadMeta()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)
        shadowOf(getMainLooper()).idle()

        metrica.assertEvent(MAIL_DNS_TAG + "load xlist")

        presenter.onUnbindView(mailView)
        shadowOf(getMainLooper()).idle()

        val lastEvent = metrica.lastEvent
        assertThat(lastEvent).isNotNull()
        assertThat(lastEvent!!.name).isEqualTo(THREADVIEW_EXHIBITION)
        val extras = lastEvent.attributes
        assertThat(extras).isNotNull
        assertThat(extras!![EXTRA_THREADVIEW_CLOSING_TYPE]).isEqualTo(VALUE_THREADVIEW_CLOSING_TYPE_CLOSE)
        assertThat(extras[TARGET_THREAD_ID]).isEqualTo(threadId.toString())
        assertThat(extras[TARGET_MESSAGE_ID]).isEqualTo("[1, 0]") // top message at the beginning
    }

    @SuppressLint("CheckResult")
    @Test
    fun `loadImageForAvatar should use glide cache if requested only from cache`() {
        avatarRequestBuilderProxy.local = BitmapWrapper(createBitmap())

        val avatar = EntitiesTestFactory.avatar().build()
        presenter.loadImageForAvatar(0L, avatar, "display name", "email", true)

        // only from cache
        avatarRequestBuilderProxy.assertCalls(1, 0)
    }

    @Test
    fun `loadImageForAvatar clears color if bitmap`() {
        avatarRequestBuilderProxy.local = BitmapWrapper(createBitmap())

        val avatar = EntitiesTestFactory.avatar().build()
        val result = presenter.loadImageForAvatar(0L, avatar, "display name", "email", true)

        assertThat(result.type).isEqualTo(Avatar.TYPE_IMAGE)
        assertThat(result.color).isNull()
        assertThat(result.imageUrl).isNotEmpty()
    }

    @Test
    fun `loadImageForAvatar clears url if color`() {
        val profileInfo = EntitiesTestFactory.profileInfo(color = "ff0000")

        avatarRequestBuilderProxy.local = BitmapWrapper(null, profileInfo)

        val avatar = EntitiesTestFactory.avatar().build()
        val result = presenter.loadImageForAvatar(0L, avatar, "display name", "email", true)

        assertThat(result.type).isEqualTo(Avatar.TYPE_MONOGRAM)
        assertThat(result.color).isEqualTo("ff0000")
        assertThat(result.imageUrl).isNull()
    }

    @Test
    fun `loadImageForAvatar clears url if color null`() {
        val profileInfo = EntitiesTestFactory.profileInfo(color = null)

        avatarRequestBuilderProxy.local = BitmapWrapper(null, profileInfo)

        val avatar = EntitiesTestFactory.avatar().build()
        val result = presenter.loadImageForAvatar(0L, avatar, "display name", "email", true)

        assertThat(result.type).isEqualTo(Avatar.TYPE_MONOGRAM)
        assertThat(result.color).isNull()
        assertThat(result.imageUrl).isNull()
    }

    @Test
    fun `loadImageForAvatar replaces number sign`() {
        val profileInfo = EntitiesTestFactory.profileInfo(color = "#ff0000") // with number sign

        avatarRequestBuilderProxy.local = BitmapWrapper(null, profileInfo)

        val avatar = EntitiesTestFactory.avatar().build()
        val result = presenter.loadImageForAvatar(0L, avatar, "display name", "email", true)

        assertThat(result.color).isEqualTo("ff0000") // without number sign
    }

    @Test
    fun `uiEvent inline attach link requested loaded`() {
        val reactAttachmentsModelMock = mock(AttachmentsModel::class.java)
        val midString = "1"
        val link1 = "link1"
        val mid = java.lang.Long.parseLong(midString)

        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_INLINE_ATTACH_LINK_REQUESTED, listOf(midString, link1))))
        whenever(selection.attachmentsModel).thenReturn(reactAttachmentsModelMock)
        whenever(reactAttachmentsModelMock.getInlineAttachLink(mid, link1)).thenReturn(Single.just("inlineAttachLink1"))

        presenter.onBindView(mailView)

        verify(mailView).onInlineAttachLinkPrepared(eq(mid), eq(link1), anyString())
    }

    @Test
    fun `uiEvent inline attach link requested error`() {
        val reactAttachmentsModelMock = mock(AttachmentsModel::class.java)
        val midString = "1"
        val link1 = "link1"
        val mid = java.lang.Long.parseLong(midString)
        val exception = IllegalStateException()

        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_INLINE_ATTACH_LINK_REQUESTED, listOf(midString, link1))))
        whenever(selection.attachmentsModel).thenReturn(reactAttachmentsModelMock)
        whenever(reactAttachmentsModelMock.getInlineAttachLink(mid, link1)).thenReturn(Single.error(exception))

        presenter.onBindView(mailView)

        verify(mailView).onCanNotPrepareAttachLink(mid, link1, exception)
    }

    @Test
    fun `uiEvent message operation selected reply`() {
        val eventMessageId = 1L
        val eventMessageIdString = eventMessageId.toString()

        whenever(mailView.uiEvents())
            .thenReturn(
                Observable.just(
                    UiEvent(
                        UiEvent.ACTION_MESSAGE_OPERATION_SELECTED,
                        listOf(eventMessageIdString, ReactMessage.Action.REPLY.action)
                    )
                )
            )

        presenter.onBindView(mailView)

        verify(mailView).onReply(eventMessageId, false)
    }

    @Test
    fun `uiEvent message operation selected reply all`() {
        val eventMessageId = 1L
        val eventMessageIdString = eventMessageId.toString()

        whenever(mailView.uiEvents())
            .thenReturn(
                Observable.just(
                    UiEvent(
                        UiEvent.ACTION_MESSAGE_OPERATION_SELECTED,
                        listOf(eventMessageIdString, ReactMessage.Action.REPLY_ALL.action)
                    )
                )
            )

        presenter.onBindView(mailView)

        verify(mailView).onReply(eventMessageId, true)
    }

    @Test
    fun `uiEvent message operation selected more`() {
        val eventMessageId = 1L
        val eventMessageIdString = eventMessageId.toString()

        whenever(mailView.uiEvents())
            .thenReturn(
                Observable.just(
                    UiEvent(
                        UiEvent.ACTION_MESSAGE_OPERATION_SELECTED,
                        listOf(eventMessageIdString, ReactMessage.Action.MORE.action)
                    )
                )
            )

        presenter.onBindView(mailView)

        verify(mailView).onMore(MessageBodyDescriptor.Companion.createMessageBodyDescriptor(eventMessageId))
    }

    @Test
    fun `uiEvent message operation selected move to trash`() {
        val eventMessageId = 1L
        val eventMessageIdString = eventMessageId.toString()

        whenever(mailView.uiEvents())
            .thenReturn(
                Observable.just(
                    UiEvent(
                        UiEvent.ACTION_MESSAGE_OPERATION_SELECTED,
                        listOf(eventMessageIdString, ReactMessage.Action.MOVE_TO_TRASH.action)
                    )
                )
            )

        presenter.onBindView(mailView)

        verify(mailView).onDeleteMessagesRequested(listOf(eventMessageId))
    }

    @Test
    fun `uiEvent message operation selected compose`() {
        val eventMessageId = 1L
        val eventMessageIdString = eventMessageId.toString()

        val subject = BehaviorSubject.create<UiEvent>()
        subject.onNext(UiEvent(UiEvent.ACTION_DOM_READY, emptyList()))

        whenever(mailView.uiEvents()).thenReturn(subject)

        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(1)
        messages[0] = messages[0].toBuilder().messageId(eventMessageId).folderType(FolderType.DRAFT.serverType).build()
        val threadMeta = ReactTestFactory.buildThreadMeta()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))

        presenter.onBindView(mailView)

        subject.onNext(
            UiEvent(
                UiEvent.ACTION_MESSAGE_OPERATION_SELECTED,
                listOf(eventMessageIdString, ReactMessage.Action.COMPOSE.action)
            )
        )

        verify(mailView).onEditDraft(eventMessageId)
    }

    @Test
    fun `uiEvent on message inserted`() {
        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_ON_MESSAGE_INSERTED, emptyList())))

        presenter.onBindView(mailView)

        verify(mailView).uiEvents()
        verify(mailView, atLeastOnce()).uid
        verify(mailView).messageId
        verify(mailView).threadId
        verify(mailView).onDismissActionSelected(any())
        verify(mailView).reportAboutUiEvent(anyString())
        verifyNoMoreInteractions(mailView)
    }

    @Test
    fun `uiEvent longtap`() {
        val url = "url1"

        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_LONGTAP, listOf(url))))

        presenter.onBindView(mailView)

        verify(mailView).onLongClickLink(url)
    }

    @Test
    fun `uiEvent yable longtap`() {
        val name = "name1"
        val email = "email1@yandex.ru"

        whenever(mailView.uiEvents())
            .thenReturn(Observable.just(UiEvent(UiEvent.ACTION_YABBLE_LONGTAP, listOf(name, email))))

        presenter.onBindView(mailView)

        verify(mailView).onLongYabbleClickLink(name, email)
    }

    @Test
    fun `showTranslator should force set a new translator`() {
        val reactMessages = ReactTestFactory.buildListOfMessagesWithoutBodies(1)
        val threadMeta = ReactTestFactory.buildThreadMeta()

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(reactMessages, threadMeta)))
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))

        presenter.onBindView(mailView)

        assertThat(reactTranslatorHolder.getTranslatorMeta(1L)).isNull()

        presenter.showTranslator(1L)

        assertThat(reactTranslatorHolder.getTranslatorMeta(1L)).isNotNull()
    }

    @Test
    fun `sends quick reply`() {
        whenever(reactQuickReplyModelStateMock.getInputByMid(anyLong())).thenReturn("text")
        whenever(quickReplyModelMock.sendQuickReply(anyLong(), anyString())).thenReturn(Completable.complete())
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.QUICK_REPLY_SEND, listOf("123"))))

        presenter.onBindView(mailView)

        verify(quickReplyModelMock).sendQuickReply(123, "text")
    }

    @Test
    fun `getFocusedMessageHTMLAndLang should get first focused message html and lang`() {
        val reactMessages = ReactTestFactory.buildListOfMessagesWithBodies(2, "testBody")
        val threadMeta = ReactTestFactory.buildThreadMeta()
        val firstMessage = reactMessages[0]
        val firstMessageLang = "test"

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(reactMessages, threadMeta)))
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))
        whenever(messagesModelMock.getMessagesLang(firstMessage.messageId())).thenReturn(Single.just(Optional.of(firstMessageLang)))

        presenter.onBindView(mailView)

        val focusedMessageHTMLAndLang = presenter.focusedMessageHTMLAndLang!!
        val focusedMessageHTML = focusedMessageHTMLAndLang.first
        val focusedLang = focusedMessageHTMLAndLang.second

        assertThat(firstMessage.body).isEqualTo(focusedMessageHTML)
        assertThat(firstMessageLang).isEqualTo(focusedLang)
    }

    @Test
    fun `getFocusedMessageHTMLAndLang should get second tapped focused message html and lang`() {
        val reactMessages = ReactTestFactory.buildListOfMessagesWithBodies(2, "testBody")
        val threadMeta = ReactTestFactory.buildThreadMeta()
        val secondMessage = reactMessages[1]
        val secondMessageLang = "test"

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(reactMessages, threadMeta)))
        whenever(mailView.uiEvents()).thenReturn(
            Observable.just(
                UiEvent(UiEvent.ACTION_DOM_READY, emptyList()),
                UiEvent(UiEvent.ACTION_LOAD_MESSAGE_BODY, listOf(secondMessage.messageId().toString()))
            )
        )
        whenever(messagesModelMock.getMessagesLang(secondMessage.messageId())).thenReturn(Single.just(Optional.of(secondMessageLang)))

        presenter.onBindView(mailView)

        val focusedMessageHTMLAndLang = presenter.focusedMessageHTMLAndLang!!
        val focusedMessageHTML = focusedMessageHTMLAndLang.first
        val focusedLang = focusedMessageHTMLAndLang.second

        assertThat(secondMessage.body).isEqualTo(focusedMessageHTML)
        assertThat(secondMessageLang).isEqualTo(focusedLang)
    }

    @Test
    fun `getFocusedMessageHTMLAndLang should get focused message`() {
        initPresenter(scrollToExpanded = true)

        val messages = listOf(buildMessageWithIdWithoutBody(1L), buildMessageWithIdWithBody(2L, "body"))
        val threadMeta = ReactTestFactory.buildThreadMeta()
        val secondMessage = messages[1]
        val secondMessageLang = "test"

        whenever(selection.threadFlowable).thenReturn(Flowable.just(ReactThread(messages, threadMeta)))
        whenever(mailView.messageId).thenReturn(messages[1].messageId())
        whenever(mailView.uiEvents()).thenReturn(Observable.just(UiEvent(UiEvent.ACTION_DOM_READY, emptyList())))
        whenever(messagesModelMock.getMessagesLang(secondMessage.messageId())).thenReturn(Single.just(Optional.of(secondMessageLang)))

        presenter.onBindView(mailView)

        val focusedMessageHTMLAndLang = presenter.focusedMessageHTMLAndLang!!
        val focusedMessageHTML = focusedMessageHTMLAndLang.first
        val focusedLang = focusedMessageHTMLAndLang.second

        assertThat(secondMessage.body).isEqualTo(focusedMessageHTML)
        assertThat(secondMessageLang).isEqualTo(focusedLang)
    }

    private fun createBitmap(): Bitmap {
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
    }

    private fun testAttachmentBuilders(count: Int): List<Attachment.Builder> {
        val builders = ArrayList<Attachment.Builder>(count)
        for (i in 0 until count) {
            builders.add(
                Attachment.Builder()
                    .previewUrl("fake$i")
                    .type(Attachment.TYPE_FILE)
                    .hid(i.toString())
                    .name("attach$i")
                    .size("100")
                    .supportsPreview(false)
                    .extension("ext$i")
                    .hasThumbnail(false)
                    .disk(false)
            )
        }
        return builders
    }

    private fun mockAvatarResources() {
        val resources = mock(Resources::class.java)
        whenever(resources.getDimensionPixelSize(R.dimen.message_list_avatar_size)).thenReturn(1)
        whenever(app.resources).thenReturn(resources)

        avatarRequestBuilderProxy.local = BitmapWrapper()
        avatarRequestBuilderProxy.remote = BitmapWrapper(createBitmap())
    }

    private fun createMessageWithAvatars(): List<ReactMessage> {
        val messages = ReactTestFactory.buildListOfMessagesWithoutBodies(2)
        messages[0] = messages[0]
            .toBuilder()
            .toCcBcc(
                Fields(
                    to = listOf(Recipient.create("to@email", null, Avatar.Builder().monogram("monogram4").build())),
                    from = listOf(Recipient.create("from@email", null, Avatar.Builder().monogram("monogram1").build())),
                    cc = listOf(Recipient.create("cc@email", null, Avatar.Builder().monogram("monogram3").build())),
                    bcc = listOf(Recipient.create("bcc@email", null, Avatar.Builder().monogram("monogram2").build()))
                )
            )
            .collapsed(true)
            .build()
        messages[1] = messages[1]
            .toBuilder()
            .collapsed(true)
            .build()
        return messages
    }

    internal inner class TestAvatarRequestBuilderProxy : AvatarRequestBuilderProxy() {

        var local: BitmapWrapper? = null

        private var localRequests: Int = 0

        var remote: BitmapWrapper? = null

        private var remoteRequests: Int = 0

        override fun glideAvatarLoadRequest(
            context: Context,
            requestManager: RequestManager,
            displayName: String,
            email: String,
            uid: Long,
            skipNetwork: Boolean
        ): BitmapWrapper {
            return if (skipNetwork) {
                localRequests++
                local!!
            } else {
                remoteRequests++
                remote!!
            }
        }

        fun assertCalls(local: Int, remote: Int) {
            assertThat(localRequests).isEqualTo(local)
            assertThat(remoteRequests).isEqualTo(remote)
        }
    }
}
