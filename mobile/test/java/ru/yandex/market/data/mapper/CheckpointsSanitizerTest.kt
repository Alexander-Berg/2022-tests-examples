package ru.yandex.market.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.data.mapper.tracking.CheckpointsSanitizer
import ru.yandex.market.clean.domain.model.order.CheckpointDomain
import ru.yandex.market.data.order.tracking.CheckpointStatus
import ru.yandex.market.data.order.tracking.DeliveryStatus

class CheckpointsSanitizerTest {

    private val sanitizer = CheckpointsSanitizer()

    @Test
    fun `Sorts checkpoints by delivery status and time`() {
        val one = CheckpointDomain(
            deliveryStatus = DeliveryStatus.DELIVERY_LOADED,
            time = 3L,
            deliveryMessage = "A",
            status = CheckpointStatus.PENDING
        )
        val two = CheckpointDomain(
            deliveryStatus = DeliveryStatus.DELIVERY_LOADED,
            time = 2L,
            deliveryMessage = "B",
            status = CheckpointStatus.PENDING
        )
        val three = CheckpointDomain(
            deliveryStatus = DeliveryStatus.SENDER_SENT,
            time = 1L,
            deliveryMessage = "C",
            status = CheckpointStatus.PENDING
        )
        val four = CheckpointDomain(
            deliveryStatus = DeliveryStatus.SENDER_SENT,
            time = 0L,
            deliveryMessage = "D",
            status = CheckpointStatus.PENDING
        )
        val input = listOf(one, two, three, four)

        val sanitized = sanitizer.sanitizeDomainCheckpoint(input)

        assertThat(sanitized).contains(four, three, two, one)
    }

    @Test
    fun `Fixes time on neighbor checkpoints`() {

        val one = CheckpointDomain(
            deliveryStatus = DeliveryStatus.SENDER_SENT,
            time = 0L,
            deliveryMessage = "A",
            status = CheckpointStatus.PENDING
        )
        val two = CheckpointDomain(
            deliveryStatus = DeliveryStatus.SENDER_SENT,
            time = 4L,
            deliveryMessage = "B",
            status = CheckpointStatus.PENDING
        )
        val three = CheckpointDomain(
            deliveryStatus = DeliveryStatus.DELIVERY_LOADED,
            time = 3L,
            deliveryMessage = "C",
            status = CheckpointStatus.PENDING
        )
        val four = CheckpointDomain(
            deliveryStatus = DeliveryStatus.DELIVERY_AT_SORTING,
            time = 2L,
            deliveryMessage = "D",
            status = CheckpointStatus.PENDING
        )
        val input = listOf(one, two, three, four)

        val sanitized = sanitizer.sanitizeDomainCheckpoint(input)

        assertThat(sanitized).contains(one, two.withTime(2L), three.withTime(2L), four)
    }

    @Test
    fun `Skips neighbor checkpoints with same delivery message`() {
        val one = CheckpointDomain(
            deliveryStatus = DeliveryStatus.SENDER_SENT,
            time = 0L,
            deliveryMessage = "A",
            status = CheckpointStatus.PENDING
        )
        val two = CheckpointDomain(
            deliveryStatus = DeliveryStatus.DELIVERY_LOADED,
            time = 1L,
            deliveryMessage = "A",
            status = CheckpointStatus.PENDING
        )
        val input = listOf(one, two)

        val sanitized = sanitizer.sanitizeDomainCheckpoint(input)

        assertThat(sanitized).contains(two)
    }

    @Test
    fun `Skips nulls`() {
        val sanitized = sanitizer.sanitizeDomainCheckpoint(listOf(null))
        assertThat(sanitized).isEmpty()
    }
}