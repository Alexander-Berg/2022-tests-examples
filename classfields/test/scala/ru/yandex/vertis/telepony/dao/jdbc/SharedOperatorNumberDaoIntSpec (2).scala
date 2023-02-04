package ru.yandex.vertis.telepony.dao.jdbc

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.dao.jdbc.SharedOperatorNumberDao.{Filter, NotEnoughRowsUpdatedException}
import ru.yandex.vertis.telepony.generator.Generator.{OperatorGen, SharedOperatorNumberGen}
import ru.yandex.vertis.telepony.generator.Producer.generatorAsProducer
import ru.yandex.vertis.telepony.model.SharedNumberStatusValues._
import ru.yandex.vertis.telepony.model.{OperatorAccounts, PhoneTypes, SharedNumberStatusValues, SharedNumbersCounter, TypedDomains}
import ru.yandex.vertis.telepony.util.SharedDbSupport

class SharedOperatorNumberDaoIntSpec extends SpecBase with BeforeAndAfterEach with SharedDbSupport {

  implicit val actorSystem = ActorSystem()
  implicit val mat = Materializer(actorSystem)

  val dao = new SharedOperatorNumberDao(sharedDualDb)

  val domain = TypedDomains.autoru_def
  val otherDomain = TypedDomains.autotest

  override protected def beforeEach() = {
    super.beforeEach()
    clearSharedDatabase()
  }

