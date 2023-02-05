package ru.yandex.staffrecyclerview.mappers

import org.junit.Assert
import org.junit.Test
import ru.yandex.staffrecyclerview.*
import ru.yandex.staffrecyclerview.data.mappers.StaffInfoMapper

class TestStaffInfoMapper {

    private val mapper = StaffInfoMapper()

    @Test
    fun testStaffUserMapper1() {
        val staffInfoConvert = mapper.toStaffInfo(staffInfoDto1)

        Assert.assertEquals(staffInfo1, staffInfoConvert)
    }

    @Test
    fun testStaffUserMapper2() {
        val staffInfoConvert = mapper.toStaffInfo(staffInfoDto2)

        Assert.assertEquals(staffInfo2, staffInfoConvert)
    }

    @Test
    fun testStaffUserMapper3() {
        val staffInfoConvert = mapper.toStaffInfo(staffInfoDto3)

        Assert.assertEquals(staffInfo3, staffInfoConvert)
    }

    @Test
    fun testStaffUserMapper4() {
        val staffInfoConvert = mapper.toStaffInfo(staffInfoDto4)

        Assert.assertEquals(staffInfo4, staffInfoConvert)
    }
}
