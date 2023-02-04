package ru.yandex.realty.feedprocessor.model

import com.google.protobuf.util.Timestamps
import org.scalacheck.Gen
import ru.yandex.capa.model.{Partner, PartnerEnvironment, PartnerStatus, PartnerType, Service, Status}
import ru.yandex.partnerdata.feedloader.common.data.{Feed => FeedloaderFeed, FeedStatus, ServiceResultState}
import ru.yandex.realty.model.gen.RealtyGenerators
import ru.yandex.realty.model.message.RealtySchema.FeedStateMessage

import java.util.Date
import scala.collection.JavaConverters._

trait PartnerFeedGenerator extends RealtyGenerators {

  def partnerFeedGen(partnerId: Long, url: String): Gen[PartnerFeed] =
    for {
      feedId <- posNum[Long]
      state = FeedStateMessage
        .newBuilder()
        .setUrl(url)
        .setIsInternal(true)
        .setExportToVos(true)
        .setLastUpdateTime(Timestamps.fromDate(new Date()))
        .build()
    } yield PartnerFeed(partnerId, feedId, state, true, None, partnerId.toInt)

  def capaPartnerGen(partnerId: Long, uid: Option[Long], env: PartnerEnvironment): Gen[Partner] =
    for {
      host <- readableString
      statusId <- posNum[Long]
      statusName <- readableString
      reason <- readableString
      extendedReason <- readableString
      statusUid <- posNum[Long]
    } yield new Partner(
      partnerId,
      host,
      Service.REALTY,
      env,
      PartnerType.FEED,
      new Date,
      new Date,
      uid.map(Long.box).orNull,
      Map[String, String]().asJava,
      new PartnerStatus(new Status(statusId, statusName), reason, extendedReason, new Date, statusUid)
    )

  def feedloaderFeedGen(partnerId: Long, ellipticsDownloadUrl: String): Gen[FeedloaderFeed] =
    for {
      taskId <- posNum[Long]
      id <- posNum[Long]
      url <- readableString
      stateInfo <- readableString
    } yield new FeedloaderFeed(
      taskId,
      partnerId,
      id,
      1,
      FeedStatus.OK,
      1,
      ellipticsDownloadUrl,
      url,
      new Date,
      stateInfo,
      ServiceResultState.OK
    )
}
