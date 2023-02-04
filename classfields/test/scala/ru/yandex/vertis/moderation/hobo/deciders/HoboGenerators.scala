package ru.yandex.vertis.moderation.hobo.deciders

import org.scalacheck.Gen
import ru.yandex.vertis.hobo.proto.Common.AutoruProvenOwnerResolution
import ru.yandex.vertis.hobo.proto.Model
import ru.yandex.vertis.hobo.proto.Model._
import ru.yandex.vertis.moderation.hobo.ProtobufImplicits._
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{BooleanGen, StringGen}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Domain.{UsersAutoru => UsersAutoruDomains}

import scala.collection.JavaConverters._

object HoboGenerators {

  val HoboTaskGen: Gen[Model.Task.Builder] = Gen.delay(Task.newBuilder.setVersion(1))

  val AutoruCarTypeDomainGen: Gen[UsersAutoruDomains] = Gen.oneOf(UsersAutoruDomains.values.toSeq)

  val TrueFalseResolutionGen: Gen[TrueFalseResolution.Builder] =
    for {
      v <- BooleanGen
    } yield TrueFalseResolution.newBuilder
      .setVersion(1)
      .setValue(v)
      .setComment(StringGen.next)

  val CallCenterResolutionGen: Gen[CallCenterResolution.Builder] =
    for {
      value   <- Gen.oneOf(CallCenterResolution.Value.values)
      comment <- StringGen
    } yield CallCenterResolution.newBuilder
      .setVersion(1)
      .setValue(value)
      .setComment(comment)

  val RealtyVisualResolutionGen: Gen[RealtyVisualResolution.Builder] =
    for {
      value   <- Gen.oneOf(RealtyVisualResolution.Value.values)
      comment <- StringGen
    } yield RealtyVisualResolution.newBuilder
      .setVersion(1)
      .addAllValues(Seq(value).asJava)
      .setComment(comment)

  val ResellersCallResolutionGen: Gen[ResellersCallResolution.Builder] =
    for {
      value   <- Gen.oneOf(ResellersCallResolution.Value.values)
      comment <- StringGen
    } yield ResellersCallResolution.newBuilder
      .setVersion(1)
      .setValue(value)
      .setComment(comment)

  val StoCallResolutionGen: Gen[StoCallResolution.Builder] =
    for {
      value   <- Gen.oneOf(StoCallResolution.Value.values)
      comment <- StringGen
    } yield StoCallResolution.newBuilder
      .setVersion(1)
      .setValue(value)
      .setComment(comment)

  val AutoruVisualResolutionGen: Gen[AutoruVisualResolution.Builder] =
    for {
      value         <- Gen.oneOf(AutoruVisualResolution.Value.values)
      licensePlate  <- Gen.oneOf(AutoruVisualResolution.LicensePlateOnPhotos.values)
      comment       <- StringGen
      completeCheck <- Gen.oneOf(AutoruVisualResolution.CompleteCheck.values)
    } yield AutoruVisualResolution.newBuilder
      .setVersion(1)
      .addAllValues(Seq(value).asJava)
      .setLicensePlateOnPhotos(licensePlate)
      .setComment(comment)
      .setCompleteCheck(completeCheck)

  val AutoruCallResolutionGen: Gen[AutoruCallResolution.Builder] =
    for {
      value   <- Gen.oneOf(AutoruCallResolution.Value.values)
      comment <- StringGen
    } yield AutoruCallResolution.newBuilder
      .setVersion(1)
      .addAllValues(Seq(value).asJava)
      .setComment(comment)

  val StoComplaintsResolutionGen: Gen[StoComplaintsResolution.Builder] =
    for {
      value   <- Gen.oneOf(StoComplaintsResolution.Value.values)
      comment <- StringGen
    } yield StoComplaintsResolution.newBuilder
      .setVersion(1)
      .setValue(value)
      .setComment(comment)

  val SuspiciousCallResolutionGen: Gen[SuspiciousCallResolution.Builder] =
    for {
      value   <- Gen.oneOf(SuspiciousCallResolution.Value.values)
      extra   <- Gen.oneOf(SuspiciousCallResolution.ExtraValue.values)
      comment <- StringGen
    } yield SuspiciousCallResolution.newBuilder
      .setVersion(1)
      .setValue(value)
      .setExtraValue(extra)
      .setComment(comment)

