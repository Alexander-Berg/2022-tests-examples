package ru.auto.api.services.vox

import org.mockito.ArgumentMatchers.{eq => eq_}
import org.mockito.Mockito.reset
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.scalacheck.Arbitrary
import ru.auto.api.BaseSpec
import ru.auto.api.CommonModel.ClientFeature
import ru.auto.api.features.FeatureManager
import ru.auto.api.model.ModelGenerators.PrivateUserRefGen
import ru.auto.api.model.gen.BasicGenerators.readableString
import ru.auto.api.model.{AutoruUser, UserInfo}
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.settings.SettingsClient
import ru.auto.api.services.vox.VoxClient.{AddUserResponse, ErrorDetails, ErrorResponse}
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eeq}
import ru.yandex.vertis.tracing.Traced

import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter
import scala.concurrent.Future

class VoxManagerTest extends BaseSpec with MockitoSupport {

  "signUpUser" should {
    "add user to vox: feature enabled, no vox username in settings, client feature not supported" in new Wiring {
      reset(mockRequest)
      when(mockRequest.user).thenReturn(mockUser)
      when(mockRequest.isSupported(ClientFeature.APP2APP_SUPPORT)).thenReturn(false)
      when(voxCheckFeature.value).thenReturn(true)

      whenReady(manager.signUpUser(mockRequest)) { result =>
        result.getUsername shouldBe usernameInVox

        verifyNoMoreInteractions(voxClient)
        verifyNoMoreInteractions(settingsClient)
      }
    }

    "add user to vox: feature enabled, no vox username in settings" in new Wiring {
      when(voxCheckFeature.value).thenReturn(true)

      when(voxClient.addUser(?, ?, ?)(?)).thenReturnF(Right(AddUserResponse(nextLong, nextLong)))
      when(settingsClient.getSettings(eeq(SettingsClient.SettingsDomain), eeq(privateUser))(?))
        .thenReturn(Future.successful(Map.empty[String, String]))
      when(
        settingsClient.updateSettings(
          eeq(SettingsClient.SettingsDomain),
          eeq(privateUser),
          eeq(Map("vox_username" -> usernameInVox))
        )(?)
      ).thenReturn(Future.successful(Map.empty[String, String]))

      whenReady(manager.signUpUser(mockRequest)) { result =>
        result.getUsername shouldBe usernameInVox

        verify(voxClient).addUser(eq_(usernameInVox), eq_(usernameInVox), eq_(passwordInVox))(any())
        verify(settingsClient).getSettings(eeq(SettingsClient.SettingsDomain), eeq(privateUser))(any())
        verify(settingsClient).updateSettings(
          eeq(SettingsClient.SettingsDomain),
          eeq(privateUser),
          eeq(Map("vox_username" -> usernameInVox))
        )(any())
        verifyNoMoreInteractions(voxClient)
        verifyNoMoreInteractions(settingsClient)
      }
    }

    "add user to vox: feature enabled, vox username exists in settings" in new Wiring {
      when(voxCheckFeature.value).thenReturn(true)

      when(voxClient.addUser(?, ?, ?)(?))
        .thenReturnF(Right(AddUserResponse(nextLong, nextLong)))
      when(settingsClient.getSettings(eeq(SettingsClient.SettingsDomain), eeq(privateUser))(?))
        .thenReturn(Future.successful(Map("vox_username" -> usernameInVox)))

      whenReady(manager.signUpUser(mockRequest)) { result =>
        result.getUsername shouldBe usernameInVox

        verifyNoMoreInteractions(voxClient)
        verify(settingsClient).getSettings(eeq(SettingsClient.SettingsDomain), eeq(privateUser))(any())
        verifyNoMoreInteractions(settingsClient)
      }
    }

    "add user to vox: feature disabled" in new Wiring {
      when(voxCheckFeature.value).thenReturn(false)

      when(voxClient.addUser(?, ?, ?)(?))
        .thenReturnF(Right(AddUserResponse(nextLong, nextLong)))

      whenReady(manager.signUpUser(mockRequest)) { result =>
        result.getUsername shouldBe usernameInVox

        verifyNoMoreInteractions(voxClient)
        verifyNoMoreInteractions(settingsClient)
      }
    }

    "ignore error when user already exists" in new Wiring {
      when(voxCheckFeature.value).thenReturn(true)

      when(voxClient.addUser(?, ?, ?)(?))
        .thenReturnF(Left(ErrorResponse(ErrorDetails("User already exists", nonUniqueUserErrorCode))))
      when(settingsClient.getSettings(eeq(SettingsClient.SettingsDomain), eeq(privateUser))(?))
        .thenReturn(Future.successful(Map.empty[String, String]))
      when(
        settingsClient.updateSettings(
          eeq(SettingsClient.SettingsDomain),
          eeq(privateUser),
          eeq(Map("vox_username" -> usernameInVox))
        )(?)
      ).thenReturn(Future.successful(Map.empty[String, String]))

      whenReady(manager.signUpUser(mockRequest)) { result =>
        result.getUsername shouldBe usernameInVox

        verify(voxClient).addUser(eq_(usernameInVox), eq_(usernameInVox), eq_(passwordInVox))(any())
        verify(settingsClient).getSettings(eeq(SettingsClient.SettingsDomain), eeq(privateUser))(any())
        verify(settingsClient).updateSettings(
          eeq(SettingsClient.SettingsDomain),
          eeq(privateUser),
          eeq(Map("vox_username" -> usernameInVox))
        )(any())
        verifyNoMoreInteractions(voxClient)
        verifyNoMoreInteractions(settingsClient)
      }
    }

    "throw exception when error code is unknown" in new Wiring {
      when(voxCheckFeature.value).thenReturn(true)

      val errorCode = nextLong
      assert(errorCode != nonUniqueUserErrorCode)

      when(voxClient.addUser(?, ?, ?)(?))
        .thenReturnF(Left(ErrorResponse(ErrorDetails("unknown error message", errorCode))))
      when(settingsClient.getSettings(eeq(SettingsClient.SettingsDomain), eeq(privateUser))(?))
        .thenReturn(Future.successful(Map.empty[String, String]))

      whenReady(manager.signUpUser(mockRequest).failed) { th =>
        th.getMessage shouldEqual s"Unexpected vox response: ${ErrorDetails("unknown error message", errorCode)}"

        verify(voxClient).addUser(eq_(usernameInVox), eq_(usernameInVox), eq_(passwordInVox))(any())
        verify(settingsClient).getSettings(eeq(SettingsClient.SettingsDomain), eeq(privateUser))(any())
        verifyNoMoreInteractions(voxClient)
        verifyNoMoreInteractions(settingsClient)
      }
    }

    "generateOtt" should {
      "generate one time token by template from vox" in new Wiring {
        val oneTimeLogin = "login"

        def MD5(s: String): String = {
          val hasher = MessageDigest.getInstance("MD5")
          hasher.update(s.getBytes())
          DatatypeConverter.printHexBinary(hasher.digest()).toLowerCase()
        }

        def calcVoxHash(login_key: String, myuser: String, mypass: String): String = {
          // formula is copy-pasted from https://voximplant.com/docs/howtos/security/ott
          MD5(s"$login_key|${MD5(s"$myuser:voximplant.com:$mypass")}")
        }

        val expected = calcVoxHash(oneTimeLogin, usernameInVox, passwordInVox)

        val res = manager.generateOtt(oneTimeLogin)(mockRequest).futureValue

        res.getToken shouldBe expected
      }
    }

  }

