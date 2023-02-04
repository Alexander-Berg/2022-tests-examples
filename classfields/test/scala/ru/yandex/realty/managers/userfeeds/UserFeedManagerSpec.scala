package ru.yandex.realty.managers.userfeeds

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.api.ProtoResponse.PartnerFeedsResponse
import ru.yandex.realty.clients.capa.PartnerStatus.PartnerStatus
import ru.yandex.realty.clients.capa.gen.PartnerGen
import ru.yandex.realty.clients.capa.{CapaAdminClient, CapaClient, PartnerStatus, Status, StatusInfo}
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.managers.feed.PartnersProtoFormats
import ru.yandex.realty.model.exception.ForbiddenException
import ru.yandex.realty.model.user.UserRefGenerators
import ru.yandex.vertis.scalamock.util._
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class UserFeedManagerSpec
  extends AsyncSpecBase
  with RequestAware
  with PropertyChecks
  with PartnersProtoFormats
  with PartnerGen
  with UserRefGenerators {

  private val capaClientMock: CapaClient = mock[CapaClient]
  private val capaAdminClientMock: CapaAdminClient = mock[CapaAdminClient]

  private val mockCapaClientGetPartners =
    toMockFunction2(capaClientMock.getPartners(_: String)(_: Traced))

  private val mockCapaAdminClientChangeStatus =
    toMockFunction6(
      capaAdminClientMock.changeStatus(_: String, _: Long, _: PartnerStatus, _: String, _: String)(_: Traced)
    )

  private val capaAdminOperatorUid = 1L

  implicit private def statusToPartnerStatus(status: Status): PartnerStatus = {
    status.enumStatus match {
      case Some(partnerStatus) => partnerStatus
      case _ => throw new IllegalStateException("cannot happen.")
    }
  }

  val manager: UserFeedManager =
    new DefaultUserFeedManager(capaClientMock, capaAdminClientMock, capaAdminOperatorUid)

  private val forbidden = new ForbiddenException("")
  private val notFound = new NoSuchElementException("")

  private val bannedPartnerStatus = PartnerStatus.Ban
  private val acceptedPartnerStatus = PartnerStatus.Accepted

  private def automaticCheckFailedStatusInfo: Long => StatusInfo = { uid =>
    StatusInfo(
      Status(PartnerStatus.AutomaticCheckFailed.id, PartnerStatus.AutomaticCheckFailed.toString),
      None,
      -1L,
      uid
    )
  }

  private def banStatusInfo: Long => StatusInfo = { uid =>
    StatusInfo(Status(PartnerStatus.Ban.id, PartnerStatus.Ban.toString), None, -1L, uid)
  }

  "UserFeedManager" when {
    "recheckFeed" should inSequence {
      "propagate missing/inapt authorization as AUTH_ERROR" in inSequence {
        forAll(partnerGen, passportUserGen) { (partner, userRef) =>
          val uid = userRef.uid

          mockCapaClientGetPartners
            .expects(uid.toString, *)
            .throwingF(forbidden)

          val result = withRequestContext(userRef) { implicit r =>
            manager.recheckFeed(userRef.uid, partner.partnerId)
          }

          val throwable = result.failed.futureValue
          throwable should be(forbidden)
        }
      }

      "propagate missing feed condition" in inSequence {
        forAll(partnerGen, passportUserGen) { (partner, userRef) =>
          val uid = userRef.uid

          mockCapaClientGetPartners
            .expects(uid.toString, *)
            .throwingF(notFound)

          val result = withRequestContext(userRef) { implicit r =>
            manager.recheckFeed(userRef.uid, partner.partnerId)
          }

          val throwable = result.failed.futureValue
          throwable should be(notFound)
        }
      }

      "conclude with default instance of PartnerFeedsResponse when successful" in inSequence {
        forAll(partnerGen, passportUserGen) { (partnerPrototype, userRef) =>
          val uid = userRef.uid
          val partner = partnerPrototype.copy(statusInfo = automaticCheckFailedStatusInfo(uid))

          mockCapaClientGetPartners
            .expects(uid.toString, *)
            .returningF(List(partner))

          mockCapaAdminClientChangeStatus
            .expects(capaAdminOperatorUid.toString, partner.partnerId, bannedPartnerStatus, *, *, *)
            .returningF(PartnerFeedsResponse.getDefaultInstance)

          mockCapaAdminClientChangeStatus
            .expects(capaAdminOperatorUid.toString, partner.partnerId, acceptedPartnerStatus, *, *, *)
            .returningF(PartnerFeedsResponse.getDefaultInstance)

          val result = withRequestContext(userRef) { implicit r =>
            manager.recheckFeed(userRef.uid, partner.partnerId)
          }

          result.futureValue should be(PartnerFeedsResponse.getDefaultInstance)
        }
      }

      "conclude with prefilled instance of PartnerFeedsResponse when feed is in inoperable state" in inSequence {
        forAll(partnerGen, passportUserGen) { (partnerPrototype, userRef) =>
          val uid = userRef.uid
          val partner = partnerPrototype.copy(statusInfo = banStatusInfo(uid))

          mockCapaClientGetPartners
            .expects(uid.toString, *)
            .returningF(List(partner))

          val result = withRequestContext(userRef) { implicit r =>
            manager.recheckFeed(userRef.uid, partner.partnerId)
          }

          result.failed.futureValue.getClass should be(classOf[IllegalArgumentException])
        }
      }
    }
  }

}
