package ru.yandex.vertis.telepony.service.after_hours

import org.mockito.Mockito
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => equ}
import ru.yandex.vertis.telepony.api.SpecBase
import ru.yandex.vertis.telepony.generator.Generator.{
  CallPeriodsGen,
  DateTimeGen,
  DomainGen,
  HistoryRedirectGen,
  PhoneGen,
  SharedOperatorNumberGen
}
import ru.yandex.vertis.telepony.generator.Producer.generatorAsProducer
import ru.yandex.vertis.telepony.model.CallbackGenerator.CallbackOrderGen
import ru.yandex.vertis.telepony.model.RedirectOptions.RedirectCallbackInfo
import ru.yandex.vertis.telepony.model.{Phone, RedirectOptions}
import ru.yandex.vertis.telepony.model.vox.{
  VoxCallbackInfo,
  VoxCallbackWithConfirmationInfo,
  VoxCallbackWithoutConfirmationInfo
}
import ru.yandex.vertis.telepony.properties.DynamicProperties
import ru.yandex.vertis.telepony.properties.DynamicProperties.IsAfterHoursCallbackConfirmationEnabled
import ru.yandex.vertis.telepony.service.{CallbackOrderService, RedirectServiceV2, SharedPoolService}
import ru.yandex.vertis.telepony.settings.CallbackSettings
import ru.yandex.vertis.telepony.settings.CallbackSettings.{AfterHoursSettings, AfterHoursWithoutConfirmationSettings}
import ru.yandex.vertis.telepony.util.{AutomatedContext, RequestContext}

