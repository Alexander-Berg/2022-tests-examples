package ru.yandex.vertis.passport.dao.impl.memory

import ru.yandex.vertis.passport.dao.FullUserDaoSpec

/**
  * Tests for [[InMemoryFullUserDao]]
  *
  * @author zvez
  */
class InMemoryFullUserDaoSpec extends FullUserDaoSpec {

  override val userDao = new InMemoryFullUserDao
}