  val PaidCallResolutionGen: Gen[PaidCallResolution.Builder] =
    for {
      value   <- Gen.oneOf(PaidCallResolution.Value.values)
      comment <- StringGen
    } yield PaidCallResolution.newBuilder
      .setValue(value)
      .setComment(comment)

  def resellerTypeGen(
      verdict: AutoruComplaintsResellerResolution.Value
  ): Gen[Seq[AutoruComplaintsResellerResolution.ResellerType]] =
    verdict match {
      case AutoruComplaintsResellerResolution.Value.RESELLER =>
        val resellerTypeGen = Gen.oneOf(AutoruComplaintsResellerResolution.ResellerType.values())
        Gen.listOf(resellerTypeGen)
      case _ => Gen.const(Seq.empty)
    }

  val AutoruComplaintsResellerResolutionGen: Gen[AutoruComplaintsResellerResolution.Builder] =
    for {
      value         <- Gen.oneOf(AutoruComplaintsResellerResolution.Value.values)
      domain        <- Gen.oneOf(UsersAutoruDomains.values)
      comment       <- StringGen
      resellerTypes <- resellerTypeGen(value)
    } yield AutoruComplaintsResellerResolution.newBuilder
      .setVersion(1)
      .addAllValues(Seq(value).asJava)
      .addAllResellerDomains(Seq(domain).asJava)
      .setComment(comment)
      .addAllResellerTypes(resellerTypes.asJava)

  val RedirectCheckResolutionGen: Gen[RedirectCheckResolution.Builder] =
    for {
      proxy   <- Gen.oneOf(RedirectCheckResolution.Value.values)
      target  <- Gen.oneOf(RedirectCheckResolution.Value.values)
      comment <- StringGen
    } yield RedirectCheckResolution.newBuilder
      .setVersion(1)
      .setProxy(proxy)
      .setTarget(target)
      .setComment(comment)

  val AutoruReviewsResolutionGen: Gen[AutoruReviewsResolution.Builder] =
    for {
      value   <- Gen.oneOf(AutoruReviewsResolution.Value.values)
      comment <- StringGen
    } yield AutoruReviewsResolution.newBuilder
      .setVersion(1)
      .addAllValues(Seq(value).asJava)
      .setComment(comment)

  val AutoruPurchasingResolutionGen: Gen[AutoruPurchasingResolution.Builder] =
    for {
      value   <- Gen.oneOf(AutoruPurchasingResolution.Value.values)
      comment <- StringGen
    } yield AutoruPurchasingResolution.newBuilder
      .setVersion(1)
      .setValue(value)
      .setComment(comment)

  val TeleponyMarkingResolutionGen: Gen[TeleponyMarkingResolution.Builder] =
    for {
      soldObjectTag <- StringGen
      humanSpamTag  <- StringGen
      comment       <- StringGen
      soldObject = TeleponyMarkingResolution.SoldObject.newBuilder.setTag(soldObjectTag)
      humanSpam  = TeleponyMarkingResolution.HumanSpam.newBuilder.setTag(humanSpamTag)
    } yield TeleponyMarkingResolution.newBuilder
      .setSoldObject(soldObject)
      .setHumanSpam(humanSpam)
      .setComment(comment)

  val RealtyComplaintsResellerResolutionGen: Gen[RealtyComplaintsResellerResolution.Builder] =
    for {
      value   <- Gen.oneOf(RealtyComplaintsResellerResolution.Value.values)
      comment <- StringGen
    } yield RealtyComplaintsResellerResolution.newBuilder
      .setValue(value)
      .setComment(comment)

  val CallCenterPhotoResolutionGen: Gen[CallCenterPhotoResolution.Builder] =
    for {
      value   <- Gen.oneOf(CallCenterPhotoResolution.Value.values)
      comment <- StringGen
    } yield CallCenterPhotoResolution.newBuilder
      .setValue(value)
      .setComment(comment)

  val SpamCallResolutionGen: Gen[SpamCallResolution.Builder] =
    for {
      value   <- Gen.oneOf(SpamCallResolution.Value.values)
      extra   <- Gen.oneOf(SpamCallResolution.ExtraValue.values)
      comment <- StringGen
    } yield SpamCallResolution.newBuilder
      .setValue(value)
      .setExtraValue(extra)
      .setComment(comment)

