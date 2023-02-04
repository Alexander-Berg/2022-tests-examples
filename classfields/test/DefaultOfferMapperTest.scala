package ru.yandex.vertis.general.search.logic.test

import com.google.protobuf.timestamp.Timestamp
import common.zio.logging.Logging
import general.aglomerat.api.GetClusteringMetaResponse
import general.aglomerat.model.FullDuplicatesMeta
import general.common.price_model
import general.common.price_model.Price
import general.common.price_model.Price.Price.PriceInCurrency
import general.common.seller_model.SellerId
import general.common.seller_model.SellerId.SellerId.UserId
import general.gost.offer_model.AttributeValue.Value.Number
import general.gost.offer_model.OfferStatusEnum.OfferStatus
import general.gost.offer_model.{
  Attribute,
  AttributeValue,
  Category => GostCategory,
  Offer,
  OfferOriginEnum,
  OfferView,
  Photo
}
import general.gost.seller_model.Seller
import general.users.model.LimitedUserView
import general.vasabi.api.GetOffersVasesResponse
import general.vasabi.offer_vas.{OfferVas, OfferVasView, OfferVases}
import ru.yandex.vertis.general.aglomerat.testkit.TestAglomeratService
import ru.yandex.vertis.general.search.constants.Constants.{
  FactorVasFieldPrefix,
  FullDuplicatesClusterId,
  TitlePriceSellerClusterId,
  VasFieldName
}
import ru.yandex.vertis.general.search.logic.extractors.OfferViewFieldExtractor.OfferViewFieldExtractor
import ru.yandex.vertis.general.search.logic.extractors.util.RawDocumentHelper
import ru.yandex.vertis.general.search.logic.extractors.util.RawDocumentHelper.AttributeZone
import ru.yandex.vertis.general.search.logic.{DefaultOfferMapper, OfferMapper, SearchEmbedder}
import ru.yandex.vertis.general.search.testkit.TestOfferViewFieldExtractor
import ru.yandex.vertis.general.vasabi.testkit.TestVasabiService
import vertis.vasgen.common.RawValue
import vertis.vasgen.common.RawValue.ValueTypeOneof
import vertis.vasgen.document.{PrimaryKey, RawDocument, RawField}
import vertis.vasgen.options.options.VasgenFieldOptions
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.mock.Expectation
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import zio.{Task, ZIO, ZLayer}

object DefaultOfferMapperTest extends DefaultRunnableSpec {

  private val OfferId = "123456789"
  private val OfferVersion = 10L
  private val OfferVasGoodsCode = "vas_code"
  private val ExtractedRawFieldName = "raw_field"

  private val ValidOffer = Offer(
    title = "TITLE",
    description = "DESCRIPTION",
    price = Some(Price(PriceInCurrency(price_model.PriceInCurrency(100)))),
    category = Some(GostCategory("category_id", 1)),
    attributes = Seq(Attribute("attribute_id", 2, Some(AttributeValue(Number(3))))),
    seller = Some(Seller())
  )

  private val ValidOfferView = OfferView(
    sellerId = Some(SellerId(UserId(123))),
    offerId = OfferId,
    offer = Some(ValidOffer),
    status = OfferStatus.ACTIVE,
    version = OfferVersion,
    origin = OfferOriginEnum.OfferOrigin.FEED,
    updatedAt = Some(Timestamp(1000))
  )

  private val OfferVasesResponse = GetOffersVasesResponse(
    Map(OfferId -> OfferVases(activeVases = Seq(OfferVasView(Some(OfferVas(OfferVasGoodsCode))))))
  )

  private val ClusteringMetaResponse = GetClusteringMetaResponse(
    meta = Some(FullDuplicatesMeta(indexedAt = Some(Timestamp(2000))))
  )

  private val ValidUserView = LimitedUserView(id = 123)

  private val ExtractedRawFields = List(
    RawDocumentHelper.createRawField(
      name = ExtractedRawFieldName,
      values = Seq(RawValue(ValueTypeOneof.String("raw_value")))
    )
  )

  private val ExtractedTextFields =
    RawDocumentHelper.createTextFields(textZone = AttributeZone, values = Seq("text_attribute"))

  private def testOfferViewFieldExtractorNotCalled = TestOfferViewFieldExtractor
    .Extract(anything, Expectation.value(Nil -> Nil))
    .atMost(0)

