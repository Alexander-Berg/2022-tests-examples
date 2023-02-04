package ru.yandex.vos2.autoru.api.utils

import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpRequest, MediaTypes, StatusCodes}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.{JsValue, Json}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.utils.Vos2ApiSuite
import ru.yandex.vos2.util.Protobuf

@RunWith(classOf[JUnitRunner])
class PatchOfferHandlerTest extends AnyFunSuite with Vos2ApiSuite with OptionValues {
  implicit val traced: Traced = Traced.empty

  def createOffer(f: Offer.Builder => Unit = _ => ()): Offer = {
    val offerId = 1043045004L
    val offer: OfferModel.Offer = getOfferById(offerId)
    val builder = offer.toBuilder
    f(builder)
    val rebuildOffer = builder.build()
    components.getOfferDao().saveMigrated(Seq(rebuildOffer))
    components.getOfferDao().findById(offer.getOfferID, includeRemoved = true).value
  }

  def clear(offer: Offer): Offer = {
    val builder = offer.toBuilder
    builder.clearTimestampAnyUpdate()
    builder.clearTimestampCheck()
    builder.build()
  }

  def compare(offer1: Offer, offer2: Offer): Unit = {
    clear(offer1) shouldBe clear(offer2)
  }

  def jsonPatchRequest(req: HttpRequest)(json: => JsValue): HttpRequest = {
    req.withEntity(
      HttpEntity(
        ContentType(MediaTypes.`application/json-patch+json`),
        Json.stringify(json).getBytes("UTF-8")
      )
    )
  }

  def doRequest(req: HttpRequest)(json: => JsValue): Offer = {
    responseWithOffer(jsonPatchRequest(req)(json))
  }

  def responseWithOffer(req: HttpRequest): Offer = {
    req ~> route ~> check {
      status shouldBe StatusCodes.OK
      Protobuf.fromJson[Offer](responseAs[String])
    }
  }

  test("add") {

    val originalOffer = createOffer()
    val offerId = originalOffer.getOfferID

    val vin = "JN1TESY61U0161815"

    val editOffer = doRequest(Patch(s"/utils/patch-offer/$offerId")) {
      Json.parse(
        s"""
           |[
           |  {
           |    "op": "add",
           |    "path": "/offer_autoru/documents/vin",
           |    "value": "$vin"
           |  }
           |]
      """.stripMargin
      )
    }

    val expectedOffer = {
      val builder = originalOffer.toBuilder
      builder.getOfferAutoruBuilder.getDocumentsBuilder
        .setVin(vin)

      builder.build()
    }

    compare(editOffer, expectedOffer)
  }

  test("remove") {

    val originalOffer = createOffer()
    val offerId = originalOffer.getOfferID

    val idx = 2

    val editOffer = doRequest(Patch(s"/utils/patch-offer/$offerId")) {
      Json.parse(
        s"""
           |[
           |  {
           |    "op": "remove",
           |    "path": "/offer_autoru/photo/$idx"
           |  }
           |]
      """.stripMargin
      )
    }

    val expectedOffer = {
      val builder = originalOffer.toBuilder
      builder.getOfferAutoruBuilder
        .removePhoto(idx)

      builder.build()
    }

    compare(editOffer, expectedOffer)
  }

  test("replace") {

    val originalOffer = createOffer(_.getOfferAutoruBuilder.setIsColorMetallic(false))
    val offerId = originalOffer.getOfferID

    val isColorMetallic = true

    val editOffer = doRequest(Patch(s"/utils/patch-offer/$offerId")) {
      Json.parse(
        s"""
           |[
           |  {
           |    "op": "replace",
           |    "path": "/offer_autoru/is_color_metallic",
           |    "value": $isColorMetallic
           |  }
           |]
      """.stripMargin
      )
    }

    val expectedOffer = {
      val builder = originalOffer.toBuilder
      builder.getOfferAutoruBuilder
        .setIsColorMetallic(isColorMetallic)

      builder.build()
    }

    compare(editOffer, expectedOffer)
  }

