package auto.carfax.carfax_money.logic.testkit

import auto.carfax.carfax_money.model.Meta.Defaults
import auto.carfax.money.money_model.{PriceCriteria, ServiceContext, UserContext}
import zio.random.Random
import zio.test.{Gen, Sized}

object PriceCriteriaGen {

  val dealersCriteriaGen: Gen[Random with Sized, PriceCriteria] = for {
    userContext <- Gen.anyInt
    serviceContext <- ServiceContextGen.serviceContextGen(Defaults.DealerAvailablePackages)
    selectionCriteria <- Gen.option(SelectionCriteriaGen.selectionCriteriaGen(1))
    geo <- Gen.oneOf(Gen.int(900, 1500), Gen.const(1), Gen.const(Defaults.EmptyNum))
  } yield PriceCriteria(
    serviceContext = Some(serviceContext),
    userContext = Some(UserContext(uid = s"dealer:$userContext", geoId = geo)),
    selectionCriteria = selectionCriteria
  )

  val userVinCriteriaGen: Gen[Random with Sized, PriceCriteria] = for {
    serviceContext <- ServiceContextGen
      .serviceContextGen(Defaults.AllUserAvailablePackages)
      .map(_.update(_.modify(_.withServiceType(ServiceContext.ServiceType.VIN_REPORT_SERVICE))))
    userContext <- Gen.anyInt
    selectionCriteria <- Gen.option(
      SelectionCriteriaGen.selectionCriteriaGen(serviceContext.getPackageInfo.servicesCount)
    )
    geo <- Gen.oneOf(Gen.int(900, 1500), Gen.const(1), Gen.const(Defaults.EmptyNum))
  } yield PriceCriteria(
    Some(serviceContext),
    Some(UserContext(uid = s"user:$userContext", geoId = geo)),
    selectionCriteria
  )

  val userOfferCriteriaGen: Gen[Random with Sized, PriceCriteria] = for {
    serviceContext <- ServiceContextGen
      .serviceContextGen(Defaults.AllUserAvailablePackages)
      .map(_.update(_.modify(_.withServiceType(ServiceContext.ServiceType.OFFER_REPORT_SERVICE))))
    userContext <- Gen.anyInt
    selectionCriteria <- Gen.option(
      SelectionCriteriaGen.selectionCriteriaGen(serviceContext.getPackageInfo.servicesCount)
    )
    geo <- Gen.oneOf(Gen.int(900, 1500), Gen.const(1), Gen.const(Defaults.EmptyNum))
  } yield PriceCriteria(
    Some(serviceContext),
    Some(UserContext(uid = s"user:$userContext", geoId = geo)),
    selectionCriteria
  )

}
