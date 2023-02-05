package ru.yandex.autotests.mobile.disk.android.notes

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Acceptance
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.notes.CreateNote
import ru.yandex.autotests.mobile.disk.android.rules.annotations.notes.DeleteAllNotes
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Images
import java.util.concurrent.TimeUnit

@Feature("NoteItems")
@UserTags("notes")
@AuthorizationTest
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class NotesTest : NotesTestBase() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("2543")
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldCreateNote() {
        onBasePage.openNotes()
        onNotes.createNoteAndReturnToList("Note name", "bodytext")
    }

    @Test
    @TmsLink("2585")
    @CreateNote
    @DeleteAllNotes
    @Category(BusinessLogic::class)
    fun shouldDeleteNoteFromList() {
        onBasePage.openNotes()
        onNotes.selectNoteByPosition(1)
        onNotes.shouldBeNoteSelected("Untitled")
        onNotes.clickDeleteInGOForNotesList()
        onNotes.shouldDeleteAlertPresented()
        onNotes.clickConfirmDeleteInGOForNotesList()
        onNotes.shouldBeEmptyNotesList()
    }

    @Test
    @TmsLink("2565")
    @CreateNote(title = "Note name", body = "Text for body")
    @CreateNote
    @DeleteAllNotes
    @Category(Acceptance::class) //BusinessLogic
    fun shouldOpenAndCloseNote() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(2)
        onNotes.shouldBeOnNoteViewer()
        onNotes.shouldBeTitle("Note name")
        onNotes.shouldBodyContain("Text for body")
        onBasePage.pressHardBack()
        onNotes.shouldBeTitleInList("Note name", 2)
    }

    @Test
    @TmsLink("5031")
    @CreateNote
    @PushFileToDevice(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [FilesAndFolders.PHOTO])
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldAddLocalAttachIntoNote() {
        onBasePage.openNotes()
        onBasePage.wait(10, TimeUnit.SECONDS) //wait for notes loading
        onNotes.tapOnNote(1)
        onNotes.tapInBody()
        onNotes.tapOnClip()
        onPhotos.openAlbum("0")
        onPhotos.shouldOpenFirstMediaItem()
        onNotes.shouldAttachesCarouselIsShown()
    }

    @Test
    @TmsLink("4693")
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldBeOnEmptyNotes() {
        onBasePage.openNotes()
        onNotes.shouldBeEmptyNotesList()
    }

    @Test
    @TmsLink("4695")
    @CreateNote(title = "Note for pin", body = "")
    @CreateNote(count = 2)
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldPinNoteFromList() {
        onBasePage.openNotes()
        onNotes.selectNoteByPosition(3)
        onNotes.shouldBeGOActive()
        onNotes.pinInGOForNotesList()
        onNotes.shouldNoteOnPositionBePresented("Note for pin", 1)
    }

    @Test
    @TmsLink("4763")
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldSyncCreatedNoteAfterAirplaneModeOff() {
        onBasePage.openNotes()
        onBasePage.switchToAirplaneMode()
        onNotes.createNoteAndReturnToList("Note name", "bodytext")
        onNotes.shouldNoteBeSynced("Note name", false)
        onNotes.switchToData()
        onNotes.wait(5, TimeUnit.SECONDS)
        onNotes.updateNotesList()
        onNotes.shouldNoteBeSynced("Note name", true)
    }

    @Test
    @TmsLink("4722")
    @Category(Regression::class)
    @CreateNote(count = 3)
    @DeleteAllNotes
    fun shouldGroupModeBeEnabled() {
        onBasePage.openNotes()
        onNotes.selectNoteByPosition(1)
        onNotes.shouldBeGOActive()
        onNotes.shouldBeNSelectedNotes(1)
        onNotes.clickNoteByPosition(2)
        onNotes.shouldBeNSelectedNotes(2)
    }

    @Test
    @TmsLink("4723")
    @Category(Regression::class)
    @DeleteAllNotes
    fun shouldEditNoteTitle() {
        val title = "Title for testcase 4723"
        onBasePage.openNotes()
        onNotes.createNewNote()
        onNotes.shouldEditorBeActivated()
        onNotes.inputTitle(title)
        onNotes.shouldBeTitle(title)
        onNotes.hideKeyboardIfShown()
        onNotes.backInNotesList()
        onNotes.shouldNoteOnPositionBePresented(title, 1)
    }

    @Test
    @TmsLink("4724")
    @Category(Regression::class)
    @DeleteAllNotes
    fun shouldEditNoteBody() {
        val body = "Body for testcase 4724"
        onBasePage.openNotes()
        onNotes.createNewNote()
        onNotes.shouldEditorBeActivated()
        onNotes.inputBody(body)
        onNotes.shouldBodyContain(body)
        onNotes.hideKeyboardIfShown()
        onNotes.backInNotesList()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.shouldNoteOnPositionBePresented(body, 1)
    }

    @Test
    @TmsLink("4764")
    @Category(Regression::class)
    @DeleteAllNotes
    fun shouldNotSaveNoteInAirplaneMode() {
        onBasePage.openNotes()
        onNotes.createNoteAndReturnToList("Note title 1", "Note body 1")
        onNotes.createNoteAndReturnToList("Note title 2", "Note body 2")
        onNotes.createNoteAndReturnToList("Note title 3", "Note body 3")
        onNotes.switchToAirplaneMode()
        onNotes.tapOnNote(3)
        onNotes.shouldBeTitle("Note title 1")
        onNotes.shouldBodyContain("Note body 1")
        onNotes.backInNotesList()
        onNotes.shouldBeTitleInList("Note title 1", 3)
        onNotes.switchToWifi()
    }

    @Test
    @TmsLink("4933")
    @Category(Regression::class)
    @DeleteAllNotes
    @CreateNote(
        title = "Attachment note title 1",
        body = "Attachment note body 1",
        attachments = [FilesAndFolders.FIRST, FilesAndFolders.SECOND]
    )
    @CreateNote(
        title = "Attachment note title 2",
        body = "Attachment note body 2",
        attachments = [FilesAndFolders.SECOND, FilesAndFolders.FIRST]
    )
    fun shouldAttachmentBeDisplayed() {
        onBasePage.openNotes()
        onBasePage.shouldBeOnNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.shouldNoteOnPositionBePresented("Attachment note title 1", 2)
        onNotes.shouldNoteAttachmentOnPositionBePresented(Images.FIRST, 2)
        onNotes.clickNoteByPosition(2)
        onNotes.shouldBeTitle("Attachment note title 1")
        onNotes.shouldBodyContain("Attachment note body 1")
        onNotes.shouldAttachesCarouselIsShown()
        onNotes.shouldAttachOnPositionBePresented(Images.FIRST, 1)
        onNotes.tapOnAttach(2)
        onNotes.shouldAttachesViewerIsShown()
        onPreview.shouldCurrentPhotoBe(Images.SECOND)
        onPreview.closePreview()
        onNotes.shouldBeOnNoteViewer()
        onNotes.backInNotesList()
        onNotes.shouldNoteOnPositionBePresented("Attachment note title 1", 2)
        onNotes.shouldNoteAttachmentOnPositionBePresented(Images.FIRST, 2)
    }
}
