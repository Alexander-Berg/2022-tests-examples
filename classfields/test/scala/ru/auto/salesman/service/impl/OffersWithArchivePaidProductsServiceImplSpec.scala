package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.salesman.dao.OffersWithPaidProductsSalesmanDao.ClientProduct
import ru.auto.salesman.dao.{
  OffersWithPaidProductsArchiveSalesDao,
  OffersWithPaidProductsSalesmanDao
}
import ru.auto.salesman.service.{DealerFeatureService, EpochService, UnfoldActivateDates}
import ru.auto.salesman.test.BaseSpec

class OffersWithArchivePaidProductsServiceImplSpec extends BaseSpec {

  val newDaoMock = mock[OffersWithPaidProductsSalesmanDao]
  val oldDaoMock = mock[OffersWithPaidProductsArchiveSalesDao]
  val unfoldAcivateDatesMock = mock[UnfoldActivateDates]
  val dealerFeatureServiceMock = mock[DealerFeatureService]
  val epochServiceMock = mock[EpochService]

  private val service = new OffersWithArchivePaidProductsServiceImpl(
    newDao = newDaoMock,
    oldDao = oldDaoMock,
    unfoldActivateDates = unfoldAcivateDatesMock,
    dealerFeatureService = dealerFeatureServiceMock,
    epochService = epochServiceMock,
    startId = 100,
    categorizedStartId = 111,
    numberEntitiesPerLaunch = 100
  )

  "OffersWithArchivePaidProductsServiceImpl" should {
    "didn't save duplicate purchase" in {
      val clientId = 1111
      val offerId = 4444
      val product = "badge"
      val firstDate = DateTime.parse("2021-11-05T17:59:58")
      val secondDate = DateTime.parse("2021-11-06T17:59:58")
      val thirdDate = DateTime.parse("2021-11-07T17:59:58")
      val clientProduct =
        ClientProduct(clientId = clientId, offerId = offerId, product = product)

      (newDaoMock.countActivationWithinDay _)
        .expects(*, *, *, firstDate.toLocalDate)
        .returningZ(0)
      (newDaoMock.countActivationWithinDay _)
        .expects(*, *, *, secondDate.toLocalDate)
        .returningZ(1)
      (newDaoMock.countActivationWithinDay _)
        .expects(*, *, *, thirdDate.toLocalDate)
        .returningZ(0)

      (newDaoMock
        .setActivateDates(_: ClientProduct)(_: List[DateTime]))
        .expects(*, List(firstDate, thirdDate))
        .returningZ(())

      service
        .save(
          clientProduct,
          dates = List(
            firstDate,
            secondDate,
            thirdDate
          )
        )
        .success
    }
  }
}
