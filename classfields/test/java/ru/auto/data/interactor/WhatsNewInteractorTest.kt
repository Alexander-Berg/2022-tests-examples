package ru.auto.data.interactor

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.data.repository.whatsnew.IWhatsNewIdRepository
import ru.auto.experiments.Experiments
import ru.auto.experiments.ExperimentsManager
import ru.auto.experiments.whatsNewStoryId
import rx.Single
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @author sorokinandrei on 1/12/21.
 */
@RunWith(AllureRunner::class)
class WhatsNewInteractorTest {

    private var isLarge: () -> Boolean = mock {
        onGeneric { invoke() } doReturn false
    }

    private val idRepo: IWhatsNewIdRepository = mock()

    private val interactor = WhatsNewInteractor(isLarge, idRepo)

    @Before
    fun setup() {
        val expManager: Experiments = mock()
        whenever(expManager.whatsNewStoryId()).thenReturn(WHATS_NEW_ID)
        ExperimentsManager.setInstance(expManager)
    }

    @Test
    fun `should save current version on shown`() {
        interactor.onWhatsNewShown(WHATS_NEW_ID).await()
        verify(idRepo).save(WHATS_NEW_ID)
    }

    @Test
    fun `should return false if id is the same`() {
        whenever(idRepo.get()) doReturn Single.just(WHATS_NEW_ID)

        assertNull(interactor.getWhatsNewStoryId().toBlocking().value())
    }

    @Test
    fun `should return true if id is not the same`() {
        whenever(idRepo.get()) doReturn Single.just("new_id")

        assertEquals(interactor.getWhatsNewStoryId().toBlocking().value(), WHATS_NEW_ID)
    }

    @Test
    fun `should return false if isLarge`() {
        whenever(isLarge.invoke()) doReturn true
        whenever(idRepo.get()) doReturn Single.just("new_id")

        assertNull(interactor.getWhatsNewStoryId().toBlocking().value())
    }

    companion object {
        private const val WHATS_NEW_ID = "some_id"
    }
}
