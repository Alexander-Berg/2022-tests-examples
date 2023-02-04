package ru.yandex.realty.componenttest.data.banks

import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.{
  extractIdFromClassName,
  loadProtoFromJsonResource
}
import ru.yandex.realty.model.message.Mortgages.Bank

object Bank_322371 {

  val Id: Long = extractIdFromClassName(getClass)

  val Proto: Bank = loadProtoFromJsonResource[Bank](s"banks/bank_$Id.json")

  require(Proto.getId == Id, s"Loaded proto ID is not matched to expected: expectedId=$Id, protoId=${Proto.getId}")

}
