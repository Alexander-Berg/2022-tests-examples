package ru.yandex.auto.wizard.utils.urls.auto

import java.util.Optional

import org.junit.runner.RunWith
import org.scalatest.{GivenWhenThen, Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.model.enums.State
import ru.yandex.auto.core.wizard.RearrsInfo
import ru.yandex.auto.wizard.utils.urls.WizardUrlConst
import ru.yandex.auto.wizard.utils.urls.auto.fixture.UrlBuildersFixture
import ru.yandex.extdata.core.logging.Logging

@RunWith(classOf[JUnitRunner])
class AutoMobileWizardUrlBuilderImplSpec
    extends WordSpec
    with Matchers
    with GivenWhenThen
    with Logging
    with UrlBuildersFixture {

  "MobileWizardUrlBuilder" should {
    "return equal results in old and new versions" in withUrlBuilderParams(addBodyTypes = false) { (params, sc) =>
      import params._
      val mobileBuilder = new AutoMobileWizardUrlBuilderImpl(TestHostHolder)

      assertEqualUrls(
        mobileBuilder.buildMainServiceUrl(sc, intention),
        "https://m.test.avto.ru/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildCommonReviewUrl(sc, intention),
        "https://media.test.avto.ru/reviews/cars/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildMarkReviewUrl(sc, intention, mark),
        "https://media.test.avto.ru/reviews/cars/test_mark/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildAddUrl(sc, intention, null),
        "https://m.test.avto.ru/add/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildPredictUrl(sc, intention),
        "https://m.test.avto.ru/cars/evaluation/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildCompareUrl(sc, intention),
        "https://m.test.avto.ru/compare/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildCertUrl(sc, intention),
        "https://m.test.avto.ru/cert/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildMarkListingUrl(sc, intention, mark),
        "https://m.test.avto.ru/cars/test_mark/used/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
          "&sort_offers=fresh_relevance_1-DESC&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildModelListingUrl(sc, intention, mark, model, enableDefaultSort = true),
        "https://m.test.avto.ru/cars/test_mark/test_model/used/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
          "&sort_offers=fresh_relevance_1-DESC&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildModelListingUrl(sc, intention, mark, model, superGenIds, sortType),
        "https://m.test.avto.ru/cars/test_mark/test_model/used/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
          "&sort_offers=power&geo_id=1&mark-model-nameplate=test_mark%23test_model%23%231" +
          "&mark-model-nameplate=test_mark%23test_model%23%233&mark-model-nameplate=test_mark%23test_model%23%235" +
          "&mark-model-nameplate=test_mark%23test_model%23%237"
      )

      assertEqualUrls(
        mobileBuilder.buildModelListingUrl(sc, intention, mark, model, superGenIds, null, sortType, states),
        "https://m.test.avto.ru/cars/test_mark/test_model/all/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&sort_offers=power" +
          "&geo_id=1&mark-model-nameplate=test_mark%23test_model%23%231" +
          "&mark-model-nameplate=test_mark%23test_model%23%233&mark-model-nameplate=test_mark%23test_model%23%235" +
          "&mark-model-nameplate=test_mark%23test_model%23%237"
      )

      assertEqualUrls(
        mobileBuilder.buildModelListingUrl(sc, intention, mark, Optional.of(model), superGenIds, adSnippet),
        "https://m.test.avto.ru/cars/test_mark/test_model/new/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
          "&sort_offers=fresh_relevance_1-DESC&geo_id=1&mark-model-nameplate=test_mark%23test_model%23%231" +
          "&mark-model-nameplate=test_mark%23test_model%23%233&mark-model-nameplate=test_mark%23test_model%23%235" +
          "&mark-model-nameplate=test_mark%23test_model%23%237"
      )

      assertEqualUrls(
        mobileBuilder
          .buildModelListingUrl(
            sc,
            intention,
            mark,
            model,
            complectation,
            techParamId,
            generationId,
            configurationId,
            State.Search.NEW
          ),
        "https://m.test.avto.ru/cars/new/group/test_mark/test_model/12/0/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&sort_offers=price-ASC&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildModelListingUrl(sc, intention, mark, model, superGenIds, complectation, sortType, states),
        "https://m.test.avto.ru/cars/test_mark/test_model/all/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&sort_offers=power" +
          "&geo_id=1&mark-model-nameplate=test_mark%23test_model%23%231" +
          "&mark-model-nameplate=test_mark%23test_model%23%233&mark-model-nameplate=test_mark%23test_model%23%235" +
          "&mark-model-nameplate=test_mark%23test_model%23%237"
      )

      assertEqualUrls(
        "https://m.test.avto.ru/cars/used/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
          "&sort_offers=fresh_relevance_1-DESC&geo_id=1",
        mobileBuilder.buildCommonListingUrl(sc, intention)
      )

      assertEqualUrls(
        mobileBuilder.buildCommonListingUrl(sc, intention, adSnippet),
        "https://m.test.avto.ru/cars/used/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
          "&sort_offers=fresh_relevance_1-DESC&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildVideoUrl(sc, intention),
        "https://m.test.avto.ru/video/cars/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )
      assertEqualUrls(
        mobileBuilder.buildVideoUrl(sc, intention, mark),
        "https://m.test.avto.ru/video/cars/test_mark/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )
      assertEqualUrls(
        mobileBuilder.buildVideoUrl(sc, intention, mark, model),
        "https://m.test.avto.ru/video/cars/test_mark/test_model/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildVideoUrl(sc, intention, mark, model, superGen),
        "https://m.test.avto.ru/video/cars/test_mark/test_model/1/" +
          "?from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildModelReviewUrlAlisa(sc, mark, model),
        "https://media.test.avto.ru/reviews/cars/test_mark/test_model/" +
          "?utm_campaign=auto.alisa.reviews&from=alisa.auto&utm_source=auto.alisa&utm_medium=touch&geo_id=1"
      )

      assertEqualUrls(
        mobileBuilder.buildModelListingUrlAlisa(sc, mark, model.getCode, superGenIds, adSnippet),
        "https://m.test.avto.ru/cars/test_mark/test_model/new/" +
          "?from=alisa.auto&utm_source=auto.alisa&utm_medium=touch&geo_id=1&utm_campaign=auto.alisa.listing" +
          "&mark-model-nameplate=test_mark%23test_model%23%231&mark-model-nameplate=test_mark%23test_model%23%233" +
          "&mark-model-nameplate=test_mark%23test_model%23%235&mark-model-nameplate=test_mark%23test_model%23%237" +
          "&sort_offers=fresh_relevance_1-DESC"
      )
    }

    "add scrolling anchor" in withUrlBuilderParams(addBodyTypes = false) { (params, sc) =>
      import params._

      val builder = new AutoMobileWizardUrlBuilderImpl(TestHostHolder)

      val rearrsInfo = mock[RearrsInfo]
      (sc.getRearrsInfo _ when ()).returns(rearrsInfo)

      val anchorPopular = "#" + WizardUrlConst.POPULAR_MODELS_ANCHOR

      val markListingUrl = inAnyOrder {
        builder.buildMarkListingUrl(sc, intention, mark)
      }
      markListingUrl should include(anchorPopular)

      val modelListingUrl = inAnyOrder {
        builder.buildModelListingUrl(sc, intention, mark, model, enableDefaultSort = true)
      }
      (modelListingUrl should not).include(anchorPopular)
    }
  }
}
