package vertis.statist.api.http.v1.domains.counters

import akka.http.scaladsl.model.{StatusCodes, Uri}
import common.zio.ops.tracing.Tracing
import ru.yandex.vertis.statist.model.api.ApiModel
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport
import vertis.statist.api.http.BaseHandlerSpec
import vertis.statist.api.http.v1.proto.ApiProtoFormats
import vertis.statist.dao.TestCounterDao
import vertis.statist.model._
import vertis.statist.dao.TestCounterDao._
import vertis.statist.service.counter.CounterServiceImpl
import zio._

/** @author zvez
  */
class CounterHandlerSpec extends BaseHandlerSpec with ApiProtoFormats with ProtobufSupport {

  private val service = new CounterServiceImpl(TestCounterDao.default)

  private val route = runSync {
    ZIO
      .service[Tracing.Service]
      .map { tr =>
        seal(
          new CounterHandler(service, tr)(Runtime.default)
        )
      }
  }.get

  "GET /components/multiple/composite" should {

    "return value" in {
      val ids = Seq(IdA, IdB)
      val components = Seq(CardShow, PhoneShow)

      val expectedResult =
        MultipleCompositeValues(
          Map(
            IdA -> ObjectCompositeValues(
              Map(
                CardShow -> CounterCompositeValue(3, 2),
                PhoneShow -> CounterCompositeValue(0, 0)
              )
            ),
            IdB -> ObjectCompositeValues(
              Map(
                CardShow -> CounterCompositeValue(4, 4),
                PhoneShow -> CounterCompositeValue(1, 1)
              )
            )
          )
        )

      val queryBuilder = Uri.Query.newBuilder
      ids.foreach(id => queryBuilder += ("id" -> id))
      components.foreach(c => queryBuilder += ("component" -> c))
      val uri = Uri("/components/multiple/composite").withQuery(queryBuilder.result())

      Get(uri) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[MultipleCompositeValues] shouldBe expectedResult
      }
    }
  }

  "GET /components/multiple/by-day" should {

    "return value" in {
      val ids = Seq(IdA, IdB)
      val components = Seq(CardShow, PhoneShow)

      val expectedResult =
        MultipleDailyValues(
          Map(
            IdA -> ObjectDailyValues(
              Map(
                yesterday ->
                  ObjectCounterValues(Map(CardShow -> 1, PhoneShow -> 0)),
                today ->
                  ObjectCounterValues(Map(CardShow -> 2, PhoneShow -> 0))
              )
            ),
            IdB -> ObjectDailyValues(
              Map(
                yesterday -> ObjectCounterValues(Map(CardShow -> 0, PhoneShow -> 0)),
                today -> ObjectCounterValues(Map(CardShow -> 4, PhoneShow -> 1))
              )
            )
          )
        )

      val queryBuilder = Uri.Query.newBuilder
      ids.foreach(id => queryBuilder += ("id" -> id))
      components.foreach(c => queryBuilder += ("component" -> c))
      val uri = Uri("/components/multiple/by-day").withQuery(queryBuilder.result())

      Get(uri) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[MultipleDailyValues] shouldBe expectedResult
      }
    }
  }
}

object CounterHandlerSpec {

  final val BarFilters = ApiModel.FieldFilters
    .newBuilder()
    .addFilters(
      ApiModel.FieldFilter
        .newBuilder()
        .setField("field")
        .setEquals(
          ApiModel.FieldFilter.Equals
            .newBuilder()
            .setValue(
              ApiModel.FieldFilter.FieldValue
                .newBuilder()
                .setStringValue("bar")
            )
        )
    )
    .build()
}
