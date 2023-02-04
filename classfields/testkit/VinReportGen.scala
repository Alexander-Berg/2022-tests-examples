package ru.auto.comeback.model.testkit

import ru.auto.comeback.model.carfax.VinReport
import ru.auto.comeback.model.testkit.PastEventsGen._
import zio.random.Random
import zio.test.Gen._
import zio.test._

object VinReportGen {

  val anyVinReport: Gen[Random with Sized, VinReport] =
    for {
      vin <- CommonGen.anyVinCode
      offerRefs <- listOf(CarfaxHistoryOfferGen.offer())
      externalOffers <- listOf(anyExternalOffer)
      maintenances <- listOf(anyMaintenance)
      externalSales <- listOf(anyExternalSale)
      estimates <- listOf(anyEstimate)
      pastSalesOfInsurance <- listOf(anySaleOfInsurance)
    } yield VinReport(vin, offerRefs, externalOffers, maintenances, externalSales, estimates, pastSalesOfInsurance)
}
