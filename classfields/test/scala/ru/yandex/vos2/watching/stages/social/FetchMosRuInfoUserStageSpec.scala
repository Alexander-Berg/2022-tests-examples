package ru.yandex.vos2.watching.stages.social

import java.nio.charset.StandardCharsets
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.clients.social.{SocialClient, SocialRawInfo}
import ru.yandex.realty.proto.social.trusted.MosRuTrustedStatus
import ru.yandex.realty.util.CryptoUtils
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.UserModel
import ru.yandex.vos2.dao.offers.OfferDao
import ru.yandex.vos2.model.CommonGen
import ru.yandex.vos2.model.user.UserGenerator
import ru.yandex.vos2.watching.stages.social.FetchMosRuInfoUserStageSpec._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class FetchMosRuInfoUserStageSpec extends WordSpec with Matchers with MockitoSupport with PropertyChecks {
  private val randomAes256KeyBase64 = "Qj9FKEgrTWJRZVRoV21acTR0N3cheiVDKkYpSkBOY1I="
  private val crypto: CryptoUtils.Crypto = CryptoUtils.Crypto.create(randomAes256KeyBase64)
  private val socialClient = mock[SocialClient]
  private val offerDao = mock[OfferDao]
  private val fetchService = new SocialInfoFetcher(socialClient, crypto)
  private val stage = new FetchMosRuInfoUserStage(offerDao, fetchService)
  private val taskId = "123"

  "FetchMosRuInfoUserStage" should {
    "inc attempts count on failure" in {
      forAll(UserGenerator.NewUserGen) { user =>
        when(socialClient.getRawInfo(?)).thenReturn(Future.failed(new Exception()))
        when(offerDao.revisitUsersOffers(?, ?)(?)).thenReturn(eq(1))
        user.getTrustedUserInfo.getRequestAttemptsCount shouldBe 0
        val res = stage.processUser(user.setUserOwner.setTaskId(taskId))
        res.getUpdate.get.getTrustedUserInfo.getRequestAttemptsCount shouldBe 1
        res.getUpdate.get.getTrustedUserInfo.getMosRuTrustedStatus shouldBe MosRuTrustedStatus.REQUESTED
      }
    }

    "reset attempts count and status on 9 failure " in {
      forAll(UserGenerator.NewUserGen) { user =>
        when(socialClient.getRawInfo(?)).thenReturn(Future.failed(new Exception()))
        when(offerDao.revisitUsersOffers(?, ?)(?)).thenReturn(eq(1))
        val res = stage.processUser(
          user.setUserOwner.setTaskId(taskId).setAttemptsCount(9)
        )
        res.getUpdate.get.getTrustedUserInfo.getRequestAttemptsCount shouldBe 0
        res.getUpdate.get.getTrustedUserInfo.getMosRuTrustedStatus shouldBe MosRuTrustedStatus.REQUESTING_ERROR
      }
    }

    "update status on success response" in {
      forAll(
        UserGenerator.NewUserGen,
        CommonGen.genRusString(5),
        CommonGen.genRusString(7),
        CommonGen.genRusString(6),
        CommonGen.BoolGen
      ) { (user, lastName, middleName, firstName, trusted) =>
        when(socialClient.getRawInfo(?))
          .thenReturn(Future(SocialRawInfo(lastName, middleName, firstName, trusted = trusted)))
        when(offerDao.revisitUsersOffers(?, ?)(?)).thenReturn(eq(1))
        val res = stage.processUser(
          user.setUserOwner.setTaskId(taskId)
        )
        res.getUpdate.get.getTrustedUserInfo.getRequestAttemptsCount shouldBe 0
        res.getUpdate.get.getTrustedUserInfo.getMosRuTrustedStatus shouldBe getTrustedStatus(trusted)
        decrypt(res.getUpdate.get.getTrustedUserInfo.getMosRuPersonEncrypted.getName) shouldBe firstName
        decrypt(res.getUpdate.get.getTrustedUserInfo.getMosRuPersonEncrypted.getPatronymic) shouldBe middleName
        decrypt(res.getUpdate.get.getTrustedUserInfo.getMosRuPersonEncrypted.getSurname) shouldBe lastName
      }
    }

    def getTrustedStatus(trusted: Boolean) = if (trusted) MosRuTrustedStatus.TRUSTED else MosRuTrustedStatus.NOT_TRUSTED
    def decrypt(value: String): String = {
      new String(crypto.decrypt(value), StandardCharsets.UTF_8)
    }
  }
}

object FetchMosRuInfoUserStageSpec {

  implicit class UserUpdate(user: UserModel.User) {

    def setUserOwner: UserModel.User = {
      user.toBuilder
        .setUserType(UserModel.UserType.UT_OWNER)
        .build()
    }

    def setTaskId(taskId: String): UserModel.User = {
      user.toBuilder
        .setTrustedUserInfo(
          user.getTrustedUserInfo.toBuilder.setMosRuTaskId(taskId)
        )
        .build()
    }

    def setAttemptsCount(count: Int): UserModel.User = {
      user.toBuilder
        .setTrustedUserInfo(
          user.getTrustedUserInfo.toBuilder.setRequestAttemptsCount(count)
        )
        .build()
    }
  }

}
