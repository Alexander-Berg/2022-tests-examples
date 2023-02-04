package ru.yandex.vertis.vsquality.hobo.service.impl.mysql

import ru.yandex.vertis.vsquality.hobo.dao.PhoneCallDao
import ru.yandex.vertis.vsquality.hobo.dao.impl.mysql.MySqlPhoneCallDao
import ru.yandex.vertis.vsquality.hobo.service.PhoneCallServiceImplSpecBase
import ru.yandex.vertis.vsquality.hobo.service.impl.PhoneCallServiceImpl
import ru.yandex.vertis.vsquality.hobo.util.MySqlSpecBase

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Specs on [[PhoneCallServiceImpl]]
  *
  * @author mpoplavkov
  */

class MySqlPhoneCallServiceImplSpec extends PhoneCallServiceImplSpecBase with MySqlSpecBase {

  override def phoneCallDaoImpl: PhoneCallDao = new MySqlPhoneCallDao(database)
}
