package ru.yandex.vertis.general.feed.processor.dictionary.testkit

import ru.yandex.vertis.general.feed.processor.dictionary.BonsaiDictionaryService
import ru.yandex.vertis.general.feed.processor.dictionary.BonsaiDictionaryService._

class BonsaiDictionaryTestService(
    categoryIds: CategoryIdDictionary,
    categoryUniqueNames: CategoryUniqueNameDictionary,
    attributeSynonyms: AttributeSynonymDictionary,
    attributeIds: AttributeIdDictionary,
    attributeUniqueNames: AttributeUniqueNameDictionary,
    dictionaryAttributeValues: DictionaryAttributeValues)
  extends BonsaiDictionaryService.Service {
  override def categoryIdDictionary: CategoryIdDictionary = categoryIds
  override def categoryUniqueNameDictionary: CategoryUniqueNameDictionary = categoryUniqueNames
  override def attributeSynonymDictionary: AttributeSynonymDictionary = attributeSynonyms
  override def attributeIdDictionary: AttributeIdDictionary = attributeIds
  override def attributeUniqueNameDictionary: AttributeUniqueNameDictionary = attributeUniqueNames
  override def attributeValueDictionary: DictionaryAttributeValues = dictionaryAttributeValues
  override def rabotaLeafCategoriesSet: Set[CategoryId] = Set.empty
  override def vakansiiLeafCategoriesSet: Set[CategoryId] = Set.empty
  override def uslugiLeafCategoriesSet: Set[CategoryId] = Set.empty
}
