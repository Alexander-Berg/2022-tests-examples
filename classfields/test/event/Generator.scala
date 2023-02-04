package ru.yandex.vertis.billing.event

import org.joda.time.format.DateTimeFormat
import org.scalacheck.Gen
import ru.yandex.vertis.billing.microcore_model.Properties
import ru.yandex.vertis.billing.model_core.TeleponyCallFact.CallResults
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{
  randomPrintableString,
  CampaignHeaderGen,
  CampaignIdGen,
  EpochGen,
  PhoneGen,
  ProductGen,
  TransactionIdGen
}
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.billing.util.DateTimeUtils.now
import ru.yandex.vertis.protobuf.kv.Converter

import scala.jdk.CollectionConverters._
import scala.concurrent.duration._

/**
  * Event stuff generators.
  *
  * @author alesavin
  */
object Generator {

  /*  Event record example:

      2014-12-28@264@23:07:49.830@4670956751391952765@http://used.mercedes-benz.ru/Other/Index/5783
      Map(rid -> 213,
      testing_group -> 9,
      timestamp -> 2014-12-28T23:07:49.830Z,
      offer_campaign_id -> 264,
      offer_model_id -> 9_3,
      project -> auto,
      offer_state -> EXCELLENT,
      format_version -> 1,
      offer_mark_id -> SAAB,
      tskv_format -> vertis-log,
      offer_url -> http://used.mercedes-benz.ru/Other/Index/5783,
      view_type -> touch-pad,
      offer_year -> 2004,
      offer_position -> 9,
      locale -> ru,
      user_yandex_uid -> 4670956751391952765,
      offer_balance_client_id -> 4036064,
      offer_from -> offers,
      offer_id -> 4644102113396557246,
      offer_partner_id -> 1008241476,
      component -> offer_click,
      portal_rid -> 213)
   */

  val DatFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZoneUTC()
  val TimeFormatter = DateTimeFormat.forPattern("HH:mm:ss.SSS").withZoneUTC()

  /** Generates [[EventRecord]] keyParts */
  val EventKeyPartsGen: Gen[List[String]] =
    for {
      dateTime <- Gen.choose(-2880, 2880).map(m => now().plusMinutes(m))
      campaign <- Gen.choose(1, 50)
      offerId <- Gen.posNum[Long]
      offerUrl <- Gen.alphaStr.map(s => s"http://$s")
    } yield DatFormatter.print(dateTime) ::
      campaign.toString ::
      TimeFormatter.print(dateTime) ::
      offerId.toString ::
      offerUrl :: Nil

  /** Generates [[EventRecord]] cells */
  val EventValuesGen: Gen[Map[String, String]] =
    for {
      dateTime <- Gen.choose(-2880, 2880).map(m => now().plusMinutes(m))
      rid <- ("rid", Gen.choose(1, 65000))
      testingGroup <- ("testing_group", Gen.choose(0, 9))
      timestamp <- ("timestamp", Gen.const(dateTime.toString))
      offerCampaignId <- ("offer_campaign_id", Gen.choose(1, 500))
      project <- ("project", Gen.const("realty"))
      offerState <- ("offer_state", Gen.oneOf("NEW", "EXCELLENT", "GOOD", "MIDDLING", "BAD", "UNDEFINED", "BEATEN"))
      formatVersion <- ("format_version", Gen.const("1"))
      tskvFormat <- ("tskv_format", Gen.const("vertis-log"))
      offerUrl <- ("offer_url", Gen.alphaStr.map(s => s"http://$s"))
      viewType <- ("view_type", Gen.const("touch-pad"))
      offerYear <- ("offer_year", Gen.choose(1950, 2014))
      offerPosition <- ("offer_position", Gen.choose(0, 19))
      locale <- ("locale", Gen.frequency((95, Gen.const("ru")), (5, Gen.const("uk"))))
      userYandexUid <- ("user_yandex_uid", Gen.posNum[Long])
      offerBalanceClientId <- ("offer_balance_client_id", Gen.posNum[Int])
      offerFrom <- ("offer_from", Gen.const("offers"))
      offerId <- ("offer_id", Gen.posNum[Long])
      offerPartnerId <- ("offer_partner_id", Gen.posNum[Int])
      component <- ("component", Gen.oneOf("offer_click"))
      portalRid <- ("portal_rid", rid._2)
    } yield (rid ::
      testingGroup ::
      timestamp ::
      offerCampaignId ::
      project ::
      offerState ::
      formatVersion ::
      tskvFormat ::
      offerUrl ::
      viewType ::
      offerYear ::
      offerPosition ::
      locale ::
      userYandexUid ::
      offerBalanceClientId ::
      offerFrom ::
      offerId ::
      offerPartnerId ::
      component ::
      portalRid ::
      Nil).map(p => (p._1, p._2.sample.get.toString)).toMap

  val BillingClickCostGen: Gen[(String, String)] =
    for {
      cost <- Gen.choose(-100, 100)
    } yield Properties.BILLING_CLICK_REVENUE -> cost.toString

  /**
    * Generates hold transaction ID event part
    */
  val HoldTransactionIdGen: Gen[(String, String)] =
    TransactionIdGen.map { id =>
      Properties.BILLING_HOLD_TRANSACTION_ID -> id
    }

  def billingCommonPropertiesGen(offerId: String): Gen[Map[String, String]] =
    for {
      deadline <- Gen.choose(3000, 4000).map(m => now().plusMinutes(m))
    } yield Properties.getCommon(deadline.toString, offerId).asScala.toMap

  /** Generates [[EventRecord]] instances */
  val EventRecordGen: Gen[EventRecord] = for {
    keyParts <- EventKeyPartsGen
    values <- EventValuesGen
    product <- ProductGen.suchThat(_.hasDefinedCost)
    campaignHeader <- CampaignHeaderGen.map(header => {
      Converter
        .toKeyValue(Conversions.toMessage(header.copy(product = product)), Some(Properties.BILLING_CAMPAIGN_HEADER))
        .get
    })
    commonProperties <- billingCommonPropertiesGen(values("offer_id"))
    clickCost <- BillingClickCostGen
    hold <- Gen.option(HoldTransactionIdGen)
    epoch <- Gen.some(EpochGen)
  } yield EventRecord(
    "test",
    keyParts,
    (values ++ campaignHeader ++ commonProperties ++ hold.toList) + clickCost,
    epoch = epoch
  )

}
