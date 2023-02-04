package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.service.BlacklistUsernameService
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.concurrent.Future
import scala.annotation.nowarn

/**
  * @author neron
  */
class BlacklistUsernameServiceIntSpec extends SpecBase with IntegrationSpecTemplate with BeforeAndAfterEach {

  private val blacklist: BlacklistUsernameService = sharedBlacklistUsernameService

  private val deadlineIsNotOver = DateTime.now.plusHours(1)
  private val deadlineIsOver = DateTime.now.minusHours(1)

  import Threads.lightWeightTasksEc

  @nowarn
  override protected def beforeEach(): Unit = {
    super.beforeEach()
    blacklistUsernameDao.clear().futureValue
  }

  "BlacklistUsernameService" should {
    "add" in {
      val info = BlockUsernameInfoGen.next
      blacklist.addInfo(info).futureValue
      blacklist.all().futureValue shouldEqual Iterable(info)
    }

    "not add twice" in {
      val info = BlockUsernameInfoGen.next
      blacklist.addInfo(info).futureValue
      blacklist.addInfo(info).futureValue
      blacklist.all().futureValue shouldEqual Iterable(info)
    }

    "delete" in {
      val info = BlockUsernameInfoGen.next
      blacklist.addInfo(info).futureValue
      blacklist.delete(info.voxUsername).futureValue
      blacklist.all().futureValue shouldEqual Iterable.empty
    }

    "get none" in {
      val info = BlockUsernameInfoGen.next
      blacklist.addInfo(info).futureValue
      blacklist.delete(info.voxUsername).futureValue
      blacklist.get(info.voxUsername).futureValue shouldBe None
    }

    "all" in {
      val srs = 1.to(10).map(_ => BlockUsernameInfoGen.next)
      Future.sequence(srs.map(blacklist.addInfo)).futureValue
      val all = blacklist.all().futureValue
      all.toSet shouldEqual srs.toSet
    }

    "get some when deadline is not over" in {
      val info = BlockUsernameInfoGen.next.copy(deadline = deadlineIsNotOver)
      blacklist.addInfo(info).futureValue
      blacklist.get(info.voxUsername).futureValue shouldBe Some(info)
    }

    "get none when deadline is over" in {
      val info = BlockUsernameInfoGen.next.copy(deadline = deadlineIsOver)
      blacklist.addInfo(info).futureValue
      blacklist.get(info.voxUsername).futureValue shouldBe None
    }

  }

}
