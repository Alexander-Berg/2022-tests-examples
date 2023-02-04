package ru.auto.testdata

import ru.auto.data.model.AutoruUserProfile
import ru.auto.data.model.User
import ru.auto.data.model.UserEmail
import ru.auto.data.model.UserPhone
import ru.auto.data.model.UserProfile

val USER_GENERIC = User.Authorized(
    id = "id00000000",
    userProfile = UserProfile(AutoruUserProfile("")),
    phones = listOf(UserPhone("+7 (000) 000-00-00")),
    emails = listOf(UserEmail("test@test.ru")),
    balance = 0,
)
