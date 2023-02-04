package ru.auto.api.services.favorite

import org.scalatest.Ignore
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.UserAlreadyHasNoteAboutThisOffer
import ru.auto.api.features.FeatureManager
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.managers.favorite.FavoritesHelper
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model._
import ru.auto.api.model.favorite.{OfferSearchesDomain, SavedSearchFactoryProvider}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.personal.PersonalFavoritesClient
import ru.auto.api.util.FavoriteUtils._
import ru.auto.api.util.search.SearchMappings
import ru.auto.api.util.search.mappers.DefaultsMapper
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Created by ndmelentev on 11.04.17.
  */
@Ignore
class DefaultFavoriteClientIntTest extends HttpClientSuite with MockitoSupport {

  import DefaultFavoriteClientSpec._

  override protected def config: HttpClientConfig =
    HttpClientConfig.apply("http", "personal-api-01-sas.test.vertis.yandex.net", 36900)

  val featureManager: FeatureManager = mock[FeatureManager]
  val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(feature.value).thenReturn(false)

  val defaultsMapper = new DefaultsMapper(featureManager)
  val searchMappings: SearchMappings = new SearchMappings(defaultsMapper, featureManager)
  val savedSearchFactoryProvider: SavedSearchFactoryProvider = new SavedSearchFactoryProvider(searchMappings)

  val favoriteClient = new DefaultFavoriteClient(http, savedSearchFactoryProvider)
  val personalFavoritesClient = new PersonalFavoritesClient(http)
  val favoriteHelper = new FavoritesHelper(personalFavoritesClient)
  private val category = Cars
  private val favDomain = getFavDomain(category)
  private val noteDomain = getNoteDomain(category)
  private val payload = "hi"

