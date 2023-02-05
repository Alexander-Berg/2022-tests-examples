package ru.yandex.market.clean.domain.usecase.postamate

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.softlogic.boxbot.boxboxbluetoot.model.RequestResult
import ru.yandex.market.clean.data.repository.postamate.PostamateCellCodesRepository
import ru.yandex.market.clean.domain.model.postamat.PostamateCheckCodeResult
import ru.yandex.market.optional.Optional

class TryOpenPostamateWithCachedCodeUseCaseTest {

    private val getPostamateCellCodeUseCase = mock<GetPostamateCellCodeUseCase>()
    private val postamateGetShipmentUseCase = mock<PostamateGetShipmentUseCase>()
    private val postamateCellCodesRepository = mock<PostamateCellCodesRepository>()
    private val tryOpenPostamateWithCachedCodeUseCase = TryOpenPostamateWithCachedCodeUseCase(
        getPostamateCellCodeUseCase,
        postamateGetShipmentUseCase,
        postamateCellCodesRepository,
    )

    @Test
    fun `everything ok`() {
        whenever(getPostamateCellCodeUseCase.getCachedCode(any())).thenReturn(Single.just(Optional.of(CACHED_CODE)))
        whenever(postamateGetShipmentUseCase.checkCode(CACHED_CODE)).thenReturn(Observable.just(SUCCESS_CHECK_RESULT))

        tryOpenPostamateWithCachedCodeUseCase.execute(ORDER_ID).test().assertValue(SUCCESS_CHECK_RESULT)
    }

    @Test
    fun `no cached code`() {
        whenever(getPostamateCellCodeUseCase.getCachedCode(any())).thenReturn(Single.just(Optional.empty()))
        whenever(postamateGetShipmentUseCase.checkCode(CACHED_CODE)).thenReturn(Observable.just(SUCCESS_CHECK_RESULT))

        tryOpenPostamateWithCachedCodeUseCase.execute(ORDER_ID).test().assertValue(ERROR_CHECK_RESULT)
    }

    @Test
    fun `error during check code`() {
        whenever(getPostamateCellCodeUseCase.getCachedCode(any())).thenReturn(Single.just(Optional.of(CACHED_CODE)))
        whenever(postamateGetShipmentUseCase.checkCode(CACHED_CODE)).thenReturn(Observable.just(ERROR_CHECK_RESULT))

        tryOpenPostamateWithCachedCodeUseCase.execute(ORDER_ID).test().assertValue(ERROR_CHECK_RESULT)
    }

    companion object {
        private const val CACHED_CODE = "cached code"
        private const val ORDER_ID = "order id"
        private val SUCCESS_CHECK_RESULT = PostamateCheckCodeResult.Success(RequestResult.OK)
        private val ERROR_CHECK_RESULT = PostamateCheckCodeResult.Error
    }
}