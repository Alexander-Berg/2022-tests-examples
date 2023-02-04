package ru.auto.salesman.service.howmuch

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.{Category, Offer, Salon, Section, SellerType}
import ru.auto.api.CarsModel.CarInfo
import ru.auto.salesman.client.howmuch.HowMuchClient
import ru.auto.salesman.client.howmuch.model.MatrixId.{CustomMatrixId, ProductMatrixId}
import ru.auto.salesman.client.howmuch.model.PriceRequest.PriceRequestEntry
import ru.auto.salesman.client.howmuch.model.PriceResponse.PriceResponseEntry
import ru.auto.salesman.client.howmuch.model.{
  EntryId,
  PriceRequest,
  PriceResponse,
  RuleId
}
import ru.auto.salesman.model.criteria.Criterion
import ru.auto.salesman.model.{CityId, Client, ClientStatuses, ProductId, RegionId}
import ru.auto.salesman.model.criteria.CriteriaContext.CallCarsNewCriteriaContext
import ru.auto.salesman.service.howmuch.HowMuchService._
import ru.auto.salesman.service.howmuch.HowMuchServiceImpl._
import ru.auto.salesman.test.BaseSpec
import zio.ZIO

class HowMuchServiceImplSpec extends BaseSpec {
  import HowMuchServiceImplSpec._

  private val howMuchClient = mock[HowMuchClient]

  private val service = new HowMuchServiceImpl(howMuchClient)

  "HowMuchServiceImpl.getPriceForClient" should {
    "return max price from response" in {
      val response = PriceResponse(
        List(
          PriceResponseEntry(EntryId("matrix:k1:v1,k2:v2,k3:v3"), RuleId("rule1"), 30000),
          PriceResponseEntry(EntryId("matrix:k1:v4,k2:v5,k3:v6"), RuleId("rule1"), 50000)
        )
      )

      (howMuchClient.getPrice _).expects(*).returningZ(response)

      service
        .getPriceForClient[None.type, ProductId.CallCarsUsed.type](
          testClient,
          callTime,
          None
        )
        .success
        .value shouldBe Some(50000)
    }

    "return None on empty response" in {
      val response = PriceResponse(List.empty)

      (howMuchClient.getPrice _).expects(*).returningZ(response)

      service
        .getPriceForClient[None.type, ProductId.CallCarsUsed.type](
          testClient,
          callTime,
          None
        )
        .success
        .value shouldBe None
    }

    "fail on Howmuch error" in {
      (howMuchClient.getPrice _).expects(*).throwingZ(new Exception())

      service
        .getPriceForClient[None.type, ProductId.CallCarsUsed.type](
          testClient,
          callTime,
          None
        )
        .failure
        .exception shouldBe an[Exception]
    }
  }

  "HowMuchServiceImpl.getPriceForOffer" should {
    "return first price from response because howmuch is expected to return only one price in seq" in {
      val response = PriceResponse(
        List(
          PriceResponseEntry(EntryId("matrix:k1:v1,k2:v2,k3:v3"), RuleId("rule1"), 30000)
        )
      )

      (howMuchClient.getPrice _).expects(*).returningZ(response)

      service
        .getPriceForOffer[ProductId.Call.type](
          testClient,
          callTime,
          testOffer(true)
        )
        .success
        .value shouldBe Some(30000)
    }

    "return None on empty response" in {
      val response = PriceResponse(List.empty)

      (howMuchClient.getPrice _).expects(*).returningZ(response)

      service
        .getPriceForOffer[ProductId.CallCarsUsed.type](
          testClient,
          callTime,
          testOffer(true)
        )
        .success
        .value shouldBe None
    }

    "fail on Howmuch error" in {
      (howMuchClient.getPrice _).expects(*).throwingZ(new Exception())

      service
        .getPriceForOffer[ProductId.CallCarsUsed.type](
          testClient,
          callTime,
          testOffer(true)
        )
        .failure
        .exception shouldBe an[Exception]
    }
  }

  "HowMuchServiceImpl.NewCarsCallOfferPrice" should {
    "create correct request" in {
      val expected = PriceRequest(
        List(
          PriceRequestEntry(
            callFullMatrix,
            CallCarsNewCriteriaContext(
              List(
                Criterion("region_id", testClient.regionId.toString),
                Criterion("mark", testMark),
                Criterion("model", testModel)
              )
            )
          )
        ),
        callTime
      )

      val actual = NewCarsCallOfferPrice.createRequest(
        testClient,
        callTime,
        testOffer(true)
      )

      actual.success.value shouldEqual expected
    }

    "fail if car info is not set" in {
      val actual = NewCarsCallOfferPrice.createRequest(
        testClient,
        callTime,
        testOffer(false)
      )

      actual.failure.exception shouldBe an[UnsuitableOffer]
    }
  }