  implicit private val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1", sessionId = Some(SessionID("test_session"))))
    r.setUser(UserRef.anon("42"))
    r.setApplication(Application.desktop)
    r
  }

  test("get favorite offers of registered user") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    favoriteClient.upsertFavorite(favDomain, user, id).futureValue

    favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds should contain(id)

    favoriteClient.deleteFavorite(favDomain, user, Seq(id))
  }
  test("get favorite offers of anon user") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.AnonymousUserRefGen.next

    favoriteClient.upsertFavorite(favDomain, user, id).futureValue

    favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds should contain(id)

    favoriteClient.deleteFavorite(favDomain, user, Seq(id))
  }

  test("get favorite offers of registered user when there are none") {
    val user = ModelGenerators.PrivateUserRefGen.next

    val favorites = favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds
    favoriteClient.deleteFavorite(favDomain, user, favorites).futureValue

    favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds.size shouldBe 0
  }

  test("get favorite offers of anon user when there are none") {
    val user = ModelGenerators.AnonymousUserRefGen.next

    val favorites = favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds
    favoriteClient.deleteFavorite(favDomain, user, favorites).futureValue

    favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds.size shouldBe 0
  }

  test("get registered user notes") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    favoriteClient.upsertNote(noteDomain, user, id, payload).futureValue

    favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap should contain((id -> payload))

    favoriteClient.deleteNote(noteDomain, user, Seq(id))
  }

  test("get anon user notes") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.AnonymousUserRefGen.next

    favoriteClient.upsertNote(noteDomain, user, id, payload).futureValue

    favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap should contain((id -> payload))

    favoriteClient.deleteNote(noteDomain, user, Seq(id))
  }

  test("get registered user notes when there are none") {
    val user = ModelGenerators.PrivateUserRefGen.next

    val notes = favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap
    favoriteClient.deleteNote(noteDomain, user, notes.keys.toSeq).futureValue

    favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap.size shouldBe 0
  }

  test("get anon user notes when there are none") {
    val user = ModelGenerators.AnonymousUserRefGen.next

    val notes = favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap
    favoriteClient.deleteNote(noteDomain, user, notes.keys.toSeq).futureValue

    favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap.size shouldBe 0
  }

  test("add note to registered user") {
    pending
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    // remove favorite offer from user
    favoriteClient.deleteNote(noteDomain, user, Seq(id)).futureValue

    // add once -> no throwable
    favoriteClient.addNote(noteDomain, user, id, payload).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap should contain(id -> payload)

    // add twice -> throws exception
    favoriteClient
      .addNote(noteDomain, user, id, payload)
      .failed
      .futureValue shouldBe an[UserAlreadyHasNoteAboutThisOffer]

    // remove to save space
    favoriteClient.deleteNote(noteDomain, user, Seq(id)).futureValue
  }

  test("add note to anon user") {
    pending
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.AnonymousUserRefGen.next

    // remove favorite offer from user
    favoriteClient.deleteNote(noteDomain, user, Seq(id)).futureValue

    // add once -> no throwable
    favoriteClient.addNote(noteDomain, user, id, payload).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap should contain(id -> payload)

    // add twice -> throws exception
    favoriteClient
      .addNote(noteDomain, user, id, payload)
      .failed
      .futureValue shouldBe an[UserAlreadyHasNoteAboutThisOffer]

    // remove to save space
    favoriteClient.deleteNote(noteDomain, user, Seq(id)).futureValue
  }

  test("upsert favorite offer to registered user") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    favoriteClient.deleteFavorite(noteDomain, user, Seq(id)).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds shouldNot contain(id)

    // add once -> no exception
    favoriteClient.upsertFavorite(favDomain, user, id).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds should contain(id)

    // add twice -> no exception
    favoriteClient.upsertFavorite(favDomain, user, id).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds should contain(id)

    // remove to save space
    favoriteClient.deleteFavorite(favDomain, user, Seq(id)).futureValue
  }

  test("upsert favorite offer to anon user") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.AnonymousUserRefGen.next

    favoriteClient.deleteFavorite(favDomain, user, Seq(id)).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds shouldNot contain(id)

    // add once -> no exception
    favoriteClient.upsertFavorite(favDomain, user, id).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds should contain(id)

    // add twice -> no exception
    favoriteClient.upsertFavorite(favDomain, user, id).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds should contain(id)

    // remove to save space
    favoriteClient.deleteFavorite(favDomain, user, Seq(id)).futureValue
  }

  test("upsert note to registered user") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    favoriteClient.deleteNote(noteDomain, user, Seq(id)).futureValue
    (favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap shouldNot contain).key(id)

    // add once -> no exception
    favoriteClient.upsertNote(noteDomain, user, id, payload).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap should contain(id -> payload)

    // add twice -> no exception
    favoriteClient.upsertNote(noteDomain, user, id, payload).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap should contain(id -> payload)

    // remove to save space
    favoriteClient.deleteNote(noteDomain, user, Seq(id)).futureValue
  }

  test("upsert note to anon user") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.AnonymousUserRefGen.next

    favoriteClient.deleteNote(noteDomain, user, Seq(id)).futureValue
    (favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap shouldNot contain).key(id)

    // add once -> no exception
    favoriteClient.upsertNote(noteDomain, user, id, payload).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap should contain(id -> payload)

    // add twice -> no exception
    favoriteClient.upsertNote(noteDomain, user, id, payload).futureValue
    favoriteHelper.getNotesAndFavorites(user, category).futureValue.notesMap should contain(id -> payload)

    // remove to save space
    favoriteClient.deleteNote(noteDomain, user, Seq(id)).futureValue
  }

  test("delete favorite offer from registered user") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    favoriteClient.upsertFavorite(favDomain, user, id).futureValue
    favoriteClient.deleteFavorite(favDomain, user, Seq(id)).futureValue
    favoriteHelper.getNotesAndFavoritesByOfferIds(user, category, Seq(id)).futureValue.favoriteIds shouldNot contain(id)
  }

  test("delete favorite offer from anon user") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.AnonymousUserRefGen.next

    favoriteClient.upsertFavorite(favDomain, user, id).futureValue
    favoriteClient.deleteFavorite(favDomain, user, Seq(id)).futureValue
    favoriteHelper.getNotesAndFavoritesByOfferIds(user, category, Seq(id)).futureValue.favoriteIds shouldNot contain(id)
  }

  test("delete note from registered user") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.PrivateUserRefGen.next

    favoriteClient.upsertNote(favDomain, user, id, "test note").futureValue
    favoriteClient.deleteNote(favDomain, user, Seq(id)).futureValue
    favoriteHelper.getNotesAndFavoritesByOfferIds(user, category, Seq(id)).futureValue.notesMap shouldNot contain(id)
  }

  test("delete note from anon user") {
    val id = ModelGenerators.OfferIDGen.next
    val user = ModelGenerators.AnonymousUserRefGen.next

    favoriteClient.upsertNote(noteDomain, user, id, "test note").futureValue
    favoriteClient.deleteNote(noteDomain, user, Seq(id)).futureValue
    favoriteHelper.getNotesAndFavoritesByOfferIds(user, category, Seq(id)).futureValue.notesMap shouldNot contain(id)
  }

  test("get favorite offers of registered user by ids") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val id1 = OfferID.parse("1")
    val id2 = OfferID.parse("2")
    val id3 = OfferID.parse("3")

    favoriteClient.deleteFavorite(favDomain, user, Seq(id1)).futureValue
    favoriteClient.upsertFavorite(favDomain, user, id2).futureValue
    favoriteClient.upsertFavorite(favDomain, user, id3).futureValue
    favoriteHelper
      .getNotesAndFavoritesByOfferIds(user, category, Seq(id1, id2, id3))
      .futureValue
      .favoriteIds shouldBe Seq(id3, id2)
    favoriteClient.deleteFavorite(favDomain, user, Seq(id2, id3)).futureValue
  }

  test("get favorite offers of anon user by ids") {
    val user = ModelGenerators.AnonymousUserRefGen.next
    val id1 = OfferID.parse("1")
    val id2 = OfferID.parse("2")
    val id3 = OfferID.parse("3")

    favoriteClient.deleteFavorite(favDomain, user, Seq(id1)).futureValue
    favoriteClient.upsertFavorite(favDomain, user, id2).futureValue
    favoriteClient.upsertFavorite(favDomain, user, id3).futureValue

    favoriteHelper
      .getNotesAndFavoritesByOfferIds(user, category, Seq(id1, id2, id3))
      .futureValue
      .favoriteIds shouldBe Seq(id3, id2)
    favoriteClient.deleteFavorite(favDomain, user, Seq(id2, id3)).futureValue
  }

  // todo test for anon users

  test("get notes of user with notes by ids") {
    val user = UserRef.user(20871601L)
    val id1 = OfferID.parse("1")
    val id2 = OfferID.parse("2")
    val id3 = OfferID.parse("3")
    val payload2 = "hi2"
    val payload3 = "hi3"

    favoriteClient.deleteNote(noteDomain, user, Seq(id1)).futureValue
    favoriteClient.upsertNote(noteDomain, user, id2, payload2).futureValue
    favoriteClient.upsertNote(noteDomain, user, id3, payload3).futureValue

    favoriteHelper.getNotesAndFavoritesByOfferIds(user, category, Seq(id1, id2, id3)).futureValue.notesMap shouldBe Map(
      (id2 -> payload2),
      (id3 -> payload3)
    )
    favoriteClient.deleteNote(noteDomain, user, Seq(id2, id3)).futureValue
  }

  test("is favorite") {
    val user = UserRef.user(20871605L)
    val id1 = OfferID.parse("1")
    val id2 = OfferID.parse("777")
    val id3 = OfferID.parse("3")

    favoriteClient.deleteFavorite(favDomain, user, Seq(id1)).futureValue
    favoriteClient.upsertFavorite(favDomain, user, id2).futureValue
    favoriteClient.upsertFavorite(favDomain, user, id3).futureValue

    favoriteClient.checkIfOffersAreInFavorite(favDomain, user, Seq(id1, id2, id3)).futureValue shouldBe Map(
      id1 -> false,
      id2 -> true,
      id3 -> true
    )
  }

  test("get favorite for new user") {
    val user = UserRef.anon("ihwoighio23u5o34ht09rh1fneowc")
    val id1 = OfferID.parse("1")

    favoriteHelper.getNotesAndFavoritesByOfferIds(user, category, Seq(id1)).futureValue.favoriteIds
  }

  test("get notes for new user") {
    val user = UserRef.anon("ihwoighio23u5o34ht09rh1fneowc")
    val id1 = OfferID.parse("1")

    favoriteHelper.getNotesAndFavoritesByOfferIds(user, category, Seq(id1)).futureValue.notesMap
  }

  test("has notes") {
    val user = UserRef.user(20871605L)
    val id1 = OfferID.parse("1")
    val id2 = OfferID.parse("777")
    val id3 = OfferID.parse("3")

    favoriteClient.deleteNote(noteDomain, user, Seq(id1)).futureValue
    favoriteClient.upsertNote(noteDomain, user, id2, "id2").futureValue
    favoriteClient.upsertNote(noteDomain, user, id3, "id3").futureValue

    favoriteClient.checkIfUserHasNotes(noteDomain, user, Seq(id1, id2, id3)).futureValue shouldBe Map(
      id1 -> false,
      id2 -> true,
      id3 -> true
    )
  }

  test("count fav offers of user") {
    val user = UserRef.user(20871607L)

    //upsert random data
    favoriteClient.upsertFavorite(favDomain, user, ModelGenerators.OfferIDGen.next).futureValue
    favoriteClient.upsertFavorite(favDomain, user, ModelGenerators.OfferIDGen.next).futureValue

    val favorites = favoriteHelper.getNotesAndFavorites(user, category).futureValue.favoriteIds

    favoriteClient.countFavorite(favDomain, user).futureValue shouldBe favorites.size
  }

  test("count notes of registered user") {
    val user = ModelGenerators.PrivateUserRefGen.next

    //upsert random data
    favoriteClient.upsertNote(noteDomain, user, ModelGenerators.OfferIDGen.next, "test note 1").futureValue
    favoriteClient.upsertNote(noteDomain, user, ModelGenerators.OfferIDGen.next, "test note 2").futureValue

    val notes0 = favoriteHelper.getNotesAndFavorites(user, category).futureValue
    val notes = notes0.notesMap

    favoriteClient.countNotes(noteDomain, user).futureValue shouldBe notes.size
  }

  test("count notes of anon user") {
    val user = ModelGenerators.AnonymousUserRefGen.next

    //upsert random data
    favoriteClient.upsertNote(noteDomain, user, ModelGenerators.OfferIDGen.next, "test note 1").futureValue
    favoriteClient.upsertNote(noteDomain, user, ModelGenerators.OfferIDGen.next, "test note 2").futureValue

    val notes0 = favoriteHelper.getNotesAndFavorites(user, category).futureValue
    val notes = notes0.notesMap

    favoriteClient.countNotes(noteDomain, user).futureValue shouldBe notes.size
  }

  test("copy favorite offers of user to another user") {
    val user1 = ModelGenerators.PersonalUserRefGen.next
    val user2 = ModelGenerators.PersonalUserRefGen.next

    //upsert random data
    favoriteClient.upsertFavorite(favDomain, user1, ModelGenerators.OfferIDGen.next).futureValue
    favoriteClient.upsertFavorite(favDomain, user1, ModelGenerators.OfferIDGen.next).futureValue

    favoriteClient.countFavorite(favDomain, user1).futureValue shouldBe 2
    favoriteClient.countFavorite(favDomain, user2).futureValue shouldBe 0

    favoriteClient.copyFavorite(user1, user2).futureValue

    favoriteClient.countFavorite(favDomain, user1).futureValue shouldBe 2
    favoriteClient.countFavorite(favDomain, user2).futureValue shouldBe 2
  }

  test("copy notes of user to another user") {
    val user1 = ModelGenerators.PersonalUserRefGen.next
    val user2 = ModelGenerators.PersonalUserRefGen.next

    favoriteClient.countNotes(noteDomain, user2).futureValue shouldBe 0

    //upsert random data
    favoriteClient.upsertNote(noteDomain, user1, ModelGenerators.OfferIDGen.next, "123").futureValue
    favoriteClient.upsertNote(noteDomain, user1, ModelGenerators.OfferIDGen.next, "321").futureValue

    favoriteClient.copyNotes(user1, user2).futureValue

    favoriteClient.countNotes(noteDomain, user1).futureValue shouldBe 2
    favoriteClient.countNotes(noteDomain, user2).futureValue shouldBe 2
  }

  test("move favorite offers of user to another user") {
    val user1 = ModelGenerators.PersonalUserRefGen.next
    val user2 = ModelGenerators.PersonalUserRefGen.next

    //upsert random data
    favoriteClient.upsertFavorite(favDomain, user1, ModelGenerators.OfferIDGen.next).futureValue
    favoriteClient.upsertFavorite(favDomain, user1, ModelGenerators.OfferIDGen.next).futureValue

    favoriteClient.countFavorite(favDomain, user2).futureValue shouldBe 0

    favoriteClient.moveFavorite(user1, user2).futureValue

    favoriteClient.countFavorite(favDomain, user1).futureValue shouldBe 0
    favoriteClient.countFavorite(favDomain, user2).futureValue shouldBe 2
  }

  test("move notes of user to another user") {
    val user1 = ModelGenerators.PersonalUserRefGen.next
    val user2 = ModelGenerators.PersonalUserRefGen.next

    favoriteClient.countNotes(noteDomain, user2).futureValue shouldBe 0

    //upsert random data
    favoriteClient.upsertNote(noteDomain, user1, ModelGenerators.OfferIDGen.next, "123").futureValue
    favoriteClient.upsertNote(noteDomain, user1, ModelGenerators.OfferIDGen.next, "321").futureValue

    favoriteClient.moveNotes(user1, user2).futureValue

    favoriteClient.countNotes(noteDomain, user1).futureValue shouldBe 0
    favoriteClient.countNotes(noteDomain, user2).futureValue shouldBe 2
  }

  test("add offer saved search") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val savedSearch = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next

    favoriteClient.addSavedSearch(user, savedSearch).futureValue

    favoriteClient.getSavedSearch(OfferSearchesDomain, user, savedSearch.id).futureValue.get.id shouldBe savedSearch.id

    favoriteClient.deleteSavedSearch(OfferSearchesDomain, user, savedSearch.id).futureValue
  }

  test("get user's offer saved searches") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val savedSearch1 = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next
    val savedSearch2 = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next

    favoriteClient.addSavedSearch(user, savedSearch1).futureValue
    favoriteClient.addSavedSearch(user, savedSearch2).futureValue

    val searches = favoriteClient.getUserSavedSearches(user, OfferSearchesDomain).futureValue
    searches.size shouldBe 2
    searches.map(_.id) should contain(savedSearch1.id)
    searches.map(_.id) should contain(savedSearch2.id)

    favoriteClient.deleteSavedSearch(OfferSearchesDomain, user, savedSearch1.id).futureValue
    favoriteClient.deleteSavedSearch(OfferSearchesDomain, user, savedSearch2.id).futureValue
  }

  test("move offer saved searches of user to another user") {
    val user1 = ModelGenerators.PersonalUserRefGen.next
    val user2 = ModelGenerators.PersonalUserRefGen.next
    val savedSearch = ModelGenerators.personalSavedSearchGen(OfferSearchesDomain).next

    favoriteClient.addSavedSearch(user1, savedSearch).futureValue

    favoriteClient.getUserSavedSearches(user1, OfferSearchesDomain).futureValue.size shouldBe 1

    favoriteClient.moveSavedSearches(OfferSearchesDomain, user1, user2).futureValue

    favoriteClient.getUserSavedSearches(user1, OfferSearchesDomain).futureValue.size shouldBe 0
    favoriteClient.getUserSavedSearches(user2, OfferSearchesDomain).futureValue.size shouldBe 1
  }

}
