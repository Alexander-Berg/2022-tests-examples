package ru.auto.salesman.dao.cached.user

import org.joda.time.DateTime
import ru.auto.salesman.dao.user.PeriodicalDiscountDao
import ru.auto.salesman.dao.user.PeriodicalDiscountDao.ActiveFilter.{All, ForUser}
import ru.auto.salesman.model.user.PeriodicalDiscount
import ru.auto.salesman.model.{AutoruUser, DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

class CachedActivePeriodicalDiscountDaoSpec extends BaseSpec with UserModelGenerators {

  private val periodicalDiscountDao: PeriodicalDiscountDao =
    mock[PeriodicalDiscountDao]

  private def createDao() =
    new CachedActivePeriodicalDiscountDao(periodicalDiscountDao)

  "getActive" should {

    "request discount from external dao for filter All and returns proper discount" in {
      val discount = None

      mockGetActiveOnce(discount, List.empty)
      val dao = createDao()

      dao.getActive(All).success.value shouldBe discount
    }

    "request discount and exclusions from external dao for filter All and returns proper discount" in {
      val discount = Some(PeriodicalDiscountGen.next)

      mockGetActiveOnce(discount, List.empty)
      val dao = createDao()

      dao.getActive(All).success.value shouldBe discount
    }

    "request discount and exclusions from external dao for filter All and returns data from cache on other calls" in {
      val filter = All
      val discount = Some(PeriodicalDiscountGen.next)

      mockGetActiveOnce(discount, List.empty)
      val dao = createDao()

      dao.getActive(filter).success.value shouldBe discount
      dao.getActive(filter).success.value shouldBe discount
    }

    "cache absent discount too" in {
      val discount = None

      mockGetActiveOnce(discount, List.empty)
      val dao = createDao()

      dao.getActive(All).success.value shouldBe discount
      dao.getActive(All).success.value shouldBe discount
    }

    "request discount from external dao for filter ForUser and returns proper discount" in {
      val user = AutoruUserGen.next
      val discount = None

      mockGetActiveOnce(discount, List.empty)
      val dao = createDao()

      dao.getActive(ForUser(user)).success.value shouldBe discount
    }

    "request discount and exclusions from external dao for filter ForUser and returns proper discount" in {
      val user = AutoruUserGen.next
      val discount = Some(PeriodicalDiscountGen.next)

      mockGetActiveOnce(discount, List.empty)
      val dao = createDao()

      dao.getActive(ForUser(user)).success.value shouldBe discount
    }

    "request discount and exclusions from external dao for filter ForUser and returns data from cache on other calls" in {
      val user = AutoruUserGen.next
      val filter = ForUser(user)
      val discount = Some(PeriodicalDiscountGen.next)

      mockGetActiveOnce(discount, List.empty)
      val dao = createDao()

      dao.getActive(filter).success.value shouldBe discount
      dao.getActive(filter).success.value shouldBe discount
    }

    "dont give discount to excluded user" in {
      val user = AutoruUserGen.next
      val discount = Some(PeriodicalDiscountGen.next)

      mockGetActiveOnce(discount, List(user))
      val dao = createDao()

      dao.getActive(ForUser(user)).success.value shouldBe None
    }

    "give discount to user that is not in exclusions" in {
      val excludedUsers = List(AutoruUser(1), AutoruUser(2))
      val user = AutoruUser(3)
      val discount = Some(PeriodicalDiscountGen.next)

      mockGetActiveOnce(discount, excludedUsers)
      val dao = createDao()

      dao.getActive(ForUser(user)).success.value shouldBe discount
    }

    "request discount and exclusions from external dao if cache expired and return new discount" in {
      val discount = Some(PeriodicalDiscountGen.next)
      mockGetActiveOnce(discount, List.empty)
      val dao = createDao()

      dao.getActive(All).success.value shouldBe discount

      val nextDiscount = Some(PeriodicalDiscountGen.next)
      mockGetActiveOnce(nextDiscount, List.empty)
      dao
        .getActive(All)
        .provideConstantClock(DateTime.now().plusMinutes(1))
        .success
        .value shouldBe nextDiscount
    }

    "request discount and dont request exclusions from external dao if cache expired while discount not changed and return old discount" in {
      val discount = Some(PeriodicalDiscountGen.next)
      mockGetActiveOnce(discount, List.empty)
      val dao = createDao()

      dao.getActive(All).success.value shouldBe discount

      (periodicalDiscountDao.getActive _).expects(All).returningZ(discount)
      dao
        .getActive(All)
        .provideConstantClock(DateTime.now().plusMinutes(1))
        .success
        .value shouldBe discount
    }

    "save exclusions if cache expired but discount not changed" in {
      val user = AutoruUserGen.next
      val filter = ForUser(user)
      val discount = Some(PeriodicalDiscountGen.next)
      mockGetActiveOnce(discount, List(user))
      val dao = createDao()

      dao.getActive(filter).success.value shouldBe None

      (periodicalDiscountDao.getActive _).expects(All).returningZ(discount)
      dao
        .getActive(filter)
        .provideConstantClock(DateTime.now().plusMinutes(1))
        .success
        .value shouldBe None
    }

  }

  private def mockGetActiveOnce(
      discount: Option[PeriodicalDiscount],
      exclusions: List[AutoruUser]
  ): Unit = {
    (periodicalDiscountDao.getActive _).expects(All).returningZ(discount)

    discount.foreach(disc =>
      (periodicalDiscountDao.getExcludedUsersByDiscount _)
        .expects(disc.discountId)
        .returningZ(exclusions)
    )
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
