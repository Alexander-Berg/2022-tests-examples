package ru.yandex.verba.billing.work

import org.scalatest.Ignore
import org.scalatest.freespec.AnyFreeSpec
import ru.yandex.verba.billing.service.{ChangeSetManager, WorkManager}
import ru.yandex.verba.billing.storage.{BillingMetaStorage, BillingServiceStorage}
import ru.yandex.verba.core._
import ru.yandex.verba.core.application._
import ru.yandex.verba.core.util.FutureUtils
import ru.yandex.verba.sec.user

import java.time.{OffsetDateTime, ZoneOffset}
import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 26.11.13 14:45
 */
@Ignore
class WorkManagerTest extends AnyFreeSpec with FutureUtils {

  DBInitializer

  val end = OffsetDateTime.of(2013, 11, 25, 0, 0, 0, 0, ZoneOffset.ofHours(3))
  val start = OffsetDateTime.of(2013, 10, 24, 0, 0, 0, 0, ZoneOffset.ofHours(3))

  val changeSetManager = new ChangeSetManager()
  implicit val duration = 60.seconds
  implicit val ec = system.dispatcher

  "WorkManager" - {
    "should do work" in {
      val userStorage = user.storage
      val metaStorage = BillingMetaStorage.ref

      val workManager = new WorkManager(
        userStorage,
        storage.entity.ref,
        storage.attributes.ref,
        metaStorage,
        changeSetManager,
        BillingServiceStorage.ref
      )

      val from = start
      val to = end

      val changes = changeSetManager.collectChangeSets(from, to).await


      val work = for {
        bss <- BillingServiceStorage.ref.getBillingServices
        res <- workManager.calculateWork(from, changes, bss)
      } yield res

      work.await.foreach(println)
    }
  }

}
