package ru.auto.cabinet.service

import java.time.OffsetDateTime
import org.mockito.Mockito._
import org.scalacheck.ScalacheckShapeless._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.{ScalaCheckPropertyChecks => PropertyChecks}
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import ru.auto.cabinet.dao.jdbc.{
  BalanceClientIdNotFound,
  BalanceDao,
  BalanceOrderClient,
  JdbcClientDao,
  SubscriptionDao
}
import ru.auto.cabinet.dao.entities.{BalanceClient, BalanceOrder}
import ru.auto.cabinet.model.ClientId
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import ru.auto.cabinet.environment
import ru.auto.cabinet.remote.impl.{BalanceDocumentIO, BalanceIO}
import ru.auto.cabinet.trace.Context

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InvoiceServiceSpec
    extends FlatSpec
    with Matchers
    with PropertyChecks
    with ScalaFutures {

  private def ?[T]: T = any()
  private val balanceIO = mock[BalanceIO]
  private val docIO = mock[BalanceDocumentIO]
  private val balanceDao = mock[BalanceDao]
  private val clientDao = mock[JdbcClientDao]
  private val subscriptionDao = mock[SubscriptionDao]
  private val balanceOrderService = mock[BalanceOrderService]
  implicit private val instr: Instr = new EmptyInstr("test")
  implicit private val rc = Context.unknown

  private val service = new InvoiceService(
    balanceIO,
    docIO,
    balanceDao,
    clientDao,
    subscriptionDao,
    balanceOrderService)

  implicit private val arbPosLong: Arbitrary[Long] = Arbitrary(Gen.posNum[Long])

  implicit private val arbDateTime: Arbitrary[OffsetDateTime] = Arbitrary(
    Gen.choose(1L, 7L).map(environment.now.plusDays))
  println(
    arbDateTime.toString
  ) // чтобы не вылетал ворнинг о неиспользовании из-за бага в компиляторе. Можно удалить после перехода на 2.13

  "InvoiceService.getBalanceClient()" should "get client as is" in {
    forAll {
      (
          baseBalanceClient: BalanceClient,
          balanceOrder: BalanceOrder,
          clientId: ClientId) =>
        reset(balanceDao)
        val balanceClient = baseBalanceClient.copy(
          balanceClientId = Some(1),
          balanceAgencyId = Some(1))
        when(balanceDao.getBalanceClient(?)(?)(?))
          .thenReturn(Future.successful(balanceClient))
        when(balanceDao.findOrder(?, ?)(?)(?))
          .thenReturn(Future.successful(Some(balanceOrder)))
        service
          .getBalanceClientWithOrder(clientId)
          .futureValue shouldBe BalanceOrderClient(
          balanceOrder.id,
          balanceClient.balanceClientId,
          balanceClient.balanceAgencyId,
          balanceClient.regionId,
          balanceClient.name,
          balanceClient.contractId
        )
        verify(balanceDao).getBalanceClient(clientId)(false)
        verify(balanceDao).findOrder(
          balanceClient.balanceClientId.get,
          balanceClient.balanceAgencyId)(false)
        verifyNoMoreInteractions(balanceDao)
    }
  }

  it should "get client without zero balance agency id" in {
    forAll {
      (
          baseBalanceClient: BalanceClient,
          balanceOrder: BalanceOrder,
          clientId: ClientId) =>
        whenever(!baseBalanceClient.balanceClientId.exists(_ <= 0)) {
          reset(balanceDao)
          val balanceClient = baseBalanceClient.copy(
            balanceClientId = Some(1),
            balanceAgencyId = Some(0))
          when(balanceDao.getBalanceClient(?)(?)(?))
            .thenReturn(Future.successful(balanceClient))
          when(balanceDao.findOrder(?, ?)(?)(?))
            .thenReturn(Future.successful(Some(balanceOrder)))
          service
            .getBalanceClientWithOrder(clientId)
            .futureValue shouldBe BalanceOrderClient(
            balanceOrder.id,
            balanceClient.balanceClientId,
            None,
            balanceClient.regionId,
            balanceClient.name,
            balanceClient.contractId
          )
          verify(balanceDao).getBalanceClient(clientId)(false)
          verify(balanceDao).findOrder(
            balanceClient.balanceClientId.get,
            balanceClient.balanceAgencyId)(false)
          verifyNoMoreInteractions(balanceDao)
        }
    }
  }

  it should "get client without both zero balance client and agency id" in {
    forAll {
      (
          baseBalanceClient: BalanceClient,
          balanceOrder: BalanceOrder,
          clientId: ClientId) =>
        reset(balanceDao)
        val balanceClient = baseBalanceClient.copy(
          balanceClientId = None,
          balanceAgencyId = Some(0))
        when(balanceDao.getBalanceClient(?)(?)(?))
          .thenReturn(Future.successful(balanceClient))
        when(balanceDao.findOrder(?, ?)(?)(?))
          .thenReturn(Future.successful(Some(balanceOrder)))
        service
          .getBalanceClientWithOrder(clientId)
          .failed
          .futureValue shouldBe an[BalanceClientIdNotFound]
        verify(balanceDao).getBalanceClient(clientId)(false)
        verifyNoMoreInteractions(balanceDao)
    }
  }
}
