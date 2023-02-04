package ru.yandex.vertis.billing.howmuch.model.get

import billing.common_model.Project
import billing.howmuch.model.RequestContext
import billing.howmuch.model.testkit.mkRequestContext
import billing.howmuch.{price_service => proto}
import cats.data.{NonEmptyList, NonEmptyMap}
import common.scalapb.ScalaProtobuf.instantToTimestamp
import common.time.Interval
import common.zio.testkit.failsWith
import ru.yandex.vertis.billing.howmuch.model.core._
import ru.yandex.vertis.billing.howmuch.model.core.criteria._
import ru.yandex.vertis.billing.howmuch.model.error.BadRequest.{
  EmptyGetEntries,
  EmptyGetProject,
  EmptyGetTimestamp,
  MultipleEntriesForSameEntryId
}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object GetPricesRequestSpec extends DefaultRunnableSpec {

  private val testProject = Project.AUTORU
  private val testMatrixId = "call"
  private val testActiveAt = Instant.ofEpochMilli(100500)

  private val testSchema =
    Schema(
      List(
        TimedSchema(
          MatrixId(testProject, testMatrixId),
          Interval(from = testActiveAt, to = None),
          ContextSchema(List(CriteriaKey("mark"), CriteriaKey("model"))),
          allowFallback = false
        )
      ),
      historyUploadAllowed = Set()
    )

  override def spec: ZSpec[TestEnvironment, Any] = suite("GetPricesRequestSpec")(
    testM("parse proto request successfully if non-empty timestamp & non-empty entries with unique entry ids") {
      val request = testProtoRequest(
        Seq(
          testProtoEntry(EntryId("1"), mkRequestContext("mark" -> "AUDI", "model" -> "A3")),
          testProtoEntry(EntryId("2"), mkRequestContext("mark" -> "AUDI", "model" -> "A4")),
          testProtoEntry(EntryId("3"), mkRequestContext("mark" -> "AUDI", "model" -> "A5"))
        )
      )
      val expected = GetPricesRequest(
        NonEmptyMap.of(
          EntryId("1") -> testEntry(Context.fromString("mark=AUDI&model=A3")),
          EntryId("2") -> testEntry(Context.fromString("mark=AUDI&model=A4")),
          EntryId("3") -> testEntry(Context.fromString("mark=AUDI&model=A5"))
        ),
        testActiveAt
      )
      assertM(parseProtoRequest(request))(equalTo(expected))
    },
    testM("fail if empty timestamp") {
      val request = testProtoRequest(
        Seq(testProtoEntry(EntryId("1234"), mkRequestContext("mark" -> "AUDI", "model" -> "A3")))
      ).copy(timestamp = None)
      assertM(parseProtoRequest(request).run)(failsWith[EmptyGetTimestamp])
    },
    testM("fail if empty entries") {
      val request = testProtoRequest(entries = Nil)
      assertM(parseProtoRequest(request).run)(failsWith[EmptyGetEntries])
    },
    testM("fail if two entries with same id") {
      val request = testProtoRequest(
        Seq(
          testProtoEntry(EntryId("1"), mkRequestContext("mark" -> "AUDI", "model" -> "A3")),
          testProtoEntry(EntryId("1"), mkRequestContext("mark" -> "AUDI", "model" -> "A4")),
          testProtoEntry(EntryId("2"), mkRequestContext("mark" -> "AUDI", "model" -> "A5"))
        )
      )
      assertM(parseProtoRequest(request).run)(failsWith[MultipleEntriesForSameEntryId])
    },
    testM("fails if provided project is unknown (should be validated first)") {
      val request = proto.GetPricesRequest(Project.UNKNOWN_PROJECT)
      assertM(parseProtoRequest(request).run)(failsWith[EmptyGetProject])
    },
    testM("fails if provided project is unrecognized (should be validated first)") {
      val request = proto.GetPricesRequest(Project.Unrecognized(100500))
      assertM(parseProtoRequest(request).run)(failsWith[EmptyGetProject])
    }
  )

  private def testProtoEntry(entryId: EntryId, requestContext: RequestContext) =
    proto.GetPricesRequestEntry(entryId.id, testMatrixId, Some(requestContext))

  private def testEntry(context: Context) =
    GetPricesRequestEntry(MatrixId(testProject, testMatrixId), NonEmptyList.one(context))

  private def testProtoRequest(entries: Seq[proto.GetPricesRequestEntry]) =
    proto.GetPricesRequest(testProject, entries, Some(instantToTimestamp(testActiveAt)))

  private def parseProtoRequest(request: proto.GetPricesRequest) =
    GetPricesRequest.parseProtoRequest(testSchema, request)
}
