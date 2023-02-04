package ru.auto.api.routes.v1.user.notes

import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, MediaTypes, StatusCodes}
import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.Offer
import ru.auto.api.ApiSuite
import ru.auto.api.managers.events.StatEventsManager
import ru.auto.api.managers.favorite.FavoritesHelper
import ru.auto.api.managers.offers.OfferCardManager
import ru.auto.api.model.CategorySelector.Cars
import ru.auto.api.model.ModelGenerators
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.api.services.{MockedClients, MockedPassport}
import ru.auto.api.util.FavoriteUtils._

import scala.concurrent.Future

/**
  * Author Nikita Melentev (ndmelentev@yandex-team.ru)
  * Created: 17.05.2017
  */
class NotesHandlerTest extends ApiSuite with MockedClients with MockedPassport {
  override lazy val offerCardManager: OfferCardManager = mock[OfferCardManager]
  override lazy val statEventsManager: StatEventsManager = mock[StatEventsManager]
  override lazy val favoritesHelper = mock[FavoritesHelper]

  private val category = Cars
  private val favDomain = getFavDomain(category)
  private val noteDomain = getNoteDomain(category)

  before {
    reset(passportManager, offerCardManager, watchClient)
    when(passportManager.getClientId(?)(?)).thenReturnF(None)
    when(statEventsManager.logFavoriteOrNoteEvent(?, ?)(?)(?)).thenReturn(Future.unit)

  }

  after {
    verifyNoMoreInteractions(passportManager, offerCardManager, watchClient)
  }

  test("get notes") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val offerId = ModelGenerators.OfferIDGen.next
    val note = BasicGenerators.readableString.next
    val offer = Offer.newBuilder().setCategory(category.enum).setId(offerId.toString).setNote(note).build()

    when(favoritesHelper.getNotesAndFavorites(?, ?)(?))
      .thenReturnF(Seq(offer))

    Get(s"/1.0/user/notes/cars/") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
          responseAs[String] should matchJson(s"""{
               |  "offers" : [ {
               |    "category" : "CARS",
               |    "id" : "$offerId",
               |    "seller_type" : "PRIVATE",
               |    "is_favorite" : false,
               |    "note" : "$note"
               |  } ],
               |  "status" : "SUCCESS"
               |}""".stripMargin)
        }
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("count notes") {
    val user = ModelGenerators.PrivateUserRefGen.next

    when(favoriteClient.countNotes(?, ?)(?))
      .thenReturnF(2)

    Get(s"/1.0/user/notes/cars/count") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        withClue(responseAs[String]) {
          status shouldBe StatusCodes.OK
        }
        verify(favoriteClient).countNotes(eq(noteDomain), eq(user))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("add note as plain text") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val offer1 = ModelGenerators.OfferGen.next
    val note = BasicGenerators.readableString.next
    val entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, note)

    when(favoriteClient.addNote(?, ?, ?, ?)(?))
      .thenReturn(Future.unit)
    when(watchClient.patchWatch(?, ?, ?)(?)).thenReturn(Future.unit)

    Post(s"/1.0/user/notes/cars/${offer1.getId}", entity) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
        verify(favoriteClient).addNote(eq(noteDomain), eq(user), eq(offer1.id), eq(note))(?)
        verify(watchClient).patchWatch(eq(user), ?, ?)(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("add note as json") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val offerId = ModelGenerators.OfferIDGen.next
    val note = BasicGenerators.readableString.next
    val entity = HttpEntity(ContentTypes.`application/json`, Json.obj("note" -> note).toString)

    when(favoriteClient.addNote(?, ?, ?, ?)(?))
      .thenReturn(Future.unit)
    when(watchClient.patchWatch(?, ?, ?)(?)).thenReturn(Future.unit)

    Post(s"/1.0/user/notes/cars/$offerId", entity) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
        verify(favoriteClient).addNote(eq(noteDomain), eq(user), eq(offerId), eq(note))(?)
        verify(watchClient).patchWatch(eq(user), ?, ?)(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("upsert note as text/plain") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val offerId = ModelGenerators.OfferIDGen.next
    val note = BasicGenerators.readableString.next
    val entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, note)

    when(favoriteClient.upsertNote(?, ?, ?, ?)(?))
      .thenReturn(Future.unit)

    Put(s"/1.0/user/notes/cars/$offerId", entity) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
        verify(favoriteClient).upsertNote(eq(noteDomain), eq(user), eq(offerId), eq(note))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("upsert note as application/json") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val offerId = ModelGenerators.OfferIDGen.next
    val note = BasicGenerators.readableString.next
    val entity = HttpEntity(ContentTypes.`application/json`, Json.obj("note" -> note).toString)

    when(favoriteClient.upsertNote(?, ?, ?, ?)(?))
      .thenReturn(Future.unit)

    Put(s"/1.0/user/notes/cars/$offerId", entity) ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
        verify(favoriteClient).upsertNote(eq(noteDomain), eq(user), eq(offerId), eq(note))(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

  test("delete note") {
    val user = ModelGenerators.PrivateUserRefGen.next
    val offerId = ModelGenerators.OfferIDGen.next

    when(favoriteClient.deleteNote(?, ?, ?)(?))
      .thenReturn(Future.unit)
    when(watchClient.patchWatch(?, ?, ?)(?)).thenReturn(Future.unit)

    Delete(s"/1.0/user/notes/cars/$offerId") ~>
      addHeader(Accept(MediaTypes.`application/json`)) ~>
      addHeader("x-uid", user.uid.toString) ~>
      xAuthorizationHeader ~>
      route ~>
      check {
        val response = responseAs[String]
        withClue(response) {
          status shouldBe StatusCodes.OK
        }
        verify(favoriteClient).deleteNote(eq(noteDomain), eq(user), eq(Seq(offerId)))(?)
        verify(watchClient).patchWatch(eq(user), ?, ?)(?)
        verify(passportManager).getClientId(eq(user))(?)
      }
  }

}
