package ru.yandex.market.clean.domain.usecase.agitation

import com.annimon.stream.Optional
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import ru.yandex.market.clean.data.mapper.agitations.AgitationTypeMapper
import ru.yandex.market.clean.data.mapper.agitations.OrderAgitationMapper
import ru.yandex.market.clean.data.repository.agitation.OrderAgitationsRepository
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderAgitationsUseCaseTestEntity.AGITATION_DTO_MOCK_1
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderAgitationsUseCaseTestEntity.AGITATION_DTO_MOCK_2
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderAgitationsUseCaseTestEntity.ORDER_AGITATION_DELETED_ITEM_RESULT
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderAgitationsUseCaseTestEntity.ORDER_CANCELLED_BY_USER_AGITATION_RESULT
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderAgitationsUseCaseTestEntity.ORDER_DIFF_MOCK
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderAgitationsUseCaseTestEntity.ORDER_ID
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderAgitationsUseCaseTestEntity.ORDER_MOCK
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.domain.usecase.order.IsChangeDeliveryDatesAvailableUseCase
import ru.yandex.market.mockResult

class GetOrderAgitationsUseCaseTest {

    private val repository = mock<OrderAgitationsRepository>()
    private val agitationTypeMapper = AgitationTypeMapper()
    private val agitationMapper = mock<OrderAgitationMapper>()
    private val getOrderUseCase = mock<GetOrderUseCase>()
    private val isChangeDeliveryDatesAvailableUseCase = mock<IsChangeDeliveryDatesAvailableUseCase>()
    private val getOrdersDiffUseCase = mock<GetOrdersDiffUseCase>()

    private val useCase = GetOrderAgitationsUseCase(
        repository = repository,
        agitationTypeMapper = agitationTypeMapper,
        agitationMapper = agitationMapper,
        getOrderUseCase = getOrderUseCase,
        isChangeDeliveryDatesAvailableUseCase = isChangeDeliveryDatesAvailableUseCase,
        getOrdersDiffUseCase = getOrdersDiffUseCase,
    )

    @Before
    fun setUp() {

        getOrderUseCase.execute(ORDER_ID, false).mockResult(Single.just(ORDER_MOCK))
        isChangeDeliveryDatesAvailableUseCase.execute(ORDER_ID).mockResult(Single.just(false))
        getOrdersDiffUseCase.execute(ORDER_ID, false).mockResult(Single.just(listOf(ORDER_DIFF_MOCK)))
    }

    @Test
    fun `checking getting of the deleted item's agitations for the order`() {

        agitationMapper.map(any(), any(), any(), anyOrNull())
            .mockResult(ORDER_AGITATION_DELETED_ITEM_RESULT)

        val agitations = listOf(

            AGITATION_DTO_MOCK_1,
            AGITATION_DTO_MOCK_2.copy(entityId = ORDER_ID, type = "ORDER_ITEM_REMOVAL")
        )

        repository.getOrderAgitations().mockResult(Single.just(agitations))

        useCase.execute(ORDER_ID)
            .test()
            .assertNoErrors()
            .assertResult(Optional.of(ORDER_AGITATION_DELETED_ITEM_RESULT))

        verify(repository).getOrderAgitations()
        verify(getOrderUseCase).execute(ORDER_ID, false)
        verify(getOrdersDiffUseCase).execute(ORDER_ID, false)
        verify(agitationMapper).map(any(), any(), any(), anyOrNull())
    }

    @Test
    fun `checking getting of the canceled agitations for the order`() {

        agitationMapper.map(any(), any(), any(), anyOrNull())
            .mockResult(ORDER_CANCELLED_BY_USER_AGITATION_RESULT)

        val agitations = listOf(

            AGITATION_DTO_MOCK_1,
            AGITATION_DTO_MOCK_2.copy(entityId = ORDER_ID, type = "ORDER_CANCELLED_BY_USER_EXTERNALLY")
        )

        repository.getOrderAgitations().mockResult(Single.just(agitations))

        useCase.execute(ORDER_ID)
            .test()
            .assertNoErrors()
            .assertResult(Optional.of(ORDER_CANCELLED_BY_USER_AGITATION_RESULT))

        verify(repository).getOrderAgitations()
        verify(getOrderUseCase).execute(ORDER_ID, false)
        verifyZeroInteractions(getOrdersDiffUseCase)
        verify(agitationMapper).map(any(), any(), any(), anyOrNull())
    }

    @Test
    fun `checking getting of the agitations for the order on empty`() {

        repository.getOrderAgitations().mockResult(Single.just(emptyList()))
        agitationMapper.map(any(), any(), any(), anyOrNull())
            .mockResult(ORDER_AGITATION_DELETED_ITEM_RESULT)

        useCase.execute()
            .test()
            .assertNoErrors()
            .assertResult(Optional.empty())

        verify(repository).getOrderAgitations()
        verifyZeroInteractions(agitationMapper, getOrderUseCase, getOrdersDiffUseCase)
    }
}
