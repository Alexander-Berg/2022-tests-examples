package ru.yandex.auto.garage.converters.cards

import auto.carfax.common.utils.avatars.PhotoInfoId
import com.google.protobuf.util.Timestamps
import com.google.protobuf.{BoolValue, Timestamp}
import org.scalatest.Assertion
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel.{Documents, PtsStatus}
import ru.auto.api.CarsModel.CarInfo
import ru.auto.api.CommonModel.Date
import ru.auto.api.internal.Mds
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.vin.Common.{IdentifierType, Photo, S3FileInfo}
import ru.auto.api.vin.VinReportModel.InsuranceType
import ru.auto.api.vin.garage.GarageApiModel
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.auto.api.vin.garage.GarageApiModel.InsuranceSource
import ru.auto.api.vin.garage.GarageApiModel.ProvenOwnerState.DocumentsPhotos.Photos
import ru.auto.api.vin.{Common, VinReportModel}
import ru.auto.api.{CatalogModel, CommonModel}
import ru.auto.panoramas.InteriorModel.InteriorPanorama
import ru.auto.panoramas.PanoramasModel.Panorama
import ru.yandex.auto.garage.managers.CardsManager.AddedByMeta
import ru.yandex.auto.garage.utils.features.GarageFeaturesRegistry
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Meta.CardTypeInfo
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Meta.CardTypeInfo.{ChangeEvent, ChangeTypeSource}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.ProvenOwnerState.{DocumentsPhotos, Promocode, Verdict}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.VehicleInfo.{AutoruCatalogData, Mileage}
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema._
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.MetaData
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.MetaData.RecognizedLicensePlates
import ru.yandex.auto.vin.decoder.proto.TtxSchema._
import auto.carfax.common.utils.protobuf.ProtobufConverterOps.ProtoTimestampOps
import auto.carfax.common.utils.misc.StringUtils.convertToLatin
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.mutable
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

class PublicToInternalCardConverterTest extends AnyFunSuite with MockitoSupport {

  private val carInfoToCatalogDataConverter = mock[CarInfoToCatalogDataConverter]
  private val garageFeaturesRegistry = mock[GarageFeaturesRegistry]
  private val converter = new PublicToInternalCardConverter(carInfoToCatalogDataConverter, garageFeaturesRegistry)
  private val now = System.currentTimeMillis()

  test("convert new card") {
    when(carInfoToCatalogDataConverter.convert(?)).thenReturn(Some(CatalogData))
    val addedByMeta = AddedByMeta(
      identifierType = IdentifierType.LICENSE_PLATE,
      addedManually = true,
      addedByOffer = false
    )
    val converted =
      converter.convertNewCard(DefaultCard, addedByMeta, GarageSchema.Source.AUTORU, now)

    assert(converted == DefaultInternalCard)
  }

  test("convert existing card") {
    when(carInfoToCatalogDataConverter.convert(?)).thenReturn(Some(CatalogData))
    val existingCard = {
      val builder = GarageCard.newBuilder()
      builder
        .setMeta(DefaultInternalCard.getMeta)
        .setSource(DefaultInternalCard.getSource)
        .setOfferInfo(OfferInfo.newBuilder().setOfferId("1049777022-811fc").build())
        .setWatchingState(GarageCard.WatchingState.newBuilder().setLastVisited(Timestamps.fromMillis(now)))
        .addNotifications(
          Notification
            .newBuilder()
            .setDeliveryDeadline(Timestamps.fromMillis(now))
            .setNotificationType(Notification.NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)
            .setMaxTries(5)
        )
        .setInsuranceInfo(DefaultInternalCard.getInsuranceInfo)

      builder.build()
    }

    val converted = converter.convertExistingCard(DefaultCard, existingCard, now)
    assert(
      converted == DefaultInternalCard.toBuilder
        .setWatchingState(GarageCard.WatchingState.newBuilder().setLastVisited(Timestamps.fromMillis(now)))
        .setInsuranceInfo(DefaultInternalCard.getInsuranceInfo)
        .addNotifications(
          Notification
            .newBuilder()
            .setDeliveryDeadline(Timestamps.fromMillis(now))
            .setNotificationType(Notification.NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)
            .setMaxTries(5)
        )
        .build()
    )
  }

  test(
    "check merge and convert InsurancesInfo with many Insurances in Internal and many Insurances in Public, when DeleteInsurancesWhenMerge is true"
  ) {
    convertInsurancesInfo(resultAfterMerge = InsurancesFromInternalAfterMergePublicCard)
  }

