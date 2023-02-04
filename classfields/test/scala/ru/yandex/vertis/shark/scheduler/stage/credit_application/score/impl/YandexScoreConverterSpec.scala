package ru.yandex.vertis.shark.scheduler.stage.credit_application.score.impl

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.proto.crypta.user_profile.Profile
import ru.yandex.proto.crypta.user_profile.Profile.ProfileItem
import ru.yandex.vertis.shark.model.{Tag, YandexScore}
import zio.clock.instant
import zio.test.Assertion.equalTo
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object YandexScoreConverterSpec extends DefaultRunnableSpec {

  private val ScoreKeywordId = 1084

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YandexScoreConverter")(
      testM("convert") {
        val items = Seq(
          ProfileItem(
            keywordId = ScoreKeywordId.some,
            weightedUintValues = Seq(
              Profile.WeightedUInt(first = 2357L.some, weight = 1L.some, updateTime = 10.some), // payment 6
              Profile.WeightedUInt(first = 2364L.some, weight = 2L.some, updateTime = 9.some), // older payment 7
              Profile.WeightedUInt(first = 2328L.some, weight = 3L.some, updateTime = 1.some), // approval 7
              Profile.WeightedUInt(first = 2626L.some, weight = 4L.some, updateTime = None), // approvalAuto 3
              Profile.WeightedUInt(first = 2512L.some, weight = 5L.some, updateTime = None) // bnpl 4
            )
          ),
          ProfileItem(
            keywordId = ScoreKeywordId.some,
            uintValues = Seq(2016232128L)
          )
        )
        val profile = Profile(items = items)
        val someHash = "some-hash".taggedWith[Tag.HashString]
        for {
          ts <- instant
          expected = YandexScore(
            paymentSegment = 6.some,
            approvalSegment = 7.some,
            approvalAutoSegment = 3.some,
            bnplSegment = 4.some,
            paymentWeight = 1d.some,
            approvalWeight = 3d.some,
            approvalAutoWeight = 4d.some,
            bnplWeight = 5d.some,
            timestamp = ts,
            sourceHash = someHash.some
          )
          score <- YandexScoreConverter.convert(profile, someHash.some, ts)
        } yield assert(score)(equalTo(expected))
      }
    )
}