  "HowMuchServiceImpl.NewCarsCallClientPrice" should {
    "create correct request" in {
      val suspendedMarks = ZIO.succeed(List("BMW", "OPEL"))
      val expected = PriceRequest(
        List(
          PriceRequestEntry(
            callByMarkMatrix,
            CallCarsNewCriteriaContext(
              List(
                Criterion("region_id", testClient.regionId.toString),
                Criterion("mark", "BMW")
              )
            )
          ),
          PriceRequestEntry(
            callByMarkMatrix,
            CallCarsNewCriteriaContext(
              List(
                Criterion("region_id", testClient.regionId.toString),
                Criterion("mark", "OPEL")
              )
            )
          )
        ),
        callTime
      )

      val actual = NewCarsCallClientPrice.createRequest(
        testClient,
        callTime,
        suspendedMarks
      )

      actual.success.value shouldEqual expected
    }

    "create correct request with default value if there is no marks" in {
      val suspendedMarks = ZIO.succeed(List.empty)
      val expected = PriceRequest(
        List(
          PriceRequestEntry(
            callByMarkMatrix,
            CallCarsNewCriteriaContext(
              List(
                Criterion("region_id", testClient.regionId.toString),
                Criterion("mark", "*")
              )
            )
          )
        ),
        callTime
      )

      val actual = NewCarsCallClientPrice.createRequest(
        testClient,
        callTime,
        suspendedMarks
      )

      actual.success.value shouldEqual expected
    }

    "fail if suspended getMarks effect fails" in {
      val failure = new Exception("Some fail during marks lookup")
      val getMarksFailure = ZIO.fail(failure)

      val actual = NewCarsCallClientPrice.createRequest(
        testClient,
        callTime,
        getMarksFailure
      )

      actual.failure.exception shouldBe failure
    }
  }

  "HowMuchServiceImpl.UsedCarsCallOfferPrice" should {
    "create correct request" in {
      val expected = PriceRequest(
        List(
          PriceRequestEntry(
            ProductMatrixId(ProductId.CallCarsUsed),
            CallCarsNewCriteriaContext(
              List(
                Criterion("region_id", testClient.regionId.toString),
                Criterion("mark", testMark),
                Criterion("model", testModel),
                Criterion("super_gen_id", testGeneration.toString)
              )
            )
          )
        ),
        callTime
      )

      val actual = UsedCarsCallOfferPrice.createRequest(
        testClient,
        callTime,
        testOffer(true)
      )

      actual.success.value shouldEqual expected
    }

    "fail if car info is not set" in {
      val actual = UsedCarsCallOfferPrice.createRequest(
        testClient,
        callTime,
        testOffer(false)
      )

      actual.failure.exception shouldBe an[UnsuitableOffer]
    }
  }

  "HowMuchServiceImpl.UsedCarsCallClientPrice" should {
    "create correct request" in {
      val expected = PriceRequest(
        List(
          PriceRequestEntry(
            callCarsUsedByRegionMatrix,
            CallCarsNewCriteriaContext(
              List(
                Criterion("region_id", testClient.regionId.toString)
              )
            )
          )
        ),
        callTime
      )

      val actual = UsedCarsCallClientPrice.createRequest(
        testClient,
        callTime,
        None
      )

      actual.success.value shouldEqual expected
    }
  }

}

object HowMuchServiceImplSpec {

  private val testClient =
    Client(
      clientId = 23965,
      agencyId = None,
      categorizedClientId = None,
      companyId = None,
      RegionId(1L),
      CityId(2L),
      ClientStatuses.Active,
      singlePayment = Set(),
      firstModerated = false,
      paidCallsAvailable = true,
      priorityPlacement = true
    )

  private val callTime = DateTime.now()

  private val testMark = "BMW"
  private val testModel = "X5"
  private val testGeneration = 100500

  private val callFullMatrix = ProductMatrixId(ProductId.Call)
  private val callByMarkMatrix = CustomMatrixId("call_by_mark")
  private val callCarsUsedByRegionMatrix = CustomMatrixId("call:cars:used_by_region")

  private def testOffer(withInfo: Boolean): Offer = {
    val offer = Offer
      .newBuilder()
      .setId("123-abc")
      .setSellerType(SellerType.COMMERCIAL)
      .setCategory(Category.CARS)
      .setSection(Section.NEW)
      .setSalon(
        Salon
          .newBuilder()
          .setClientId(testClient.clientId.toString)
      )
    if (withInfo)
      offer.setCarInfo(
        CarInfo
          .newBuilder()
          .setMark(testMark)
          .setModel(testModel)
          .setSuperGenId(testGeneration)
      )

    offer.build()
  }

}
