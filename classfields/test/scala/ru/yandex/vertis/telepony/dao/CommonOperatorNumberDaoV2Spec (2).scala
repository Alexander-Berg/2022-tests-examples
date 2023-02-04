package ru.yandex.vertis.telepony.dao

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.generator.Generator.OperatorNumberGen
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.{NumbersCounter, OperatorAccounts, PhoneTypes, Status, StatusValues, TypedDomains}
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}

trait CommonOperatorNumberDaoV2Spec extends SpecBase with BeforeAndAfterEach with DatabaseSpec {

  def dao: CommonOperatorNumberDao

  "CommonOperatorNumberDao" should {
    "count free numbers by domains and operators" in {
      val geoId = 1

      val newStatus = Status(StatusValues.New, None)
      val readyStatus = Status(StatusValues.Ready, None)
      val garbageStatus = Status(StatusValues.Garbage, None)

      // suitable numbers
      val mtsNumbersBillingRealty =
        OperatorNumberGen
          .next(2)
          .map(
            _.copy(
              phoneType = PhoneTypes.Local,
              geoId = geoId,
              account = OperatorAccounts.MtsShared,
              status = newStatus
            )
          )
      val mtsNumbersAutoDealers =
        OperatorNumberGen
          .next(3)
          .map(
            _.copy(
              phoneType = PhoneTypes.Local,
              geoId = geoId,
              account = OperatorAccounts.MtsShared,
              status = readyStatus
            )
          )
      val mttNumbersBillingRealty =
        OperatorNumberGen
          .next(2)
          .map(
            _.copy(
              phoneType = PhoneTypes.Local,
              geoId = geoId,
              account = OperatorAccounts.MttShared,
              status = readyStatus
            )
          )

      // unsuitable numbers
      val mtsNumbersBillingRealtyGarbage =
        OperatorNumberGen
          .next(2)
          .map(
            _.copy(
              phoneType = PhoneTypes.Local,
              geoId = geoId,
              account = OperatorAccounts.MtsShared,
              status = garbageStatus
            )
          )
      val mtsNumbersBillingRealtyMobile =
        OperatorNumberGen
          .next(2)
          .map(
            _.copy(
              phoneType = PhoneTypes.Mobile,
              geoId = geoId,
              account = OperatorAccounts.MtsShared,
              status = readyStatus
            )
          )
      val mttNumbersGeneral =
        OperatorNumberGen
          .next(3)
          .map(
            _.copy(
              phoneType = PhoneTypes.Local,
              geoId = geoId,
              account = OperatorAccounts.MttShared,
              status = readyStatus
            )
          )
      val voxNumbersAutoDealers =
        OperatorNumberGen
          .next(4)
          .map(
            _.copy(
              phoneType = PhoneTypes.Local,
              geoId = geoId,
              account = OperatorAccounts.VoxShared,
              status = readyStatus
            )
          )

      val createNumbers =
        mtsNumbersBillingRealty.map(n => dao.createInDomain(n, TypedDomains.billing_realty)) ++
          mtsNumbersBillingRealtyGarbage.map(n => dao.createInDomain(n, TypedDomains.billing_realty)) ++
          mtsNumbersBillingRealtyMobile.map(n => dao.createInDomain(n, TypedDomains.billing_realty)) ++
          mtsNumbersAutoDealers.map(n => dao.createInDomain(n, TypedDomains.`auto-dealers`)) ++
          mttNumbersBillingRealty.map(n => dao.createInDomain(n, TypedDomains.billing_realty)) ++
          mttNumbersGeneral.map(n => dao.createInDomain(n, TypedDomains.ya_general)) ++
          voxNumbersAutoDealers.map(n => dao.createInDomain(n, TypedDomains.`auto-dealers`))

      createNumbers.foreach(_.databaseValue.futureValue)

      val expected = Seq(
        NumbersCounter(
          geoId = geoId,
          phoneType = PhoneTypes.Local,
          account = OperatorAccounts.MtsShared,
          domain = TypedDomains.billing_realty,
          status = newStatus.value,
          2
        ),
        NumbersCounter(
          geoId = geoId,
          phoneType = PhoneTypes.Local,
          account = OperatorAccounts.MttShared,
          domain = TypedDomains.billing_realty,
          status = readyStatus.value,
          2
        ),
        NumbersCounter(
          geoId = geoId,
          phoneType = PhoneTypes.Local,
          account = OperatorAccounts.MtsShared,
          domain = TypedDomains.`auto-dealers`,
          status = readyStatus.value,
          3
        )
      )

      val result = dao
        .getNumbersCounters(
          sourceDomains = Seq(TypedDomains.billing_realty, TypedDomains.`auto-dealers`),
          phoneType = PhoneTypes.Local,
          accounts = Seq(OperatorAccounts.MtsShared, OperatorAccounts.MttShared),
          geoId = geoId,
          statuses = Seq(newStatus.value, readyStatus.value)
        )
        .databaseValue
        .futureValue

      result.diff(expected) shouldEqual Seq.empty
    }
  }
}
