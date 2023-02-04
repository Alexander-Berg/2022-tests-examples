package ru.yandex.vertis.telepony.service.impl

import org.joda.time.DateTime
import org.mockito.Mockito
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{any, eq => equ}
import ru.yandex.vertis.telepony.api.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.inspection.NumberInspectionService
import ru.yandex.vertis.telepony.model.RedirectOptions.RedirectCallbackInfo
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.model.inspection.InspectionState
import ru.yandex.vertis.telepony.properties.DomainDynamicPropertiesReader
import ru.yandex.vertis.telepony.properties.DynamicProperties.ConvertAfterHoursCallsToCallbackProperty
import ru.yandex.vertis.telepony.service.CallRoutingService.Destination
import ru.yandex.vertis.telepony.service.after_hours.AfterHoursCallbackService
import ru.yandex.vertis.telepony.service.{ActionDecider, DevNullPhonesService, RedirectSupplier}
import ru.yandex.vertis.telepony.util.Threads.lightWeightTasksEc

import scala.concurrent.Future

/**
  * @author neron
  */
class CallRoutingServiceSpec extends SpecBase with MockitoSupport {

  trait Test extends MockitoSupport {
    val source = RefinedSourceGen.next
    val russianSource = RussianRefinedSourceGen.next
    val internationalSource = InternationalRefinedSourceGen.next
    val redirect = ActualRedirectGen.next
    val proxy = redirect.source.number
    val target = redirect.target
    val mockedRedirectSupplier = mock[RedirectSupplier]
    val mockedActionDecider = mock[ActionDecider]
    val mockedDevNullPhones = mock[DevNullPhonesService]
    val mockedRedirectCallback = mock[AfterHoursCallbackService]
    val mockedDomainDynamicProperties = mock[DomainDynamicPropertiesReader]
    val mockedNumberInspectionService = mock[NumberInspectionService]
    val domain = DomainGen.next

    val service = new CallRoutingServiceImpl(
      mockedRedirectSupplier,
      mockedActionDecider,
      mockedDevNullPhones,
      mockedRedirectCallback,
      mockedDomainDynamicProperties,
      domain,
      mockedNumberInspectionService
    )
    when(mockedDevNullPhones.get).thenReturn(target)
  }

  "CallRoutingService" should {
    "route blocked" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      when(mockedActionDecider.decide(equ(source), equ(proxy))).thenReturn(Future.successful(Block))
      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(Some(redirect)))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(false)

      val expectedDestination = Destination(target, RouteResults.Blocked)
      val destination = service.route(Some(source), proxy).futureValue
      destination should ===(expectedDestination)

