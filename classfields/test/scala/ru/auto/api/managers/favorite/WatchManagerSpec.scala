package ru.auto.api.managers.favorite

import org.scalacheck.Gen
import org.scalatest.BeforeAndAfter
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.model.{ModelGenerators, PersonalUserRef, RequestParams}
import ru.auto.api.services.subscriptions.WatchClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.subscriptions.api.ApiModel.{Deliveries, Watch, WatchPatch}
import ru.yandex.vertis.tracing.Traced
import org.mockito.Mockito.verify
import ru.yandex.vertis.broker.client.simple.BrokerClient

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class WatchManagerSpec extends BaseSpec with MockitoSupport with BeforeAndAfter {
  val watchClient: WatchClient = mock[WatchClient]
  val brokerClient: BrokerClient = mock[BrokerClient]

  val manager = new WatchManager(watchClient, brokerClient)
  implicit val trace: Traced = Traced.empty

  "WatchManager" should {
    "delete all watches for anon web client" in {
      implicit val request: Request = {
        val r = new RequestImpl
        r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.filter(_.nonEmpty).next)))
        r.setTrace(trace)
        r.setUser(ModelGenerators.AnonymousUserRefGen.next)
        r.setApplication(Application.web)
        r
      }
      val patternSet = Gen.listOfN(5, ModelGenerators.OfferIDGen).next.toSet
      val existedSet = Gen.listOfN(5, ModelGenerators.OfferIDGen).next.toSet
      val existedAsStrings = existedSet.map(_.toString)
      val watch = Watch
        .newBuilder()
        .addAllObjects(existedAsStrings.asJava)
        .build()
      var patch: WatchPatch = WatchPatch.getDefaultInstance

      when(watchClient.getWatch(?)(?)).thenReturnF(watch)
      stub(watchClient.patchWatch(_: PersonalUserRef, _: WatchPatch, _: Deliveries)(_: Traced)) {
        case (_, p, _, _) =>
          patch = p
          Future.unit
      }

      manager.syncWatchObjects(request.user.personalRef, patternSet).futureValue

      verify(watchClient).getWatch(request.user.personalRef)(trace)
      verify(watchClient).patchWatch(eq(request.user.personalRef), ?, ?)(eq(trace))

      patch.getRemoveList.asScala.toSet shouldEqual existedAsStrings
      patch.getAddCount shouldBe 0
    }
  }
}
