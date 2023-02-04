package ru.yandex.vertis.promocoder.dao.impl.jvm

import org.junit.runner.RunWith

import ru.yandex.vertis.promocoder.dao.PromocodeAliasDaoSpec

/** Runnable specs on [[JvmPromocodeAliasDao]]
  *
  * @author alex-kovalenko
  */
class JvmPromocodeAliasDaoSpec extends PromocodeAliasDaoSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  val dao = new JvmPromocodeAliasDao
}
