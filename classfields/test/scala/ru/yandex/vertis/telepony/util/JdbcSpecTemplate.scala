package ru.yandex.vertis.telepony.util

import org.scalatest.Suite
import ru.yandex.vertis.telepony.util.db.{DualDatabase, PlainDualDatabase}

/**
  * Provides database for tests runs
  *
  * @author dimas
  */
trait JdbcSpecTemplate extends SharedDbSupport {
  this: Suite =>

  //should be lazy due strange immediate instantiation of all test in maven-surefire-plugin
  def database: PlainDualDatabase = sharedDatabase

  def dualDb: DualDatabase = sharedDualDb

}
