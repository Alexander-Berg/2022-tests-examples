package ru.yandex.vos2.util

import com.google.protobuf.Timestamp
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.api.vin.VinResolutionEnums.ResolutionPart
import ru.auto.api.vin.VinResolutionModel.{ResolutionEntry, VinIndexResolution}

trait VinIndexResolutionUtils {
  val version = 1

  def timestampFromMs(ms: Long): Timestamp = {
    com.google.protobuf.util.Timestamps.fromMillis(ms)
  }

  def resolutionLegalOk(ms: Option[Long] = None): VinIndexResolution = {
    val builder = VinIndexResolution
      .newBuilder()
      .setVersion(version)
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.SUMMARY)
          .setStatus(VinResolutionEnums.Status.OK)
      )
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.RP_LEGAL_GROUP)
          .setStatus(VinResolutionEnums.Status.OK)
      )
    ms.map(t => builder.setUpdated(timestampFromMs(t)))
    builder.build()
  }

  def resolutionLegalError(ms: Option[Long] = None): VinIndexResolution = {
    val builder = VinIndexResolution
      .newBuilder()
      .setVersion(version)
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.SUMMARY)
          .setStatus(VinResolutionEnums.Status.OK)
      )
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.RP_LEGAL_GROUP)
          .setStatus(VinResolutionEnums.Status.ERROR)
      )
    ms.map(t => builder.setUpdated(timestampFromMs(t)))
    builder.build()
  }

  def resolutionServiceHistory: VinIndexResolution = {
    val builder = VinIndexResolution
      .newBuilder()
      .setVersion(version)
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.SUMMARY)
          .setStatus(VinResolutionEnums.Status.OK)
      )
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.RP_SERVICE_HISTORY)
          .setStatus(VinResolutionEnums.Status.OK)
      )
    builder.build()
  }

  def resolutionOffersHistory: VinIndexResolution = {
    val builder = VinIndexResolution
      .newBuilder()
      .setVersion(version)
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.SUMMARY)
          .setStatus(VinResolutionEnums.Status.OK)
      )
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.RP_OFFERS_HISTORY)
          .setStatus(VinResolutionEnums.Status.OK)
      )
    builder.build()
  }

  def resolutionEmpty: VinIndexResolution = {
    VinIndexResolution
      .newBuilder()
      .setVersion(version)
      .build()
  }

  def resolutionNotEmpty: VinIndexResolution = {
    VinIndexResolution
      .newBuilder()
      .setVersion(version)
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.SUMMARY)
          .setStatus(VinResolutionEnums.Status.OK)
      )
      .build()
  }

  def resolutionWithSummary(status: VinResolutionEnums.Status): VinIndexResolution = {
    VinIndexResolution
      .newBuilder()
      .setVersion(version)
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.SUMMARY)
          .setStatus(status)
      )
      .build()
  }
}
