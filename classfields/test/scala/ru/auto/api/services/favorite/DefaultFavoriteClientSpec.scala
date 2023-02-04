package ru.auto.api.services.favorite

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes._
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.favorite.FavoritesHelper
import ru.auto.api.model.CategorySelector.{All, Cars, Moto}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.favorite._
import ru.auto.api.model.{CategorySelector, ModelGenerators, OfferID, RequestParams}
import ru.auto.api.services.personal.PersonalFavoritesClient
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.FavoriteUtils._
import ru.auto.api.util.StringUtils._
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._

/**
  * Created by ndmelentev on 20.04.17.
  */
class DefaultFavoriteClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with MockitoSupport {

  import DefaultFavoriteClientSpec._

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)
  val defaultsMapper = new DefaultsMapper(featureManager)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)
  val savedSearchFactoryProvider: SavedSearchFactoryProvider = new SavedSearchFactoryProvider(searchMappings)

  val favoriteClient = new DefaultFavoriteClient(http, savedSearchFactoryProvider)
  val personalFavoritesClient = new PersonalFavoritesClient(http)
  val favoritesHelper = new FavoritesHelper(personalFavoritesClient)
  private val categoryCars = Cars
  private val favCarsDomain = getFavDomain(categoryCars)
  private val noteCarsDomain = getNoteDomain(categoryCars)

  private val categoryAll = All
  private val favAllDomain = getFavDomain(categoryAll)
  private val noteAllDomain = getNoteDomain(categoryAll)

  private val payload = "hi"

  implicit val request: Request = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1", deviceUid = Some(Gen.identifier.next)))
    r.setTrace(trace)
    r.setUser(ModelGenerators.PrivateUserRefGen.next)
    r.setApplication(Application.iosApp)
    r
  }

  "FavoriteClient" should {
    "get notes and favorites of user" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Acar_note%2Ccar_sale/uid%3A$uid/")

        http.respondWithJson(
          OK,
          s"""{
            "service": "autoru",
            "version": "2.0",
            "user": "uid:123",
            "entities": {
              "car_sale": [
                {
                  "entity_id": "9-24b2acf0",
                  "create_timestamp": 1492699080090,
                  "update_timestamp": 1492699080090
                },
                {
                  "entity_id": "9-25b2acf0",
                  "create_timestamp": 1492699080090,
                  "update_timestamp": 1492699080090
                }
              ],
              "car_note": [
                {
                  "entity_id": "9-24b2acf0",
                  "create_timestamp": 1494858716013,
                  "update_timestamp": 1494859050006,
                  "payload": "qwe"
                },
                {
                  "entity_id": "2",
                  "create_timestamp": 1494861250839,
                  "update_timestamp": 1494861250839,
                  "payload": "rty"
                }
              ]
            }
          }"""
        )

        val result = favoritesHelper.getNotesAndFavorites(user, categoryCars).futureValue.map(o => o.getId -> o).toMap
        result("9-24b2acf0").getIsFavorite shouldBe true
        result("9-24b2acf0").getNote shouldBe "qwe"
        result("9-25b2acf0").getIsFavorite shouldBe true
        result("2").getNote shouldBe "rty"
      }
    }

    "get notes and favorites for all categories" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid

        //noinspection ScalaStyle
        http.expectUrl(
          GET,
          url"/favorites/2.0/autoru%3Amoto_sale%2Ccommercial_sale%2Ccommercial_note%2Ccar_sale%2Cmoto_note%2Ccar_note/uid%3A$uid/"
        )

        http.respondWithJson(
          OK,
          s"""{
            "service": "autoru",
            "version": "2.0",
            "user": "uid:123",
            "entities": {
              "car_sale": [
                {
                  "entity_id": "9-24b2acf0",
                  "create_timestamp": 1492699080090,
                  "update_timestamp": 1492699080090
                }
              ],
              "moto_sale": [
                {
                  "entity_id": "9-25b2acf0",
                  "create_timestamp": 1492699080090,
                  "update_timestamp": 1492699080090
                }
              ],
              "car_note": [
                {
                  "entity_id": "9-24b2acf0",
                  "create_timestamp": 1494858716013,
                  "update_timestamp": 1494859050006,
                  "payload": "qwe"
                },
                {
                  "entity_id": "2",
                  "create_timestamp": 1494861250839,
                  "update_timestamp": 1494861250839,
                  "payload": "rty"
                }
              ]
            }
          }"""
        )

        val result = favoritesHelper.getNotesAndFavorites(user, categoryAll).futureValue.map(o => o.getId -> o).toMap
        result("9-24b2acf0").getIsFavorite shouldBe true
        result("9-24b2acf0").getNote shouldBe "qwe"
        result("9-25b2acf0").getIsFavorite shouldBe true
        result("9-25b2acf0").category shouldBe Moto
        result("2").getNote shouldBe "rty"
      }
    }

    "add favorite offer of user" in {
      forAll(PrivateUserRefGen, OfferIDGen) { (user, offerId) =>
        val uid = user.uid

        http.expectUrl(POST, url"/favorites/2.0/autoru%3Acar_sale/uid%3A$uid/$offerId")
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        favoriteClient.addFavorite(favCarsDomain, user, offerId).futureValue
      }
    }

    "add note to user" in {
      forAll(PrivateUserRefGen, OfferIDGen) { (user, offerId) =>
        val uid = user.uid

        http.expectUrl(POST, url"/favorites/2.0/autoru%3Acar_note/uid%3A$uid/$offerId")
        http.expectJson(payload)
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        favoriteClient.addNote(noteCarsDomain, user, offerId, payload = payload).futureValue
      }
    }

    "upsert favorite offer of user" in {
      forAll(PrivateUserRefGen, OfferIDGen) { (user, offerId) =>
        val uid = user.uid

        http.expectUrl(PUT, url"/favorites/2.0/autoru%3Acar_sale/uid%3A$uid/$offerId")
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        favoriteClient.upsertFavorite(favCarsDomain, user, offerId).futureValue
      }
    }

    "upsert favorite offer of user with note" in {
      forAll(PrivateUserRefGen, OfferIDGen) { (user, offerId) =>
        val uid = user.uid

        http.expectUrl(PUT, url"/favorites/2.0/autoru%3Acar_note/uid%3A$uid/$offerId")
        http.expectJson(payload)
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        favoriteClient.upsertNote(noteCarsDomain, user, offerId, payload).futureValue
      }
    }

    "delete favorite offer of user" in {
      forAll(PrivateUserRefGen, OfferIDGen) { (user, offerId) =>
        val uid = user.uid

        http.expectUrl(DELETE, url"/favorites/2.0/autoru%3Acar_sale/uid%3A$uid/$offerId")
        http.respondWithJson(OK, s"""{"status": "OK"""")

        favoriteClient.deleteFavorite(favCarsDomain, user, Seq(offerId)).futureValue
      }
    }

    "delete favorite offers batch of user" in {
      forAll(PrivateUserListingResponseGen) {
        case (user, listing) =>
          val uid = user.uid
          val ids = listing.getOffersList.asScala.toSeq.map(_.id)
          val idsStrings = ids.mkString(",")

          http.expectUrl(DELETE, url"/favorites/2.0/autoru%3Acar_sale/uid%3A$uid/$idsStrings")
          http.respondWithJson(OK, s"""{"status": "OK"""")

          favoriteClient.deleteFavorite(favCarsDomain, user, ids).futureValue
      }
    }

    "delete note of user" in {
      forAll(PrivateUserRefGen, OfferIDGen) { (user, offerId) =>
        val uid = user.uid

        http.expectUrl(DELETE, url"/favorites/2.0/autoru%3Acar_note/uid%3A$uid/$offerId")
        http.respondWithJson(OK, s"""{"status": "OK"""")

        favoriteClient.deleteNote(noteCarsDomain, user, Seq(offerId)).futureValue
      }
    }

    "get notes offers of user using their ids" in {
      forAll(PrivateUserRefGen, OfferIDGen, OfferIDGen) { (user, offerID1, offerID2) =>
        val uid = user.uid
        val offersIds = List(offerID1, offerID2).map(_.toString).distinct.mkString(url",")
        http.expectUrl(GET, url"/favorites/2.0/autoru%3Acar_note%2Ccar_sale/uid%3A$uid/$offersIds")
        http.respondWithJson(
          OK,
          s"""{
            "service": "autoru",
            "version": "2.0",
            "user": "uid:123",
            "entities": {
              "car_note": [
                {
                  "entity_id": "1",
                  "create_timestamp": 1494858716013,
                  "update_timestamp": 1494859050006,
                  "payload": "qwe"
                },
                {
                  "entity_id": "2",
                  "create_timestamp": 1494861250839,
                  "update_timestamp": 1494861250839,
                  "payload": "rty"
                }
              ]
            }
          }"""
        )

        val idsToNotes = favoritesHelper
          .getNotesAndFavoritesByOfferIds(user, categoryCars, Seq(offerID1, offerID2))
          .futureValue
          .notesMap
        idsToNotes(OfferID.parse("1")) shouldBe "qwe"
        idsToNotes(OfferID.parse("2")) shouldBe "rty"
      }
    }

    "get favorites of user using their ids" in {
      forAll(PrivateUserRefGen, OfferIDGen, OfferIDGen) { (user, offerID1, offerID2) =>
        val uid = user.uid

        val offersIds = List(offerID1, offerID2).map(_.toString).distinct.mkString(url",")
        http.expectUrl(GET, url"/favorites/2.0/autoru%3Acar_note%2Ccar_sale/uid%3A$uid/$offersIds")

        http.respondWithJson(
          OK,
          s"""{"service": "autoru",
                            "version": "2.0",
                            "user": "uid:$uid",
                            "entities": {
                            "car_sale": [{"entity_id": "$offerID1",
                            "create_timestamp": 1492699080090,
                            "update_timestamp": 1492699080090},
                            {"entity_id": "$offerID2",
                            "create_timestamp": 1492699080090,
                            "update_timestamp": 1492699080090}]}}"""
        )

        val ids = favoritesHelper
          .getNotesAndFavoritesByOfferIds(user, categoryCars, Seq(offerID1, offerID2))
          .futureValue
          .favoriteIds
        ids.toSet shouldEqual Set(offerID1, offerID2)
      }
    }

    "check favorite offers of user using their ids" in {
      forAll(PrivateUserRefGen, OfferIDGen, OfferIDGen) { (user, offerID1, offerID2) =>
        val uid = user.uid

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Acar_sale/uid%3A$uid/check/$offerID1%2C$offerID2")

        http.respondWithJson(OK, s"""{
                "$offerID1": false,
                "$offerID2": true
              }""")

        val favorites =
          favoriteClient.checkIfOffersAreInFavorite(favCarsDomain, user, Seq(offerID1, offerID2)).futureValue

        favorites shouldBe Map(offerID1 -> false, offerID2 -> true)
      }
    }

    "check notes of user using their ids" in {
      forAll(PrivateUserRefGen, OfferIDGen, OfferIDGen) { (user, offerID1, offerID2) =>
        val uid = user.uid

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Acar_note/uid%3A$uid/check/$offerID1%2C$offerID2")

        http.respondWithJson(OK, s"""{
                "$offerID1": false,
                "$offerID2": true
              }""")

        val favorites =
          favoriteClient.checkIfUserHasNotes(noteCarsDomain, user, Seq(offerID1, offerID2)).futureValue

        favorites shouldBe Map(offerID1 -> false, offerID2 -> true)
      }
    }

    "count favorite offers of user" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Acar_sale/uid%3A$uid/count")

        http.respondWithJson(
          OK,
          s"""{"service": "autoru", "version": "2.0", "user": "uid:$uid", "entities": {"car_sale": 777}}"""
        )

        val res = favoriteClient.countFavorite(favCarsDomain, user).futureValue
        res shouldBe 777
      }
    }

    "count notes of user" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Acar_note/uid%3A$uid/count")

        http.respondWithJson(
          OK,
          s"""{"service": "autoru", "version": "2.0", "user": "uid:$uid", "entities": {"car_note": 777}}"""
        )

        val res = favoriteClient.countNotes(noteCarsDomain, user).futureValue
        res shouldBe 777
      }
    }

    "count favorite offers of user when there are none of them" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Acar_sale/uid%3A$uid/count")

        http.respondWithJson(OK, s"""{"service": "autoru", "version": "2.0", "user": "uid:$uid", "entities": {}}""")

        val res = favoriteClient.countFavorite(favCarsDomain, user).futureValue
        res shouldBe 0
      }
    }

    "count notes of user when there are none of them" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Acar_note/uid%3A$uid/count")

        http.respondWithJson(OK, s"""{"service": "autoru", "version": "2.0", "user": "uid:$uid", "entities": {}}""")

        val res = favoriteClient.countNotes(noteCarsDomain, user).futureValue
        res shouldBe 0
      }
    }

    "copy favorite offer of user to another user" in {
      forAll(PrivateUserRefGen, PrivateUserRefGen) { (user1, user2) =>
        val uid1 = user1.uid
        val uid2 = user2.uid

        http.expectUrl(
          POST,
          url"/favorites/2.0/autoru%3Acar_sale%2Cmoto_sale%2Ccommercial_sale/uid%3A$uid1/copy/uid%3A$uid2"
        )
        http.respondWithJson(OK, s"""{"status": "OK"""")

        favoriteClient.copyFavorite(user1, user2).futureValue
      }
    }

    "copy notes of user to another user" in {
      forAll(PrivateUserRefGen, PrivateUserRefGen) { (user1, user2) =>
        val uid1 = user1.uid
        val uid2 = user2.uid

        http.expectUrl(
          POST,
          url"/favorites/2.0/autoru%3Acar_note%2Cmoto_note%2Ccommercial_note/uid%3A$uid1/copy/uid%3A$uid2"
        )
        http.respondWithJson(OK, s"""{"status": "OK"""")

        favoriteClient.copyNotes(user1, user2).futureValue
      }
    }

    "move favorite offer of user to another user" in {
      forAll(PrivateUserRefGen, PrivateUserRefGen) { (user1, user2) =>
        val uid1 = user1.uid
        val uid2 = user2.uid

        http.expectUrl(
          POST,
          url"/favorites/2.0/autoru%3Acar_sale%2Cmoto_sale%2Ccommercial_sale/uid%3A$uid1/move/uid%3A$uid2"
        )
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        favoriteClient.moveFavorite(user1, user2).futureValue
      }
    }

    "move notes of user to another user" in {
      forAll(PrivateUserRefGen, PrivateUserRefGen) { (user1, user2) =>
        val uid1 = user1.uid
        val uid2 = user2.uid

        http.expectUrl(
          POST,
          url"/favorites/2.0/autoru%3Acar_note%2Cmoto_note%2Ccommercial_note/uid%3A$uid1/move/uid%3A$uid2"
        )
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        favoriteClient.moveNotes(user1, user2).futureValue
      }
    }

    "add favorite subscription to user" in {
      forAll(PrivateUserRefGen, personalSavedSearchGen(OfferSearchesDomain)) { (user, personalSS) =>
        val uid = user.uid
        val category = Cars

        http.expectUrl(POST, url"/favorites/2.0/autoru%3Asearches/uid%3A$uid/${personalSS.id}")
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        favoriteClient.addSavedSearch(user, personalSS).futureValue
      }
    }

    "update favorite subscription to user" in {
      forAll(PrivateUserRefGen, personalSavedSearchGen(OfferSearchesDomain)) { (user, personalSS) =>
        val uid = user.uid

        http.expectUrl(PUT, url"/favorites/2.0/autoru%3Asearches/uid%3A$uid/${personalSS.id}")
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        favoriteClient.upsertSavedSearch(user, personalSS.id, personalSS).futureValue
      }
    }

    "delete saved search of  user" in {
      forAll(PrivateUserRefGen, personalSavedSearchGen(OfferSearchesDomain)) { (user, personalSS) =>
        val uid = user.uid

        http.expectUrl(DELETE, url"/favorites/2.0/autoru%3Asearches/uid%3A$uid/${personalSS.id}")
        http.respondWithJson(OK, s"""{"status": "OK"}""")

        favoriteClient.deleteSavedSearch(OfferSearchesDomain, user, personalSS.id).futureValue
      }
    }

    "get saved search of user" in {
      forAll(PrivateUserRefGen) { (user) =>
        val uid = user.uid
        val category = Cars

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Asearches/uid%3A$uid")
        http.respondWithJson(
          OK,
          s"""{ "service": "autoru",
                "version": "2.0",
                "user": "uid:$uid",
                "entities": {
                  "searches": [
                    {
                      "entity_id": "93458ae27cf28d5334c508c33b6302d617ec78c3",
                      "create_timestamp": 1495632254816,
                      "update_timestamp": 1495632254816,
                      "payload": "{\\"http_query\\":\\"custom_state_key=CLEARED&image=true&mark-model-nameplate=HONDA%23CB_400&moto_category=motorcycle&rid=1&state=USED\\",\\"title\\":\\"Мотоциклы Honda CB 400 Super Four в Москве и Московской области\\",\\"qualifier\\":\\"moto\\"}"
                    },
                    {
                      "entity_id": "bf05c7d484da562381d0f28546e38969edc692c1",
                      "create_timestamp": 1495804619411,
                      "update_timestamp": 1495804619411,
                      "payload": "{\\"http_query\\":\\"custom_state_key=CLEARED&image=true&mark-model-nameplate=KAWASAKI%23VERSYS_650&moto_category=motorcycle&rid=1&state=NEW&state=USED\\",\\"title\\":\\"Мотоциклы Kawasaki Versys 650 в Москве и Московской области\\",\\"qualifier\\":\\"moto\\"}"
                    }
                    ]
                  }
              }"""
        )

        val result = favoriteClient.getUserSavedSearches(user, OfferSearchesDomain).futureValue
        result.length shouldBe 2
        result.head.category shouldBe CategorySelector.Moto
      }
    }

    "return None if user doesn't have subscription" in {
      forAll(PrivateUserRefGen, personalSavedSearchGen(OfferSearchesDomain)) { (user, subscription) =>
        val uid = user.uid

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Asearches/uid%3A$uid/${subscription.id}")
        http.respondWithJson(
          OK,
          s"""{ "service": "autoru",
                "version": "2.0",
                "user": "uid:$uid",
                "entities": {
                  "searches": []
                  }
              }"""
        )

        val res = favoriteClient.getSavedSearch(OfferSearchesDomain, user, subscription.id).futureValue
        res shouldBe None
      }
    }

    "get saved search of user by ID" in {
      forAll(PrivateUserRefGen, personalSavedSearchGen(OfferSearchesDomain)) { (user, subscription) =>
        val uid = user.uid

        http.expectUrl(GET, url"/favorites/2.0/autoru%3Asearches/uid%3A$uid/${subscription.id}")
        http.respondWithJson(
          OK,
          s"""{ "service": "autoru",
                "version": "2.0",
                "user": "uid:$uid",
                "entities": {
                  "searches": [
                    {
                      "entity_id": "${subscription.id}",
                      "create_timestamp": 1495632254816,
                      "update_timestamp": 1495632254816,
                      "payload": "{\\"http_query\\":\\"custom_state_key=CLEARED&image=true&mark-model-nameplate=HONDA%23CB_400&moto_category=motorcycle&rid=1&state=USED\\",\\"title\\":\\"Мотоциклы Honda CB 400 Super Four в Москве и Московской области\\",\\"qualifier\\":\\"moto\\"}"
                    }
                    ]
                  }
              }"""
        )

        val result = favoriteClient.getSavedSearch(OfferSearchesDomain, user, subscription.id).futureValue.get
        result.id shouldBe subscription.id
      }
    }

  }
}

object DefaultFavoriteClientSpec {

  implicit class RichOfferSeq(offers: Seq[Offer]) {

    def favoriteIds: Seq[OfferID] = {
      offers.collect {
        case offer if offer.getIsFavorite => offer.id
      }
    }

    def notesMap: Map[OfferID, String] = {
      offers.collect {
        case offer if offer.getNote.nonEmpty => offer.id -> offer.getNote
      }.toMap
    }
  }
}
