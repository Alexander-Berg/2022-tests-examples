package ru.yandex.realty.rent.backend.manager

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.realty.clients.searcher.gen.SearcherResponseModelGenerators
import ru.yandex.realty.geohub.api.GeohubApi.{UnifyLocationRequest, UnifyLocationResponse}
import ru.yandex.realty.http.RequestAware
import ru.yandex.realty.logging.TracedLogging
import ru.yandex.realty.model.message.RealtySchema.{LocationMessage, RawLocationMessage}
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.proto.PersonFullName
import ru.yandex.realty.rent.model.enums.{OwnerRequestStatus, Role}
import ru.yandex.realty.rent.model.{Flat, OwnerRequest, User, UserFlat}
import ru.yandex.realty.rent.proto.api.flats.UpdateFlatDraftRequest
import ru.yandex.realty.telepony.PhoneInfo
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, SpecBase}

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FlatDraftManagerSpec
  extends SpecBase
  with AsyncSpecBase
  with RequestAware
  with ScalaCheckPropertyChecks
  with SearcherResponseModelGenerators
  with TracedLogging {

  "FlatDraftManagerSpec.upsertFlatDraft" should {
    "insert successfully when geohub request succeeded" in new Wiring with Data with FlatDraftManagerData {

      val geohubRequest = UnifyLocationRequest
        .newBuilder()
        .setRawLocation(RawLocationMessage.newBuilder().setAddress("address"))
        .build

      (mockGeohubClient
        .unifyLocation(_: UnifyLocationRequest)(_: Traced))
        .expects(geohubRequest, *)
        .once()
        .returning(
          Future.successful(
            UnifyLocationResponse
              .newBuilder()
              .setLocation(LocationMessage.newBuilder().setSubjectFederationId(1).build())
              .build()
          )
        )

      val result = flatDraftManager.upsertFlatDraft(passportUser, request).futureValue

      result.getFlat.getAddress.getAddress shouldBe "address"
      result.getFlat.getAddress.getSubjectFederationId shouldBe 1
    }
    "insert successfully when geohub request failed" in new Wiring with Data with FlatDraftManagerData {

      (mockGeohubClient
        .unifyLocation(_: UnifyLocationRequest)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.failed(new RuntimeException))

      val result = flatDraftManager.upsertFlatDraft(passportUser, request).futureValue

      result.getFlat.getAddress.getAddress shouldBe "address"
      result.getFlat.getAddress.getSubjectFederationId shouldBe 0
    }
    "update successfully" in new Wiring with Data {

      val flat = flatGen(false).next.copy(
        ownerRequests = Seq(ownerRequestGen.next.copy(status = OwnerRequestStatus.Draft))
      )

      val user: User = userGen(false).next.copy(
        assignedFlats = Map(
          Role.Owner -> Seq(flat)
        )
      )

      val passportUser = PassportUser(user.uid)

      val request = UpdateFlatDraftRequest
        .newBuilder()
        .setAddress("address")
        .setFlatNumber("flatNumber")
        .setPhone(sampleTenantPhone)
        .setEmail("email")
        .setPerson(PersonFullName.newBuilder().setName("name").setSurname("surname").build())
        .build()

      val geohubRequest = UnifyLocationRequest
        .newBuilder()
        .setRawLocation(RawLocationMessage.newBuilder().setAddress("address"))
        .build

      (mockPhoneUnifierClient
        .unify(_: String))
        .expects(sampleTenantPhone)
        .returning(PhoneInfo(sampleTenantPhone, 0, ""))

      (mockUserDao
        .findByUidOpt(_: Long, _: Boolean)(_: Traced))
        .expects(user.uid, *, *)
        .once()
        .returning(Future.successful(Some(user)))

      (mockGeohubClient
        .unifyLocation(_: UnifyLocationRequest)(_: Traced))
        .expects(geohubRequest, *)
        .once()
        .returning(
          Future.successful(
            UnifyLocationResponse
              .newBuilder()
              .setLocation(LocationMessage.newBuilder().setSubjectFederationId(1).build())
              .build()
          )
        )

      (flatDao
        .update(_: String, _: Boolean)(_: Flat => Flat)(_: Traced))
        .expects(flat.flatId, *, *, *)
        .once()
        .returning(Future.successful(flat))

      flatDraftManager.upsertFlatDraft(passportUser, request).futureValue
    }
  }

  trait FlatDraftManagerData { this: Wiring with Data =>

    val user: User = userGen(false).next

    val passportUser = PassportUser(user.uid)

    val request = UpdateFlatDraftRequest
      .newBuilder()
      .setAddress("address")
      .setFlatNumber("flatNumber")
      .setPhone(sampleTenantPhone)
      .setEmail("email")
      .setPerson(PersonFullName.newBuilder().setName("name").setSurname("surname").build())
      .build()

    (mockPhoneUnifierClient
      .unify(_: String))
      .expects(sampleTenantPhone)
      .returning(PhoneInfo(sampleTenantPhone, 0, ""))

    (mockUserDao
      .findByUidOpt(_: Long, _: Boolean)(_: Traced))
      .expects(user.uid, *, *)
      .once()
      .returning(Future.successful(Some(user)))

    (mockUserDao
      .findByUid(_: Long)(_: Traced))
      .expects(user.uid, *)
      .once()
      .returning(Future.successful(user))

    (flatDao
      .create(_: Flat)(_: Traced))
      .expects(*, *)
      .once()
      .returning(Future.unit)

    (ownerRequestDao
      .create(_: Iterable[OwnerRequest])(_: Traced))
      .expects(*, *)
      .once()
      .returning(Future.unit)

    (flatDao
      .create(_: Flat)(_: Traced))
      .expects(*, *)
      .once()
      .returning(Future.unit)

    (mockUserFlatDao
      .create(_: UserFlat)(_: Traced))
      .expects(*, *)
      .once()
      .returning(Future.unit)
  }

}
