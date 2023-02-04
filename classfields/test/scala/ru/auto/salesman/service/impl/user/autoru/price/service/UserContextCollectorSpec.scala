package ru.auto.salesman.service.impl.user.autoru.price.service

import ru.auto.salesman.model.{AutoruUser, DeprecatedDomain, DeprecatedDomains, RegionId}
import ru.auto.salesman.service.PassportService
import ru.auto.salesman.service.geoservice.RegionService
import ru.auto.salesman.service.impl.user.PromocoderServiceImpl
import ru.auto.salesman.service.user.{ExperimentService, PriceService}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.{ServiceModelGenerators, UserModelGenerators}
import ru.yandex.passport.model.common.CommonModel.UserModerationStatus

class UserContextCollectorSpec
    extends BaseSpec
    with UserModelGenerators
    with ServiceModelGenerators {
  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val experimentService: ExperimentService = mock[ExperimentService]
  val regionService: RegionService = mock[RegionService]
  val passportService: PassportService = mock[PassportService]
  val promocoderService: PromocoderServiceImpl = mock[PromocoderServiceImpl]

  val priceService = mock[PriceService]

  val userContextCollector = new UserContextCollectorImpl(
    experimentService,
    regionService,
    passportService,
    promocoderService
  )

  "UserContextCollector" should {

    "collect all users price modifiers" in {
      val autoruUserOpt = AutoruUserGen.next
      val regionId = RegionIdGen.next

      (regionService
        .expandGeoIds(_: RegionId))
        .expects(*)
        .returningZ(List(RegionId(1)))

      (experimentService
        .getExperimentsForUser(_: AutoruUser))
        .expects(autoruUserOpt)
        .returningZ(None)

      (passportService.userModeration _)
        .expects(*)
        .returningZ(UserModerationStatus.newBuilder.build)

      (promocoderService
        .getFeatures(_: AutoruUser))
        .expects(*)
        .returningZ(List())

      userContextCollector
        .getUserContext(autoruUserOpt, Some(regionId), hasOffer = true)
        .success
    }

  }
}