  trait Wiring {
    def nextLong: Long = Arbitrary.arbLong.arbitrary.next

    val nonUniqueUserErrorCode: Long = 118L

    val privateUser: AutoruUser = PrivateUserRefGen.next
    val hashOfUserIdAndSalt = readableString(40, 40).next

    val usernameInVox = hashOfUserIdAndSalt.take(20)
    val passwordInVox = hashOfUserIdAndSalt.drop(20)

    val mockUser = mock[UserInfo]
    when(mockUser.uid).thenReturn(privateUser.uid)
    when(mockUser.privateRef).thenReturn(privateUser)
    val mockRequest = mock[RequestImpl]
    when(mockRequest.user).thenReturn(mockUser)
    when(mockRequest.trace).thenReturn(Traced.empty)
    when(mockRequest.isSupported(ClientFeature.APP2APP_SUPPORT)).thenReturn(true)

    val voxClient = mock[VoxClient]
    val settingsClient = mock[SettingsClient]
    val passportClient = mock[PassportClient]
    val featureManager = mock[FeatureManager]
    when(passportClient.voxNameAndPassword(?)(?))
      .thenReturn(Future.successful(VoxNamePassword(usernameInVox, passwordInVox)))

    val voxCheckFeature = mock[Feature[Boolean]]
    when(featureManager.voxCheck).thenReturn(voxCheckFeature)
    val manager = new VoxManager(passportClient, voxClient, settingsClient, featureManager, "test.yavert-test.n4")
  }

}
