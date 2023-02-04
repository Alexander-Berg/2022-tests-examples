package ru.yandex.vertis.telepony.dao.jdbc

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.ReasonBlacklistDao
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.{AntiFraudOptions, BlockReasons}
import ru.yandex.vertis.telepony.util.{SharedDbSupport, Threads}

import scala.concurrent.Future

/**
  * @author neron
  */
class JdbcReasonBlacklistDaoIntSpec extends SpecBase with BeforeAndAfterEach with SharedDbSupport {

  val dao: ReasonBlacklistDao = new JdbcReasonBlacklistDao(sharedDualDb)

  import Threads.lightWeightTasksEc

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dao.clear().futureValue
  }

  "ReasonBlacklistDao" should {
    "upsert first time" in {
      val info = BlockInfoGen.next
      dao.upsert(info).futureValue
      val foundBs = dao.getList(info.source).futureValue
      foundBs should contain only info
    }

    "not add" when {
      "upsert second time" in {
        val info = BlockInfoGen.next
        dao.upsert(info).futureValue
        dao.upsert(info).futureValue
        val foundBs = dao.getList(info.source).futureValue
        foundBs should contain only info
        dao.all.futureValue should have size 1
      }
    }
    "update reason" when {
      "upsert" in {
        val source = RefinedSourceGen.next
        val infoOld = BlockInfoFixedSourceGen(source).next.copy(reason = BlockReasons.Scam)
        val infoNew = infoOld.copy(reason = BlockReasons.Debt)
        dao.upsert(infoOld).futureValue
        dao.upsert(infoNew).futureValue
        val foundBs = dao.getList(infoNew.source).futureValue
        foundBs should contain only infoNew
        dao.all.futureValue should have size 1
      }
    }

    "return all" in {
      val bss = 1.to(10).map(_ => BlockInfoGen.next)
      Future.sequence(bss.map(dao.upsert)).futureValue
      dao.all.futureValue.toSet shouldBe bss.toSet
    }

    "return ban list for source" in {
      val source = RefinedSourceGen.next
      val gen = BlockInfoFixedSourceGen(source)
      val bss = Seq(
        gen.next.copy(antiFraud = AntiFraudOptions.Blacklist),
        gen.next.copy(antiFraud = AntiFraudOptions.AonBlacklist),
        gen.next.copy(antiFraud = AntiFraudOptions.CallsCounter)
      )
      Future.sequence(bss.map(dao.upsert)).futureValue
      val source2 = RefinedSourceGen.suchThat(_ != source).next
      val anotherSourceBan = BlockInfoFixedSourceGen(source2).next
      dao.upsert(anotherSourceBan).futureValue
      val res = dao.getList(source).futureValue.toSet
      res shouldBe bss.toSet
    }

    "not mark outdated inactive ban" in {
      val source = RefinedSourceGen.next
      val now = DateTime.now()
      val beforeNow = now.minusDays(1)
      val outdatedDeadline =
        BlockInfoFixedSourceGen(source).next.copy(deadline = Some(beforeNow), antiFraud = AntiFraudOptions.Blacklist)
      dao.upsert(outdatedDeadline).futureValue
      val res = dao.markOutdated(source).futureValue
      res shouldBe false
      val all = dao.all.futureValue.toSet
      all.size shouldBe 1
      all.head shouldBe outdatedDeadline
    }

    "mark outdated active ban without deadline" in {
      val source = RefinedSourceGen.next
      val actual =
        BlockInfoFixedSourceGen(source).next.copy(deadline = None, antiFraud = AntiFraudOptions.Blacklist)
      dao.upsert(actual).futureValue
      val res = dao.markOutdated(source).futureValue
      res shouldBe true
      val all = dao.all.futureValue.toSet
      all.size shouldBe 1
      all.head.copy(deadline = None) shouldBe actual
      all.head.deadline should not be empty
    }

    "mark outdated active ban with deadline" in {
      val source = RefinedSourceGen.next
      val now = DateTime.now()
      val afterNow = now.plusDays(1)
      val actual =
        BlockInfoFixedSourceGen(source).next.copy(deadline = Some(afterNow), antiFraud = AntiFraudOptions.Blacklist)
      dao.upsert(actual).futureValue
      val res = dao.markOutdated(source).futureValue
      res shouldBe true
      val all = dao.all.futureValue.toSet
      all.size shouldBe 1
      all.head.copy(deadline = None) shouldBe actual.copy(deadline = None)
      all.head.deadline should not be empty
      all.head.deadline.get.isBefore(afterNow)
    }

    "mark outdated only active ban and skip others" in {
      val source = RefinedSourceGen.next
      val now = DateTime.now()
      val beforeNow = now.minusDays(1)
      val afterNow = now.plusDays(1)
      val withDeadline =
        BlockInfoFixedSourceGen(source).next.copy(deadline = Some(afterNow), antiFraud = AntiFraudOptions.Blacklist)
      val outdated =
        BlockInfoFixedSourceGen(source).next.copy(deadline = Some(beforeNow), antiFraud = AntiFraudOptions.AonBlacklist)
      val withoutDeadline =
        BlockInfoFixedSourceGen(source).next.copy(deadline = None, antiFraud = AntiFraudOptions.CallsCounter)
      Future.sequence(Set(withDeadline, outdated, withoutDeadline).map(dao.upsert)).futureValue
      val res = dao.markOutdated(source).futureValue
      res shouldBe true
      val all = dao.all.futureValue.toSet
      all.size shouldBe 3
      all should contain(outdated)
      all.map(_.copy(deadline = None)) should contain theSameElementsAs Seq(outdated, withDeadline, withoutDeadline)
        .map(_.copy(deadline = None))
    }
  }
}
