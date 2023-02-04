package ru.auto.comeback.model.testkit

import auto.common.manager.catalog.model.CatalogNames
import auto.common.model.user.AutoruUser.UserRef.DealerId
import ru.auto.api.comeback_model.SearchSubscription._
import ru.auto.comeback.model.subscriptions.SearchSubscription
import ru.auto.comeback.model.subscriptions.SearchSubscription.{NewSearchSubscription, SearchSubscription}
import zio.random.Random
import zio.test.{Gen, Sized}

object SearchSubscriptionGen {

  val anySetting: Gen[Random with Sized, Settings] = for {
    filter <- ExternalFiltersGen.anyComebackFilter
    emails <- Gen.listOfBounded(1, 5)(CommonGen.anyYandexEmail)
  } yield Settings(filter, emails)

  val anyStatus: Gen[Any, Status] = Gen.fromIterable(Status.values)

  val anySearchSubscription: Gen[Random with Sized, SearchSubscription] = for {
    id <- Gen.int(1, Int.MaxValue)
    userId <- Gen.long(0, Long.MaxValue)
    createdDate <- CommonGen.anyInstant
    subscriptionId <- Gen.anyUUID.map(_.toString)
    settings <- anySetting
    status <- anyStatus
  } yield SearchSubscription(id, DealerId(userId), createdDate, subscriptionId, settings, status)

  val anyCatalogNamesSubscription: Gen[Random with Sized, CatalogNames] = for {
    mark <- Gen.alphaNumericStringBounded(1, 10)
    model <- Gen.option(Gen.alphaNumericStringBounded(1, 10))
    superGen <- Gen.option(Gen.alphaNumericStringBounded(1, 10))
    techParam <- Gen.option(Gen.alphaNumericStringBounded(1, 10))
  } yield CatalogNames(mark, model, superGen, techParam)

  val anySearchSubscriptionList: Gen[Random with Sized, List[SearchSubscription]] =
    Gen.listOfBounded(1, 5)(anySearchSubscription)

  val anyNewSearchSubscription: Gen[Random with Sized, NewSearchSubscription] =
    anySearchSubscription.map(_.copy(id = None, status = Status.ACTIVE))

  val anyNewSearchSubscriptionList: Gen[Random with Sized, List[NewSearchSubscription]] =
    Gen.listOfBounded(2, 5)(anyNewSearchSubscription)
}
