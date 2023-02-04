package ru.auto.cabinet.dao

import org.scalatest.flatspec.AsyncFlatSpec
import ru.auto.cabinet.dao.jdbc.AgreementDao
import ru.auto.cabinet.service.instr.EmptyInstr
import ru.auto.cabinet.test.JdbcSpecTemplate

/** Specs [[AgreementDao]]
  */
class AgreementDaoSpec extends AsyncFlatSpec with JdbcSpecTemplate {
  implicit val instr = new EmptyInstr("test")
  val dao = new AgreementDao(office7Database, office7Database)

  "AgreementDao" should "return false for the random client" in {
    for {
      got <- dao.getAgreement(wrongClientId)
    } yield {
      got.agreement should be(false)
      got.offerId should be(agreementOfferId)
    }
  }

  it should "add agreement for the client" in {
    for {
      result <- dao.addAgreement(agreementOfferId, client1Id, 0)
      got <- dao.getAgreement(client1Id)
    } yield {
      result.rowCount should be(1)
      got.agreement should be(true)
      got.offerId should be(agreementOfferId)
    }
  }

}