  private def offerMapperLayer = {
    val offerViewFieldExtractorMock = ZLayer.requires[OfferViewFieldExtractor]
    val testVasabi = TestVasabiService.layer
    val testAglomerat = TestAglomeratService.layer
    val testSearchEmbedder = SearchEmbedder.noop
    val logging = Logging.any
    val clock = Clock.any
    val inputLayers =
      offerViewFieldExtractorMock ++ testVasabi ++ testAglomerat ++ testSearchEmbedder ++ logging ++ clock
    (inputLayers >>> OfferMapper.live) ++ testVasabi ++ testAglomerat
  }

  private def buildOfferMapperLayer(offerViewFieldExtractorMock: Expectation[OfferViewFieldExtractor]) = {
    val logging = Logging.live
    val clock = Clock.live
    logging ++ clock ++ offerViewFieldExtractorMock.toLayer >+> offerMapperLayer
  }

  private def assertCommonFields(document: RawDocument) = assert(document) {
    hasField("pk", (_: RawDocument).pk, isSome(equalTo(PrimaryKey(PrimaryKey.Value.Str(OfferId))))) &&
    hasField("version", (_: RawDocument).version, equalTo(OfferVersion)) &&
    hasField("epoch", (_: RawDocument).epoch, equalTo(DefaultOfferMapper.Epoch)) &&
    hasField("ttl", (_: RawDocument).ttl, isNone) &&
    hasField("modifiedAt", (_: RawDocument).modifiedAt, isSome)
  }

  private def assertHasRawField(document: RawDocument, name: String) = assert(document) {
    hasField(
      "fields",
      _.fields,
      exists(
        hasField(
          "metadata",
          (_: RawField).metadata,
          isSome(hasField("name", (_: VasgenFieldOptions).name, isSome(equalTo(name))))
        )
      )
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("OfferMapper")(
      testM("Преобразование валидного документа для добавления в поиск") {
        val offerViewFieldExtractorMock = TestOfferViewFieldExtractor
          .Extract(anything, Expectation.value(ExtractedRawFields -> ExtractedTextFields))
          .atMost(1)
        (for {
          _ <- TestVasabiService.setGetOfferVases(_ => Task.succeed(OfferVasesResponse))
          _ <- TestAglomeratService.setGetClusteringMetaResponse(_ => Task.succeed(ClusteringMetaResponse))
          document <- OfferMapper.map(ValidOfferView, Some(ValidUserView))
        } yield assertCommonFields(document) &&
          assertHasRawField(document, FullDuplicatesClusterId) &&
          assertHasRawField(document, TitlePriceSellerClusterId) &&
          assertHasRawField(document, ExtractedRawFieldName) &&
          assertHasRawField(document, VasFieldName) &&
          assertHasRawField(document, s"$FactorVasFieldPrefix.$OfferVasGoodsCode") &&
          assert(document) {
            hasField("action", (_: RawDocument).action.isUpsert, isTrue) &&
            hasField("text", (_: RawDocument).text, hasSameElements(ExtractedTextFields))
          }).provideLayer(buildOfferMapperLayer(offerViewFieldExtractorMock))
      },
      testM("Если пользователь удален, то отправляем документ на удаление") {
        (for {
          deletedUser <- ZIO.succeed(ValidUserView.copy(isDeleted = true))
          document <- OfferMapper.map(ValidOfferView, Some(deletedUser))
        } yield assertCommonFields(document) && assert(document) {
          hasField("action", (_: RawDocument).action.isDelete, isTrue)
        }).provideLayer(buildOfferMapperLayer(testOfferViewFieldExtractorNotCalled))
      },
      testM("Если фото не прогрузились, то отправляем документ на удаление") {
        (for {
          offerWithIncompletePhotos <- ZIO.succeed(
            ValidOfferView.copy(offer = Some(ValidOffer.copy(photos = Seq(Photo(mdsLocation = None, error = None)))))
          )
          document <- OfferMapper.map(offerWithIncompletePhotos, Some(ValidUserView))
        } yield assertCommonFields(document) && assert(document) {
          hasField("action", (_: RawDocument).action.isDelete, isTrue)
        }).provideLayer(buildOfferMapperLayer(testOfferViewFieldExtractorNotCalled))
      },
      testM("Если оффер забанен, то отправляем документ на удаление") {
        (for {
          bannedOffer <- ZIO.succeed(ValidOfferView.copy(status = OfferStatus.BANNED))
          document <- OfferMapper.map(bannedOffer, Some(ValidUserView))
        } yield assertCommonFields(document) && assert(document) {
          hasField("action", (_: RawDocument).action.isDelete, isTrue)
        }).provideLayer(buildOfferMapperLayer(testOfferViewFieldExtractorNotCalled))
      }
    )
  }
}
