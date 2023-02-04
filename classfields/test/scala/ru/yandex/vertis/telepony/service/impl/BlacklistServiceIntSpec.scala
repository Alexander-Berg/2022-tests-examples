package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.service.BlacklistService
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.concurrent.Future

/**
  * @author neron
  */
class BlacklistServiceIntSpec extends SpecBase with IntegrationSpecTemplate with BeforeAndAfterEach {

  private val blacklist: BlacklistService = sharedBlacklistService

  private val deadlineIsNotOver = DateTime.now.plusHours(1)
  private val deadlineIsOver = DateTime.now.minusHours(1)

  import Threads.lightWeightTasksEc

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reasonBlacklistDao.clear().futureValue
  }

  "BlacklistService" should {
    "add" in {
      val info = BlockInfoGen.next
      blacklist.add(info).futureValue
      blacklist.all().futureValue shouldEqual Iterable(info)
    }

    "not add twice" in {
      val info = BlockInfoGen.next
      blacklist.add(info).futureValue
      blacklist.add(info).futureValue
      blacklist.all().futureValue shouldEqual Iterable(info)
    }

    "list outdated in all" in {
      val info = BlockInfoGen.next.copy(deadline = None)
      blacklist.add(info).futureValue
      blacklist.delete(info.source).futureValue
      val res = blacklist.all().futureValue.toSet
      res.map(_.copy(deadline = None)) shouldEqual Set(info)
      res.foreach { resInfo =>
        resInfo.deadline should not be empty
      }
    }

    "not list outdated in list" in {
      val info = BlockInfoGen.next.copy(deadline = None)
      blacklist.add(info).futureValue
      blacklist.delete(info.source).futureValue
      blacklist.getList(info.source).futureValue shouldBe empty
    }

    "all" in {
      val srs = 1.to(10).map(_ => BlockInfoGen.next)
      Future.sequence(srs.map(blacklist.add)).futureValue
      val all = blacklist.all().futureValue
      all.toSet shouldEqual srs.toSet
    }

    "get some when deadline is not over" in {
      val info = BlockInfoGen.next.copy(deadline = Some(deadlineIsNotOver))
      blacklist.add(info).futureValue
      blacklist.getList(info.source).futureValue should contain only info
    }

    "get some when no deadline" in {
      val info = BlockInfoGen.next.copy(deadline = None)
      blacklist.add(info).futureValue
      blacklist.getList(info.source).futureValue should contain only info
    }

    "get none when deadline is over" in {
      val info = BlockInfoGen.next.copy(deadline = Some(deadlineIsOver))
      blacklist.add(info).futureValue
      blacklist.getList(info.source).futureValue shouldBe empty
    }

  }

}
