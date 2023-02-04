package ru.yandex.realty.provider.stub

import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.model.sites.{Company, CompanyType}
import ru.yandex.realty.provider.stub.CompaniesStorageTestComponents.buildCompaniesStorage
import ru.yandex.realty.sites.CompaniesStorage

import java.util.Date
import scala.collection.JavaConverters._

trait CompaniesStorageTestComponents {
  val companiesProvider: Provider[CompaniesStorage] = buildCompaniesStorage()
}

object CompaniesStorageTestComponents {

  private def buildTestCompany(id: Int, name: String, companyType: CompanyType, startDateOpt: Option[Date] = None) = {
    val company = new Company(id)
    company.setName(name)
    company.setCompanyType(companyType)
    startDateOpt.foreach(company.setStartDate)
    company
  }

  def buildCompaniesStorage(): Provider[CompaniesStorage] = {
    val triPorosenka =
      buildTestCompany(
        1,
        "Три поросёнка",
        CompanyType.DEVELOPER
      )
    val diogen = buildTestCompany(
      2,
      "ИП Диоген",
      CompanyType.DEVELOPER
    )
    val shrek = buildTestCompany(
      3,
      "Шрек и осёл",
      CompanyType.DEVELOPER
    )

    val companies = Seq(triPorosenka, diogen, shrek)
    val companiesStorage = new CompaniesStorage(companies.asJava)
    val provider = new Provider[CompaniesStorage] {
      override def get(): CompaniesStorage = companiesStorage
    }
    provider
  }
}
