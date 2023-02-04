package ru.yandex.auto.wizard.utils.urls.auto

import java.util.Optional

import org.junit.runner.RunWith
import org.scalatest.{GivenWhenThen, Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.wizard.utils.urls.auto.fixture.UrlBuildersFixture
import ru.yandex.extdata.core.logging.Logging

@RunWith(classOf[JUnitRunner])
class AutoWizardUrlBuilderImplSpec
    extends WordSpec
    with Matchers
    with GivenWhenThen
    with Logging
    with UrlBuildersFixture {

  "AutoWizardUrlBuilderImpl" should {
    "return urls equal to references" in withUrlBuilderParams(addBodyTypes = false) {
      case (params, sc) =>
        import params._
        val builder = new AutoWizardUrlBuilderImpl(TestHostHolder)

        import builder._

        assertEqualUrls(
          buildMainServiceUrl(sc, intention),
          "https://test.avto.ru/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildCommonReviewUrl(sc, intention),
          "https://media.test.avto.ru/reviews/cars/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildMarkReviewUrl(sc, intention, mark),
          "https://media.test.avto.ru/reviews/cars/test_mark/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildModelReviewUrl(sc, intention, mark, model, superGenIds),
          "https://media.test.avto.ru/reviews/cars/test_mark/test_model/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1" +
            "&mark_model_nameplate=test_mark%23test_model%23%231" +
            "&mark_model_nameplate=test_mark%23test_model%23%233" +
            "&mark_model_nameplate=test_mark%23test_model%23%235" +
            "&mark_model_nameplate=test_mark%23test_model%23%237"
        )
        assertEqualUrls(
          buildReviewUrl(sc, intention, mark, model, superGen, reviewId),
          "https://media.test.avto.ru/review/cars/test_mark/test_model/0/test_review/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildAddUrl(sc, intention, null),
          "https://test.avto.ru/cars/used/add/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildPredictUrl(sc, intention),
          "https://test.avto.ru/cars/evaluation/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildCompareUrl(sc, intention),
          "https://test.avto.ru/compare/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildVinHistoryUrl(sc, intention, carState.name()),
          "https://test.avto.ru/history/GOOD/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildVinHistoryUrlFormat(sc, intention),
          "https://test.avto.ru/history/%3Ccar_id%3E/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildCertUrl(sc, intention),
          "https://test.avto.ru/cert/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildMarkListingUrl(sc, intention, mark),
          "https://test.avto.ru/cars/test_mark/used/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=fresh_relevance_1-DESC&geo_id=1"
        )
        assertEqualUrls(
          buildMarkCatalogUrl(sc, intention, mark),
          "https://test.avto.ru/catalog/cars/test_mark/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildModelListingUrl(sc, intention, mark, model, enableDefaultSort = true),
          "https://test.avto.ru/cars/test_mark/test_model/used/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=fresh_relevance_1-DESC&geo_id=1"
        )
        assertEqualUrls(
          buildModelListingUrl(sc, intention, mark, model, superGenIds, sortType),
          "https://test.avto.ru/cars/test_mark/test_model/used/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=power&geo_id=1&mark-model-nameplate=test_mark%23test_model%23%231" +
            "&mark-model-nameplate=test_mark%23test_model%23%233" +
            "&mark-model-nameplate=test_mark%23test_model%23%235" +
            "&mark-model-nameplate=test_mark%23test_model%23%237"
        )
        assertEqualUrls(
          buildModelListingUrl(sc, intention, mark, model, superGenIds, null, sortType, states),
          "https://test.avto.ru/cars/test_mark/test_model/all/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&sort_offers=power" +
            "&geo_id=1&mark-model-nameplate=test_mark%23test_model%23%231" +
            "&mark-model-nameplate=test_mark%23test_model%23%233&mark-model-nameplate=test_mark%23test_model%23%235" +
            "&mark-model-nameplate=test_mark%23test_model%23%237"
        )
        assertEqualUrls(
          buildModelListingUrl(sc, intention, mark, model, superGenIds, complectation, sortType, states),
          "https://test.avto.ru/cars/test_mark/test_model/all/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&sort_offers=power&geo_id=1" +
            "&mark-model-nameplate=test_mark%23test_model%23%231&mark-model-nameplate=test_mark%23test_model%23%233" +
            "&mark-model-nameplate=test_mark%23test_model%23%235&mark-model-nameplate=test_mark%23test_model%23%237"
        )
        assertEqualUrls(
          buildModelListingUrl(
            sc,
            intention,
            mark,
            model,
            complectation,
            1L,
            generationId,
            configurationId,
            states.get(0)
          ),
          "https://test.avto.ru/cars/new/group/test_mark/test_model/123-124/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=price-ASC&geo_id=1&catalog_filter=model%3Dtest_model%2Cmark%3Dtest_mark%2Cgeneration%3D123%2Cconfiguration%3D124%2Ccomplectation_name%3Dnull%2Ctech_param%3D1"
        )
        assertEqualUrls(
          buildModelListingUrl(sc, intention, mark, Optional.of(model), superGenIds, adSnippet),
          "https://test.avto.ru/cars/test_mark/test_model/new/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=fresh_relevance_1-DESC&geo_id=1&mark-model-nameplate=test_mark%23test_model%23%231" +
            "&mark-model-nameplate=test_mark%23test_model%23%233&mark-model-nameplate=test_mark%23test_model%23%235" +
            "&mark-model-nameplate=test_mark%23test_model%23%237"
        )
        assertEqualUrls(
          buildConfigurationListingUrl(sc, intention, mark, model, superGen, carConfig, states),
          "https://test.avto.ru/cars/test_mark/test_model/0/1/all/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildModelCatalogUrl(sc, intention, mark, model),
          "https://test.avto.ru/catalog/cars/test_mark/test_model/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildConfigurationCatalogUrl(sc, intention, mark, model, superGen, carConfig, catalogSuffix),
          "https://test.avto.ru/catalog/cars/test_mark/test_model/0/1/test_catalog/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildCommonListingUrl(sc, intention),
          "https://test.avto.ru/cars/used/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=fresh_relevance_1-DESC&geo_id=1#popularMMM"
        )
        assertEqualUrls(
          buildCommonListingUrl(sc, intention),
          "https://test.avto.ru/cars/used/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=fresh_relevance_1-DESC&geo_id=1#popularMMM"
        )
        assertEqualUrls(
          buildDKPUrl(sc, intention),
          "https://test.avto.ru/docs/dkp/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildCommonCatalogUrl(sc, intention),
          "https://test.avto.ru/catalog/cars/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildVideoUrl(sc, intention),
          "https://test.avto.ru/video/cars/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildVideoUrl(sc, intention),
          "https://test.avto.ru/video/cars/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildVideoUrl(sc, intention),
          "https://test.avto.ru/video/cars/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildVideoUrl(sc, intention),
          "https://test.avto.ru/video/cars/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
        assertEqualUrls(
          buildConfigurationCatalogUrlForAlisa(sc, alisaResultType, mark, model, superGen, carConfig),
          "https://test.avto.ru/catalog/cars/test_mark/test_model/0/1/?"
            + "from=alisa.auto&utm_source=auto.alisa&utm_medium=touch&geo_id=1&utm_campaign=auto.alisa.catalog"
        )
        assertEqualUrls(
          buildAutoPartsUrl(sc, intention, mark, model, superGen),
          "https://test.avto.ru/parts/avtotovary/test_mark/test_model/1/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&geo_id=1"
        )
    }

    "return new types of urls with body type inside path equal to references" in withUrlBuilderParams(
      addBodyTypes = true
    ) {
      case (params, sc) =>
        import params._
        val builder = new AutoWizardUrlBuilderImpl(TestHostHolder)

        import builder._

        assertEqualUrls(
          buildMarkListingUrl(sc, intention, mark),
          "https://test.avto.ru/cars/test_mark/used/body-sedan/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=fresh_relevance_1-DESC&geo_id=1"
        )
        assertEqualUrls(
          buildMarkListingUrl(sc, intention, mark),
          "https://test.avto.ru/cars/test_mark/used/body-sedan/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=fresh_relevance_1-DESC&geo_id=1"
        )
        assertEqualUrls(
          buildModelListingUrl(sc, intention, mark, model, enableDefaultSort = true),
          "https://test.avto.ru/cars/test_mark/test_model/used/body-sedan/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&sort_offers=fresh_relevance_1-DESC&geo_id=1"
        )
        assertEqualUrls(
          buildModelListingUrl(sc, intention, mark, model, enableDefaultSort = true),
          "https://test.avto.ru/cars/test_mark/test_model/used/body-sedan/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=fresh_relevance_1-DESC&geo_id=1"
        )
        assertEqualUrls(
          buildModelListingUrl(sc, intention, mark, model, superGenIds, sortType),
          "https://test.avto.ru/cars/test_mark/test_model/used/body-sedan/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=power&geo_id=1&mark-model-nameplate=test_mark%23test_model%23%231" +
            "&mark-model-nameplate=test_mark%23test_model%23%233" +
            "&mark-model-nameplate=test_mark%23test_model%23%235" +
            "&mark-model-nameplate=test_mark%23test_model%23%237"
        )
        assertEqualUrls(
          buildModelListingUrl(sc, intention, mark, model, superGenIds, null, sortType, states),
          "https://test.avto.ru/cars/test_mark/test_model/all/body-sedan/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&sort_offers=power" +
            "&geo_id=1&mark-model-nameplate=test_mark%23test_model%23%231" +
            "&mark-model-nameplate=test_mark%23test_model%23%233&mark-model-nameplate=test_mark%23test_model%23%235" +
            "&mark-model-nameplate=test_mark%23test_model%23%237"
        )
        assertEqualUrls(
          buildModelListingUrl(sc, intention, mark, model, superGenIds, complectation, sortType, states),
          "https://test.avto.ru/cars/test_mark/test_model/all/body-sedan/?"
            + "from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone&sort_offers=power&geo_id=1" +
            "&mark-model-nameplate=test_mark%23test_model%23%231&mark-model-nameplate=test_mark%23test_model%23%233" +
            "&mark-model-nameplate=test_mark%23test_model%23%235&mark-model-nameplate=test_mark%23test_model%23%237"
        )
        assertEqualUrls(
          buildModelListingUrl(
            sc,
            intention,
            mark,
            model,
            complectation,
            1L,
            generationId,
            configurationId,
            states.get(0)
          ),
          "https://test.avto.ru/cars/new/group/test_mark/test_model/123-124/?"
            + "autoru_body_type=SEDAN&from=wizard.test&utm_source=auto_wizard&utm_medium=touch&utm_campaign=test&utm_content=listing&_openstat=none%3Bnone%3Bnone%3Bnone" +
            "&sort_offers=price-ASC&geo_id=1&catalog_filter=model%3Dtest_model%2Cmark%3Dtest_mark%2Cgeneration%3D123%2Cconfiguration%3D124%2Ccomplectation_name%3Dnull%2Ctech_param%3D1"
        )
    }
  }
}
