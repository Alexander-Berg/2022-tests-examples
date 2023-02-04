package ru.yandex.vertis.feedprocessor.autoru.scheduler.services.palma

import com.google.protobuf.any.{Any => ProtoAny}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures.{convertScalaFuture, whenReady}
import ru.auto.multiposting.settings_palma_model.MultipostingSettings
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting.FeedGeneratorService.ClientMultipostingStatus
import ru.yandex.vertis.palma.services.proto_dictionary_service.ProtoDictionaryApiModel.{EnrichedItem, ListResponse}
import ru.yandex.vertis.palma.services.proto_dictionary_service.ProtoDictionaryServiceGrpc.ProtoDictionaryService

import scala.concurrent.Future

class MultipostingSettingsServiceImplTest extends WordSpecBase with MockFactory {
  private val palmaClient = mock[ProtoDictionaryService]

  private val multipostingSettings: ProtoAny = ProtoAny.pack(MultipostingSettings(Seq[Long](123)))

  private val listResponse = ListResponse(
    Seq(
      EnrichedItem(
        multipostingSettings
      )
    )
  )

  private val clientForChecking = Set(123L, 777L)

  "MultipostingSettingsServiceImpl" when {
    "hasMultipostingSiteEnabled" should {
      val service = new MultipostingSettingsServiceImpl(palmaClient)
      "return tuples (clientId, isSettingsEnabled) in palma site settings" in {
        (palmaClient.list _).expects(*).returning(Future(listResponse))
        whenReady(service.hasMultipostingSiteEnabled(clientForChecking)) { result =>
          result shouldEqual List(
            ClientMultipostingStatus(clientId = 123, siteMultipostingStatus = true),
            ClientMultipostingStatus(clientId = 777, siteMultipostingStatus = false)
          )
        }
      }
      "throw wrapped error" in {
        (palmaClient.list _).expects(*).returns(Future.failed(new Exception("Some test exception")))
        val testResult = service.hasMultipostingSiteEnabled(clientForChecking).failed.futureValue
        testResult shouldBe a[MultipostingSettingsServiceImpl.MultipostingSettingsException]
        testResult.getMessage shouldBe "Cannot get site enabled settings from one of client id: 123, 777 in palma. Error: Some test exception"
      }
    }
  }
}
