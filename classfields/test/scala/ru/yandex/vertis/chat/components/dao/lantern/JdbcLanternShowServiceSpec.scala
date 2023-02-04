package ru.yandex.vertis.chat.components.dao.lantern

import ru.yandex.vertis.chat.service.impl.jdbc.JdbcSpec

class JdbcLanternShowServiceSpec extends LanternShowServiceSpec with JdbcSpec {

  override val lanternShowService: LanternShowService = {
    new JdbcLanternShowService(database)
  }

}