  "SharedOperatorNumberDao" should {
    "create and get" in {
      val opn = SharedOperatorNumberGen.next
      dao.upsert(opn).futureValue
      dao.get(opn.number).futureValue shouldBe Some(opn)
    }

    "update" in {
      val opn = SharedOperatorNumberGen.next
      dao.upsert(opn).futureValue

      val updated = SharedOperatorNumberGen.next.copy(number = opn.number)
      dao.update(updated).futureValue

      dao.get(opn.number).futureValue shouldBe Some(updated)
    }

    "updateCas" in {
      val opn = SharedOperatorNumberGen.next.copy(status = Free)
      dao.upsert(opn).futureValue

      val updated = SharedOperatorNumberGen.next.copy(number = opn.number)

      dao.updateCas(updated, Assigned).futureValue shouldBe false
      dao.get(opn.number).futureValue shouldBe Some(opn)

      dao.updateCas(updated, Free).futureValue shouldBe true
      dao.get(opn.number).futureValue shouldBe Some(updated)
    }

    "list by ByOperatorAccountAll" in {
      val someNumbers = SharedOperatorNumberGen.nextUniqueBy(20)(_.number)
      someNumbers.foreach(opn => dao.upsert(opn).futureValue)

      someNumbers.groupBy(_.account).foreach {
        case (account, numbers) =>
          val filter = Filter.ByOperatorAccountAll(account)
          val all = Source.fromPublisher(dao.list(filter, useDbStream = false)).runWith(Sink.seq).futureValue
          all.foreach(_.account shouldBe account)
          all should contain allElementsOf numbers
      }
    }

    "list ByDomainAssigned" in {
      val assignedNumbers = SharedOperatorNumberGen
        .next(5)
        .map(_.copy(domain = Some(domain), status = Assigned))
      val deletedNumbers = SharedOperatorNumberGen
        .next(2)
        .map(_.copy(domain = Some(domain), status = Deleted))
      val otherNumbers = SharedOperatorNumberGen.filter(n => !n.domain.contains(domain)).next(5)

      (assignedNumbers ++ deletedNumbers ++ otherNumbers)
        .foreach(opn => dao.upsert(opn).futureValue)

      val result = Source
        .fromPublisher(dao.list(Filter.ByDomainAssigned(domain), useDbStream = false))
        .runWith(Sink.seq)
        .futureValue
      result should contain theSameElementsAs assignedNumbers
    }

    "changeStatusAndDomain" in {
      val someNumbers = SharedOperatorNumberGen
        .filter(_.geoId < 10)
        .map(_.copy(status = Free, domain = None))
        .nextUniqueBy(20)(_.number)
      someNumbers.foreach(opn => dao.upsert(opn).futureValue)
      val markerPhone = someNumbers.head

      val filter =
        Filter.ForRelease(markerPhone.phoneType, markerPhone.geoId, markerPhone.account)
      val updatedCount =
        dao.changeStatusAndDomain(Free, None, Assigned, Some(otherDomain), filter, 20).futureValue
      updatedCount should be > 0

      someNumbers.foreach { opn =>
        val loaded = dao.get(opn.number).futureValue.getOrElse {
          fail("Number not found")
        }
        if (opn.phoneType == filter.phoneType
            && opn.geoId == filter.geoId
            && opn.account == filter.account) {
          loaded.domain shouldBe Some(otherDomain)
          loaded.status shouldBe Assigned
        } else {
          loaded shouldBe opn
        }
      }
    }

    "changeStatusAndDomain with limit" in {
      val account = OperatorAccounts.MtsShared
      val someNumbers = SharedOperatorNumberGen
        .map(_.copy(status = Free, account = account, domain = None))
        .nextUniqueBy(10)(_.number)
      someNumbers.foreach(opn => dao.upsert(opn).futureValue)
      val count = 4

      val filter = Filter.ByOperatorAccountAll(account)
      val updatedCount = dao.changeStatusAndDomain(Free, None, Assigned, Some(otherDomain), filter, count).futureValue
      updatedCount shouldBe count

      val loaded = someNumbers.map(opn =>
        dao.get(opn.number).futureValue.getOrElse {
          fail("Number not found")
        }
      )
      val assigned = loaded.filter(_.status == Assigned)
      assigned.size shouldBe count
      assigned.foreach(_.domain shouldBe Some(otherDomain))
    }

    "changeStatusAndDomain fail when not enough rows got updated" in {
      val account = OperatorAccounts.MtsShared
      val someNumbers = SharedOperatorNumberGen
        .map(_.copy(status = Assigned, account = account, domain = Some(domain)))
        .nextUniqueBy(5)(_.number)
      someNumbers.foreach(opn => dao.upsert(opn).futureValue)

      val filter = Filter.ByOperatorAccountAll(account)

      dao
        .changeStatusAndDomain(Assigned, Some(domain), Releasing, Some(domain), filter, 6, failIfNotEnough = true)
        .failed
        .futureValue shouldBe a[NotEnoughRowsUpdatedException]

      someNumbers.foreach { opn =>
        dao.get(opn.number).futureValue shouldBe Some(opn)
      }
    }

    "changeStatusAndDomain with failOnLess (success)" in {
      val account = OperatorAccounts.MtsShared
      val count = 5
      val someNumbers = SharedOperatorNumberGen
        .map(_.copy(status = Assigned, account = account, domain = Some(domain)))
        .nextUniqueBy(count)(_.number)
      someNumbers.foreach(opn => dao.upsert(opn).futureValue)

      val filter = Filter.ByOperatorAccountAll(account)

      dao
        .changeStatusAndDomain(Assigned, Some(domain), Releasing, Some(domain), filter, count, failIfNotEnough = true)
        .futureValue shouldBe count

      someNumbers.foreach { opn =>
        val loaded = dao.get(opn.number).futureValue.getOrElse {
          fail("Number not found")
        }
        loaded.status shouldBe Releasing
        loaded.domain shouldBe opn.domain
      }
    }

    "changeStatusAndDomain with ByPhones filter" in {
      val count = 5
      val someNumbers = SharedOperatorNumberGen
        .map(_.copy(status = Assigned, domain = Some(domain)))
        .nextUniqueBy(count)(_.number)
      someNumbers.foreach(opn => dao.upsert(opn).futureValue)

      val filter = Filter.ByPhones(someNumbers.map(_.number))

      dao
        .changeStatusAndDomain(Assigned, Some(domain), Releasing, Some(domain), filter, count)
        .futureValue shouldBe count

      someNumbers.foreach { opn =>
        val loaded = dao.get(opn.number).futureValue.getOrElse {
          fail("Number not found")
        }
        loaded.status shouldBe Releasing
        loaded.domain shouldBe opn.domain
      }
    }

    "update origin operator" when {
      "numbers with Free status" in {
        val count = 10
        val sharedPoolNumbers = SharedOperatorNumberGen
          .map(_.copy(status = Free, domain = Some(domain)))
          .nextUniqueBy(count)(_.number)
        sharedPoolNumbers.foreach(opn => dao.upsert(opn).futureValue)

        val numbers = sharedPoolNumbers.map(_.number)
        val originOperator = OperatorGen.next
        dao.updateNumbersOriginOperatorCas(numbers, originOperator).futureValue

        val result = Source
          .fromPublisher(dao.list(Filter.ByPhones(numbers), useDbStream = false))
          .runWith(Sink.seq)
          .futureValue

        result.flatMap(_.originOperator).toSet shouldBe Set(originOperator)
      }
    }

    "fail update origin operator" when {
      "not all numbers with Free status" in {
        val count = 10
        val sharedPoolNumbers = SharedOperatorNumberGen
          .map(_.copy(status = Free, domain = Some(domain)))
          .nextUniqueBy(count)(_.number)
        val spoiledSharedPoolNumber = sharedPoolNumbers.head.copy(status = Assigned)
        val spoiledSharedPoolNumbers = spoiledSharedPoolNumber +: sharedPoolNumbers.tail.toSeq
        spoiledSharedPoolNumbers.foreach(opn => dao.upsert(opn).futureValue)

        val numbers = sharedPoolNumbers.map(_.number)
        val originOperator = OperatorGen.next
        val result = dao
          .updateNumbersOriginOperatorCas(numbers, originOperator)
          .failed
          .futureValue
        result shouldBe an[NotEnoughRowsUpdatedException]
      }
    }

    "count free numbers by operators" in {
      val geoId = 1

      // suitable numbers
      val mtsLocalNumbers = SharedOperatorNumberGen
        .next(2)
        .map(_.copy(phoneType = PhoneTypes.Local, geoId = geoId, status = Free, account = OperatorAccounts.MtsShared))

      val mttLocalNumbers = SharedOperatorNumberGen
        .next(3)
        .map(
          _.copy(phoneType = PhoneTypes.Local, geoId = geoId, status = Assigned, account = OperatorAccounts.MttShared)
        )

      val mtsMobileNumbers = SharedOperatorNumberGen
        .next(2)
        .map(
          _.copy(phoneType = PhoneTypes.Mobile, geoId = geoId, status = Free, account = OperatorAccounts.MtsShared)
        )

      val voxLocalNumbers = SharedOperatorNumberGen
        .next(1)
        .map(
          _.copy(phoneType = PhoneTypes.Local, geoId = geoId, status = Assigned, account = OperatorAccounts.VoxShared)
        )

      (mtsLocalNumbers ++ mttLocalNumbers ++ mtsMobileNumbers ++ voxLocalNumbers).foreach(dao.upsert(_).futureValue)

      val expectedResult =
        Seq(
          SharedNumbersCounter(
            geoId = geoId,
            phoneType = PhoneTypes.Local,
            account = OperatorAccounts.MtsShared,
            status = Free,
            count = 2
          ),
          SharedNumbersCounter(
            geoId = geoId,
            phoneType = PhoneTypes.Local,
            account = OperatorAccounts.MttShared,
            status = Assigned,
            count = 3
          )
        )

      val result = dao
        .getNumbersCounters(
          geoId,
          phoneType = PhoneTypes.Local,
          Seq(OperatorAccounts.MtsShared, OperatorAccounts.MttShared),
          statuses = Seq(Free, Assigned)
        )
        .futureValue

      result.diff(expectedResult) shouldEqual Seq.empty
    }

  }
}
