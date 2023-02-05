package ru.yandex.staffrecyclerview.mappers

import org.junit.Assert
import org.junit.Test
import ru.yandex.staffrecyclerview.data.mappers.StaffUserMapper
import ru.yandex.staffrecyclerview.staffUser1
import ru.yandex.staffrecyclerview.staffUser2
import ru.yandex.staffrecyclerview.staffUserDto1
import ru.yandex.staffrecyclerview.staffUserDto2

class TestStaffUserMapper {

    private val mapper = StaffUserMapper()

    @Test
    fun testStaffUserMapper1() {
        val staffUserConvert = mapper.toStaffUser(staffUserDto1)

        Assert.assertEquals(staffUser1, staffUserConvert)
    }

    @Test
    fun testStaffUserMapper2() {
        val staffUserConvert = mapper.toStaffUser(staffUserDto2)

        Assert.assertEquals(staffUser2, staffUserConvert)
    }
}
