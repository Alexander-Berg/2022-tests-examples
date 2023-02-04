package ru.yandex.vertis.shark.api.manager

import cats.syntax.option._
import com.softwaremill.tagging.Tagger
import org.scalacheck.Gen
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.model.GeobaseId
import ru.yandex.vertis.shark.controller.requirements.RequirementsCheck
import ru.yandex.vertis.shark.controller.credit_product_calculator.testkit.CreditProductCalculatorMock
import ru.yandex.vertis.shark.controller.{CreditProductController, PreconditionsCalculator}
import ru.yandex.vertis.shark.model.Api
import ru.yandex.vertis.shark.model.Arbitraries._
import ru.yandex.vertis.shark.model.AutoCreditProduct
import ru.yandex.vertis.shark.model.CommonApplicationObjectPayload
import ru.yandex.vertis.shark.model.CreditProduct
import ru.yandex.vertis.shark.model.CreditProductId
import ru.yandex.vertis.shark.model.Tag
import ru.yandex.vertis.shark.proto.model.CreditProduct.PriorityTag
import zio.{Task, UIO, ZIO}
import zio.test.Assertion.anything
import zio.test.Assertion.equalTo
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation.value
import zio.test.mock.Expectation.valueF
import zio.test.mock.Expectation.valueM
import zio.test.mock.mockable
import ru.yandex.vertis.shark.model.PriorityTagsData

object PreconditionsManagerSpec extends DefaultRunnableSpec {
  import org.scalacheck.magnolia._

  private val domain = Domain.DOMAIN_AUTO
  private val defaultGeobaseIds: Seq[GeobaseId] = Seq(225L.taggedWith[zio_baker.Tag.GeobaseId])

  @mockable[CreditProductController.Service]
  object CreditProductControllerMock

  @mockable[RequirementsCheck.Service]
  object RequirementsCheckMock

  @mockable[PreconditionsCalculator.Service]
  object PreconditionsCalculatorMock

  private val samplePrecondition = gen[Api.ProductPrecondition].arbitrary.sample.get

  private val sampleProduct =
    gen[AutoCreditProduct].arbitrary.sample.get.copy(isActive = true, priorityTags = Set.empty)
  private val sampleObjectInfo = gen[Api.ObjectInfo].arbitrary.sample.get
  private val sampleGeobaseIds = Gen.listOfN(3, GeobaseIdArb.arbitrary).sample.get
  private val sampleObjectPayload = gen[Api.PreconditionsRequest.ObjectPayload].arbitrary.sample.get

  /**
    * A simple implementation that should be good enough for this test. It's
    * important that the resulting preconditions are equal iff they are built
    * from the same object and product IDs.
    */
  private def precondition(objectId: String, productIds: Seq[CreditProductId]) = Api.Precondition(
    objectId,
    productIds.map { creditProductId =>
      samplePrecondition.copy(productId = creditProductId)
    }
  )

  private case class PreconditionsTestCase(
      description: String,
      request: Api.PreconditionsRequest,
      expected: Api.PreconditionsResponse,
      products: Seq[CreditProduct],
      checkGeo: ((Seq[GeobaseId], CreditProduct)) => UIO[Boolean] = _ => ZIO.succeed(true),
      checkObject: ((Option[CommonApplicationObjectPayload], CreditProduct)) => UIO[Boolean] = _ => ZIO.succeed(true),
      priorityTags: PriorityTagsData => Set[PriorityTag] = _ => Set.empty)

  private def creditProductId(s: String): CreditProductId = s.taggedWith[Tag.CreditProductId]

