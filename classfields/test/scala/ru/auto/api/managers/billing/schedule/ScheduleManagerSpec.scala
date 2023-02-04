package ru.auto.api.managers.billing.schedule

import org.mockito.Mockito._
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.{ResponseStatus, SuccessResponse}
import ru.auto.api.billing.schedules.ScheduleModel._
import ru.auto.api.managers.billing.schedule.ScheduleManager.{ForOfferAndProduct, ForOffers}
import ru.auto.api.model._
import ru.auto.api.services.salesman.SalesmanUserClient.SalesmanDomain.AutoruSalesmanDomain
import ru.auto.api.services.salesman.{SalesmanClient, SalesmanUserClient}
import ru.auto.api.util.{OwnershipChecker, RequestImpl}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class ScheduleManagerSpec extends BaseSpec with MockitoSupport {
  val ownershipChecker = mock[OwnershipChecker]
  val salesmanClient = mock[SalesmanClient]
  val salesmanUserClient = mock[SalesmanUserClient]

  val scheduleManager = new ScheduleManager(salesmanClient, salesmanUserClient, ownershipChecker)

  after {
    reset(salesmanClient)
    reset(salesmanUserClient)
    reset(ownershipChecker)
  }

  implicit val trace: Traced = Traced.empty

  implicit val userRequest = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setUser(AutoruUser(1))
    r.setRequestParams(RequestParams.construct("0.0.0.0", sessionId = Some("fake_session")))
    r
  }

  val dealerRequest = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setUser(AutoruDealer(1))
    r.setRequestParams(RequestParams.construct("0.0.0.0", sessionId = Some("fake_session")))
    r
  }

  "ScheduleManager.schedules" should {
    "get schedules for users" in {
      val schedulesResponse = ModelGenerators.scheduleResponseGen().next
      when(salesmanUserClient.getSchedules(?, ?, ?, ?)(?)).thenReturnF(schedulesResponse)

      scheduleManager.getSchedules()(userRequest).await shouldBe schedulesResponse

      verify(salesmanUserClient).getSchedules(
        eq(AutoruSalesmanDomain),
        eq(userRequest.user.ref.asPrivate),
        products = eq(Nil),
        offerIds = eq(Nil)
      )(eq(userRequest))
    }

    "get schedules for users with ForOfferAndProduct filter" in {
      val schedulesResponse = ModelGenerators.scheduleResponseGen().next
      when(salesmanUserClient.getSchedules(?, ?, ?, ?)(?)).thenReturnF(schedulesResponse)
      when(ownershipChecker.checkOwnership(?)(?)).thenReturn(Future.unit)

      val offerId = OfferID.parse("123-fff")
      val product = AutoruProduct.Boost

      scheduleManager.getSchedules(ForOfferAndProduct(offerId, product))(userRequest).await shouldBe schedulesResponse

      verify(salesmanUserClient).getSchedules(
        eq(AutoruSalesmanDomain),
        eq(userRequest.user.ref.asPrivate),
        products = eq(List(product)),
        offerIds = eq(List(offerId))
      )(eq(userRequest))
    }

    "get schedules for users with ForOffers filter" in {
      val schedulesResponse = ModelGenerators.scheduleResponseGen().next
      when(salesmanUserClient.getSchedules(?, ?, ?, ?)(?)).thenReturnF(schedulesResponse)
      when(ownershipChecker.checkOwnership(?)(?)).thenReturn(Future.unit)

      val offerIds = List(OfferID.parse("123-fff"), OfferID.parse("321-fff"))

      scheduleManager.getSchedules(ForOffers(offerIds))(userRequest).await shouldBe schedulesResponse

      verify(salesmanUserClient).getSchedules(
        eq(AutoruSalesmanDomain),
        eq(userRequest.user.ref.asPrivate),
        products = eq(Nil),
        offerIds = eq(offerIds)
      )(eq(userRequest))
    }

    "get schedules for dealers" in {
      val schedulesResponse = ModelGenerators.scheduleResponseGen().next
      when(salesmanClient.getSchedules(?, ?, ?)(?)).thenReturnF(schedulesResponse)

      scheduleManager.getSchedules()(dealerRequest).await shouldBe schedulesResponse

      verify(salesmanClient).getSchedules(
        eq(dealerRequest.user.ref.asDealer),
        products = eq(Nil),
        offerIds = eq(Nil)
      )(eq(dealerRequest))
    }

    "get schedules for dealers with non-empty offers filter" in {
      val schedulesResponse = ModelGenerators.scheduleResponseGen().next
      when(salesmanClient.getSchedules(?, ?, ?)(?)).thenReturnF(schedulesResponse)
      when(ownershipChecker.checkOwnership(?)(?)).thenReturn(Future.unit)

      val offerIds = List(OfferID.parse("123-fff"), OfferID.parse("321-fff"))

      scheduleManager.getSchedules(ForOffers(offerIds))(dealerRequest).await shouldBe schedulesResponse

      verify(salesmanClient).getSchedules(
        eq(dealerRequest.user.ref.asDealer),
        products = eq(Nil),
        offerIds = eq(offerIds)
      )(eq(dealerRequest))
    }
  }

  "ScheduleManager.upsertSchedule" should {
    "save schedule for users" in {
      val expected = SuccessResponse
        .newBuilder()
        .setStatus(ResponseStatus.SUCCESS)
        .build()

      when(ownershipChecker.checkOwnership(?)(?)).thenReturn(Future.unit)
      when(salesmanUserClient.putSchedules(?, ?, ?, ?, ?)(?)).thenReturnF(expected)

      val category = CategorySelector.Moto
      val offerId = OfferID(1111111, Some("fff"))
      val product = AutoruProduct.Boost
      val schedule = ScheduleRequest
        .newBuilder()
        .setScheduleType(ScheduleType.ONCE_AT_TIME)
        .setTimezone("+03:00")
        .addAllWeekdays(Iterable(1, 3, 5, 6, 7).map(Int.box).asJava)
        .setTime("10:00")
        .build()

      val result =
        scheduleManager.upsertSchedules(category, ForOfferAndProduct(offerId, product), schedule)(userRequest).await

      result shouldBe expected

      verify(salesmanUserClient).putSchedules(
        eq(AutoruSalesmanDomain),
        eq(userRequest.user.ref.asPrivate),
        eq(product),
        eq(List(offerId)),
        eq(schedule)
      )(eq(userRequest))
    }

    "save schedule for dealers" in {
      val expected = SuccessResponse
        .newBuilder()
        .setStatus(ResponseStatus.SUCCESS)
        .build()

      when(ownershipChecker.checkOwnership(?)(?)).thenReturn(Future.unit)
      when(salesmanClient.putSchedules(?, ?, ?, ?)(?)).thenReturnF(expected)

      val category = CategorySelector.Moto
      val offerId = OfferID(1111111, Some("fff"))
      val product = AutoruProduct.Boost
      val schedule = ScheduleRequest
        .newBuilder()
        .setScheduleType(ScheduleType.ONCE_AT_TIME)
        .addAllWeekdays(Iterable(1, 3, 5, 6, 7).map(Int.box).asJava)
        .setTime("10:00")
        .build()

      val result =
        scheduleManager.upsertSchedules(category, ForOfferAndProduct(offerId, product), schedule)(dealerRequest).await

      result shouldBe expected

      verify(salesmanClient, times(1))
        .putSchedules(
          eq(dealerRequest.user.ref.asDealer),
          eq(product),
          eq(List(offerId)),
          eq(schedule)
        )(eq(dealerRequest))
    }

    "FAILED save schedule" in {
      when(ownershipChecker.checkOwnership(?)(?)).thenReturn(Future.unit)
      when(salesmanUserClient.putSchedules(?, ?, ?, ?, ?)(?))
        .thenThrow(new RuntimeException("Unable to save schedule"))

      val category = CategorySelector.Trucks
      val offerId = OfferID(3333333, Some("fff"))
      val product = AutoruProduct.Boost
      val schedule = ScheduleRequest
        .newBuilder()
        .setScheduleType(ScheduleType.ONCE_AT_TIME)
        .setTimezone("+03:00")
        .setTime("8:00")
        .build()

      val exception = intercept[RuntimeException] {
        scheduleManager.upsertSchedules(category, ForOfferAndProduct(offerId, product), schedule)(userRequest).await
      }

      exception.getMessage shouldBe "Unable to save schedule"

      verify(salesmanUserClient).putSchedules(
        eq(AutoruSalesmanDomain),
        eq(userRequest.user.ref.asPrivate),
        eq(product),
        eq(List(offerId)),
        eq(schedule)
      )(eq(userRequest))
    }
  }
}
