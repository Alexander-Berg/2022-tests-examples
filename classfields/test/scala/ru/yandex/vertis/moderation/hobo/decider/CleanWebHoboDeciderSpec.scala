package ru.yandex.vertis.moderation.hobo.decider

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.hobo.decider.CleanWebHoboDecider.{
  IsCleanWebDeciderEnabledFeatureName,
  IsCleanWebPhotoEnabledFeatureName
}
import ru.yandex.vertis.moderation.hobo.decider.CleanWebHoboDeciderSpec._
import ru.yandex.vertis.moderation.model.ModerationRequest.InitialDepth
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, Diff, Essentials, ExternalId, RealtyEssentials}
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service, Visibility}
import ru.yandex.vertis.moderation.util.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class CleanWebHoboDeciderSpec extends SpecBase {

  implicit private val featureRegistry: FeatureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)

  private val decider = new CleanWebHoboDecider

  featureRegistry.updateFeature(IsCleanWebDeciderEnabledFeatureName, true).futureValue
  featureRegistry.updateFeature(IsCleanWebPhotoEnabledFeatureName, true).futureValue

  "CleanWebHoboDecider" should {
    val testCases: Seq[TestCase] =
      Seq(
        TestCase(
          description = "decide to create hobo task for new autoru offer",
          source = newSource(Service.AUTORU, Diff.all(Service.AUTORU), withDesc = false),
          shouldBeEmpty = false
        ),
        TestCase(
          description = "decide to create hobo task for new realty offer",
          source = newSource(Service.REALTY, Diff.all(Service.REALTY), withImages = false),
          shouldBeEmpty = false
        ),
        TestCase(
          description = "decide to not create hobo task for new autoru user",
          source = newSource(Service.USERS_AUTORU, Diff.all(Service.USERS_AUTORU)),
          shouldBeEmpty = true
        ),
        TestCase(
          description = "decide to not create hobo task for relaty offer without images and description",
          source = newSource(Service.REALTY, Diff.all(Service.REALTY), false, false),
          shouldBeEmpty = true
        ),
        TestCase(
          description = "decide to not create hobo task for autoru offer without images and description",
          source = newSource(Service.AUTORU, Diff.all(Service.AUTORU), false, false),
          shouldBeEmpty = true
        )
      )

    testCases.foreach { case TestCase(description, source, shouldBeEmpty) =>
      description in {
        val expectedPayload = extractPayload(source.instance.essentials)

        val result = decider.decide(source).futureValue

        if (shouldBeEmpty) {
          result shouldBe empty
        } else {
          result should not be empty
          result.get shouldBe a[HoboDecider.NeedCreate]
          val request = result.get.asInstanceOf[HoboDecider.NeedCreate].request
          request.hoboSignalSource.snapshotPayload shouldBe expectedPayload
          request.hoboSignalSource.`type` shouldBe HoboCheckType.CLEAN_WEB
        }
      }
    }
  }
}

object CleanWebHoboDeciderSpec {

  private case class TestCase(description: String, source: HoboDecider.Source, shouldBeEmpty: Boolean)

  private def newSource(service: Service,
                        diff: Diff,
                        withDesc: Boolean = true,
                        withImages: Boolean = true
                       ): HoboDecider.Source = {
    val essentials =
      service match {
        case Service.AUTORU =>
          val photosOpt = Gen.listOfN(2, AutoPhotoInfoOptGen).next
          val photos = photosOpt.map(p => p.copy(picaInfo = Option(AutoPicaInfoGen.next)))
          AutoruEssentialsGen.next.copy(
            description = if (withDesc) Some(stringGen(3, 8).next) else None,
            photos = if (withImages) photos else Nil
          )
        case Service.REALTY =>
          RealtyEssentialsGen.next.copy(
            description = if (withDesc) Some(stringGen(3, 8).next) else None,
            photos = if (withImages) Gen.listOfN(2, RealtyPhotoInfoGen).next else Nil
          )
        case _ => essentialsGen(service).next
      }

    val instance =
      InstanceGen.next.copy(
        id = instanceId,
        signals = SignalSetGen.next,
        essentials = essentials,
        context = ContextGen.next.copy(visibility = Visibility.VISIBLE),
        metadata = MetadataSetGen.next
      )
    HoboDecider.Source(instance, None, diff, OpinionsGen.next, DateTimeUtil.now(), InitialDepth)
  }

  private def instanceId = {
    val externalId =
      ExternalId(
        user = UserYandexGen.next,
        objectId = ObjectIdGen.next
      )
    instanceIdGen(externalId).next
  }

  private def extractPayload(essentials: Essentials): Option[String] = {
    val (desc, images) =
      essentials match {
        case offer: AutoruEssentials =>
          val description = offer.description
          val photos = offer.photos.flatMap(_.picaInfo).map(_.srcUrl)
          (description, photos)
        case offer: RealtyEssentials =>
          val description = offer.description
          val photos = offer.photos.flatMap(_.srcUrl)
          (description, photos)
        case _ => (None, Nil)
      }

    val description = desc.map(d => s""""description":"$d",""").getOrElse("")
    val photos = images.map(url => s""""$url"""").mkString(",")
    Some(s"""{$description"images":[$photos]}""")
  }
}
