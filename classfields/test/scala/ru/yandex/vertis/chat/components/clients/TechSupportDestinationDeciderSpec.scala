package ru.yandex.vertis.chat.components.clients

import org.mockito.Mockito._
import ru.yandex.vertis.MimeType
import ru.yandex.vertis.chat.RequestContext
import ru.yandex.vertis.chat.components.ComponentsSpecBase
import ru.yandex.vertis.chat.components.clients.techsupport.TechSupportDestinationDecider.Destination
import ru.yandex.vertis.chat.components.clients.techsupport.AutoTechSupportDestinationDecider
import ru.yandex.vertis.chat.components.dao.chat.techsupport.experimentassistant.ExperimentAssistantBuyers
import ru.yandex.vertis.chat.components.dao.chat.techsupport.yandexstaff.YandexStaff
import ru.yandex.vertis.chat.components.workersfactory.workers.{TestWorkersFactory, WorkersFactory}
import ru.yandex.vertis.chat.model.{MessagePayload, ModelGenerators, UserId}
import ru.yandex.vertis.chat.service.CreateMessageParameters
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.service.features.ChatFeatures
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.chat.util.DMap
import ru.yandex.vertis.chat.Domain
import ru.yandex.vertis.chat.Domains

class TechSupportDestinationDeciderSpec extends ComponentsSpecBase with MockitoSupport {

  private val features = mock[ChatFeatures]
  private val DirectJivositeSupportFeature = mock[Feature[Boolean]]
  private val ExperimentAssistantFeature = mock[Feature[Boolean]]
  private val YandexStaffFeature = mock[Feature[Boolean]]

  implicit private val rc: RequestContext = mock[RequestContext]

  private val decider = newDecider()

  private def newDecider(sellers: Seq[UserId] = Seq.empty,
                         buyers: Seq[UserId] = Seq.empty,
                         staff: Seq[UserId] = Seq.empty) =
    new AutoTechSupportDestinationDecider(features) {

      implicit override def domain: Domain = Domains.Auto

      override def workersFactory: WorkersFactory = new TestWorkersFactory

      override def experimentAssistantSellers: Seq[UserId] = {
        sellers
      }

      override def experimentAssistantBuyers =
        DMap.forAllDomains(ExperimentAssistantBuyers(buyers))

      override def yandexStaff = DMap.forAllDomains(YandexStaff(staff))
    }