  val AutoruTamagotchiPhotoResultGen: Gen[AutoruTamagotchiResolution.PhotoResult.Builder] =
    for {
      url    <- StringGen
      result <- Gen.oneOf(AutoruTamagotchiResolution.PhotoResult.Result.values)
    } yield AutoruTamagotchiResolution.PhotoResult.newBuilder
      .setUrl(url)
      .setResult(result)

  val AutoruTamagotchiResolutionGen: Gen[AutoruTamagotchiResolution.Builder] =
    for {
      offer   <- AutoruVisualResolutionGen
      photo   <- AutoruTamagotchiPhotoResultGen
      comment <- StringGen
    } yield AutoruTamagotchiResolution.newBuilder
      .setOffer(offer)
      .addAllPhotos(Seq(photo.build).asJava)
      .setComment(comment)

  val AutoruLoyaltyDealersResolutionGen: Gen[AutoruLoyaltyDealersResolution.Builder] =
    for {
      value   <- Gen.oneOf(AutoruLoyaltyDealersResolution.Value.values)
      extra   <- Gen.oneOf(AutoruLoyaltyDealersResolution.ExtraValue.values)
      comment <- StringGen
    } yield AutoruLoyaltyDealersResolution.newBuilder
      .setValue(value)
      .setExtraValue(extra)
      .setComment(comment)

  val VinReportResolutionGen: Gen[VinReportResolution.Builder] =
    for {
      value   <- Gen.oneOf(VinReportResolution.Value.values)
      comment <- StringGen
    } yield VinReportResolution.newBuilder
      .setValue(value)
      .setComment(comment)

  val CallgateResolutionPhoneCallGen: Gen[CallgateResolution.PhoneCall.Builder] =
    for {
      phone <- StringGen
      url   <- StringGen
    } yield CallgateResolution.PhoneCall.newBuilder
      .setPhone(phone)
      .setUrl(url)

  val CallgateResolutionGen: Gen[CallgateResolution.Builder] =
    for {
      call      <- CallgateResolutionPhoneCallGen
      moderator <- StringGen
      comment   <- StringGen
    } yield CallgateResolution.newBuilder
      .addAllPhoneCalls(Seq(call.build).asJava)
      .setModerator(moderator)
      .setComment(comment)

  val AutoruPremoderationDealerResolutionGen: Gen[AutoruPremoderationDealerResolution.Builder] =
    for {
      value   <- Gen.oneOf(AutoruPremoderationDealerResolution.Value.values)
      comment <- StringGen
    } yield AutoruPremoderationDealerResolution.newBuilder
      .setValue(value)
      .setComment(comment)

  val RealtyAgencyCardsCheckResolutionGen: Gen[RealtyAgencyCardsCheckResolution.Builder] =
    for {
      value   <- Gen.oneOf(RealtyAgencyCardsCheckResolution.Value.values)
      comment <- StringGen
    } yield RealtyAgencyCardsCheckResolution.newBuilder
      .addAllValues(Seq(value).asJava)
      .setComment(comment)

  val AutoruAnalyticUsersCheckResolutionGen: Gen[AutoruAnalyticUsersCheckResolution.Builder] =
    for {
      value   <- Gen.oneOf(AutoruAnalyticUsersCheckResolution.Value.values)
      comment <- StringGen
    } yield AutoruAnalyticUsersCheckResolution.newBuilder
      .setValue(value)
      .setComment(comment)

  val AutoruProvenOwnerResolutionValueGen: Gen[AutoruProvenOwnerResolution.Value.Builder] =
    for {
      verdict <- Gen.oneOf(AutoruProvenOwnerResolution.Value.Verdict.values)
      offerId <- StringGen
      vin     <- StringGen
    } yield AutoruProvenOwnerResolution.Value.newBuilder
      .setVin(vin)
      .setOfferId(offerId)
      .setVerdict(verdict)

  val AutoruProvenOwnerResolutionGen: Gen[AutoruProvenOwnerResolution.Builder] =
    for {
      value   <- AutoruProvenOwnerResolutionValueGen
      comment <- StringGen
    } yield AutoruProvenOwnerResolution.newBuilder
      .addAllValues(Seq(value.build).asJava)
      .setComment(comment)

