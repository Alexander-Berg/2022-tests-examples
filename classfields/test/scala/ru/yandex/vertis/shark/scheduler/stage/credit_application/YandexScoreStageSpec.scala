package ru.yandex.vertis.shark.scheduler.stage.credit_application

import cats.implicits._
import com.softwaremill.tagging.Tagger
import ru.yandex.proto.crypta.user_profile.Profile
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.shark.client.bigbrother.BigBrotherClient
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.shark.model._
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.shark.proto.model.CreditApplication.State
import ru.yandex.vertis.shark.scheduler.stage.Stage
import ru.yandex.vertis.shark.scheduler.stage.credit_application.score.{ScoreConverter, ScoreStage}
import ru.yandex.vertis.zio_baker.util.DateTimeUtil.RichInstant
import ru.yandex.vertis.zio_baker.zio.features.DurationRange
import zio.test.Assertion.{anything, equalTo, isGreaterThanEqualTo, isLessThan, isNone}
import zio.test.environment.TestClock
import zio.test.mock.{mockable, Expectation, Mock}
import zio.test.{assert, assertM, mock, DefaultRunnableSpec, ZSpec}
import zio.{Has, Task, URIO, URLayer, ZIO, ZLayer}

import java.time.{Instant, ZoneOffset}
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.DurationInt

object YandexScoreStageSpec extends DefaultRunnableSpec with CreditApplicationGen {

  @mockable[BigBrotherClient.Service]
  private object BigBrotherClientMock

  private object YandexScoreConverterMock extends Mock[Has[ScoreConverter.Service[Profile, YandexScore]]] {

    object Convert extends Effect[(Profile, Instant), Throwable, YandexScore]

    val compose: URLayer[Has[mock.Proxy], Has[ScoreConverter.Service[Profile, YandexScore]]] =
      ZLayer.fromService { proxy =>
        new ScoreConverter.Service[Profile, YandexScore] {
          override def convert(source: Profile, sourceHash: Option[HashString], ts: Instant): Task[YandexScore] =
            proxy(Convert, source, ts)
        }
      }
  }

  private val ts = Instant.now
  private val someHash = "5619338c2cb5b25f3009288f809ffe6f".taggedWith[Tag.HashString]

  private val borrowerPersonProfile = PersonProfileImpl.forTest(
    phones = Block.PhonesBlock(Seq(Entity.PhoneEntity.forTest(Phone("+79652861010")))).some,
    emails = Block.EmailsBlock(Seq(Entity.EmailEntity(Email("ya@ya.ru")))).some
  )

  private val bigBrotherMockLayer =
    BigBrotherClientMock
      .GetProfile(
        anything,
        Expectation.value(Profile())
      )
      .optional

  private val expectedYandexScore: YandexScore =
    YandexScore(1.some, 2.some, None, None, None, None, None, None, ts, someHash.some)

  private val yandexScoreConverterLayer =
    YandexScoreConverterMock
      .Convert(
        anything,
        Expectation.value(expectedYandexScore)
      )
      .optional

  private def layer =
    TestClock.any ++ bigBrotherMockLayer ++ yandexScoreConverterLayer >>>
      YandexScoreStage.live(Domain.DOMAIN_AUTO)

