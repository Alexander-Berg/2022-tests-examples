package ru.yandex.vertis.moderation.api.v1.service.external

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.mockito.ArgumentMatchers.{any => argAny}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Diff
import ru.yandex.vertis.moderation.model.signal.HoboSignalSource
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.searcher.core.model.Sort
import ru.yandex.vertis.moderation.searcher.core.saas.search.SearchQuery
import ru.yandex.vertis.moderation.util.{HandlerSpecBase, Slice}
import ru.yandex.vertis.moderation.view.ViewCompanion.MarshallingContext

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class ExtObjectHandlerSpec extends HandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.{BadRequest, NotFound, OK}

  private val service: Service = ServiceGen.next
  private val apiService = environmentRegistry(service).apiService
  private val instanceDao = environmentRegistry(service).instanceDao

  override def basePath: String = s"/api/1.x/$service/ext-object"

  "last" should {

    "invoke correct method" in {
      val instance = InstanceGen.next
      updateInstanceDao(instance, instanceDao)()
      val externalId = instance.externalId
      Get(url(s"/${externalId.user.key}/${externalId.objectId}")) ~> route ~> check {
        status shouldBe OK
        there.was(one(apiService).get(externalId, allowExpired = false))
      }
    }

    "invoke correct method if allow expired" in {
      val instance = InstanceGen.next
      updateInstanceDao(instance, instanceDao)()
      val externalId = instance.externalId
      Get(url(s"/${externalId.user.key}/${externalId.objectId}?allow_expired=true")) ~> route ~> check {
        status shouldBe OK
        there.was(one(apiService).get(externalId, allowExpired = true))
      }
    }

    "return 404 if no such instance" in {
      val externalId = ExternalIdGen.next
      Get(url(s"/${externalId.user.key}/${externalId.objectId}")) ~> route ~> check {
        status shouldBe NotFound
      }
    }
  }

  "lastByFilter" should {

    "invoke correct method" in {
      Get(url(s"/?page_number=0&page_size=1&sort=i_update_date_sort&opinion=Unknown")) ~> route ~> check {
        status shouldBe OK
        there.was(one(apiService).list(argAny[SearchQuery], argAny[Slice], argAny[Sort]))
      }
    }

    "return 400 if no sort parameter" in {
      Get(url(s"/?page_number=0&page_size=1&opinion=Unknown")) ~> route ~> check {
        status shouldBe BadRequest
      }
    }
  }

  "updateLastByFilter" should {

    "fail with 400 if signals contain hobo" in {
      val signalSources =
        Seq(
          SignalSourceGen
            .suchThat {
              case _: HoboSignalSource => true
              case _                   => false
            }
            .withoutMarker
            .next
        )
      Put(url("/signal?opinion=Unknown"), signalSources) ~> route ~> check {
        status shouldBe BadRequest
      }
    }
  }

  "sendToConsumer" should {

    "invoke correct method" in {
      val instance = InstanceGen.next
      val externalId = instance.externalId
      updateInstanceDao(instance, instanceDao)()
      Put(url(s"/${externalId.user.key}/${externalId.objectId}"), Seq("OPINION")) ~> route ~> check {
        status shouldBe OK
        there.was(one(apiService).writeToUpdateJournal(externalId, Diff.opinion(service)))
      }
    }
  }

  implicit override def marshallingContext: MarshallingContext = MarshallingContext(service)
}
