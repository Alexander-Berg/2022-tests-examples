package ru.yandex.realty.rent.backend.manager

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import realty.palma.rent_common.RentDocument
import realty.palma.rent_flat.{RentFlat => PalmaRentFlat}
import ru.yandex.realty.clients.searcher.gen.SearcherResponseModelGenerators
import ru.yandex.realty.errors.NotFoundApiException
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.proto.model.flat.FlatDocuments
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.vertis
import ru.yandex.vertis.palma.encrypted
import ru.yandex.vertis.palma.services.encrypted_service.Images

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FlatDocumentManagerSpec
  extends SpecBase
  with AsyncSpecBase
  with RequestAware
  with PropertyChecks
  with SearcherResponseModelGenerators {

  "FlatDocumentManager.getDocumentUrl" should {
    "fail if document is not in db-flat" in new Wiring with Data with FlatDocumentManagerData {

      (mockFlatDao
        .findById(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(Future.successful(flatWithoutDocument))

      (mockPalmaRentFlatClient
        .get(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(
          Future.successful(Some(PalmaRentFlat(flatId = sampleFlatId, documents = List(rentDocument))))
        )

      val error =
        flatDocumentManager.getDocumentUrl(sampleFlatId, documentId, PassportUser(sampleUid)).failed.futureValue
      error shouldBe a[NotFoundApiException]
    }

    "fail if document is not in palma-flat" in new Wiring with Data with FlatDocumentManagerData {

      (mockFlatDao
        .findById(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(Future.successful(flatWithDocument))

      (mockPalmaRentFlatClient
        .get(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(
          Future.successful(Some(PalmaRentFlat(flatId = sampleFlatId)))
        )

      val error =
        flatDocumentManager.getDocumentUrl(sampleFlatId, documentId, PassportUser(sampleUid)).failed.futureValue
      error shouldBe a[NotFoundApiException]
    }

    "return links to correct image" in new Wiring with Data with FlatDocumentManagerData {

      (mockFlatDao
        .findById(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(Future.successful(flatWithDocument))

      (mockPalmaRentFlatClient
        .get(_: String)(_: Traced))
        .expects(sampleFlatId, *)
        .returning(
          Future.successful(Some(PalmaRentFlat(flatId = sampleFlatId, documents = List(rentDocument))))
        )

      val result = flatDocumentManager.getDocumentUrl(sampleFlatId, documentId, PassportUser(sampleUid)).futureValue
      result.getUrlsMap.asScala.toMap shouldEqual palmaImage.aliases.mapValues(value => s"$avatarnicaDownloadUrl$value")
    }
  }

  trait FlatDocumentManagerData extends RentModelsGen {
    this: Wiring with Data =>

    val documentId = "document_id"
    val imageKey = "image_key"

    val rentDocument = RentDocument(id = documentId, image = Some(encrypted.content.Image(key = imageKey)))

    val flatWithDocument = sampleFlat.copy(
      data = sampleFlat.data.toBuilder
        .setDocuments(FlatDocuments.newBuilder().setFlatKeysPhotoDocumentId(documentId))
        .build()
    )

    val flatWithoutDocument = sampleFlat.copy(
      data = sampleFlat.data.toBuilder
        .setDocuments(FlatDocuments.newBuilder())
        .build()
    )

    val palmaImage = vertis.palma.images.images.Image(aliases = Map("orig" -> "orig_url"))

    (mockUserDao
      .findByUidOpt(_: Long, _: Boolean)(_: Traced))
      .expects(sampleUid, *, *)
      .returning(Future.successful(Some(sampleUser)))

    (mockPalmaEncryptedServiceClient
      .getImage(_: Images.GetRequest)(_: Traced))
      .expects(where {
        case (request: Images.GetRequest, _) =>
          request.key == imageKey && request.watermark.contains(sampleUserId)
      })
      .returning(Future.successful(Some(palmaImage)))
  }
}
