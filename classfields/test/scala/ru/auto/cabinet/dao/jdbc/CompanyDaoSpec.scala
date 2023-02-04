package ru.auto.cabinet.dao.jdbc

import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.enablers.Definition.definitionOfOption
import ru.auto.cabinet.test.JdbcSpecTemplate

class CompanyDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {

  val companyDao = new CompanyDao(office7Database, office7Database)

  "CompanyDao" should {
    "find company by id" in {
      for {
        company <- companyDao.findOne(1L)
      } yield company shouldBe defined
    }
    "find company by client_id" in {
      for {
        company <- companyDao.findByClientId(105L)
      } yield company shouldBe defined
    }
  }

}
