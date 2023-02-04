package ru.yandex.vertis.passport.service.user

import org.joda.time.DateTime
import org.scalatest.FunSuite
import ru.yandex.vertis.passport.model.{AutoruUserProfile, FullUser, PasswordHashingStrategies, PasswordHashingStrategy, UserProfile}

class PasswordUtilsTest extends FunSuite {
  test("passwordMatches for user with empty pwdHash") {
    val user = FullUser(
      id = "user:1000",
      profile = AutoruUserProfile(),
      registrationDate = DateTime.now(),
      updated = DateTime.now(),
      active = true,
      pwdHash = None,
      passwordDate = None,
      hashingStrategy = PasswordHashingStrategies.Legacy
    )
    assert(!PasswordUtils.passwordMatches(user, "password"))
  }
}
