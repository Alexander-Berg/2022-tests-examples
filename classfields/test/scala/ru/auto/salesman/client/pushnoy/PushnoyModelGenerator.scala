package ru.auto.salesman.client.pushnoy

import org.scalacheck.Gen
import ru.auto.salesman.client.pushnoy
import ru.auto.salesman.model.push.{PushBody, PushName, PushTitle}
import ru.yandex.vertis.generators.BasicGenerators

trait PushnoyModelGenerator extends BasicGenerators {

  lazy val AppVersionGenerator: Gen[AppVersionLimit] = for {
    compare <- Gen.alphaNumStr
    isBoth <- bool
    isIos <- bool
    ios <- Gen.alphaNumStr
    android <- Gen.alphaNumStr
  } yield
    if (isBoth)
      AppVersionLimit(compare, Some(ios), Some(android))
    else if (isIos)
      AppVersionLimit(compare, Some(ios), None)
    else
      AppVersionLimit(compare, None, Some(android))

  lazy val PushnoyDeliveryGenerator: Gen[PushnoyDelivery] =
    Gen.oneOf(PushnoyDelivery.values)

  private val pushNameGen = readableString.map(PushName)
  private val pushTitleGen = readableString.map(PushTitle)
  private val pushBodyGen = readableString.map(PushBody)

  val pushTemplateV1Gen: Gen[PushTemplateV1] =
    for {
      event <- Gen.alphaNumStr
      pushName <- pushNameGen
      deepLink <- Gen.alphaNumStr
      title <- pushTitleGen
      body <- pushBodyGen
    } yield pushnoy.PushTemplateV1(event, pushName, deepLink, title, body)

  lazy val ToPushDeliveryGenerator: Gen[ToPushDelivery] = for {
    userId <- Gen.alphaNumStr
    delivery <- Gen.option(PushnoyDeliveryGenerator)
    appVersion <- Gen.option(AppVersionGenerator)
  } yield ToPushDelivery(userId, delivery, appVersion)

  lazy val ToPushDeliveryGeneratorWithDelivery: Gen[ToPushDelivery] = for {
    userId <- Gen.alphaNumStr
    delivery <- Gen.some(PushnoyDeliveryGenerator)
    appVersion <- Gen.option(AppVersionGenerator)
  } yield ToPushDelivery(userId, delivery, appVersion)

  lazy val ToPushDeliveryGeneratorWithoutDelivery: Gen[ToPushDelivery] = for {
    userId <- Gen.alphaNumStr
    appVersion <- Gen.option(AppVersionGenerator)
  } yield ToPushDelivery(userId, None, appVersion)

  lazy val PushResponseGen: Gen[PushResponse] = for {
    count <- Gen.posNum[Int]
  } yield PushResponse(count)

  def alertGen(titleGen: Gen[PushTitle] = pushTitleGen): Gen[Alert] =
    for {
      body <- pushBodyGen
      title <- titleGen
      contentAvailable <- Gen.posNum[Int]
    } yield Alert(body, title, contentAvailable)
}