  private val setTime: URIO[TestClock, Unit] = TestClock.setDateTime(ts.toOffsetDateTime(ZoneOffset.UTC))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("ScoreStage")(
      testM("don't process canceled") {
        val creditApplication = sampleAutoruCreditApplication().copy(
          state = State.CANCELED
        )
        val actual = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )
          .map(_.result)
        assertM(actual)(isNone).provideLayer(layer)
      },
      testM("don't process without phone and email") {
        val creditApplication = sampleAutoruCreditApplication().copy(
          state = State.ACTIVE,
          borrowerPersonProfile = PersonProfileImpl.forTest().some
        )
        val actual = ZIO
          .accessM[Stage[CreditApplication, CreditApplication.UpdateRequest]](
            _.get.process(creditApplication, ScheduleDurationRange)
          )
          .map(_.result)
        assertM(actual)(isNone).provideLayer(layer)
      },
      testM("don't process if schedule time didnt passed") {
        val testCase = for {
          lastUpdate <- Task.effect(ts.minus(6, ChronoUnit.DAYS))
          creditApplication = sampleAutoruCreditApplication().copy(
            state = State.ACTIVE,
            borrowerPersonProfile = borrowerPersonProfile.some,
            scores = Seq(YandexScore(3.some, 2.some, None, None, None, None, None, None, lastUpdate, someHash.some))
          )
          stage <- ZIO.service[Stage.Service[CreditApplication, CreditApplication.UpdateRequest]]
          _ <- setTime
          res <- stage.process(creditApplication, ScheduleDurationRange)
          expectedRechedule = ts.diff(lastUpdate.plusDuration(ScoreStage.DefaultRescheduleDuration))
        } yield {
          assert(res.result)(isNone) &&
          assert(res.reschedule)(isGreaterThanEqualTo(expectedRechedule.some)) &&
          assert(res.reschedule)(isLessThan(expectedRechedule.plus(5.seconds).some))
        }
        testCase.provideSomeLayer(layer)
      },
      testM("process yandex score if none") {
        val testCase = for {
          creditApplication <- Task.succeed {
            sampleAutoruCreditApplication().copy(
              state = State.ACTIVE,
              borrowerPersonProfile = borrowerPersonProfile.some,
              scores = Seq(OkbScore(2, ts.minus(10, ChronoUnit.DAYS), None))
            )
          }
          stage <- ZIO.service[Stage.Service[CreditApplication, CreditApplication.UpdateRequest]]
          _ <- setTime
          res <- stage.process(creditApplication, ScheduleDurationRange)
          scores = Seq(expectedYandexScore)
        } yield assert(res.result.map(_.scores))(equalTo(scores.some))
        testCase.provideSomeLayer(layer)
      },
      testM("process yandex score if expired") {
        val expiredTs = ts.minus(8, ChronoUnit.DAYS)
        val testCase = for {
          creditApplication <- Task.succeed {
            sampleAutoruCreditApplication().copy(
              state = State.ACTIVE,
              borrowerPersonProfile = borrowerPersonProfile.some,
              scores = Seq(
                OkbScore(1, expiredTs, someHash.some),
                YandexScore(1.some, 2.some, 3.some, 4.some, None, None, None, None, expiredTs, someHash.some)
              )
            )
          }
          stage <- ZIO.service[Stage.Service[CreditApplication, CreditApplication.UpdateRequest]]
          _ <- setTime
          res <- stage.process(creditApplication, ScheduleDurationRange)
          scores = Seq(expectedYandexScore)
        } yield assert(res.result.map(_.scores).orEmpty)(equalTo(scores))
        testCase.provideSomeLayer(layer)
      },
      testM("process yandex score if hash changed") {
        val notExpiredTs = ts.minus(5, ChronoUnit.DAYS)
        val oldHash = "old-hash".taggedWith[Tag.HashString]
        val testCase = for {
          creditApplication <- Task.succeed {
            sampleAutoruCreditApplication().copy(
              state = State.ACTIVE,
              borrowerPersonProfile = borrowerPersonProfile.some,
              scores = Seq(
                YandexScore(1.some, 2.some, 3.some, 4.some, None, None, None, None, notExpiredTs, oldHash.some)
              )
            )
          }
          stage <- ZIO.service[Stage.Service[CreditApplication, CreditApplication.UpdateRequest]]
          _ <- setTime
          res <- stage.process(creditApplication, ScheduleDurationRange)
          scores = Seq(expectedYandexScore)
        } yield assert(res.result.map(_.scores))(equalTo(scores.some))
        testCase.provideSomeLayer(layer)
      }
    )
  }

  private val ScheduleDurationRange: DurationRange = DurationRange(1.minute, 2.minutes)
}
