package auto.dealers.calltracking.model.testkit

import auto.dealers.calltracking.model.ClientSettings
import zio.random.Random
import zio.test.{Gen, Sized}

object ClientSettingsGen {

  val notificationEmailGen = Gen.anyString.map(_ + "@notification.email")

  def clientSettings(
      relevantCallDuration: Gen[Random, Int] = Gen.anyInt.filter(_ >= 0),
      uniqueCallWindow: Gen[Random, Int] = Gen.int(1, 1024),
      notificationEmail: Gen[Random with Sized, String] = notificationEmailGen,
      calltrackingClassifiedsEnabled: Gen[Random, Boolean] = Gen.boolean,
      calltrackingEnabled: Gen[Random, Boolean] = Gen.boolean,
      calltrackingByOfferEnabled: Gen[Random, Boolean] = Gen.boolean): Gen[Random with Sized, ClientSettings] =
    for {
      relevantCallDuration <- relevantCallDuration
      uniqueCallWindow <- uniqueCallWindow
      notificationEmail <- notificationEmail
      calltrackingClassifiedsEnabled <- calltrackingClassifiedsEnabled
      calltrackingEnabled <- calltrackingEnabled
      calltrackingByOfferEnabled <- calltrackingByOfferEnabled
    } yield ClientSettings(
      relevantCallDuration,
      uniqueCallWindow,
      notificationEmail,
      calltrackingClassifiedsEnabled,
      calltrackingEnabled,
      calltrackingByOfferEnabled
    )

  val anyClientSettings: Gen[Random with Sized, ClientSettings] = clientSettings()
}
