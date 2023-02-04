package ru.yandex.vertis.shark.api.enricher

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.quicklens._
import com.softwaremill.tagging.Tagger
import common.zio.features.{FeatureRegistryWrapper, Features}
import mouse.ignore
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.feature.impl.{CompositeFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.shark.dictionary.FiasGeoDictionary
import ru.yandex.vertis.shark.model.Block.ResidenceAddressBlock
import ru.yandex.vertis.shark.model.CreditApplication.Requirements
import ru.yandex.vertis.shark.model.Entity.AddressEntity
import ru.yandex.vertis.shark.model.{CreditApplicationSource, PersonProfileImpl, Tag}
import ru.yandex.vertis.shark.model.TestSyntax._
import ru.yandex.vertis.zio_baker.{model => zio_baker}
import ru.yandex.vertis.zio_baker.zio.features.CommonFeatureTypes
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation.value
import zio.test.mock.mockable
import zio.test.{assertM, DefaultRunnableSpec, TestAspect, ZSpec}
import zio.{UIO, ZIO, ZLayer}

object CreditApplicationSourceEnricherSpec extends DefaultRunnableSpec {

  @mockable[FiasGeoDictionary.Service]
  object FiasGeoDictionaryMock

  private val fias1 = "0c5b2444-70a0-4932-980c-b4dc0d3f02b5".taggedWith[zio_baker.Tag.FiasId]
  private val geo1 = 213L.taggedWith[zio_baker.Tag.GeobaseId]

  private val fiasGeobaseMock = FiasGeoDictionaryMock.FindGeobase(equalTo(fias1), value(Some(geo1))).optional

  private def featureRegistryLayer(updateRequirementGeoFromResidence: Boolean) = {
    val memoryFeatureRegistry = new InMemoryFeatureRegistry(new CompositeFeatureTypes(Seq(CommonFeatureTypes)))
    ignore(
      memoryFeatureRegistry.register(
        "update-requirements-geo-from-residence",
        initialValue = updateRequirementGeoFromResidence
      )
    )
    ZLayer.succeed(new Features.Service {
      override def featureRegistry: UIO[FeatureRegistryWrapper] =
        ZIO.succeed(FeatureRegistryWrapper(memoryFeatureRegistry))
    })
  }

  private def layer(updateRequirementGeoFromResidence: Boolean) =
    featureRegistryLayer(updateRequirementGeoFromResidence) ++ fiasGeobaseMock >>> CreditApplicationSourceEnricher.live

  case class UpdateResidenceGeoTest(
      description: String,
      source: CreditApplicationSource,
      expected: CreditApplicationSource,
      featureOn: Boolean)

  private val updateResidenceGeoTestCases = {
    val initialSource = CreditApplicationSource.forTest(
      requirements = Requirements(
        1L.taggedWith[Tag.MoneyRub],
        1L.taggedWith[Tag.MoneyRub],
        1.taggedWith[Tag.MonthAmount],
        Seq(123L.taggedWith[zio_baker.Tag.GeobaseId])
      ).some,
      borrowerPersonProfile = PersonProfileImpl
        .forTest(
          residenceAddress = ResidenceAddressBlock(
            addressEntity = AddressEntity(
              region = "region",
              city = "city".some,
              settlement = "settlement".some,
              district = "district".some,
              street = "street".some,
              building = "building".some,
              corpus = "corpus".some,
              construction = "construction".some,
              apartment = "apartment".some,
              postCode = 123.taggedWith[Tag.RusPostCode],
              kladr = AddressEntity.Kladr.forTest(),
              fias = AddressEntity.Fias.forTest(areaId = fias1.some)
            )
          ).some
        )
        .some
    )
    Seq(
      UpdateResidenceGeoTest(
        description = "update with residence geo tests when feature on",
        source = initialSource,
        expected = initialSource.modify(_.requirements.each.geobaseIds).setTo(Seq(geo1)),
        featureOn = true
      ),
      UpdateResidenceGeoTest(
        description = "do not update with residence geo tests when feature off",
        source = initialSource,
        expected = initialSource,
        featureOn = false
      )
    )
  }

  private val updateResidenceGeoTests = updateResidenceGeoTestCases.map {
    case UpdateResidenceGeoTest(description, source, expected, featureOn) =>
      testM(description) {
        val result = for {
          enricher <- ZIO.service[CreditApplicationSourceEnricher.Service]
          response <- enricher.enrich(source)
        } yield response
        assertM(result)(equalTo(expected)).provideLayer(layer(featureOn))
      }
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("CreditApplicationSourceEnricher")(
      suite("updateResidenceGeoTests")(updateResidenceGeoTests: _*) @@ TestAspect.sequential
    )
}