  val AutoruResellersRevalidationResolutionGen: Gen[AutoruResellersRevalidationResolution.Builder] =
    for {
      value   <- Gen.oneOf(AutoruResellersRevalidationResolution.Value.values)
      comment <- StringGen
    } yield AutoruResellersRevalidationResolution.newBuilder
      .addAllValues(Seq(value).asJava)
      .setComment(comment)

  val RealtySuspiciousUsersResolutionGen: Gen[RealtySuspiciousUsersResolution.Builder] =
    for {
      value   <- Gen.oneOf(RealtySuspiciousUsersResolution.Value.values)
      comment <- StringGen
    } yield RealtySuspiciousUsersResolution.newBuilder
      .addAllValues(Seq(value).asJava)
      .setComment(comment)

  val AutoruCallMarkingResolutionGen: Gen[AutoruCallMarkingResolution.Builder] =
    for {
      value   <- Gen.oneOf(AutoruCallMarkingResolution.Value.values)
      extra   <- Gen.oneOf(AutoruCallMarkingResolution.ExtraValue.values)
      comment <- StringGen
    } yield AutoruCallMarkingResolution.newBuilder
      .addAllValues(Seq(value).asJava)
      .setExtraValue(extra)
      .setComment(comment)

  val RealtyChatSuspiciousCheckResolutionGen: Gen[RealtyChatSuspiciousCheckResolution.Builder] =
    for {
      value   <- Gen.oneOf(RealtyChatSuspiciousCheckResolution.Value.values)
      comment <- StringGen
    } yield RealtyChatSuspiciousCheckResolution.newBuilder
      .addAllValues(Seq(value).asJava)
      .setComment(comment)

  val AutoruResellerCleanNameResolutionValueGen: Gen[AutoruResellerCleanNameResolution.Value.Builder] =
    for {
      verdict <- Gen.oneOf(AutoruResellerCleanNameResolution.Value.Verdict.values)
      offerId <- StringGen
      vin     <- StringGen
    } yield AutoruResellerCleanNameResolution.Value.newBuilder
      .setVin(vin)
      .setOfferId(offerId)
      .setVerdict(verdict)

  val AutoruResellerCleanNameResolutionGen: Gen[AutoruResellerCleanNameResolution.Builder] =
    for {
      value   <- AutoruResellerCleanNameResolutionValueGen
      comment <- StringGen
    } yield AutoruResellerCleanNameResolution.newBuilder
      .addAllValues(Seq(value.build).asJava)
      .setComment(comment)

  private def makeResolution = Resolution.newBuilder.setVersion(1)

  implicit def trueFalseToResolution(rb: TrueFalseResolution.Builder): Resolution.Builder =
    makeResolution.setTrueFalse(rb)

  implicit def callCenterToResolution(rb: CallCenterResolution.Builder): Resolution.Builder =
    makeResolution.setCallCenter(rb)

  implicit def realtyVisualResolutionToResolution(rb: RealtyVisualResolution.Builder): Resolution.Builder =
    makeResolution.setRealtyVisual(rb)

  implicit def resellersCallResolutionToResolution(rb: ResellersCallResolution.Builder): Resolution.Builder =
    makeResolution.setResellersCall(rb)

  implicit def stoCallResolutionToResolution(rb: StoCallResolution.Builder): Resolution.Builder =
    makeResolution.setStoCall(rb)

  implicit def autoruVisualResolutionToResolution(rb: AutoruVisualResolution.Builder): Resolution.Builder =
    makeResolution.setAutoruVisual(rb)

  implicit def autoruCallResolutionToResolution(rb: AutoruCallResolution.Builder): Resolution.Builder =
    makeResolution.setAutoruCall(rb)

  implicit def stoComplaintsResolutionToResolution(rb: StoComplaintsResolution.Builder): Resolution.Builder =
    makeResolution.setStoComplaints(rb)

  implicit def suspiciousCallResolutionToResolution(rb: SuspiciousCallResolution.Builder): Resolution.Builder =
    makeResolution.setSuspiciousCall(rb)

  implicit def paidCallResolutionToResolution(rb: PaidCallResolution.Builder): Resolution.Builder =
    makeResolution.setPaidCallResolution(rb)

