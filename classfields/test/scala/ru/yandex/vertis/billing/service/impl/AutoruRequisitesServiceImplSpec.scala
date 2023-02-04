package ru.yandex.vertis.billing.service.impl

import org.scalatest.TryValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.balance.model.{Balance, Person, PersonRequest}
import ru.yandex.vertis.billing.balance.xmlrpc.BalanceXmlRpcModelException
import ru.yandex.vertis.billing.dao.AutoruBalanceDao
import ru.yandex.vertis.billing.exceptions.RequisitesException.UnknownErrorCode
import ru.yandex.vertis.billing.exceptions.{AutoruClientNotFoundException, RequisitesApiException}
import ru.yandex.vertis.billing.model_core.gens.{Producer, RequisitesGen}
import ru.yandex.vertis.billing.model_core.requisites.Requisites
import ru.yandex.vertis.billing.model_core.{AutoruClientId, ClientId, Uid}
import ru.yandex.vertis.billing.service.impl.AutoruRequisitesServiceImplSpec.RequisitesSetup
import ru.yandex.vertis.billing.util.OperatorContext
import ru.yandex.vertis.mockito.MockitoSupport
import zio.Task

import scala.util.{Failure, Success}

class AutoruRequisitesServiceImplSpec extends AnyWordSpec with Matchers with TryValues with MockitoSupport {

  import AutoruRequisitesServiceImplSpec.rc

  "get requisites" in new RequisitesSetup {
    val recordsCount = 10
    val records = RequisitesGen.next(recordsCount).map(_.copy(clientId = balanceClientId))
    val persons = records.map(r => Person(r.id, balanceClientId, Requisites.toBalancePerson(r.properties)))

    when(balance.getClientPersons(balanceClientId)).thenReturn(Success(persons))

    requisitesService.getPaymentRequisites(autoruClientId) match {
      case Success(rs) => rs should contain theSameElementsAs records
      case other => fail(s"Unexpected $other")
    }
  }

  "add requisites" in new RequisitesSetup {
    val requisitesId = 50L
    val properties = RequisitesGen.next.properties
    val personRequest = PersonRequest(None, balanceClientId, Requisites.toBalancePerson(properties))

    when(balance.createPerson(personRequest)(Uid(1))).thenReturn(Success(requisitesId))

    val result = requisitesService.addPaymentRequisites(autoruClientId, properties)
    result shouldBe Success(requisitesId)
  }

  "update requisites" in new RequisitesSetup {
    val requisitesId = 50L
    val properties = RequisitesGen.next.properties
    val personRequest = PersonRequest(Some(requisitesId), balanceClientId, Requisites.toBalancePerson(properties))

    when(balance.createPerson(personRequest)(Uid(1))).thenReturn(Success(requisitesId))

    val result = requisitesService.updatePaymentRequisites(autoruClientId, requisitesId, properties)
    result shouldBe Success(requisitesId)
  }

  "fail for non existing client" in new RequisitesSetup {
    val clientId = 0L

    requisitesService.getPaymentRequisites(clientId) match {
      case Failure(e: AutoruClientNotFoundException) => e.clientId shouldBe clientId
      case other => fail(s"Unexpected $other")
    }
  }

  "parse error code and message from balance api" in new RequisitesSetup {
    val requisitesId = 50L
    val properties = RequisitesGen.next.properties
    val personRequest = PersonRequest(Some(requisitesId), balanceClientId, Requisites.toBalancePerson(properties))

    val errorMessage =
      """Wrong response MethodResponseValue(FaultValue(-1,<error><msg>Person type (ph) cannot be changed to ur</msg>
        |<db-id>ph</db-id><wo-rollback>0</wo-rollback><hash-id>ur</hash-id><method>Balance2.CreatePerson</method
        |><code>PERSON_TYPE_MISMATCH</code><parent-codes><code>INVALID_PARAM</code><code>EXCEPTION</code>
        |</parent-codes><contents>Person type (ph) cannot be changed to ur</contents></error>))""".stripMargin
    when(balance.createPerson(personRequest)(Uid(1))).thenReturn(Failure(BalanceXmlRpcModelException(errorMessage)))

    requisitesService.updatePaymentRequisites(autoruClientId, requisitesId, properties) match {
      case Failure(RequisitesApiException(code, msg, raw)) =>
        (code, msg, raw) should be(("PERSON_TYPE_MISMATCH", "Person type (ph) cannot be changed to ur", errorMessage))
      case other => fail(s"Unexpected $other")
    }
  }

  "parse unknown error code and message from balance api" in new RequisitesSetup {
    val requisitesId = 50L
    val properties = RequisitesGen.next.properties
    val personRequest = PersonRequest(Some(requisitesId), balanceClientId, Requisites.toBalancePerson(properties))

    val errorMessage = "<error>corrupted error</error>"
    when(balance.createPerson(personRequest)(Uid(1))).thenReturn(Failure(BalanceXmlRpcModelException(errorMessage)))

    requisitesService.updatePaymentRequisites(autoruClientId, requisitesId, properties) match {
      case Failure(RequisitesApiException(code, _, _)) =>
        code should be(UnknownErrorCode)
      case other => fail(s"Unexpected $other")
    }
  }
}

object AutoruRequisitesServiceImplSpec {

  implicit private val rc: OperatorContext = OperatorContext("test", Uid(1))

  trait RequisitesSetup extends MockitoSupport {

    val autoruClientId: AutoruClientId = 10L
    val balanceClientId: ClientId = 100L

    protected val balance = {
      val m = mock[Balance]
      when(m.getClientPersons(?, ?)).thenReturn(Success(List()))
      m
    }

    protected val autoruBalanceDao = {
      new AutoruBalanceDao {
        override def findBalanceClientId(id: AutoruClientId): Task[Option[ClientId]] = {
          Task.succeed(Option.when(id == autoruClientId)(balanceClientId))
        }
      }
    }

    protected val requisitesService = new AutoruRequisitesServiceImpl(balance, autoruBalanceDao)
  }
}
