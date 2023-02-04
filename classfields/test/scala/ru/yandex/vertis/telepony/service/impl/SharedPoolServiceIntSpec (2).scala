package ru.yandex.vertis.telepony.service.impl

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.RecoverMethods.recoverToExceptionIf
import ru.yandex.vertis.telepony.SampleHelper.rc
import ru.yandex.vertis.telepony.dao.jdbc.SharedOperatorNumberDao.Filter
import ru.yandex.vertis.telepony.generator.Generator.{originOperatorGen, DomainGen, OperatorAccountGen, OperatorGen, PhoneGen, SharedNumberCreateRequestGen, UpdateOriginOperatorRequestGen}
import ru.yandex.vertis.telepony.generator.Producer.generatorAsProducer
import ru.yandex.vertis.telepony.model.SharedNumberStatusValues._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.SharedPoolService.{NotCorrectNumberException, UnexpectedStatusException, UpdateGarbageRequest, UpdateOriginOperatorRequest}
import ru.yandex.vertis.telepony.{IntegrationSpecTemplate, SpecBase}

import scala.reflect.ClassTag
import scala.annotation.nowarn

@nowarn
class SharedPoolServiceIntSpec extends SpecBase with IntegrationSpecTemplate with BeforeAndAfterEach {

  val someDomain = TypedDomains.autoru_def
  val otherDomain = TypedDomains.autotest

