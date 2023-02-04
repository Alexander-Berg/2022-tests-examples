package ru.yandex.realty.componenttest.data.campaigns

import com.google.protobuf.Int64Value

import java.util.UUID
import ru.yandex.realty.componenttest.data.companies.Company_56576
import ru.yandex.realty.componenttest.data.sites.Site_57547
import ru.yandex.realty.model.message.ExtDataSchema.{CampaignMessage, StringToStringMessage}
import ru.yandex.realty.model.phone.RealtyPhoneTags
import ru.yandex.realty.model.serialization.RealtySchemaVersions
import ru.yandex.realty.proto.phone.PhoneRedirectMessage
import ru.yandex.realty.telepony.TeleponyClient.Domain
import ru.yandex.vertis.telepony.model.proto.PhoneType

import scala.concurrent.duration._

object Campaign_56576 {

  val Id: String = UUID.randomUUID().toString
  val TargetPhone: String = "+73517319999"

  val RedirectPhone: PhoneRedirectMessage =
    PhoneRedirectMessage
      .newBuilder()
      .setDomain(Domain.`billing_realty`)
      .setObjectId(Id)
      .setId(TargetPhone)
      .setTarget(TargetPhone)
      .setSource("+73517310000")
      .setPhoneType(PhoneType.LOCAL)
      .setTag(RealtyPhoneTags.EmptyTagName)
      .setTtlSeconds(Int64Value.of(1.hour.toSeconds))
      .build()

  val Proto: CampaignMessage =
    CampaignMessage
      .newBuilder()
      .setVersion(RealtySchemaVersions.CAMPAIGN_VERSION)
      .setId(Id)
      .setNewbuildingId(Site_57547.Id)
      .setCompanyId(Company_56576.Id)
      .setTargetPhone(RedirectPhone.getTarget)
      .addPhones(
        StringToStringMessage
          .newBuilder()
          .setKey(RedirectPhone.getTag)
          .setValue(RedirectPhone.getSource)
          .build()
      )
      .addRedirects(RedirectPhone)
      .build()

}
