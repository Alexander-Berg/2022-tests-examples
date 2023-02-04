package ru.yandex.vertis.promocoder.dao.impl.jvm

import org.joda.time.DateTime
import ru.yandex.vertis.promocoder.dao.{CleanableJvmFeatureInstanceDao, FeatureInstanceDaoSpec}
import ru.yandex.vertis.promocoder.util.TimeService

/** Runnable specs on [[JvmFeatureInstanceDao]]
  *
  * @author alex-kovalenko
  */
class JvmFeatureInstanceDaoSpec extends FeatureInstanceDaoSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  val timeService = new TimeService {
    override def now(): DateTime = StableNow
  }

  val dao = new JvmFeatureInstanceDao(timeService) with CleanableJvmFeatureInstanceDao

}
