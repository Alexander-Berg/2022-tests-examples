package ru.yandex.vertis.telepony.dao.yql

import akka.actor.{ActorSystem, Scheduler}
import com.typesafe.config.ConfigFactory
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.AonBlacklistUpdatesDao.AonBlacklistTableName
import ru.yandex.vertis.telepony.util.TestComponent
import scala.concurrent.ExecutionContext
import ru.yandex.vertis.telepony.generator.Generator.{AonChangeActionGen, DateTimeGen}
import ru.yandex.vertis.telepony.generator.Producer._

/**
  * @author tolmach
  */
class YqlAonBlacklistUpdatesDaoSpec extends SpecBase with TestComponent with YqlAonBlacklistDaoSpecBase {

  private lazy val config = ConfigFactory.parseResources("service.conf").resolve()
  lazy val actorSystem = ActorSystem(component.name, config)
  implicit val scheduler: Scheduler = actorSystem.scheduler
  implicit val ec: ExecutionContext = actorSystem.dispatcher

  lazy val dao = new TestOnlyYqlAonBlacklistUpdatesDao(yqlClient)

  private val date = DateTimeGen.next.withTimeAtStartOfDay()
  private val tableName = AonBlacklistTableName(date)

  override protected def afterAll(): Unit = {
    yqlClient.executeUpdate("drop", dropTableQuery(dao.fullTablePath(tableName))).futureValue
    super.afterAll()
  }

  "YqlAonBlacklistUpdatesDao" should {
    "fetch nothing for last blacklist table name" when {
      "yt is empty" in {
        val last = dao.lastBlacklistTableName.futureValue
        last shouldBe None
      }
    }
    "fill yt and fetch data" in {
      val actions = AonChangeActionGen.next(10).toSeq
      batchInsert(dao.fullTablePath(tableName), actions)

      val last = dao.lastBlacklistTableName.futureValue
      last shouldBe Some(tableName)

      dao
        .handleUpdatesBatched(tableName, YtBatchSize)(checkBatchEquality(actions)(_))
        .futureValue
    }
  }

}
