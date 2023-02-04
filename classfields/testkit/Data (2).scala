package ru.yandex.vertis.general.gost.model.testkit

import general.bonsai.attribute_model.AttributeDefinition
import general.bonsai.category_model.{Category, CategoryAttribute}
import general.bonsai.export_model.ExportedEntity
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.gost.model.SellingAddress.{AddressInfo, GeoPoint}
import ru.yandex.vertis.general.gost.model.Offer.{CategoryInfo, Contact}
import ru.yandex.vertis.general.gost.model.attributes.Attributes
import ru.yandex.vertis.general.gost.model.{DraftUpdate, Offer, OfferUpdate, Price, SellingAddress, WayToContact}

object Data {

  val validDraftUpdate: DraftUpdate = DraftUpdate(
    title = Some("Title"),
    description = "Description",
    categoryId = Some("bosonozhki_Sh6Wjv"),
    marketSkuId = None,
    attributes = Attributes.empty,
    photos = Seq.empty,
    video = None,
    price = Price.InCurrency(priceRur = 1000),
    addresses = Seq(
      SellingAddress(GeoPoint(55.791957, 37.557322), Some(AddressInfo("Москва")), None, None, None)
    ),
    contacts = Seq(
      Contact(phone = Some("88005553535"))
    ),
    preferredWayToContact = WayToContact.Chat,
    isPhoneRedirectEnabled = Some(false),
    condition = Some(Offer.Used),
    currentControlNum = 5,
    categoryPreset = None,
    deliveryInfo = None
  )

  val validOfferUpdate: OfferUpdate = OfferUpdate(
    title = "Title",
    description = "Description",
    category = CategoryInfo("bosonozhki_Sh6Wjv", version = 0),
    attributes = Attributes.empty,
    photos = Seq.empty,
    video = None,
    price = Price.InCurrency(priceRur = 1000),
    addresses = Seq(
      SellingAddress(GeoPoint(55.791957, 37.557322), Some(AddressInfo("Москва")), None, None, None)
    ),
    contacts = Seq(
      Contact(phone = Some("88005553535"))
    ),
    preferredWayToContact = WayToContact.Chat,
    condition = Some(Offer.Used),
    isPhoneRedirectEnabled = Some(false),
    deliveryInfo = None,
    externalUrl = None,
    vendor = None,
    shopInfo = None,
    hideOnService = false,
    yaMarketInfo = None
  )

  val mockAttribute1: CategoryAttribute = CategoryAttribute("attribute1", 50)
  val mockAttribute2: CategoryAttribute = CategoryAttribute("attribute2", 500)
  val mockAttribute3: CategoryAttribute = CategoryAttribute("attribute3", 5000)

  val mockCategory1: Category =
    Category(id = "category1", synonyms = Seq(), version = 100, attributes = Seq(mockAttribute1, mockAttribute2))

  val mockCategory2: Category =
    Category(id = "category2", synonyms = Seq(), attributes = Seq(mockAttribute3, mockAttribute2))

  val mockCategory3: Category =
    Category(
      id = "category3",
      synonyms = Seq(),
      attributes = Seq(mockAttribute1, mockAttribute2, mockAttribute3),
      ignoreCondition = true
    )

  val categories = Seq(mockCategory1, mockCategory2, mockCategory3)

  private val snapshotSource: Seq[ExportedEntity] = Seq(
    ExportedEntity(ExportedEntity.CatalogEntity.Category(mockCategory1)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(mockCategory2)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(mockCategory3)),
    ExportedEntity(
      ExportedEntity.CatalogEntity.Attribute(AttributeDefinition(mockAttribute1.attributeId, mockAttribute1.version))
    ),
    ExportedEntity(
      ExportedEntity.CatalogEntity.Attribute(AttributeDefinition(mockAttribute2.attributeId, mockAttribute2.version))
    ),
    ExportedEntity(
      ExportedEntity.CatalogEntity.Attribute(AttributeDefinition(mockAttribute3.attributeId, mockAttribute3.version))
    )
  )

  val bonsaiSnapshotMock: BonsaiSnapshot = BonsaiSnapshot(snapshotSource)
}
