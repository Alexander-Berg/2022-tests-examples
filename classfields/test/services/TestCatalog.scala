package ru.yandex.vertis.general.wizard.api.services

import general.bonsai.attribute_model.DictionarySettings.DictionaryValue
import general.bonsai.attribute_model.{AttributeDefinition, DictionarySettings}
import general.bonsai.category_model.{Category, CategoryAttribute, CategoryState}
import general.bonsai.export_model.ExportedEntity
import general.bonsai.lang_model.NameForms
import general.wizard.synonyms.CatalogSynonymsMapping
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.wizard.meta.resources.MetaPragmaticsSnapshot

object TestCatalog {

  val instruments: Category =
    Category(id = "instr", synonyms = Seq("инструмент", "инструменты"), state = CategoryState.DEFAULT)

  val musicInstruments: Category =
    Category(id = "music", synonyms = Seq("музыкальный инструмент"), state = CategoryState.DEFAULT)

  val guitars: Category =
    Category(id = "guitar", parentId = "music", synonyms = Seq("гитара", "гитару"), state = CategoryState.DEFAULT)

  val balalaika: Category =
    Category(id = "balalaika", parentId = "music", synonyms = Seq("балалайка"), state = CategoryState.DEFAULT)

  val guitarString: Category =
    Category(id = "guitar_strings", parentId = "guitar", synonyms = Seq("струны"), state = CategoryState.DEFAULT)
  val soft: DictionaryValue = DictionaryValue(key = "soft", synonyms = Seq("мягкую"))
  val materialDict: DictionarySettings = DictionarySettings(allowedValues = Seq(soft))

  val material: AttributeDefinition =
    AttributeDefinition(id = "material", synonyms = Seq("материал")).withDictionarySettings(materialDict)

  val siberian: DictionaryValue = DictionaryValue(key = "siberian", synonyms = Seq("сибирскую"))
  val catBreedDict: DictionarySettings = DictionarySettings(allowedValues = Seq(siberian))

  val husky: DictionaryValue = DictionaryValue(key = "husky", synonyms = Seq("хаски"), name = "хаски")
  val dogBreedDict: DictionarySettings = DictionarySettings(allowedValues = Seq(husky))

  val catBreeds: AttributeDefinition =
    AttributeDefinition(id = "catBreed", synonyms = Seq("порода")).withDictionarySettings(catBreedDict)
  val catBreedsDescription: CategoryAttribute = CategoryAttribute("catBreed")

  val dogBreeds: AttributeDefinition =
    AttributeDefinition(id = "dogBreed", synonyms = Seq("порода"), isAutonomous = true)
      .withDictionarySettings(dogBreedDict)
  val dogBreedsDescription: CategoryAttribute = CategoryAttribute("dogBreed")

  val male: DictionaryValue = DictionaryValue(key = "male", synonyms = Seq("мальчик"), name = "мальчик")
  val female: DictionaryValue = DictionaryValue(key = "female", synonyms = Seq("девочка"), name = "девочка")
  val sexDict: DictionarySettings = DictionarySettings(allowedValues = Seq(male, female))

  val sexes: AttributeDefinition =
    AttributeDefinition(id = "sex", synonyms = Seq("пол"), isAutonomous = false)
      .withDictionarySettings(sexDict)
  val sexDescription: CategoryAttribute = CategoryAttribute("sex")

  val extraAutonomousAttrValue: DictionaryValue = DictionaryValue(key = "extra", name = "какие то")
  val extraAutonomousAttrDict: DictionarySettings = DictionarySettings(allowedValues = Seq(extraAutonomousAttrValue))

  val extraAutonomousAttr: AttributeDefinition =
    AttributeDefinition(id = "extra", isAutonomous = true)
      .withDictionarySettings(extraAutonomousAttrDict)
  val extraAutonomousAttrDescription: CategoryAttribute = CategoryAttribute("extra")

  val small: DictionaryValue = DictionaryValue(key = "small", synonyms = Seq("маленькую"))
  val sizeDict: DictionarySettings = DictionarySettings(allowedValues = Seq(small))

  val size: AttributeDefinition =
    AttributeDefinition(id = "size", synonyms = Seq("размер")).withDictionarySettings(sizeDict)

  val catsSize: CategoryAttribute = CategoryAttribute("size", isHiddenOverride = Some(true))

  val black: DictionaryValue = DictionaryValue(key = "black", synonyms = Seq("черную"))
  val colorDict: DictionarySettings = DictionarySettings(allowedValues = Seq(black))

  val disabledColorAttr: AttributeDefinition =
    AttributeDefinition(id = "color", synonyms = Seq("цвет"), isHidden = true).withDictionarySettings(colorDict)

  val catsColor: CategoryAttribute = CategoryAttribute("color")

  val animals: Category =
    Category(
      id = "animals"
    )

  val cats: Category =
    Category(
      id = "cat",
      synonyms = Seq("кошку"),
      attributes = Seq(catBreedsDescription, catsSize, catsColor),
      nameForms = Some(NameForms(accusativePlural = "кошек", accusativeSingular = "кошку")),
      parentId = "animals"
    )

  val dogs: Category =
    Category(
      id = "dog",
      name = "Собаки",
      synonyms = Seq("собаку"),
      attributes = Seq(dogBreedsDescription, sexDescription, extraAutonomousAttrDescription),
      parentId = "animals"
    )

  val rabota: Category =
    Category(
      id = "rabota",
      name = "Работа",
      synonyms = Seq("работы"),
      state = CategoryState.DEFAULT
    )

  val phone: Category =
    Category(
      id = "phone",
      name = "Телефоны",
      synonyms = Seq("телефон"),
      state = CategoryState.DEFAULT
    )

  val iPhoneAttrValue: DictionaryValue =
    DictionaryValue(key = "proizvoditel-mobilnogo-telefona_454ghb", name = "iPhone")
  val iPhoneAttrDict: DictionarySettings = DictionarySettings(allowedValues = Seq(iPhoneAttrValue))

  val iPhoneAttr: AttributeDefinition =
    AttributeDefinition(id = "proizvoditel-mobilnogo-telefona_454ghb", isAutonomous = false)
      .withDictionarySettings(iPhoneAttrDict)

  private val snapshotSource: Seq[ExportedEntity] = Seq(
    ExportedEntity(ExportedEntity.CatalogEntity.Category(instruments)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(musicInstruments)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(guitars)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(balalaika)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(guitarString)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(size)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(material)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(catBreeds)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(dogBreeds)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(sexes)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(extraAutonomousAttr)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(iPhoneAttr)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(phone)),
    ExportedEntity(ExportedEntity.CatalogEntity.Attribute(disabledColorAttr)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(cats)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(animals)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(dogs)),
    ExportedEntity(ExportedEntity.CatalogEntity.Category(rabota))
  )

  val metaPragmaticsSnapshot: MetaPragmaticsSnapshot = MetaPragmaticsSnapshot(Seq.empty)

  val bonsaiSnapshot: BonsaiSnapshot = BonsaiSnapshot(snapshotSource)

  val catalogSynonymsMapping: CatalogSynonymsMapping = CatalogSynonymsMapping.defaultInstance
}
