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
import java.util.concurrent.TimeUnit

@Feature("NoteItems")
@UserTags("notes")
@AuthorizationTest
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class NotesTest2 : NotesTestBase() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("4694")
    @CreateNote(count = 3)
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldBeOnNotEmptyNotes() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.shouldBeTitleInList("Untitled", 1)
        onNotes.shouldSecondLineContain(":", 1)
        onNotes.checkCountOfNotesInList(3)
    }

    @Test
    @TmsLink("5055")
    @CreateNote(title = "Note for pin")
    @CreateNote
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldPinnedNoteBeOnTopOfList() {
        onBasePage.openNotes()
        onNotes.tapOnNote(2)
        onNotes.shouldBeOnNoteViewer()
        onNotes.shouldNoteIsUnpinned()
        onNotes.pinNote()
        onNotes.pressHardBack()
        onNotes.shouldNoteInListBePinned("Note for pin")
        onNotes.shouldBeTitleInList("Note for pin", 1)
    }

    @Test
    @TmsLink("4703")
    @CreateNote(title = "This note will be delete", body = "Good by, my dear note...")
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldDeleteNoteFromViewer() {
        onBasePage.openNotes()
        onNotes.tapOnNote(1)
        onNotes.shouldBeOnNoteViewer()
        onNotes.tapOnDelete()
        onNotes.shouldDeleteAlertInViewerBePresented()
        onNotes.clickConfirmDeleteInGOForNotesList()
        onNotes.shouldBeEmptyNotesList()
        onNotes.updateNotesList()
        onNotes.shouldBeEmptyNotesList()
    }

    @Test
    @TmsLink("5027")
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldBeDeleteAttach() {
        onNotesApi.createNoteWithAttach(
            "This note for attaches",
            "Here could be your advertisement",
            null,
            FilesAndFolders.ORIGINAL_FILE
        )
        onBasePage.openNotes()
        onNotes.tapOnNote(1)
        onNotes.shouldAttachesCarouselIsShown()
        onNotes.selectAttachByPosition(1)
        onNotes.tapOnDelete()
        onNotes.clickConfirmDeleteInViewer()
        onNotes.shouldAttachesCarouselIsNotShown()
    }

    @Test
    @TmsLink("5057")
    @Category(BusinessLogic::class)
    @DeleteAllNotes
    fun shouldOpenAndCloseAttachInViewer() {
        onNotesApi.createNoteWithAttach(
            "This note for attaches",
            "Here could be your advertisement",
            null,
            FilesAndFolders.ORIGINAL_FILE
        )
        onBasePage.openNotes()
        onNotes.tapOnNote(1)
        onNotes.shouldAttachesCarouselIsShown()
        onNotes.tapOnAttach(1)
        onNotes.shouldAttachesViewerIsShown()
        onNotes.swipeUpToDownNTimes(1)
        onNotes.shouldBeOnNoteViewer()
    }

    @Test
    @TmsLink("4771")
    @DeleteAllNotes
    @CreateNote
    @Category(Regression::class)
    fun shouldDeleteNoteFromViewerInOffline() {
        onBasePage.openNotes()
        onBasePage.wait(10, TimeUnit.SECONDS) //wait for notes loading
        onNotes.shouldBeNotEmptyNotesList()
        onMobile.switchToAirplaneMode()
        onNotes.tapOnNote(1)
        onNotes.shouldBeOnNoteViewer()
        onNotes.tapOnDelete()
        onNotes.shouldDeleteAlertInViewerBePresented()
        onNotes.clickConfirmDeleteInGOForNotesList()
        onNotes.shouldBeEmptyNotesList()
        onMobile.switchToData()
        onNotes.updateNotesList()
        onNotes.shouldBeEmptyNotesList()
    }

    @Test
    @TmsLink("4696")
    @CreateNote
    @CreateNote(title = "Note for unpin", body = "LOLOLO", tag = [CreateNote.PIN], count = 2)
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldUnpinNoteFromList() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.selectNoteByPosition(1)
        onNotes.clickUnpinInGOForNotesList()
        onNotes.shouldNoteOnPositionBePresented("Note for unpin 1", 3)
    }

    @Test
    @TmsLink("4707")
    @CreateNote
    @CreateNote(title = "Note for unpin", body = "LOLOLO", tag = [CreateNote.PIN], count = 3)
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldUnpinMultipleNotesFromList() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.selectNoteByPosition(1)
        onNotes.clickNoteByPosition(2)
        onNotes.clickUnpinInGOForNotesList()
        onNotes.shouldNoteOnPositionBePresented("Note for unpin 2", 3)
        onNotes.shouldNoteOnPositionBePresented("Note for unpin 1", 4)
    }

    @Test
    @TmsLink("4708")
    @CreateNote(title = "Note for unpin", body = "LOLOLO", tag = [CreateNote.PIN])
    @CreateNote(title = "This note 1", body = "Good by, my dear note...")
    @CreateNote(title = "This note 2", body = "Good by, my dear note...")
    @CreateNote(title = "This note 3", body = "Good by, my dear note...")
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldPinMultipleMixNotesFromList() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.selectNoteByPosition(3)
        onNotes.clickNoteByPosition(4)
        onNotes.pinInGOForNotesList()
        onNotes.shouldNoteOnPositionBePresented("This note 2", 2)
        onNotes.shouldNoteOnPositionBePresented("This note 1", 3)
    }

    @Test
    @TmsLink("4709")
    @CreateNote(title = "Note for unpin", body = "LOLOLO", tag = [CreateNote.PIN])
    @CreateNote(title = "This note 1", body = "Good by, my dear note...")
    @CreateNote(title = "This note 2", body = "Good by, my dear note...")
    @CreateNote(title = "This note 3", body = "Good by, my dear note...")
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldPinMultipleNotesFromList() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.selectNoteByPosition(2)
        onNotes.clickNoteByPosition(3)
        onNotes.pinInGOForNotesList()
        onNotes.shouldNoteOnPositionBePresented("This note 3", 2)
        onNotes.shouldNoteOnPositionBePresented("This note 2", 3)
    }

    @Test
    @TmsLink("4711")
    @CreateNote(title = "Note for unpin", body = "LOLOLO", tag = [CreateNote.PIN])
    @CreateNote(title = "This note 1", body = "Good by, my dear note...")
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldDeleteAllMultipleNotesFromList() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.selectNoteByPosition(1)
        onNotes.clickNoteByPosition(2)
        onNotes.clickDeleteInGOForNotesList()
        onNotes.shouldDeleteAlertPresented()
        onNotes.clickConfirmDeleteInGOForNotesList()
        onNotes.shouldBeEmptyNotesList()
    }

    @Test
    @TmsLink("4710")
    @CreateNote(title = "Note for delete", body = "LOLOLO", tag = [CreateNote.PIN], count = 2)
    @CreateNote(title = "This note 1", body = "Good by, my dear note...")
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldDeleteMultipleNotesFromList() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.selectNoteByPosition(3)
        onNotes.clickNoteByPosition(2)
        onNotes.clickDeleteInGOForNotesList()
        onNotes.shouldDeleteAlertPresented()
        onNotes.clickConfirmDeleteInGOForNotesList()
        onNotes.checkCountOfNotesInList(1)
    }

    @Test
    @TmsLink("4772")
    @CreateNote(title = "Note for unpin", body = "LOLOLO", tag = [CreateNote.PIN], count = 3)
    @CreateNote(title = "This note 1", body = "Good by, my dear note...")
    @CreateNote(title = "This note 2", body = "Good by, my dear note...")
    @CreateNote(title = "This note 3", body = "Good by, my dear note...")
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldPinUnpinMultipleNotesFromListInOffline() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.selectNoteByPosition(4)
        onNotes.clickNoteByPosition(5)
        onNotes.pinInGOForNotesList()
        onNotes.shouldNoteOnPositionBePresented("This note 3", 4)
        onNotes.shouldNoteOnPositionBePresented("This note 2", 5)
        onNotes.selectNoteByPosition(3)
        onNotes.clickNoteByPosition(2)
        onNotes.clickUnpinInGOForNotesList()
        onNotes.shouldNoteOnPositionBePresented("Note for unpin 1", 4)
        onNotes.shouldNoteOnPositionBePresented("Note for unpin 0", 5)
    }

    @Test
    @TmsLink("5028")
    @CreateNote(
        title = "Attachment note title 1",
        body = "Attachment note body 1",
        attachments = [FilesAndFolders.FIRST, FilesAndFolders.SECOND]
    )
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldBeDeleteNotAllAttach() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(1)
        onNotes.shouldAttachesCarouselIsShown()
        onNotes.selectAttachByPosition(1)
        onNotes.tapOnDelete()
        onNotes.clickConfirmDeleteInViewer()
        onNotes.shouldAttachesCountBeEqualN(1)
    }

    @Test
    @TmsLink("5029")
    @CreateNote(
        title = "Attachment note title 1",
        body = "Attachment note body 1",
        attachments = [FilesAndFolders.FIRST, FilesAndFolders.SECOND]
    )
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldBeDeleteAllAttach() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(1)
        onNotes.shouldAttachesCarouselIsShown()
        onNotes.selectAttachByPosition(1)
        onNotes.selectAttachByPosition(2)
        onNotes.tapOnDelete()
        onNotes.clickConfirmDeleteInViewer()
        onNotes.shouldAttachesCarouselIsNotShown()
    }

    @Test
    @TmsLink("5030")
    @CreateNote(
        title = "Attachment note title 1",
        body = "Attachment note body 1",
        attachments = [FilesAndFolders.FIRST, FilesAndFolders.SECOND]
    )
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldBeDeleteNotAllAttachInOfflineMode() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(1)
        onBasePage.switchToAirplaneMode()
        onNotes.shouldAttachesCarouselIsShown()
        onNotes.selectAttachByPosition(1)
        onNotes.tapOnDelete()
        onNotes.clickConfirmDeleteInViewer()
        onMobile.switchToData()
        onMobile.pressHardBack()
        onNotes.updateNotesList()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(1)
        onNotes.shouldAttachesCountBeEqualN(1)
    }

    @Test
    @TmsLink("5032")
    @CreateNote(
        title = "Attachment note title 1",
        body = "Attachment note body 1",
        attachments = [FilesAndFolders.FIRST, FilesAndFolders.JPG_1]
    )
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldBeAddAllAttachAllType() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(1)
        onNotes.shouldAttachesCountBeEqualN(2)
    }

    @Test
    @TmsLink("5034")
    @PushFileToDevice(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [FilesAndFolders.PHOTO])
    @CreateNote
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldAddLocalAttachIntoNoteInOfflineMode() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(1)
        onNotes.tapInBody()
        onBasePage.switchToAirplaneMode()
        onNotes.tapOnClip()
        onPhotos.openAlbum("0")
        onPhotos.shouldOpenFirstMediaItem()
        onNotes.shouldAttachesCarouselIsShown()
        onMobile.switchToData()
        onMobile.pressHardBack()
        onNotes.updateNotesList()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(1)
        onNotes.shouldAttachesCountBeEqualN(1)
    }

    @Test
    @TmsLink("5035")
    @PushFileToDevice(filePaths = [FilesAndFolders.PHOTO])
    @DeleteFilesOnDevice(filesPath = [FilesAndFolders.PHOTO])
    @CreateNote(
        title = "Attachment note title 1",
        body = "Attachment note body 1",
        attachments = [FilesAndFolders.FIRST]
    )
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldAddLocalAttachAndDeleteAttachIntoNoteInOfflineMode() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(1)
        onNotes.tapInBody()
        onBasePage.switchToAirplaneMode()
        onNotes.shouldAttachesCarouselIsShown()
        onNotes.selectAttachByPosition(1)
        onNotes.tapOnDelete()
        onNotes.clickConfirmDeleteInViewer()
        onNotes.tapOnClip()
        onPhotos.openAlbum("0")
        onPhotos.shouldOpenFirstMediaItem()
        onNotes.shouldAttachesCarouselIsShown()
        onMobile.switchToData()
        onMobile.pressHardBack()
        onNotes.updateNotesList()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(1)
        onNotes.shouldAttachesCountBeEqualN(1)
    }

    @Test
    @TmsLink("5041")
    @CreateNote(
        title = "Attachment note title 1",
        body = "Attachment note body 1",
        attachments = [FilesAndFolders.FIRST]
    )
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldBeCancelGoMode() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.tapOnNote(1)
        onNotes.shouldAttachesCarouselIsShown()
        onNotes.selectAttachByPosition(1)
        onNotes.shouldBeGOActivedInNotesTile()
        onGroupMode.shouldClickCloseButton()
        onBasePage.shouldBeNotOnGroupMode()
        onNotes.selectAttachByPosition(1)
        onNotes.selectAttachByPosition(1)
        onBasePage.shouldBeNotOnGroupMode()
    }
}
