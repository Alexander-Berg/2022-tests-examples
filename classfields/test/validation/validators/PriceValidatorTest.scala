package ru.yandex.vertis.general.gost.logic.test.validation.validators

import ru.yandex.vertis.general.gost.logic.testkit.ValidatorTestkit.{odezhdaCategory, rabotaCategory, validatorTest}
import ru.yandex.vertis.general.gost.logic.validation.Validator
import ru.yandex.vertis.general.gost.logic.validation.validators.PriceValidator
import ru.yandex.vertis.general.gost.model.{Price, SalaryProfit}
import ru.yandex.vertis.general.gost.model.validation.fields._
import zio.ZLayer
import zio.test.Assertion._
import zio.test._

object PriceValidatorTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PriceValidator")(
      validatorTest("Fail when price in currency less than zero", odezhdaCategory)(
        _.copy(price = Price.InCurrency(-10L))
      )(contains(PriceInvalidValue)),
      validatorTest("Fail when price is too big", odezhdaCategory)(
        _.copy(price = Price.InCurrency(777777777777L))
      )(contains(TooBigPrice)),
      validatorTest("Fail when price is set for rabota", rabotaCategory)(
        _.copy(price = Price.InCurrency(30000L))
      )(contains(NotSalary)),
      validatorTest("Fail when salary is below zero", rabotaCategory)(
        _.copy(price = Price.Salary(-2, SalaryProfit.Gross))
      )(contains(SalaryBoundsBelowZero)),
      validatorTest("Fail when salary is too big", rabotaCategory)(
        _.copy(price = Price.Salary(777777777777L, SalaryProfit.Gross))
      )(contains(TooBigSalary)),
      validatorTest("Fail when salaryRange is below zero", rabotaCategory)(
        _.copy(price = Price.SalaryRange(Some(-3), None, SalaryProfit.Gross))
      )(contains(SalaryBoundsBelowZero)),
      validatorTest("Fail when salaryRange is too big", rabotaCategory)(
        _.copy(price = Price.SalaryRange(Some(777777777777L), None, SalaryProfit.Gross))
      )(contains(TooBigSalary)),
      validatorTest("Fail when salaryRange is incorrect", rabotaCategory)(
        _.copy(price = Price.SalaryRange(Some(20), Some(10), SalaryProfit.Gross))
      )(contains(SalaryBoundsConsistent)),
      validatorTest("Work fine with correct salary", rabotaCategory)(
        _.copy(price = Price.Salary(50, SalaryProfit.Gross))
      )(isEmpty),
      validatorTest("Fail when salary is set for odezhda", odezhdaCategory)(
        _.copy(price = Price.Salary(30000L, SalaryProfit.Gross))
      )(contains(NotPrice))
    ).provideCustomLayerShared(ZLayer.succeed[Validator](PriceValidator))
}
