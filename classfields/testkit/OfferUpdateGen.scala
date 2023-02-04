package ru.yandex.vertis.general.gost.model.testkit

import general.bonsai.category_model.Category
import general.gost.offer_model.YaMarketInfo
import ru.yandex.vertis.general.common.model.delivery.DeliveryInfo
import ru.yandex.vertis.general.gost.model.Offer._
import ru.yandex.vertis.general.gost.model.{Photo, Vendor}
import ru.yandex.vertis.general.gost.model.attributes.{AttributeValue, Attributes}
import ru.yandex.vertis.general.gost.model.testkit.ConditionGen.anyCondition
import ru.yandex.vertis.general.gost.model.testkit.OfferGen._
import ru.yandex.vertis.general.gost.model.{OfferUpdate, Price, SellingAddress, WayToContact}
import zio.random.Random
import zio.test.{Gen, Sized}

object OfferUpdateGen {

  def anyOfferUpdate(
      title: Gen[Random with Sized, String] = Gen.anyString,
      description: Gen[Random with Sized, String] = Gen.anyString,
      category: Gen[Random with Sized, Category] = predefCategory,
      attributes: Category => Gen[Random with Sized, Attributes[AttributeValue]] = AttributeGen.ofCategory,
      photos: Gen[Random with Sized, Seq[Photo]] = anyPhotos,
      video: Gen[Random with Sized, Option[Video]] = Gen.option(anyVideo),
      price: Gen[Random, Price] = anyPrice,
      addresses: Gen[Random with Sized, Seq[SellingAddress]] = anyAddresses,
      contacts: Gen[Random with Sized, Seq[Contact]] = anyContacts,
      preferredWayToContact: Gen[Random, WayToContact] = anyWayToContact,
      isPhoneRedirectEnabled: Gen[Random, Option[Boolean]] = Gen.boolean.map(Option(_)),
      condition: Category => Gen[Random, Option[Condition]] = _ => anyCondition,
      deliveryInfo: Gen[Random, Option[DeliveryInfo]] = Gen.option(anyDeliveryInfo),
      marketModelId: Gen[Random with Sized, Option[String]] = Gen.option(Gen.anyString),
      externalUrl: Gen[Random with Sized, Option[String]] = Gen.option(Gen.anyString),
      vendor: Gen[Random with Sized, Option[Vendor]] = anyVendor,
      shopInfo: Gen[Random with Sized, Option[ShopInfo]] = anyShopInfo,
      hideOnService: Gen[Random, Boolean] =
        Gen.elements(false, false, false, true)): Gen[Random with Sized, OfferUpdate] =
    for {
      title <- title
      description <- description
      category <- category
      attributes <- attributes(category)
      photos <- photos
      video <- video
      price <- price
      addresses <- addresses
      contacts <- contacts
      preferredWayToContact <- preferredWayToContact
      isPhoneRedirectEnabled <- isPhoneRedirectEnabled
      condition <- condition(category)
      deliveryInfo <- deliveryInfo
      marketModelId <- marketModelId
      externalUrl <- externalUrl
      vendor <- vendor
      shopInfo <- shopInfo
      hideOnService <- hideOnService
    } yield OfferUpdate(
      title = title,
      description = description,
      category = CategoryInfo(category.id, category.version),
      attributes = attributes,
      photos = photos,
      video = video,
      price = price,
      addresses = addresses,
      contacts = contacts,
      preferredWayToContact = preferredWayToContact,
      isPhoneRedirectEnabled = isPhoneRedirectEnabled,
      condition = condition,
      deliveryInfo = deliveryInfo,
      externalUrl = externalUrl,
      vendor = vendor,
      shopInfo = shopInfo,
      hideOnService = hideOnService,
      yaMarketInfo = Some(YaMarketInfo(sku = marketModelId))
    )

  val anyOfferUpdate: Gen[Random with Sized, OfferUpdate] = anyOfferUpdate()

  val feedOfferUpdate: Gen[Random with Sized, OfferUpdate] = anyOfferUpdate(condition = ConditionGen.ofCategory)

  def anyOfferUpdates(count: Int): Gen[Random with Sized, List[OfferUpdate]] =
    Gen.listOfN(count)(anyOfferUpdate)
}
