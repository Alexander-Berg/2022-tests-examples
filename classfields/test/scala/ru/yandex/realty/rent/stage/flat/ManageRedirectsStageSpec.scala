package ru.yandex.realty.rent.stage.flat

import java.time.Duration
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.clients.maps.RentPolygon
import ru.yandex.realty.model.phone.{PhoneRedirect, TeleponyInfo}
import ru.yandex.realty.util.Mappings._
import ru.yandex.realty.model.serialization.phone.PhoneRedirectProtoConverter
import ru.yandex.realty.phone.AbstractRedirectPhoneService
import ru.yandex.realty.proto.phone.PhoneRedirectMessage
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.rent.model.enums.OwnerRequestStatus
import ru.yandex.realty.rent.model.Flat
import ru.yandex.realty.rent.proto.api.moderation.ClassifiedTypeNamespace
import ru.yandex.realty.rent.proto.api.moderation.ClassifiedTypeNamespace.ClassifiedType.{
  AVITO,
  CIAN,
  JCAT,
  YANDEX_REALTY
}
import ru.yandex.realty.rent.proto.model.flat.{ClassifiedRedirectNumber, ClassifiedsPubStatusInternal}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching.ProcessingState

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

@RunWith(classOf[JUnitRunner])
class ManageRedirectsStageSpec extends AsyncSpecBase with RentModelsGen {

  private val redirectPhoneService = mock[AbstractRedirectPhoneService]

  implicit val traced: Traced = Traced.empty

  private def invokeStage(flat: Flat): ProcessingState[Flat] = {
    val state = ProcessingState(flat)
    val stage = new ManageRedirectsStage(redirectPhoneService) {
      override protected def isProduction: Boolean = true
    }
    stage.process(state).futureValue
  }

  private def createRedirectProto(deadline: Long, objectId: String = "") =
    PhoneRedirectMessage
      .newBuilder()
      .setId(readableString.next)
      .setTarget("123456")
      .setObjectId(objectId)
      .setDeadline(deadline)
      .setSource(readableString.next)
      .build()

  private def createRedirect(deadline: Long, objectId: String = "") =
    PhoneRedirectProtoConverter.fromMessage(createRedirectProto(deadline, objectId))

  private def getClassifiedsPubStatuses(
    enabledClassifieds: Set[ClassifiedTypeNamespace.ClassifiedType]
  ): Seq[ClassifiedsPubStatusInternal] =
    ClassifiedTypeNamespace.ClassifiedType
      .values()
      .toSeq
      .filterNot(_ == ClassifiedTypeNamespace.ClassifiedType.UNKNOWN)
      .filterNot(_ == ClassifiedTypeNamespace.ClassifiedType.UNRECOGNIZED)
      .map(
        classifiedType =>
          ClassifiedsPubStatusInternal
            .newBuilder()
            .setEnabled(enabledClassifieds.contains(classifiedType))
            .setClassifiedType(classifiedType)
            .build()
      )

