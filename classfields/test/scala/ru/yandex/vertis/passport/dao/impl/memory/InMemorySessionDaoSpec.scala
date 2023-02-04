package ru.yandex.vertis.passport.dao.impl.memory

import ru.yandex.vertis.passport.dao.SessionDaoSpec

/**
  * Tests for [[InMemorySessionDao]]
  *
  * @author zvez
  */
class InMemorySessionDaoSpec extends SessionDaoSpec {
  val sessionDao = new InMemorySessionDao
}
