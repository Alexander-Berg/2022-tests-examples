package ru.yandex.vertis.passport.dao.impl.redis

import ru.yandex.vertis.passport.dao.{SessionDao, SessionDaoSpec}
import ru.yandex.vertis.passport.test.{RedisStartStopSupport, RedisSupport}

class RedisSessionDaoSpec extends SessionDaoSpec with RedisSupport with RedisStartStopSupport {

  override lazy val sessionDao: SessionDao = new RedisSessionDao(createCache("session-test"))
}
