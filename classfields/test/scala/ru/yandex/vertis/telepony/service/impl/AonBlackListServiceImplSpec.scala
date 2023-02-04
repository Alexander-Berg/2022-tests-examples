package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.generator.Generator.AonBlockInfoGen
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.service.AonBlacklistService
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.annotation.nowarn

class AonBlackListServiceImplSpec extends SpecBase with IntegrationSpecTemplate with BeforeAndAfterEach {

  val aonBlackList: AonBlacklistService = sharedAonBlacklistService

  private val now = DateTime.now
  private val updated = now.minusMinutes(30)
  private val deadlineIsNotOver = now.plusHours(1)
  private val deadlineIsOver = now.minusHours(1)

  @nowarn
  override protected def beforeEach(): Unit = {
    super.beforeEach()
    aonBlacklistDao.clear().futureValue
  }

  "AonBlacklistService" should {

    "add" in {
      val info = AonBlockInfoGen.next
      aonBlackList.add(info).futureValue
      aonBlackList.all().futureValue shouldEqual Iterable(info)
    }

    "not add twice" in {
      val info = AonBlockInfoGen.next
      aonBlackList.add(info).futureValue
      aonBlackList.add(info).futureValue
      aonBlackList.all().futureValue shouldEqual Iterable(info)
    }

    "delete" in {
      val info = AonBlockInfoGen.next
      aonBlackList.add(info).futureValue
      aonBlackList.delete(info.source).futureValue
      aonBlackList.get(info.source).futureValue shouldBe None
    }

    "get some when no deadline" in {
      val info = AonBlockInfoGen.next.copy(deadline = None)
      aonBlackList.add(info).futureValue
      aonBlackList.get(info.source).futureValue shouldBe Some(info)
    }

    "get some when deadline is not over" in {
      val info = AonBlockInfoGen.next.copy(deadline = Some(deadlineIsNotOver))
      aonBlackList.add(info).futureValue
      aonBlackList.get(info.source).futureValue shouldBe Some(info)
    }

    "get none when deadline is over" in {
      val info = AonBlockInfoGen.next.copy(deadline = Some(deadlineIsOver))
      aonBlackList.add(info).futureValue
      aonBlackList.get(info.source).futureValue shouldBe None
    }

    "get some when time is passed and deadline is not over" in {
      val info = AonBlockInfoGen.next.copy(deadline = Some(deadlineIsNotOver), updateTime = updated)
      aonBlackList.add(info).futureValue
      aonBlackList.getForTime(info.source, now).futureValue shouldBe Some(info)
    }

    "get none when time is passed and deadline is over" in {
      val info = AonBlockInfoGen.next.copy(deadline = Some(deadlineIsOver), updateTime = updated)
      aonBlackList.add(info).futureValue
      aonBlackList.getForTime(info.source, now).futureValue shouldBe None
    }

    "get none when time is passed and block info updated after time" in {
      val info = AonBlockInfoGen.next.copy(deadline = Some(deadlineIsNotOver), updateTime = now.plusMinutes(30))
      aonBlackList.add(info).futureValue
      aonBlackList.getForTime(info.source, now).futureValue shouldBe None
    }
  }

}
