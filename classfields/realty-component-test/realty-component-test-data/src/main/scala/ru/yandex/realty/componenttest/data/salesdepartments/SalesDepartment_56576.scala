package ru.yandex.realty.componenttest.data.salesdepartments

import ru.yandex.realty.componenttest.data.campaigns.Campaign_56576
import ru.yandex.realty.componenttest.data.companies.Company_56576
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils
import ru.yandex.realty.componenttest.data.utils.ComponentTestDataUtils.extractIdFromClassName
import ru.yandex.realty.model.message.ExtDataSchema.SalesDepartmentMessage

object SalesDepartment_56576 {

  val Id: Long = extractIdFromClassName(getClass)

  val Proto: SalesDepartmentMessage =
    SalesDepartmentMessage
      .newBuilder()
      .setId(Company_56576.Id)
      .setName("СУ-155")
      .addPhones(Campaign_56576.RedirectPhone.getSource)
      .addPhonesWithTag(ComponentTestDataUtils.asPhoneMessage(Campaign_56576.RedirectPhone))
      .setIsRedirectPhones(true)
      .build()

  require(Proto.getId == Id, s"Proto ID is not matched to expected: expectedId=$Id, protoId=${Proto.getId}")

}
