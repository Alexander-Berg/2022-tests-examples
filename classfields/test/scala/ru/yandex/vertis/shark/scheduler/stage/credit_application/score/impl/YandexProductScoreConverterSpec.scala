package ru.yandex.vertis.shark.scheduler.stage.credit_application.score.impl

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.client.saturn.SaturnClient
import ru.yandex.vertis.shark.model._
import zio.clock.instant
import zio.test.Assertion.equalTo
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object YandexProductScoreConverterSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YandexProductScoreConverter")(
      testM("convert") {
        val searchResponse = SaturnClient.SearchResponse(
          Seq(
            SaturnClient.SearchResponse.ProductEntity("alfabank-1", 0.15f),
            SaturnClient.SearchResponse.ProductEntity("tinkoff-1", 0.25f)
          )
        )
        val someHash = "some-hash".taggedWith[Tag.HashString]
        for {
          ts <- instant
          expected = YandexProductScore(
            Seq(
              YandexProductScore.ProductEntity("alfabank-1".taggedWith[Tag.CreditProductId], 0.15f),
              YandexProductScore.ProductEntity("tinkoff-1".taggedWith[Tag.CreditProductId], 0.25f)
            ),
            ts,
            someHash.some
          )
          score <- YandexProductScoreConverter.convert(searchResponse, someHash.some, ts)
        } yield assert(score)(equalTo(expected))
      }
    )
}
