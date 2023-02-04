package ru.auto.data.repository

import io.qameta.allure.kotlin.junit4.AllureRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.auto.data.model.network.scala.draft.NWDraft
import ru.auto.data.network.scala.ScalaApi
import rx.Single
import kotlin.test.assertEquals

/**
 * @author themishkun on 05/07/2018.
 */
@RunWith(AllureRunner::class) class DraftRepositoryTest {
    private val CATEGORY = "CATEGORY"
    private val OFFER_ID = "OFFER_ID"
    private val api = mock<ScalaApi>()
    private val dictionaryRepository = mock<IDictionaryRepository>()

    @Test
    fun `given draft from wizard it should add 'from=wizard' query parameter`() {
        val parameterCaptor = argumentCaptor<String>()
        whenever(api.publishOffer(any(), any(), parameterCaptor.capture())).thenReturn(Single.just(NWDraft()))
        val draftRepository = DraftRepository(
            publishedOfferId = null,
            api = api,
            dictionaryRepository = dictionaryRepository,
            category = CATEGORY
        )

        try {
            draftRepository.publishOffer(offerId = OFFER_ID, isFromWizard = true).toBlocking().value()
        } catch (e: NullPointerException){
            // We don't test offer conversion, just the call to API
        }

        assertEquals("wizard", parameterCaptor.firstValue)
    }

    @Test
    fun `given draft not from wizard it should not add 'from=wizard' query parameter`() {
        val parameterCaptor = argumentCaptor<String>()
        whenever(api.publishOffer(any(), any(), parameterCaptor.capture())).thenReturn(Single.just(NWDraft()))
        val draftRepository = DraftRepository(
            publishedOfferId = null,
            api = api,
            dictionaryRepository = dictionaryRepository,
            category = CATEGORY
        )

        try {
            draftRepository.publishOffer(offerId = OFFER_ID, isFromWizard = false).toBlocking().value()
        } catch (e: NullPointerException){
            // We don't test offer conversion, just the call to API
        }

        assertEquals<String?>(null, parameterCaptor.firstValue)
    }
}
