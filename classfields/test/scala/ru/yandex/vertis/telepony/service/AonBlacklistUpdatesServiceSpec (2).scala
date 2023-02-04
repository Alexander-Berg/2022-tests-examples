package ru.yandex.vertis.telepony.service

import akka.actor.{ActorSystem, Scheduler}
import com.typesafe.config.ConfigFactory
import org.scalatest.Ignore
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.AonBlacklistUpdatesDao.AonBlacklistTableName
import ru.yandex.vertis.telepony.dao.yql.{TestOnlyYqlAonBlacklistUpdatesDao, YqlAonBlacklistDaoSpecBase}
import ru.yandex.vertis.telepony.generator.Generator.{AonChangeActionGen, DateTimeGen}
import ru.yandex.vertis.telepony.service.impl.AonBlacklistUpdatesServiceImpl
import ru.yandex.vertis.telepony.util.TestComponent

import scala.concurrent.ExecutionContext
import ru.yandex.vertis.telepony.generator.Producer._

import scala.annotation.nowarn

/**
  * @author tolmach
  */
@Ignore
class AonBlacklistUpdatesServiceSpec extends SpecBase with TestComponent with YqlAonBlacklistDaoSpecBase {

  private lazy val config = ConfigFactory.parseResources("service.conf").resolve()
  lazy val actorSystem = ActorSystem(component.name, config)
  implicit val scheduler: Scheduler = actorSystem.scheduler
  implicit val ec: ExecutionContext = actorSystem.dispatcher

  private val date = DateTimeGen.next.withTimeAtStartOfDay()
  private val lastTable = AonBlacklistTableName(date)
  private val actions = AonChangeActionGen.next(10).toSeq
  private val previousTable = AonBlacklistTableName(date.minusDays(1))
  private val nextTable = AonBlacklistTableName(date.plusDays(1))

  @nowarn
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    batchInsert(dao.fullTablePath(lastTable), actions)
  }

  override protected def afterAll(): Unit = {
    yqlClient.executeUpdate("drop", dropTableQuery(dao.fullTablePath(lastTable))).futureValue
    yqlClient.executeUpdate("drop", dropTableQuery(dao.fullTablePath(previousTable))).futureValue
    super.afterAll()
  }

  lazy val dao = new TestOnlyYqlAonBlacklistUpdatesDao(yqlClient)
  lazy val service = new AonBlacklistUpdatesServiceImpl(dao)

  "AonBlacklistUpdatesService" should {
    "handle updates" when {
      "aon table hasn't been processed yet" in {
        service
          .handleUpdatesIfNeeded(None, YtBatchSize)(checkBatchEquality(actions)(_))
          .futureValue
          .shouldBe(Some(lastTable))
      }
      "the last processed table is older than the current one" in {
        service
          .handleUpdatesIfNeeded(Some(previousTable), YtBatchSize)(checkBatchEquality(actions)(_))
          .futureValue
          .shouldBe(Some(lastTable))
      }
      "the last processed table is equal to the current one" in {
        service
          .handleUpdatesIfNeeded(Some(lastTable), YtBatchSize)(checkBatchEquality(actions)(_))
          .futureValue
          .shouldBe(Some(lastTable))
      }
    }
    "fail handle updates" when {
      "the last processed table is younger than the current one" in {
        service
          .handleUpdatesIfNeeded(Some(nextTable), YtBatchSize)(checkBatchEquality(actions)(_))
          .failed
          .futureValue
          .shouldBe(an[IllegalArgumentException])
      }
    }
    "rehandle updates" when {
      "passed the name of an existing table" in {
        service
          .rehandleUpdates(lastTable, YtBatchSize)(checkBatchEquality(actions)(_))
          .futureValue
      }
    }
    "fail rehandle updates" when {
      "passed the name of an non-existing table" in {
        service
          .rehandleUpdates(previousTable, YtBatchSize)(checkBatchEquality(actions)(_))
          .failed
          .futureValue
          .shouldBe(an[Exception])
      }
    }
  }

}
