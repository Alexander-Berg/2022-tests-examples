package ru.yandex.vertis.safe_deal.model

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.safe_deal.model.Entity._
import ru.yandex.vertis.safe_deal.util.ValidationUtils
import ru.yandex.vertis.zio_baker.model.WithValidate.ValidationErrors
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.LocalDate

object EntitySpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("EntitySpec")(
      suite("BankingEntity")(
        test("badBic") {
          zio.test.assert(
            BankingEntity(
              "0445252256".taggedWith[Tag.Bic], // Сбер
              "40817810938160925982".taggedWith[Tag.AccountNumber],
              "Аркадий Аркадиев Аркадиевич".taggedWith[Tag.Name],
              AdditionalBankingDetails(
                "30101810400000000225".taggedWith[Tag.CorrAccountNumber],
                "АО ТИНЬКОФФ БАНК".taggedWith[Tag.BankName]
              ).some
            )
          )(throws(isSubtype[ValidationErrors](Assertion.anything)))
        },
        test("bad account") {
          zio.test.assert(
            BankingEntity(
              "044525225".taggedWith[Tag.Bic], // Сбер
              "40817810938161925982".taggedWith[Tag.AccountNumber],
              "Аркадий Аркадиев Аркадиевич".taggedWith[Tag.Name],
              AdditionalBankingDetails(
                "30101810400000000225".taggedWith[Tag.CorrAccountNumber],
                "АО ТИНЬКОФФ БАНК".taggedWith[Tag.BankName]
              ).some
            )
          )(throws(isSubtype[ValidationErrors](Assertion.anything)))
        },
        test("bad corr") {
          zio.test.assert(
            BankingEntity(
              "044525225".taggedWith[Tag.Bic], // Сбер
              "40817810938160925982".taggedWith[Tag.AccountNumber],
              "Аркадий Аркадиев Аркадиевич".taggedWith[Tag.Name],
              AdditionalBankingDetails(
                "1231231".taggedWith[Tag.CorrAccountNumber],
                "АО ТИНЬКОФФ БАНК".taggedWith[Tag.BankName]
              ).some
            )
          )(throws(isSubtype[ValidationErrors](Assertion.anything)))
        },
        test("normal") {
          zio.test.assert(
            BankingEntity(
              "044525225".taggedWith[Tag.Bic], // Сбер
              "40817810938160925982".taggedWith[Tag.AccountNumber],
              "Аркадий Аркадиев Аркадиевич".taggedWith[Tag.Name],
              AdditionalBankingDetails(
                "30101810400000000225".taggedWith[Tag.CorrAccountNumber],
                "АО ТИНЬКОФФ БАНК".taggedWith[Tag.BankName]
              ).some
            )
          )(isSubtype[BankingEntity](Assertion.anything))
        }
      ),
      suite("PassportRfEntity")(
        test("bad age") {
          val currentDate = LocalDate.now
          val birthDate = currentDate.minusYears(17).format(ValidationUtils.DateFormatter)
          zio.test.assert(
            PassportRfEntity(
              series = "2010".taggedWith[Tag.DocSeries],
              number = "123456".taggedWith[Tag.DocNumber],
              issueDate = "02.01.2010".taggedWith[Tag.StringDate],
              departCode = "300-200".taggedWith[Tag.DepartCode],
              departName = "УФМС".taggedWith[Tag.DepartName],
              birthPlace = "Урюпиеск".taggedWith[Tag.BirthPlace],
              birthDate = birthDate.taggedWith[Tag.StringDate],
              address = "г. Урюпиеск, ул. Ленина, д. 2".taggedWith[Tag.Address]
            )
          )(throws(isSubtype[ValidationErrors](Assertion.anything)))
        },
        test("normal") {
          zio.test.assert(
            PassportRfEntity(
              series = "2010".taggedWith[Tag.DocSeries],
              number = "123456".taggedWith[Tag.DocNumber],
              issueDate = "02.01.2010".taggedWith[Tag.StringDate],
              departCode = "300-200".taggedWith[Tag.DepartCode],
              departName = "УФМС".taggedWith[Tag.DepartName],
              birthPlace = "Урюпиеск".taggedWith[Tag.BirthPlace],
              birthDate = "02.01.1990".taggedWith[Tag.StringDate],
              address = "г. Урюпиеск, ул. Ленина, д. 2".taggedWith[Tag.Address]
            )
          )(isSubtype[PassportRfEntity](Assertion.anything))
        }
      )
    )
  }
}
