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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.notes.CreateNote
import ru.yandex.autotests.mobile.disk.android.rules.annotations.notes.DeleteAllNotes
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.NotesSteps

@Feature("Search in notes")
@UserTags("SearchNotes")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
@AuthorizationTest
class SearchInNotesTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onNotes: NotesSteps

    @Test
    @TmsLink("6295")
    @CreateNote(title = "Name", body = "Text for body")
    @CreateNote(title = "Note name", body = "Text for body")
    @CreateNote
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldFindFilterInTitle() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.enterSearch("note")
        onNotes.checkCountOfNotesInList(1)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Note name", 1)
    }

    @Test
    @TmsLink("6297")
    @CreateNote(title = "Name", body = "Text")
    @CreateNote(title = "Note name", body = "Text for .\nsnippet\n.\nbody")
    @CreateNote
    @DeleteAllNotes
    @Category(BusinessLogic::class)
    fun shouldFindFilterInBody() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.enterSearch("body")
        onNotes.checkCountOfNotesInList(1)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Note name", 1)
    }

    @Test
    @TmsLink("6391")
    @CreateNote(title = "Name", body = "Text")
    @CreateNote(title = "Note name", body = "Text for .\nsnippet\n.\nbody")
    @CreateNote
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldSaveFilterInSuggests() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.enterSearch("note")
        onNotes.checkCountOfNotesInList(1)
        onNotes.tapToNoteInListInSearch(1)
        onNotes.shouldBeOnNoteViewer()
        onNotes.pressHardBack()
        onNotes.hideKeyboardIfShown()
        onNotes.pressHardBack()
        onNotes.tapOnSearch()
        onNotes.shouldSuggestsIsShown()
        onNotes.shouldSuggestOnPositionBePresentedInSearch("note", 1)
    }

    @Test
    @TmsLink("6393")
    @CreateNote(title = "Name", body = "Text")
    @CreateNote(title = "Note name", body = "Text for .\nsnippet\n.\nbody")
    @CreateNote
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldNotFoundNotes() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.enterSearch("texts")
        onNotes.checkCountOfNotesInListInSearch(0)
        onNotes.shouldSeeSearchIsEmpty()
    }

    @Test
    @TmsLink("6394")
    @CreateNote(title = "Name", body = "Text")
    @CreateNote(title = "Note name", body = "Text for .\nsnippet\n.\nbody")
    @CreateNote
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldPinNotesUpIt() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.enterSearch("N")
        onNotes.checkCountOfNotesInListInSearch(2)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Name", 2)
        onNotes.tapToNoteInListInSearch(2)
        onNotes.pinNote()
        onNotes.pressHardBack()
        onNotes.checkCountOfNotesInListInSearch(2)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Name", 1)
    }

    @Test
    @TmsLink("6409")
    @CreateNote(title = "Name", body = "Text")
    @CreateNote
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldTapOnClearButtonRemoveText() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.enterSearch("name")
        onNotes.checkCountOfNotesInList(1)
        onNotes.tapToClearButtonInSearch()
        onNotes.shouldSuggestsIsShown()
    }

    @Test
    @TmsLink("6416")
    @CreateNote(title = "Nam", body = "Text")
    @CreateNote(title = "Name2", body = "Text for .\nsnippet\n.\n")
    @CreateNote(title = "Na", body = "Text for .\nsnippet\n.\nbody")
    @CreateNote
    @DeleteAllNotes
    @Category(Regression::class)
    fun shouldListNoteIsSaveAtHardBack() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.enterSearch("name")
        onNotes.checkCountOfNotesInList(1)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Name2", 1)
        onNotes.tapToNoteInListInSearch(1)
        onNotes.shouldBeOnNoteViewer()
        onNotes.pressHardBack()
        onNotes.checkCountOfNotesInList(1)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Name2", 1)
    }

    @Test
    @TmsLink("6404")
    @CreateNote(title = "Name1", body = "Text")
    @CreateNote(title = "Name2", body = "Text for .\nsnippet\n.\n")
    @CreateNote(title = "Name3", body = "Text for .\nsnippet\n.\nbody")
    @CreateNote
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldSuggestsListIsEmpty() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.enterSearch("NAME")
        onNotes.checkCountOfNotesInListInSearch(3)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Name3", 1)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Name2", 2)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Name1", 3)
    }

    @Test
    @TmsLink("6400")
    @CreateNote(title = "Name1", body = "Text")
    @CreateNote(title = "Name2", body = "Text for .\nsnippet\n.\n")
    @CreateNote(title = "Name3", body = "Text for .\nsnippet\n.\nbody")
    @CreateNote
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldNoteFindByFullText() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.enterSearch("body")
        onNotes.checkCountOfNotesInListInSearch(1)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Name3", 1)
    }

    @Test
    @TmsLink("6424")
    @CreateNote(title = "Nam1", body = "Text")
    @CreateNote(title = "Name2", body = "Text for .\nsnippet\n.\n")
    @CreateNote(title = "Nam3", body = "Text for .\nsnippet\n.\nbody")
    @CreateNote
    @DeleteAllNotes
    @Category(FullRegress::class)
    fun shouldPullToRefreshNotModifiedSearch() {
        onBasePage.openNotes()
        onNotes.shouldBeNotEmptyNotesList()
        onNotes.enterSearch("name")
        onNotes.checkCountOfNotesInList(1)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Name2", 1)
        onNotes.updateNotesListInSearch()
        onNotes.checkCountOfNotesInList(1)
        onNotes.shouldNoteOnPositionBePresentedInSearch("Name2", 1)
    }

    @Test
    @TmsLink("5610")
    @Category(Regression::class)
    fun shouldSearchBeDisplayed() {
        onBasePage.openNotes()
        onBasePage.shouldBeOnNotes()
        onNotes.shouldSearchButtonBeDisplayed()
    }

    @Test
    @TmsLink("6223")
    @Category(Regression::class)
    fun shouldSearchFieldBeClearable() {
        onBasePage.openNotes()
        onBasePage.shouldBeOnNotes()
        onNotes.enterSearch("adisk-6223")
        onNotes.tapToClearButtonInSearch()
        onNotes.shouldSearchEditTextBeDisplayed()
        onNotes.shouldSearchEditTextHasText("Search in Notes")
    }
}