  "ManageRedirectsStage" should {
    "request redirects only for enabled classifieds" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      val pubilshedFlat = flat.copy(
        data = flat.data.toBuilder
          .clearClassifiedsPubStatuses()
          .addAllClassifiedsPubStatuses(getClassifiedsPubStatuses(Set(YANDEX_REALTY, AVITO)).asJava)
          .build()
      )

      val rentPolygon = flat.subjectFederationId.flatMap(RentPolygon.fromSubjectFederationId).getOrElse(RentPolygon.MSK)

      val yandexRedirect = createRedirect(11)
      val avitoRedirect = createRedirect(12)
      val yandexRedirectProto = PhoneRedirectProtoConverter.toMessage(yandexRedirect)
      val avitoRedirectProto = PhoneRedirectProtoConverter.toMessage(avitoRedirect)

      (redirectPhoneService
        .createRedirectAsync(_: TeleponyInfo, _: FiniteDuration)(_: Traced))
        .expects(where { (t: TeleponyInfo, _, _) =>
          val expectedTag = s"polygon=${rentPolygon.value}#classified=YANDEX_REALTY"
          t.objectId == flat.flatId && t.tag.contains(expectedTag) && t.target == rentPolygon.phone.yandex
        })
        .returning(Future.successful(yandexRedirect))

      (redirectPhoneService
        .createRedirectAsync(_: TeleponyInfo, _: FiniteDuration)(_: Traced))
        .expects(where { (t: TeleponyInfo, _, _) =>
          val expectedTag = s"polygon=${rentPolygon.value}#classified=AVITO"
          t.objectId == flat.flatId && t.tag.contains(expectedTag) && t.target == rentPolygon.phone.avito
        })
        .returning(Future.successful(avitoRedirect))

      // JCAT uses classified pub status from YANDEX
      (redirectPhoneService
        .createRedirectAsync(_: TeleponyInfo, _: FiniteDuration)(_: Traced))
        .expects(where { (t: TeleponyInfo, _, _) =>
          val expectedTag = s"polygon=${rentPolygon.value}#classified=JCAT"
          t.objectId == flat.flatId && t.tag.contains(expectedTag) && t.target == rentPolygon.phone.jcat
        })
        .returning(Future.successful(avitoRedirect))

      val state = invokeStage(pubilshedFlat)
      state.entry.data.getRedirectNumbersList.asScala.filter(_.hasRedirect).toSet shouldBe Set(
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(YANDEX_REALTY).setRedirect(yandexRedirectProto).build(),
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(AVITO).setRedirect(avitoRedirectProto).build(),
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(JCAT).setRedirect(avitoRedirectProto).build()
      )
      state.entry.data.getRedirectNumbersList.asScala.filter(_.hasPreviousRedirect) shouldBe empty
      state.entry.visitTime shouldBe defined
    }

    "remove redirects if all classifieds disabled" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      val avitoRedirectProto = PhoneRedirectProtoConverter.toMessage(createRedirect(21, flat.flatId))

      val lookingForTenantFlat = flat.copy(
        data = flat.data.toBuilder
          .clearClassifiedsPubStatuses()
          .addAllClassifiedsPubStatuses(getClassifiedsPubStatuses(Set()).asJava)
          .addRedirectNumbers(ClassifiedRedirectNumber.newBuilder().setClassifiedType(CIAN).build())
          .addRedirectNumbers(
            ClassifiedRedirectNumber.newBuilder().setClassifiedType(AVITO).setRedirect(avitoRedirectProto).build()
          )
          .build()
      )

