package vertis.palma.utils

import com.google.protobuf.DynamicMessage
import common.clients.avatars.model.AvatarsCoordinates
import ru.yandex.vertis.palma.images.images.Image
import vertis.palma.external.avatars.TestAvatarsService
import vertis.palma.service.DictionaryBaseSpec
import vertis.palma.service.model.DictionaryException._
import vertis.palma.test.images_samples._
import vertis.zio.test.ZioSpecBase
import zio.Task

/** @author kusaeva
  */
class ProtoReflectionUtilsSpec extends ZioSpecBase with DictionaryBaseSpec {
  import ProtoReflectionUtilsSpec._

  "ProtoReflectionUtils" should {
    "correctly clean relations" in ioTest {
      for {
        actual <- ProtoReflectionUtils.processRelations(foo)
        _ <- check {
          actual shouldBe fooCleaned
        }
      } yield ()
    }
    "correctly clean images" in ioTest {
      for {
        cleaned <- ProtoReflectionUtils.processImages(
          DynamicMessage.parseFrom(VerbaPhoto.javaDescriptor, verbaPhoto.toByteArray)
        )
        _ <- check {
          VerbaPhoto.parseFrom(cleaned.toByteArray) shouldBe cleanedVerbaPhoto
        }
      } yield ()
    }
    "fail on inconsistent namespace" in {
      intercept[InconsistentImageNamespace] {
        ioTest {
          ProtoReflectionUtils.processImages(
            DynamicMessage.parseFrom(NeverbaPhoto.javaDescriptor, neverbaPhoto.toByteArray)
          )
        }
      }
    }
    "enrich image with aliases" in ioTest {
      for {
        enriched <- ProtoReflectionUtils.enrichResponse(new TestAvatarsService) {
          DynamicMessage.parseFrom(VerbaPhoto.javaDescriptor, verbaPhoto.toByteArray)
        }
        _ <- check {
          VerbaPhoto.parseFrom(enriched.toByteArray) shouldBe enrichedVerbaPhoto
        }
      } yield ()
    }

    "delete image on item delete" in ioTest {
      val brokenNamespace = "broken"
      val brokenImage = verbaPhoto.copy(image = Some(image.copy(namespace = brokenNamespace)))

      val avatarsService = new TestAvatarsService {
        override def delete(coordinates: AvatarsCoordinates): Task[Unit] =
          if (coordinates.namespace == brokenNamespace) {
            Task.fail(new RuntimeException("oops"))
          } else {
            Task.succeed(())
          }
      }

      for {
        _ <- ProtoReflectionUtils.postDelete(avatarsService) {
          DynamicMessage.parseFrom(VerbaPhoto.javaDescriptor, verbaPhoto.toByteArray)
        }
        _ <- ProtoReflectionUtils.postDelete(avatarsService) {
          DynamicMessage.parseFrom(VerbaPhoto.javaDescriptor, brokenImage.toByteArray)
        }
      } yield ()
    }
  }
}

object ProtoReflectionUtilsSpec {

  private val image = Image(
    groupId = "111",
    name = "qwerty",
    namespace = "verba",
    aliases = Map(
      "thumbnail" -> "thumbnail_url",
      "image" -> "image_url",
      "image2" -> "image2_url",
      "offer" -> "offer_url"
    )
  )

  private val verbaPhoto =
    VerbaPhoto(
      code = "test",
      image = Some(
        image
      )
    )

  private val enrichedVerbaPhoto =
    VerbaPhoto(
      code = "test",
      image = Some(
        image.copy(aliases =
          Map(
            "thumbnail" -> "localhost:80/get-verba/111/qwerty/thumbnail",
            "offer" -> "localhost:80/get-verba/111/qwerty/offer"
          )
        )
      )
    )

  private val cleanedVerbaPhoto =
    VerbaPhoto(
      code = "test",
      image = Some(
        image.copy(aliases = Map.empty)
      )
    )

  private val neverbaPhoto =
    NeverbaPhoto(
      code = "test",
      image = Some(
        image
      )
    )
}
