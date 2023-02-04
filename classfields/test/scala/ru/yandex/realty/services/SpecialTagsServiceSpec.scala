package ru.yandex.realty.services

import ru.yandex.realty.storage.pinned.PinnedSpecialProjectsTestComponents
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.model.sites.Site
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.CommonConstants.SAMOLET_DEVELOPER_ID
import ru.yandex.realty.model.phone.RealtyPhoneTags.SamoletSpecialTagName
import ru.yandex.realty.model.region.{NodeRgid, Regions}

import java.util.Random
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SpecialTagsServiceSpec extends SpecBase with PinnedSpecialProjectsTestComponents with FeaturesStubComponent {
  private var randomResult = 1
  private val random = new Random {
    override def nextInt(bound: Int): Int = randomResult
  }
  private val specialCallsRatioFeature = features.SpecialCallsRatio
  private val specialTagsService =
    new SpecialTagsService(pinnedSpecialProjectsProvider, random, specialCallsRatioFeature)
  private val mskSamoletSite = new Site(2890148)
  mskSamoletSite.setBuilders(Seq(Long.box(SAMOLET_DEVELOPER_ID)).asJava)
  private val mskLocation = new Location()
  mskLocation.setSubjectFederation(Regions.MSK_AND_MOS_OBLAST, NodeRgid.MOSCOW_AND_MOS_OBLAST)
  mskSamoletSite.setLocation(mskLocation)
  private val nonSpecialSamoletSite = new Site(2)
  nonSpecialSamoletSite.setBuilders(Seq(Long.box(SAMOLET_DEVELOPER_ID)).asJava)
  nonSpecialSamoletSite.setLocation(mskLocation)
  private val spbSamoletSite = new Site(57547)
  spbSamoletSite.setBuilders(Seq(Long.box(SAMOLET_DEVELOPER_ID)).asJava)
  private val spbLocation = new Location()
  spbLocation.setSubjectFederation(Regions.SPB_AND_LEN_OBLAST, NodeRgid.SPB_AND_LEN_OBLAST)
  spbSamoletSite.setLocation(spbLocation)
  private val vertoletSite = new Site(1)
  vertoletSite.setBuilders(Seq(Long.box(102322L)).asJava)
  private val novosibLocation = new Location()
  novosibLocation.setSubjectFederation(Regions.NOVOSIBIRSKAYA_OBLAST, NodeRgid.NOVOSIBIRSKAYA_OBLAST)
  vertoletSite.setLocation(novosibLocation)

  "SpecialTagsService in getTag" should {
    "return None if specialCallsRation feature is disabled" in {
      specialCallsRatioFeature.setNewState(false)
      specialTagsService.getTag(mskSamoletSite) shouldBe None
    }

    "return None if there is no samolet special project in newbuilding's geo" in {
      specialCallsRatioFeature.setNewState(true)
      specialTagsService.getTag(vertoletSite) shouldBe None
    }

    "return None if there is no newbuilding id in special project's newbuildings" in {
      specialCallsRatioFeature.setNewState(true)
      specialTagsService.getTag(nonSpecialSamoletSite) shouldBe None
    }

    "return None if special calls ratio is not defined for a special project" in {
      specialCallsRatioFeature.setNewState(true)
      specialTagsService.getTag(spbSamoletSite) shouldBe None
    }

    "return None if random.nextInt != 0" in {
      specialCallsRatioFeature.setNewState(true)
      specialTagsService.getTag(mskSamoletSite) shouldBe None
    }

    "return samolet special tag if random.nextInt == 0" in {
      specialCallsRatioFeature.setNewState(true)
      randomResult = 0
      specialTagsService.getTag(mskSamoletSite) shouldBe Some(SamoletSpecialTagName)
    }
  }
}
