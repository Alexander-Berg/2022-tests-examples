package ru.auto.data.interactor

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.exception.NotFoundException
import ru.auto.data.model.data.offer.CAR
import ru.auto.data.model.note.Note
import ru.auto.data.repository.INoteRepository
import rx.Single
import kotlin.test.assertEquals

@RunWith(AllureRunner::class)
class NoteInteractorTest {
    val offerIds: Set<String> = setOf("1", "2", "3")
    val notes: Single<Map<String, Note>> = Single.just(createNotes(offerIds))
    val noteRepository: INoteRepository = mock { on { getNotes() } doReturn notes }
    val noteInteractor: INoteInteractor = NoteInteractor(noteRepository)

    private fun createNotes(offerIds: Set<String>): Map<String, Note> {
        return offerIds.map { createNoteEntry(it) }.toMap()
    }

    private fun createNoteEntry(id: String): Pair<String, Note> = id to Note(CAR, id, id)

    @Test
    fun `interactor returns right notes`() {
        assertEquals(notes.toBlocking().value(), noteInteractor.getNotes().toBlocking().value())
    }

    @Test
    fun `interactor returns note if note exists`() {
        val id = offerIds.first()
        assertEquals(notes.toBlocking().value()[id], noteInteractor.getNote(id).toBlocking().value())
    }

    @Test(expected = NotFoundException::class)
    fun `interactor throws exception if note does not exists`() {
        val id = "0"
        noteInteractor.getNote(id).toBlocking().value()
    }
}
