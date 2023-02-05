package com.yandex.mail.compose

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.text.util.Rfc822Token
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.yandex.mail.BaseMailApplication.Companion.getAccountComponent
import com.yandex.mail.R
import com.yandex.mail.asserts.MetricaConditions
import com.yandex.mail.asserts.ViewConditions.focused
import com.yandex.mail.asserts.ViewConditions.hierarchySize
import com.yandex.mail.asserts.ViewConditions.snackbarWithText
import com.yandex.mail.asserts.action
import com.yandex.mail.asserts.keyWithLongValue
import com.yandex.mail.asserts.uid
import com.yandex.mail.attach.OrderedUriAttach
import com.yandex.mail.compose.ComposeAction.EDIT_DRAFT
import com.yandex.mail.compose.ComposeFragment.Requests
import com.yandex.mail.compose.ComposeFragmentTest.ModifiedField.CONTENT
import com.yandex.mail.compose.ComposeFragmentTest.ModifiedField.SENDER
import com.yandex.mail.compose.ComposeMetaController.State.COLLAPSED
import com.yandex.mail.compose.ComposeMetaController.State.EXPANDED
import com.yandex.mail.compose.ComposeMetaController.State.FULLY_EXPANDED
import com.yandex.mail.compose.WebViewComposeContent.JS_INTERFACE_NAME
import com.yandex.mail.conforms
import com.yandex.mail.entity.DraftAttachEntry
import com.yandex.mail.metrica.MetricaConstns
import com.yandex.mail.metrica.MetricaConstns.EventMetrics.ComposeEventMetrics
import com.yandex.mail.network.json.response.Recipient
import com.yandex.mail.network.request.MailSendRequest
import com.yandex.mail.network.tasks.NanoSaveDraftTask
import com.yandex.mail.notifications.NotificationsConstants
import com.yandex.mail.provider.Constants.ACTION_EXTRA
import com.yandex.mail.provider.Constants.DRAFT_ID_EXTRAS
import com.yandex.mail.provider.Constants.MESSAGE_ID_EXTRAS
import com.yandex.mail.provider.Constants.NO_MESSAGE_ID
import com.yandex.mail.react.ReactMustacheHandling.Compose.Value.Companion.SUFFIX
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.service.CommandsServiceActions
import com.yandex.mail.service.CommandsServiceActions.MARK_AS_READ_ACTION
import com.yandex.mail.settings.dialog.SpeechkitTermsDialogFragment
import com.yandex.mail.storage.entities.EntitiesTestFactory
import com.yandex.mail.tools.Accounts
import com.yandex.mail.tools.FragmentController
import com.yandex.mail.tools.LocalHelper
import com.yandex.mail.tools.RobolectricTools
import com.yandex.mail.tools.ServerHelper
import com.yandex.mail.tools.TestFragmentActivity
import com.yandex.mail.tools.TestWorkerFactory.WorkerInfo
import com.yandex.mail.tools.Tools
import com.yandex.mail.util.BaseIntegrationTest
import com.yandex.mail.util.UnexpectedCaseException
import com.yandex.mail.util.Utils.arrayToList
import com.yandex.mail.util.compress.CompressType
import com.yandex.mail.util.mailbox.Mailbox
import com.yandex.mail.util.mailbox.MailboxEditor.Folder.createFolder
import com.yandex.mail.wrappers.AttachmentWrapper
import com.yandex.mail.wrappers.MessageWrapper
import com.yandex.mail.yables.YableEditTextView
import com.yandex.mail.yables.YableReflowView
import com.yandex.mail.yables.YableReflowViewAssert.assertThat
import com.yandex.mail.yables.YableReflowViewTest
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.assertj.core.condition.AllOf.allOf
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.util.ReflectionHelpers
import java.io.File
import java.util.concurrent.TimeUnit

@RunWith(IntegrationTestRunner::class)
class ComposeFragmentTest : BaseIntegrationTest() {

    @Rule
    @JvmField
    var tmpFolder = TemporaryFolder()

    @Before
    fun `before each test`() {
        init(Accounts.testLoginData)
        shadowOf(app).grantPermissions(
            permission.WRITE_EXTERNAL_STORAGE,
            permission.READ_EXTERNAL_STORAGE,
            permission.CAMERA,
            permission.READ_CONTACTS
        )
    }

    fun createController(intent: Intent) =
        ComposeFragmentController(TestComposeFragment.create(user.uid, intent), intent)

    fun createMessage(
        folderName: String,
        from: String = DEFAULT_FROM,
        to: List<String> = emptyList(),
        cc: List<String> = emptyList(),
        bcc: List<String> = emptyList()
    ): MessageWrapper {
        val message = account.newReadMessage(getServerFolder(folderName))
            .content("Message!")
            .from(from)
            .to(to)
            .cc(cc)
            .bcc(bcc)
            .build()
        account.addMessages(message)
        user.fetchMessages(folderName)
        return message
    }

    fun createMessage(
        folder: String,
        from: String,
        toCount: Int,
        ccCount: Int,
        bccCount: Int
    ) = createMessage(
        folder,
        from,
        createMessagesRange("to", toCount),
        createMessagesRange("to", ccCount),
        createMessagesRange("to", bccCount)
    )

    fun createDraft(toCount: Int, ccCount: Int, bccCount: Int) =
        createMessage(
            DRAFTS_FOLDER,
            DEFAULT_FROM,
            toCount,
            ccCount,
            bccCount
        )

    fun createDraftWithRecognizer() = IntegrationTestRunner.app().createNewDraftIntentWithRecognizer(user.uid)

    fun createEditIntent(localMid: Long, fromUndo: Boolean = false) =
        IntegrationTestRunner.app().createEditDraftIntent(user.uid, localMid, fromUndo)

    fun createReplyIntent(localMid: Long) = IntegrationTestRunner.app().createReplyIntent(user.uid, localMid, false)

    fun createReplyAllIntent(localMid: Long) = IntegrationTestRunner.app().createReplyIntent(user.uid, localMid, true)

    fun createForwardIntent(localMid: Long) = IntegrationTestRunner.app().createForwardIntent(user.uid, localMid)

    fun createNewMessageIntent() = IntegrationTestRunner.app().createNewDraftIntent(user.uid)

    fun createShareTextIntent(text: String) = Intent(Intent.ACTION_SENDTO).apply {
        putExtra(Intent.EXTRA_TEXT, text)
    }