  "TechSupportDestinationDecider" should {

    "Send user to jivosite when feature enabled" in {
      val user = ModelGenerators.userId.next
      when(features.DirectJivositeSupport).thenReturn(DirectJivositeSupportFeature)
      when(DirectJivositeSupportFeature.value).thenReturn(true)
      val params4User = CreateMessageParameters("room", user, MessagePayload(MimeType.TEXT_PLAIN, ""), Seq(), None)
      decider.destination(params4User) should equal(Destination.JivositePrivate)
      verify(features).DirectJivositeSupport
      verify(DirectJivositeSupportFeature).value
      reset(features, DirectJivositeSupportFeature, rc)
    }

    "Send dealer to jivosite when feature enabled" in {
      val dealer = ModelGenerators.dealerId.next
      when(features.DirectJivositeSupport).thenReturn(DirectJivositeSupportFeature)
      when(DirectJivositeSupportFeature.value).thenReturn(true)
      val params4Dealer = CreateMessageParameters("room", dealer, MessagePayload(MimeType.TEXT_PLAIN, ""), Seq(), None)
      decider.destination(params4Dealer) should equal(Destination.JivositeDealers)
      verify(features).DirectJivositeSupport
      verify(DirectJivositeSupportFeature).value
      reset(features, DirectJivositeSupportFeature, rc)
    }

    "Send dealer to jivosite when feature disabled" in {
      val dealer = ModelGenerators.dealerId.next
      when(features.DirectJivositeSupport).thenReturn(DirectJivositeSupportFeature)
      when(DirectJivositeSupportFeature.value).thenReturn(false)
      val params4Dealer = CreateMessageParameters("room", dealer, MessagePayload(MimeType.TEXT_PLAIN, ""), Seq(), None)
      decider.destination(params4Dealer) should equal(Destination.JivositeDealers)
      verify(features).DirectJivositeSupport
      verify(DirectJivositeSupportFeature).value
      reset(features, DirectJivositeSupportFeature, rc)
    }

    "Send user to techsupport when feature disabled" in {
      val user = ModelGenerators.userId.next
      when(features.DirectJivositeSupport).thenReturn(DirectJivositeSupportFeature)
      when(DirectJivositeSupportFeature.value).thenReturn(false)
      val params4User = CreateMessageParameters("room", user, MessagePayload(MimeType.TEXT_PLAIN, ""), Seq(), None)
      decider.destination(params4User) should equal(Destination.VertisTechsupport)
      verify(features).DirectJivositeSupport
      verify(DirectJivositeSupportFeature).value
      reset(features, DirectJivositeSupportFeature, rc)
    }

    "Send user to jivosite when sellers contains userId" in {
      when(features.ExperimentAssistant).thenReturn(ExperimentAssistantFeature)
      when(ExperimentAssistantFeature.value).thenReturn(true)
      val user = ModelGenerators.userId.next
      val decider = newDecider(sellers = Seq(user))
      val params4User = CreateMessageParameters("room", user, MessagePayload(MimeType.TEXT_PLAIN, ""), Seq(), None)
      decider.destination(params4User) should equal(Destination.JivositeExperimentAssistantUsers)
    }

    "Send user to jivosite when buyers contains userId" in {
      when(features.ExperimentAssistant).thenReturn(ExperimentAssistantFeature)
      when(ExperimentAssistantFeature.value).thenReturn(true)
      val user = ModelGenerators.userId.next
      val decider = newDecider(buyers = Seq(user))
      val params4User = CreateMessageParameters("room", user, MessagePayload(MimeType.TEXT_PLAIN, ""), Seq(), None)
      decider.destination(params4User) should equal(Destination.JivositeExperimentAssistantUsers)
    }

    "Send user to jivosite when staff contains userId" in {
      when(features.YandexStaff).thenReturn(YandexStaffFeature)
      when(YandexStaffFeature.value).thenReturn(true)
      val user = ModelGenerators.userId.next
      val decider = newDecider(staff = Seq(user))
      val params4User = CreateMessageParameters("room", user, MessagePayload(MimeType.TEXT_PLAIN, ""), Seq(), None)
      decider.destination(params4User) should equal(Destination.JivositeYandexStaff)
    }

    "Not send user to jivosite when sellers contains userId and feature disabled" in {
      when(features.ExperimentAssistant).thenReturn(ExperimentAssistantFeature)
      when(ExperimentAssistantFeature.value).thenReturn(false)
      when(features.DirectJivositeSupport).thenReturn(DirectJivositeSupportFeature)
      when(DirectJivositeSupportFeature.value).thenReturn(false)
      val user = ModelGenerators.userId.next
      val decider = newDecider(sellers = Seq(user))
      val params4User = CreateMessageParameters("room", user, MessagePayload(MimeType.TEXT_PLAIN, ""), Seq(), None)
      decider.destination(params4User) should equal(Destination.VertisTechsupport)
    }

    "Not send user to jivosite when buyers contains userId and feature disabled" in {
      when(features.ExperimentAssistant).thenReturn(ExperimentAssistantFeature)
      when(ExperimentAssistantFeature.value).thenReturn(false)
      when(features.DirectJivositeSupport).thenReturn(DirectJivositeSupportFeature)
      when(DirectJivositeSupportFeature.value).thenReturn(false)
      val user = ModelGenerators.userId.next
      val decider = newDecider(buyers = Seq(user))
      val params4User = CreateMessageParameters("room", user, MessagePayload(MimeType.TEXT_PLAIN, ""), Seq(), None)
      decider.destination(params4User) should equal(Destination.VertisTechsupport)
    }

    "Not send user to jivosite when staff contains userId and feature disabled" in {
      when(features.YandexStaff).thenReturn(YandexStaffFeature)
      when(YandexStaffFeature.value).thenReturn(false)
      when(features.DirectJivositeSupport).thenReturn(DirectJivositeSupportFeature)
      when(DirectJivositeSupportFeature.value).thenReturn(false)
      val user = ModelGenerators.userId.next
      val decider = newDecider(staff = Seq(user))
      val params4User = CreateMessageParameters("room", user, MessagePayload(MimeType.TEXT_PLAIN, ""), Seq(), None)
      decider.destination(params4User) should equal(Destination.VertisTechsupport)
    }
  }
}
