package ru.yandex.vertis.general.search.logic.test

import general.bonsai.attribute_model.AttributeDefinition.AttributeSettings
import general.bonsai.attribute_model.DictionarySettings.{DictionaryValue => ProtoDictionaryValue}
import general.bonsai.attribute_model.SearchFormSettings.SearchFormControl.{
  CHECKBOX,
  CHECKBOX_SET,
  RANGE,
  SELECT,
  SELECT_MULTIPLE,
  TAGS
}
import general.bonsai.attribute_model.{
  AttributeDefinition,
  BooleanSettings => ProtoBooleanSettings,
  DictionarySettings => ProtoDictionarySettings,
  SearchFormSettings,
  StringSettings => ProtoStringSettings
}
import general.bonsai.category_model.{Category, CategoryAttribute}
import general.bonsai.lang_model.{NameForms => ProtoNameForms}
import ru.yandex.vertis.general.bonsai.public.{BonsaiSnapshot, Constants}
import ru.yandex.vertis.general.search.constants.Constants._
import ru.yandex.vertis.general.search.logic.DefaultFilterCreator._
import ru.yandex.vertis.general.search.logic.{DefaultFilterCreator, FilterCreator, FilterKeyToDocumentFieldMapping}
import ru.yandex.vertis.general.search.model.FilterSettings.{DictionaryValue, NameForms}
import ru.yandex.vertis.general.search.model.SearchFilterControl._
import ru.yandex.vertis.general.search.model._
import common.zio.logging.Logging
import zio.test.Assertion._
import zio.test._
import zio.{Has, Ref, URIO, ZIO, ZRef}

object DefaultFilterCreatorTest extends DefaultRunnableSpec {

  private val DictionaryAttribute = AttributeDefinition(
    id = "dictionary_attribute_id",
    name = "dictionary_attribute_name",
    attributeSettings = AttributeSettings.DictionarySettings(
      ProtoDictionarySettings(
        allowedValues = Seq(
          ProtoDictionaryValue(
            key = "dictionary_value_key",
            name = "dictionary_value_name",
            description = "desc",
            nameForms = Some(ProtoNameForms(accusativePlural = "a", accusativeSingular = "b", genitivePlural = "c"))
          ),
          ProtoDictionaryValue(
            key = "dictionary_value_key2",
            name = "dictionary_value_name2",
            description = "desc",
            nameForms = Some(ProtoNameForms(accusativePlural = "a", accusativeSingular = "b", genitivePlural = "c"))
          ),
          ProtoDictionaryValue(
            key = "dictionary_value_key3",
            name = "dictionary_value_name3",
            description = "desc",
            nameForms = Some(ProtoNameForms(accusativePlural = "a", accusativeSingular = "b", genitivePlural = "c"))
          )
        ),
        allowCustomValues = true
      )
    ),
    searchFormSettings = Some(SearchFormSettings(CHECKBOX)),
    searchFormSettingsTouch = Some(SearchFormSettings(RANGE)),
    searchFormSettingsApp = Some(SearchFormSettings(TAGS))
  )

