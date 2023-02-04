package ru.yandex.vertis.billing.howmuch.model.patch

import billing.common_model.{Money, Project}
import billing.howmuch.model.Source.{Source => ProtoSource}
import billing.howmuch.model.{Source => OuterProtoSource}
import billing.howmuch.price_service.PatchPricesRequest.From
import billing.howmuch.price_service.PatchPricesRequest.From.{FromFuture, FromNow}
import billing.howmuch.price_service.PatchPricesRequestEntry.Patch.NextPrice
import billing.howmuch.{price_service => proto}
import cats.data.NonEmptyList
import com.google.protobuf.timestamp.Timestamp
import common.time.Interval
import common.zio.testkit.failsWith
import ru.yandex.vertis.billing.common.money.Kopecks
import billing.howmuch.model.testkit._
import ru.yandex.vertis.billing.howmuch.model.core._
import ru.yandex.vertis.billing.howmuch.model.core.criteria.CriteriaKey
import ru.yandex.vertis.billing.howmuch.model.error.BadRequest.{
  EmptyPatchEntries,
  EmptyPatchFrom,
  EmptyPatchProject,
  EmptyPatchSource,
  MultipleEntriesForSameRule,
  PatchFromPast
}
import ru.yandex.vertis.billing.howmuch.model.patch.PatchPricesRequestEntry.Create
import common.zio.ops.tracing.RequestId
import zio.test.Assertion._
import zio.test._
import zio.test.environment.{TestClock, TestEnvironment}

import java.time.{Instant, OffsetDateTime, ZoneOffset}

object PatchPricesRequestSpec extends DefaultRunnableSpec {

  private val testProject = Project.AUTORU
  private val testMatrixId = "call"
  private val testKey = RuleKey(MatrixId(testProject, testMatrixId), Context.fromString("mark=AUDI&model=TT"))
  private val beforeTestFrom = OffsetDateTime.parse("2021-04-28T12:34+03:00")
  private val testFrom = Instant.parse("2021-04-30T21:00:00Z")
  private val afterTestFrom = OffsetDateTime.parse("2021-05-01T12:34+03:00")
  private val testRuleContext = Some(mkRuleContext("mark" -> "AUDI", "model" -> "TT"))
  private val testSource = Source.StartrekTicket("VSMONEY-2750")
  private val testProtoSource = OuterProtoSource(ProtoSource.StartrekTicket("VSMONEY-2750"))

  private val testSchema =
    Schema(
      List(
        TimedSchema(
          MatrixId(testProject, testMatrixId),
          Interval(from = testFrom, to = None),
          ContextSchema(List(CriteriaKey("mark"), CriteriaKey("model"))),
          allowFallback = false
        )
      ),
      historyUploadAllowed = Set()
    )

  private val testProtoEntry = proto.PatchPricesRequestEntry(
    testMatrixId,
    testRuleContext,
    previousPrice = None,
    patch = NextPrice(Money(6000))
  )

  private val testRule = {
    Rule(
      testKey,
      Interval(testFrom, to = None),
      Kopecks(6000),
      testSource
    )
  }