  private val preconditionsTestCases = Seq(
    PreconditionsTestCase(
      "simple case",
      request = Api.PreconditionsRequest(
        Seq(
          sampleObjectInfo
        )
      ),
      products = Seq(sampleProduct.copy(id = creditProductId("foo"))),
      expected = Api.PreconditionsResponse(
        preconditions = Seq(
          precondition(sampleObjectInfo.id, Seq(creditProductId("foo")))
        )
      )
    ),
    PreconditionsTestCase(
      "filter products by checkGeo",
      request = Api.PreconditionsRequest(
        Seq(
          sampleObjectInfo.copy(geobaseIds = sampleGeobaseIds)
        )
      ),
      products = Seq(
        sampleProduct.copy(id = creditProductId("foo")),
        sampleProduct.copy(id = creditProductId("bar"))
      ),
      checkGeo = {
        case (`sampleGeobaseIds`, p) => ZIO.succeed(p.id == creditProductId("foo"))
        case (ids, _) => ZIO.dieMessage(s"Unexpected geobaseIds: $ids")
      },
      expected = Api.PreconditionsResponse(
        preconditions = Seq(
          precondition(sampleObjectInfo.id, Seq(creditProductId("foo")))
        )
      )
    ),
    PreconditionsTestCase(
      "fall back to the default geobaseId list",
      request = Api.PreconditionsRequest(
        Seq(
          sampleObjectInfo.copy(geobaseIds = Seq.empty)
        )
      ),
      products = Seq(sampleProduct.copy(id = creditProductId("foo"))),
      checkGeo = {
        case (`defaultGeobaseIds`, _) => ZIO.succeed(true)
        case (ids, _) => ZIO.dieMessage(s"Unexpected geobaseIds: $ids")
      },
      expected = Api.PreconditionsResponse(
        preconditions = Seq(
          precondition(sampleObjectInfo.id, Seq(creditProductId("foo")))
        )
      )
    ),
    PreconditionsTestCase(
      "filter products by checkObject",
      request = Api.PreconditionsRequest(
        Seq(
          sampleObjectInfo.copy(
            objectPayload = sampleObjectPayload.some
          )
        )
      ),
      products = Seq(
        sampleProduct.copy(id = creditProductId("foo")),
        sampleProduct.copy(id = creditProductId("bar"))
      ),
      checkObject = {
        case (Some(payload), p) if payload == CommonApplicationObjectPayload.from(sampleObjectPayload) =>
          ZIO.succeed(p.id == creditProductId("foo"))
        case (payload, _) => ZIO.dieMessage(s"Unexpected payload: $payload")
      },
      expected = Api.PreconditionsResponse(
        preconditions = Seq(
          precondition(sampleObjectInfo.id, Seq(creditProductId("foo")))
        )
      )
    ),
    PreconditionsTestCase(
      "ignore inactive products",
      request = Api.PreconditionsRequest(
        Seq(
          sampleObjectInfo
        )
      ),
      products = Seq(
        sampleProduct.copy(id = creditProductId("foo")),
        sampleProduct.copy(id = creditProductId("bar"), isActive = false)
      ),
      expected = Api.PreconditionsResponse(
        preconditions = Seq(
          precondition(sampleObjectInfo.id, Seq(creditProductId("foo")))
        )
      )
    )
  )

  private def preconditionsTests = preconditionsTestCases.map { tc =>
    val controllerMocks = tc.request.objectInfo.flatMap { objectInfo =>
      tc.products.map { product =>
        val objectPayload = objectInfo.objectPayload.map(CommonApplicationObjectPayload.from)
        CreditProductControllerMock.Get._0(
          equalTo((product.id, objectInfo.geobaseIds, objectPayload)),
          valueM(_ => Task.succeed(tc.products.find(_.id == product.id).get))
        )
      }
    }
    val creditProductControllerGetMock = controllerMocks.tail.foldLeft(controllerMocks.head)((a, b) => a || b)

    val creditProductControllerMock =
      CreditProductControllerMock.List._0(anything, value(tc.products)) || creditProductControllerGetMock

    val creditProductCalculatorMock =
      RequirementsCheckMock.CheckGeo(anything, valueM(tc.checkGeo)) ||
        CreditProductCalculatorMock.CheckObject(anything, valueM(tc.checkObject)) ||
        CreditProductCalculatorMock.PriorityTags(anything, valueF(tc.priorityTags)) ||
        CreditProductCalculatorMock.SortCreditProducts(anything, valueM(args => Task.succeed(args._1)))

    val preconditionsCalculatorMock =
      PreconditionsCalculatorMock.Calculate(
        anything,
        valueF { case (objectInfo, products) =>
          if (products.isEmpty) none
          else precondition(objectInfo.id, products.map(_.id)).some
        }
      )

    val mocks = (creditProductControllerMock || creditProductCalculatorMock || preconditionsCalculatorMock)
      .repeats(0 to Int.MaxValue)

    val preconditionsManagerLayer = mocks >>> PreconditionsManager.live

    testM(tc.description)(
      assertM(PreconditionsManager.preconditions(domain, tc.request))(equalTo(tc.expected))
    ).provideLayer(preconditionsManagerLayer)
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("PreconditionsManager")(
      suite("preconditions")(preconditionsTests: _*)
    )
}
