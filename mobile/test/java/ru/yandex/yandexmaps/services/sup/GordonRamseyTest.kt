package ru.yandex.yandexmaps.services.sup

import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import ru.yandex.maps.utils.CaptorUtils
import ru.yandex.yandexmaps.app.IdentifiersLoader
import ru.yandex.yandexmaps.multiplatform.core.auth.Identifiers
import utils.shouldReturn
import java.io.IOException
import java.util.concurrent.TimeUnit

class GordonRamsayTest {

    private lateinit var supService: SupService
    private lateinit var syncStorage: SupSyncStorage
    private lateinit var identifiersLoader: IdentifiersLoader

    private lateinit var gordonRamsay: GordonRamsay

    @Before
    fun setUp() {
        supService = Mockito.mock(SupService::class.java)
        syncStorage = Mockito.mock(SupSyncStorage::class.java)
        identifiersLoader = Mockito.mock(IdentifiersLoader::class.java)

        `when`(supService.updateTags(any(), any())).thenReturn(Completable.complete())
        `when`(identifiersLoader.sharedRequest).thenReturn(Single.just(Identifiers("1234", "5678")))

        gordonRamsay = GordonRamsay(supService, syncStorage, identifiersLoader, Schedulers.trampoline(), Schedulers.trampoline())
    }

    @Test
    fun toggleSuggestFeedback_on() {
        toggleSuggestFeedback(
            true,
            arrayOf(
                TagOp(SupService.TAG_NOTIF, SupService.TAG_NOTIF_REVIEWS_ON, Operation.ADD),
                TagOp(SupService.TAG_NOTIF, SupService.TAG_NOTIF_REVIEWS_OFF, Operation.REMOVE)
            )
        )
    }

    @Test
    fun toggleSuggestFeedback_off() {
        toggleSuggestFeedback(
            false,
            arrayOf(
                TagOp(SupService.TAG_NOTIF, SupService.TAG_NOTIF_REVIEWS_ON, Operation.REMOVE),
                TagOp(SupService.TAG_NOTIF, SupService.TAG_NOTIF_REVIEWS_OFF, Operation.ADD)
            )
        )
    }

    @Test
    fun whenSupEmitsKnownError_completeAfterRetries() {

        val ioScheduler = TestScheduler()
        val ramsay = GordonRamsay(supService, syncStorage, identifiersLoader, ioScheduler, Schedulers.trampoline())

        supService.updateTags(anyList(), any()).shouldReturn(Completable.error(IOException()))

        val subscriber = ramsay.toggle(GordonRamsay.Dish.SuggestReviews, true).test()

        ioScheduler.advanceTimeBy(100, TimeUnit.MINUTES)
        ioScheduler.advanceTimeBy(100, TimeUnit.MINUTES)
        ioScheduler.advanceTimeBy(100, TimeUnit.MINUTES)
        ioScheduler.advanceTimeBy(100, TimeUnit.MINUTES)

        subscriber.assertComplete()
    }

    @Test
    fun sync_storageNotEmpty() {
        `when`(syncStorage.contains(any())).thenReturn(true)

        gordonRamsay.unfreeze()

        verify(syncStorage, times(0)).put(any(), any())
        verify(syncStorage, times(GordonRamsay.Dish.values().size)).remove(any())
        verify(supService, times(GordonRamsay.Dish.values().size)).updateTags(any(), any())
    }

    @Test
    fun sync_storageEmpty() {
        `when`(syncStorage.contains(any())).thenReturn(false)

        gordonRamsay.unfreeze()

        verify(syncStorage, times(GordonRamsay.Dish.values().size)).contains(any())
        verifyZeroInteractions(syncStorage, supService)
    }

    private fun toggleSuggestFeedback(checked: Boolean, expected: Array<TagOp>) {
        gordonRamsay.toggle(GordonRamsay.Dish.SuggestReviews, checked).subscribe()

        val captor: KArgumentCaptor<List<TagOp>> = KArgumentCaptor(CaptorUtils.listCaptor(), List::class)
        verify(supService).updateTags(captor.capture(), any())

        val ops = captor.firstValue
        assertThat(ops)
            .hasSize(2)
            .containsExactlyInAnyOrder(*expected)

        verify(syncStorage).put(any(), eq(checked))
        verify(syncStorage).remove(any())
    }
}
