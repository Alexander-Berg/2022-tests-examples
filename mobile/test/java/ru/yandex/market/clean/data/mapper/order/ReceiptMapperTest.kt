package ru.yandex.market.clean.data.mapper.order

import com.annimon.stream.Exceptional
import junit.framework.TestCase
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.fapi.dto.FrontApiReceiptDto
import ru.yandex.market.clean.data.fapi.dto.ReceiptStatusDto
import ru.yandex.market.clean.data.fapi.dto.ReceiptTypeDto
import ru.yandex.market.clean.domain.model.order.Receipt
import ru.yandex.market.clean.domain.model.order.ReceiptStatus
import ru.yandex.market.clean.domain.model.order.ReceiptType
import ru.yandex.market.common.datetimeparser.DateTimeParser
import ru.yandex.market.data.order.ReceiptDto
import java.util.Date

class ReceiptMapperTest : TestCase() {

    private val dateTimeParser = mock<DateTimeParser> {
        on { parse(DATE_STRING) } doReturn Exceptional.of { Date(DATE_LONG) }
    }
    private val receiptMapper = ReceiptMapper(dateTimeParser)

    @Test
    fun `test mapping fapi`() {
        val fapiDto = FrontApiReceiptDto(
            id = 1,
            type = ReceiptTypeDto.INCOME,
            createdAt = DATE_LONG,
            status = ReceiptStatusDto.PRINTED
        )
        val expected = Receipt(
            id = "1",
            type = ReceiptType.INCOME,
            status = ReceiptStatus.PRINTED,
            createdAt = Date(DATE_LONG)
        )
        assertEquals(expected, receiptMapper.map(fapiDto))
    }

    @Test
    fun `test mapping capi`() {
        val capiDto = ReceiptDto.builder().id("1").type(ReceiptDto.Type.INCOME).creationDate(DATE_STRING)
            .status(ReceiptDto.Status.PRINTED).build()
        val expected = Receipt(
            id = "1",
            type = ReceiptType.INCOME,
            status = ReceiptStatus.PRINTED,
            createdAt = Date(DATE_LONG)
        )
        assertEquals(expected, receiptMapper.map(capiDto))
    }

    companion object {
        private const val DATE_LONG = 0L
        private const val DATE_STRING = "01 январь 1970"
    }
}