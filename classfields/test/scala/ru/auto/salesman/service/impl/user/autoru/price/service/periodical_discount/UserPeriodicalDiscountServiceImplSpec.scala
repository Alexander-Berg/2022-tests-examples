package ru.auto.salesman.service.impl.user.autoru.price.service.periodical_discount

import ru.auto.salesman.dao.user.ActivePeriodicalDiscountDao
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.dao.user.PeriodicalDiscountDao.ActiveFilter._
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.model.user.periodical_discount_exclusion.User._
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

class UserPeriodicalDiscountServiceImplSpec extends BaseSpec with UserModelGenerators {

  val discountDao: ActivePeriodicalDiscountDao =
    mock[ActivePeriodicalDiscountDao]

  val userPeriodicalDiscountService = new UserPeriodicalDiscountServiceImpl(
    discountDao
  )

  "UserPeriodicalDiscountServiceImpl" should {

    "use discount in case if there is no user" in {
      forAll(PeriodicalDiscountGen) { periodicalDiscount =>
        (discountDao.getActive _)
          .expects(All)
          .returningZ(Some(periodicalDiscount))

        userPeriodicalDiscountService
          .getActiveDiscountFor(user = None)
          .success
          .value shouldBe UserInPeriodicalDiscount(periodicalDiscount)
      }
    }

    "not give discount to anonymous in case there is no active discount" in {
      (discountDao.getActive _).expects(All).returningZ(None)

      userPeriodicalDiscountService
        .getActiveDiscountFor(user = None)
        .success
        .value shouldBe NoActiveDiscount
    }

    "use existing discount" in {
      forAll(PeriodicalDiscountGen, AutoruUserGen) { (periodicalDiscount, user) =>
        (discountDao.getActive _)
          .expects(ForUser(user))
          .returningZ(Some(periodicalDiscount))

        userPeriodicalDiscountService
          .getActiveDiscountFor(Some(user))
          .success
          .value shouldBe UserInPeriodicalDiscount(periodicalDiscount)
      }
    }

    "exclude user from discount" in {
      forAll(PeriodicalDiscountGen, AutoruUserGen) { (periodicalDiscount, user) =>
        (discountDao.getActive _)
          .expects(All)
          .returningZ(Some(periodicalDiscount))
        (discountDao.getActive _)
          .expects(ForUser(user))
          .returningZ(None)

        userPeriodicalDiscountService
          .getActiveDiscountFor(Some(user))
          .success
          .value shouldBe UserExcludedFromDiscount(periodicalDiscount)
      }
    }

    "not give discount to authorized user in case there is no active discount" in {
      forAll(AutoruUserGen) { user =>
        (discountDao.getActive _).expects(All).returningZ(None)
        (discountDao.getActive _)
          .expects(ForUser(user))
          .returningZ(None)

        userPeriodicalDiscountService
          .getActiveDiscountFor(Some(user))
          .success
          .value shouldBe NoActiveDiscount
      }
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
