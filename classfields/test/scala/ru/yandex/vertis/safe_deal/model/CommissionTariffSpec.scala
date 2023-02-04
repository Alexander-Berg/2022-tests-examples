package ru.yandex.vertis.safe_deal.model

import cats.implicits.catsSyntaxOptionId
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.common.Domain
import ru.yandex.vertis.zio_baker.model.WithValidate.ValidationErrors
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object CommissionTariffSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("CommissionTariff")(
      test("empty") {
        zio.test.assert(
          CommissionTariff(CommissionTariffSpec.id, Domain.DOMAIN_AUTO, enabled = true, Seq())
        )(throws(isSubtype[ValidationErrors](Assertion.anything)))
      },
      test("gap") {
        zio.test.assert(
          CommissionTariff(
            CommissionTariffSpec.id,
            Domain.DOMAIN_AUTO,
            enabled = true,
            Seq(
              CommissionTariff.RangeItem(None, R1000.some, R1000, R2000.some),
              CommissionTariff.RangeItem(R2000.some, None, R1000, R2000.some)
            )
          )
        )(throws(isSubtype[ValidationErrors](Assertion.anything)))
      },
      test("intersect") {
        zio.test.assert(
          CommissionTariff(
            CommissionTariffSpec.id,
            Domain.DOMAIN_AUTO,
            enabled = true,
            Seq(
              CommissionTariff.RangeItem(None, R1000.some, R1000, R2000.some),
              CommissionTariff.RangeItem(None, R2000.some, R1000, R2000.some)
            )
          )
        )(throws(isSubtype[ValidationErrors](Assertion.anything)))
      },
      test("const") {
        zio.test.assert(
          CommissionTariff(
            CommissionTariffSpec.id,
            Domain.DOMAIN_AUTO,
            enabled = true,
            Seq(CommissionTariff.RangeItem(None, None, R1000, R2000.some))
          )
        )(isSubtype[CommissionTariff](Assertion.anything))

      },
      test("normal") {
        zio.test.assert(
          CommissionTariff(
            CommissionTariffSpec.id,
            Domain.DOMAIN_AUTO,
            enabled = true,
            Seq(
              CommissionTariff.RangeItem(None, R1000.some, R1000, R2000.some),
              CommissionTariff.RangeItem(R1000.some, R2000.some, R2000, R4000.some),
              CommissionTariff.RangeItem(R2000.some, None, R2000, None)
            )
          )
        )(isSubtype[CommissionTariff](Assertion.anything))
      }
    )

  private val id: CommissionTariffId = "aaa".taggedWith[Tag.CommissionTariffId]
  private val R1000: MoneyRub = 1000L.taggedWith[Tag.MoneyRub]
  private val R2000: MoneyRub = 2000L.taggedWith[Tag.MoneyRub]
  private val R4000: MoneyRub = 4000L.taggedWith[Tag.MoneyRub]
}
