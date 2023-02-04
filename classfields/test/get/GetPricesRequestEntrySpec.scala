package ru.yandex.vertis.billing.howmuch.model.get

import billing.common_model.Project
import billing.howmuch.model.{RequestContext, RequestCriteria}
import billing.howmuch.{price_service => proto}
import cats.data.NonEmptyList
import common.time.Interval
import common.zio.testkit.failsWith
import ru.yandex.vertis.billing.howmuch.model.core._
import ru.yandex.vertis.billing.howmuch.model.core.criteria.CriteriaKey
import ru.yandex.vertis.billing.howmuch.model.error.BadRequest.EmptyRequestContext
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant

object GetPricesRequestEntrySpec extends DefaultRunnableSpec {

  private val testProject = Project.AUTORU
  private val testMatrixId = "call"
  private val testFrom = Instant.ofEpochMilli(100500)
  private val testEntryId = "1234"

  private val testSchema =
    Schema(
      List(
        TimedSchema(
          MatrixId(testProject, testMatrixId),
          Interval(from = testFrom, to = None),
          ContextSchema(List(CriteriaKey("mark"), CriteriaKey("model"))),
          allowFallback = true
        )
      ),
      historyUploadAllowed = Set()
    )

  override def spec: ZSpec[TestEnvironment, Any] = suite("GetPricesRequestEntrySpec")(
    testM("parse entry valid by schema") {
      val expected = GetPricesRequestEntry(
        MatrixId(testProject, testMatrixId),
        NonEmptyList.of(
          Context.fromString("mark=AUDI&model=A3"),
          Context.fromString("mark=AUDI&model->*"),
          Context.fromString("mark->*&model->*")
        )
      )
      val actual =
        parseProtoEntry(Some(RequestContext(Seq(RequestCriteria("mark", "AUDI"), RequestCriteria("model", "A3")))))
      assertM(actual)(equalTo(expected))
    },
    testM("fail if requestContext is empty") {
      assertM(parseProtoEntry(context = None).run)(failsWith[EmptyRequestContext])
    }
  )

  private def testProtoEntry(context: Option[RequestContext]) =
    proto.GetPricesRequestEntry(testEntryId, testMatrixId, context)

  private def parseProtoEntry(context: Option[RequestContext]) =
    GetPricesRequestEntry.parseProtoEntry(testSchema, testProject, testFrom, testProtoEntry(context))
}