  test(
    "check merge and convert InsurancesInfo with many Insurances in Internal and empty Insurances in Public and DeleteInsurancesWhenMerge is true"
  ) {
    when(carInfoToCatalogDataConverter.convert(?)).thenReturn(Some(CatalogData))

    val cardWithEmptyInsurance =
      GarageApiModel.Card.newBuilder().setInsuranceInfo(GarageApiModel.InsuranceInfo.newBuilder().build()).build()
    val existingCard = Some(GarageSchema.GarageCard.newBuilder().setInsuranceInfo(InsurancesFromInternalCard).build())

    import scala.jdk.CollectionConverters.ListHasAsScala
    val convertedWithoutEmptyInsurance = converter.convertToInternalInsuranceInfoOpt(
      cardWithEmptyInsurance.getInsuranceInfo,
      existingCard.map(_.getInsuranceInfo),
      now,
      cardWithEmptyInsurance.hasInsuranceInfo
    )

    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.size() == InsurancesFromInternalCard.getInsurancesList.size()
    )
    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.asScala.forall(_.getIsDeleted)
    )
  }

  test(
    "check merge and convert InsurancesInfo with many Insurances in Internal and without Insurances in Public and DeleteInsurancesWhenMerge is true"
  ) {
    when(carInfoToCatalogDataConverter.convert(?)).thenReturn(Some(CatalogData))

    val cardWithoutInsurance = GarageApiModel.Card.newBuilder().build()
    val existingCard = Some(GarageSchema.GarageCard.newBuilder().setInsuranceInfo(InsurancesFromInternalCard).build())

    import scala.jdk.CollectionConverters.ListHasAsScala
    val convertedWithoutEmptyInsurance = converter.convertToInternalInsuranceInfoOpt(
      cardWithoutInsurance.getInsuranceInfo,
      existingCard.map(_.getInsuranceInfo),
      now,
      cardWithoutInsurance.hasInsuranceInfo
    )

    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.size() == InsurancesFromInternalCard.getInsurancesList.size()
    )
    for {
      insuranceConverted <- convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
    } yield {
      assert(
        InsurancesFromInternalCard.getInsurancesList.asScala.contains(insuranceConverted)
      )
    }
  }

  test(
    "check Files Without Update when many Insurances in Internal and without Insurances in Public and DeleteInsurancesWhenMerge is true"
  ) {
    when(carInfoToCatalogDataConverter.convert(?)).thenReturn(Some(CatalogData))

    val insurancePublic = GarageApiModel.InsuranceInfo
      .newBuilder()
      .addAllInsurances(
        List(
          // ОСАГО
          buildPublicInsurance(
            number = "new_insurance_with_attachment",
            serial = "new",
            startDate = Timestamps.fromMillis(now - 100.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 250.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            attachmentOpt = Some {
              buildPublicAttachment(
                buildFile(
                  "new_insurance_with_attachment_url",
                  buildS3FileInfo("new_insurance_with_attachment", "insurance", Common.MimeType.APPLICATION_PDF)
                )
              )
            }
          ), // Актуальная страховка ОСАГО // Без файлов
          buildPublicInsurance(
            number = "new_insurance_without_attachment",
            serial = "new",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO
          ),
          buildPublicInsurance(
            number = "was_insurance_with_new_attachment",
            serial = "was",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            attachmentOpt = Some {
              buildPublicAttachment(
                buildFile("new_url", buildS3FileInfo("new_attachment", "insurance", Common.MimeType.APPLICATION_PDF))
              )
            }
          ),
          buildPublicInsurance(
            number = "was_insurance_delete_attachment",
            serial = "was",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            attachmentOpt = Some(GarageApiModel.Insurance.Attachment.newBuilder().build())
          ),
          buildPublicInsurance(
            number = "was_insurance_change_attachment",
            serial = "was",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            attachmentOpt = Some {
              buildPublicAttachment(
                buildFile(
                  "change_url",
                  buildS3FileInfo("change_attachment", "insurance", Common.MimeType.APPLICATION_PDF)
                )
              )
            }
          ),
          buildPublicInsurance(
            number = "was_insurance_with_attachment_and_in_request_with_empty_attachment",
            serial = "was",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            attachmentOpt = None
          )
        ).asJava
      )
      .build()

    val insuranceInternal = GarageSchema.InsuranceInfo
      .newBuilder()
      .addAllInsurances(
        List(
          buildInternalInsurance(
            number = "was_insurance_with_new_attachment",
            serial = "was",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            attachments = List.empty
          ),
          buildInternalInsurance(
            number = "was_insurance_delete_attachment",
            serial = "was",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            attachments = List(
              buildInternalAttachment(
                buildS3FileInfo("was_insurance_delete_attachment.pdf", "insurance", Common.MimeType.APPLICATION_PDF)
              )
            )
          ),
          buildInternalInsurance(
            number = "was_insurance_change_attachment",
            serial = "was",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            attachments = List(
              buildInternalAttachment(
                buildS3FileInfo("was_insurance_change_attachment.pdf", "insurance", Common.MimeType.APPLICATION_PDF)
              )
            )
          ),
          buildInternalInsurance(
            number = "was_insurance_with_attachment_and_in_request_with_empty_attachment",
            serial = "was",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            attachments = List(
              buildInternalAttachment(
                buildS3FileInfo(
                  "was_insurance_with_attachment_and_in_request_with_empty_attachment.pdf",
                  "insurance",
                  Common.MimeType.APPLICATION_PDF
                )
              )
            )
          )
        ).asJava
      )
      .build()

    val cardInsurance = GarageApiModel.Card.newBuilder().setInsuranceInfo(insurancePublic).build()
    val existingCard = Some(GarageSchema.GarageCard.newBuilder().setInsuranceInfo(insuranceInternal).build())
    val convertedWithoutEmptyInsurance = converter.convertToInternalInsuranceInfoOpt(
      cardInsurance.getInsuranceInfo,
      existingCard.map(_.getInsuranceInfo),
      now,
      cardInsurance.hasInsuranceInfo
    )
    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
        .find(_.getNumber == "new_insurance_with_attachment")
        .get
        .getAttachmentList
        .asScala
        .head
        .getFileInfo
        .getName == "new_insurance_with_attachment"
    )

    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
        .find(_.getNumber == "new_insurance_without_attachment")
        .get
        .getAttachmentCount == 0
    )

    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
        .find(_.getNumber == "was_insurance_with_new_attachment")
        .get
        .getAttachmentCount == 1
    )
    assert(
      !convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
        .find(_.getNumber == "was_insurance_with_new_attachment")
        .get
        .getAttachmentList
        .asScala
        .find(p = _.getFileInfo.getName == "new_attachment")
        .get
        .getIsDeleted
    )

    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
        .find(_.getNumber == "was_insurance_delete_attachment")
        .get
        .getAttachmentCount == 1
    )
    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
        .find(_.getNumber == "was_insurance_delete_attachment")
        .get
        .getAttachmentList
        .asScala
        .head
        .getIsDeleted
    )

    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
        .find(_.getNumber == "was_insurance_change_attachment")
        .get
        .getAttachmentCount == 2
    )
    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
        .find(_.getNumber == "was_insurance_change_attachment")
        .get
        .getAttachmentList
        .asScala
        .find(p = _.getFileInfo.getName == "was_insurance_change_attachment.pdf")
        .get
        .getIsDeleted
    )
    assert(
      !convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
        .find(_.getNumber == "was_insurance_change_attachment")
        .get
        .getAttachmentList
        .asScala
        .find(p = _.getFileInfo.getName == "change_attachment")
        .get
        .getIsDeleted
    )

    assert(
      convertedWithoutEmptyInsurance.get.getInsurancesList.asScala
        .find(_.getNumber == "was_insurance_with_attachment_and_in_request_with_empty_attachment")
        .get
        .getAttachmentCount == 1
    )

  }

  test(
    "check merge and convert InsurancesInfo from Public to Internal when source in Public different"
  ) {
    when(carInfoToCatalogDataConverter.convert(?)).thenReturn(Some(CatalogData))

    val insurancePublic = GarageApiModel.InsuranceInfo
      .newBuilder()
      .addAllInsurances(
        List(
          // ОСАГО
          buildPublicInsurance(
            number = "new_insurance_with_attachment",
            serial = "new",
            startDate = Timestamps.fromMillis(now - 100.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 250.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            insuranceSourceOpt = Some(InsuranceSource.MANUAL)
          )
        ).asJava
      )
      .build()

    val cardWithInsurance =
      GarageApiModel.Card.newBuilder().setInsuranceInfo(insurancePublic).build()

    val insuranceInternal = GarageSchema.InsuranceInfo
      .newBuilder()
      .addAllInsurances(
        List(
          // ОСАГО
          buildInternalInsurance(
            number = "new_insurance_with_attachment",
            serial = "new",
            startDate = Timestamps.fromMillis(now - 100.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 250.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            insuranceSourceOpt = Some(InsuranceSource.RSA)
          )
        ).asJava
      )
      .build()

    val existingCard = Some(GarageSchema.GarageCard.newBuilder().setInsuranceInfo(insuranceInternal).build())

    import scala.jdk.CollectionConverters.ListHasAsScala
    val converted = converter.convertToInternalInsuranceInfoOpt(
      cardWithInsurance.getInsuranceInfo,
      existingCard.map(_.getInsuranceInfo),
      now,
      cardWithInsurance.hasInsuranceInfo
    )

    assert(
      converted.get.getInsurancesList.size() == 1
    )
    assert(
      converted.get.getInsurancesList.asScala.head.getSource == InsuranceSource.RSA
    )
  }

  test(
    "check merge and convert InsurancesInfo from Public to Internal when source in Public empty"
  ) {
    when(carInfoToCatalogDataConverter.convert(?)).thenReturn(Some(CatalogData))

    val insurancePublic = GarageApiModel.InsuranceInfo
      .newBuilder()
      .addAllInsurances(
        List(
          // ОСАГО
          buildPublicInsurance(
            number = "123456789",
            serial = "PPP",
            startDate = Timestamps.fromMillis(now - 100.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 250.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO
          )
        ).asJava
      )
      .build()

    val cardWithInsurance =
      GarageApiModel.Card.newBuilder().setInsuranceInfo(insurancePublic).build()

    val insuranceInternal = GarageSchema.InsuranceInfo
      .newBuilder()
      .addAllInsurances(
        List(
          // ОСАГО
          buildInternalInsurance(
            number = "123456789",
            serial = "PPP",
            startDate = Timestamps.fromMillis(now - 100.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 250.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            insuranceSourceOpt = Some(InsuranceSource.RSA)
          )
        ).asJava
      )
      .build()

    val existingCard = Some(GarageSchema.GarageCard.newBuilder().setInsuranceInfo(insuranceInternal).build())

    import scala.jdk.CollectionConverters.ListHasAsScala
    val converted = converter.convertToInternalInsuranceInfoOpt(
      cardWithInsurance.getInsuranceInfo,
      existingCard.map(_.getInsuranceInfo),
      now,
      cardWithInsurance.hasInsuranceInfo
    )

    assert(
      converted.get.getInsurancesList.size() == 1
    )
    assert(
      converted.get.getInsurancesList.asScala.head.getSource == InsuranceSource.RSA
    )
  }

  test(
    "check add and convert InsurancesInfo from Public to Internal when source in Public empty"
  ) {
    when(carInfoToCatalogDataConverter.convert(?)).thenReturn(Some(CatalogData))

    val insurancePublic = GarageApiModel.InsuranceInfo
      .newBuilder()
      .addAllInsurances(
        List(
          // ОСАГО
          buildPublicInsurance(
            number = "123456789",
            serial = "PPP",
            startDate = Timestamps.fromMillis(now - 100.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 250.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO
          )
        ).asJava
      )
      .build()

    val cardWithInsurance =
      GarageApiModel.Card.newBuilder().setInsuranceInfo(insurancePublic).build()

    val existingCard = Some(GarageSchema.GarageCard.newBuilder().build())

    import scala.jdk.CollectionConverters.ListHasAsScala
    val converted = converter.convertToInternalInsuranceInfoOpt(
      cardWithInsurance.getInsuranceInfo,
      existingCard.map(_.getInsuranceInfo),
      now,
      cardWithInsurance.hasInsuranceInfo
    )

    assert(
      converted.get.getInsurancesList.size() == 1
    )
    assert(
      converted.get.getInsurancesList.asScala.head.getSource == InsuranceSource.MANUAL
    )
  }

  private def convertInsurancesInfo(
      resultAfterMerge: InsuranceInfo): mutable.Buffer[Assertion] = {
    when(carInfoToCatalogDataConverter.convert(?)).thenReturn(Some(CatalogData))

    val card = GarageApiModel.Card.newBuilder().setInsuranceInfo(InsurancesFromPublicCard).build()
    val existingCard = Some(GarageSchema.GarageCard.newBuilder().setInsuranceInfo(InsurancesFromInternalCard).build())
    import scala.jdk.CollectionConverters.ListHasAsScala
    val converted = converter.convertToInternalInsuranceInfoOpt(
      card.getInsuranceInfo,
      existingCard.map(_.getInsuranceInfo),
      now,
      card.hasInsuranceInfo
    )

    assert(
      converted.get.getInsurancesList.size() == resultAfterMerge.getInsurancesList.size()
    )
    for {
      insuranceConverted <- converted.get.getInsurancesList.asScala
    } yield {
      assert(
        resultAfterMerge.getInsurancesList.asScala.contains(insuranceConverted)
      )
    }
  }

  test("check getId in different insurances") {
    import InsuranceConverter.RichGarageSchemaInsurance

    val OSAGO = buildInternalInsurance(
      number = "12234",
      serial = "АА97 65",
      startDate = Timestamps.fromMillis(now - 240.day.toMillis),
      expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
      insuranceType = InsuranceType.OSAGO
    )

    val KASKO = buildInternalInsurance(
      number = "12234",
      serial = "АА97 65",
      startDate = Timestamps.fromMillis(now - 240.day.toMillis),
      expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
      insuranceType = InsuranceType.KASKO
    )

    val unknownInsurance = buildInternalInsurance(
      number = "12234",
      serial = "АА97 65",
      startDate = Timestamps.fromMillis(now - 240.day.toMillis),
      expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
      insuranceType = InsuranceType.UNKNOWN_INSURANCE
    )

    assert(
      InsuranceType.values().size == 4
    ) // Если появился новый тип страховки, не забудь обязательно определить для него PK в RichGarageSchemaInsurance.getId!

    assert(OSAGO.getId != KASKO.getId)
    assert(OSAGO.getId != OSAGO.getSerial + OSAGO.getNumber)
    assert(OSAGO.getId == convertToLatin(OSAGO.getSerial) + OSAGO.getNumber)
    assert(KASKO.getId == OSAGO.getNumber)
    assert(unknownInsurance.getId == convertToLatin(unknownInsurance.getSerial) + unknownInsurance.getNumber)
  }

  test("convert meta with current") {
    val cardTypeInfo = GarageApiModel.CardTypeInfo.newBuilder().setCardType(CardType.CURRENT_CAR).build()
    val current =
      Meta
        .newBuilder()
        .setCreated(Timestamps.fromMillis(now - 1.day.toMillis))
        .setStatus(GarageCard.Status.DELETED)
        .setCardTypeInfo(
          CardTypeInfo
            .newBuilder()
            .addHistory(
              ChangeEvent
                .newBuilder()
                .setState(
                  Meta.CardTypeInfo.State
                    .newBuilder()
                    .setCardType(CardType.CURRENT_CAR)
                    .setSource(ChangeTypeSource.MANUAL)
                )
            )
            .addHistory(
              ChangeEvent
                .newBuilder()
                .setState(
                  Meta.CardTypeInfo.State
                    .newBuilder()
                    .setCardType(CardType.CURRENT_CAR)
                    .setSource(ChangeTypeSource.MANUAL)
                )
                .setDate(Timestamps.fromMillis(System.currentTimeMillis()))
            )
        )
        .build()
    val converted = converter.convertMeta(cardTypeInfo, Some(current), now)

    assert(converted == current)
  }

  test("convert meta without current (with card type)") {
    val current =
      Meta
        .newBuilder()
        .setCreated(Timestamps.fromMillis(now))
        .setStatus(GarageCard.Status.ACTIVE)
        .setCardTypeInfo(
          CardTypeInfo
            .newBuilder()
            .setCurrentState(CardTypeInfo.State.newBuilder().setCardType(CardType.EX_CAR))
            .addHistory(
              ChangeEvent
                .newBuilder()
                .setState(CardTypeInfo.State.newBuilder().setCardType(CardType.EX_CAR))
                .setDate(Timestamps.fromMillis(now))
            )
        )
        .build()

    val cardTypeInfo = GarageApiModel.CardTypeInfo.newBuilder().setCardType(CardType.EX_CAR).build()
    val converted = converter.convertMeta(cardTypeInfo, None, now)

    assert(converted == current)
  }

  test("convert meta without current (without card type)") {
    val current =
      Meta
        .newBuilder()
        .setCreated(Timestamps.fromMillis(now))
        .setStatus(GarageCard.Status.ACTIVE)
        .setCardTypeInfo(
          CardTypeInfo
            .newBuilder()
            .setCurrentState(CardTypeInfo.State.newBuilder().setCardType(CardType.CURRENT_CAR))
            .addHistory(
              ChangeEvent
                .newBuilder()
                .setState(CardTypeInfo.State.newBuilder().setCardType(CardType.CURRENT_CAR))
                .setDate(Timestamps.fromMillis(now))
            )
        )
        .build()

    val cardTypeInfo = GarageApiModel.CardTypeInfo.newBuilder().build()
    val converted = converter.convertMeta(cardTypeInfo, None, now)

    assert(converted == current)
  }

  test("convert vehicle without existing current card") {
    when(carInfoToCatalogDataConverter.convert(?)).thenReturn(Some(CatalogData))
    val converted = converter.convertVehicle(DefaultCard.getVehicleInfo, None, true, now)

    assert(converted == DefaultInternalCard.getVehicleInfo)
  }

  test("convert ttx with empty fields") {
    val carInfo = CarInfo.newBuilder().build()

    val converted = converter.convertTtx(carInfo, 0, "")

    assert(converted == CommonTtx.newBuilder().build())
  }

  test("convert invalid ttx") {
    val carInfo = {
      CarInfo
        .newBuilder()
        .setDrive("4X4")
        .build()
    }

    intercept[IllegalArgumentException] {
      converter.convertTtx(carInfo, 2015, "c0c0c0")
    }
  }

  test("convert mileage history if current mileage already in history") {
    val state = GarageApiModel.Vehicle.State
      .newBuilder()
      .setMileage(GarageApiModel.Vehicle.Mileage.newBuilder().setValue(2000).setDate(Timestamps.fromMillis(now)))
      .build()
    val history = List(
      Mileage.newBuilder().setValue(1000).setDate(Timestamps.fromMillis(now - 30.days.toMillis)).build(),
      Mileage.newBuilder().setValue(2000).setDate(Timestamps.fromMillis(now - 10.days.toMillis)).build()
    )

    val converted = converter.convertMileageHistory(state, history, now)

    assert(converted.size == 2)
    assert(converted(0) == history(0))
    assert(converted(1) == history(1))
  }

  test("convert mileage history if current mileage does not in history") {
    val state = GarageApiModel.Vehicle.State
      .newBuilder()
      .setMileage(GarageApiModel.Vehicle.Mileage.newBuilder().setValue(2000).setDate(Timestamps.fromMillis(now)))
      .build()
    val history = List(
      Mileage.newBuilder().setValue(1500).setDate(Timestamps.fromMillis(now - 10.days.toMillis)).build(),
      Mileage.newBuilder().setValue(1000).setDate(Timestamps.fromMillis(now - 30.days.toMillis)).build()
    )

    val converted = converter.convertMileageHistory(state, history, now)

    assert(converted.size == 3)
    assert(converted(2).getValue == 2000)
    assert(converted(2).getDate.toMillis == now)

    assert(converted(0) == history(1))
    assert(converted(1) == history(0))
  }

  test("convert mileage history if old history is empty") {
    val state = GarageApiModel.Vehicle.State
      .newBuilder()
      .setMileage(GarageApiModel.Vehicle.Mileage.newBuilder().setValue(1000).setDate(Timestamps.fromMillis(now)))
      .build()

    val converted = converter.convertMileageHistory(state, List.empty, 123)

    assert(converted.size == 1)
    assert(converted(0).getValue == 1000)
    assert(converted(0).getDate.toMillis == now)
  }

  test("convert documents with invalid vin") {
    val documents = {
      Documents
        .newBuilder()
        .setVin("12")
        .setLicensePlate("В188СТ716")
        .build()
    }

    intercept[IllegalArgumentException] {
      converter.convertDocuments(documents, None, true)
    }
  }

  test("convert documents with invalid license plate") {
    val documents = {
      Documents
        .newBuilder()
        .setVin("WBAFG81000LJ37566")
        .setLicensePlate("12")
        .build()
    }

    intercept[IllegalArgumentException] {
      converter.convertDocuments(documents, None, true)
    }
  }

  test("convert equipments") {
    val equipments = Map(
      "a" -> true,
      "b" -> false,
      "c" -> true
    )

    val carInfo = {
      val builder = CarInfo.newBuilder()
      equipments.foreach { case (code, equipped) =>
        builder.putEquipment(code, equipped)
      }
      builder.build()
    }

    val converted = converter.convertEquipments(carInfo)

    assert(converted.size == 2)
    assert(converted.find(_.getVerbaCode == "a").get.getEquipped.getValue)
    assert(converted.find(_.getVerbaCode == "c").get.getEquipped.getValue)
  }

  test("convert images") {
    val photo1 = buildPublicImage(PhotoInfoId(1, "name-1", "autoru-carfax"))
    val photo2 = buildPublicImage(PhotoInfoId(5, "name-2", "autoru"))

    val existedPhoto2 = PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name-2").setNamespace("autoru").setGroupId(5))
      .setExternalPhotoUrl("abc")
      .setMeta(
        MetaData
          .newBuilder()
          .addRecognizedLicensePlates(
            RecognizedLicensePlates.newBuilder().setLicensePlate("T700TT62").build()
          )
      )
      .build()

    val existedPhoto3 = PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name-3").setNamespace("autoru").setGroupId(5))
      .setMeta(
        MetaData
          .newBuilder()
          .addRecognizedLicensePlates(
            RecognizedLicensePlates.newBuilder().setLicensePlate("T700TT62").build()
          )
      )
      .build()

    val targetPhoto1 = PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name-1").setNamespace("autoru-carfax").setGroupId(1))
      .build()

    val targetPhoto2 = existedPhoto2

    val targetPhoto3 = existedPhoto3.toBuilder.setIsDeleted(true).build()

    val converted = converter.convertImages(List(photo1, photo2), List(existedPhoto2, existedPhoto3))

    assert(converted.size == 3)
    assert(converted(0) == targetPhoto1)
    assert(converted(1) == targetPhoto2)
    assert(converted(2) == targetPhoto3)
  }

  test("convert proven owner") {
    val photo = buildPublicImage(PhotoInfoId(1, "name-1", "autoru-carfax"))

    val targetPhoto = PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name-1").setNamespace("autoru-carfax").setGroupId(1))
      .build()

    val vehicle = GarageApiModel.Card
      .newBuilder()
      .setProvenOwnerState(
        GarageApiModel.ProvenOwnerState
          .newBuilder()
          .setDocumentsPhotos(
            GarageApiModel.ProvenOwnerState.DocumentsPhotos
              .newBuilder()
              .setPhotos(
                GarageApiModel.ProvenOwnerState.DocumentsPhotos.Photos
                  .newBuilder()
                  .setDrivingLicense(photo)
                  .setStsBack(photo)
                  .setStsFront(photo)
              )
          )
      )
      .build()

    val provenOwnerState =
      ProvenOwnerState
        .newBuilder()
        .setDocumentsPhotos(
          DocumentsPhotos
            .newBuilder()
            .setDrivingLicense(targetPhoto)
            .setStsBack(targetPhoto)
            .setStsFront(targetPhoto)
            .setUploadedAt(Timestamps.fromMillis(now))
        )
        .setVerdict(Verdict.NEW)
        .build()

    val converted = converter.convertProvenOwnerState(
      vehicle,
      Some(
        GarageCard
          .newBuilder()
          .build()
      ),
      now
    )

    assert(converted.head == provenOwnerState)
  }

  test("convert proven owner when already has state and docs the same") {
    val photo = buildPublicImage(PhotoInfoId(1, "name-1", "autoru-carfax"))

    val targetPhoto = PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name-1").setNamespace("autoru-carfax").setGroupId(1))
      .build()

    val vehicle = GarageApiModel.Card
      .newBuilder()
      .setProvenOwnerState(
        GarageApiModel.ProvenOwnerState
          .newBuilder()
          .setDocumentsPhotos(
            GarageApiModel.ProvenOwnerState.DocumentsPhotos
              .newBuilder()
              .setPhotos(
                GarageApiModel.ProvenOwnerState.DocumentsPhotos.Photos
                  .newBuilder()
                  .setDrivingLicense(photo)
                  .setStsBack(photo)
                  .setStsFront(photo)
              )
          )
      )
      .build()

    val provenOwnerState =
      ProvenOwnerState
        .newBuilder()
        .setDocumentsPhotos(
          DocumentsPhotos
            .newBuilder()
            .setDrivingLicense(targetPhoto)
            .setStsBack(targetPhoto)
            .setStsFront(targetPhoto)
            .setUploadedAt(Timestamps.fromMillis(now))
        )
        .setPromocode(Promocode.newBuilder().setId("id").setIssuedAt(Timestamps.fromMillis(now)))
        .setVerdict(Verdict.PROVEN_OWNER_OK)
        .build()

    val converted = converter.convertProvenOwnerState(
      vehicle,
      Some(
        GarageCard
          .newBuilder()
          .setProvenOwnerState(provenOwnerState)
          .build()
      ),
      now
    )
    assert(converted.head == provenOwnerState)
  }

  test("convert proven owner when already has state but docs not the same") {
    val photo = buildPublicImage(PhotoInfoId(1, "name-2", "autoru-carfax"))

    val targetPhoto = PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name-1").setNamespace("autoru-carfax").setGroupId(1))
      .build()

    val targetPhoto2 = PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name-2").setNamespace("autoru-carfax").setGroupId(1))
      .build()

    val vehicle = GarageApiModel.Card
      .newBuilder()
      .setProvenOwnerState(
        GarageApiModel.ProvenOwnerState
          .newBuilder()
          .setDocumentsPhotos(
            GarageApiModel.ProvenOwnerState.DocumentsPhotos
              .newBuilder()
              .setPhotos(
                GarageApiModel.ProvenOwnerState.DocumentsPhotos.Photos
                  .newBuilder()
                  .setDrivingLicense(photo)
                  .setStsBack(photo)
                  .setStsFront(photo)
              )
          )
      )
      .build()

    val provenOwnerState =
      ProvenOwnerState
        .newBuilder()
        .setDocumentsPhotos(
          DocumentsPhotos
            .newBuilder()
            .setDrivingLicense(targetPhoto)
            .setStsBack(targetPhoto)
            .setStsFront(targetPhoto)
            .setUploadedAt(Timestamps.fromMillis(now))
        )
        .setVerdict(Verdict.PROVEN_OWNER_OK)
        .build()

    val converted = converter.convertProvenOwnerState(
      vehicle,
      Some(
        GarageCard
          .newBuilder()
          .setProvenOwnerState(provenOwnerState)
          .build()
      ),
      now
    )

    assert(
      converted.head == provenOwnerState.toBuilder
        .setDocumentsPhotos(
          DocumentsPhotos
            .newBuilder()
            .setDrivingLicense(targetPhoto2)
            .setStsBack(targetPhoto2)
            .setStsFront(targetPhoto2)
            .setUploadedAt(Timestamps.fromMillis(now))
        )
        .setVerdict(Verdict.NEW)
        .build()
    )
  }

  test("convert existed proven owner when no new photos") {

    val targetPhoto = PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name-1").setNamespace("autoru-carfax").setGroupId(1))
      .build()

    val vehicle = GarageApiModel.Card
      .newBuilder()
      .build()

    val provenOwnerState =
      ProvenOwnerState
        .newBuilder()
        .setDocumentsPhotos(
          DocumentsPhotos
            .newBuilder()
            .setDrivingLicense(targetPhoto)
            .setStsBack(targetPhoto)
            .setStsFront(targetPhoto)
            .setUploadedAt(Timestamps.fromMillis(now))
        )
        .setVerdict(Verdict.PROVEN_OWNER_OK)
        .build()

    val converted = converter.convertProvenOwnerState(
      vehicle,
      Some(
        GarageCard
          .newBuilder()
          .setProvenOwnerState(
            provenOwnerState
          )
          .build()
      ),
      now
    )

    assert(converted.head == provenOwnerState)
  }

  test("convert proven owner when existed is not defined") {
    val photo = buildPublicImage(PhotoInfoId(1, "name-1", "autoru-carfax"))

    val targetPhoto = PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name-1").setNamespace("autoru-carfax").setGroupId(1))
      .build()

    val vehicle = GarageApiModel.Card
      .newBuilder()
      .setProvenOwnerState(
        GarageApiModel.ProvenOwnerState
          .newBuilder()
          .setDocumentsPhotos(
            GarageApiModel.ProvenOwnerState.DocumentsPhotos
              .newBuilder()
              .setPhotos(
                GarageApiModel.ProvenOwnerState.DocumentsPhotos.Photos
                  .newBuilder()
                  .setDrivingLicense(photo)
                  .setStsBack(photo)
                  .setStsFront(photo)
              )
          )
      )
      .build()

    val provenOwnerState =
      ProvenOwnerState
        .newBuilder()
        .setDocumentsPhotos(
          DocumentsPhotos
            .newBuilder()
            .setDrivingLicense(targetPhoto)
            .setStsBack(targetPhoto)
            .setStsFront(targetPhoto)
            .setUploadedAt(Timestamps.fromMillis(now))
        )
        .setVerdict(Verdict.NEW)
        .build()

    val converted = converter.convertProvenOwnerState(
      vehicle,
      None,
      now
    )

    assert(converted.head == provenOwnerState)
  }

  private def buildPublicImage(id: PhotoInfoId): Photo = {
    val mdsInfo = MdsPhotoInfo.newBuilder().setGroupId(id.groupId).setName(id.name).setNamespace(id.namespace).build()
    Photo.newBuilder().setMdsPhotoInfo(mdsInfo).build()
  }

  private val insuranceStartDate = Timestamps.fromMillis(now - 240.day.toMillis)
  private val insuranceExpirationDate = Timestamps.fromMillis(now + 125.day.toMillis)
  private val insuranceUpdateTimestamp = Timestamps.fromMillis(now)

  private lazy val DefaultCard = {
    val builder = GarageApiModel.Card.newBuilder()

    builder.getInsuranceInfoBuilder.addInsurances(
      GarageApiModel.Insurance
        .newBuilder()
        .setFrom(insuranceStartDate)
        .setTo(insuranceExpirationDate)
        .setInsuranceType(InsuranceType.OSAGO)
        .setStatus(VinReportModel.InsuranceStatus.ACTIVE)
        .setNumber("12345679")
        .setSerial("97 65")
        .setUpdateTimestamp(insuranceUpdateTimestamp)
        .setCompany(
          GarageApiModel.Company
            .newBuilder()
            .setName("Рога и Копыта")
            .setPhoneNumber("+ 1 234 567 89 00")
            .build()
        )
        .build()
    )

    builder.getOfferInfoBuilder.setOfferId("1049777022-811fc")

    val vehicleBuilder = builder.getVehicleInfoBuilder
    vehicleBuilder.getCarInfoBuilder
      .setMark("BMW")
      .setModel("X5")
      .setHorsePower(150)
      .setSteeringWheel(CommonModel.SteeringWheel.LEFT)
      .setDrive("ALL_WHEEL_DRIVE")
      .setBodyType("ALLROAD_5_DOORS")
      .setEngineType("DIESEL")
      .setTransmission("AUTOMATIC")
      .setConfiguration(CatalogModel.Configuration.newBuilder().setDoorsCount(5))
      .setTechParam(CatalogModel.TechParam.newBuilder().setDisplacement(2100))
      .setSuperGenId(1)
      .setConfigurationId(2)
      .setTechParamId(3)
      .setComplectationId(4)

    vehicleBuilder.getDocumentsBuilder
      .setVin("WBAFG81000LJ37566")
      .setLicensePlate("В188СТ716")
      .setOwnersNumber(3)
      .setPtsOriginal(true)
      .setPts(PtsStatus.ORIGINAL)
      .setYear(2015)
      .setPurchaseDate(Date.newBuilder().setYear(2015).setMonth(6).setDay(12))

    vehicleBuilder.getStateBuilder.setMileage(
      GarageApiModel.Vehicle.Mileage.newBuilder().setValue(2000).setDate(Timestamps.fromMillis(now))
    )

    vehicleBuilder.getColorBuilder
      .setId("4A2197")
      .setName("Фиолетовый")

    vehicleBuilder.setSaleDate(Date.newBuilder().setYear(2016).setMonth(6).setDay(12))

    builder.setUserId("user:123")

    builder.getVehicleInfoBuilder.addVehicleImages(
      buildPublicImage(PhotoInfoId(1, "name-1", "autoru-carfax"))
    )

    builder.getVehicleInfoBuilder.getExteriorPanoramaBuilder
      .setPanorama(Panorama.newBuilder.setId("123"))

    builder.getVehicleInfoBuilder.getInteriorPanoramaBuilder
      .setPanorama(InteriorPanorama.newBuilder.setId("321"))

    builder.getProvenOwnerStateBuilder
      .setDocumentsPhotos(
        GarageApiModel.ProvenOwnerState.DocumentsPhotos
          .newBuilder()
          .setPhotos(
            Photos
              .newBuilder()
              .setStsBack(
                Photo
                  .newBuilder()
                  .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name").setNamespace("namespace").setGroupId(1))
              )
          )
          .setUploadedAt(Timestamps.fromMillis(now))
      )
    builder.build()
  }

  private lazy val DefaultInternalCard = {
    val builder = GarageCard.newBuilder()

    builder.getInsuranceInfoBuilder.addInsurances(
      GarageSchema.Insurance
        .newBuilder()
        .setFrom(insuranceStartDate)
        .setTo(insuranceExpirationDate)
        .setInsuranceType(InsuranceType.OSAGO)
        .setStatus(VinReportModel.InsuranceStatus.ACTIVE)
        .setNumber("12345679")
        .setSerial("97 65")
        .setUpdateTimestamp(insuranceUpdateTimestamp)
        .setIsDeleted(false)
        .setSource(InsuranceSource.MANUAL)
        .setCompany(
          GarageSchema.Company
            .newBuilder()
            .setName("Рога и Копыта")
            .setPhoneNumber("12345678900")
            .build()
        )
        .build()
    )

    builder.getOfferInfoBuilder.setOfferId("1049777022-811fc")

    val vehicleBuilder = builder.getVehicleInfoBuilder

    vehicleBuilder.getDocumentsBuilder
      .setVin("WBAFG81000LJ37566")
      .setLicensePlate("B188CT716")
      .setOwnersNumber(3)
      .setPtsOriginal(BoolValue.newBuilder().setValue(true))
      .setPurchaseDate(Timestamps.fromMillis(1434056400000L))

    vehicleBuilder.getDocumentsBuilder.setSaleDate(Timestamps.fromMillis(1465678800000L))

    vehicleBuilder.getTtxBuilder
      .setMark("BMW")
      .setModel("X5")
      .setYear(2015)
      .setPowerHp(150)
      .setDisplacement(2100)
      .setSteeringWheel(SteeringWheel.LEFT)
      .setGearType(GearType.ALL_WHEEL_DRIVE)
      .setBodyType(BodyType.ALLROAD_5_DOORS)
      .setTransmission(Transmission.AUTOMATIC)
      .setEngineType(EngineType.DIESEL)
      .setDoorsCount(5)
      .setColor(ColorInfo.newBuilder().setRgbCode("9966cc").setName("Фиолетовый").setAutoruColorId("4A2197"))

    vehicleBuilder.getCatalogBuilder
      .setSuperGenId(1)
      .setConfigurationId(2)
      .setTechParamId(3)
      .setComplectationId(4)

    vehicleBuilder.addMileageHistory(
      Mileage.newBuilder().setDate(Timestamps.fromMillis(now)).setValue(2000)
    )

    vehicleBuilder.addImages(
      PhotoInfo
        .newBuilder()
        .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name-1").setNamespace("autoru-carfax").setGroupId(1))
        .build()
    )

    vehicleBuilder.getExteriorPanoramaBuilder
      .setPanorama(Panorama.newBuilder.setId("123"))

    vehicleBuilder.getInteriorPanoramaBuilder
      .setPanorama(InteriorPanorama.newBuilder.setId("321"))

    builder.setMeta(
      Meta
        .newBuilder()
        .setCreated(Timestamps.fromMillis(now))
        .setStatus(GarageCard.Status.ACTIVE)
        .setCardTypeInfo(
          CardTypeInfo
            .newBuilder()
            .setCurrentState(CardTypeInfo.State.newBuilder().setCardType(CardType.CURRENT_CAR))
            .addHistory(
              ChangeEvent
                .newBuilder()
                .setState(CardTypeInfo.State.newBuilder().setCardType(CardType.CURRENT_CAR))
                .setDate(Timestamps.fromMillis(now))
            )
        )
    )

    builder.getSourceBuilder
      .setAddedByIdentifier(IdentifierType.LICENSE_PLATE)
      .setManuallyAdded(true)
      .setSource(GarageSchema.Source.AUTORU)
      .setUserId("user:123")

    builder.getProvenOwnerStateBuilder
      .setDocumentsPhotos(
        DocumentsPhotos
          .newBuilder()
          .setStsBack(
            PhotoInfo
              .newBuilder()
              .setMdsPhotoInfo(MdsPhotoInfo.newBuilder().setName("name").setNamespace("namespace").setGroupId(1))
          )
          .setDrivingLicense(PhotoInfo.newBuilder().setMdsPhotoInfo(MdsPhotoInfo.newBuilder()))
          .setStsFront(PhotoInfo.newBuilder().setMdsPhotoInfo(MdsPhotoInfo.newBuilder()))
          .setUploadedAt(Timestamps.fromMillis(now))
      )
      .setVerdict(Verdict.NEW)

    builder.build()
  }

  private lazy val CatalogData = AutoruCatalogData
    .newBuilder()
    .setSuperGenId(1)
    .setConfigurationId(2)
    .setTechParamId(3)
    .setComplectationId(4)
    .build()

  private lazy val InsurancesFromPublicCard: GarageApiModel.InsuranceInfo = {
    import scala.jdk.CollectionConverters.IterableHasAsJava
    GarageApiModel.InsuranceInfo
      .newBuilder()
      .addAllInsurances(
        List(
          // ОСАГО
          buildPublicInsurance(
            number = "new12345679",
            serial = "127 445",
            startDate = Timestamps.fromMillis(now - 100.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 250.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO
          ), // Актуальная страховка ОСАГО
          buildPublicInsurance(
            number = "12345679",
            serial = "97 65",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO
          ), // Была Актуальная, а станет не актуальной страховка ОСАГО
          buildPublicInsurance(
            number = "0001",
            serial = "01 20",
            startDate = Timestamps.fromMillis(now - 1350.day.toMillis),
            expirationDate = Timestamps.fromMillis(now - 1235.day.toMillis), // 1 год без пересечения
            insuranceType = InsuranceType.OSAGO
          ), // Не актуальная страховка ОСАГО

          // Каско
          buildPublicInsurance(
            number = "каско111",
            serial = "10 02",
            startDate = Timestamps.fromMillis(now - 1828.day.toMillis), // Страховка на 2 года, день в день
            expirationDate = Timestamps.fromMillis(now - 734.day.toMillis),
            insuranceType = InsuranceType.KASKO
          ), // Актуальной должна стать страховка КАСКО
          buildPublicInsurance(
            number = "9999999",
            serial = "11 22",
            startDate = Timestamps.fromMillis(now - 3660.day.toMillis), // Страховка на 5 лет без пересечения
            expirationDate = Timestamps.fromMillis(now - 2570.day.toMillis),
            insuranceType = InsuranceType.KASKO
          ) // Не актуальная страховка КАСКО
        ).asJava
      )
      .build()
  }

  private lazy val InsurancesFromInternalCard: GarageSchema.InsuranceInfo = {
    import scala.jdk.CollectionConverters.IterableHasAsJava
    GarageSchema.InsuranceInfo
      .newBuilder()
      .addAllInsurances(
        List(
          // ОСАГО
          buildInternalInsurance(
            number = "12345679",
            serial = "97 65",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 125.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            insuranceSourceOpt = Some(InsuranceSource.RSA)
          ), // Актуальная страховка ОСАГО
          buildInternalInsurance(
            number = "0000",
            serial = "00 00",
            startDate = Timestamps.fromMillis(now - 975.day.toMillis),
            expirationDate = Timestamps.fromMillis(
              now - 860.day.toMillis
            ), // 2 года с пересечение на 5 дней с актуальной страховкой
            insuranceType = InsuranceType.OSAGO
          ), // Не актуальная страховка ОСАГО
          buildInternalInsurance(
            number = "0001",
            serial = "01 20",
            startDate = Timestamps.fromMillis(now - 1350.day.toMillis),
            expirationDate = Timestamps.fromMillis(now - 1235.day.toMillis), // 1 год без пересечения
            insuranceType = InsuranceType.OSAGO
          ), // Не актуальная страховка ОСАГО

          // Каско
          buildInternalInsurance(
            number = "9999999",
            serial = "11 22",
            startDate = Timestamps.fromMillis(now - 1098.day.toMillis), // Страховка на 3 года
            expirationDate = Timestamps.fromMillis(now - 4.day.toMillis),
            insuranceType = InsuranceType.KASKO
          ), // Акутальная страховка КАСКО, но вышел срок действия 4 дня назад
          buildInternalInsurance(
            number = "каско111",
            serial = "10 02",
            startDate = Timestamps.fromMillis(now - 1828.day.toMillis), // Страховка на 2 года, день в день
            expirationDate = Timestamps.fromMillis(now - 734.day.toMillis),
            insuranceType = InsuranceType.KASKO
          ), // Не актуальная страховка КАСКО
          buildInternalInsurance(
            number = "00-9999999",
            serial = "88-22",
            startDate = Timestamps.fromMillis(
              now - 3660.day.toMillis
            ), // Страховка на 5 лет без пересечения
            expirationDate = Timestamps.fromMillis(now - 2570.day.toMillis),
            insuranceType = InsuranceType.KASKO
          ), // Не актуальная страховка КАСКО

          // Не известного типа страховка
          buildInternalInsurance(
            number = "water-000",
            serial = "44 32",
            startDate = Timestamps.fromMillis(now - 952.day.toMillis), // Использовалась 2 года назад
            expirationDate = Timestamps.fromMillis(now + 770.day.toMillis), // на пол года
            insuranceType = InsuranceType.UNKNOWN_INSURANCE
          ), // Актуальная страховка неизвестного типа (от утопления машины, например)
          buildInternalInsurance(
            number = "ground-109",
            serial = "566 33",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 125.day.toMillis),
            insuranceType = InsuranceType.UNKNOWN_INSURANCE
          )
        ).asJava
      )
      .build()
  }

  private lazy val InsurancesFromInternalAfterMergePublicCard: GarageSchema.InsuranceInfo = {
    import scala.jdk.CollectionConverters.IterableHasAsJava
    GarageSchema.InsuranceInfo
      .newBuilder()
      .addAllInsurances(
        List(
          // ОСАГО
          buildInternalInsurance(
            number = "new12345679",
            serial = "127 445",
            startDate = Timestamps.fromMillis(now - 100.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 250.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            insuranceSourceOpt = Some(InsuranceSource.MANUAL)
          ), // Стала актуальной страховка ОСАГО
          buildInternalInsurance(
            number = "12345679",
            serial = "97 65",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 150.day.toMillis), // Страховка на 1 год
            insuranceType = InsuranceType.OSAGO,
            insuranceSourceOpt = Some(InsuranceSource.RSA),
            isEditedOpt = Some(true)
          ), // не актуальная страховка ОСАГО
          buildInternalInsurance(
            number = "0000",
            serial = "00 00",
            startDate = Timestamps.fromMillis(now - 975.day.toMillis),
            expirationDate = Timestamps.fromMillis(
              now - 860.day.toMillis
            ), // 2 года с пересечение на 5 дней с актуальной страховкой
            insuranceType = InsuranceType.OSAGO,
            isDeleted = true
          ), // Не актуальная страховка ОСАГО
          buildInternalInsurance(
            number = "0001",
            serial = "01 20",
            startDate = Timestamps.fromMillis(now - 1350.day.toMillis),
            expirationDate = Timestamps.fromMillis(now - 1235.day.toMillis), // 1 год без пересечения
            insuranceType = InsuranceType.OSAGO
          ), // Не актуальная страховка ОСАГО

          // Каско
          buildInternalInsurance(
            number = "9999999",
            serial = "11 22",
            startDate = Timestamps.fromMillis(now - 3660.day.toMillis), // Страховка на 5 лет без пересечения
            expirationDate = Timestamps.fromMillis(now - 2570.day.toMillis),
            insuranceType = InsuranceType.KASKO,
            isEditedOpt = Some(true)
          ), // Акутальная страховка КАСКО, но вышел срок действия 4 дня назад
          buildInternalInsurance(
            number = "каско111",
            serial = "10 02",
            startDate = Timestamps.fromMillis(now - 1828.day.toMillis), // Страховка на 2 года, день в день
            expirationDate = Timestamps.fromMillis(now - 734.day.toMillis),
            insuranceType = InsuranceType.KASKO
          ), // Не актуальная страховка КАСКО
          buildInternalInsurance(
            number = "00-9999999",
            serial = "88-22",
            startDate = Timestamps.fromMillis(
              now - 3660.day.toMillis
            ), // Страховка на 5 лет без пересечения
            expirationDate = Timestamps.fromMillis(now - 2570.day.toMillis),
            insuranceType = InsuranceType.KASKO,
            isDeleted = true
          ), // Не актуальная страховка КАСКО

          // Не известного типа страховка
          buildInternalInsurance(
            number = "water-000",
            serial = "44 32",
            startDate = Timestamps.fromMillis(now - 952.day.toMillis), // Использовалась 2 года назад
            expirationDate = Timestamps.fromMillis(now + 770.day.toMillis), // на пол года
            insuranceType = InsuranceType.UNKNOWN_INSURANCE,
            isDeleted = true
          ), // Актуальная страховка неизвестного типа (от утопления машины, например)
          buildInternalInsurance(
            number = "ground-109",
            serial = "566 33",
            startDate = Timestamps.fromMillis(now - 240.day.toMillis),
            expirationDate = Timestamps.fromMillis(now + 125.day.toMillis),
            insuranceType = InsuranceType.UNKNOWN_INSURANCE,
            isDeleted = true
          )
        ).sortBy(insurance => (insurance.getInsuranceType.getNumber, insurance.getFrom.getSeconds)).asJava
      )
      .build()
  }

  private lazy val InsurancesFromInternalAfterMergeWithoutDeletePublicCard: GarageSchema.InsuranceInfo = {
    val insuranceInfoB = InsurancesFromInternalAfterMergePublicCard.toBuilder
    val insurances = insuranceInfoB.getInsurancesBuilderList.asScala.map { insuranceB =>
      insuranceB.clearIsDeleted
      insuranceB.build()
    }
    insuranceInfoB.clearInsurances().addAllInsurances(insurances.asJava).build
  }

  private def buildInternalInsurance(
      number: String,
      serial: String,
      startDate: Timestamp,
      expirationDate: Timestamp,
      insuranceType: VinReportModel.InsuranceType,
      insuranceStatus: VinReportModel.InsuranceStatus = VinReportModel.InsuranceStatus.ACTIVE,
      companyName: String = "Рога и Копыта",
      phoneNumber: String = "12345678900",
      isDeleted: Boolean = false,
      updateTimestamp: Timestamp = Timestamps.fromMillis(now),
      insuranceSourceOpt: Option[InsuranceSource] = None,
      isEditedOpt: Option[Boolean] = None,
      attachments: List[GarageSchema.Insurance.Attachment] = List.empty): GarageSchema.Insurance = {
    val insuranceB = GarageSchema.Insurance
      .newBuilder()
      .setNumber(number)
      .setSerial(serial)
      .setFrom(startDate)
      .setTo(expirationDate)
      .setInsuranceType(insuranceType)
      .setStatus(insuranceStatus)
      .setCompany(
        GarageSchema.Company
          .newBuilder()
          .setName(companyName)
          .setPhoneNumber(phoneNumber)
          .build()
      )
      .setIsDeleted(isDeleted)
      .setUpdateTimestamp(updateTimestamp)
    isEditedOpt.foreach(insuranceB.setIsEdited)
    insuranceSourceOpt.foreach(insuranceB.setSource)
    attachments.foreach(insuranceB.addAttachment)
    insuranceB.build()
  }

  private def buildPublicInsurance(
      number: String,
      serial: String,
      startDate: Timestamp,
      expirationDate: Timestamp,
      insuranceType: InsuranceType,
      insuranceStatus: VinReportModel.InsuranceStatus = VinReportModel.InsuranceStatus.ACTIVE,
      companyName: String = "Рога и Копыта",
      phoneNumber: String = "+ 1 234 567 89 00",
      updateTimestamp: Timestamp = Timestamps.fromMillis(now),
      insuranceSourceOpt: Option[InsuranceSource] = None,
      attachmentOpt: Option[GarageApiModel.Insurance.Attachment] = None): GarageApiModel.Insurance = {
    val insuranceB = GarageApiModel.Insurance
      .newBuilder()
      .setNumber(number)
      .setSerial(serial)
      .setFrom(startDate)
      .setTo(expirationDate)
      .setInsuranceType(insuranceType)
      .setStatus(insuranceStatus)
      .setCompany(
        GarageApiModel.Company
          .newBuilder()
          .setName(companyName)
          .setPhoneNumber(phoneNumber)
          .build()
      )
      .setUpdateTimestamp(updateTimestamp)
    attachmentOpt.foreach(insuranceB.setAttachment)
    insuranceSourceOpt.foreach(insuranceB.setSource)
    insuranceB.build()
  }

  private def buildFile(url: String, s3FileInfo: Common.S3FileInfo): Common.File = {
    Common.File
      .newBuilder()
      .setUrl(url)
      .setS3FileInfo(s3FileInfo)
      .build()
  }

  private def buildS3FileInfo(name: String, namespace: String, mimeType: Common.MimeType): Common.S3FileInfo = {
    S3FileInfo.newBuilder().setName(name).setNamespace(namespace).setMimeType(mimeType).build()
  }

  private def buildPhoto(mdsPhotoInfo: Mds.MdsPhotoInfo): Common.Photo = {
    Common.Photo
      .newBuilder()
      .setMdsPhotoInfo(mdsPhotoInfo)
      .build()
  }

  private def buildPublicAttachment(
      file: Common.File): GarageApiModel.Insurance.Attachment = {
    val aB = GarageApiModel.Insurance.Attachment
      .newBuilder()
    aB.setFile(
      file
    ).build()
  }

  private def buildInternalAttachment(
      fileInfo: Common.S3FileInfo): GarageSchema.Insurance.Attachment = {
    val aB = GarageSchema.Insurance.Attachment
      .newBuilder()
    aB.setFileInfo(
      fileInfo
    ).build()
  }

}
