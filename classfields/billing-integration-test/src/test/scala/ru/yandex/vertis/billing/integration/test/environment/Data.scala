package ru.yandex.vertis.billing.integration.test.environment

import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.yandex.vertis.billing.model_core.{
  CallSettings,
  CampaignSettings,
  CoefficientDeposit,
  Limit,
  LimitSetting,
  Product
}
import ru.yandex.vertis.billing.service.CampaignService.Source
import ru.yandex.vertis.telepony.model.proto.{CallResultEnum, OperatorEnum, RedirectCallInfo, TeleponyCall}
import ru.yandex.vertis.telepony.model.proto.TeleponyCall.CallType
import scala.concurrent.duration._

import scala.util.Random

object Data {

  val carNewTag = "category=CARS#section=NEW#offer_id=1114743705-192a9b1b"
  val carUsedTag = "category=CARS#section=USED#offer_id=1114743705-1921239b1b"

  def getCampaignSettings(enabled: Boolean, dailyLimit: Option[Int], callSettings: Option[CallSettings]) =
    CampaignSettings(
      isEnabled = enabled,
      Limit(
        current = dailyLimit.map(LimitSetting.Daily(_, DateTime.now)),
        coming = dailyLimit.map(LimitSetting.Daily(_, DateTime.now)),
        weekCurrent = None,
        weekComing = None,
        monthCurrent = None,
        monthComing = None
      ),
      callSettings = callSettings,
      duration = None,
      attachRule = None,
      platforms = None,
      deposit = Some(CoefficientDeposit(0))
    )

  def getCampaign(orderId: Long, product: Product, campaignSettings: CampaignSettings) =
    Source(
      name = Some("calls_campaign_autoru_client_1"),
      orderId = orderId,
      product = product,
      settings = campaignSettings,
      campaignId = Some(s"campaign-${Random.nextLong()}"),
      offerIds = Nil,
      targetPhone = None,
      action = None
    )

  def getAutoruCall(
      objectId: String,
      tag: String = Data.carNewTag,
      now: Long = System.currentTimeMillis(),
      duration: FiniteDuration = 70.seconds,
      talkDuration: FiniteDuration = 60.seconds,
      isModeration: Boolean = false) =
    TeleponyCall
      .newBuilder()
      .setDomain("autoru_billing")
      .setObjectId(objectId)
      .setTag(tag)
      .setHasRecord(true)
      .setCallId(s"call-${Random.nextLong()}")
      .setTimestamp(Timestamps.fromMillis(now))
      .setTime(Timestamps.fromMillis(now))
      .setDurationSeconds(duration.toSeconds.toInt)
      .setTalkDurationSeconds(talkDuration.toSeconds.toInt)
      .setSourcePhone(s"+79170864543${Random.nextLong()}")
      .setTargetPhone("+74951528218")
      .setCallTypeValue(CallType.REDIRECT_CALL.getNumber)
      .setRedirectCallInfo(
        RedirectCallInfo
          .newBuilder()
          .setCallResult(CallResultEnum.CallResult.SUCCESS)
          .setOriginOperator(OperatorEnum.Operator.BEELINE)
          .setProxyGeoId(1)
          .setProxyNumber("+79652745612")
          .setProxyOperator(OperatorEnum.Operator.BEELINE)
          .setRedirectId("DL-u0PQZhfg")
          .setWhitelistOwnerId(if (isModeration) "Moderation" else "")
          .build()
      )
      .build()

  val realtyCall = TeleponyCall
    .newBuilder()
    .setDomain("realty-offers")
    .setObjectId("b8160aa0-10a3-4c85-9f60-b675e216411f")
    .setTag("tuzParamAgencyProfile=AGENCY_PROFILE#tuzParamUid=4059534018")
    .setHasRecord(true)
    .setCallId("VaDJ16Ip968")
    .setTimestamp(Timestamps.parse("2022-03-13T10:36:04.313000Z"))
    .setTime(Timestamps.parse("2022-03-13T10:21:48.000000Z"))
    .setDurationSeconds(70)
    .setTalkDurationSeconds(60)
    .setSourcePhone("+79157100491")
    .setTargetPhone("+79043333333")
    .setCallTypeValue(CallType.REDIRECT_CALL.getNumber)
    .setRedirectCallInfo(
      RedirectCallInfo
        .newBuilder()
        .setCallResult(CallResultEnum.CallResult.SUCCESS)
        .setOriginOperator(OperatorEnum.Operator.UNDEFINED)
        .setProxyGeoId(10174)
        .setProxyNumber("+79119326689")
        .setProxyOperator(OperatorEnum.Operator.MTS)
        .setRedirectId("U0pROiZTK0A")
        .build()
    )
    .build()
}
