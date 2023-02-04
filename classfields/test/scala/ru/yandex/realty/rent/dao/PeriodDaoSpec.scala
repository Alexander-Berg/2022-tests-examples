package ru.yandex.realty.rent.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.util.Page
import ru.yandex.realty.rent.model.RentContract
import ru.yandex.realty.rent.model.enums.{AggregatedMeterReadingsStatus, ContractStatus}
import ru.yandex.realty.rent.model.house.services.Period
import ru.yandex.realty.rent.proto.model.house.service.periods.{PaymentConfirmation, PeriodData}
import ru.yandex.realty.rent.proto.model.image.Image
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TimeUtils.RichDateTime
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class PeriodDaoSpec extends WordSpecLike with RentSpecBase with CleanSchemaBeforeEach {

  implicit val trace: Traced = Traced.empty

  "PeriodDao " should {
    "create and find period" in {
      val period = createPeriod()

      periodDao.create(Seq(period)).futureValue
      val foundedPeriod = periodDao.findById(period.periodId).futureValue
      val foundedPeriodOpt = foundedPeriod.find(_.periodId == period.periodId)
      assert(foundedPeriodOpt.isDefined)
      assert(period == foundedPeriodOpt.get)
    }

    "create and find period by flat" in {
      periodDao.create(createPeriod(rentContractGen(ContractStatus.Draft).next, 10)).futureValue
      val rentContract = rentContractGen(ContractStatus.Active).next
      val period = createPeriod(rentContract, 5)
      periodDao.create(period).futureValue
      val foundedPeriod = periodDao.findByFlatId(rentContract.flatId, Page(1, 3)).futureValue
      foundedPeriod.toSeq.exists(c => period.exists(_.periodId == c.periodId))
    }

    "create and find period by contractId" in {
      periodDao.create(createPeriod(rentContractGen(ContractStatus.Draft).next, 10)).futureValue
      val rentContract = rentContractGen(ContractStatus.Active).next
      val period = createPeriod(rentContract, 5)
      period.size shouldBe (5)
      periodDao.create(period).futureValue
      val foundedPeriod = periodDao.getByContractId(rentContract.contractId).futureValue
      period.forall(c => foundedPeriod.exists(_.periodId == c.periodId)) shouldBe (true)
    }

    "create and find period by contractId for period" in {
      periodDao.create(createPeriod(rentContractGen(ContractStatus.Draft).next, 10)).futureValue
      val rentContract = rentContractGen(ContractStatus.Active).next
      val period = createPeriod(rentContract, 5)
      period.size shouldBe (5)
      periodDao.create(period).futureValue
      val foundedPeriod =
        periodDao.findByContractIdForPeriod(rentContract.contractId, DateTimeUtil.now()).futureValue
      foundedPeriod.nonEmpty shouldBe true
    }

    "create and find period by flat for period" in {
      periodDao.create(createPeriod(rentContractGen(ContractStatus.Draft).next, 10)).futureValue
      val rentContract = rentContractGen(ContractStatus.Active).next
      val period = createPeriod(rentContract, 1).head
      periodDao.create(Seq(period)).futureValue
      val foundedPeriod = periodDao.findByFlatForPeriod(rentContract.flatId, DateTimeUtil.now()).futureValue
      assert(foundedPeriod.isDefined)
      assert(period == foundedPeriod.get)
    }

    "update and find updated meter readings" in {
      val periodToCreate = createPeriod()
      periodDao.create(Seq(periodToCreate)).futureValue
      val foundedPeriod = periodDao.findById(periodToCreate.periodId).futureValue
      val foundedPeriodOpt = foundedPeriod.find(_.periodId == periodToCreate.periodId)
      assert(foundedPeriodOpt.isDefined)
      assert(periodToCreate == foundedPeriodOpt.get)

      val updatedPeriod = periodToCreate.copy(
        data = PeriodData
          .newBuilder()
          .addPaymentConfirmation(
            PaymentConfirmation.newBuilder().addPhoto(Image.getDefaultInstance).setIsDeleted(false).build()
          )
          .build()
      )

      val period = periodDao
        .updateF(periodToCreate.periodId)(_ => Future.successful(updatedPeriod))
        .futureValue
      assert(period == updatedPeriod)

      val newUpdatedPeriod = updatedPeriod.copy(meterReadingsStatus = AggregatedMeterReadingsStatus.NotSent)
      val newPeriod = periodDao
        .update(updatedPeriod.periodId)(_ => newUpdatedPeriod)
        .futureValue
      assert(newPeriod == newUpdatedPeriod)

      val foundedUpdatedPeriod = periodDao.findById(newPeriod.periodId).futureValue
      assert(foundedUpdatedPeriod.isDefined)
      assert(foundedUpdatedPeriod.head == newUpdatedPeriod)
    }
  }

  def createPeriod(): Period = {
    val rentContract = rentContractGen(ContractStatus.Draft).next
    val period = DateTimeUtil.now().withFirstDayOfMonth
    rentContractDao.create(Seq(rentContract)).futureValue
    periodGen(rentContract.contractId, period = period).next
  }

  def createPeriod(rentContract: RentContract, num: Int): Seq[Period] = {
    rentContractDao.create(Seq(rentContract)).futureValue
    periodGen(rentContract.contractId).next(num).toSeq
  }
}
