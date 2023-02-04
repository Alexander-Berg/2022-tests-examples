package ru.yandex.vertis.passport.dao.impl.memory

import ru.yandex.vertis.passport.dao.UserEssentialsCacheSpec

/**
  * Tests for [[InMemoryUserEssentialsCache]]
  *
  * @author zvez
  */
class InMemoryUserEssentialsCacheSpec extends UserEssentialsCacheSpec {
  val userDao = new InMemoryUserEssentialsCache
}
