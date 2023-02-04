package auto.dealers.calltracking.logic.test

import auto.dealers.calltracking.logic.CallsEnricher

import java.time.Instant
import zio._
import zio.test._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test.environment.TestEnvironment
import auto.dealers.calltracking.model.testkit.CallGen
import ru.auto.api.api_offer_model.{Category, Section}
import auto.common.clients.vos.testkit.DummyVosClient
import auto.dealers.calltracking.logic.CallsEnricher.EnrichFailure
import auto.dealers.calltracking.model.{CallInfo, CallInfoRequest}
import auto.dealers.calltracking.logic.repository.callinfo.CallInfoRepository

object CallsEnricherSpec extends DefaultRunnableSpec {

  override def spec = {
    suite("CallsEnricher")(
      testM("Enrich call with non empty platform") {
        val callInfoResponse =
          CallInfo(1, "", Category.CARS, Section.NEW, "DIRECT")
        val objectId = "dealer-123"
        val tag = "category=CARS#section=NEW"
        val callTime = Instant.EPOCH

        val callInfoRepository =
          ZLayer.succeed(Map(CallInfoRequest(objectId, tag, callTime) -> callInfoResponse)) >>>
            CallInfoRepository.test
        val layer = DummyVosClient.dummy ++ callInfoRepository >>> CallsEnricher.live

        checkNM(1)(
          CallGen.anyCall(callTime = Gen.const(callTime)),
          CallGen.anyTelepony(objectId = Gen.const(objectId), tag = Gen.const(tag))
        ) { (call, telepony) =>
          for {
            enriched <- CallsEnricher.enrich(call.copy(telepony = Some(telepony)))
          } yield {
            assert(enriched.platform)(equalTo(Some("direct")))
          }
        }.provideCustomLayer(TestEnvironment.live ++ layer)
      },
      testM("Set platform to AUTORU if telepony.platform is empty") {
        val callInfoResponse =
          CallInfo(1, "", Category.CARS, Section.NEW, "")
        val objectId = "dealer-123"
        val tag = "category=CARS#section=NEW"
        val callTime = Instant.EPOCH

        val callInfoRepository =
          ZLayer.succeed(Map(CallInfoRequest(objectId, tag, callTime) -> callInfoResponse)) >>>
            CallInfoRepository.test
        val layer = DummyVosClient.dummy ++ callInfoRepository >>> CallsEnricher.live

        checkNM(1)(
          CallGen.anyCall(callTime = Gen.const(callTime)),
          CallGen.anyTelepony(objectId = Gen.const(objectId), tag = Gen.const(tag))
        ) { (call, telepony) =>
          for {
            enriched <- CallsEnricher.enrich(call.copy(telepony = Some(telepony)))
          } yield {
            assert(enriched.platform)(equalTo(Some("autoru")))
          }
        }.provideCustomLayer(TestEnvironment.live ++ layer)
      },
      testM("fail if call info not found") {

        val callInfoRepository =
          ZLayer.succeed(Map.empty[CallInfoRequest, CallInfo]) >>> CallInfoRepository.test
        val layer = DummyVosClient.dummy ++ callInfoRepository >>> CallsEnricher.live

        checkNM(1)(CallGen.anyCall, CallGen.anyTelepony()) { (call, telepony) =>
          val action = CallsEnricher.enrich(call.copy(telepony = Some(telepony)))
          assertM(action.run)(fails(isSubtype[EnrichFailure](anything)))
        }.provideCustomLayer(TestEnvironment.live ++ layer)
      }
    ) @@ sequential
  }
}
