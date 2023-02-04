package ru.yandex.realty.rent.backend.manager

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.WordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.rent.backend.converter.TermsConverter
import ru.yandex.realty.rent.dao.RentSpecBase
import ru.yandex.realty.rent.model.{ContractTerms, User}
import ru.yandex.realty.rent.proto.api.internal.terms.{
  InternalGetUserTerms,
  InternalHideTermsForUser,
  InternalShowTermsForUser
}
import ru.yandex.realty.rent.proto.api.terms.GetUserContractTermsResponse
import ru.yandex.realty.rent.proto.model.user.TenantTermsOfUse
import ru.yandex.realty.tracing.Traced
import ru.yandex.vertis.protobuf.BasicProtoFormats
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class TermsManagerSpec extends WordSpec with RentSpecBase with MockFactory {

  "TermsManager.getUserTerms" should {
    "return valid terms for user without last accepted terms" in new Wiring with Data {
      val user = userWithTermsGen(ContractTerms.v3).next
      (mockUserDao
        .findByUserId(_: String)(_: Traced))
        .expects(where { (id: String, _) =>
          id == user.userId
        })
        .once()
        .returning(Future.successful(user))
      val result: InternalGetUserTerms.Response = termsManager.getUserTerms(user.userId).futureValue
      val userContractTerms: GetUserContractTermsResponse = result.getUserContractTerms
      userContractTerms.hasLastTerms shouldBe true
      userContractTerms.hasAcceptedTerms shouldBe true
      userContractTerms.getLastTerms.getTerms shouldEqual TermsConverter.convertTerms(ContractTerms.latestTerms)
      userContractTerms.getLastTerms.getShouldShow shouldBe true
      userContractTerms.getAcceptedTerms shouldEqual TermsConverter.convertTerms(ContractTerms.v3)
      userContractTerms.getPaymentChangesTerms.getVersion shouldEqual user.getLastAcceptedPaymentChangesTenantTermsVersion.get
    }

    "return valid terms for user with last accepted terms" in new Wiring with Data {
      val user = userWithTermsGen(ContractTerms.latestTerms).next
      (mockUserDao
        .findByUserId(_: String)(_: Traced))
        .expects(where { (id: String, _) =>
          id == user.userId
        })
        .returning(Future.successful(user))
      val result: InternalGetUserTerms.Response = termsManager.getUserTerms(user.userId).futureValue
      val userContractTerms: GetUserContractTermsResponse = result.getUserContractTerms
      userContractTerms.hasAcceptedTerms shouldBe true
      userContractTerms.getAcceptedTerms shouldEqual TermsConverter.convertTerms(ContractTerms.latestTerms)
      userContractTerms.hasLastTerms shouldBe false
      userContractTerms.getPaymentChangesTerms.getVersion shouldEqual user.getLastAcceptedPaymentChangesTenantTermsVersion.get
    }

    "hide last accepted terms" in new Wiring with Data {
      val user = userWithTermsGen(ContractTerms.v3).next
      (mockUserDao
        .findByUserId(_: String)(_: Traced))
        .expects(where { (id: String, _) =>
          id == user.userId
        })
        .once()
        .returning(Future.successful(user))
      (mockUserDao
        .update(_: Long)(_: User => User)(_: Traced))
        .expects(user.uid, *, *)
        .once()
        .returning(Future.successful(user))
      val result: InternalHideTermsForUser.Response = termsManager.hideTermsForUser(user.userId, 4).futureValue
      result.hasSuccess shouldBe true
    }

    "show last accepted terms" in new Wiring with Data {
      val user: User = userWithTermsGen(ContractTerms.v3).next
      val userWithHiddenTerms: User = user.copy(
        data = user.data.toBuilder
          .setTenantTermsOfUse(user.data.getTenantTermsOfUse.toBuilder.addHiddenTermsVersions(4).build())
          .build()
      )
      (mockUserDao
        .findByUserId(_: String)(_: Traced))
        .expects(where { (id: String, _) =>
          id == user.userId
        })
        .once()
        .returning(Future.successful(userWithHiddenTerms))
      (mockUserDao
        .update(_: Long)(_: User => User)(_: Traced))
        .expects(user.uid, *, *)
        .once()
        .returning(Future.successful(user))
      val result: InternalShowTermsForUser.Response = termsManager.showTermsForUser(user.userId, 4).futureValue
      result.hasSuccess shouldBe true
    }
  }

  trait Data extends BasicProtoFormats {
    this: Wiring =>

    def userWithTermsGen(terms: ContractTerms) =
      for {
        user <- userGen(true)
        userWithNotLastTermsAccepted = user.copy(
          data = user.data.toBuilder
            .setTenantTermsOfUse(
              TenantTermsOfUse
                .newBuilder()
                .setAgreementDate(DateTimeFormat.write(DateTimeUtil.now()))
                .setAcceptedTermsVersion(terms.version)
                .build()
            )
            .build()
        )
      } yield userWithNotLastTermsAccepted
  }
}
