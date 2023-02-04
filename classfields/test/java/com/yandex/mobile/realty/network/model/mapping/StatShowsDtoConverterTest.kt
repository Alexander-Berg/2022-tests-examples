package com.yandex.mobile.realty.network.model.mapping

import com.google.gson.Gson
import com.yandex.mobile.realty.data.mapping.EmptyDescriptor
import com.yandex.mobile.realty.network.InvalidDtoException
import com.yandex.mobile.realty.network.model.StatShowsDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

/**
 * @author rogovalex on 05.06.18.
 */
class StatShowsDtoConverterTest {
    private val gson = Gson()

    @Test(expected = InvalidDtoException::class)
    fun shouldThrowWhenEmpty() {
        StatShowsDto.CONVERTER.map(StatShowsDto(emptyList()), EmptyDescriptor)
    }

    @Test(expected = InvalidDtoException::class)
    fun shouldThrowWhenValuesIsNull() {
        val json = gson.toJson(StatShowsDtoNullable(null))

        val dto = gson.fromJson<StatShowsDto>(json, StatShowsDto::class.java)

        StatShowsDto.CONVERTER.map(dto, EmptyDescriptor)
    }

    @Test(expected = InvalidDtoException::class)
    fun shouldThrowWhenValuesContainsNull() {
        val json = gson.toJson(StatShowsDtoNullable(listOf(null)))

        val dto = gson.fromJson<StatShowsDto>(json, StatShowsDto::class.java)

        StatShowsDto.CONVERTER.map(dto, EmptyDescriptor)
    }

    @Test(expected = InvalidDtoException::class)
    fun shouldThrowWhenValueIsNull() {
        val json = gson.toJson(
            StatShowsDtoNullable(
                listOf(
                    ShowsItemDtoNullable(Date(), null)
                )
            )
        )

        val dto = gson.fromJson<StatShowsDto>(json, StatShowsDto::class.java)

        StatShowsDto.CONVERTER.map(dto, EmptyDescriptor)
    }

    @Test(expected = InvalidDtoException::class)
    fun shouldThrowWhenCardShowIsNegative() {
        val json = gson.toJson(
            StatShowsDtoNullable(
                listOf(
                    ShowsItemDtoNullable(Date(), ShowsDtoNullable(-10))
                )
            )
        )

        val dto = gson.fromJson<StatShowsDto>(json, StatShowsDto::class.java)

        StatShowsDto.CONVERTER.map(dto, EmptyDescriptor)
    }

    @Test(expected = InvalidDtoException::class)
    fun shouldThrowWhenNotLastDateIsNull() {
        val json = gson.toJson(
            StatShowsDtoNullable(
                listOf(
                    ShowsItemDtoNullable(null, ShowsDtoNullable(10)),
                    ShowsItemDtoNullable(Date(), ShowsDtoNullable(20))
                )
            )
        )

        val dto = gson.fromJson<StatShowsDto>(json, StatShowsDto::class.java)

        StatShowsDto.CONVERTER.map(dto, EmptyDescriptor)
    }

    @Test(expected = InvalidDtoException::class)
    fun shouldThrowWhenLastDateIsNull() {
        val json = gson.toJson(
            StatShowsDtoNullable(
                listOf(
                    ShowsItemDtoNullable(Date(), ShowsDtoNullable(10)),
                    ShowsItemDtoNullable(null, ShowsDtoNullable(20))
                )
            )
        )

        val dto = gson.fromJson<StatShowsDto>(json, StatShowsDto::class.java)

        StatShowsDto.CONVERTER.map(dto, EmptyDescriptor)
    }

    @Test(expected = InvalidDtoException::class)
    fun shouldThrowWhenEqualDates() {
        val json = gson.toJson(
            StatShowsDtoNullable(
                listOf(
                    ShowsItemDtoNullable(Date(), ShowsDtoNullable(10)),
                    ShowsItemDtoNullable(Date(), ShowsDtoNullable(20))
                )
            )
        )

        val dto = gson.fromJson<StatShowsDto>(json, StatShowsDto::class.java)

        StatShowsDto.CONVERTER.map(dto, EmptyDescriptor)
    }

    @Test(expected = InvalidDtoException::class)
    fun shouldThrowWhenNotSequentialDates() {
        val calendar = Calendar.getInstance()
        val lastDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val firstDate = calendar.time
        val json = gson.toJson(
            StatShowsDtoNullable(
                listOf(
                    ShowsItemDtoNullable(firstDate, ShowsDtoNullable(10)),
                    ShowsItemDtoNullable(lastDate, ShowsDtoNullable(20))
                )
            )
        )

        val dto = gson.fromJson<StatShowsDto>(json, StatShowsDto::class.java)

        StatShowsDto.CONVERTER.map(dto, EmptyDescriptor)
    }

    @Test(expected = InvalidDtoException::class)
    fun convertWhenCardShowIsNull() {
        val nullableItem = ShowsItemDtoNullable(Date(), ShowsDtoNullable(null))

        val json = gson.toJson(StatShowsDtoNullable(listOf(nullableItem)))

        val dto = gson.fromJson<StatShowsDto>(json, StatShowsDto::class.java)

        StatShowsDto.CONVERTER.map(dto, EmptyDescriptor)
    }

    @Test
    fun convertMultipleItems() {
        val calendar = Calendar.getInstance()
        val lastDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val middleDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val firstDate = calendar.time
        val json = gson.toJson(
            StatShowsDtoNullable(
                listOf(
                    ShowsItemDtoNullable(firstDate, ShowsDtoNullable(0)),
                    ShowsItemDtoNullable(middleDate, ShowsDtoNullable(10)),
                    ShowsItemDtoNullable(lastDate, ShowsDtoNullable(20))
                )
            )
        )

        val dto = gson.fromJson<StatShowsDto>(json, StatShowsDto::class.java)

        val data = StatShowsDto.CONVERTER.map(dto, EmptyDescriptor)
        assertEquals(3, data.values.size)
        assertEquals(lastDate.time / 1000, data.date.time / 1000)
        assertEquals(0, data.values[0])
        assertEquals(10, data.values[1])
        assertEquals(20, data.values[2])
    }

    class StatShowsDtoNullable(val values: List<ShowsItemDtoNullable?>?)

    class ShowsItemDtoNullable(val date: Date?, val value: ShowsDtoNullable?)

    class ShowsDtoNullable(val cardShow: Int?)
}
