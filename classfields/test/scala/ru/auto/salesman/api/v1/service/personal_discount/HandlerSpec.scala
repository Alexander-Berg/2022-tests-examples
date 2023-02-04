package ru.auto.salesman.api.v1.service.personal_discount

import akka.http.javadsl.model.StatusCodes
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.auto.salesman.api.v1.HandlerBaseSpec
import ru.auto.salesman.api.v1.SalesmanApiUtils.SalesmanHttpRequest
import ru.auto.salesman.model.DeprecatedDomains.AutoRu
import ru.auto.salesman.model.push._
import ru.auto.salesman.model.user.PersonalDiscount.{
  CreatePersonalDiscountRequest,
  PushRequest => ProtoPushRequest
}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.Turbo
import ru.auto.salesman.model.{AutoruUser, DeprecatedDomain}
import ru.auto.salesman.service.user.personal_discount.PersonalDiscountService.PersonalDiscountModel
import ru.auto.salesman.util.DateTimeInterval
import ru.yandex.vertis.util.time.DateTimeUtil

class HandlerSpec extends HandlerBaseSpec {

  private val validRequest = CreatePersonalDiscountRequest
    .newBuilder()
    .setUser("user:123")
    .setProduct("turbo-package")
    .setDiscount(70)
    .setFrom(Timestamps.parse("2020-10-14T21:00:00Z"))
    .setTo(Timestamps.parse("2020-10-15T20:59:59Z"))
    .build()

  private val expectedValidModel = PersonalDiscountModel(
    AutoruUser("user:123"),
    Turbo,
    70,
    DateTimeInterval(
      DateTime
        .parse("2020-10-15T00:00:00.000+03:00")
        .withZone(DateTimeUtil.DefaultTimeZone),
      DateTime
        .parse("2020-10-15T23:59:59.000+03:00")
        .withZone(DateTimeUtil.DefaultTimeZone)
    ),
    None,
    Some(""),
    None
  )

  "PersonalDiscountHandler" should {

    "respond with OK on valid request and successful service work" in {
      mockSuccessfulCreatePersonalDiscount()
      Post("/api/1.x/service/autoru/discount/personal", validRequest)
        .withSalesmanTestHeader() ~> route ~> check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "do right field converting" in {
      val pushRequest = ProtoPushRequest.newBuilder
        .setFrom(Timestamps.parse("2020-10-15T00:00:00Z"))
        .setTo(Timestamps.parse("2020-10-15T00:00:00Z"))
        .setPushName("push_name")
        .setPushTitle("push_title")
        .setPushBody("push_body")

      val request = validRequest.toBuilder
        .setExperiment("experiment")
        .setPushRequest(pushRequest)
        .setFeatureCount(20)
        .build()

      val expectedPushRequest = PushRequest(
        PushName("push_name"),
        PushTitle("push_title"),
        PushBody("push_body"),
        DateTimeInterval(
          DateTime
            .parse("2020-10-15T03:00:00.000+03:00")
            .withZone(DateTimeUtil.DefaultTimeZone),
          DateTime
            .parse("2020-10-15T03:00:00.000+03:00")
            .withZone(DateTimeUtil.DefaultTimeZone)
        )
      )

      val expected = expectedValidModel.copy(
        pushRequest = Some(expectedPushRequest),
        experiment = Some("experiment"),
        featureCount = Some(20)
      )

      mockExpectedCreatePersonalDisount(expected)

      Post("/api/1.x/service/autoru/discount/personal", request)
        .withSalesmanTestHeader() ~> route ~> check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "do right field converting when featureCount is missing in the request" in {
      val request = validRequest

      val expected = expectedValidModel

      mockExpectedCreatePersonalDisount(expected)

      Post("/api/1.x/service/autoru/discount/personal", request)
        .withSalesmanTestHeader() ~> route ~> check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "do right field converting when featureCount is zero" in {
      val request = validRequest.toBuilder
        .setFeatureCount(0)
        .build()

      val expected = expectedValidModel

      mockExpectedCreatePersonalDisount(expected)

      Post("/api/1.x/service/autoru/discount/personal", request)
        .withSalesmanTestHeader() ~> route ~> check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    }

    "do right field converting when featureCount is negative" in {
      val request = validRequest.toBuilder
        .setFeatureCount(-123)
        .build()

      val expected = expectedValidModel

      mockExpectedCreatePersonalDisount(expected)

      Post("/api/1.x/service/autoru/discount/personal", request)
        .withSalesmanTestHeader() ~> route ~> check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
      }
    }
  }

  private def mockSuccessfulCreatePersonalDiscount(): Unit =
    (personalDiscountService.createPersonalDiscount _)
      .expects(*)
      .returningZ(())

  private def mockExpectedCreatePersonalDisount(
      expected: PersonalDiscountModel
  ): Unit =
    (personalDiscountService.createPersonalDiscount _)
      .expects(expected)
      .returningZ(())

  implicit override def domain: DeprecatedDomain = AutoRu
}
