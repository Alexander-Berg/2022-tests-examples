package ru.yandex.staffrecyclerview

import ru.yandex.staffrecyclerview.data.models.StaffInfoDto
import ru.yandex.staffrecyclerview.data.models.StaffUserDto
import ru.yandex.staffrecyclerview.domain.models.StaffInfo
import ru.yandex.staffrecyclerview.domain.models.StaffUser
import ru.yandex.staffrecyclerview.presentation.ui.models.StaffUserUi

val staffUserDto1 = StaffUserDto(
    firstName = null,
    lastName = null,
    login = null,
    avatar = null,
    location = null,
    department = null
)

val staffUserDto2 = StaffUserDto(
    firstName = "Дмитрий",
    lastName = "Лосев",
    login = "dmitrilosev",
    avatar = "https://yandex.ru/",
    location = "Москва",
    department = "Маркет"
)

val staffUser1 = StaffUser(
    firstName = "",
    lastName = "",
    login = "@",
    avatar = "",
    location = "",
    department = ""
)

val staffUser2 = StaffUser(
    firstName = "Дмитрий",
    lastName = "Лосев",
    login = "@dmitrilosev",
    avatar = "https://yandex.ru/",
    location = "Москва",
    department = "Маркет"
)

val staffUserUi1 = StaffUserUi(
    name = " ",
    login = "@",
    avatar = "",
    location = "",
    department = ""
)

val staffUserUi2 = StaffUserUi(
    name = "Дмитрий Лосев",
    login = "@dmitrilosev",
    avatar = "https://yandex.ru/",
    location = "Москва",
    department = "Маркет"
)

val staffInfoDto1 = StaffInfoDto(
    page = null,
    pagesCount = null,
    staffUsers = null
)

val staffInfoDto2 = StaffInfoDto(
    page = 1,
    pagesCount = 100,
    staffUsers = listOf(staffUserDto1, staffUserDto2)
)

val staffInfoDto3 = StaffInfoDto(
    page = 234,
    pagesCount = 432432,
    staffUsers = listOf(
        staffUserDto1,
        staffUserDto1,
        staffUserDto1,
        staffUserDto2,
        staffUserDto2,
        staffUserDto2
    )
)

val staffInfoDto4 = StaffInfoDto(
    page = 32,
    pagesCount = 3443,
    staffUsers = emptyList()
)

val staffInfo1 = StaffInfo(
    page = 1,
    pagesCount = 1,
    staffUsers = emptyList()
)

val staffInfo2 = StaffInfo(
    page = 1,
    pagesCount = 100,
    staffUsers = listOf(staffUser1, staffUser2)
)

val staffInfo3 = StaffInfo(
    page = 234,
    pagesCount = 432432,
    staffUsers = listOf(
        staffUser1,
        staffUser1,
        staffUser1,
        staffUser2,
        staffUser2,
        staffUser2
    )
)

val staffInfo4 = StaffInfo(
    page = 32,
    pagesCount = 3443,
    staffUsers = emptyList()
)

