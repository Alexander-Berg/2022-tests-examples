package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.AntiFraud
import ru.yandex.vertis.telepony.service.WhitelistService
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.concurrent.Future

/**
  * @author neron
  */
class WhitelistServiceIntSpec extends SpecBase with IntegrationSpecTemplate with BeforeAndAfterEach {

  private val whitelist: WhitelistService = sharedWhitelistService

  private val deadlineIsNotOver = DateTime.now.plusHours(1)
  private val deadlineIsOver = DateTime.now.minusHours(1)

  import Threads.lightWeightTasksEc

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    whitelistDao.clear().futureValue
  }

  "WhitelistService" should {
    "add" in {
      val entry = WhitelistEntryGen.next
      whitelist.add(entry).futureValue
      whitelist.getAll.futureValue shouldEqual Iterable(entry)
    }

    "not add twice" in {
      val entry = WhitelistEntryGen.next
      whitelist.add(entry).futureValue
      whitelist.add(entry).futureValue
      whitelist.getAll.futureValue shouldEqual Iterable(entry)
    }

    "delete" in {
      val entry = WhitelistEntryGen.next
      whitelist.add(entry).futureValue
      whitelist.delete(entry.source).futureValue
      whitelist.getAll.futureValue shouldEqual Iterable.empty
    }

    "get some when deadline is not over" in {
      val entry = WhitelistEntryGen.next.copy(deadline = Some(deadlineIsNotOver))
      whitelist.add(entry).futureValue
      whitelist.get(entry.source).futureValue shouldBe Some(entry)
    }

    "get some when deadline is not set" in {
      val entry = WhitelistEntryGen.next.copy(deadline = None)
      whitelist.add(entry).futureValue
      whitelist.get(entry.source).futureValue shouldBe Some(entry)
    }

    "get none when deadline is over" in {
      val entry = WhitelistEntryGen.next.copy(deadline = Some(deadlineIsOver))
      whitelist.add(entry).futureValue
      whitelist.get(entry.source).futureValue should not be defined
    }

    "get none" in {
      val entry = WhitelistEntryGen.next
      whitelist.add(entry).futureValue
      whitelist.delete(entry.source).futureValue
      whitelist.get(entry.source).futureValue should not be defined
    }

    "all" in {
      val srs = WhitelistEntryGen.next(10)
      Future.sequence(srs.map(whitelist.add)).futureValue
      val all = whitelist.getAll.futureValue
      all.toSet shouldEqual srs.toSet
    }

    "return antiFraud disabled when source not in whitelist" in {
      val entry = WhitelistEntryGen.next
      whitelist.exists(entry.source).futureValue shouldBe false
    }

    "return antiFraud disabled when deadline is over" in {
      val entry = WhitelistEntryGen.next.copy(deadline = Some(deadlineIsOver))
      whitelist.add(entry).futureValue
      whitelist.exists(entry.source).futureValue shouldBe false
    }

    "return entry antiFraud when deadline is not over" in {
      val antiFraud = AntiFraudOptionSetGen.next
      val entry = WhitelistEntryGen.next.copy(deadline = Some(deadlineIsNotOver))
      whitelist.add(entry).futureValue
      whitelist.get(entry.source).futureValue shouldBe Some(entry)
      whitelist.exists(entry.source).futureValue shouldBe true
    }

    "return entry antiFraud when no deadline" in {
      val antiFraud = AntiFraudOptionSetGen.next
      val entry = WhitelistEntryGen.next.copy(deadline = None)
      whitelist.add(entry).futureValue
      whitelist.get(entry.source).futureValue shouldBe Some(entry)
      whitelist.exists(entry.source).futureValue shouldEqual true
    }
  }

}
