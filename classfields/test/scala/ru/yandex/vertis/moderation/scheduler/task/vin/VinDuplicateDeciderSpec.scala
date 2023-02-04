package ru.yandex.vertis.moderation.scheduler.task.vin

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.httpclient.vin.{Vin, Year}
import ru.yandex.vertis.moderation.model.ObjectId
import ru.yandex.vertis.moderation.model.autoru.GeobaseId
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{instanceGen, AutoruEssentialsGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{ExternalId, User}
import ru.yandex.vertis.moderation.proto.Autoru
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials._
import ru.yandex.vertis.moderation.proto.Model.{Service, Visibility}
import ru.yandex.vertis.moderation.feature.EmptyFeatureRegistry
import ru.yandex.vertis.moderation.scheduler.task.vin.VinDuplicateDecider.AutoruVinDuplicateDecider

/**
  * Specs on [[VinDuplicateDecider]]
  *
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class VinDuplicateDeciderSpec extends SpecBase {

  implicit val featureRegistry: FeatureRegistry = EmptyFeatureRegistry
  val decider: VinDuplicateDecider = VinDuplicateDecider.forService(Service.AUTORU)

  import VinDuplicateDecider.Source

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
                       visibility: Visibility = Visibility.VISIBLE
                      ) = {
    val autoru = AutoruEssentialsGen.next
    val instance = instanceGen(ExternalId(user, objectId)).next

    instance.copy(
      context = instance.context.copy(visibility = visibility),
      essentials =
        autoru.copy(
          source = Autoru.AutoruEssentials.Source.AUTO_RU,
          vin = vin,
          sellerType = Some(sellerType),
          availability = Some(availability),
          timestampCreate = Some(createTime),
          geobaseId = Seq(geobaseId),
          year = Some(year),
          steeringWheel = Some(steeringWheel),
          category = Some(category),
          section = section
        )
    )
  }

  private val noneVin = instance(vin = None, objectId = "noneVin")
  private val emptyVin = instance(vin = Some(""), objectId = "emptyVin")
  private val emptyVin2 = instance(vin = Some(""), objectId = "emptyVin2")
  private val rightSteeringWheel = instance(objectId = "rightSteeringWheel", steeringWheel = SteeringWheel.RIGHT)
  private val nonCars = instance(objectId = "nonCars", category = Category.ARTIC)
  private val oldCar = instance(vin = Some("TEST-VIN"), objectId = "oldCar", year = 1989)
  private val newCar = instance(objectId = "newCar", section = Some(Section.NEW))
  private val inactiveCar = instance(objectId = "inactive", visibility = Visibility.INACTIVE)
  private val undefinedCar = instance(objectId = "undefinedCar", section = None)
  private val defaultInstance = instance(objectId = "defaultInstance")
  private val unknownVin = instance(objectId = "unknownVin", vin = Some("other-vin"))
  private val commercial = instance(objectId = "commercial", sellerType = SellerType.COMMERCIAL)
  private val commercialUpdated =
    instance(objectId = "commercial", sellerType = SellerType.COMMERCIAL, createTime = now.plusDays(1))
  private val commercialCreatedDayAgo =
    instance(objectId = "commercialCreatedDayAgo", sellerType = SellerType.COMMERCIAL, createTime = now.minusDays(1))
  private val createdDayAgo = instance(objectId = "createdDayAgo", createTime = now.minusDays(1))

  private val inOrder =
    instance(objectId = "inOrder", sellerType = SellerType.COMMERCIAL, availability = Availability.IN_ORDER)

  "VinDuplicateDecider" should {

    "pass source with valid VINs" in {
      Source(noneVin, Seq(noneVin))
      Source(emptyVin, Seq(emptyVin2))
      Source(noneVin, Seq(emptyVin2))
      Source(emptyVin, Seq(noneVin, emptyVin2))
      Source(
        rightSteeringWheel,
        Seq(
          rightSteeringWheel,
          nonCars,
          newCar,
          inactiveCar,
          undefinedCar,
          defaultInstance,
          commercial,
          commercialUpdated,
          commercialCreatedDayAgo,
          createdDayAgo,
          inOrder,
          oldCar
        )
      )
    }

    "fail source with invalid VINs" in {
      intercept[IllegalArgumentException](Source(noneVin, Seq(rightSteeringWheel)))
      intercept[IllegalArgumentException](Source(emptyVin, Seq(commercial)))
      intercept[IllegalArgumentException](Source(commercial, Seq(emptyVin)))
      intercept[IllegalArgumentException](Source(unknownVin, Seq(commercial)))
      intercept[IllegalArgumentException](Source(commercial, Seq(unknownVin)))
      intercept[IllegalArgumentException] {
        Source(
          noneVin,
          Seq(
            rightSteeringWheel,
            nonCars,
            newCar,
            inactiveCar,
            undefinedCar,
            defaultInstance,
            commercial,
            commercialUpdated,
            commercialCreatedDayAgo,
            createdDayAgo,
            inOrder
          )
        )
      }
      intercept[IllegalArgumentException] {
        Source(
          rightSteeringWheel,
          Seq(
            rightSteeringWheel,
            nonCars,
            newCar,
            inactiveCar,
            undefinedCar,
            defaultInstance,
            commercial,
            commercialUpdated,
            commercialCreatedDayAgo,
            createdDayAgo,
            inOrder,
            unknownVin
          )
        )
      }
      intercept[IllegalArgumentException] {
        Source(
          unknownVin,
          Seq(
            rightSteeringWheel,
            nonCars,
            newCar,
            inactiveCar,
            undefinedCar,
            defaultInstance,
            commercial,
            commercialUpdated,
            commercialCreatedDayAgo,
            createdDayAgo,
            inOrder,
            oldCar
          )
        )
      }
    }

    "correctly sorts instances by person and cr_date" in {
      val checkSort =
        Seq(
          commercialCreatedDayAgo,
          commercial,
          defaultInstance,
          createdDayAgo
        )
      val featureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)
      val decider: AutoruVinDuplicateDecider =
        VinDuplicateDecider.forService(Service.AUTORU)(featureRegistry).asInstanceOf[AutoruVinDuplicateDecider]
      featureRegistry.updateFeature(AutoruVinDuplicateDecider.NewRulesFeatureEnabled, false).futureValue
      checkSort.sorted(decider.SameVinOrdering) shouldBe
        Seq(defaultInstance, createdDayAgo, commercial, commercialCreatedDayAgo)
    }

    val NotFitSources =
      Seq(
        Source(noneVin, Seq(noneVin)),
        Source(emptyVin, Seq(noneVin, emptyVin2)),
        Source(emptyVin2, Seq.empty),
        Source(nonCars, Seq.empty),
        Source(rightSteeringWheel, Seq.empty),
        Source(newCar, Seq.empty),
        Source(undefinedCar, Seq.empty),
        Source(inactiveCar, Seq.empty),
        Source(oldCar, Seq.empty),
        Source(rightSteeringWheel, Seq(commercial)),
        Source(inOrder, Seq.empty)
      )

    NotFitSources.foreach { source =>
      s"skip $source" in {
        decider(source) shouldBe None
      }
    }

    val SourcesWithOriginal =
      Seq(
        Source(defaultInstance, Seq(nonCars, rightSteeringWheel, newCar)),
        Source(defaultInstance, Seq(defaultInstance, nonCars, rightSteeringWheel, newCar)),
        Source(commercial, Seq(defaultInstance, createdDayAgo, rightSteeringWheel, oldCar)),
        Source(createdDayAgo, Seq(defaultInstance, undefinedCar, rightSteeringWheel)),
        Source(commercialCreatedDayAgo, Seq(commercial, inactiveCar, defaultInstance, createdDayAgo, oldCar, newCar)),
        Source(commercial, Seq(rightSteeringWheel, inactiveCar)),
        Source(commercial, Seq.empty),
        Source(commercial, Seq(commercial)),
        Source(commercialUpdated, Seq(commercial, inOrder, defaultInstance, createdDayAgo, newCar)),
        Source(commercial, Seq(inOrder, defaultInstance, createdDayAgo)),
        Source(createdDayAgo, Seq.empty),
        Source(commercialCreatedDayAgo, Seq.empty)
      )

    SourcesWithOriginal.foreach { source =>
      s"instance is original for $source" in {
        decider(source).get.original == source.instance
      }
    }

    val SourcesWithDuplicate =
      Seq(
        Source(defaultInstance, Seq(inOrder, createdDayAgo, newCar)),
        Source(defaultInstance, Seq(defaultInstance, inOrder, createdDayAgo, newCar)),
        Source(createdDayAgo, Seq(inOrder, defaultInstance, inactiveCar, commercial)),
        Source(createdDayAgo, Seq(inOrder, defaultInstance, inactiveCar, commercial, createdDayAgo)),
        Source(commercial, Seq(commercialCreatedDayAgo, undefinedCar, newCar, rightSteeringWheel, oldCar)),
        Source(commercial, Seq(commercialCreatedDayAgo, commercial, undefinedCar, newCar, rightSteeringWheel, oldCar)),
        Source(defaultInstance, Seq(createdDayAgo, rightSteeringWheel, oldCar)),
        Source(defaultInstance, Seq(createdDayAgo, rightSteeringWheel, oldCar, defaultInstance)),
        Source(createdDayAgo, Seq(rightSteeringWheel, commercial, inactiveCar, newCar)),
        Source(createdDayAgo, Seq(createdDayAgo, rightSteeringWheel, commercial, inactiveCar, newCar))
      )

    SourcesWithDuplicate.foreach { source =>
      s"instance is Duplicate for $source" in {
        decider(source).get.duplicateIds contains source.instance.externalId
      }
    }
  }

}