  test("move") {

    val originalOffer = createOffer()
    val offerId = originalOffer.getOfferID

    val countPhoto = originalOffer.getOfferAutoru.getPhotoCount

    val idxFrom = 1
    val idxTo = 5
    val order = 10

    val editOffer = doRequest(Patch(s"/utils/patch-offer/$offerId")) {
      Json.parse(
        s"""
           |[
           |  {
           |    "op": "move",
           |    "from": "/offer_autoru/photo/$idxFrom",
           |    "path": "/offer_autoru/photo/$idxTo"
           |  },
           |  {
           |    "op": "replace",
           |    "path": "/offer_autoru/photo/$idxTo/order",
           |    "value" : $order
           |  }
           |]
      """.stripMargin
      )
    }

    editOffer.getOfferAutoru.getPhotoCount shouldBe countPhoto

    val expectedOffer = {
      val builder = originalOffer.toBuilder

      val photo = builder.getOfferAutoru.getPhoto(idxFrom)

      builder.getOfferAutoruBuilder
        .removePhoto(idxFrom)
        .addPhoto(idxTo, photo)
        .getPhotoBuilder(idxTo)
        .setOrder(order)

      builder.build()
    }

    compare(editOffer, expectedOffer)
  }

  test("copy") {

    val originalOffer = createOffer()
    val offerId = originalOffer.getOfferID

    val countPhoto = originalOffer.getOfferAutoru.getPhotoCount

    val idxFrom = 3
    val idxTo = 5
    val order = 10

    val editOffer = doRequest(Patch(s"/utils/patch-offer/$offerId")) {
      Json.parse(
        s"""
           |[
           |  {
           |    "op": "copy",
           |    "from": "/offer_autoru/photo/$idxFrom",
           |    "path": "/offer_autoru/photo/$idxTo"
           |  },
           |  {
           |    "op": "replace",
           |    "path": "/offer_autoru/photo/$idxTo/order",
           |    "value" : $order
           |  }
           |]
      """.stripMargin
      )
    }

    editOffer.getOfferAutoru.getPhotoCount shouldBe countPhoto + 1

    val expectedOffer = {
      val builder = originalOffer.toBuilder

      val photo = builder.getOfferAutoru.getPhoto(idxFrom)

      builder.getOfferAutoruBuilder
        .addPhoto(idxTo, photo)
        .getPhotoBuilder(idxTo)
        .setOrder(order)

      builder.build()
    }

    compare(editOffer, expectedOffer)
  }

  test("test") {

    val originalOffer = createOffer()
    val offerId = originalOffer.getOfferID

    val idx = 4
    val name = originalOffer.getOfferAutoru.getPhoto(idx).getName

    val req = jsonPatchRequest(Patch(s"/utils/patch-offer/$offerId")) {
      Json.parse(
        s"""
           |[
           |  {
           |    "op": "test",
           |    "path": "/offer_autoru/photo/$idx/name",
           |    "value": "$name"
           |  }
           |]
      """.stripMargin
      )
    }

    req ~> route ~> check {
      status shouldBe StatusCodes.OK
    }

  }

  test("error") {

    val originalOffer = createOffer()
    val offerId = originalOffer.getOfferID

    val vin = "JN1TESY61U0161815"

    val req = jsonPatchRequest(Patch(s"/utils/patch-offer/$offerId")) {
      Json.parse(
        s"""
           |[
           |  {
           |    "op": "add",
           |    "path": "/offer_autoru/XXXX/vin",
           |    "value": "$vin"
           |  }
           |]
      """.stripMargin
      )
    }

    req ~> route ~> check {
      status shouldBe StatusCodes.InternalServerError
    }

  }

  test("not found offer") {

    val offerId = "1076986273-83d6aa"

    val req = jsonPatchRequest(Patch(s"/utils/patch-offer/$offerId")) {
      Json.parse(
        s"""
           |[
           |  {
           |    "op": "add",
           |    "path": "/offer_autoru/XXXXX/vin",
           |    "value": "YYY"
           |  }
           |]
      """.stripMargin
      )
    }

    req ~> route ~> check {
      status shouldBe StatusCodes.NotFound
    }
  }
}
