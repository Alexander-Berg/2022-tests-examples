package ru.yandex.realty.phone

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.realty.model.phone.{PhoneRedirect, PhoneType, TeleponyInfo}
import ru.yandex.realty.phone.RedirectPhoneService.Tag
import ru.yandex.realty.proto.phone.{PhoneRedirectStrategyAlgorithmType, PhoneRedirectStrategyStep}
import ru.yandex.realty.telepony.TeleponyClient.Domain.`billing_realty`
import ru.yandex.vertis.generators.BasicGenerators

import scala.concurrent.duration._

trait PhoneGenerators extends BasicGenerators {

  val phoneGen: Gen[String] = for {
    country <- Gen.choose(0, 9)
    code <- Gen.choose(0, 999)
    phone <- Gen.choose(0, 9999999)
  } yield f"$country%01d-$code%03d-$phone%07d"

  def redirectTeleponyInfoGen(
    targetPhone: String = phoneGen.next,
    tag: Tag = Tag.empty,
    phoneType: Option[PhoneType] = None,
    geoId: Option[Int] = None,
    strategy: PhoneRedirectStrategyAlgorithmType = PhoneRedirectStrategyAlgorithmType.PRS_ONE_STEP
  ): Gen[TeleponyInfo] =
    for {
      id <- readableString
    } yield {
      val strategyStep = PhoneRedirectStrategyStep
        .newBuilder()
        .setStrategy(strategy)
        .build()

      TeleponyInfo(
        objectId = id,
        target = targetPhone,
        domain = `billing_realty`,
        geoId = geoId,
        phoneType = phoneType,
        tag = tag,
        ttl = Some(3.seconds),
        strategy = strategyStep
      )
    }

  def phoneRedirectGen(
    targetPhone: String = phoneGen.next,
    tag: Tag = Tag.empty,
    phoneType: Option[PhoneType] = None,
    geoId: Option[Int] = None,
    strategy: PhoneRedirectStrategyAlgorithmType = PhoneRedirectStrategyAlgorithmType.PRS_ONE_STEP,
    strategyStepNumber: Int = PhoneRedirect.PhoneRedirectStrategyInitialStepNumber
  ): Gen[PhoneRedirect] =
    for {
      id <- readableString
      objectId <- readableString
      source <- phoneGen
    } yield {
      PhoneRedirect(
        domain = `billing_realty`,
        id = id,
        objectId = objectId,
        tag = tag,
        createTime = DateTime.now(),
        deadline = None,
        source = source,
        target = targetPhone,
        phoneType = phoneType,
        geoId = geoId,
        ttl = None,
        strategy = PhoneRedirectStrategyStep
          .newBuilder()
          .setStrategy(strategy)
          .setStepNumber(strategyStepNumber)
          .build()
      )
    }

}
