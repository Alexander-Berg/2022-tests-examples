package ru.yandex.auto.vin.decoder.scheduler.workers.data

import auto.carfax.common.clients.avatars.AvatarsClient
import cats.syntax.option._
import com.google.common.util.concurrent.RateLimiter
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.internal.Mds.MdsPhotoInfo
import auto.carfax.common.clients.avatars.AvatarsExceptions.SourceUrlNotFound
import auto.carfax.common.utils.tracing.Traced
import ru.yandex.auto.vin.decoder.proto.CommonModels.{OriginalSize, PhotoInfo}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AvatarsPhotoManagerTest extends AnyWordSpecLike with Matchers with MockitoSupport with BeforeAndAfter {

  private val client = mock[AvatarsClient]
  val rateLimiter = mock[RateLimiter]
  implicit val t: Traced = Traced.empty

  when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)

  val manager = new AvatarsPhotoManager(client, rateLimiter)

  before {
    reset(client)
  }

  "getPhotoInfo" should {
    "return None" when {
      "avatars throw unexpected error" in {
        when(client.putImageFromUrl(?, ?, ?)(?)).thenReturn(Future.failed(new RuntimeException("")))

        val photo = buildPhoto("url", false, false)
        val res = manager.getPhotoInfo(photo, List.empty).await
        res shouldBe None
      }
    }
    "return not found image" when {
      "avatars throw not found error" in {
        when(client.putImageFromUrl(?, ?, ?)(?)).thenReturn(Future.failed(new SourceUrlNotFound("url")))

        val photo = buildPhoto("url", false, false)
        val res = manager.getPhotoInfo(photo, List.empty).await
        res shouldBe buildPhoto("url", notFound = true, hasMdsInfo = false).some
      }
    }
    "return photo info" when {
      "photo already processed" in {
        val photo = buildPhoto("url", false, true)

        val res = manager.getPhotoInfo(photo, List(photo)).await
        res shouldBe photo.some
      }
      "avatars successfully upload photo to mds storage" in {
        val photo = buildPhoto("url", false, true)
        when(client.putImageFromUrl(?, ?, ?)(?)).thenReturn(Future.successful(photo))

        val res = manager.getPhotoInfo(photo, List.empty).await
        res shouldBe photo.some
      }
    }
  }

  "isImageProcessed" should {
    "return false" when {
      "image hase not mds info and  not_found = false" in {
        val photo = buildPhoto("photo", false, false)
        manager.isImageProcessed(photo) shouldBe false
      }
    }
    "return true" when {
      "images has mds info" in {
        val photo = buildPhoto("photo", false, true)
        manager.isImageProcessed(photo) shouldBe true
      }
      "images not_found = true" in {
        val photo = buildPhoto("photo", true, false)
        manager.isImageProcessed(photo) shouldBe true
      }
    }
  }

  "getAlreadySavedPhoto" should {
    "return empty map" when {
      "empty photo list" in {
        val res = manager.getAlreadySavedPhotos(List.empty)
        res.isEmpty shouldBe true
      }
      "no already processed photo" in {
        val photo1 = buildPhoto("photo1", false, false)
        val res = manager.getAlreadySavedPhotos(List(photo1))
        res.isEmpty shouldBe true
      }
    }
    "return processed photo" in {
      val photo1 = buildPhoto("photo1", false, true)
      val photo2 = buildPhoto("photo2", true, false)
      val res = manager.getAlreadySavedPhotos(List(photo1, photo2))

      println(res)

      res.length shouldBe 2
      res should contain(photo1)
      res should contain(photo2)
    }

  }

  private def buildPhoto(url: String, notFound: Boolean, hasMdsInfo: Boolean): PhotoInfo = {
    val builder = PhotoInfo.newBuilder().setExternalPhotoUrl(url).setNotFound(notFound)
    if (hasMdsInfo)
      builder
        .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setGroupId(System.currentTimeMillis().toInt).build())
        .setOriginalSize(OriginalSize.newBuilder.setX(100).setY(100))
    builder.build()
  }

}
