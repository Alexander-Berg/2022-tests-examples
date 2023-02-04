package ru.yandex.realty.rent.chat.service.analytics.offerchat.status

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.db.mysql.api._
import ru.yandex.realty.ops.DaoMetrics
import ru.yandex.realty.rent.chat.{BaseDbSpec, CleanSchemaBeforeAll}
import ru.yandex.realty.rent.chat.model.RentOfferChatRoomState.Session.CategoryNamespace.Category

import java.time.Instant

@RunWith(classOf[JUnitRunner])
class OfferChatAnalyticsDaoSpec extends FlatSpec with Matchers with BaseDbSpec with CleanSchemaBeforeAll {

  behavior of classOf[MysqlOfferChatAnalyticsDao].getName

  private val dao = new MysqlOfferChatAnalyticsDao(masterSlaveDatabase, DaoMetrics.stub())

  private val roomId = "roomId"
  private val createdTime = Instant.ofEpochSecond(1)

  private def findRecord(): Option[OfferChatAnalyticsRecord] = {
    masterSlaveDatabase.master.db
      .run(OfferChatAnalyticsTable.Table.filter(_.roomId === roomId).result.headOption)
      .futureValue
  }

  it should "create record" in {
    findRecord() shouldBe None
    whenReady(dao.create(roomId, createdTime)) { _ =>
      val record = findRecord().get
      record.createdTime shouldBe createdTime
      record.category shouldBe "NEW"
      record.switchToOperatorTime shouldBe None
      record.hasMessagesFromUser shouldBe false
    }
  }

  it should "set category" in {
    whenReady(dao.setCategory(roomId, Category.CLIENT)) { _ =>
      val record = findRecord().get
      record.createdTime shouldBe createdTime
      record.category shouldBe "CLIENT"
      record.switchToOperatorTime shouldBe None
      record.hasMessagesFromUser shouldBe false
    }
  }

  it should "set switched-to-operator time" in {
    whenReady(dao.setSwitchToOperatorTime(roomId, createdTime.plusSeconds(1))) { _ =>
      val record = findRecord().get
      record.createdTime shouldBe createdTime
      record.category shouldBe "CLIENT"
      record.switchToOperatorTime.map(_.getEpochSecond) shouldBe Some(2L)
      record.hasMessagesFromUser shouldBe false
    }
  }

  it should "set has-messages-from-user flag" in {
    whenReady(dao.setHasMessagesFromUser(roomId)) { _ =>
      val record = findRecord().get
      record.createdTime shouldBe createdTime
      record.category shouldBe "CLIENT"
      record.switchToOperatorTime.map(_.getEpochSecond) shouldBe Some(2L)
      record.hasMessagesFromUser shouldBe true
    }
  }

  it should "reset record" in {
    findRecord().get.createdTime shouldBe createdTime
    whenReady(dao.create(roomId, createdTime.plusSeconds(1))) { _ =>
      val record = findRecord().get
      record.createdTime shouldBe createdTime.plusSeconds(1)
      record.category shouldBe "NEW"
      record.switchToOperatorTime shouldBe None
      record.hasMessagesFromUser shouldBe false
    }
  }

  it should "load records" in {
    whenReady(dao.loadAllRecords()) { records =>
      records shouldBe Seq(OfferChatAnalyticsRecord(roomId, createdTime.plusSeconds(1), "NEW", None, false))
    }
  }

}
