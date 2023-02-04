package ru.yandex.realty2.extdataloader.loaders.sites.genplan

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.palma.NewbuildingGenplanPolygonsOuterClass.NewbuildingGenplanPolygons
import realty.palma.NewbuildingGenplanPolygonsOuterClass.NewbuildingGenplanPolygons.HousePolygon
import realty.palma.newbuilding_genplan_polygons.NewbuildingGenplanPolygons.fromJavaProto
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.sites.gallery.SiteGalleryType
import ru.yandex.realty.model.sites.{House, Phase, Photo, Site}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class GenplanEnricherSpec extends SpecBase {

  private val siteId = 1;
  private val genplanUrlPrefix = "//avatars.mds.yandex.net/get-verba/93714"

  "GenplanEnricher" should {
    "enrich newbuilding with genplan polygons if they exists" in {
      val genplanEnricher = initGenplanEnricher(genplanUrlPrefix)

      val siteWithGenplan = buildSite(siteId)
      genplanEnricher.enrich(siteWithGenplan)
      siteWithGenplan.getPhases.get(0).getHouses.get(0).getGenplanPolygon.getHouseId shouldBe 101
      siteWithGenplan.getPhases.get(0).getHouses.get(1).getGenplanPolygon.getHouseId shouldBe 102
      siteWithGenplan.getPhases.get(0).getHouses.get(2).getGenplanPolygon.getHouseId shouldBe 103
      siteWithGenplan.getPhases.get(1).getHouses.get(0).getGenplanPolygon.getHouseId shouldBe 201
      siteWithGenplan.getPhases.get(1).getHouses.get(1).getGenplanPolygon.getHouseId shouldBe 202
      siteWithGenplan.getPhases.get(1).getHouses.get(2).getGenplanPolygon.getHouseId shouldBe 203
      siteWithGenplan.getPhases.get(2).getHouses.get(0).getGenplanPolygon.getHouseId shouldBe 301
      siteWithGenplan.getPhases.get(2).getHouses.get(1).getGenplanPolygon.getHouseId shouldBe 302
      siteWithGenplan.getPhases.get(2).getHouses.get(2).getGenplanPolygon.getHouseId shouldBe 303
    }

    "don't enrich newbuilding for wrong url" in {
      val genplanEnricher = initGenplanEnricher("//avatars.mds.yandex.net/get-verba/937147/")

      val siteWithoutGenplan = buildSite(siteId)
      genplanEnricher.enrich(siteWithoutGenplan)
      siteWithoutGenplan.getPhases.get(0).getHouses.get(0).getGenplanPolygon shouldBe null
    }

    "don't enrich newbuilding for wrong houseId" in {
      val genplanEnricher = initGenplanEnricher(genplanUrlPrefix, 30)

      val siteWithoutGenplan = buildSite(siteId)
      genplanEnricher.enrich(siteWithoutGenplan)
      siteWithoutGenplan.getPhases.get(0).getHouses.get(0).getGenplanPolygon shouldBe null
    }

    "don't enrich newbuilding for wrong siteId" in {
      val genplanEnricher = initGenplanEnricher(genplanUrlPrefix)

      val siteWithoutGenplan = buildSite(2)
      genplanEnricher.enrich(siteWithoutGenplan)
      siteWithoutGenplan.getPhases.get(0).getHouses.get(0).getGenplanPolygon shouldBe null
    }
  }

  private def initGenplanEnricher(genplanUrlPrefix: String, brakeHouseId: Int = 0) = {
    val genplanData = NewbuildingGenplanPolygons
      .newBuilder()
      .setNewbuildingId("1")
      .setGenplanUrl(genplanUrlPrefix + "/genplan")
      .addPolygons(HousePolygon.newBuilder().setHouseId(101 + brakeHouseId).build())
      .addPolygons(HousePolygon.newBuilder().setHouseId(102 + brakeHouseId).build())
      .addPolygons(HousePolygon.newBuilder().setHouseId(103 + brakeHouseId).build())
      .addPolygons(HousePolygon.newBuilder().setHouseId(201 + brakeHouseId).build())
      .addPolygons(HousePolygon.newBuilder().setHouseId(202 + brakeHouseId).build())
      .addPolygons(HousePolygon.newBuilder().setHouseId(203 + brakeHouseId).build())
      .addPolygons(HousePolygon.newBuilder().setHouseId(301 + brakeHouseId).build())
      .addPolygons(HousePolygon.newBuilder().setHouseId(302 + brakeHouseId).build())
      .addPolygons(HousePolygon.newBuilder().setHouseId(303 + brakeHouseId).build())
      .build()
    new GenplanEnricher(Map(1L -> fromJavaProto(genplanData)))
  }

  private def buildSite(siteId: Long): Site = {
    val site = new Site(siteId)
    var phases = Seq[Phase]()
    for (phaseId <- 1 to 3) {
      val phase = new Phase(phaseId)
      var houses = Seq[House]()
      for (houseId <- 1 to 3) {
        houses = houses :+ new House(phaseId * 100 + houseId)
      }
      phase.setHouses(houses.asJava)
      phases = phases :+ phase
    }
    site.setPhases(phases.asJava)

    val photoGeneral = new Photo("rawImageGeneral", "sourceNameGeneral", null)
    photoGeneral.setUrlPrefix("//avatars.mds.yandex.net/get-verba/93714/general")
    photoGeneral.setViewType(SiteGalleryType.GENERAL)
    val photoGenplan = new Photo("rawImage", "sourceName", null)
    photoGenplan.setUrlPrefix(genplanUrlPrefix + "/genplan")
    photoGenplan.setViewType(SiteGalleryType.GENPLAN)
    val photoEntrance = new Photo("rawImageEntrance", "sourceNameEntrance", null)
    photoEntrance.setUrlPrefix("//avatars.mds.yandex.net/get-verba/93714/entrace")
    photoEntrance.setViewType(SiteGalleryType.ENTRANCE)

    site.setPhotos(Seq(photoGeneral, photoGenplan, photoEntrance).asJava)
    site
  }

}