  implicit def autoruComplaintsResellerResolutionToResolution(
      rb: AutoruComplaintsResellerResolution.Builder
  ): Resolution.Builder = makeResolution.setAutoruComplaintsReseller(rb)

  implicit def redirectCheckResolutionToResolution(rb: RedirectCheckResolution.Builder): Resolution.Builder =
    makeResolution.setRedirectCheck(rb)

  implicit def autoruReviewsResolutionToResolution(rb: AutoruReviewsResolution.Builder): Resolution.Builder =
    makeResolution.setAutoruReviews(rb)

  implicit def autoruPurchasingResolutionToResolution(rb: AutoruPurchasingResolution.Builder): Resolution.Builder =
    makeResolution.setAutoruPurchasing(rb)

  implicit def teleponyMarkingResolutionToResolution(rb: TeleponyMarkingResolution.Builder): Resolution.Builder =
    makeResolution.setTeleponyMarking(rb)

  implicit def realtyComplaintsResellerResolutionToResolution(
      rb: RealtyComplaintsResellerResolution.Builder
  ): Resolution.Builder = makeResolution.setRealtyComplaintsReseller(rb)

  implicit def callCenterPhotoResolutionToResolution(rb: CallCenterPhotoResolution.Builder): Resolution.Builder =
    makeResolution.setCallCenterPhoto(rb)

  implicit def spamCallResolutionToResolution(rb: SpamCallResolution.Builder): Resolution.Builder =
    makeResolution.setSpamCall(rb)

  implicit def autoruTamagotchiResolutionToResolution(rb: AutoruTamagotchiResolution.Builder): Resolution.Builder =
    makeResolution.setAutoruTamagotchi(rb)

  implicit def atoruLoyaltyDealersResolutionToResolution(
      rb: AutoruLoyaltyDealersResolution.Builder
  ): Resolution.Builder = makeResolution.setAutoruLoyaltyDealers(rb)

  implicit def vinReportResolutionToResolution(rb: VinReportResolution.Builder): Resolution.Builder =
    makeResolution.setVinReport(rb)

  implicit def callgateResolutionToResolution(rb: CallgateResolution.Builder): Resolution.Builder =
    makeResolution.setCallgate(rb)

  implicit def autoruPremoderationDealerResolutionToResolution(
      rb: AutoruPremoderationDealerResolution.Builder
  ): Resolution.Builder = makeResolution.setAutoruPremoderationDealer(rb)

  implicit def realtyAgencyCardsCheckResolutionToResolution(
      rb: RealtyAgencyCardsCheckResolution.Builder
  ): Resolution.Builder = makeResolution.setRealtyAgencyCardsCheck(rb)

  implicit def autoruAnalyticUsersCheckResolutionToResolution(
      rb: AutoruAnalyticUsersCheckResolution.Builder
  ): Resolution.Builder = makeResolution.setAutoruAnalyticUsersCheck(rb)

  implicit def autoruProvenOwnerResolutionToResolution(rb: AutoruProvenOwnerResolution.Builder): Resolution.Builder =
    makeResolution.setAutoruProvenOwner(rb)

  implicit def autoruResellersRevalidationResolutionToResolution(
      rb: AutoruResellersRevalidationResolution.Builder
  ): Resolution.Builder = makeResolution.setAutoruResellersRevalidation(rb)

  implicit def realtySuspiciousUsersResolutionToResolution(
      rb: RealtySuspiciousUsersResolution.Builder
  ): Resolution.Builder = makeResolution.setRealtySuspiciousUsers(rb)

  implicit def autoruCallMarkingResolutionToResolution(rb: AutoruCallMarkingResolution.Builder): Resolution.Builder =
    makeResolution.setAutoruCallMarking(rb)

  implicit def realtyChatSuspiciousCheckResolutionToResolution(
      rb: RealtyChatSuspiciousCheckResolution.Builder
  ): Resolution.Builder = makeResolution.setRealtyChatSuspiciousCheck(rb)

  implicit def autoruResellerCleanNameResolutionToResolution(
      rb: AutoruResellerCleanNameResolution.Builder
  ): Resolution.Builder = makeResolution.setAutoruResellerCleanName(rb)
}