      verify(mockedDevNullPhones).get
      verify(mockedActionDecider).decide(equ(source), equ(proxy))
      verify(mockedRedirectSupplier).getRedirect(equ(proxy))
    }

    "route no redirect" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      when(mockedActionDecider.decide(equ(source), equ(proxy))).thenReturn(Future.successful(Pass))
      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(None))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(false)

      val expectedDestination = Destination(target, RouteResults.NoRedirect)
      val destination = service.route(Some(source), proxy).futureValue
      destination should ===(expectedDestination)

      verify(mockedActionDecider).decide(equ(source), equ(proxy))
      verify(mockedDevNullPhones).get
      verify(mockedRedirectSupplier).getRedirect(equ(proxy))
    }

    "route passed" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      when(mockedActionDecider.decide(equ(source), equ(proxy))).thenReturn(Future.successful(Pass))
      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(Some(redirect)))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(false)

      val expectedDestination = Destination(target, RouteResults.Passed, redirect.callerId)
      val destination = service.route(Some(source), proxy).futureValue
      destination should ===(expectedDestination)

      verify(mockedActionDecider).decide(equ(source), equ(proxy))
      verify(mockedRedirectSupplier).getRedirect(equ(proxy))
    }

    "route passed for empty caller" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      when(mockedActionDecider.decide(any(), any())).thenReturn(Future.successful(Block))

      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(Some(redirect)))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(false)

      val expectedDestination = Destination(target, RouteResults.Passed, redirect.callerId)
      val destination = service.route(None, proxy).futureValue
      destination should ===(expectedDestination)

      verify(mockedRedirectSupplier).getRedirect(equ(proxy))
    }

    "route no redirect for empty caller" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      when(mockedActionDecider.decide(any(), any())).thenReturn(Future.successful(Block))

      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(None))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(false)

      val expectedDestination = Destination(target, RouteResults.NoRedirect)
      val destination = service.route(None, proxy).futureValue
      destination should ===(expectedDestination)

      verify(mockedDevNullPhones).get
      verify(mockedRedirectSupplier).getRedirect(equ(proxy))
    }

    "route passed redirect with disable audio" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      val newOptions = RedirectOptions.Empty.copy(callerIdMode = Some(true), disableTargetAudio = Some(true))
      val oneTimeRedirect = redirect.copy(options = Some(newOptions))
      when(mockedActionDecider.decide(equ(source), equ(proxy))).thenReturn(Future.successful(Pass))
      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(Some(oneTimeRedirect)))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(false)

      val expectedDestination =
        Destination(target, RouteResults.Passed, oneTimeRedirect.callerId, disableTargetAudio = true)
      val destination = service.route(Some(source), proxy).futureValue
      destination should ===(expectedDestination)

      verify(mockedActionDecider).decide(equ(source), equ(proxy))
      verify(mockedRedirectSupplier).getRedirect(equ(proxy))
    }

    "route to unmatched if not in personal whitelist" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      val randomSource = RefinedSourceGen.next
      val myRedirect =
        redirect.copy(options = Some(RedirectOptions.Empty.copy(callPassRules = Some(CallPassRules(Set(source))))))

      when(mockedActionDecider.decide(?, equ(proxy))).thenReturn(Future.successful(Pass))
      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(Some(myRedirect)))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(false)

      val destination1 = service.route(Some(source), proxy).futureValue
      val destination2 = service.route(Some(randomSource), proxy).futureValue

      destination1.routeResult shouldBe RouteResults.Passed
      destination2.routeResult shouldBe RouteResults.NoRedirect

      verify(mockedActionDecider).decide(eq(source), eq(proxy))
      verify(mockedActionDecider).decide(eq(randomSource), eq(proxy))
      verifyNoMoreInteractions(mockedActionDecider)

      verify(mockedRedirectSupplier, Mockito.times(2)).getRedirect(equ(proxy))
      verifyNoMoreInteractions(mockedRedirectSupplier)
    }

    "pass even blocked if it is in personal whitelist" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      val myRedirect =
        redirect.copy(options = Some(RedirectOptions.Empty.copy(callPassRules = Some(CallPassRules(Set(source))))))

      when(mockedActionDecider.decide(?, equ(proxy))).thenReturn(Future.successful(Block))
      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(Some(myRedirect)))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(false)

      val destination1 = service.route(Some(source), proxy).futureValue

      destination1.routeResult shouldBe RouteResults.Passed

      verify(mockedActionDecider).decide(eq(source), eq(proxy))
      verifyNoMoreInteractions(mockedActionDecider)

      verify(mockedRedirectSupplier).getRedirect(equ(proxy))
      verifyNoMoreInteractions(mockedRedirectSupplier)
    }

    "redirect to callback number if callback redirects enabled and its not working hours" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      val now = DateTime.now()
      val myRedirect =
        redirect.copy(
          options = Some(
            RedirectOptions.Empty.copy(callbackInfo =
              Some(
                RedirectCallbackInfo(
                  callPeriods = Seq(CallPeriod(openTime = now.plusDays(1), closeTime = now.plusDays(2))),
                  Some("ПИК")
                )
              )
            )
          )
        )

      val callbackRedirectPhone = Phone("+79999999999")

      when(mockedActionDecider.decide(?, equ(proxy))).thenReturn(Future.successful(Pass))
      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(Some(myRedirect)))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(true)
      when(mockedRedirectCallback.getCallbackOrderPhone(?)).thenReturn(callbackRedirectPhone)

      val destination = service.route(Some(russianSource), proxy).futureValue

      destination.routeResult shouldBe RouteResults.Redirected
      destination.targetNumber shouldBe callbackRedirectPhone
    }

    "not redirect to callback number if source is not russian phone" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      val now = DateTime.now()
      val myRedirect =
        redirect.copy(
          options = Some(
            RedirectOptions.Empty.copy(callbackInfo =
              Some(
                RedirectCallbackInfo(
                  callPeriods = Seq(CallPeriod(openTime = now.plusDays(1), closeTime = now.plusDays(2))),
                  Some("ПИК")
                )
              )
            )
          )
        )

      val callbackRedirectPhone = Phone("+79999999999")

      when(mockedActionDecider.decide(?, equ(proxy))).thenReturn(Future.successful(Pass))
      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(Some(myRedirect)))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(true)
      when(mockedRedirectCallback.getCallbackOrderPhone(?)).thenReturn(callbackRedirectPhone)

      val destination = service.route(Some(internationalSource), proxy).futureValue

      destination.targetNumber should not be callbackRedirectPhone
      destination.targetNumber shouldBe redirect.target
    }

    "not redirect to callback number if callback enabled and not working hours but after all intervals" in new Test {
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(false))
      val now = DateTime.now()
      val myRedirect =
        redirect.copy(
          options = Some(
            RedirectOptions.Empty.copy(callbackInfo =
              Some(
                RedirectCallbackInfo(
                  callPeriods = Seq(CallPeriod(openTime = now.minusDays(2), closeTime = now.minusDays(1))),
                  Some("ПИК")
                )
              )
            )
          )
        )

      val callbackRedirectPhone = Phone("+79999999999")

      when(mockedActionDecider.decide(?, equ(proxy))).thenReturn(Future.successful(Pass))
      when(mockedRedirectSupplier.getRedirect(equ(proxy))).thenReturn(Future.successful(Some(myRedirect)))
      when(mockedDomainDynamicProperties.getValue(equ(ConvertAfterHoursCallsToCallbackProperty))).thenReturn(true)
      when(mockedRedirectCallback.getCallbackOrderPhone(?)).thenReturn(callbackRedirectPhone)

      val destination = service.route(Some(russianSource), proxy).futureValue

      destination.targetNumber should not be callbackRedirectPhone
      destination.targetNumber shouldBe redirect.target
    }

    "update inspection state for active number inspection" in new Test {
      val opNumber = PhoneGen.next
      val callerId = PhoneGen.next
      when(mockedNumberInspectionService.hasActiveInspection(any(), any())).thenReturn(Future.successful(true))
      when(mockedNumberInspectionService.updateState(any(), any())).thenReturn(Future.unit)

      val expectedDestination = Destination(target, RouteResults.Passed)
      val destination = service.route(Some(RefinedSource.from(callerId)), opNumber).futureValue
      destination should ===(expectedDestination)

      verify(mockedDevNullPhones).get
      verify(mockedNumberInspectionService).updateState(equ(opNumber), equ(InspectionState.CallPassedEvent))
    }
  }

}
