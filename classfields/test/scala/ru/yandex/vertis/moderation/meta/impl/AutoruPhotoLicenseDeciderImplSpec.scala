package ru.yandex.vertis.moderation.meta.impl

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.meta.AutoruPhotoLicenseDecider
import ru.yandex.vertis.moderation.model.context.Context
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, Instance}
import ru.yandex.vertis.moderation.model.meta.{Metadata, MetadataSet}
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials._
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Metadata.AutoruPhotoLicensePlate.Value.WAITING_FOR_PHOTO

@RunWith(classOf[JUnitRunner])
class AutoruPhotoLicenseDeciderImplSpec extends SpecBase {

  private val decider: AutoruPhotoLicenseDecider = new AutoruPhotoLicenseDeciderImpl

  case class TestCase(description: String,
                      instance: Instance,
                      timestamp: DateTime,
                      expected: Option[Metadata.AutoruPhotoLicensePlate]
                     )

  private val validEssential: AutoruEssentials =
    AutoruEssentialsGen.next
      .copy(
        geobaseId = Seq(1),
        category = Some(Category.CARS),
        section = Some(Section.USED),
        sellerType = Some(SellerType.PRIVATE),
        condition = Some(Condition.EXCELLENT),
        customHouseState = CustomHouseState.CLEARED,
        isCallCenter = false,
        notRegisteredInRussia = Some(false),
        hasLicensePlateOnPhotos = Some(false)
      )

  private val invalidEssential: AutoruEssentials = AutoruEssentialsGen.next.copy(category = None)

  private val metadata: Metadata = AutoruPhotoLicensePlateMetadataGen.next.copy(value = WAITING_FOR_PHOTO)
  private val metadataSet: MetadataSet = MetadataSet.apply(metadata)

  private val visibleContext: Context = ContextGen.next.copy(visibility = Model.Visibility.VISIBLE)
  private def invisibleContext(): Context =
    ContextGen.next.copy(visibility =
      Gen.oneOf(Model.Visibility.values().filterNot(_ == Model.Visibility.VISIBLE)).next
    )

  private val testCases: Seq[TestCase] =
    Seq(
      {
        val ts = DateTimeGen.next
        TestCase(
          description = "Valid conditions",
          instance =
            InstanceGen.next.copy(essentials = validEssential, metadata = MetadataSet.Empty, context = visibleContext),
          timestamp = ts,
          expected = Some(Metadata.AutoruPhotoLicensePlate(WAITING_FOR_PHOTO, ts, ttl = None))
        )
      }, {
        val ts = DateTimeGen.next
        TestCase(
          description = "Valid conditions, but not visible context",
          instance =
            InstanceGen.next
              .copy(essentials = validEssential, metadata = MetadataSet.Empty, context = invisibleContext()),
          timestamp = ts,
          expected = None
        )
      },
      TestCase(
        description = "Valid conditions, but meta exist",
        instance = InstanceGen.next.copy(essentials = validEssential, metadata = metadataSet, context = visibleContext),
        timestamp = DateTimeGen.next,
        expected = None
      ),
      TestCase(
        description = "Invalid conditions",
        instance = InstanceGen.next.copy(essentials = invalidEssential, context = visibleContext),
        timestamp = DateTimeGen.next,
        expected = None
      )
    )

  "AutoruPhotoLicenseDecider" should {
    testCases.foreach { testCase =>
      import testCase._
      description in {
        decider(instance, timestamp) shouldBe expected
      }
    }
  }
}
