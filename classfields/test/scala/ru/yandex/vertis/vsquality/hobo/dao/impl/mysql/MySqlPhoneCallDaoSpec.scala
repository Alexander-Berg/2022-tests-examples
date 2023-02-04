package ru.yandex.vertis.vsquality.hobo.dao.impl.mysql

import ru.yandex.vertis.vsquality.hobo.dao.{PhoneCallDao, PhoneCallDaoSpecBase}
import ru.yandex.vertis.vsquality.hobo.util.MySqlSpecBase

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Specs on [[MySqlPhoneCallDao]]
  *
  * @author semkagtn
  */

class MySqlPhoneCallDaoSpec extends PhoneCallDaoSpecBase with MySqlSpecBase {

  override val phoneCallDao: PhoneCallDao = new MySqlPhoneCallDao(database)
}
