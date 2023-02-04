package ru.yandex.vertis.passport.dao.impl.redis

import ru.yandex.vertis.passport.dao.{ConfirmationDao, ConfirmationDaoSpec}
import ru.yandex.vertis.passport.test.{RedisStartStopSupport, RedisSupport}

class RedisConfirmationDaoSpec extends ConfirmationDaoSpec with RedisSupport with RedisStartStopSupport {
  override lazy val confirmationDao: ConfirmationDao = new RedisConfirmationDao(createCache("confirmation-test"))
}
