package common.clients.spellchecker.test

import common.clients.spellchecker.model.SpellcheckerFlag.{Keyboard, Porno, PornoCorrection, Split}
import common.clients.spellchecker.model.SpellcheckerLanguageSetting.{AllLanguages, AllLanguagesWithPriority, Russian}
import common.clients.spellchecker.model.SpellcheckerOption.PornoRule
import common.clients.spellchecker.model.SpellcheckerReliability.Unreliable
import common.clients.spellchecker.model.SpellcheckerResult
import common.clients.spellchecker.model.SpellcheckerResult.MisspellCorrection
import common.clients.spellchecker.{SpellcheckerClient, SpellcheckerClientLive}
import common.zio.sttp.endpoint.Endpoint
import common.zio.app.Application
import common.zio.sttp.Sttp
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.test.Assertion._
import zio.test._

import scala.io.Source

object SpellcheckerClientLiveSpec extends DefaultRunnableSpec {

  private val stubWithCorrectedText = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("corrected_answer.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  private val stubWithNotCorrectedText = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("not_corrected_answer.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SpellcheckerClientLive")(
      testM("parse response with corrected text") {
        for {
          response <- SpellcheckerClient.check(
            "пронография vjcrdfискат",
            AllLanguagesWithPriority(List(Russian)),
            Set(PornoRule)
          )
        } yield assert(response)(
          equalTo(
            SpellcheckerResult(
              Set(Porno, PornoCorrection, Keyboard, Split),
              Some(
                MisspellCorrection(
                  "пронография vjcrdfискат",
                  "порнография москва искать",
                  Unreliable
                )
              )
            )
          )
        )
      }.provideCustomLayer(
        (Endpoint.testEndpointLayer ++ Sttp.fromStub(stubWithCorrectedText) ++ Application.live.orDie) >>>
          SpellcheckerClientLive.live
      ),
      testM("parse response with not corrected text") {
        for {
          response <- SpellcheckerClient.check(
            "телефон",
            AllLanguages,
            Set.empty
          )
        } yield assert(response)(
          equalTo(
            SpellcheckerResult(Set.empty, None)
          )
        )
      }.provideCustomLayer(
        (Endpoint.testEndpointLayer ++ Sttp.fromStub(stubWithNotCorrectedText) ++ Application.live.orDie) >>>
          SpellcheckerClientLive.live
      )
    )
  }
}