  implicit val am: Materializer = materializer
  import am.executionContext

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    sharedOperatorNumberDao.clear().futureValue
  }

  private def checkRelease(withOriginOperator: Boolean): Unit = {
    val number = addNumber(Some(someDomain), withOriginOperator)
    sharedPoolService.release(number, someDomain).futureValue shouldBe true
    val opn = sharedPoolService.get(number).futureValue
    opn.domain shouldBe None
    opn.status shouldBe Free
  }

  private def checkReleaseAndMoveToOtherDomain(withOriginOperator: Boolean): Unit = {
    val number = addNumber(Some(someDomain), withOriginOperator)
    sharedPoolService.moveToOtherDomain(number, someDomain, otherDomain).futureValue shouldBe true
    val opn = sharedPoolService.get(number).futureValue
    opn.domain shouldBe Some(otherDomain)
    opn.status shouldBe Assigned
  }

  private def checkSetOriginOperatorSuccess(account: OperatorAccount, originOperator: Operator): Unit = {
    val req = SharedNumberCreateRequestGen.next.copy(
      domain = None,
      account = account,
      originOperator = None
    )
    val result = sharedPoolService.createOrUpdate(req).futureValue

    val request = UpdateOriginOperatorRequest(Seq(result.number), originOperator)
    sharedPoolService.updateOriginOperator(request).futureValue

    val actual = sharedPoolService.get(req.number).futureValue
    actual.originOperator shouldBe Some(originOperator)
  }

  private def checkSetOriginOperatorFailure[T: ClassTag](
      account: OperatorAccount,
      originOperator: Operator,
      domain: Option[TypedDomain] = None): Unit = {
    val req = SharedNumberCreateRequestGen.next.copy(
      domain = domain,
      account = account,
      originOperator = None
    )
    val result = sharedPoolService.createOrUpdate(req).futureValue

    val request = UpdateOriginOperatorRequest(Seq(result.number), originOperator)
    sharedPoolService.updateOriginOperator(request).failed.futureValue shouldBe an[T]
  }

  "SharedPoolService" should {
    "create number" in {
      val req = SharedNumberCreateRequestGen.filter(_.domain.isEmpty).next
      val result = sharedPoolService.createOrUpdate(req).futureValue
      result.number shouldBe req.number
      result.account shouldBe req.account
      result.originOperator shouldBe req.originOperator
      result.domain shouldBe empty
      result.status shouldBe SharedNumberStatusValues.Free
    }

    "create number with specified domain" in {
      val req = SharedNumberCreateRequestGen.filter(_.domain.isDefined).next
      val result = sharedPoolService.createOrUpdate(req).futureValue
      result.number shouldBe req.number
      result.account shouldBe req.account
      result.originOperator shouldBe req.originOperator
      result.domain shouldBe req.domain
      result.status shouldBe SharedNumberStatusValues.Assigned
    }

    "recreate deleted number" in {
      val number = addNumber()
      sharedPoolService.delete(number).futureValue

      val req = SharedNumberCreateRequestGen.next.copy(number = number, domain = None)
      val result = sharedPoolService.createOrUpdate(req).futureValue
      result.number shouldBe req.number
      result.account shouldBe req.account
      result.originOperator shouldBe req.originOperator
      result.domain shouldBe empty
      result.status shouldBe SharedNumberStatusValues.Free
    }

    "get number" in {
      val req = SharedNumberCreateRequestGen.next
      val createNumber = sharedPoolService.createOrUpdate(req).futureValue
      sharedPoolService.get(req.number).futureValue shouldBe createNumber
    }

    "delete number" in {
      val req = SharedNumberCreateRequestGen.next
      sharedPoolService.createOrUpdate(req).futureValue

      sharedPoolService.delete(req.number).futureValue

      val deletedNumber = sharedPoolService.get(req.number).futureValue
      deletedNumber.status shouldBe Deleted
    }

    "assign number to domain" in {
      val number = addNumber()
      sharedPoolService.assign(number, someDomain).futureValue
      val opn = sharedPoolService.get(number).futureValue
      opn.domain shouldBe Some(someDomain)
      opn.status shouldBe Assigned
    }

    "not allow to assign assigned number" in {
      val number = addNumber(Some(otherDomain))
      sharedPoolService.assign(number, someDomain).failed.futureValue shouldBe an[UnexpectedStatusException]
    }

    "release number from domain" when {
      "origin operator is set" in {
        checkRelease(withOriginOperator = true)
      }
      "origin operator is not set" in {
        checkRelease(withOriginOperator = false)
      }
    }

    "release and move to another domain" when {
      "origin operator is set" in {
        checkReleaseAndMoveToOtherDomain(withOriginOperator = true)
      }
      "origin operator is not set" in {
        checkReleaseAndMoveToOtherDomain(withOriginOperator = false)
      }
    }

    "list domain numbers" in {
      val domainNumbers = (1 to 5).map(_ => addNumber(Some(someDomain)))
      val otherDomainNumbers = (1 to 4).map(_ => addNumber(Some(otherDomain)))
      (1 to 10).map(_ => addNumber())

      sharedPoolService
        .listDomainNumbers(someDomain)
        .futureValue
        .map(_.number) should contain theSameElementsAs domainNumbers

      sharedPoolService
        .listDomainNumbers(otherDomain)
        .futureValue
        .map(_.number) should contain theSameElementsAs otherDomainNumbers
    }

    "statistics" in {
      val domainNumbers = (1 to 5).map(_ => addNumber(Some(someDomain)))
      val otherDomainNumbers = (1 to 4).map(_ => addNumber(Some(otherDomain)))
      val freeNumbers = (1 to 10).map(_ => addNumber())

      val stat = sharedPoolService.statistics().futureValue
      stat.all.statuses(Free) shouldBe freeNumbers.size
      stat.all.statuses(Assigned) shouldBe (domainNumbers.size + otherDomainNumbers.size)
      stat.all.domains(someDomain) shouldBe domainNumbers.size
      stat.all.domains(otherDomain) shouldBe otherDomainNumbers.size
      stat.accounts should not be empty
    }

    "list all numbers" in {
      val account = OperatorAccounts.BeelineShared

      val numbers = SharedNumberCreateRequestGen
        .next(10)
        .map(_.copy(account = account))
        .map(req => sharedPoolService.createOrUpdate(req).futureValue)
        .map(_.number)

      val filter = Filter.ByOperatorAccountActual(account)

      val actualNumbers = Source
        .fromPublisher(sharedPoolService.list(filter, useDbStream = true))
        .map(_.number)
        .runWith(Sink.seq)
        .futureValue

      (actualNumbers should contain).theSameElementsInOrderAs(numbers.toSeq.sortBy(_.value))
    }

    "move to garbage numbers" in {
      val number1 = addNumber()
      val number2 = addNumber()
      val number3 = addNumber()
      sharedPoolService.assign(number3, someDomain).futureValue

      val request = UpdateGarbageRequest(Seq(number1, number2), toGarbage = true)
      sharedPoolService.updateGarbage(request).futureValue

      val actual = Source
        .fromPublisher(sharedPoolService.list(Filter.All, useDbStream = true))
        .map(n => (n.number, n.status))
        .runWith(Sink.seq)
        .futureValue

      val expected = Seq(number1 -> Garbage, number2 -> Garbage, number3 -> Assigned)
      (actual should contain).theSameElementsInOrderAs(expected)
    }

    "move to free from garbage numbers" in {
      val number1 = addNumber()
      val number2 = addNumber()

      val request1 = UpdateGarbageRequest(Seq(number1, number2), toGarbage = true)
      sharedPoolService.updateGarbage(request1).futureValue

      val request2 = UpdateGarbageRequest(Seq(number1, number2), toGarbage = false)
      sharedPoolService.updateGarbage(request2).futureValue

      val actual = Source
        .fromPublisher(sharedPoolService.list(Filter.All, useDbStream = true))
        .map(n => (n.number, n.status))
        .runWith(Sink.seq)
        .futureValue

      val expected = Seq(number1 -> Free, number2 -> Free)
      (actual should contain).theSameElementsInOrderAs(expected)
    }

    "failed move to garbage not existed number" in {
      val number = PhoneGen.next
      val request = UpdateGarbageRequest(Seq(number), toGarbage = true)

      recoverToExceptionIf[NotCorrectNumberException] {
        sharedPoolService.updateGarbage(request)
      }.futureValue
    }

    "failed move to garbage not free number" in {
      val number = addNumber()
      sharedPoolService.assign(number, someDomain).futureValue
      val request = UpdateGarbageRequest(Seq(number), toGarbage = true)

      recoverToExceptionIf[NotCorrectNumberException] {
        sharedPoolService.updateGarbage(request)
      }.futureValue
    }

    "set origin operator" when {
      "Vox number with Vox origin operator" in {
        checkSetOriginOperatorSuccess(OperatorAccounts.VoxShared, Operators.Vox)
      }
      "Vox number with possible non Vox origin operator" in {
        val originOperator = originOperatorGen(OperatorAccounts.VoxShared.operator).next
        checkSetOriginOperatorSuccess(OperatorAccounts.VoxShared, originOperator)
      }
      "non Vox number with same origin operator" in {
        val account = OperatorAccountGen.filter(_.operator != Operators.Vox).next
        checkSetOriginOperatorSuccess(account, account.operator)
      }
    }
    "fail set origin operator" when {
      "non Vox number with non equal origin operator" in {
        val account = OperatorAccountGen.filter(_.operator != Operators.Vox).next
        val originOperator = OperatorGen.filter(_ != account.operator).next
        checkSetOriginOperatorFailure[IllegalArgumentException](account, originOperator)
      }
      "on assigned numbers" in {
        val account = OperatorAccountGen.filter(_.operator != Operators.Vox).next
        val domain = DomainGen.next
        checkSetOriginOperatorFailure[IllegalStateException](account, account.operator, Some(domain))
      }
    }

  }

  private def addNumber(domain: Option[TypedDomain] = None, withOriginOperator: Boolean = true): Phone = {
    val reqGen = SharedNumberCreateRequestGen.filter { number =>
      if (withOriginOperator) {
        number.originOperator.isDefined
      } else {
        number.originOperator.isEmpty
      }
    }
    val req = reqGen.next.copy(domain = domain)
    val result = sharedPoolService.createOrUpdate(req).futureValue
    result.number
  }
}