  override def spec: ZSpec[TestEnvironment, Any] = suite("PatchPricesRequestSpec")(
    testM("return PatchPricesRequest for patch with `from` in future") {
      for {
        _ <- TestClock.setDateTime(beforeTestFrom)
        parsed <- parseProtoRequest(
          testRequest(
            FromFuture(Timestamp(testFrom.getEpochSecond))
          )
        )
      } yield assert(parsed)(
        equalTo(PatchPricesRequest(NonEmptyList.of(Create(testRule)), testFrom))
      )
    },
    testM("fail for patch with empty entries list") {
      val effect =
        parseProtoRequest(
          testRequest(
            FromFuture(Timestamp(testFrom.getEpochSecond))
          ).copy(entries = Nil)
        )
      assertM(effect.run)(failsWith[EmptyPatchEntries])
    },
    testM("fail for patch with no source") {
      val effect =
        parseProtoRequest(
          testRequest(
            FromFuture(Timestamp(testFrom.getEpochSecond))
          ).copy(source = None)
        )
      assertM(effect.run)(failsWith[EmptyPatchSource])
    },
    testM("fail for patch with `from` in the past") {
      val effect =
        TestClock.setDateTime(afterTestFrom) *>
          parseProtoRequest(testRequest(FromFuture(Timestamp(testFrom.getEpochSecond))))
      assertM(effect.run)(failsWith[PatchFromPast])
    },
    testM("return PatchPricesRequest for patch with `from` = now") {
      for {
        _ <- TestClock.setDateTime(testFrom.atOffset(ZoneOffset.UTC))
        parsed <- parseProtoRequest(testRequest(FromNow(true)))
      } yield assert(parsed)(
        equalTo(PatchPricesRequest(NonEmptyList.of(Create(testRule)), testFrom))
      )
    },
    testM("fail for patch with FromNow(false)") {
      val effect = parseProtoRequest(testRequest(FromNow(false)))
      assertM(effect.run)(failsWith[EmptyPatchFrom])
    },
    testM("fail for patch without from") {
      val effect = parseProtoRequest(testRequest(From.Empty))
      assertM(effect.run)(failsWith[EmptyPatchFrom])
    },
    testM("fail for patch with two different elements for the same rule key") {
      val effect =
        TestClock.setDateTime(afterTestFrom) *>
          parseProtoRequest(
            testRequest(FromNow(true))
              .copy(entries = Seq(testProtoEntry, testProtoEntry.copy(patch = NextPrice(Money(100500)))))
          )
      assertM(effect.run)(failsWith[MultipleEntriesForSameRule])
    },
    testM("deduplicate two equal elements for the same rule key in the patch") {
      for {
        _ <- TestClock.setDateTime(testFrom.atOffset(ZoneOffset.UTC))
        parsed <-
          parseProtoRequest(
            testRequest(FromNow(true))
              .copy(entries = Seq(testProtoEntry, testProtoEntry))
          )
      } yield assertTrue(parsed == PatchPricesRequest(NonEmptyList.of(Create(testRule)), testFrom))
    },
    testM("return two elements for different rule keys") {
      val effect =
        TestClock.setDateTime(beforeTestFrom) *>
          parseProtoRequest(
            testRequest(FromFuture(Timestamp(testFrom.getEpochSecond))).copy(entries =
              Seq(
                testProtoEntry.copy(context = Some(mkRuleContext("mark" -> "AUDI", "model" -> "Q7"))),
                testProtoEntry.copy(context = Some(mkRuleContext("mark" -> "AUDI", "model" -> "A3")))
              )
            )
          )
      val expectedKey1 = testKey.copy(context = Context.fromString("mark=AUDI&model=Q7"))
      val expectedKey2 = testKey.copy(context = Context.fromString("mark=AUDI&model=A3"))
      assertM(effect)(
        equalTo(
          PatchPricesRequest(
            NonEmptyList
              .of(
                Create(testRule.copy(key = expectedKey1)),
                Create(testRule.copy(key = expectedKey2))
              ),
            testFrom
          )
        )
      )
    },
    testM("fails if provided project is unknown (should be validated first)") {
      val request = proto.PatchPricesRequest(Project.UNKNOWN_PROJECT)
      assertM(parseProtoRequest(request).run)(failsWith[EmptyPatchProject])
    },
    testM("fails if provided project is unrecognized (should be validated first)") {
      val request = proto.PatchPricesRequest(Project.Unrecognized(666))
      assertM(parseProtoRequest(request).run)(failsWith[EmptyPatchProject])
    }
  ).provideCustomLayerShared(RequestId.test(None))

  private def parseProtoRequest(request: proto.PatchPricesRequest) =
    PatchPricesRequest.parseProtoRequest(testSchema, request)

  private def testRequest(from: From) =
    proto.PatchPricesRequest(testProject, from, Seq(testProtoEntry), Some(testProtoSource))
}
