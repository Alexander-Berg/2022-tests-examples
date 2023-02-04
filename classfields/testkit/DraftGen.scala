package ru.yandex.vertis.general.gost.model.testkit

import java.time.Instant
import ru.yandex.vertis.general.gost.model.Draft.{All, DraftId, Goods, Rabota}
import ru.yandex.vertis.general.gost.model.attributes.{AttributeConverter, Attributes}
import ru.yandex.vertis.general.gost.model.{Draft, Offer}
import zio.random.Random
import zio.test.{Gen, Sized}

object DraftGen {

  val anyDraftId: Gen[Random with Sized, DraftId] = {
    Gen.anyUUID.map(uuid => DraftId(uuid.toString)).noShrink
  }

  def anyDraft(
      id: Gen[Random with Sized, DraftId] = anyDraftId,
      offer: Gen[Random with Sized, Offer] = OfferGen.anyOffer): Gen[Random with Sized, Draft] =
    for {
      id <- id
      offer <- offer
      categoryPreset <- Gen.some(Gen.fromIterable(List(Goods, Rabota, All)))

    } yield Draft(
      id = id,
      publishedOfferId = None,
      title = Some(offer.title),
      description = offer.description,
      categoryId = Some(offer.category.id),
      marketSkuId = offer.yaMarketInfo.flatMap(_.sku),
      attributes = Attributes(offer.attributes.map(AttributeConverter.toDraftAttribute)),
      photos = offer.photos,
      video = offer.video,
      price = offer.price,
      addresses = offer.addresses,
      contacts = offer.contacts,
      preferredWayToContact = offer.preferredWayToContact,
      isPhoneRedirectEnabled = offer.isPhoneRedirectEnabled,
      condition = offer.condition,
      currentControlNum = 0,
      updated = Instant.ofEpochMilli(0),
      categoryPreset = categoryPreset,
      delivery = offer.delivery
    )

  val anyDraft: Gen[Random with Sized, Draft] = anyDraft().noShrink
}
