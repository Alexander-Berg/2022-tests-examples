package ru.yandex.auto.wizard.utils.urls.auto.fixture

import java.net.URL
import java.util.Collections

import org.mortbay.util.StringUtil
import org.scalamock.scalatest.MockFactory
import org.scalatest.{OneInstancePerTest, TestSuite}
import ru.yandex.auto.core.catalog.model.moto.{MotoMark, MotoModel, MotoType}
import ru.yandex.auto.core.catalog.model.trucks.{TrucksMark, TrucksModel, TrucksType}
import ru.yandex.auto.core.catalog.model.{Complectation, Configuration, Mark, Model, NamePlate, SuperGeneration}
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.core.region.{Region, RegionType}
import ru.yandex.auto.core.wizard.OpenStatTag
import ru.yandex.auto.searcher.configuration.{CoreSearchConfiguration, WizardSearchConfiguration}
import ru.yandex.auto.searcher.sort.SortType
import ru.yandex.auto.wizard.AdSnippet
import ru.yandex.auto.wizard.result.builders.WizardIntentionType
import ru.yandex.auto.wizard.result.builders.auto.utils.SearchConfigurationUtils
import ru.yandex.auto.wizard.result.data.alisa.AlisaResultType
import ru.yandex.auto.wizard.utils.HostHolder
import ru.yandex.common.util.StringUtils

import scala.collection.JavaConversions._

trait UrlBuildersFixture extends MockFactory with OneInstancePerTest { self: TestSuite =>

  val TestHostHolder: HostHolder = {
    val TestUrl = "test.avto.ru"
    val MobileTestUrl = "m.test.avto.ru"
    val MediaUrl = "media.test.avto.ru"

    new HostHolder(
      TestUrl,
      TestUrl,
      MobileTestUrl,
      MobileTestUrl,
      TestUrl,
      TestUrl,
      MobileTestUrl,
      MobileTestUrl,
      TestUrl,
      TestUrl,
      MobileTestUrl,
      MobileTestUrl,
      TestUrl,
      TestUrl,
      MobileTestUrl,
      MobileTestUrl,
      MediaUrl,
      MediaUrl
    )
  }

  object UrlBuilderParams {

    val intention: WizardIntentionType = WizardIntentionType.LISTING
    val mark: Mark = stub[Mark]
    (mark.getCode _).when().returns("test_mark")

    val model: Model = stub[Model]
    (model.getCode _).when().returns("test_model")
    (model.getNamePlatesFront _).when().returns(List.empty[NamePlate])

    val superGenIds: java.util.Set[java.lang.Long] = Set(1L, 3L, 5L, 7L).map(long2Long)
    val superGen: SuperGeneration = stub[SuperGeneration]
    (superGen.getBoxedId _).when().returns(1L)

    val states: java.util.List[State.Search] = Seq(State.Search.NEW, State.Search.USED)
    val adSnippet: AdSnippet = stub[AdSnippet]

    val complectation: Complectation = stub[Complectation]
    val generationId: Long = 123
    val configurationId: Long = 124

    val reviewId: String = "test_review"
    val catalogSuffix: String = "test_catalog"

    val carConfig: Configuration = stub[Configuration]
    (carConfig.getId _).when().returns(1L)
    val alisaResultType: AlisaResultType = AlisaResultType.AUTO

    val sortType: SortType = SortType.POWER
    val carState: State = State.GOOD

    val techParamId: Long = 12L

    val trucksType: TrucksType = stub[TrucksType]
    (trucksType.getUrlCode _).when().returns("test_truck_url_code")
    (trucksType.getCode _).when().returns("test_truck_type_code")

    val trucksMark: TrucksMark = stub[TrucksMark]
    (trucksMark.getCode _).when().returns("test_trucks_mark_code")

    val trucksModel: TrucksModel = stub[TrucksModel]
    (trucksModel.getCode _).when().returns("test_trucks_model_code")

    val motoType: MotoType = stub[MotoType]
    (motoType.getUrlCode _).when().returns("test_moto_url_code")
    (motoType.getCode _).when().returns("test_moto_type_code")

    val motoMark: MotoMark = stub[MotoMark]
    (motoMark.getCode _).when().returns("test_moto_mark_code")

    val motoModel: MotoModel = stub[MotoModel]
    (motoModel.getCode _).when().returns("test_moto_model_code")

    val region = new Region(1, "test", RegionType.CITY, null, 1.0, 1.0)
  }

  def assertEqualUrls(left: String, right: String): Unit = {

    val (leftUrl, rightUrl) = new URL(left) -> new URL(right)
    val leftKeys = leftUrl.getQuery.split("&").sorted
    val rightKeys = rightUrl.getQuery.split("&").sorted

    val leftPath = leftUrl.getPath
    val rightPath = rightUrl.getPath

    assert(leftPath == rightPath)
    assert(leftKeys.sorted.mkString("\n") == rightKeys.sorted.mkString("\n"))
  }

  def withSearchConfiguration[T](addBodyTypes: Boolean)(body: CoreSearchConfiguration => T): T = {
    val sc = stub[CoreSearchConfiguration]
    val bodyTypes = if (addBodyTypes) List("SEDAN") else List("test")
    val transmissions = List.empty[String]
    val engineTypes = List.empty[String]
    val gearTypes = List.empty[String]
    val nameplates = List.empty[String]
    val states = List.empty[State.Search]
    val superGenerations = Set.empty[java.lang.Long]

    (sc.getWizardSubtype _).when().returns("test")
    (sc.getOpenStatTag _).when().returns(OpenStatTag.Empty)
    (sc.getUtmCampaign _).when().returns("test")
    (sc.getWizardRegion _).when().returns(UrlBuilderParams.region)
    (sc.getParamBodyTypes _).when().returns(bodyTypes)
    (sc.getTransmissions _).when().returns(transmissions)
    (sc.getEngineTypes _).when().returns(engineTypes)
    (sc.getGearTypes _).when().returns(gearTypes)
    (sc.getCorrectedStates _).when().returns(states)
    (sc.getNameplates _).when().returns(nameplates)
    (sc.isMobileWizard _).when().returns(true)
    (sc.getParamSuperGenerations _).when().returns(superGenerations)
    (sc.getSortOffers _).when().returns(SearchConfigurationUtils.DefaultSort)
    (sc.getUrlColors _).when().returns(Collections.emptySet())

    body(sc)
  }

  def withWizardSearchConfiguration[T](body: WizardSearchConfiguration => T): T = {
    val sc = stub[WizardSearchConfiguration]
    (sc.getWizardSubtype _).when().returns("test")
    (sc.getWizardRegion _).when().returns(UrlBuilderParams.region)
    (sc.isMobileWizard _).when().returns(true)
    (sc.getOpenStatTag _).when().returns(OpenStatTag.Empty)
    (sc.getUtmCampaign _).when().returns("test")

    body(sc)
  }

  def withHostHolder[T](body: HostHolder => T): T = {
    body(TestHostHolder)
  }

  def withUrlBuilderParams[T](addBodyTypes: Boolean)(body: (UrlBuilderParams.type, CoreSearchConfiguration) => T): T = {
    withSearchConfiguration(addBodyTypes) { sc =>
      body(UrlBuilderParams, sc)
    }
  }

}
