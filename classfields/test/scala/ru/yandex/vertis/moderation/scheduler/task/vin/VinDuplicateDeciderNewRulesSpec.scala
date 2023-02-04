package ru.yandex.vertis.moderation.scheduler.task.vin

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.httpclient.vin.{Vin, Year}
import ru.yandex.vertis.moderation.model.ObjectId
import ru.yandex.vertis.moderation.model.autoru.GeobaseId
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{instanceGen, AutoruEssentialsGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, ExternalId, Instance, User}
import ru.yandex.vertis.moderation.proto.Autoru
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.{
  Availability,
  Category,
  Section,
  SellerType,
  SteeringWheel
}
import ru.yandex.vertis.moderation.proto.Model.{Service, Visibility}
import ru.yandex.vertis.moderation.scheduler.task.vin.VinDuplicateDecider.AutoruVinDuplicateDecider

@RunWith(classOf[JUnitRunner])
class VinDuplicateDeciderNewRulesSpec extends SpecBase {

  import VinDuplicateDeciderNewRulesSpec._

  private def newDecider(featureValue: Boolean): AutoruVinDuplicateDecider = {
    val featureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)
    val deciderNew: AutoruVinDuplicateDecider =
      VinDuplicateDecider.forService(Service.AUTORU)(featureRegistry).asInstanceOf[AutoruVinDuplicateDecider]
    featureRegistry.updateFeature(AutoruVinDuplicateDecider.NewRulesFeatureEnabled, featureValue).futureValue
    deciderNew
  }

  private val now = DateTime.now()

  private def instance(vin: Option[Vin] = Some("test-vin"),
                       availability: Availability = Availability.IN_STOCK,
                       createTime: DateTime = now,
                       geobaseId: GeobaseId = 0,
                       sellerType: SellerType = SellerType.PRIVATE,
                       year: Year = 1990,
                       steeringWheel: SteeringWheel = SteeringWheel.LEFT,
                       category: Category = Category.CARS,
                       objectId: ObjectId,
                       user: User = User.Autoru("5"),
                       section: Option[Section] = Some(Section.USED),
                       visibility: Visibility = Visibility.VISIBLE,
                       isCallCenter: Boolean = false,
                       hasLicensePlateOnPhotos: Option[Boolean] = None
                      ) = {

    val autoruEssentials = AutoruEssentialsGen.next
    val instance = instanceGen(ExternalId(user, objectId)).next

    instance.copy(
      context = instance.context.copy(visibility = visibility),
      essentials =
        autoruEssentials.copy(
          source = Autoru.AutoruEssentials.Source.AUTO_RU,
          vin = vin,
          sellerType = Some(sellerType),
          availability = Some(availability),
          timestampCreate = Some(createTime),
          geobaseId = Seq(geobaseId),
          year = Some(year),
          steeringWheel = Some(steeringWheel),
          category = Some(category),
          section = section,
          isCallCenter = isCallCenter,
          hasLicensePlateOnPhotos = hasLicensePlateOnPhotos
        )
    )
  }

  private val defaultInstance = instance(objectId = "defaultInstance")
  private val defaultEssentials: AutoruEssentials = defaultInstance.essentials.asInstanceOf[AutoruEssentials]

  private val basicPrivateEssentials = defaultEssentials.copy(sellerType = Some(SellerType.PRIVATE))
  private val basicPrivateOldEssentials =
    defaultEssentials.copy(
      sellerType = Some(SellerType.PRIVATE),
      timestampCreate = Some(now.minusDays(1))
    )
  private val basicCommercialEssentials = defaultEssentials.copy(sellerType = Some(SellerType.COMMERCIAL))
  private val basicPrivate = defaultInstance.copy(essentials = basicPrivateEssentials)
  private val basicPrivateOld = defaultInstance.copy(essentials = basicPrivateOldEssentials)
  private val basicCommercial = defaultInstance.copy(essentials = basicCommercialEssentials)

  private val nonCallCenterEssentials = defaultEssentials
  private val fromcallCenterEssentials = defaultEssentials.copy(isCallCenter = true)
  private val fromcallCenterOldEssentials =
    defaultEssentials.copy(
      isCallCenter = true,
      timestampCreate = Some(now.minusDays(1))
    )
  private val nonCallCenter = defaultInstance.copy(essentials = nonCallCenterEssentials)
  private val fromCallCenter = defaultInstance.copy(essentials = fromcallCenterEssentials)
  private val fromCallCenterOld = defaultInstance.copy(essentials = fromcallCenterOldEssentials)

  private val privateWithLicensePlateEssentials = defaultEssentials.copy(hasLicensePlateOnPhotos = Some(true))
  private val privateWithoutLicensePlateEssentials = defaultEssentials.copy(hasLicensePlateOnPhotos = Some(false))
  private val privateWithoutLicensePlatOldEssentials =
    defaultEssentials.copy(
      hasLicensePlateOnPhotos = Some(false),
      timestampCreate = Some(now.minusDays(1))
    )
  private val privateWithLicensePlate = defaultInstance.copy(essentials = privateWithLicensePlateEssentials)
  private val privateWithoutLicensePlate = defaultInstance.copy(essentials = privateWithoutLicensePlateEssentials)
  private val privateWithoutLicensePlateOld = defaultInstance.copy(essentials = privateWithoutLicensePlatOldEssentials)

  private val testCases =
    Seq(
      VinDuplicateTestCase(
        "correctly sorts instances with SellerType",
        newDecider(true),
        Seq(basicCommercial, basicPrivateOld, basicPrivate),
        Seq(basicPrivate, basicPrivateOld, basicCommercial)
      ),
      VinDuplicateTestCase(
        "correctly sorts with old logic instances with SellerType",
        newDecider(false),
        Seq(basicCommercial, basicPrivateOld, basicPrivate),
        Seq(basicPrivate, basicPrivateOld, basicCommercial)
      ),
      VinDuplicateTestCase(
        "correctly sorts instances with CallCenter",
        newDecider(true),
        Seq(fromCallCenterOld, nonCallCenter, fromCallCenter),
        Seq(fromCallCenter, fromCallCenterOld, nonCallCenter)
      ),
      VinDuplicateTestCase(
        "correctly sorts with old logic instances with CallCenter",
        newDecider(false),
        Seq(fromCallCenterOld, nonCallCenter, fromCallCenter),
        Seq(nonCallCenter, fromCallCenter, fromCallCenterOld)
      ),
      VinDuplicateTestCase(
        "correctly sorts instances with SellerType and hasLicensePlateOnPhotos",
        newDecider(true),
        Seq(privateWithoutLicensePlateOld, privateWithLicensePlate, privateWithoutLicensePlate),
        Seq(privateWithoutLicensePlate, privateWithoutLicensePlateOld, privateWithLicensePlate)
      ),
      VinDuplicateTestCase(
        "correctly sorts with old logic instances with SellerType and hasLicensePlateOnPhotos",
        newDecider(false),
        Seq(privateWithoutLicensePlateOld, privateWithoutLicensePlate, privateWithLicensePlate),
        Seq(privateWithoutLicensePlate, privateWithLicensePlate, privateWithoutLicensePlateOld)
      ),
      VinDuplicateTestCase(
        "correctly sorts instances with SellerType + hasLicensePlateOnPhotos and with CallCenter",
        newDecider(true),
        Seq(fromCallCenterOld, privateWithoutLicensePlate),
        Seq(privateWithoutLicensePlate, fromCallCenterOld)
      ),
      VinDuplicateTestCase(
        "correctly sorts instances with SellerType + hasLicensePlateOnPhotos and with CallCenter 2",
        newDecider(true),
        Seq(privateWithoutLicensePlateOld, fromCallCenter),
        Seq(fromCallCenter, privateWithoutLicensePlateOld)
      )
    )

  "VinDuplicateDecider with" should {

    testCases.foreach { case VinDuplicateTestCase(description, decider, toCheck, expected) =>
      description in {
        toCheck.sorted(decider.SameVinOrdering) shouldBe expected
      }
    }
  }
}

object VinDuplicateDeciderNewRulesSpec {

  case class VinDuplicateTestCase(description: String,
                                  decider: AutoruVinDuplicateDecider,
                                  toCheck: Seq[Instance],
                                  expected: Seq[Instance]
                                 )
}
