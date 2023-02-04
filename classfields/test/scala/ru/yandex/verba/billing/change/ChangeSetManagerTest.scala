package ru.yandex.verba.billing.change

import org.scalatest.freespec.AnyFreeSpec
import ru.yandex.verba.billing.service.ChangeSetManager
import ru.yandex.verba.core.application.DBInitializer
import ru.yandex.verba.core.util.FutureUtils

import java.time.OffsetDateTime
import scala.concurrent.duration._

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 19.11.13 17:43
  */

class ChangeSetManagerTest extends AnyFreeSpec with FutureUtils {
  DBInitializer

  val changeSetManager = new ChangeSetManager()
  implicit val duration = 30.seconds

  "ChangeSetManager" - {
    "should collect ChangeSets" in {
      val changes = changeSetManager.collectChangeSets(OffsetDateTime.now().minusDays(1), OffsetDateTime.now()).await
      changes.foreach(println)

      changes.groupBy(_.userId).view.mapValues(_.size).foreach(println)
    }
  }
}
