package ru.yandex.staffrecyclerview.formatters

import org.junit.Assert
import org.junit.Test
import ru.yandex.staffrecyclerview.staffUser1
import ru.yandex.staffrecyclerview.staffUser2
import ru.yandex.staffrecyclerview.staffUserUi1
import ru.yandex.staffrecyclerview.staffUserUi2
import ru.yandex.staffrecyclerview.presentation.ui.formatters.StaffUserUiFormatter

class TestStaffUserUiFormatter {

    private val formatter = StaffUserUiFormatter()

    @Test
    fun testStaffUserMapper1() {
        val staffUserConvert = formatter.toStaffUserUi(staffUser1)

        Assert.assertEquals(staffUserUi1, staffUserConvert)
    }

    @Test
    fun testStaffUserMapper2() {
        val staffUserConvert = formatter.toStaffUserUi(staffUser2)

        Assert.assertEquals(staffUserUi2, staffUserConvert)
    }
}
