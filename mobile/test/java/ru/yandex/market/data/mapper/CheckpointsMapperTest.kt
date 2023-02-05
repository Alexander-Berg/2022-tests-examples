package ru.yandex.market.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.mapper.tracking.CheckpointsMapper
import ru.yandex.market.clean.data.mapper.tracking.CheckpointsSanitizer
import ru.yandex.market.clean.domain.model.order.CheckpointDomain
import ru.yandex.market.clean.domain.model.order.Order
import ru.yandex.market.data.order.OrderStatus
import ru.yandex.market.data.order.tracking.CheckpointStatus
import ru.yandex.market.data.order.tracking.DeliveryStatus.DELIVERY_DELIVERED
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.tracking.model.domain.CanceledCheckpoint
import ru.yandex.market.tracking.model.domain.CompletedCheckpoint
import ru.yandex.market.tracking.model.domain.CreationCheckpoint
import ru.yandex.market.tracking.model.domain.PassedCheckpoint

@RunWith(MockitoJUnitRunner::class)
class CheckpointsMapperTest {

    private val checkpointsSanitizer = mock<CheckpointsSanitizer>()

    private val dateTimeProvider = mock<DateTimeProvider>()

    private val mapper =
        CheckpointsMapper(dateTimeProvider, checkpointsSanitizer)

    @Test
    fun `Sanitizes checkpoints before mapping`() {
        val sanitized = listOf(
            CheckpointDomain(0, 100, null, CheckpointStatus.IN_TRANSIT),
            CheckpointDomain(1, 0, null, CheckpointStatus.IN_TRANSIT),
        )
        whenever(checkpointsSanitizer.sanitizeDomainCheckpoint(any())).thenReturn(sanitized)

        mapper.map(Order.generateTestInstance())

        verify(checkpointsSanitizer).sanitizeDomainCheckpoint(any())
    }

    @Test
    fun `Returns creation and completed checkpoints when checkpoints are empty and order is delivered`() {
        whenever(checkpointsSanitizer.sanitizeDomainCheckpoint(any())).thenReturn(emptyList())
        val order = Order.generateTestInstance().copy(status = OrderStatus.DELIVERED)

        val mapped = mapper.map(order)

        assertThat(mapped).`is`(
            HamcrestCondition(
                Matchers.contains(
                    Matchers.instanceOf(CreationCheckpoint::class.java),
                    Matchers.instanceOf(CompletedCheckpoint::class.java)
                )
            )
        )
    }

    @Test
    fun `Returns creation and canceled checkpoints when checkpoints are empty and order is canceled`() {
        whenever(checkpointsSanitizer.sanitizeDomainCheckpoint(any())).thenReturn(emptyList())
        val order = Order.generateTestInstance().copy(status = OrderStatus.CANCELLED)

        val mapped = mapper.map(order)

        assertThat(mapped).`is`(
            HamcrestCondition(
                Matchers.contains(
                    Matchers.instanceOf(CreationCheckpoint::class.java),
                    Matchers.instanceOf(CanceledCheckpoint::class.java)
                )
            )
        )
    }

    @Test
    fun `Properly maps checkpoints`() {
        val sanitized = listOf(

            CheckpointDomain(0, 0, null, CheckpointStatus.PENDING),
            CheckpointDomain(DELIVERY_DELIVERED, 100, null, CheckpointStatus.DELIVERED)
        )
        whenever(checkpointsSanitizer.sanitizeDomainCheckpoint(any())).thenReturn(sanitized)

        val order = Order.generateTestInstance()

        val mapped = mapper.map(order)

        assertThat(mapped).`is`(
            HamcrestCondition(
                Matchers.contains(
                    Matchers.instanceOf(CreationCheckpoint::class.java),
                    Matchers.instanceOf(PassedCheckpoint::class.java),
                    Matchers.instanceOf(CompletedCheckpoint::class.java)
                )
            )
        )
    }

    @Test
    fun `Returns creation checkpoint when checkpoints are empty and order is not finished`() {
        whenever(checkpointsSanitizer.sanitizeDomainCheckpoint(any())).thenReturn(emptyList())
        val order = Order.generateTestInstance().copy(status = OrderStatus.PLACING)

        val mapped = mapper.map(order)

        assertThat(mapped).`is`(
            HamcrestCondition(
                Matchers.contains(
                    Matchers.instanceOf(CreationCheckpoint::class.java)
                )
            )
        )
    }
}