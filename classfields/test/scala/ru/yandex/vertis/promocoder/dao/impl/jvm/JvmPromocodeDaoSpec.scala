package ru.yandex.vertis.promocoder.dao.impl.jvm

import org.junit.runner.RunWith

import ru.yandex.vertis.promocoder.dao.PromocodeDaoSpec

/** Runnable specs on [[JvmPromocodeDao]]
  *
  * @author alex-kovalenko
  */
class JvmPromocodeDaoSpec extends PromocodeDaoSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  val dao = new JvmPromocodeDao
}
