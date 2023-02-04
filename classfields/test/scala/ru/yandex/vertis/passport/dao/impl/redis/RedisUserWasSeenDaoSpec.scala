package ru.yandex.vertis.passport.dao.impl.redis

import ru.yandex.vertis.passport.dao.UserWasSeenDao
import ru.yandex.vertis.passport.service.session.UserWasSeenDaoSpec
import ru.yandex.vertis.passport.test.{RedisStartStopSupport, RedisSupport}

class RedisUserWasSeenDaoSpec extends UserWasSeenDaoSpec with RedisSupport with RedisStartStopSupport {
  override lazy val dao: UserWasSeenDao = new RedisUserWasSeenDao(createCache("user_was_seen-test"))
}
