package ru.yandex.realty2.extdataloader.loaders.campaign

import org.apache.commons.lang3.StringUtils.EMPTY
import org.joda.time.{DateTime, Duration, Instant}
import org.scalacheck.Gen
import ru.yandex.realty.model.billing.Campaign
import ru.yandex.realty.model.message.ExtDataSchema.SuperCall
import ru.yandex.realty.model.phone.{PhoneRedirect, TeleponyInfo}
import ru.yandex.realty.phone.PhoneGenerators

import scala.collection.JavaConverters._

trait CampaignGenerators extends PhoneGenerators {

  val campaignIdGen: Gen[String] = readableString

  def campaignGen(targetPhone: String = phoneGen.next, redirects: Seq[PhoneRedirect] = Seq.empty): Gen[Campaign] =
    for {
      id <- campaignIdGen
      siteId <- Gen.posNum[Long]
      companyId <- Gen.posNum[Long]
      cost <- Gen.chooseNum(0, 1000)
    } yield {
      new Campaign(
        id,
        siteId,
        companyId,
        targetPhone,
        redirects.map(redirect => (redirect.tag.getOrElse(EMPTY), redirect.source)).toMap.asJava,
        redirects.asJava,
        redirects.map(TeleponyInfo.fromPhoneRedirect).asJava,
        cost,
        cost,
        true,
        true,
        100L,
        100L,
        Instant.parse("2000-01-01"),
        Map.empty[String, String].asJava,
        null,
        null
      )
    }

  val pr = PhoneRedirect(
    domain = "test",
    id = "aaa",
    objectId = "someId",
    tag = None,
    createTime = DateTime.now,
    deadline = None,
    source = "+79999999999",
    target = "+78888888888",
    phoneType = None,
    geoId = None,
    ttl = None
  )

  val teleponyInfo = TeleponyInfo.fromPhoneRedirect(pr)

  val dummyCampaign: Campaign = new Campaign(
    "someId",
    1L,
    11L,
    "00000000000",
    Map("" -> pr.source).asJava,
    Seq(pr).asJava,
    Seq(teleponyInfo).asJava,
    150L,
    100L,
    true,
    true,
    100L,
    100L,
    Instant.parse("2000-01-01"),
    Map.empty[String, String].asJava,
    null,
    null
  )

  val inactiveDummyCampaignWithSuperCall: Campaign = new Campaign(
    "someId",
    1L,
    11L,
    "00000000000",
    Map("" -> pr.source).asJava,
    Seq(pr).asJava,
    Seq(teleponyInfo).asJava,
    150L,
    100L,
    true,
    true,
    100L,
    100L,
    Instant.parse("2000-01-01"),
    Map.empty[String, String].asJava,
    SuperCall
      .newBuilder()
      .setIsActive(false)
      .setSpecialPrice(10)
      .setFrom(Instant.parse("2020-01-01").getMillis)
      .setEndTime(Instant.now().plus(Duration.standardHours(1)).getMillis)
      .setCampaignId("inactive-campaign")
      .build(),
    null
  )

  val mapsExtendedCampaign: Campaign = new Campaign(
    "someId",
    42L,
    1121L,
    "00000000000",
    Map("" -> pr.source).asJava,
    Seq(pr).asJava,
    Seq(teleponyInfo).asJava,
    150L,
    100L,
    true,
    true,
    100L,
    100L,
    Instant.parse("2000-01-01"),
    Map.empty[String, String].asJava,
    null,
    null
  )

  val promotionExtendedCampaign: Campaign = new Campaign(
    "someId",
    101L,
    1121L,
    "00000000000",
    Map("" -> pr.source).asJava,
    Seq(pr).asJava,
    Seq(teleponyInfo).asJava,
    150L,
    100L,
    true,
    true,
    100L,
    100L,
    Instant.parse("2000-01-01"),
    Map.empty[String, String].asJava,
    null,
    null
  )

}