      val state = invokeStage(lookingForTenantFlat)
      state.entry.data.getRedirectNumbersList.asScala.filter(_.hasRedirect) shouldBe empty
      state.entry.data.getRedirectNumbersList.asScala.filter(_.hasPreviousRedirect).toSet shouldBe Set(
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(AVITO).setPreviousRedirect(avitoRedirectProto).build()
      )
      state.entry.visitTime shouldBe empty
    }

    "manage redirect numbers list if status is not LookingForTenant" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.Completed).next
      val avitoRedirectProto = PhoneRedirectProtoConverter.toMessage(createRedirect(31, flat.flatId))
      val cianRedirectProto = PhoneRedirectProtoConverter.toMessage(createRedirect(32, flat.flatId))

      val rentedFlat = flat
        .copy(
          data = flat.data.toBuilder
            .clearClassifiedsPubStatuses()
            .addAllClassifiedsPubStatuses(getClassifiedsPubStatuses(Set(CIAN, AVITO)).asJava)
            .addRedirectNumbers(
              ClassifiedRedirectNumber.newBuilder().setClassifiedType(CIAN).setRedirect(cianRedirectProto).build()
            )
            .addRedirectNumbers(
              ClassifiedRedirectNumber.newBuilder().setClassifiedType(AVITO).setRedirect(avitoRedirectProto).build()
            )
            .build()
        )

      val state = invokeStage(rentedFlat)
      state.entry.data.getRedirectNumbersList.asScala.filter(_.hasRedirect) shouldBe empty
      state.entry.data.getRedirectNumbersList.asScala.filter(_.hasPreviousRedirect).toSet shouldBe Set(
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(CIAN).setPreviousRedirect(cianRedirectProto).build(),
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(AVITO).setPreviousRedirect(avitoRedirectProto).build()
      )
      state.entry.visitTime shouldBe empty
    }

    "Save previous redirect" in {
      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next
      val publishedFlat = flat.copy(
        data = flat.data.toBuilder
          .clearClassifiedsPubStatuses()
          .addAllClassifiedsPubStatuses(getClassifiedsPubStatuses(Set(CIAN)).asJava)
          .build()
      )

      val firstRedirect = createRedirect(41)
      val firstRedirectProto = PhoneRedirectProtoConverter.toMessage(firstRedirect)

      val firstRedirectRefreshed = firstRedirect.copy(deadline = Some(firstRedirect.deadline.get.plusHours(1)))
      val firstRedirectRefreshedProto = PhoneRedirectProtoConverter.toMessage(firstRedirectRefreshed)

      val secondRedirect = createRedirect(42)
      val secondRedirectProto = PhoneRedirectProtoConverter.toMessage(secondRedirect)

      val thirdRedirect = createRedirect(43)
      val thirdRedirectProto = PhoneRedirectProtoConverter.toMessage(thirdRedirect)

      def expectCreateRedirect(returns: PhoneRedirect): Unit =
        (redirectPhoneService
          .createRedirectAsync(_: TeleponyInfo, _: FiniteDuration)(_: Traced))
          .expects(*, *, *)
          .returning(Future.successful(returns))

      def expectRemoveRedirect: Unit =
        (redirectPhoneService
          .removeRedirect(_: String, _: String, _: String)(_: Traced))
          .expects(*, *, *, *)
          .returning(Future.unit)

      // redirects in flat:         1    -> 1 (updated) -> 2           -> NONE -> 3
      // expected prev. redirects:  NONE -> NONE        -> 1 (updated) -> 2    -> 2

      inSequence {
        expectCreateRedirect(firstRedirect)
        expectCreateRedirect(firstRedirectRefreshed)
        expectCreateRedirect(secondRedirect)
        expectCreateRedirect(thirdRedirect)
      }
      val expectedCurrentRedirects = List(
        Some(firstRedirectProto),
        Some(firstRedirectRefreshedProto),
        Some(secondRedirectProto),
        None,
        Some(thirdRedirectProto)
      )
      val expectedPrevRedirects =
        List(None, None, Some(firstRedirectRefreshedProto), Some(secondRedirectProto), Some(secondRedirectProto))

      List(0, 1, 2, 3, 4).foldLeft(publishedFlat) {
        case (prevFlat, i) =>
          val expectedCurrent = expectedCurrentRedirects(i)
          val expectedPrevious = expectedPrevRedirects(i)
          val prevFlatFixed = if (i == 3) {
            prevFlat.copy(
              ownerRequests = Seq(flat.ownerRequests.head.updateStatus(status = OwnerRequestStatus.Completed))
            )
          } else if (i == 4) {
            prevFlat.copy(
              ownerRequests = Seq(flat.ownerRequests.head.updateStatus(status = OwnerRequestStatus.LookingForTenant))
            )
          } else {
            prevFlat
          }
          val state = invokeStage(prevFlatFixed)
          state.entry.data.getRedirectNumbersList.asScala
            .filter(_.getClassifiedType == CIAN)
            .toSet shouldBe Set(
            ClassifiedRedirectNumber
              .newBuilder()
              .setClassifiedType(CIAN)
              .applyTransforms[PhoneRedirectMessage](expectedCurrent, _.setRedirect(_))
              .applyTransforms[PhoneRedirectMessage](expectedPrevious, _.setPreviousRedirect(_))
              .build()
          )
          state.entry
      }
    }

    "preserve redirects on error in any way" in {
      val currentTime = System.currentTimeMillis()
      val soonTimeMills = currentTime + Duration.ofMinutes(10).toMillis
      val notSoonTimeMillis = currentTime + Duration.ofDays(3).toMillis

      val flat = flatWithOwnerRequest(OwnerRequestStatus.LookingForTenant).next

      // should not be deleted on fail (deadline is not soon)
      val yandexRedirectProto = PhoneRedirectProtoConverter.toMessage(createRedirect(notSoonTimeMillis, flat.flatId))
      val yandexRedirectNumber =
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(YANDEX_REALTY).setRedirect(yandexRedirectProto).build()

      // should be deleted on fail (deadline is soon)
      val cianRedirectProto = PhoneRedirectProtoConverter.toMessage(createRedirect(soonTimeMills, flat.flatId))
      val cianRedirectNumber =
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(CIAN).setRedirect(cianRedirectProto).build()

      // should not be deleted on fail (deadline is not soon)
      val jcatRedirectProto = PhoneRedirectProtoConverter.toMessage(createRedirect(notSoonTimeMillis, flat.flatId))
      val jcatRedirectNumber =
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(JCAT).setRedirect(jcatRedirectProto).build()

      // should be updated (request will not fail)
      val avitoRedirectProto = PhoneRedirectProtoConverter.toMessage(createRedirect(notSoonTimeMillis, flat.flatId))
      val avitoRedirectNumber =
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(AVITO).setRedirect(avitoRedirectProto).build()

      val newAvitoRedirect = createRedirect(notSoonTimeMillis + 10, flat.flatId)
      val newAvitoRedirectProto = PhoneRedirectProtoConverter.toMessage(newAvitoRedirect)
      val newAvitoRedirectNumber =
        ClassifiedRedirectNumber.newBuilder().setClassifiedType(AVITO).setRedirect(newAvitoRedirectProto).build()
      val newAvitoRedirectNumberWithPreviousRedirect =
        newAvitoRedirectNumber.toBuilder.setPreviousRedirect(avitoRedirectProto).build()

      val publishedFlat = flat
        .copy(
          data = flat.data.toBuilder
            .clearClassifiedsPubStatuses()
            .addAllClassifiedsPubStatuses(getClassifiedsPubStatuses(Set(YANDEX_REALTY, CIAN, AVITO, JCAT)).asJava)
            .addRedirectNumbers(yandexRedirectNumber)
            .addRedirectNumbers(cianRedirectNumber)
            .addRedirectNumbers(avitoRedirectNumber)
            .addRedirectNumbers(jcatRedirectNumber)
            .build()
        )

      val rentPolygon = flat.subjectFederationId.flatMap(RentPolygon.fromSubjectFederationId).getOrElse(RentPolygon.MSK)

      (redirectPhoneService
        .createRedirectAsync(_: TeleponyInfo, _: FiniteDuration)(_: Traced))
        .expects(where { (t: TeleponyInfo, _, _) =>
          t.target == rentPolygon.phone.yandex
        })
        .returning(Future.failed(new RuntimeException("fail 1")))

      (redirectPhoneService
        .createRedirectAsync(_: TeleponyInfo, _: FiniteDuration)(_: Traced))
        .expects(where { (t: TeleponyInfo, _, _) =>
          t.target == rentPolygon.phone.cian
        })
        .returning(Future.failed(new RuntimeException("fail 2")))

      (redirectPhoneService
        .createRedirectAsync(_: TeleponyInfo, _: FiniteDuration)(_: Traced))
        .expects(where { (t: TeleponyInfo, _, _) =>
          t.target == rentPolygon.phone.avito
        })
        .returning(Future.successful(newAvitoRedirect))

      (redirectPhoneService
        .createRedirectAsync(_: TeleponyInfo, _: FiniteDuration)(_: Traced))
        .expects(where { (t: TeleponyInfo, _, _) =>
          t.target == rentPolygon.phone.jcat
        })
        .returning(Future.failed(new RuntimeException("fail 3")))

      val state = invokeStage(publishedFlat)
      state.entry.data.getRedirectNumbersList.asScala.filter(_.hasRedirect).toSet shouldBe
        Set(yandexRedirectNumber, newAvitoRedirectNumberWithPreviousRedirect, jcatRedirectNumber, cianRedirectNumber)

      state.entry.visitTime shouldBe defined
      // revisit on error
      state.entry.visitTime.get
        .plusMillis(ru.yandex.realty.rent.stage.flat.RevisitMaxDelay.toMillis.toInt + 10000)
        .isBeforeNow
    }
  }

}
