package ru.yandex.market.checkout.domain.usecase

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.checkout.data.repository.CheckoutStateRepository
import ru.yandex.market.clean.domain.model.cart.PackPositions

class PackIdsUseCaseTest {

    private val checkoutStateRepository = mock<CheckoutStateRepository>()
    private val packPositions = PackPositions(mapOf(packId1 to 0, packId2 to 1))
    private val useCase = PackIdsUseCase(checkoutStateRepository, packPositions)

    @Test
    fun `Returns next packId when current is not last`() {
        whenever(checkoutStateRepository.normalizedPackIds).thenReturn(Single.just(listOf(packId1, packId2)))

        useCase.getNextPackId(packId1)
            .test()
            .assertValue(packId2)
    }

    @Test
    fun `Returns current packId when it is last`() {
        whenever(checkoutStateRepository.normalizedPackIds).thenReturn(Single.just(listOf(packId1, packId2)))

        useCase.getNextPackId(packId2)
            .test()
            .assertValue(packId2)
    }

    @Test
    fun `Returns current packId when ids contains only one`() {

        whenever(checkoutStateRepository.normalizedPackIds).thenReturn(Single.just(listOf(packId1)))

        useCase.getNextPackId(packId1)
            .test()
            .assertValue(packId1)
    }

    companion object {
        private const val packId1 = "packId1"
        private const val packId2 = "packId2"
    }
}