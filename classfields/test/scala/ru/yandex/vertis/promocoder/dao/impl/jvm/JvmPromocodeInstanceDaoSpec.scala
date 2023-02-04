package ru.yandex.vertis.promocoder.dao.impl.jvm

import org.junit.runner.RunWith

import ru.yandex.vertis.promocoder.dao.{CleanableJvmPromocodeInstanceDso, PromocodeInstanceDaoSpec}

/** Runnable specs on [[JvmPromocodeInstanceDao]]
  *
  * @author alex-kovalenko
  */
class JvmPromocodeInstanceDaoSpec extends PromocodeInstanceDaoSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  val promocodeDao = new JvmPromocodeDao

  val dao = new JvmPromocodeInstanceDao(promocodeDao) with CleanableJvmPromocodeInstanceDso

}
