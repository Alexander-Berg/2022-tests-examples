package com.yandex.mail.compose

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.yandex.mail.R
import com.yandex.mail.compose.ComposeFragmentTest.ComposeFragmentController
import com.yandex.mail.metrica.MetricaConstns
import com.yandex.mail.runners.IntegrationTestRunner
import com.yandex.mail.tools.Accounts
import com.yandex.mail.tools.TestFragmentActivity
import com.yandex.mail.ui.constants.ActivityConstants.REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT
import com.yandex.mail.util.BaseIntegrationTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@Config(sdk = [23])
@RunWith(IntegrationTestRunner::class)
open class ComposeFragmentPermissionsTest : BaseIntegrationTest() {

    @Rule
    @JvmField
    var tmpFolder = TemporaryFolder()

    lateinit var shadowApplication: ShadowApplication

    lateinit var composeFragmentTest: ComposeFragmentTest

    lateinit var controller: ComposeFragmentController

    lateinit var uri: Uri

    val activity: TestFragmentActivity
        get() = controller.activityController.get() as TestFragmentActivity

    @Before
    fun `before each test`() {
        shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application)

        composeFragmentTest = ComposeFragmentTest().apply {
            init(Accounts.teamLoginData)
        }

        uri = Uri.fromFile(tmpFolder.root)
    }

    // region request just contacts

    @Test
    fun `should not request storage without attachments`() {
        shadowApplication.denyPermissions(READ_CONTACTS, WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithText())
        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT, arrayOf(READ_CONTACTS), intArrayOf(PERMISSION_GRANTED))
            verify(this, never()).onPickAttachmentsFromUri(any())
            verify(this).showKeyboardForToCcOrBcc()
            verify(this, never()).showNoPermissionSnackbar(anyInt(), anyBoolean())
        }
    }

    @Test
    fun `on grant contacts if storage already granted should call showKeyboardForToCcOrBcc`() {
        shadowApplication.denyPermissions(READ_CONTACTS)
        shadowApplication.grantPermissions(WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())
        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT, arrayOf(READ_CONTACTS), intArrayOf(PERMISSION_GRANTED))
            verify(this).showKeyboardForToCcOrBcc() // yes, it is success callback =/
            verify(this, never()).showNoPermissionSnackbar(anyInt(), anyBoolean())
        }
    }

    @Test
    fun `on deny now contacts if storage already granted shows toast without button`() {
        shadowApplication.denyPermissions(READ_CONTACTS)
        shadowApplication.grantPermissions(WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())
        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT, arrayOf(READ_CONTACTS), intArrayOf(PERMISSION_DENIED))
            verify(this, never()).requestContactPermission()
            verify(this).showNoPermissionSnackbar(R.string.permission_contacts_from_phone_denied, false)
        }
    }

    @Test
    fun `on never ask again contacts if storage already granted shows toast with button`() {
        shadowApplication.denyPermissions(READ_CONTACTS)
        shadowApplication.grantPermissions(WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())

        activity.neverAskAgainPermissions.add(READ_CONTACTS)

        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT, arrayOf(READ_CONTACTS), intArrayOf(PERMISSION_DENIED))
            verify(this, never()).requestContactPermission()
            verify(this).showNoPermissionSnackbar(R.string.permission_contacts_from_phone_denied, true)
        }
    }

    // endregion

    // region request just storage

    @Test
    fun `should not request contacts on reply without focus`() {
        shadowApplication.denyPermissions(READ_CONTACTS, WRITE_EXTERNAL_STORAGE)

        createController(createReplyIntent())
        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT, arrayOf(WRITE_EXTERNAL_STORAGE), intArrayOf(PERMISSION_GRANTED))
            verify(this).onPickAttachmentsFromUri(emptySet())
            verify(this, never()).requestContactPermission()
            verify(this, never()).showNoPermissionSnackbar(anyInt(), anyBoolean())
        }
    }

    @Test
    fun `on grant storage if contacts already granted should call onPickAttachmentsFromUri without any toast`() {
        shadowApplication.denyPermissions(WRITE_EXTERNAL_STORAGE)
        shadowApplication.grantPermissions(READ_CONTACTS)

        createController(createIntentWithAttachments())
        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT, arrayOf(WRITE_EXTERNAL_STORAGE), intArrayOf(PERMISSION_GRANTED))
            verify(this).onPickAttachmentsFromUri(setOf(uri))
            verify(this, never()).requestContactPermission()
            verify(this, never()).showNoPermissionSnackbar(anyInt(), anyBoolean())
        }
    }

    @Test
    fun `on deny now storage if contacts already granted should show toast without button`() {
        shadowApplication.denyPermissions(WRITE_EXTERNAL_STORAGE)
        shadowApplication.grantPermissions(READ_CONTACTS)

        createController(createIntentWithAttachments())
        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT, arrayOf(WRITE_EXTERNAL_STORAGE), intArrayOf(PERMISSION_DENIED))
            verify(this).showNoPermissionSnackbar(R.string.permission_storage_access_denied, false)
            verify(this, never()).onPickAttachmentsFromUri(any())
            verify(this, never()).requestContactPermission()
        }
    }

    @Test
    fun `on never ask again storage if contacts already granted should show toast with button`() {
        shadowApplication.denyPermissions(WRITE_EXTERNAL_STORAGE)
        shadowApplication.grantPermissions(READ_CONTACTS)

        createController(createIntentWithAttachments())

        activity.neverAskAgainPermissions.add(WRITE_EXTERNAL_STORAGE)

        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT, arrayOf(WRITE_EXTERNAL_STORAGE), intArrayOf(PERMISSION_DENIED))
            verify(this).showNoPermissionSnackbar(R.string.permission_storage_access_denied, true)
            verify(this, never()).onPickAttachmentsFromUri(any())
            verify(this, never()).requestContactPermission()
        }
    }

    // endregion

    // region partial grant

    @Test
    fun `on grant contacts but deny now storage should call showKeyboardForToCcOrBcc and show toast without button`() {
        shadowApplication.denyPermissions(READ_CONTACTS, WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())
        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(
                REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT,
                arrayOf(READ_CONTACTS, WRITE_EXTERNAL_STORAGE),
                intArrayOf(PERMISSION_GRANTED, PERMISSION_DENIED)
            )
            verify(this).showKeyboardForToCcOrBcc() // success
            verify(this).showNoPermissionSnackbar(R.string.permission_storage_access_denied, false)
            verify(this, never()).onPickAttachmentsFromUri(any())
        }
    }

    @Test
    fun `on grant contacts but never ask again storage should call showKeyboardForToCcOrBcc and shows toast with button`() {
        shadowApplication.denyPermissions(READ_CONTACTS, WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())

        activity.neverAskAgainPermissions.add(WRITE_EXTERNAL_STORAGE)

        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(
                REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT,
                arrayOf(READ_CONTACTS, WRITE_EXTERNAL_STORAGE),
                intArrayOf(PERMISSION_GRANTED, PERMISSION_DENIED)
            )
            verify(this).showKeyboardForToCcOrBcc() // success
            verify(this).showNoPermissionSnackbar(R.string.permission_storage_access_denied, true)
            verify(this, never()).onPickAttachmentsFromUri(any())
        }
    }

    @Test
    fun `on grant storage but deny now contacts should call onPickAttachmentsFromUri and show toast without button`() {
        shadowApplication.denyPermissions(READ_CONTACTS, WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())
        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(
                REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT,
                arrayOf(READ_CONTACTS, WRITE_EXTERNAL_STORAGE),
                intArrayOf(PERMISSION_DENIED, PERMISSION_GRANTED)
            )
            verify(this).onPickAttachmentsFromUri(setOf(uri))
            verify(this).showNoPermissionSnackbar(R.string.permission_contacts_from_phone_denied, false)
            verify(this, never()).requestContactPermission()
        }
    }

    @Test
    fun `on grant storage but never ask again contacts should call onPickAttachmentsFromUri and show toast with button`() {
        shadowApplication.denyPermissions(READ_CONTACTS, WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())

        activity.neverAskAgainPermissions.add(READ_CONTACTS)

        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(
                REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT,
                arrayOf(READ_CONTACTS, WRITE_EXTERNAL_STORAGE),
                intArrayOf(PERMISSION_DENIED, PERMISSION_GRANTED)
            )
            verify(this).onPickAttachmentsFromUri(setOf(uri))
            verify(this).showNoPermissionSnackbar(R.string.permission_contacts_from_phone_denied, true)
            verify(this, never()).requestContactPermission()
        }
    }

    // endregion

    // region denied both

    @Test
    fun `on never ask again for both should show toast with button`() {
        shadowApplication.denyPermissions(READ_CONTACTS, WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())

        activity.neverAskAgainPermissions.apply {
            add(READ_CONTACTS)
            add(WRITE_EXTERNAL_STORAGE)
        }

        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(
                REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT,
                arrayOf(READ_CONTACTS, WRITE_EXTERNAL_STORAGE),
                intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED)
            )
            verify(this).showNoPermissionSnackbar(R.string.permission_contacts_and_external_storage_denied, true)
        }
    }

    @Test
    fun `on never ask contacts and deny now storage should show toast with button`() {
        shadowApplication.denyPermissions(READ_CONTACTS, WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())

        activity.neverAskAgainPermissions.add(READ_CONTACTS)

        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(
                REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT,
                arrayOf(READ_CONTACTS, WRITE_EXTERNAL_STORAGE),
                intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED)
            )
            verify(this).showNoPermissionSnackbar(R.string.permission_contacts_and_external_storage_denied, true)
        }
    }

    @Test
    fun `on deny now contacts and never ask storage should show toast with button`() {
        shadowApplication.denyPermissions(READ_CONTACTS, WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())

        activity.neverAskAgainPermissions.add(WRITE_EXTERNAL_STORAGE)

        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(
                REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT,
                arrayOf(READ_CONTACTS, WRITE_EXTERNAL_STORAGE),
                intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED)
            )
            verify(this).showNoPermissionSnackbar(R.string.permission_contacts_and_external_storage_denied, true)
        }
    }

    @Test
    fun `on deny now both should show toast without button`() {
        shadowApplication.denyPermissions(READ_CONTACTS, WRITE_EXTERNAL_STORAGE)

        createController(createIntentWithAttachments())
        controller.prepareFragment()

        with(fragmentSpy()) {
            onRequestPermissionsResult(
                REQUEST_PERMISSIONS_MANUALLY_FOR_COMPOSE_FRAGMENT,
                arrayOf(READ_CONTACTS, WRITE_EXTERNAL_STORAGE),
                intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED)
            )
            verify(this).showNoPermissionSnackbar(R.string.permission_contacts_and_external_storage_denied, false)
        }
    }

    @Test
    fun `on grant record audio and terms should start recognizer`() {
        shadowApplication.grantPermissions(RECORD_AUDIO)
        composeFragmentTest.generalSettings.edit().setSpeechkitTermsAccepted(true).apply()
        createController(createIntentWithRecognizer())

        with(fragmentSpy()) {
            controller.prepareFragment()
            composeFragmentTest.metrica.assertEvent(MetricaConstns.SpeechkitEvents.RECOGNIZER_START, expectedCount = 1)
        }
    }

    @Test
    fun `on grant record audio and deny terms should not start recognizer`() {
        shadowApplication.grantPermissions(RECORD_AUDIO)
        composeFragmentTest.generalSettings.edit().setSpeechkitTermsAccepted(false).apply()

        createController(createIntentWithRecognizer())

        with(fragmentSpy()) {
            controller.prepareFragment()
            composeFragmentTest.metrica.assertNoEvent(MetricaConstns.SpeechkitEvents.RECOGNIZER_START)
        }
    }

    @Test
    fun `on deny record audio and grant terms should not start recognizer`() {
        shadowApplication.denyPermissions(RECORD_AUDIO)
        composeFragmentTest.generalSettings.edit().setSpeechkitTermsAccepted(true).apply()

        createController(createIntentWithRecognizer())

        with(fragmentSpy()) {
            controller.prepareFragment()
            composeFragmentTest.metrica.assertNoEvent(MetricaConstns.SpeechkitEvents.RECOGNIZER_START)
        }
    }

    @Test
    fun `on deny record audio and deny terms should not start recognizer`() {
        shadowApplication.denyPermissions(RECORD_AUDIO)
        composeFragmentTest.generalSettings.edit().setSpeechkitTermsAccepted(false).apply()

        createController(createIntentWithRecognizer())
        controller.prepareFragment()

        with(fragmentSpy()) {
            initRecognizer()
            verify(this, never()).startRecognizer()
        }
    }

    // endregion

    fun createIntentWithText() = composeFragmentTest.createShareTextIntent("some text")

    fun createIntentWithAttachments() = Intent(Intent.ACTION_SENDTO).apply {
        putExtra(Intent.EXTRA_TEXT, "some text")
        putExtra(Intent.EXTRA_STREAM, uri)
    }

    fun createIntentWithRecognizer() = composeFragmentTest.createDraftWithRecognizer()

    fun createReplyIntent(): Intent {
        val message = composeFragmentTest.createMessage(ComposeFragmentTest.DRAFTS_FOLDER)
        return composeFragmentTest.createReplyIntent(
            composeFragmentTest.user.getLocalMessage(message).localMid
        )
    }

    fun createController(intent: Intent) {
        controller = composeFragmentTest.createController(intent)
    }

    fun fragmentSpy(): ComposeFragment = spy(controller.get())
}
