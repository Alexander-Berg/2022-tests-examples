package ru.yandex.vertis.telepony.dao

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.telepony.generator.Generator.{generateRedirectV2, CallMarkGen, HoboCallCheckTaskGen, PhoneGen, RefinedSourceGen}
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}
import ru.yandex.vertis.telepony.dao.HoboCallCheckTaskDao.{ByCallId, ByTaskId}

import scala.concurrent.duration._
import ru.yandex.vertis.telepony.model.{ActualRedirect, CallResults, CallV2, GeoId, OperatorAccounts, OperatorNumber, Operators, Phone, PhoneTypes, RefinedSource, Status}

/**
  * @author ponydev
  */
trait HoboCallCheckTaskDaoSpec
  extends SpecBase
  with DatabaseSpec
  with BeforeAndAfterEach
  with ScalaCheckDrivenPropertyChecks {

  def hoboCallCheckTaskDao: HoboCallCheckTaskDao

  def callDao: CallDaoV2

  def numberDao: OperatorNumberDaoV2

  def redirectDao: RedirectDaoV2

  private val Moscow: GeoId = 213

  private def forNumber(phone: Phone) = {
    OperatorNumber(phone, OperatorAccounts.MtsShared, Operators.Mts, Moscow, PhoneTypes.Local, Status.New(None), None)
  }

  private def sampleFor(id: String, from: RefinedSource, proxy: ActualRedirect) =
    sampleWithTimeFor(id, from, proxy, DateTime.now())

  private def sampleWithTimeFor(id: String, from: RefinedSource, proxy: ActualRedirect, dateTime: DateTime) =
    CallV2(
      id,
      dateTime,
      dateTime,
      id,
      Some(from),
      proxy.asHistoryRedirect,
      dateTime,
      20.seconds,
      15.seconds,
      hasRecord = true,
      CallResults.Unknown,
      fallbackCall = None,
      whitelistOwnerId = None
    )

  override protected def beforeEach(): Unit = {
    hoboCallCheckTaskDao.clear().futureValue
    callDao.clear().databaseValue.futureValue
    redirectDao.clear().databaseValue.futureValue
    numberDao.clear().databaseValue.futureValue
    super.beforeEach()
  }

  override protected def afterEach(): Unit = {
    hoboCallCheckTaskDao.clear().futureValue
    callDao.clear().databaseValue.futureValue
    redirectDao.clear().databaseValue.futureValue
    numberDao.clear().databaseValue.futureValue
    super.afterEach()
  }

  "HoboCallCheckTaskDao" should {
    "hobo call check task update resolution" in {
      forAll(PhoneGen, PhoneGen, RefinedSourceGen, HoboCallCheckTaskGen, CallMarkGen) {
        (operator, target, refinedSource, hoboCallCheckTask, newCallMark) =>
          val phone = forNumber(operator)
          numberDao.create(phone).databaseValue.futureValue
          val red = generateRedirectV2(phone, target).next
          redirectDao.create(red).databaseValue.futureValue
          val call = sampleFor(hoboCallCheckTask.callId, refinedSource, red)
          callDao.create(call).databaseValue.futureValue

          hoboCallCheckTaskDao.store(hoboCallCheckTask.callId, hoboCallCheckTask.taskId).futureValue
          val storedTask = hoboCallCheckTaskDao.get(ByTaskId(hoboCallCheckTask.taskId)).futureValue
          storedTask.head.resolution shouldBe None
          hoboCallCheckTaskDao.setResolution(hoboCallCheckTask.taskId, Some(newCallMark)).futureValue
          val updatedTask = hoboCallCheckTaskDao.get(ByCallId(hoboCallCheckTask.callId)).futureValue
          updatedTask.head.resolution shouldBe Some(newCallMark)
          hoboCallCheckTaskDao.countSinceDate(DateTime.now().plusMinutes(1)).futureValue shouldBe 0
          hoboCallCheckTaskDao.countSinceDate(DateTime.now().minusMinutes(1)).futureValue should be >= 1
      }
    }
  }

}
