package ru.yandex.realty.rent.dao

import org.joda.time.LocalDate
import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.model.util.Page
import ru.yandex.realty.rent.dao.actions.impl.{
  FlatDbActionsImpl,
  FlatQuestionnaireDbActionsImpl,
  FlatShowingDbActionsImpl,
  KeysHandoverDbActionsImpl,
  PaymentDbActionsImpl,
  PaymentHistoryDbActionsImpl,
  PeriodDbActionsImpl,
  RentContractDbActionsImpl,
  UserDbActionsImpl
}
import ru.yandex.realty.rent.dao.impl.{FlatDaoImpl, PaymentDaoImpl, RentContractDaoImpl}
import ru.yandex.realty.rent.model.enums.OwnerRequestStatus.OwnerRequestStatus
import ru.yandex.realty.rent.model.enums.{ContractStatus, OwnerRequestStatus}
import ru.yandex.realty.rent.model.{ContractWithPayments, Flat, Payment, RentContract}
import ru.yandex.realty.rent.proto.model.contract.ContractData
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.protobuf.BasicProtoFormats.DateTimeFormat.{write => toGoogleTimestamp}

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FlatDaoSpec extends WordSpecLike with RentSpecBase with CleanSchemaBeforeEach {

  implicit val trace: Traced = Traced.empty

  "FlatDao.findByKeyCode" should {
    "find by flat code" in new Wiring with DataKeyCode {
      val result = flatDao.findByKeyCode("A00000000001").futureValue.map(_.flatId)
      result shouldEqual Some("flat-keycode-1")
    }
  }

  "FlatDao.findByIds" should {
    "return flats which are sorted by create date desc" in new Wiring with Data {
      val result: Seq[LocalDate] = flatDao
        .findByIds(allFlats.map(_.flatId).toSet)
        .futureValue
        .map(_.createTime.toLocalDate)

      result.zip(result.tail).forall(e => e._1.isAfter(e._2)) shouldEqual true
    }
  }

  "FlatDao.findByUnifiedAddress" should {
    "return successful result of searching" in new Wiring with DataForSearching {

      cases foreach {
        case c @ TestCase(samplePrefix, sampleStates, sampleLimit, expectedResult) =>
          log.info(s"Test case [$c]")
          val result: Seq[Flat] = flatDao.findByUnifiedAddress(samplePrefix, sampleStates, sampleLimit).futureValue
          result.map(_.flatId) should contain theSameElementsAs expectedResult
      }
    }
  }

  "RentContractDao.findContractsWithPaymentsByFlatIds" should {
    "return correct list" in new Wiring with Data {
      val expectedResult: Seq[ContractWithPayments] =
        contracts.toSeq.map(c => ContractWithPayments(c, payments.filter(_.contractId == c.contractId).toList))
      val contractsWithPayments: Iterable[ContractWithPayments] =
        rentContractDao.findContractsWithPaymentsByFlatIds(flatIds).futureValue

      contractsWithPayments should contain theSameElementsAs expectedResult
    }
  }

  trait Wiring {
    val features = new SimpleFeatures

    val flatDbActions = new FlatDbActionsImpl()
    val contractDbActions = new RentContractDbActionsImpl()
    val paymentDbActions = new PaymentDbActionsImpl()
    val paymentHistoryDbActions = new PaymentHistoryDbActionsImpl()
    val periodDbActions = new PeriodDbActionsImpl()
    val userDbActions = new UserDbActionsImpl()
    val flatShowingDbActions = new FlatShowingDbActionsImpl()
    val keysHandoverDbActions = new KeysHandoverDbActionsImpl()
    val flatQuestionnaireDbActions = new FlatQuestionnaireDbActionsImpl()

    val flatDao = new FlatDaoImpl(
      flatDbActions,
      contractDbActions,
      paymentDbActions,
      ownerRequestDbActions,
      periodDbActions,
      meterReadingsDbActions,
      houseServiceDbActions,
      flatShowingDbActions,
      keysHandoverDbActions,
      flatQuestionnaireDbActions,
      masterSlaveDb2,
      daoMetrics
    )

    val rentContractDao =
      new RentContractDaoImpl(
        contractDbActions,
        paymentDbActions,
        periodDbActions,
        userDbActions,
        flatShowingDbActions,
        masterSlaveDb2,
        daoMetrics
      )
    val paymentDao = new PaymentDaoImpl(paymentDbActions, masterSlaveDb2, daoMetrics)
  }

  trait Data {
    this: Wiring =>

    val statuses: Map[Option[Boolean], Set[OwnerRequestStatus]] = Map.empty
    val filter: FlatFilter = FlatFilter(None, statuses, Page(0, 20))

    val FlatsCount: Int = 10
    val FlatsWithContactCount: Int = FlatsCount / 2

    val (flatsWithContract, flatsWithoutConracts) =
      flatGen()
        .next(FlatsCount)
        .zipWithIndex
        .map {
          case (flat, i) => flat.copy(flatId = s"flat-$i", createTime = flat.createTime.plusDays(i))
        }
        .toSeq
        .splitAt(FlatsWithContactCount)

    val allFlats: Seq[Flat] = flatsWithContract ++ flatsWithoutConracts

    val contracts: Iterable[RentContract] = flatsWithContract
      .zip(rentContractGen(ContractStatus.Active).next(FlatsWithContactCount))
      .zipWithIndex
      .map {
        case ((flat, contract), i) =>
          val data = if (i % 2 == 0) {
            ContractData.getDefaultInstance.toBuilder
              .setRentStartDate(toGoogleTimestamp(contract.createTime.plusMonths(i)))
              .build()
          } else {
            ContractData.getDefaultInstance.toBuilder.clearRentStartDate().build()
          }

          contract
            .copy(
              contractId = s"${flat.flatId}-contract",
              flatId = flat.flatId,
              data = data
            )
      }

    val flatIds: Iterable[String] = contracts.map(_.flatId)

    val payments: Seq[Payment] =
      paymentGen(paymentType = None, paymentStatus = None).next(contracts.size).toSeq.zip(contracts).map {
        case (p, c) => p.copy(contractId = c.contractId)
      }

    rentContractDao.create(contracts).futureValue
    Future.sequence(allFlats.map(flatDao.create)).futureValue
    Future.sequence(payments.map(paymentDao.insert)).futureValue
  }

  trait DataKeyCode {
    this: Wiring =>

    val flatsWithKeyCode = Seq {
      val flat = flatGen().next
      flat.copy(flatId = "flat-keycode-1", keyCode = Some("A00000000001"))
    }

    Future.sequence(flatsWithKeyCode.map(flatDao.create)).futureValue
  }

  trait DataForSearching {
    this: Wiring =>

    val sampleFlats: Seq[Flat] = {
      val src = Seq(
        ("f-0", "Россия, Санкт-Петербург, Кушелевская дорога, 8", OwnerRequestStatus.Confirmed, false),
        ("f-1", "Россия, Санкт-Петербург, проспект Энергетиков, 30к1", OwnerRequestStatus.Confirmed, false),
        ("f-2", "Россия, Санкт-Петербург, Кушелевская дорога, 3к1", OwnerRequestStatus.Confirmed, false),
        ("f-3", "Россия, Санкт-Петербург, Кушелевская дорога, 3к2", OwnerRequestStatus.Confirmed, false),
        ("f-4", "Россия, Санкт-Петербург, Кушелевская дорога, 3к3", OwnerRequestStatus.Confirmed, false),
        ("f-5", "Россия, Санкт-Петербург, Кушелевская дорога, 3к4", OwnerRequestStatus.Confirmed, false),
        ("f-6", "Россия, Санкт-Петербург, проспект Энергетиков, 30к1", OwnerRequestStatus.Confirmed, false),
        ("f-7", "Россия, Санкт-Петербург, проспект Энергетиков, 30к2", OwnerRequestStatus.Completed, true),
        ("f-8", "Россия, Санкт-Петербург, проспект Энергетиков, 30к3", OwnerRequestStatus.WorkInProgress, false),
        ("f-9", "Россия, Москва, Тверская улица, 30/2с1", OwnerRequestStatus.Confirmed, false),
        ("f-10", "Россия, Санкт-Петербург, проспект Энергетиков, 30к2", OwnerRequestStatus.Completed, false)
      )
      flatWithOwnerRequest(OwnerRequestStatus.Unknown).next(src.length).toSeq.zip(src).map {
        case (f, (id, addr, status, isRented)) =>
          f.copy(
            flatId = id,
            address = addr,
            unifiedAddress = Some(addr),
            ownerRequests = Seq(f.ownerRequests.head.copy(flatId = id).updateStatus(status = status)),
            isRented = isRented
          )
      }
    }
    Future.sequence(sampleFlats.map(flatDao.create)).futureValue
    ownerRequestDao.create(sampleFlats.flatMap(_.ownerRequests)).futureValue

    case class TestCase(
      prefix: String,
      statuses: Map[Option[Boolean], Set[OwnerRequestStatus]],
      limit: Int,
      expectedFlatIds: Seq[String]
    )

    val sampleGoodPrefix = "Россия, Санкт-Петербург, проспект Энер"

    val cases = Seq(
      // good cases
      TestCase(
        prefix = sampleGoodPrefix,
        statuses = Map((Some(true), Set(OwnerRequestStatus.Completed))),
        limit = 100,
        expectedFlatIds = Seq("f-7")
      ),
      TestCase(
        prefix = sampleGoodPrefix,
        statuses = Map((None, Set(OwnerRequestStatus.Confirmed))),
        limit = 100,
        expectedFlatIds = Seq("f-1", "f-6")
      ),
      TestCase(
        prefix = sampleGoodPrefix,
        statuses = Map((None, Set(OwnerRequestStatus.Confirmed, OwnerRequestStatus.WorkInProgress))),
        limit = 100,
        expectedFlatIds = Seq("f-1", "f-6", "f-8")
      ),
      TestCase(
        prefix = sampleGoodPrefix,
        statuses = Map.empty,
        limit = 100,
        expectedFlatIds = Seq("f-1", "f-6", "f-7", "f-8", "f-10")
      ),
      TestCase(
        prefix = sampleGoodPrefix,
        statuses = Map((Some(false), Set(OwnerRequestStatus.Completed))),
        limit = 100,
        expectedFlatIds = Seq("f-10")
      ),
      // cases without results
      TestCase(
        prefix = sampleGoodPrefix,
        statuses = Map((None, Set(OwnerRequestStatus.Draft))),
        limit = 100,
        expectedFlatIds = Seq()
      ),
      TestCase(
        prefix = "Unknown prefix",
        statuses = Map((None, Set(OwnerRequestStatus.Confirmed))),
        limit = 100,
        expectedFlatIds = Seq()
      )
    )
  }
}
