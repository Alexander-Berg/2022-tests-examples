package ru.yandex.vertis.subscriptions.util.test

import org.scalacheck.Gen
import ru.yandex.vertis.generators.Producer
import ru.yandex.vertis.subscriptions.Model
import ru.yandex.vertis.subscriptions.model.LegacyGenerators

/**
  * Generators for Subscription domain objects
  *
  * @author dimas
  */
trait CoreGenerators extends LegacyGenerators {

  val Users: Producer[Model.User] = userGen

  val EmailDeliveries: Producer[Model.Delivery] = emailDeliveryGen

  val requestSources: Producer[Model.RequestSource] = requestSourceGen

  val emailSubscriptions: Producer[Model.Subscription] = emailSubscriptionGen

  val requests: Producer[Model.Request] = requestGen

  val subscriptions: Producer[Model.Subscription] = subscriptionGen

  val documents: Producer[Model.Document] = documentGen

  val emailSubscriptionsSources: Producer[Model.SubscriptionSource] = emailSubscriptionSourceGen

  val pushSubscriptionsSources: Producer[Model.SubscriptionSource] = pushSubscriptionSourceGen

  val anySubscriptionsSources: Producer[Model.SubscriptionSource] =
    Gen.oneOf(emailSubscriptionSourceGen, pushSubscriptionSourceGen)
}

object CoreGenerators extends CoreGenerators
