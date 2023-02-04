package ru.yandex.vertis.passport.dao.impl.redis

import ru.yandex.vertis.passport.dao.{MarkerDao, MarkerDaoSpec}
import ru.yandex.vertis.passport.test.{RedisStartStopSupport, RedisSupport}

class RedisMarkerDaoSpec extends MarkerDaoSpec with RedisSupport with RedisStartStopSupport {
  override lazy val dao: MarkerDao = new RedisMarkerDao(createCache("marker-test"))
}
