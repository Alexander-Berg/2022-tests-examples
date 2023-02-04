package ru.auto.salesman.service.impl.user.autoru.price.service

import ru.auto.salesman.model.user.ExperimentInfo
import ru.auto.salesman.model.{AutoruUser, DeprecatedDomain, DeprecatedDomains, RegionId}
import ru.auto.salesman.service.impl.user.autoru.price.reducer.AutoruPriceReducer
import ru.auto.salesman.service.user.autoru.price.service.UserContextCollector
import ru.auto.salesman.service.user.ModifyPriceService
import ru.auto.salesman.service.user.autoru.price.service.sale.UserPeriodicalDiscountService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators

class AutoruPriceReducerSpec extends BaseSpec with ServiceModelGenerators {
  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val userContextCollector: UserContextCollector = mock[UserContextCollector]
  val modifyPriceService: ModifyPriceService = mock[ModifyPriceService]

  val periodicalDiscountService: UserPeriodicalDiscountService =
    mock[UserPeriodicalDiscountService]

  val autoruPriceReducer = new AutoruPriceReducer(
    userContextCollector,
    periodicalDiscountService,
    modifyPriceService
  )

  "reducePrice" should {
    "use external experiment for buildPatchedPrice" in {
      forAll(
        FundsGen,
        ProductGen,
        AutoruUserGen,
        PriceServiceUserContextGen,
        patchedPriceGen()
      ) { (funds, product, user, userContext, price) =>
        (periodicalDiscountService
          .getActiveDiscountFor(_: Option[AutoruUser]))
          .expects(*)
          .returningZ(userPeriodicalDiscountGen().next)

        (userContextCollector
          .getUserContext(
            _: AutoruUser,
            _: Option[RegionId],
            _: Boolean
          ))
          .expects(user, None, false)
          .returningZ(userContext)

        val testExperiment = Some(ExperimentInfo(Some("test"), "box"))
        val testPrice = price.copy(modifier =
          price.modifier.map(_.copy(experimentInfo = testExperiment))
        )
        (modifyPriceService.buildPatchedPrice _)
          .expects(
            false,
            funds,
            *,
            *,
            *,
            testExperiment,
            testExperiment.flatMap(_.activeExperimentId),
            true,
            *,
            *
          )
          .returningZ(testPrice)

        autoruPriceReducer
          .reducePrice(
            funds,
            product,
            user,
            optOffer = None,
            testExperiment,
            testExperiment.flatMap(_.activeExperimentId)
          )
          .success
          .value shouldBe testPrice
      }
    }

  }
}