    fun createMailToWithBodyIntent(to: String, body: String) = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$to?body=$body")
    }

    // Twitter uses scheme like this
    fun createMailToWithExtraTextIntent(body: String) = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_TEXT, body)
    }

    override fun get(dummy: LocalHelper) = user

    override fun get(dummy: ServerHelper) = account

    private enum class ModifiedField {
        CONTENT,
        SENDER
    }

    private fun testSavesHelper(content: ModifiedField) {
        val message = createMessage(DRAFTS_FOLDER)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid))) {
            prepareFragment()

            when (content) {
                CONTENT -> appendToBody("test")
                SENDER -> senderSelection.setSelection(0) // should be at least one item
                else -> throw UnexpectedCaseException(content)
            }

            assertThat(saveIntent).isNull()
            // A request to ComposeStoreService should be submitted on pause.
            pause()
            assertThat(saveIntent).isNotNull()
        }
    }

    /**
     * Tests that draft saving request is sent on onPause
     */
    @Test
    fun `test saves on content changes`() {
        testSavesHelper(CONTENT)
    }

    /**
     * Tests that on no specific sender, the default sender is set
     */
    @Test
    fun `test sets default sender`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            assertThat(sender).isEqualTo(user.defaultEmail)
        }
    }

    /**
     * Tests that the default sender is set if the sender in existing draft is no more valid
     */
    @Test
    fun `test unknown sender`() {
        val message = createMessage(
            DRAFTS_FOLDER,
            "from54353456@gdfgdf.com",
            emptyList(),
            emptyList(),
            emptyList()
        )
        with(createController(createEditIntent(user.getLocalMessage(message).localMid))) {
            prepareFragment()

            assertThat(sender).isEqualTo(user.defaultEmail)
        }
    }

    /**
     * Tests that on sender change, the draft gets saved
     */
    @Test
    fun `test saves on sender selection`() {
        testSavesHelper(SENDER)
    }

    /**
     * Tests that on sender removal, the draft gets saved
     */
    @Test
    fun `test saves on recipient removal`() {
        val message = createMessage(
            DRAFTS_FOLDER,
            "from54353456@gdfgdf.com",
            listOf("to@ya.ru"),
            emptyList(),
            emptyList()
        )
        with(createController(createEditIntent(user.getLocalMessage(message).localMid))) {
            prepareFragment()

            to.removeAllYables()
            pause()
        }
        assertThat(saveIntent).isNotNull()
    }

    @Test
    fun `test sends draft`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            to.createYable("test@ya.ru", true)
            subject.setText("message")
            clickSend()
            shadowOf(getMainLooper()).idle()

            // A request to ComposeStoreService should be submitted.
            assertThat(sendIntent).isNotNull()
        }
    }

    /**
     * Tests that the message does not get sent if there are no recipients
     */
    @Test
    fun testNoSendOnNoRecipients() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            subject.setText("message")
            clickSend()

            assertThat(sendIntent).isNull()
        }
    }

    internal fun testNoSendOnInvalidRecipientsHelper(field: Field) {
        val controller = createController(createNewMessageIntent())
        controller.prepareFragment()

        with(controller.getField(field)) {
            createYable("hi@ya.ru", true)
            createYable("trash", true)
            createYable("bye@ya.ru", true)
        }

        controller.clickSend()

        assertThat(sendIntent).isNull()
    }

    /**
     * Tests that the message does not get sent if there is an invalid recipient in TO
     */
    @Test
    fun `no send on invalid recipients helper to`() {
        testNoSendOnInvalidRecipientsHelper(Field.TO)
    }

    /**
     * Tests that the message does not get sent if there is an invalid recipient in CC
     */
    @Test
    fun `no send on invalid recipients helper cc`() {
        testNoSendOnInvalidRecipientsHelper(Field.CC)
    }

    /**
     * Tests that the message does not get sent if there is an invalid recipient in BCC
     */
    @Test
    fun `no send on invalid recipients helper bcc`() {
        testNoSendOnInvalidRecipientsHelper(Field.BCC)
    }

    private fun testExpandedHelper(expand: Boolean) {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            if (expand) {
                toggleExpand()
            }

            if (expand) {
                assertCcBccVisible()
            } else {
                assertCcBccInvisible()
            }

            pause()
            resume().visible()

            if (expand) {
                assertCcBccVisible()
            } else {
                assertCcBccInvisible()
            }
        }
    }

    @Test
    fun `expanded not visible`() {
        testExpandedHelper(false)
    }

    @Test
    fun `expanded visible`() {
        testExpandedHelper(true)
    }

    @Test
    fun `no toast on no changes`() {
        val message = createMessage(DRAFTS_FOLDER)
        with(createController(createReplyIntent(user.getLocalMessage(message).localMid))) {
            prepareFragment()
            pause()

            assertThat(saveIntent).isNull()
            assertThat(sendIntent).isNull()
        }
    }

    /**
     * Tests that an empty reply draft gets removed on closing it, and the original message doesn't
     */
    @Test
    fun `reply and close`() {
        val mailbox = Mailbox.nonThreaded(this)
            .folder(createFolder().folderId(inboxFid()).addReadMessages(1))
            .applyAndSync()

        val originalMessage = mailbox.folder(inboxFid()).messages().firstOrNull()!!

        with(createController(createReplyIntent(originalMessage.meta.mid))) {
            prepareFragment()

            val replyDid = draftId
            val replyMid = draftsModel.getMidByDidOrThrow(replyDid).blockingGet()

            activityController.get().finish() // it should not be configurations changes
            pause().stop().destroy()

            assertThat(mailbox.folder(inboxFid()).messages()).contains(originalMessage)
            assertThat(messagesModel.getMessageByMid(replyMid).blockingGet().isPresent).isFalse()
            assertThat(draftsModel.getMidByDid(replyDid).blockingGet().isPresent).isFalse()
        }
    }

    /**
     * Enter two addresses in TO, they should appear as yables
     *
     *
     * TODO: should probably test Yable views, not compose fragment
     */
    @Test
    fun `creates yables to`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            testCreatesYablesHelper(to)
        }
    }

    /**
     * Enter two addresses in CC, they should appear as yables.
     */
    @Test
    fun `creates yables cc`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            toggleExpand()
            testCreatesYablesHelper(cc)
        }
    }

    /**
     * Enter two addresses in BCC, they should appear as yables.
     */
    @Test
    fun `creates yables bcc`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            toggleExpand()
            testCreatesYablesHelper(bcc)
        }
    }

    /**
     * Just a simple counter test.
     *
     *
     * TODO actually tests [ComposeMetaController]
     */
    @Test
    fun `test counter`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toggleExpand()
            cc.createYable("testCc@ya.ru", false)
            bcc.createYable("testBcc@ya.ru", true)
            to.createYable("testTo@ya.ru", true)
            toggleExpand()
            assertShowsCounter(2)

            toggleExpand()
            bcc.removeAllYables()
            toggleExpand()
            assertShowsCounter(1)
        }
    }

    @Test
    fun `no counter on no recipients`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            fillRecipients(to, 0)
            assertNoCounter()
        }
    }

    @Test
    fun `no counter on one recipient to`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            fillRecipients(to, 1)
            assertNoCounter()
        }
    }

    /**
     * Expand, add one recipient to CC, collapse.
     * The counter is expected to be shown.
     *
     * TODO: how to provoke focus changes?
     */
    @Test
    fun `shows counter one one recipient cc`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            fillRecipients(cc, 1)
            assertShowsCounter(1)
        }
    }

    @Test
    fun `no counter if expanded`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toggleExpand()
            cc.createYable("testCc@ya.ru", false)
            bcc.createYable("testBcc@ya.ru", false)
            assertNoCounter()

            to.createYable("testTo@ya.ru", false)
            assertNoCounter()

            toEditText.requestFocus()
            assertNoCounter()
            toEditText.clearFocus()
            assertNoCounter()
        }
    }

    /**
     * Test for https://st.yandex-team.ru/MOBILEMAIL-3454
     * +1 in the TO field should stay there once we lose focus
     */
    @Test
    fun `counter does not disappear MOBILEMAIL-3454`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toggleExpand() // expand
            shadowOf(getMainLooper()).idle()
            cc.requestFocus()
            cc.createYable("testCc@ya.ru", false)
            to.requestFocus()
            toEditText.append("tralala")
            toggleExpand() // collapse
            shadowOf(getMainLooper()).idle()

            assertCcBccInvisible()
            assertShowsCounter(1)
        }
    }

    /**
     * Test for https://st.yandex-team.ru/MOBILEMAIL-3281
     */
    @Test
    fun `counter does not disappear MOBILEMAIL-3281`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toggleExpand() // expand copy block
            cc.createYable("testCc@ya.ru", false)
            bcc.createYable("testBcc@ya.ru", false)
            toggleExpand() // collapse

            assertShowsCounter(2)

            toEditText.requestFocus() // tap in TO
            to.createYable("testTo@ya.ru", false)
            to.removeAllYables()

            subject.requestFocus()

            assertShowsCounter(2)
        }
    }

    /**
     * Loads a message with two recipients in TO.
     * Checks that TO gets expanded on gaining focus and copy block does not get expanded.
     */
    @Test
    fun `expands on focus`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid))) {
            prepareFragment()

            toEditText.requestFocus()

            assertNoCounter()
            assertCcBccInvisible()
        }
    }

    /**
     * Loads a message with one recipient in TO, one in CC.
     * Checks that everything gets collapsed on CC losing focus.
     */
    @Test
    fun `collapses on lost focus`() {
        val message = createDraft(1, 1, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid))) {

            // There's a robolectric issue with spinner. During creating list of views any added view calls requestLayout() which is working immediately.
            // As a result, we'll catch StackOverflowError. To prevent it we should pause main looper during spinner preparing
            create().start().resume().visible()
            shadowOf(getMainLooper()).idle()
            prepareContent()
            shadowOf(getMainLooper()).idle()

            ccEditText.requestFocus()
            subject.requestFocus()

            shadowOf(getMainLooper()).idle()
            Robolectric.getForegroundThreadScheduler().advanceBy(400, TimeUnit.MILLISECONDS)
            assertShowsCounter(1)
            assertCcBccInvisible()
        }
    }

    @Test
    fun `recipients frame focuses on to after collapse if cc is empty`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toggleExpand() // expand
            ccEditText.requestFocus() // tap in Cc
            toggleExpand() // collapse
            assertThat(toEditText.hasFocus()).isTrue()
        }
    }

    @Test
    fun `recipients frame loses focus to subject if cc is not empty`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toggleExpand() // expand
            val address = "test@ya.ru"
            ccEditText.requestFocus() // tap in Cc
            ccEditText.append(address)
            toggleExpand() // collapse
            assertThat(subject.hasFocus()).isTrue()
        }
    }

    /**
     * Test for https://st.yandex-team.ru/MOBILEMAIL-4927
     * Checks that on recipients frame losing focus it goes wherever requested
     */
    @Test
    fun `MOBILEMAIL-4927`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toEditText.requestFocus() // tap in To
            subject.requestFocus() // tap in Subject
            assertThat(subject.hasFocus()).isTrue() // focus should stay in Subject
        }
    }

    /**
     * Loads a message with two recipients in TO.
     * Checks that recipients are expanded on loading.
     */
    @Test
    fun `not expanded initially to`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid))) {
            prepareFragment()

            assertNoCounter()
            assertCcBccInvisible()
        }
    }

    /**
     * Loads a message with one recipient in BCC.
     * Checks that recipients are expanded on loading.
     */
    @Test
    fun `expanded initially cc bcc`() {
        val message = createDraft(0, 0, 1)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid))) {
            create().start().resume().visible()
            prepareContent()

            shadowOf(getMainLooper()).idle()

            assertNoCounter()
            assertCcBccVisible()
        }
    }

    /**
     * Loads a message with several recipients in TO, clicks the arrow two times.
     * Checks that the counter has the correct value and the copy block is hidden.
     */
    @Test
    fun `counter expand collapse`() {
        val toCount = 3
        val message = createDraft(toCount, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid))) {
            prepareFragment()

            toggleExpand() // expands CC and BCC
            shadowOf(getMainLooper()).idle()
            toggleExpand() // collapses
            shadowOf(getMainLooper()).idle()

            assertShowsCounter(toCount - 1)
            assertCcBccInvisible()
        }
    }

    @Test
    fun `commits yable on collapse`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toggleExpand() // expand
            toEditText.requestFocus()
            val address = "test@ya.ru"
            toEditText.append(address)
            toggleExpand() // collapse
            assertThat(to).hasYable(address)
        }
    }

    @Test
    fun `commits yable on collapse cc`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toggleExpand() // expand
            toEditText.requestFocus()
            toEditText.append("to@ya.ru")
            ccEditText.requestFocus()
            ccEditText.append("cc@ya.ru")

            toggleExpand() // collapse
            assertShowsCounter(1)
        }
    }

    @Test
    fun `commit yable should commit with left round bracket`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toEditText.requestFocus()
            toEditText.append("(")
            subject.requestFocus()

            assertThat(to).hasYable("(")
        }
    }

    /**
     * TODO move this test to YableReflowViewTest
     */
    @Test
    fun `edit on tap`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            val reflow = to
            val addresses = arrayToList("test@ya.ru", "test2@ya.ru")
            for (address in addresses) {
                reflow.createYable(address, true)
            }
            val yable = reflow.childYables.first()
            val address = yable.contactInfo.address
            toggleExpand() // expand

            YableReflowViewTest.tapYable(yable)

            assertThat(reflow.yablesCount()).isEqualTo(addresses.size - 1)

            val edit = reflow.yableTextView
            assertThat(edit.text).contains(address)
        }
    }

    /**
     * Tests that on reply we reply only to the one who sent us the message
     */
    @Test
    fun testRepliesOnlyFrom() {
        val helper = object : TestRepliesHelper() {
            override fun runAsserts() {
                assertThat(defaultEmail).isNot(containedIn(data.to))
                assertThat(from).conforms(containedIn(data.to))
                assertThat(to).areNot(containedIn(data.to))
                assertThat(cc).areNot(containedIn(data.cc))
                assertThat(bcc).areNot(containedIn(data.to))
                assertThat(bcc).areNot(containedIn(data.cc!!))
                assertThat(bcc).areNot(containedIn(data.bcc))
            }
        }
        with(helper) {
            val controller = prepare(false, false)
            extractDraftData(controller)
            shadowOf(getMainLooper()).idle()
            runAsserts()
        }
    }

    /**
     * Tests that on reply all:
     * 1. BCC recipients are not on reply recipients list
     * 2. All of TO recipients except the user are in TO
     * 3. All of CC recipients are in CC
     */
    @Test
    fun testReplyAll() {
        val helper = object : TestRepliesHelper() {
            override fun runAsserts() {
                assertThat(defaultEmail).isNot(containedIn(data.to))
                assertThat(from).conforms(containedIn(data.to))
                assertThat(to).are(containedIn(data.to))
                assertThat(cc).are(containedIn(data.cc))
                assertThat(bcc).areNot(containedIn(data.to))
                assertThat(bcc).areNot(containedIn(data.cc))
                assertThat(bcc).areNot(containedIn(data.bcc))
            }
        }
        with(helper) {
            val controller = prepare(true, false)
            extractDraftData(controller)
            shadowOf(getMainLooper()).idle()
            runAsserts()
        }
    }

    /**
     * look at https://st.yandex-team.ru/MOBILEMAIL-3461
     */
    @Test
    fun testReplyFromSent() {
        val helper = object : TestRepliesHelper() {
            override fun runAsserts() {
                assertThat(defaultEmail).isNot(containedIn(data.to))
                assertThat(cc).areNot(containedIn(data.cc))
                assertThat(bcc).areNot(containedIn(data.to))
                assertThat(bcc).areNot(containedIn(data.cc))
                assertThat(bcc).areNot(containedIn(data.bcc))
            }
        }
        with(helper) {
            val controller = prepare(false, true)
            extractDraftData(controller)
            shadowOf(getMainLooper()).idle()
            runAsserts()
        }
    }

    @Test
    fun testReplyAllFromSent3461() {
        val helper = object : TestRepliesHelper() {
            override fun runAsserts() {
                assertThat(defaultEmail).isNot(containedIn(data.to))
                assertThat(to).are(containedIn(data.to))
                assertThat(cc).are(containedIn(data.cc))
                assertThat(bcc).areNot(containedIn(data.to))
                assertThat(bcc).areNot(containedIn(data.cc))
                assertThat(bcc).areNot(containedIn(data.bcc))
            }
        }
        with(helper) {
            val controller = prepare(true, true)
            extractDraftData(controller)
            shadowOf(getMainLooper()).idle()
            runAsserts()
        }
    }

    @Test
    fun `compose preserves references on edit`() {
        val serverMessage = account
            .newReadMessage(serverDrafts())
            .rfcId("<123@test.ru>")
            .references("<m-1@ya.ru>", "<m-2@google.com>")
            .build()
        account.addMessages(serverMessage)
        user.fetchMessages(drafts())
        with(createController(createEditIntent(user.getLocalMessage(serverMessage).localMid))) {
            prepareFragment()

            var draftData = DraftData.create(get(), true)
            draftData = get().presenter.composeStrategy.modifyDraftBeforeStore(draftData).blockingGet()
            shadowOf(getMainLooper()).idle()

            val requestCaptor = argumentCaptor<MailSendRequest>()
            val task = spy(NanoSaveDraftTask(RuntimeEnvironment.application, draftData, 1L))
            ReflectionHelpers.setField(task, "newUploadedAttaches", emptyList<DraftAttachEntry>())
            task.sendDataToServer(RuntimeEnvironment.application)
            shadowOf(getMainLooper()).idle()

            verify(task).performNetworkOperationWithRequest(any(), requestCaptor.capture())

            val request = requestCaptor.firstValue
            assertThat(request.references).isEqualTo("<m-1@ya.ru> <m-2@google.com>")

            // TODO unfortunately, backend doesn't send us In-Reply-To header :(
            // see https://st.yandex-team.ru/MOBILEMAIL-7016
            // assertThat(request.getInReplyTo()).isEqualTo("<123@test.ru>");
        }
    }

    /**
     * Sorry for such an ugly test.. but until we refactor compose fragment, there is not much we can do.
     * See https://st.yandex-team.ru/MOBILEMAIL-7016
     */
    @Test
    fun `compose appends references on reply`() {
        val serverMessage = account
            .newReadMessage(serverInbox())
            .rfcId("<123@test.ru>")
            .references("<m-1@ya.ru>", "<m-2@google.com>")
            .build()
        account.addMessages(serverMessage)
        user.fetchMessages(inbox())
        with(createController(createReplyIntent(user.getLocalMessage(serverMessage).localMid))) {
            prepareFragment()

            get().presenter.sendMessage(DraftData.create(get(), true))
            shadowOf(getMainLooper()).idle()

            val worker = workerFactory.getAllStartedWorkers()
                .first { worker ->
                    worker.getInputString(ACTION_EXTRA) in listOf(
                        CommandsServiceActions.SAVE_DRAFT_ACTION,
                        CommandsServiceActions.SEND_MAIL_ACTION
                    )
                }
            shadowOf(getMainLooper()).idle()
            val data = DraftData.deserializeFromData(worker.workerParameters.inputData)!!
            assertThat(data.references).isEqualTo("<m-1@ya.ru> <m-2@google.com> <123@test.ru>")
            assertThat(data.rfcId).isEqualTo("<123@test.ru>")
        }
    }

    @Test
    fun `compose reply duplicate emails`() {
        val serverMessage = account
            .newReadMessage(serverInbox())
            .apply {
                recipients.add(Recipient("Default-Fake-sender@ya.ru", "Default fake name", Recipient.Type.FROM))
            }
            .build()
        account.addMessages(serverMessage)
        user.fetchMessages(inbox())
        with(createController(createReplyIntent(user.getLocalMessage(serverMessage).localMid))) {
            prepareFragment()
            shadowOf(getMainLooper()).idle()

            get().presenter.sendMessage(DraftData.create(get(), true))
            shadowOf(getMainLooper()).idle()

            val worker = workerFactory.getAllStartedWorkers()
                .first { worker ->
                    worker.getInputString(ACTION_EXTRA) in listOf(
                        CommandsServiceActions.SAVE_DRAFT_ACTION,
                        CommandsServiceActions.SEND_MAIL_ACTION
                    )
                }
            shadowOf(getMainLooper()).idle()
            val data = DraftData.deserializeFromData(worker.workerParameters.inputData)!!
            assertThat(data.to).isEqualToIgnoringCase("Default fake name <default-fake-sender@ya.ru>;")
        }
    }

    @Test
    fun `compose show attachments on forward`() {
        val serverMessage = account
            .newReadMessage(serverInbox())
            .content("content")
            .attachments(
                listOf(
                    AttachmentWrapper.newTextAttachment("attachmentName1", "attachmentContent1", "hid1"),
                    AttachmentWrapper.newTextAttachment("attachmentName2", "attachmentContent2", "hid2")
                )
            )
            .build()

        account.addMessages(serverMessage)
        user.fetchMessages(inbox())

        with(createController(createForwardIntent(user.getLocalMessage(serverMessage).localMid))) {
            prepareFragment()
            shadowOf(getMainLooper()).idle()
            assertThat(attachmentsList).has(hierarchySize(2))
        }
    }

    @Test
    fun `onPickAttachments uri from intent`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            val intent = Intent()
            val uri = Uri.fromFile(File("testAttachment.txt"))
            intent.data = uri

            val composeFragment = spy(get())
            composeFragment.onPickAttachments(intent)
            verify(composeFragment).onPickAttachmentsFromUri(setOf(uri))
        }
    }

    @Test
    fun `onPickAttachments clip data from intent`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            val intent = Intent()
            val uri1 = Uri.fromFile(File("testAttachment1.txt"))
            val clipData = ClipData("", arrayOfNulls<String>(0), ClipData.Item(uri1))
            val uri2 = Uri.fromFile(File("testAttachment2.txt"))
            clipData.addItem(ClipData.Item(uri2))
            intent.clipData = clipData

            val composeFragment = spy(get())
            composeFragment.onPickAttachments(intent)
            verify(composeFragment).onPickAttachmentsFromUri(setOf(uri1, uri2))
        }
    }

    @Test
    fun `onPickAttachments clip data and uri intent`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            val intent = Intent()
            val uri1 = Uri.fromFile(File("testAttachment1.txt"))
            val uri2 = Uri.fromFile(File("testAttachment2.txt"))
            val clipData = ClipData("", arrayOfNulls<String>(0), ClipData.Item(uri1))
            clipData.addItem(ClipData.Item(uri2))
            intent.clipData = clipData
            intent.data = uri1

            val composeFragment = spy(get())
            composeFragment.onPickAttachments(intent)
            verify(composeFragment).onPickAttachmentsFromUri(setOf(uri1, uri2))
        }
    }

    @Test
    fun `onPickAttachments empty intent`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            val composeFragment = spy(get())
            composeFragment.onPickAttachments(Intent())
            verify(composeFragment, never()).onPickAttachmentsFromUri(any())
        }
    }

    @Test
    fun `onPickAttachmentFromUri one file failed toast`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            val fragment = get()
            fragment.onPickAttachmentsFromUri(setOf(Uri.fromFile(File("unknown_file"))))
            shadowOf(getMainLooper()).idle()

            val root = get().activity!!.findViewById<ViewGroup>(android.R.id.content)
            assertThat(get().view).has(snackbarWithText(root, R.string.failed_to_attach_file))
        }
    }

    @Test
    fun `onPickAttachmentFromUri many files failed toast`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            val fragment = get()

            val fakeFileUri1 = Uri.fromFile(File("fake_file1"))
            val fakeFileUri2 = Uri.fromFile(File("fake_file2"))
            fragment.onPickAttachmentsFromUri(setOf(fakeFileUri1, fakeFileUri2))
            shadowOf(getMainLooper()).idle()

            val root = get().activity!!.findViewById<ViewGroup>(android.R.id.content)
            assertThat(get().view).has(snackbarWithText(root, R.string.failed_to_attach_files))
        }
    }

    @Test
    fun `should mark existing draft as read`() {
        val mailbox = Mailbox.nonThreaded(this)
            .folder(createFolder().folderId(draftsFid()).addUnreadMessages(1))
            .applyAndSync()
        val originalMessage = mailbox.folder(draftsFid()).messages().firstOrNull()!!

        with(createController(createEditIntent(originalMessage.meta.mid))) {
            prepareFragment()

            val workers = workerFactory.getAllStartedWorkers().filter { worker -> MARK_AS_READ_ACTION == worker.getInputString(ACTION_EXTRA) }
            assertThat(workers).hasSize(1)
            assertThat(workers.first().getInputLongArray(MESSAGE_ID_EXTRAS)).isEqualTo(longArrayOf(originalMessage.meta.mid))
        }
    }

    @SuppressLint("SetTextI18n")
    @Test
    fun `fragment should not crash on destroy if yable not created`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            toEditText.setText("testFrom@ya.ru")

            pause().stop().destroy()
        }
    }

    @Test
    fun `fragment pick file after restore state`() {
        val intent = createNewMessageIntent()

        val activityBeforeRestoreController =
            ComposeFragmentController(TestComposeFragment.create(user.uid, intent), intent)
        activityBeforeRestoreController.create().start().resume().visible()

        val state = Bundle()
        activityBeforeRestoreController.activityController.saveInstanceState(state)
        activityBeforeRestoreController.pause().stop().destroy()

        val draftId = activityBeforeRestoreController.draftId

        val activityAfterRestoreController = Robolectric.buildActivity(ComposeTestFragmentActivity::class.java, intent)
        activityAfterRestoreController.create(state).start().resume()

        val restoredFragment = activityAfterRestoreController.get().supportFragmentManager.fragments.first()
        assertThat(restoredFragment).isInstanceOf(ComposeFragment::class.java)

        val pickedFileUri = File(tmpFolder.root, "testFile.txt")
        FileUtils.writeStringToFile(pickedFileUri, "testData")
        Tools.registerFile(pickedFileUri)

        val pickedPhotoIntent = Intent()
        val pickedPhotoUri = Uri.fromFile(pickedFileUri)
        pickedPhotoIntent.data = pickedPhotoUri
        restoredFragment.onActivityResult(Requests.PICK_ATTACHMENT, Activity.RESULT_OK, pickedPhotoIntent)

        val workers = workerFactory.getAllStartedWorkers()
        assertThat(workers).haveExactly(
            1,
            allOf(
                action(CommandsServiceActions.UPLOAD_ATTACHMENT_ONLINE_ACTION),
                uid(user.uid),
                keyWithLongValue(DRAFT_ID_EXTRAS, draftId)
            )
        )
        pickedFileUri.delete()
    }

    @Test
    fun `fragment should not pick inner files`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            val fragment = get().fragmentManager!!.fragments.first()

            val innerFile = File(RuntimeEnvironment.application.filesDir, "testFile.txt")
            FileUtils.writeStringToFile(innerFile, "testData")
            val pickedIntent = Intent()
            pickedIntent.data = Uri.fromFile(innerFile)

            fragment.onActivityResult(Requests.PICK_ATTACHMENT, Activity.RESULT_OK, pickedIntent)

            val startedServices = workerFactory.getAllStartedWorkers()
            assertThat(startedServices).haveExactly(0, action(CommandsServiceActions.UPLOAD_ATTACHMENT_ONLINE_ACTION))

            innerFile.delete()
        }
    }

    @Test
    fun `fragment should pick outer files and not pick inner`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()

            val fragment = get().fragmentManager!!.fragments.first()

            val pickedIntent = Intent()

            val innerFile = File(RuntimeEnvironment.application.filesDir, "inner.txt")
            FileUtils.writeStringToFile(innerFile, "data")
            pickedIntent.clipData = ClipData(ClipDescription("", arrayOf()), ClipData.Item(Uri.fromFile(innerFile)))

            val outerFile1 = File.createTempFile("outer1", ".txt")
            val outerFile2 = File.createTempFile("outer2", ".txt")
            FileUtils.writeStringToFile(outerFile1, "data")
            FileUtils.writeStringToFile(outerFile2, "data")
            Tools.registerFile(outerFile1)
            Tools.registerFile(outerFile2)
            pickedIntent.clipData!!.addItem(ClipData.Item(Uri.fromFile(outerFile1)))
            pickedIntent.clipData!!.addItem(ClipData.Item(Uri.fromFile(outerFile2)))

            fragment.onActivityResult(Requests.PICK_ATTACHMENT, Activity.RESULT_OK, pickedIntent)

            val startedServices = workerFactory.getAllStartedWorkers()
            assertThat(startedServices).haveExactly(2, action(CommandsServiceActions.UPLOAD_ATTACHMENT_ONLINE_ACTION))

            innerFile.delete()
            outerFile1.delete()
            outerFile2.delete()
        }
    }

    @Test
    fun `fragment should save state if data was not loaded`() {
        val intent = createNewMessageIntent()

        val firstStartController =
            ComposeFragmentController(TestComposeFragment.create(user.uid, intent), intent)
        firstStartController.create().start().resume().visible()
        shadowOf(getMainLooper()).idle()

        val state1 = Bundle()
        firstStartController.activityController.saveInstanceState(state1)
        firstStartController.pause().stop().destroy()
        shadowOf(getMainLooper()).idle()

        val secondStartController = Robolectric.buildActivity(ComposeTestFragmentActivity::class.java, intent)
        secondStartController.create(state1).start().resume()
        shadowOf(getMainLooper()).idle()
        // do not trigger scheduler -> loading was not complete

        val state2 = Bundle()
        secondStartController.pause().saveInstanceState(state2).stop().destroy()
        shadowOf(getMainLooper()).idle()

        val thirdStartController = Robolectric.buildActivity(ComposeTestFragmentActivity::class.java, intent)
        thirdStartController.create(state2).start().resume()
        shadowOf(getMainLooper()).idle()

        val restoredFragment = thirdStartController.get().supportFragmentManager.fragments.first()
        assertThat(restoredFragment).isInstanceOf(ComposeFragment::class.java)
        assertThat((restoredFragment as ComposeFragment).senderName).isNotNull()
    }

    @Test
    fun `compose input body from external intent with signature`() {
        account.getSettings().signature = "signature"
        user.loadSettings()

        with(createController(createShareTextIntent("some_content"))) {
            prepareFragment()

            assertThat(body).has(content("some_content$BR${BR}signature"))
        }
    }

    @Test
    fun `compose put body from external intent without signature`() {
        account.getSettings().signature = ""
        user.loadSettings()

        with(createController(createShareTextIntent("some_content"))) {
            prepareFragment()

            assertThat(body).has(content("some_content"))
        }
    }

    @Test
    fun `compose put body from mail to intent with signature`() {
        account.getSettings().signature = "some_signature"
        user.loadSettings()

        with(createController(createMailToWithBodyIntent("test@ya.ru", "some_content"))) {
            prepareFragment()

            assertThat(to).hasYable("test@ya.ru")
            assertThat(body).has(content("some_content$BR${BR}some_signature"))
        }
    }

    @Test
    fun `compose put body from mail to intent without signature`() {
        account.getSettings().signature = ""
        user.loadSettings()

        with(createController(createMailToWithBodyIntent("test@ya.ru", "some_content"))) {
            prepareFragment()

            assertThat(to).hasYable("test@ya.ru")
            assertThat(body).has(content("some_content"))
        }
    }

    @Test
    fun `compose put body from mail to with extra body intent with signature`() {
        account.getSettings().signature = "some_signature"
        user.loadSettings()

        with(createController(createMailToWithExtraTextIntent("some_content"))) {
            prepareFragment()

            assertThat(body).has(content("some_content$BR${BR}some_signature"))
        }
    }

    @Test
    fun `compose put body from mail to with extra body intent without signature`() {
        account.getSettings().signature = ""
        user.loadSettings()

        with(createController(createMailToWithExtraTextIntent("some_content"))) {
            prepareFragment()

            assertThat(body).has(content("some_content"))
        }
    }

    @Test
    fun `restores meta expanded states after rotation`() {
        val intent = createNewMessageIntent()

        listOf(EXPANDED, FULLY_EXPANDED, COLLAPSED).forEach { targetState ->
            val activityBeforeRestoreController =
                ComposeFragmentController(TestComposeFragment.create(user.uid, intent), intent)
            activityBeforeRestoreController.create().start().resume().visible()

            activityBeforeRestoreController.get().metaController.setState(targetState)

            val bundle = Bundle()
            activityBeforeRestoreController.activityController.saveInstanceState(bundle)
            activityBeforeRestoreController.pause().stop().destroy()

            val activityAfterRestoreController = Robolectric.buildActivity(ComposeTestFragmentActivity::class.java, intent)
            activityAfterRestoreController.create(bundle).start().resume()

            val restoredFragment = activityAfterRestoreController.get().supportFragmentManager.fragments.first() as ComposeFragment

            assertThat(restoredFragment.metaController.getState()).isEqualTo(targetState)
        }

//        listOf(EXPANDED, FULLY_EXPANDED, COLLAPSED).forEach { targetState ->
//            val activityBeforeRestoreController =
//                ComposeFragmentController(TestComposeFragment.create(user.uid, intent), intent)
//            activityBeforeRestoreController.create().start().resume().visible()
//
//            activityBeforeRestoreController.get().metaController.state = targetState
//            activityBeforeRestoreController.get().bcc = RecipientsViewHolder().apply {
//                reflow = mock {
//                    on { getText(com.nhaarman.mockito_kotlin.any()) } doReturn ("test")
//                }
//            }
//
//            val bundle = Bundle()
//            activityBeforeRestoreController.activityController.saveInstanceState(bundle)
//            activityBeforeRestoreController.pause().stop().destroy()
//
//            val activityAfterRestoreController = Robolectric.buildActivity(ComposeTestFragmentActivity::class.java, intent)
//            activityAfterRestoreController.create(bundle).start().resume()
//
//            val restoredFragment = activityAfterRestoreController.get().supportFragmentManager.fragments.first() as ComposeFragment
//
//            assertThat(restoredFragment.metaController.state).isEqualTo(targetState)
//        }
    }

    @Test
    fun `open compose from notification should report to metrica`() {
        val message = createMessage(inbox().folder.name)
        val mid = message.mid.toLong()
        val intent = createReplyIntent(mid)
        intent.putExtra(NotificationsConstants.STARTED_FROM_NOTIFICATION, true)

        metrica.clearEvents()
        createController(intent).create()
        shadowOf(getMainLooper()).idle()

        assertThat(metrica).has(
            MetricaConditions.notificationEvent(
                MetricaConstns.Notification.COMPOSE_OPENED_FROM_NOTIFICATION,
                user.uid,
                mid
            )
        )
    }

    @Test
    fun `open draft with invalid sender mail should fallback to default sender mail`() {
        val defaultSender = Rfc822Token("name", "Name@ya.ru", "comment")
        val emails = setOf(defaultSender.address!!, "name@ya.ru")
        val message = createDraft(2, 0, 0)
        val intent = createEditIntent(user.getLocalMessage(message).localMid)

        with(createController(intent)) {
            prepareFragment()
            get().setDefaultSender(defaultSender)
            get().setUserEmails(emails)
            get().setSenderEmail("invalid@ya.ru")

            assertThat(sender).isEqualTo(defaultSender.address)
        }
    }

    @Test
    fun `open draft with capitalized invalid sender mail should fallback to lowercase mail`() {
        val defaultSender = Rfc822Token("name", "Name@ya.ru", "comment")
        val emails = setOf(defaultSender.address!!, "name@ya.ru", "name2@ya.ru")
        val message = createDraft(2, 0, 0)
        val intent = createEditIntent(user.getLocalMessage(message).localMid)

        with(createController(intent)) {
            prepareFragment()
            get().setDefaultSender(defaultSender)
            get().setUserEmails(emails)
            get().setSenderEmail("Name2@ya.ru")

            assertThat(sender).isEqualTo("name2@ya.ru")
        }
    }

    @Test
    fun `open draft with lowercase invalid sender mail should fallback to capitalized mail`() {
        val defaultSender = Rfc822Token("name", "Name@ya.ru", "comment")
        val emails = setOf(defaultSender.address!!, "name@ya.ru", "Name2@ya.ru")
        val message = createDraft(2, 0, 0)
        val intent = createEditIntent(user.getLocalMessage(message).localMid)

        with(createController(intent)) {
            prepareFragment()
            get().setDefaultSender(defaultSender)
            get().setUserEmails(emails)
            get().setSenderEmail("name2@ya.ru")

            assertThat(sender).isEqualTo("Name2@ya.ru")
        }
    }

    @Test
    fun `open draft with valid sender mail should show this mail`() {
        val defaultSender = Rfc822Token("name", "Name@ya.ru", "comment")
        val emails = setOf(defaultSender.address!!, "name@ya.ru", "name2@ya.ru")
        val message = createDraft(2, 0, 0)
        val intent = createEditIntent(user.getLocalMessage(message).localMid)

        with(createController(intent)) {
            prepareFragment()
            get().setDefaultSender(defaultSender)
            get().setUserEmails(emails)
            get().setSenderEmail("name2@ya.ru")

            assertThat(sender).isEqualTo("name2@ya.ru")
        }
    }

    @Test
    fun `open draft with null sender mail should fallback to default sender mail`() {
        val defaultSender = Rfc822Token("name", "Name@ya.ru", "comment")
        val emails = setOf(defaultSender.address!!, "name@ya.ru", "name2@ya.ru")
        val message = createDraft(2, 0, 0)
        val intent = createEditIntent(user.getLocalMessage(message).localMid)

        with(createController(intent)) {
            prepareFragment()
            get().setDefaultSender(defaultSender)
            get().setUserEmails(emails)
            get().setSenderEmail(null)

            assertThat(sender).isEqualTo(defaultSender.address)
        }
    }

    @Test
    fun `showKeyboardForToCcOrBcc should focus to field if nothing focused`() {
        with(createController(createNewMessageIntent())) {
            create().start().resume().pause()
            get().showKeyboardForToCcOrBcc()
            assertThat(toEditText).conforms(focused())
        }
    }

    @Test
    fun `should store if share even on no edit`() {
        account.getSettings().signature = ""
        user.loadSettings()
        with(createController(createShareTextIntent("some content"))) {
            prepareFragment()
            pause()
            assertThat(saveIntent).isNotNull
        }
    }

    @Test
    fun `should make disk item enabled on network available`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            get().setDiskSupported(true)

            get().composeView.subjectBinding.attachFile.performClick()
            RobolectricTools.callOnPreDraw(get().composeView.root)
            get().onNetworkRestored()
            shadowOf(getMainLooper()).idle()

            val item = get().composeView.attachPanel!!.controller.getMenuItem(R.id.menu_attach_disk)!!
            assertThat(item.isEnabled).isTrue()
        }
    }

    @Test
    fun `should make disk item disabled on network unavailable`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            shadowOf(getMainLooper()).idle()
            get().setDiskSupported(true)

            get().composeView.subjectBinding.attachFile.performClick()
            RobolectricTools.callOnPreDraw(get().composeView.root)
            shadowOf(getMainLooper()).idle()
            get().onNetworkError()
            shadowOf(getMainLooper()).idle()

            val item = get().composeView.attachPanel!!.controller.getMenuItem(R.id.menu_attach_disk)!!
            assertThat(item.isEnabled).isFalse()
        }
    }

    @Test
    fun `system can not resolve action_pick intent and activity starts with action_get_content intent when attach image`() {
        checkOpenActivityForAttachImage(Intent.ACTION_GET_CONTENT)
    }

    @Test
    @Config(shadows = [ShadowIntent::class])
    fun `system can resolve action_pick intent and activity starts with it when attach image`() {
        checkOpenActivityForAttachImage(Intent.ACTION_PICK)
    }

    private fun checkOpenActivityForAttachImage(intentAction: String) {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            get().composeView.subjectBinding.attachFile.performClick()
            val attachAlbumMenuItem = RoboMenuItem(R.id.menu_attach_album)

            get().composeView.attachPanel!!.onAttachListener!!.onMenuClicked(
                attachAlbumMenuItem
            )

            val startedIntent = shadowOf(get().activity).nextStartedActivity
            assertEquals(intentAction, startedIntent.action)
        }
    }

    @Test
    fun `fragment should report to metrica on change to recipients`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            to.createYable("test", false)
            metrica.assertLastEvent(ComposeEventMetrics.CHANGE_RECIPIENTS)
        }
    }

    @Test
    fun `fragment should report to metrica on change to recipient after undo`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid, fromUndo = true))) {
            prepareFragment()
            to.createYable("test", false)
            metrica.assertLastEvent(ComposeEventMetrics.UNDO_CHANGE_RECIPIENTS)
        }
    }

    @Test
    fun `fragment should report to metrica on change cc recipient`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            cc.createYable("test", false)
            metrica.assertLastEvent(ComposeEventMetrics.CHANGE_RECIPIENTS)
        }
    }

    @Test
    fun `fragment should report to metrica on change cc recipient after undo`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid, fromUndo = true))) {
            prepareFragment()
            cc.createYable("test", false)
            metrica.assertLastEvent(ComposeEventMetrics.UNDO_CHANGE_RECIPIENTS)
        }
    }

    @Test
    fun `fragment should report to metrica on change bcc recipient`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            bcc.createYable("test", false)
            metrica.assertLastEvent(ComposeEventMetrics.CHANGE_RECIPIENTS)
        }
    }

    @Test
    fun `fragment should report to metrica on change bcc recipient after undo`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid, fromUndo = true))) {
            prepareFragment()
            bcc.createYable("test", false)
            metrica.assertLastEvent(ComposeEventMetrics.UNDO_CHANGE_RECIPIENTS)
        }
    }

    @Test
    fun `fragment should report to metrica on change subject`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            subject.requestFocus()
            subject.append("text")
            subject.clearFocus()
            metrica.assertLastEvent(ComposeEventMetrics.CHANGE_SUBJECT)
        }
    }

    @Test
    fun `fragment should report to metrica on change subject after undo`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid, fromUndo = true))) {
            prepareFragment()
            subject.requestFocus()
            subject.append("text")
            subject.clearFocus()
            metrica.assertLastEvent(ComposeEventMetrics.UNDO_CHANGE_SUBJECT)
        }
    }

    @Test
    fun `fragment should not report to metrica if subject isn't changed`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            subject.requestFocus()
            subject.append("text")
            subject.clearFocus()
            metrica.clearEvents()
            subject.requestFocus()
            subject.clearFocus()
            metrica.assertNoEvents()
        }
    }

    @Test
    fun `fragment should report to metrica on change body`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            val shadowContentView = shadowOf(contentView)
            shadowContentView.setViewFocus(true)
            appendToBody("text")
            shadowContentView.setViewFocus(false)
            metrica.assertLastEvent(ComposeEventMetrics.CHANGE_BODY)
        }
    }

    @Test
    fun `fragment should report to metrica on change body after undo`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid, fromUndo = true))) {
            prepareFragment()
            val shadowContentView = shadowOf(contentView)
            shadowContentView.setViewFocus(true)
            appendToBody("text")
            shadowContentView.setViewFocus(false)
            metrica.assertLastEvent(ComposeEventMetrics.UNDO_CHANGE_BODY)
        }
    }

    @Test
    fun `fragment should not report to metrica if body isn't changed`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            appendToBody("text")
            metrica.clearEvents()
            val shadowContentView = shadowOf(contentView)
            shadowContentView.setViewFocus(true)
            shadowContentView.setViewFocus(false)
            metrica.assertNoEvents()
        }
    }

    @Test
    fun `uploadAttachment should report to metrica`() {
        val attachFile = Tools.createRandomFile(1)!!
        Tools.registerFile(attachFile)
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            get().uploadAttachments(setOf(OrderedUriAttach(0, Uri.fromFile(attachFile))), CompressType.COMPRESS_NONE)
            metrica.assertLastEvent(ComposeEventMetrics.CHANGE_ATTACHMENTS)
        }
    }

    @Test
    fun `uploadAttachment should report to metrica from undo`() {
        val attachFile = Tools.createRandomFile(1)!!
        Tools.registerFile(attachFile)
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid, fromUndo = true))) {
            prepareFragment()
            get().uploadAttachments(setOf(OrderedUriAttach(0, Uri.fromFile(attachFile))), CompressType.COMPRESS_NONE)
            metrica.assertLastEvent(ComposeEventMetrics.UNDO_CHANGE_ATTACHMENTS)
        }
    }

    @Test
    fun `showAttachments should report to metrica`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            get().showAttachments(listOf(EntitiesTestFactory.buildAttachment()))
            get().view!!.findViewById<View>(R.id.delete_attach).performClick()
            metrica.assertEvent(ComposeEventMetrics.CHANGE_ATTACHMENTS)
        }
    }

    @Test
    fun `showAttachments should report to metrica from undo`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid, fromUndo = true))) {
            prepareFragment()
            get().showAttachments(listOf(EntitiesTestFactory.buildAttachment()))
            get().view!!.findViewById<View>(R.id.delete_attach).performClick()
            metrica.assertEvent(ComposeEventMetrics.UNDO_CHANGE_ATTACHMENTS)
        }
    }

    @Test
    fun `showAttachments should not show disk folder size`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid, fromUndo = true))) {
            prepareFragment()
            get().showAttachments(
                listOf(
                    EntitiesTestFactory.buildAttachment().copy(
                        display_name = "Folder",
                        is_disk = true,
                        is_folder = true,
                        size = 0L,
                    )
                )
            )
            val attachSizeText = get().view!!.findViewById<TextView>(R.id.attachment_size).text.toString()
            assertThat(attachSizeText).isEqualTo("Folder will appear as a link to Yandex Disk.")
        }
    }

    @Test
    fun `showAttachments should show disk file size`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid, fromUndo = true))) {
            prepareFragment()
            val fileAttachment = EntitiesTestFactory.buildAttachment().copy(
                display_name = "Image.jpg",
                is_folder = false,
                is_disk = true,
                mime_type = "image/jpg",
                size = 1000L,
            )
            get().showAttachments(listOf(fileAttachment))
            val attachSizeText = get().view!!.findViewById<TextView>(R.id.attachment_size).text.toString()
            assertThat(attachSizeText).isEqualTo("0.98 KB. File will appear as a link to Yandex Disk.")
        }
    }

    @Test
    fun `onActivityResult should report to metrica for disk attaches`() {
        with(createController(createNewMessageIntent())) {
            prepareFragment()
            pause().stop()
            get().onActivityResult(Requests.DISK_ATTACH, Activity.RESULT_OK, null)
            metrica.assertLastEvent(ComposeEventMetrics.CHANGE_ATTACHMENTS)
        }
    }

    @Test
    fun `onActivityResult should report to metrica for disk attaches from undo`() {
        val message = createDraft(2, 0, 0)
        with(createController(createEditIntent(user.getLocalMessage(message).localMid, fromUndo = true))) {
            prepareFragment()
            pause().stop()
            get().onActivityResult(Requests.DISK_ATTACH, Activity.RESULT_OK, null)
            metrica.assertLastEvent(ComposeEventMetrics.UNDO_CHANGE_ATTACHMENTS)
        }
    }

    private val sendIntent: WorkerInfo?
        get() = getWorkWithAction(CommandsServiceActions.SEND_MAIL_ACTION)

    private val saveIntent: WorkerInfo?
        get() = getWorkWithAction(CommandsServiceActions.SAVE_DRAFT_ACTION)

    private fun getWorkWithAction(action: String): WorkerInfo? {
        return workerFactory.getAllStartedWorkers().firstOrNull { worker -> action == worker.getInputString(ACTION_EXTRA) }
    }

    @SuppressWarnings("unused")
    @Implements(Intent::class)
    class ShadowIntent {
        fun resolveActivity(pm: PackageManager): ComponentName? {
            return ComponentName("com.google", "GooglePhotoActivity")
        }
    }

    private abstract inner class TestRepliesHelper {

        lateinit var from: String

        lateinit var defaultEmail: String

        lateinit var to: List<String>

        lateinit var cc: List<String>

        lateinit var bcc: List<String>

        lateinit var data: DraftData

        fun prepare(all: Boolean, fromSelf: Boolean): ComposeFragmentController {
            defaultEmail = user.defaultEmail
            from = if (fromSelf) defaultEmail else "testFrom@ya.ru"
            to = createMessagesRange("to", 2)
            cc = createMessagesRange("cc", 2)
            bcc = createMessagesRange("bcc", 2)
            val toWithMe = to + defaultEmail
            val message = createMessage(
                INBOX_FOLDER,
                from,
                toWithMe,
                cc,
                bcc
            )
            val localMid = user.getLocalMessage(message).localMid
            val intent = if (all) createReplyAllIntent(localMid) else createReplyIntent(localMid)
            return createController(intent).apply {
                prepareFragment()
                shadowOf(getMainLooper()).idle()
            }
        }

        fun extractDraftData(controller: ComposeFragmentController) {
            controller.clickSend()
            shadowOf(getMainLooper()).idle()
            val worker = workerFactory.getAllStartedWorkers()
                .first { service ->
                    service.getInputString(ACTION_EXTRA) in listOf(
                        CommandsServiceActions.SAVE_DRAFT_ACTION,
                        CommandsServiceActions.SEND_MAIL_ACTION
                    )
                }
            data = DraftData.deserializeFromData(worker.workerParameters.inputData)!!
        }

        internal abstract fun runAsserts()
    }

    class ComposeFragmentController(fragment: TestComposeFragment, intent: Intent?) :
        FragmentController<ComposeFragment>(fragment, ComposeTestFragmentActivity::class.java, intent) {

        val body: String?
            get() = get().composeView.getBody()

        fun appendToBody(appendingText: String) {
            val contentView = get().composeView.content.contentView
            if (contentView is EditText) {
                contentView.append(appendingText)
            } else {
                val shadowWebView = shadowOf(contentView as WebView)
                val jsBridge = shadowWebView.getJavascriptInterface(JS_INTERFACE_NAME) as WebViewComposeContent.ChangedJsBridge
                jsBridge.contentChanged(shadowWebView.lastLoadDataWithBaseURL.data + appendingText)
            }
        }

        val arrow: View
            get() = get().composeView.to.expandArrow()

        val recipientsBlock: View
            get() = get().composeView.viewBinding.recipientsFrame

        val counter: TextView
            get() = get().composeView.to.counter()

        val senderSelection: Spinner
            get() = get().composeView.fromBinding.composeFromSpinner

        val attachmentsList: LinearLayout
            get() = get().composeView.viewBinding.attachmentList

        val sender: String
            get() = senderSelection.selectedItem as String

        val to: YableReflowView
            get() = get().composeView.to.reflow()

        val cc: YableReflowView
            get() = get().composeView.cc.reflow()

        val bcc: YableReflowView
            get() = get().composeView.bcc.reflow()

        val toEditText: YableEditTextView
            get() = get().composeView.to.editText()

        val ccEditText: YableEditTextView
            get() = get().composeView.cc.editText()

        val bccEditText: YableEditTextView
            get() = get().composeView.bcc.editText()

        fun getField(field: Field): YableReflowView {
            return when (field) {
                ComposeFragmentTest.Field.TO -> to
                ComposeFragmentTest.Field.CC -> cc
                ComposeFragmentTest.Field.BCC -> bcc
                else -> throw UnexpectedCaseException(field)
            }
        }

        val subject: EditText
            get() = get().composeView.subjectBinding.subjectEdit

        val contentView: View
            get() = get().composeView.contentBinding.contentEdit

        fun clickSend() = get().onOptionsItemSelected(RoboMenuItem(R.id.menu_send))

        val draftId: Long
            get() = get().draftId()

        fun assertCcBccInvisible() {
            assertThat(cc.isShown).isFalse
            assertThat(bcc.isShown).isFalse
        }

        fun assertCcBccVisible() {
            assertThat(cc.isShown).isTrue
            assertThat(bcc.isShown).isTrue
        }

        fun assertNoCounter() {
            assertThat(counter.isShown).isFalse
        }

        fun assertShowsCounter(count: Int) {
            assertThat(counter.text).isEqualTo("+$count")
        }

        /**
         * Expands/collapses cc/bcc
         */
        fun toggleExpand() = arrow.performClick()

        fun prepareFragment() {
            create().start().resume().visible()
            prepareContent()
            shadowOf(getMainLooper()).idle()
        }

        fun prepareContent() {
            val contentView = get().composeView.content.contentView
            if (contentView is WebView) {
                val shadowWebView = shadowOf(contentView)
                val jsBridge = shadowWebView.getJavascriptInterface(JS_INTERFACE_NAME) as WebViewComposeContent.ChangedJsBridge
                jsBridge.loaded(shadowWebView.lastLoadDataWithBaseURL.data)
            }
        }

        fun fillRecipients(reflow: YableReflowView, recipients: Int) {
            toggleExpand()
            for (i in 0 until recipients) {
                reflow.createYable("test$i@ya.ru", false)
            }
            toggleExpand()
        }

        fun testCreatesYablesHelper(reflow: YableReflowView) {
            val edit = reflow.yableTextView

            // initially, recipients are expanded
            edit.requestFocus()
            edit.append("test@ya.ru")
            subject.requestFocus() // we lose focus, so recipients collapse
            toggleExpand() // expand
            edit.requestFocus()
            edit.append("test2@ya.ru")
            subject.requestFocus() // we lose focus, recipients collapse

            assertThat(reflow.yablesCount()).isEqualTo(2)
        }
    }

    class ComposeTestFragmentActivity :
        TestFragmentActivity(),
        ComposeFragment.ComposeFragmentCallback,
        SpeechkitTermsDialogFragment.TermsConfirmationCallback {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(
                if (isDarkThemeEnabled)
                    R.style.YaTheme_Compose_Dark
                else
                    R.style.YaTheme_Compose_Light
            )
        }

        override fun onMessageSent(immediate: Boolean, isDelayedSend: Boolean) {}
        override fun onDraftSaved() {}

        override fun onShowGallery(
            composeAttachMode: ComposeAttachMode,
            thumbViews: Map<Uri?, View?>,
            imageUris: List<Uri>,
            checkedUris: Set<Uri?>,
            position: Int
        ) {
        }

        override fun onShowScanResultGallery(imageUris: List<Uri>) {}

        override val isReloginRequested: Boolean
            get() = false

        override fun onTermsAccepted() {}
        override fun onTermsDeclined() {}
    }

    enum class Field {
        TO,
        CC,
        BCC
    }

    open class TestComposeFragment : ComposeFragment() {

        override fun initToolbar() {
            // workaround for weird Robolectric exceptions while testing ComposeFragment
        }

        companion object {

            fun create(uid: Long, intent: Intent): TestComposeFragment {
                val accountComponent = getAccountComponent(RuntimeEnvironment.application, uid)
                val draftsModel = accountComponent.draftsModel()

                val isEdit = EDIT_DRAFT == intent.action

                val did = if (isEdit) {
                    val midToEdit = intent.getLongExtra(MESSAGE_ID_EXTRAS, NO_MESSAGE_ID)
                    draftsModel.getOrCreateDidByMid(midToEdit).blockingGet()
                } else {
                    draftsModel.createNewDraftAndReturnDid().blockingGet()
                }

                val args = Bundle().apply {
                    putLong(UID_KEY, uid)
                    putLong(DRAFT_ID_EXTRAS, did)
                }
                return TestComposeFragment().apply { arguments = args }
            }
        }
    }

    companion object {

        val DRAFTS_FOLDER = "Drafts"

        private val INBOX_FOLDER = "Inbox"

        private val DEFAULT_FROM = "from@ya.ru"

        private fun createMessagesRange(prefix: String, count: Int): List<String> {
            return (0 until count).map { i -> "$prefix$i@ya.ru" }
        }

        private fun containedIn(string: String?): Condition<String?> {
            return object : Condition<String?>() {
                override fun matches(value: String?): Boolean {
                    return if (value != null && string != null) {
                        string.contains(value)
                    } else {
                        false
                    }
                }
            }
        }

        private fun content(expected: String): Condition<String> {
            return object : Condition<String>() {
                override fun matches(actual: String): Boolean {
                    val expectedContent = Jsoup.parse(expected).body().html()
                    val document = Jsoup.parse(actual)
                    val actualContent = document.getElementById("content$SUFFIX")
                    return expectedContent == actualContent.html()
                }
            }
        }
    }
}
