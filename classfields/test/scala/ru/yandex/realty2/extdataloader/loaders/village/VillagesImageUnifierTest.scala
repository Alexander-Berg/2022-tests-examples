package ru.yandex.realty2.extdataloader.loaders.village

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, OneInstancePerTest, WordSpec}
import ru.yandex.realty.proto.village.{Village, VillagePhotoType}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class VillagesImageUnifierTest extends WordSpec with MockFactory with Matchers with OneInstancePerTest {

  val imageUnifier = new VillagesImageUnifierImpl()

  "VillageImageUnifier" should {
    "work correct" in {
      val village = Village.newBuilder()
      village.setId(1L)
      village.setName("TheVillage")

      addPhoto(
        village,
        "http://avatars.mds.yandex.net:80/get-verba/937147/2a000001609d9634f622d70f50e9f6d26978/optimize"
      )

      addPhoto(
        village,
        "http://avatars.mds.yandex.net:80/get-verba/937147/2a000001609328b4f68ee1a95d3af5fbceb0/optimize"
      )

      imageUnifier.process(Seq(village))

      village.getPhotoCount should be(2)

      village.getPhotoList.asScala.map(_.getPhoto.getUrlPrefix) should be(
        Seq(
          "//avatars.mds.yandex.net/get-verba/937147/2a000001609d9634f622d70f50e9f6d26978",
          "//avatars.mds.yandex.net/get-verba/937147/2a000001609328b4f68ee1a95d3af5fbceb0"
        )
      )
    }
  }

  private def addPhoto(village: Village.Builder, url: String): Unit = {
    val photo = village.addPhotoBuilder()
    photo.setType(VillagePhotoType.VILLAGE_PHOTO_TYPE_COMMON)
    val image = photo.getPhotoBuilder
    val rawPhoto = image.getRawImageBuilder
    rawPhoto.setOrigUrl(url)
  }
}
