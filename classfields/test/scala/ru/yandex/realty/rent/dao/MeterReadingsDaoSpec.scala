package ru.yandex.realty.rent.dao

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.proto.model.image.Image
import ru.yandex.realty.rent.proto.model.house.service.periods.{
  MeterReadingsData,
  Information,
  MeterReadings => ProtoReadings
}
import ru.yandex.realty.rent.model.enums.{ContractStatus, MeterReadingsStatus}
import ru.yandex.realty.rent.model.house.services.MeterReadings
import ru.yandex.realty.sharding.Shard
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class MeterReadingsDaoSpec extends WordSpecLike with RentSpecBase with CleanSchemaBeforeEach {

  implicit val traced = Traced.empty

  "MeterReadingsDao" should {
    "create and find meter readings" in {
      val meterReadings = createMeterReadings()

      meterReadingsDao.create(Seq(meterReadings)).futureValue
      val foundedMeterReadings = meterReadingsDao.findById(meterReadings.meterReadingsId).futureValue
      val foundedMeterReadingsOpt = foundedMeterReadings.find(_.meterReadingsId == meterReadings.meterReadingsId)
      assert(foundedMeterReadingsOpt.isDefined)
      assert(meterReadings == foundedMeterReadingsOpt.get)
    }

    "create and find meter readings by periodId" in {
      val meterReadings = createMeterReadings()

      meterReadingsDao.create(Seq(meterReadings)).futureValue
      val foundedMeterReadings = meterReadingsDao.findByPeriodId(meterReadings.periodId).futureValue
      val foundedMeterReadingsOpt = foundedMeterReadings.find(_.meterReadingsId == meterReadings.meterReadingsId)
      assert(foundedMeterReadingsOpt.isDefined)
      assert(meterReadings == foundedMeterReadingsOpt.get)
    }

    "watch" in {
      val now = DateTimeUtil.now()
      val meterReadings = createMeterReadings()
        .copy(status = MeterReadingsStatus.NotSent, visitTime = Some(now), shardKey = 0)
      val periodId = meterReadings.periodId
      meterReadingsDao
        .create(
          Seq(
            meterReadings,
            createMeterReadings(periodId)
              .copy(status = MeterReadingsStatus.ShouldBeSent, visitTime = Some(now), shardKey = 0),
            createMeterReadings(periodId)
              .copy(status = MeterReadingsStatus.Sending, visitTime = Some(now), shardKey = 0)
          )
        )
        .futureValue
      meterReadingsDao.watch(10, Shard(0, 1))(metric => Future.successful(metric))
    }

    "update and find updated meter readings" in {
      val meterReadingsToCreate = createMeterReadings()

      meterReadingsDao.create(Seq(meterReadingsToCreate)).futureValue
      val foundedMeterReadings = meterReadingsDao.findById(meterReadingsToCreate.meterReadingsId).futureValue
      val foundedMeterReadingsOpt =
        foundedMeterReadings.find(_.meterReadingsId == meterReadingsToCreate.meterReadingsId)
      assert(foundedMeterReadingsOpt.isDefined)
      assert(meterReadingsToCreate == foundedMeterReadingsOpt.get)

      val updatedMeterReadings = meterReadingsToCreate.copy(
        data = MeterReadingsData
          .newBuilder()
          .setInformation(
            Information
              .newBuilder()
              .addMeterReadings(
                ProtoReadings
                  .newBuilder()
                  .setMeterValue(999.9d)
                  .setMeterPhoto(Image.getDefaultInstance)
                  .build()
              )
              .build()
          )
          .build()
      )

      val meterReadings = meterReadingsDao
        .updateF(meterReadingsToCreate.meterReadingsId)(_ => Future.successful(updatedMeterReadings))
        .futureValue
      assert(meterReadings == updatedMeterReadings)

      val newUpdatedMeterReadings = updatedMeterReadings.copy(status = MeterReadingsStatus.Sending)
      val newMeterReadings = meterReadingsDao
        .update(updatedMeterReadings.meterReadingsId)(_ => newUpdatedMeterReadings)
        .futureValue
      assert(newMeterReadings == newUpdatedMeterReadings)

      val findUpdatedMeterReadings = meterReadingsDao.findById(newMeterReadings.meterReadingsId).futureValue
      assert(findUpdatedMeterReadings.isDefined)
      assert(findUpdatedMeterReadings.head == newUpdatedMeterReadings)
    }
  }

  def createMeterReadings(): MeterReadings = {
    val rentContract = rentContractGen(ContractStatus.Draft).next
    rentContractDao.create(Seq(rentContract)).futureValue
    val period = periodGen(rentContract.contractId).next
    periodDao.create(Seq(period)).futureValue
    createMeterReadings(period.periodId)
  }

  def createMeterReadings(periodId: String): MeterReadings = {
    val ownerRequest = ownerRequestGen.next
    ownerRequestDao.create(Seq(ownerRequest)).futureValue
    val houseService = houseServiceGen(ownerRequest.ownerRequestId).next
    houseServiceDao.create(Seq(houseService)).futureValue
    meterReadingsGen(houseService.houseServiceId, periodId).next
  }
}
