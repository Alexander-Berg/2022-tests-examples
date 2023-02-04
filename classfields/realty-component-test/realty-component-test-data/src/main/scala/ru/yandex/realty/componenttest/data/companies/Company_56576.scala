package ru.yandex.realty.componenttest.data.companies

import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.{
  extractIdFromClassName,
  loadProtoFromJsonResource
}
import ru.yandex.realty.model.message.ExtDataSchema.CompanyMessage

object Company_56576 {

  val Id: Long = extractIdFromClassName(getClass)

  val Proto: CompanyMessage = loadProtoFromJsonResource[CompanyMessage](s"companies/company_$Id.json")

  require(Proto.getId == Id, s"Loaded proto ID is not matched to expected: expectedId=$Id, protoId=${Proto.getId}")

}
