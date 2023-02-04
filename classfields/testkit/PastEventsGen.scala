package ru.auto.comeback.model.testkit

import java.time.Instant

import ru.auto.api.api_offer_model.Section
import ru.auto.comeback.model.Comeback.OfferRef
import ru.auto.comeback.model.PastEvents
import ru.auto.comeback.model.PastEvents.{
  PastCarfaxReportPurchase,
  PastEstimate,
  PastExternalOffer,
  PastExternalSale,
  PastMaintenance,
  PastOffer,
  PastSaleOfInsurance
}
import ru.auto.comeback.model.carfax.{
  CarfaxReportPurchase,
  Estimate,
  ExternalOffer,
  ExternalSale,
  Maintenance,
  SaleOfInsurance
}
import ru.auto.comeback.model.testkit.OfferGen.anyOfferRef
import zio.random.Random
import zio.test.{Gen, Sized}

object PastEventsGen {

  def externalOffer[R <: Random with Sized](
      date: Gen[R, Instant] = CommonGen.anyInstant,
      clientId: Gen[R, Option[Long]] = Gen.option(Gen.anyLong)): Gen[R, ExternalOffer] =
    date.zip(clientId).map(ExternalOffer.tupled)

  val anyExternalOffer: Gen[Random with Sized, ExternalOffer] = externalOffer()

  val anyPastExternalOffer: Gen[Random with Sized, PastExternalOffer] =
    anyExternalOffer.map(o => PastExternalOffer(o.activated))

  def maintenance[R <: Random with Sized](
      date: Gen[R, Instant] = CommonGen.anyInstant,
      clientId: Gen[R, Option[Long]] = Gen.option(Gen.anyLong)): Gen[R, Maintenance] =
    date.zip(clientId).map(Maintenance.tupled)

  val anyMaintenance: Gen[Random with Sized, Maintenance] = maintenance()

  val anyPastMaintenance: Gen[Random with Sized, PastMaintenance] =
    anyMaintenance.map(m => PastMaintenance(m.date))

  def externalSale[R <: Random with Sized](
      date: Gen[R, Instant] = CommonGen.anyInstant,
      clientId: Gen[R, Option[Long]] = Gen.option(Gen.anyLong)): Gen[R, ExternalSale] = {
    date.zip(clientId).map(ExternalSale.tupled)
  }

  val anyExternalSale: Gen[Random with Sized, ExternalSale] = externalSale()

  val anyPastExternalSale: Gen[Random with Sized, PastExternalSale] =
    anyExternalSale.map(s => PastExternalSale(s.date))

  def estimate[R <: Random with Sized](
      date: Gen[R, Instant] = CommonGen.anyInstant,
      clientId: Gen[R, Option[Long]] = Gen.option(Gen.anyLong)): Gen[R, Estimate] =
    date.zip(clientId).map(Estimate.tupled)

  def saleOfInsurance[R <: Random with Sized](
      date: Gen[R, Instant] = CommonGen.anyInstant,
      clientId: Gen[R, Option[Long]] = Gen.option(Gen.anyLong)): Gen[R, SaleOfInsurance] =
    date.zip(clientId).map(SaleOfInsurance.tupled)

  val anyEstimate: Gen[Random with Sized, Estimate] = estimate()

  val anyPastEstimate: Gen[Random with Sized, PastEstimate] =
    anyEstimate.map(m => PastEstimate(m.date))

  val anySaleOfInsurance: Gen[Random with Sized, SaleOfInsurance] = saleOfInsurance()

  val anyPastSaleOfInsurance: Gen[Random with Sized, PastSaleOfInsurance] =
    anyEstimate.map(m => PastSaleOfInsurance(m.date))

  def carfaxReportPurchase[R <: Random with Sized](
      date: Gen[R, Instant] = CommonGen.anyInstant,
      vin: Gen[R, String] = CommonGen.anyVinCode,
      clientId: Gen[R, Long] = Gen.anyLong): Gen[R, CarfaxReportPurchase] = for {
    date <- date
    vin <- vin
    clientId <- clientId
  } yield CarfaxReportPurchase(date, vin, Some(clientId))

  val anyCarfaxReportPurchase: Gen[Random with Sized, CarfaxReportPurchase] = carfaxReportPurchase()

  val anyPastCarfaxReportPurchase: Gen[Random with Sized, PastCarfaxReportPurchase] =
    anyCarfaxReportPurchase.map(o => PastCarfaxReportPurchase(o.date))

  def pastOffer(
      refGen: Gen[Random with Sized, OfferRef] = anyOfferRef,
      createdGen: Gen[Random with Sized, Instant] = CommonGen.anyInstant,
      deactivatedGen: Gen[Random with Sized, Option[Instant]] = Gen.option(CommonGen.anyInstant),
      sectionGen: Gen[Random with Sized, Section] = CommonGen.anySection): Gen[Random with Sized, PastOffer] = {
    for {
      ref <- refGen
      created <- createdGen
      deactivated <- deactivatedGen
      section <- sectionGen
    } yield PastOffer(ref, created, deactivated, section)
  }

  val anyPastOffer: Gen[Random with Sized, PastOffer] = pastOffer()

  def pastEvents[R <: Random with Sized](
      pastOfferGen: Gen[R, Option[PastOffer]] = Gen.option(anyPastOffer),
      pastExternalOfferGen: Gen[R, Option[PastExternalOffer]] = Gen.option(anyPastExternalOffer),
      pastMaintenanceGen: Gen[R, Option[PastMaintenance]] = Gen.option(anyPastMaintenance),
      pastExternalSaleGen: Gen[R, Option[PastExternalSale]] = Gen.option(anyPastExternalSale),
      pastEstimateGen: Gen[R, Option[PastEstimate]] = Gen.option(anyPastEstimate),
      pastSaleOfInsuranceGen: Gen[R, Option[PastSaleOfInsurance]] = Gen.option(anyPastSaleOfInsurance),
      pastCarfaxReportPurchaseGen: Gen[R, Option[PastCarfaxReportPurchase]] =
        Gen.option(anyPastCarfaxReportPurchase)): Gen[R, PastEvents] = {
    for {
      pastOffer <- pastOfferGen
      pastExternalOffer <- pastExternalOfferGen
      pastMaintenance <- pastMaintenanceGen
      pastExternalSale <- pastExternalSaleGen
      pastEstimate <- pastEstimateGen
      pastSaleOfInsurance <- pastSaleOfInsuranceGen
      pastCarfaxReportPurchase <- pastCarfaxReportPurchaseGen
    } yield PastEvents(
      pastOffer,
      pastExternalOffer,
      pastMaintenance,
      pastExternalSale,
      pastEstimate,
      pastSaleOfInsurance,
      pastCarfaxReportPurchase
    )
  }

  val anyPastEvents: Gen[Random with Sized, PastEvents] = pastEvents()

  val emptyPastEvents: Gen[Random with Sized, PastEvents] =
    pastEvents(Gen.none, Gen.none, Gen.none, Gen.none, Gen.none, Gen.none, Gen.none)

  def anyNonEmptyPastEvents: Gen[Random with Sized, PastEvents] =
    pastEvents(
      Gen.some(anyPastOffer),
      Gen.some(anyPastExternalOffer),
      Gen.some(anyPastMaintenance),
      Gen.some(anyPastExternalSale),
      Gen.some(anyPastEstimate),
      Gen.some(anyPastSaleOfInsurance),
      Gen.some(anyPastCarfaxReportPurchase)
    )
}
