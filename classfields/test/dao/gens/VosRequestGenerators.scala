package ru.auto.salesman.test.dao.gens

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.salesman.client.VosClient.{CountUserOffersQuery, GetUserOffersQuery}
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens._

trait VosRequestGenerators extends BasicSalesmanGenerators with OfferModelGenerators {

  val GetUserOffersQueryGen: Gen[GetUserOffersQuery] = for {
    userRef <- Gen.identifier
    category <- Gen.some(OfferCategoryGen)
    geoId <- Gen.option(Gen.posNum[Long])
    mark <- Gen.option(Gen.identifier)
    model <- Gen.option(Gen.identifier)
    section <- Gen.option(OfferSectionGen)
    interval <- Gen.option(dateTimeIntervalGen)
    statuses <- Gen.containerOf[Set, OfferStatus](OfferStatusGen).map(_.toSeq)
    includeRemoved <- bool
    page <- Gen.posNum[Int]
    pageSize <- Gen.posNum[Int]
  } yield
    GetUserOffersQuery(
      userRef,
      category,
      geoId,
      mark,
      model,
      section,
      interval.map(_.from),
      interval.map(_.to),
      statuses,
      includeRemoved,
      page,
      pageSize
    )

  val CountUserOffersQueryGen: Gen[CountUserOffersQuery] = for {
    userRef <- Gen.identifier
    category <- Gen.some(OfferCategoryGen)
    geoId <- Gen.option(Gen.posNum[Long])
    mark <- Gen.option(Gen.identifier)
    model <- Gen.option(Gen.identifier)
    section <- Gen.option(OfferSectionGen)
    interval <- Gen.option(dateTimeIntervalGen)
    statuses <- Gen.containerOf[Set, OfferStatus](OfferStatusGen).map(_.toSeq)
    includeRemoved <- bool
  } yield
    CountUserOffersQuery(
      userRef,
      category,
      geoId,
      mark,
      model,
      section,
      interval.map(_.from),
      interval.map(_.to),
      statuses,
      includeRemoved
    )
}
