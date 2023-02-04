package ru.yandex.vertis.billing.dao

import billing.common.testkit.zio.ZIOSpecBase
import org.scalatest.{BeforeAndAfterEach, TryValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.CallCenterCallDao._
import ru.yandex.vertis.billing.model_core.gens.{CallCenterCallGen, Producer}
import ru.yandex.vertis.billing.util.DateTimeInterval
import ru.yandex.vertis.billing.util.clean.CleanableDao

trait CallCenterCallDaoSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with TryValues with ZIOSpecBase {

  protected def callCenterDao: CallCenterCallDao with CleanableDao

  override def beforeEach(): Unit = {
    callCenterDao.clean().get
    super.beforeEach()
  }

  "CallCenterCallDao" should {
    "upsert and get call" in {
      val callsCount = 2
      val calls = CallCenterCallGen.next(callsCount)
      callCenterDao.upsert(calls).unsafeRun()

      val actualCallsByAllFilter = callCenterDao.getTry(All).get
      actualCallsByAllFilter should contain theSameElementsAs calls

      val callIds = calls.map(_.id).toNelUnsafe.toNes
      val actualCallsByIdsFilter = callCenterDao.get(ByIds(callIds)).unsafeRun()
      actualCallsByIdsFilter should contain theSameElementsAs calls
    }
    "correctly mark as matched and get" in {
      val callsCount = 4
      val calls = CallCenterCallGen.next(callsCount)
      callCenterDao.upsert(calls).unsafeRun()

      val markAsMatched = calls.take(callsCount / 2).map(_.id).toNelUnsafe.toNes
      val expectedNotMatched = calls.drop(callsCount / 2)

      val patch = MarkAsMatched(markAsMatched)
      callCenterDao.update(patch).get

      val actualNotMatched = callCenterDao.getTry(NotMatched).get
      actualNotMatched should contain theSameElementsAs expectedNotMatched
    }

    "correctly get call by specified filter" in {
      val callsCount = 50
      val calls = CallCenterCallGen.next(callsCount)
      callCenterDao.upsert(calls).unsafeRun()

      val expectedAsMatched = calls.take(callsCount / 2)
      val expectedNotMatched = calls.drop(callsCount / 2)

      val patch = MarkAsMatched(expectedAsMatched.map(_.id).toNelUnsafe.toNes)
      callCenterDao.update(patch).success.value

      val matched = callCenterDao.getTry(ForInterval(DateTimeInterval.currentDay, isMatched = true)).get
      val notMatched = callCenterDao.getTry(ForInterval(DateTimeInterval.currentDay, isMatched = false)).get

      matched should contain theSameElementsAs expectedAsMatched
      notMatched should contain theSameElementsAs expectedNotMatched
    }
  }

}