  def setCatalogState(
      categories: List[Category],
      attributes: List[AttributeDefinition]): URIO[Has[Ref[BonsaiSnapshot]], Unit] =
    for {
      ref <- ZIO.service[Ref[BonsaiSnapshot]]
      _ <- ref.set(BonsaiSnapshot(categories, attributes))
    } yield ()

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("FilterCreator")(
      testM("Do not create filters if foundCategoryIds, parameters, searchCategoryId are empty") {
        for {
          _ <- setCatalogState(List.empty, List.empty)
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq(),
            foundAttributes = Set.empty,
            foundDictionaryAttributeValues = Map.empty,
            parameters = Seq.empty,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(isEmpty)
      },
      testM("Not create filters from foundCategoryIds that were not requested") {
        val attribute = AttributeDefinition(
          id = "a",
          name = "name",
          attributeSettings = AttributeSettings.BooleanSettings(ProtoBooleanSettings())
        )
        val category = Category(id = "c", name = "c_name", attributes = List(CategoryAttribute(attribute.id)))
        for {
          _ <- setCatalogState(category :: Nil, attribute :: Nil)
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq("1", "2"),
            foundAttributes = Set.empty,
            foundDictionaryAttributeValues = Map.empty,
            parameters = Seq.empty,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(isEmpty)
      },
      testM("Create regular filter by found categories") {
        val attribute = AttributeDefinition(
          id = "attribute_id",
          name = "name",
          attributeSettings = AttributeSettings.DictionarySettings(
            ProtoDictionarySettings(
              allowedValues = Seq(
                ProtoDictionaryValue(
                  key = "key",
                  name = "name",
                  description = "desc",
                  nameForms =
                    Some(ProtoNameForms(accusativePlural = "a", accusativeSingular = "b", genitivePlural = "c"))
                )
              ),
              allowCustomValues = true
            )
          ),
          searchFormSettings = Some(SearchFormSettings(CHECKBOX)),
          searchFormSettingsTouch = Some(SearchFormSettings(RANGE)),
          searchFormSettingsApp = Some(SearchFormSettings(TAGS))
        )
        val category =
          Category(id = "c", name = "c_name", attributes = Seq(CategoryAttribute(attribute.id, attribute.version)))

        for {
          _ <- setCatalogState(category :: Nil, attribute :: Nil)
          expectedFilters = DefaultFilterCreator.FallbackFilters ++ List(
            SearchFilterControl(
              key = AttributeFieldPrefix + "attribute_id",
              name = "name",
              filterSettings = DictionarySettings(
                allowedValues = Seq(
                  DictionaryValue(
                    key = "key",
                    name = "name",
                    description = "desc",
                    NameForms(accusativePlural = "a", accusativeSingular = "b", genitivePlural = "c")
                  )
                ),
                allowCustomValues = true
              ),
              controlTypeWeb = Checkbox,
              controlTypeTouch = Range,
              controlTypeApp = Tags,
              incompatibleFilters = Nil,
              relatedAttributeId = Some("attribute_id")
            )
          )

          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq("c"),
            expectedFilters.map(filter => FilterKeyToDocumentFieldMapping.mapFilterKeyToFieldName(filter.key)).toSet,
            foundDictionaryAttributeValues = Map("attribute_id" -> Set("key")),
            parameters = Seq.empty,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(hasSameElements(expectedFilters))
      },
      testM("Ignore attributes hidden in category") {
        val attribute = AttributeDefinition(
          id = "attribute_id",
          name = "name",
          attributeSettings = AttributeSettings.DictionarySettings(
            ProtoDictionarySettings(
              allowedValues = Seq(
                ProtoDictionaryValue(key = "key", name = "name", description = "desc")
              ),
              allowCustomValues = true
            )
          ),
          searchFormSettings = Some(SearchFormSettings(CHECKBOX)),
          searchFormSettingsTouch = Some(SearchFormSettings(RANGE)),
          searchFormSettingsApp = Some(SearchFormSettings(TAGS))
        )
        val category =
          Category(
            id = "c",
            name = "c_name",
            attributes = Seq(CategoryAttribute(attribute.id, attribute.version, isHiddenOverride = Some(true)))
          )
        for {
          _ <- setCatalogState(category :: Nil, attribute :: Nil)
          expectedFilters = DefaultFilterCreator.FallbackFilters
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq("c"),
            foundAttributes =
              expectedFilters.map(filter => FilterKeyToDocumentFieldMapping.mapFilterKeyToFieldName(filter.key)).toSet,
            foundDictionaryAttributeValues = Map.empty,
            parameters = Seq.empty,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(hasSameElements(expectedFilters))
      },
      testM("Create regular filter by parameters") {
        val attribute = AttributeDefinition(
          id = "attribute_id",
          name = "name",
          attributeSettings = AttributeSettings.DictionarySettings(
            ProtoDictionarySettings(
              allowedValues = Seq(
                ProtoDictionaryValue(
                  key = "key",
                  name = "name",
                  description = "desc",
                  nameForms =
                    Some(ProtoNameForms(accusativePlural = "a", accusativeSingular = "b", genitivePlural = "c"))
                )
              ),
              allowCustomValues = true
            )
          ),
          searchFormSettings = Some(SearchFormSettings(CHECKBOX)),
          searchFormSettingsTouch = Some(SearchFormSettings(RANGE)),
          searchFormSettingsApp = Some(SearchFormSettings(TAGS))
        )
        for {
          _ <- setCatalogState(Nil, attribute :: Nil)
          expectedFilters = Set(
            SearchFilterControl(
              key = AttributeFieldPrefix + "attribute_id",
              name = "name",
              filterSettings = DictionarySettings(
                allowedValues = Seq(
                  DictionaryValue(
                    key = "key",
                    name = "name",
                    description = "desc",
                    NameForms(accusativePlural = "a", accusativeSingular = "b", genitivePlural = "c")
                  )
                ),
                allowCustomValues = true
              ),
              controlTypeWeb = Checkbox,
              controlTypeTouch = Range,
              controlTypeApp = Tags,
              incompatibleFilters = Nil,
              relatedAttributeId = Some("attribute_id")
            )
          )
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq.empty,
            foundAttributes = expectedFilters.map(_.key),
            foundDictionaryAttributeValues = Map.empty,
            parameters = Seq(
              SearchFilter(key = "attribute_id", operation = Equal(StringValue("key"))),
              SearchFilter(key = HasPhotosFieldName, operation = Equal(BooleanValue(true)))
            ),
            categoryIdsForDeliveryFilter = Seq.empty
          )

        } yield assert(filters)(hasSameElements(expectedFilters))
      },
      testM("Отбрасывает фильтры, которых нет в категориях") {
        for {
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq("category"),
            foundAttributes =
              Set(Price.key, State.key, "unknown_attr").map(FilterKeyToDocumentFieldMapping.mapFilterKeyToFieldName),
            foundDictionaryAttributeValues = Map.empty,
            parameters = Seq.empty,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(hasSameElements(Set(Price, State)))
      },
      testM("Not create filters for hidden attributes") {
        val attribute = AttributeDefinition(
          id = "attribute_id",
          name = "name",
          isHidden = true,
          attributeSettings = AttributeSettings.DictionarySettings(
            ProtoDictionarySettings(
              allowedValues = Seq(
                ProtoDictionaryValue(key = "key", name = "name", description = "desc")
              ),
              allowCustomValues = true
            )
          ),
          searchFormSettings = Some(SearchFormSettings(CHECKBOX)),
          searchFormSettingsTouch = Some(SearchFormSettings(RANGE)),
          searchFormSettingsApp = Some(SearchFormSettings(TAGS))
        )
        val category = Category(id = "c", name = "c_name")
        for {
          _ <- setCatalogState(category :: Nil, attribute :: Nil)
          expectedFilters = DefaultFilterCreator.FallbackFilters
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq("c"),
            foundAttributes =
              expectedFilters.map(filter => FilterKeyToDocumentFieldMapping.mapFilterKeyToFieldName(filter.key)).toSet,
            foundDictionaryAttributeValues = Map.empty,
            parameters = Seq.empty,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(hasSameElements(expectedFilters))
      },
      testM("Intersect filters") {
        val attributeInIntersection = AttributeDefinition(
          id = "a_intersection",
          name = "name",
          attributeSettings = AttributeSettings.BooleanSettings(ProtoBooleanSettings()),
          searchFormSettings = Some(SearchFormSettings(CHECKBOX_SET)),
          searchFormSettingsTouch = Some(SearchFormSettings(SELECT)),
          searchFormSettingsApp = Some(SearchFormSettings(SELECT_MULTIPLE))
        )
        val uniqueAttribute1 = AttributeDefinition(
          id = "a_1",
          name = "non_unique_name",
          attributeSettings = AttributeSettings.StringSettings(ProtoStringSettings())
        )
        val uniqueAttribute2 = AttributeDefinition(
          id = "a_2",
          name = "non_unique_name",
          attributeSettings = AttributeSettings.StringSettings(ProtoStringSettings())
        )
        val rootCategory = Category(id = "root", name = "Одежда")

        val rabotaCategory = Category(id = "rabota", name = "Работа", parentId = rootCategory.id)
        val electronicsCategory = Category(id = "electronics", name = "Электроника", parentId = rootCategory.id)

        val cv = Category(
          id = "cv",
          name = "Резюме",
          parentId = rabotaCategory.id,
          attributes = Seq(
            CategoryAttribute(attributeInIntersection.id, attributeInIntersection.version),
            CategoryAttribute(uniqueAttribute1.id, uniqueAttribute1.version)
          )
        )
        val mobilePhones =
          Category(
            id = "mobile_phones",
            name = "Мобильные телефоны",
            parentId = electronicsCategory.id,
            attributes = Seq(
              CategoryAttribute(attributeInIntersection.id, attributeInIntersection.version),
              CategoryAttribute(uniqueAttribute2.id, uniqueAttribute2.version)
            )
          )
        for {
          _ <- setCatalogState(
            List(rootCategory, rabotaCategory, electronicsCategory, cv, mobilePhones),
            List(attributeInIntersection, uniqueAttribute1, uniqueAttribute2)
          )
          expectedFilters = Seq(
            SearchFilterControl(
              key = AttributeFieldPrefix + "a_intersection",
              name = "name",
              filterSettings = BooleanSettings,
              controlTypeWeb = CheckboxSet,
              controlTypeTouch = Select,
              controlTypeApp = SelectMultiple,
              incompatibleFilters = Nil,
              relatedAttributeId = Some("a_intersection")
            )
          )
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq("cv", "mobile_phones"),
            foundAttributes =
              expectedFilters.map(filter => FilterKeyToDocumentFieldMapping.mapFilterKeyToFieldName(filter.key)).toSet,
            foundDictionaryAttributeValues = Map.empty,
            parameters = Seq.empty,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(hasSameElements(expectedFilters))
      },
      testM("Get rabota filters when only rabota children searched") {
        val rootCategory = Category(id = Constants.rabotaCategoryId, name = "Работа")

        val cv = Category(id = "cv", name = "Резюме", parentId = Constants.rabotaCategoryId, ignoreCondition = true)
        val vacancy =
          Category(id = "vacancy", name = "Вакансия", parentId = Constants.rabotaCategoryId, ignoreCondition = true)
        for {
          _ <- setCatalogState(rootCategory :: cv :: vacancy :: Nil, Nil)
          expectedFilters = Seq(DefaultFilterCreator.Salary)
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq("cv", "vacancy"),
            foundAttributes =
              expectedFilters.map(filter => FilterKeyToDocumentFieldMapping.mapFilterKeyToFieldName(filter.key)).toSet,
            foundDictionaryAttributeValues = Map.empty,
            parameters = Seq.empty,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(hasSameElements(expectedFilters))
      },
      testM("Get filters intersection when rabota and non rabota children categories appeared") {
        val rootCategory = Category(id = Constants.rabotaCategoryId, name = "Работа")
        val cv = Category(id = "cv", name = "Резюме", parentId = Constants.rabotaCategoryId, ignoreCondition = true)
        val vacancy =
          Category(id = "vacancy", name = "Вакансия", parentId = Constants.rabotaCategoryId, ignoreCondition = true)

        val anotherRoot = Category(id = "notrabota", name = "Электроника")
        val cameras = Category(id = "camera", name = "Камера", parentId = "notrabota")
        for {
          _ <- setCatalogState(List(rootCategory, cv, vacancy, anotherRoot, cameras), Nil)
          expectedFilters = Seq.empty[SearchFilterControl]
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq("cv", "vacancy", "camera"),
            foundAttributes =
              expectedFilters.map(filter => FilterKeyToDocumentFieldMapping.mapFilterKeyToFieldName(filter.key)).toSet,
            foundDictionaryAttributeValues = Map.empty,
            parameters = Seq.empty,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(hasSameElements(expectedFilters))
      },
      testM("Фильтрует значения словарных атрибутов по найденным") {
        val category =
          Category(
            id = "c",
            name = "c_name",
            attributes = Seq(CategoryAttribute(DictionaryAttribute.id, DictionaryAttribute.version))
          )

        for {
          _ <- setCatalogState(category :: Nil, DictionaryAttribute :: Nil)
          expectedFilters = Set(
            SearchFilterControl(
              key = AttributeFieldPrefix + DictionaryAttribute.id,
              name = DictionaryAttribute.name,
              filterSettings = DictionarySettings(
                allowedValues = Seq(
                  DictionaryValue(
                    key = "dictionary_value_key",
                    name = "dictionary_value_name",
                    description = "desc",
                    nameForms = NameForms(accusativePlural = "a", accusativeSingular = "b", genitivePlural = "c")
                  )
                ),
                allowCustomValues = true
              ),
              controlTypeWeb = Checkbox,
              controlTypeTouch = Range,
              controlTypeApp = Tags,
              incompatibleFilters = Nil,
              relatedAttributeId = Some(DictionaryAttribute.id)
            )
          )
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq("c"),
            foundAttributes = expectedFilters.map(_.key),
            foundDictionaryAttributeValues = Map(DictionaryAttribute.id -> Set("dictionary_value_key")),
            parameters = Seq.empty,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(hasSameElements(expectedFilters))
      },
      testM("Если атрибуты в выдаче отсутствуют, то возвращаются только выбранные фильтры") {
        val category =
          Category(
            id = "c",
            name = "c_name",
            attributes = Seq(CategoryAttribute(DictionaryAttribute.id, DictionaryAttribute.version))
          )

        for {
          _ <- setCatalogState(category :: Nil, DictionaryAttribute :: Nil)
          selectedParameters = Seq(
            SearchFilter(
              key = AttributeFieldPrefix + DictionaryAttribute.id,
              operation = In(
                Seq(
                  StringValue("dictionary_value_key"),
                  StringValue("dictionary_value_key2")
                )
              )
            )
          )
          expectedFilters = Set(
            SearchFilterControl(
              key = AttributeFieldPrefix + DictionaryAttribute.id,
              name = DictionaryAttribute.name,
              filterSettings = DictionarySettings(
                allowedValues = Seq(
                  DictionaryValue(
                    key = "dictionary_value_key",
                    name = "dictionary_value_name",
                    description = "desc",
                    nameForms = NameForms(accusativePlural = "a", accusativeSingular = "b", genitivePlural = "c")
                  ),
                  DictionaryValue(
                    key = "dictionary_value_key2",
                    name = "dictionary_value_name2",
                    description = "desc",
                    nameForms = NameForms(accusativePlural = "a", accusativeSingular = "b", genitivePlural = "c")
                  )
                ),
                allowCustomValues = true
              ),
              controlTypeWeb = Checkbox,
              controlTypeTouch = Range,
              controlTypeApp = Tags,
              incompatibleFilters = Nil,
              relatedAttributeId = Some(DictionaryAttribute.id)
            )
          )
          filters <- FilterCreator.createFilters(
            foundCategoryIds = Seq("c"),
            foundAttributes = Set.empty,
            foundDictionaryAttributeValues = Map.empty,
            parameters = selectedParameters,
            categoryIdsForDeliveryFilter = Seq.empty
          )
        } yield assert(filters)(hasSameElements(expectedFilters))
      }
    ).provideCustomLayer {
      Logging.live >+>
        ZRef.make(BonsaiSnapshot(List.empty, List.empty)).toLayer >+>
        FilterCreator.live
    }
  }
}
