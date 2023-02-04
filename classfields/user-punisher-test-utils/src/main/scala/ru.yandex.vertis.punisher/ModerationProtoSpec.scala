package ru.yandex.vertis.punisher

import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.{Category => ProtoCategory, SellerType}
import ru.yandex.vertis.moderation.proto.Model.Opinions.Entry
import ru.yandex.vertis.moderation.proto.Model.{
  Context,
  Diff,
  Domain,
  Essentials,
  ExternalId,
  Instance,
  Metadata,
  Opinion,
  Opinions,
  Reason,
  UpdateJournalRecord,
  User
}

import scala.jdk.CollectionConverters._

trait ModerationProtoSpec {

  protected val Timestamp = 1590000000000L
  protected val Vin = "VERY_VIN"
  protected val PreviousVin = "VERY_VERY_VIN"
  protected val Mark = "BMW"
  protected val Model = "5ER"
  protected val ComplectationId = "6088683"
  protected val Year = 2011
  protected val GeobaseIds = Seq(10740, 98596)
  protected val AutoruObjectId = "1099046246-2b96dc1a"
  protected val RealtyObjectId = "7567973758093205791"
  protected val UserId = "111111"
  protected val DealerId = "222222"
  protected val YandexUid = "333333333"
  protected val Category = ProtoCategory.CARS

  protected def buildExternalId(user: User.Builder, objectId: String): ExternalId =
    ExternalId
      .newBuilder()
      .setVersion(1)
      .setUser(user)
      .setObjectId(objectId)
      .build

  protected def buildOpinion(`type`: Opinion.Type, reasons: Seq[Reason]): Opinion =
    Opinion
      .newBuilder()
      .setVersion(1)
      .setType(`type`)
      .addAllReasons(reasons.asJava)
      .build

  protected def buildEntry(domain: Domain.Builder, opinion: Opinion): Entry =
    Entry
      .newBuilder()
      .setVersion(1)
      .setDomain(domain)
      .setOpinion(opinion)
      .build()

  protected def buildOpinions(entries: Seq[Entry]): Opinions =
    Opinions.newBuilder
      .setVersion(1)
      .addAllEntries(entries.asJava)
      .build

  protected def buildAutoruEssentials(vin: String = Vin,
                                      mark: String = Mark,
                                      model: String = Model,
                                      complectationId: String = ComplectationId,
                                      year: Int = Year,
                                      isPlacedForFree: Boolean = true,
                                      geobaseIds: Seq[Int] = GeobaseIds,
                                      sellerType: SellerType = SellerType.PRIVATE,
                                      category: ProtoCategory = Category,
                                      offerWasActive: Boolean = false
                                     ): AutoruEssentials =
    AutoruEssentials
      .newBuilder()
      .setVersion(1)
      .setVin(vin)
      .setMark(mark)
      .setModel(model)
      .setComplectationId(complectationId)
      .setYear(year)
      .setIsPlacedForFree(isPlacedForFree)
      .addAllGeobaseId(geobaseIds.map(int2Integer).asJava)
      .setSellerType(sellerType)
      .setCategory(category)
      .setOfferWasActive(offerWasActive)
      .build()

  protected def buildInstance(externalId: ExternalId,
                              essentials: Essentials.Builder,
                              opinions: Opinions,
                              context: Context.Builder,
                              metadata: Iterable[Metadata] = Iterable()
                             ): Instance =
    Instance
      .newBuilder()
      .setVersion(1)
      .setHashVersion(1)
      .setExternalId(externalId)
      .setEssentials(essentials)
      .setOpinions(opinions)
      .setContext(context)
      .addAllMetadata(metadata.asJava)
      .build

  protected def buildUpdateJournalRecord(instance: Option[Instance] = None,
                                         prev: Option[Instance] = None,
                                         diff: Option[Diff.Builder] = None,
                                         ts: Option[Long] = None
                                        ): UpdateJournalRecord = {
    val builder = UpdateJournalRecord.newBuilder()

    builder.setVersion(1)
    instance.foreach(builder.setInstance)
    diff.foreach(builder.setDiff)
    prev.foreach(builder.setPrev)
    ts.foreach(builder.setTimestamp)
    builder.build
  }

}
