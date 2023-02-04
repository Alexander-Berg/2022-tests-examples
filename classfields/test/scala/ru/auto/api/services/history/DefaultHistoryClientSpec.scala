package ru.auto.api.services.history

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import org.scalatest.OptionValues
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.favorite.SearchHistoryItem
import ru.auto.api.model.{CategorySelector, RequestParams}
import ru.auto.api.model.CategorySelector.{Cars, Moto, Trucks}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.history.HistoryEntity
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.RequestImpl
import ru.auto.api.util.StringUtils._
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.duration._

class DefaultHistoryClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with OptionValues
  with MockitoSupport {

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  when(featureManager.oldOptionsSearchMapping).thenReturn(feature)
  when(featureManager.allowSearcherRequestEnrichmentWithExpFlags).thenReturn(Feature("", _ => true))
  when(featureManager.allowSearcherRequestEnrichmentWithGlobalFlags).thenReturn(Feature("", _ => true))
  when(featureManager.dealerBoostCoefficient).thenReturn(Feature("", _ => 1.1f))

  val defaultsMapper = new DefaultsMapper(featureManager)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)

  private val historyClient = new DefaultHistoryClient(http)

  implicit val request: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(trace)
    req.setRequestParams(RequestParams.empty)
    req
  }

  private def getDomain(offer: Offer): String =
    "autoru:" + (CategorySelector.from(offer.getCategory) match {
      case Cars => "car_offers"
      case Moto => "moto_offers"
      case Trucks => "commercial_offers"
    })

  "HistoryClient" should {
    "get history" in {
      forAll(PersonalUserRefGen, HashedOfferIDGen) { (user, offerID) =>
        http.expectUrl(GET, url"/history/1.0/autoru%3Acar_offers%2Cmoto_offers%2Ccommercial_offers/$user")
        http.respondWithJson(
          OK,
          s"""{
            "service": "autoru",
            "version": "1.0",
            "user": "${user.toPlain}",
            "entities": {
              "car_offers": [{
                "entity_id": "${offerID.toPlain}",
                "visit_timestamp": 1520244286700,
                "add_count": 1
              }]
            }
          }"""
        )

        historyClient.getHistory(user).futureValue shouldBe Seq(
          HistoryEntity(offerID, 1520244286700L, Some(1))
        )
      }
    }

    "add history" in {
      forAll(OfferGen, PersonalUserRefGen) { (offer, user) =>
        val domain = getDomain(offer)
        http.expectUrl(PUT, url"/history/1.0/$domain/$user/${offer.id}?update_interval=5+minutes")
        http.respondWithJson(
          OK,
          s"""{
          "status": "OK",
          "entity": {
             "entity_id": "${offer.id.toPlain}",
             "visit_timestamp": 1520244286700,
             "add_count": 3
           }
          }"""
        )

        val res = historyClient.addHistory(offer, user, Some(5.minutes)).await
        res.status shouldBe "OK"
        res.entity.entityId shouldBe offer.id
        res.entity.visitTimestamp shouldBe 1520244286700L
        res.entity.addCount.value shouldBe 3
      }
    }

    "move history" in {
      forAll(AnonymousUserRefGen, PrivateUserRefGen) { (anon, user) =>
        http.expectUrl(
          POST,
          url"/history/1.0/autoru%3Acar_offers%2Cmoto_offers%2Ccommercial_offers/$anon/move/$user"
        )
        http.respondWithJson(OK, """{"status": "OK"}""")

        historyClient.moveHistory(anon, user).futureValue
      }
    }

    "move search history" in {
      forAll(AnonymousUserRefGen, PrivateUserRefGen) { (anon, user) =>
        http.expectUrl(
          POST,
          url"/history/1.0/autoru:searches_history/$anon/move/$user"
        )
        http.respondWithJson(OK, """{"status": "OK"}""")

        historyClient.moveSearchHistory(anon, user).futureValue
      }
    }

    "add searches history" in {
      forAll(PersonalUserRefGen, SearchFilterGen) { (user, filter) =>
        val id = searchMappings.generateId(Cars, filter)
        http.expectUrl(PUT, url"/history/1.0/autoru:searches_history/$user/$id")
        http.respondWithJson(OK, """{"status": "OK"}""")

        historyClient.addSearchHistory(user, SearchHistoryItem(id, title = "", Cars, filter, None, None)).futureValue
      }
    }

    "get searches history" in {
      forAll(PersonalUserRefGen) { (user) =>
        http.expectUrl(GET, url"/history/1.0/autoru:searches_history/$user")
        http.respondWithJson(
          OK,
          s"""{
            "service": "autoru",
            "version": "1.0",
            "user": "${user.toPlain}",
            "entities": {
              "searches_history": [{
                "entity_id": "testId",
                "visit_timestamp": 1520244286700,
                "add_count": 1,
                "payload": "{\\"category\\":\\"cars\\",\\"params\\":{\\"carsParams\\":{\\"bodyTypeGroup\\":[\\"ANY_BODY\\"],\\"seatsGroup\\":\\"ANY_SEATS\\",\\"engineGroup\\":[\\"ANY_ENGINE\\"]},\\"currency\\":\\"RUR\\",\\"hasImage\\":true,\\"inStock\\":\\"ANY_STOCK\\",\\"state\\":[\\"NEW\\",\\"USED\\"],\\"customStateKey\\":[\\"CLEARED\\"],\\"exchangeStatus\\":[\\"NO_EXCHANGE\\"],\\"markModelNameplate\\":[\\"BMW\\"],\\"dealerOrgType\\":[1,2,4],\\"stateGroup\\":\\"ALL\\",\\"exchangeGroup\\":\\"NO_EXCHANGE\\",\\"sellerGroup\\":[\\"ANY_SELLER\\"],\\"damageGroup\\":\\"NOT_BEATEN\\",\\"ownersCountGroup\\":\\"ANY_COUNT\\",\\"owningTimeGroup\\":\\"ANY_TIME\\",\\"customsStateGroup\\":\\"CLEARED\\"}}"
              }]
            }
          }"""
        )

        val res = historyClient.getSearchHistory(user).futureValue
        res.size shouldBe 1
        res.head.filter.getMarkModelNameplate(0) shouldBe "BMW"
      }
    }
  }
}
