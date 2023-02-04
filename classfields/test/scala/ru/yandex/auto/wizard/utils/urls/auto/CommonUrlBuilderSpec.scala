package ru.yandex.auto.wizard.utils.urls.auto

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.region.{RegionService, RegionTree}
import ru.yandex.auto.searcher.configuration.CoreSearchConfiguration
import ru.yandex.auto.wizard.utils.urls.auto.fixture.UrlBuildersFixture
import ru.yandex.auto.wizard.utils.urls.moto.{
  MotoDesktopWizardUrlBuilder,
  MotoMobileWizardUrlBuilder,
  MotoWizardUrlBuilder
}
import ru.yandex.auto.wizard.utils.urls.trucks.{
  TrucksMobileWizardUrlBuilderImpl,
  TrucksWizardUrlBuilder,
  TrucksWizardUrlBuilderImpl
}

@RunWith(classOf[JUnitRunner])
class CommonUrlBuilderSpec extends WordSpec with UrlBuildersFixture with Matchers {
  "builders with review links" should {
    "create contain media prefix" in withUrlBuilderParams(addBodyTypes = false) { (params, sc) =>
      withHostHolder { hostHolder =>
        withWizardSearchConfiguration { wsc =>
          import params._
          val mediaLinkRoot = "https://media.test.avto.ru/"

          //auto
          def checkAutoUrls(builder: AutoWizardUrlBuilder[CoreSearchConfiguration]) {
            import builder._

            buildCommonReviewUrl(sc, intention) should startWith(mediaLinkRoot)
            buildMarkReviewUrl(sc, intention, mark) should startWith(mediaLinkRoot)
            buildModelReviewUrl(sc, intention, mark, model, superGenIds) should startWith(mediaLinkRoot)
            buildReviewUrl(sc, intention, mark, model, superGen, reviewId) should startWith(mediaLinkRoot)
          }

          checkAutoUrls(new AutoMobileWizardUrlBuilderImpl(hostHolder))
          checkAutoUrls(new AutoWizardUrlBuilderImpl(hostHolder))
          checkAutoUrls(new CarsMordaMobileUrlBuilder(hostHolder))

          // trucks
          def checkTruckUrls(builder: TrucksWizardUrlBuilder) = {
            import builder._
            buildCategoryUrlReviews(wsc, trucksType) should startWith(mediaLinkRoot)
            buildMarkUrlReviews(wsc, trucksType, trucksMark) should startWith(mediaLinkRoot)
            buildModelUrlReviews(wsc, trucksMark, trucksType, trucksModel) should startWith(mediaLinkRoot)
          }
          checkTruckUrls(new TrucksMobileWizardUrlBuilderImpl(hostHolder))
          checkTruckUrls(new TrucksWizardUrlBuilderImpl(hostHolder))

          // moto
          def checkMotoUrls(builder: MotoWizardUrlBuilder) = {
            //todo verify logic - same signature
            builder.buildReviewModelUrl(wsc, motoMark, motoType, motoModel) should startWith(mediaLinkRoot)
            builder.buildModelReviewsUrl(wsc, motoMark, motoType, motoModel) should startWith(mediaLinkRoot)

            builder.buildMarkReviewsUrl(wsc, motoType, motoMark) should startWith(mediaLinkRoot)
            builder.buildCategoryReviewsUrl(wsc, motoType) should startWith(mediaLinkRoot)
          }

          val regionService = stub[RegionService]
          val regionTree = new RegionTree(region)

          (regionService.getRegionTree _).when().returns(regionTree)

          regionService.getRegionTree().getRegion(sc.getWizardRegion().getId());
          checkMotoUrls(new MotoDesktopWizardUrlBuilder(hostHolder).setRegionService(regionService))
          checkMotoUrls(new MotoMobileWizardUrlBuilder(hostHolder).setRegionService(regionService))
        }
      }
    }
  }
}