import java.util.concurrent.Executors
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class AfterHoursCallbackServiceImplSpec extends SpecBase with MockitoSupport {

  private lazy val domain = DomainGen.next
  private lazy val redirectService = mock[RedirectServiceV2]
  private lazy val sharedPoolService = mock[SharedPoolService]
  private lazy val callbackSettings = mock[CallbackSettings]
  private lazy val afterHoursSettings = mock[AfterHoursSettings]
  private lazy val afterHoursWithoutConfirmationSettings = mock[AfterHoursWithoutConfirmationSettings]
  private lazy val callbackOrderService = mock[CallbackOrderService]
  private lazy val dynamicProperties = mock[DynamicProperties]

  implicit val rc: RequestContext = AutomatedContext("any id")
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(1))

  private lazy val service: AfterHoursCallbackService = new AfterHoursCallbackServiceImpl(
    callbackSettingsMap = Map(domain -> callbackSettings),
    redirectService = redirectService,
    sharedPoolService = sharedPoolService,
    callbackOrderService = callbackOrderService,
    dynamicProperties = dynamicProperties
  )

  @tailrec
  private def genAnotherSourcePhone(redirectPhone: Phone): Phone = {
    PhoneGen.next match {
      case `redirectPhone` => genAnotherSourcePhone(redirectPhone)
      case x => x
    }
  }

  private lazy val validCallbackOption: RedirectCallbackInfo =
    RedirectCallbackInfo(CallPeriodsGen.next, Some("some name"))

  "AfterHoursCallbackServiceImpl" should {
    "return proxy phone" in {
      val proxyPhone = PhoneGen.next
      when(callbackSettings.afterHoursCallbackSettings).thenReturn(afterHoursSettings)
      when(callbackSettings.afterHoursCallbackProxy).thenReturn(proxyPhone)
      service.getCallbackOrderPhone(domain) shouldBe proxyPhone
    }

    "create callback order" in {
      val callbackOption = validCallbackOption
      val redirectOptions = RedirectOptions.Empty.copy(callbackInfo = Some(callbackOption))
      val redirectPhone = PhoneGen.next
      val source = genAnotherSourcePhone(redirectPhone)
      val redirect = HistoryRedirectGen.next.copy(number = redirectPhone, options = Some(redirectOptions))
      val redirectId = redirect.id
      when(redirectService.getHistoryRedirectByDomain(redirectId, domain)).thenReturn(Future.successful(redirect))
      val anyOrder = CallbackOrderGen.next
      when(callbackOrderService.order(?, ?, ?)(?)).thenReturn(Future.successful(anyOrder))
      service.createCallbackOrder(redirectId, source, domain).futureValue
      Mockito.verify(callbackOrderService, Mockito.times(1)).order(?, ?, ?)(?)
    }

    "fail to create callback order if invalid redirect options" in {
      val callbackOption = validCallbackOption
      val redirectOptions = RedirectOptions.Empty.copy(callerIdMode = Some(true), callbackInfo = Some(callbackOption))
      val redirectPhone = PhoneGen.next
      val source = genAnotherSourcePhone(redirectPhone)
      val redirect = HistoryRedirectGen.next.copy(number = redirectPhone, options = Some(redirectOptions))
      val redirectId = redirect.id
      when(redirectService.getHistoryRedirectByDomain(redirectId, domain)).thenReturn(Future.successful(redirect))
      service
        .createCallbackOrder(redirectId, source, domain)
        .failed
        .futureValue shouldBe an[RedirectOptions.InvalidOptionsException]
      Mockito.verifyNoMoreInteractions(callbackOrderService)
    }

    "fail to create callback order if proxy and source are equal" in {
      val callbackOption = validCallbackOption
      val redirectOptions = RedirectOptions.Empty.copy(callbackInfo = Some(callbackOption))
      val redirectPhone = PhoneGen.next
      val source = redirectPhone
      val redirect = HistoryRedirectGen.next.copy(number = redirectPhone, options = Some(redirectOptions))
      val redirectId = redirect.id
      when(redirectService.getHistoryRedirectByDomain(redirectId, domain)).thenReturn(Future.successful(redirect))
      service
        .createCallbackOrder(redirectId, source, domain)
        .failed
        .futureValue shouldBe an[AfterHoursCallbackServiceImpl.SourceNumberEqualsProxyException]
      Mockito.verifyNoMoreInteractions(callbackOrderService)
    }

    "return correct callback info with confirmation" in {
      val number = SharedOperatorNumberGen.next.copy(domain = Some(domain))
      val incomeRedirectPhone = number.number
      val time = DateTimeGen.next
      val targetName = "Компания ПИК"

      val callbackOption = RedirectCallbackInfo(Seq.empty, Some(targetName))
      val redirectOptions = RedirectOptions.Empty.copy(callbackInfo = Some(callbackOption))
      val redirect = HistoryRedirectGen.next.copy(number = incomeRedirectPhone, options = Some(redirectOptions))

      when(sharedPoolService.get(equ(incomeRedirectPhone))(?)).thenReturn(Future.successful(number))
      when(redirectService.getOnTimeDomain(equ(incomeRedirectPhone), equ(time), equ(domain))(?))
        .thenReturn(Future.successful(Some(redirect)))

      when(afterHoursSettings.expectedKey).thenReturn("5")
      when(afterHoursSettings.confirmationPhrase).thenReturn("6")

      when(afterHoursSettings.templateReplacement).thenReturn("{{xxx}}")
      when(afterHoursSettings.welcomeMessageTemplate).thenReturn("Здравствуйте, {{xxx}}, спасибо за звонок!")
      when(afterHoursSettings.confirmationRepeatableMessage).thenReturn("Нажмите")
      when(afterHoursSettings.finalOkMessage).thenReturn("Ждите звонка")
      when(afterHoursSettings.finalDefaultMessage).thenReturn("Пока")

      when(callbackSettings.afterHoursCallbackSettings).thenReturn(afterHoursSettings)
      when(dynamicProperties.getValue(domain, IsAfterHoursCallbackConfirmationEnabled)).thenReturn(true)

      val expected = VoxCallbackWithConfirmationInfo(
        redirectId = redirect.id.value,
        domain = domain,
        confirmation = VoxCallbackInfo.Confirmation(
          phrase = "6",
          key = "5"
        ),
        messages = VoxCallbackInfo.Messages(
          welcomeMessage = VoxCallbackInfo.TextMessage(text = "Здравствуйте, Компания ПИК, спасибо за звонок!"),
          confirmationRepeatableMessage = VoxCallbackInfo.TextMessage(text = "Нажмите"),
          finalOkMessage = VoxCallbackInfo.TextMessage(text = "Ждите звонка"),
          finalDefaultMessage = VoxCallbackInfo.TextMessage(text = "Пока"),
          finalMessage = VoxCallbackInfo.TextMessage(text = "Пока")
        )
      )

      service.getConfirmationInfo(incomeRedirectPhone, time).futureValue shouldBe expected
    }

    "return correct callback info without confirmation" in {
      val number = SharedOperatorNumberGen.next.copy(domain = Some(domain))
      val incomeRedirectPhone = number.number
      val time = DateTimeGen.next
      val targetName = "Компания ПИК"

      val callbackOption = RedirectCallbackInfo(Seq.empty, Some(targetName))
      val redirectOptions = RedirectOptions.Empty.copy(callbackInfo = Some(callbackOption))
      val redirect = HistoryRedirectGen.next.copy(number = incomeRedirectPhone, options = Some(redirectOptions))

      when(sharedPoolService.get(equ(incomeRedirectPhone))(?)).thenReturn(Future.successful(number))
      when(redirectService.getOnTimeDomain(equ(incomeRedirectPhone), equ(time), equ(domain))(?))
        .thenReturn(Future.successful(Some(redirect)))

      when(afterHoursWithoutConfirmationSettings.templateReplacement).thenReturn("{{xxx}}")
      when(afterHoursWithoutConfirmationSettings.withoutConfirmationMessageTemplate).thenReturn(
        "Здравствуйте, {{xxx}}, спасибо за звонок!"
      )
      when(callbackSettings.afterHoursCallbackWithoutConfirmationSettings).thenReturn(
        afterHoursWithoutConfirmationSettings
      )
      when(dynamicProperties.getValue(domain, IsAfterHoursCallbackConfirmationEnabled)).thenReturn(false)

      val expected = VoxCallbackWithoutConfirmationInfo(
        redirectId = redirect.id.value,
        domain = domain,
        withoutConfirmationMessage =
          VoxCallbackInfo.TextMessage(text = "Здравствуйте, Компания ПИК, спасибо за звонок!")
      )

      service.getConfirmationInfo(incomeRedirectPhone, time).futureValue shouldBe expected
    }
  }

}
