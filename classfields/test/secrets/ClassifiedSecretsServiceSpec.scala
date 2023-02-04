package auto.dealers.multiposting.logic.test.secrets

import cats.syntax.option._
import common.palma.Palma.Palma
import common.palma.testkit.MockPalma
import common.zio.context.ContextHolder
import auto.dealers.multiposting.logic.secrets.ClassifiedSecretsService
import auto.dealers.multiposting.logic.secrets.ClassifiedSecretsService.ClassifiedSecretsService
import ru.auto.multiposting.api_model.{AvitoSecret, ClassifiedSecret, ClearedAvitoSecret, ClearedSecret, Secret}
import ru.auto.multiposting.api_model.ClearedSecret.ClearedClassified
import auto.dealers.multiposting.model.{Classified, ClientId}
import common.zio.logging.Logging
import ru.auto.common.broker.testkit.ClientActionLoggerDummy
import ru.auto.common.context.RequestPayload
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestEnvironment
import ru.auto.multiposting.classified_secrets_palma_model.{ClassifiedSecrets => PalmaClassifiedSecret}
import auto.dealers.multiposting.logic.test.secrets.ClassifiedSecretsDictionaryTest
import zio.{Has, ZLayer}
import zio.magic._

object ClassifiedSecretsServiceSpec extends DefaultRunnableSpec {

  override val spec: ZSpec[TestEnvironment, Any] =
    (suite("DefaultClassifiedSecretsService")(
      testM("read cleared secret") {
        val clientId1 = ClientId(20101)
        val clientId2 = ClientId(68)
        val secret1 =
          ClassifiedSecret("abc".some, "p@ssword".some, "avito_user".some, "avito_client".some, "avito_secret".some)
        val clearedClassifiedSecret1 = ClearedAvitoSecret(
          hasAvitoUserId = true,
          hasAvitoClientId = true,
          hasAvitoClientSecret = true
        )
        val expectedSecret1 = ClearedSecret(
          classified = "avito",
          login = "abc",
          hasPassword = true,
          clearedClassified = ClearedClassified.ClearedAvitoSecret(clearedClassifiedSecret1)
        )
        val secret2 = ClassifiedSecret("abc".some, "p@ssword123".some)
        val clearedClassifiedSecret2 = ClearedAvitoSecret(
          hasAvitoUserId = false,
          hasAvitoClientId = false,
          hasAvitoClientSecret = false
        )
        val expectedSecret2 = ClearedSecret(
          classified = "drom",
          login = "abc",
          hasPassword = true,
          clearedClassified = ClearedClassified.ClearedAvitoSecret(clearedClassifiedSecret2)
        )
        val secret3 = ClassifiedSecret("abc68".some, "p@ssword".some)
        for {
          _ <- ClassifiedSecretsService.write(clientId1, Classified("avito"), secret1)
          _ <- ClassifiedSecretsService.write(clientId1, Classified("drom"), secret2)
          _ <- ClassifiedSecretsService.write(clientId2, Classified("drom"), secret3)
          read <- ClassifiedSecretsService.readCleared(clientId1)
        } yield assert(read)(hasSameElements(List(expectedSecret1, expectedSecret2)))
      },
      testM("read secret") {
        val clientId1 = ClientId(20101)
        val clientId2 = ClientId(68)
        val secret1 =
          ClassifiedSecret("abc".some, "p@ssword".some, "avito_user".some, "avito_client".some, "avito_secret".some)
        val avitoSecret = AvitoSecret(
          avitoUserId = "avito_user",
          avitoClientId = "avito_client",
          avitoClientSecret = "avito_secret"
        )
        val expectedSecret1 = Secret(
          classified = "avito",
          login = "abc",
          password = "p@ssword",
          secret = Secret.Secret.AvitoSecret(avitoSecret)
        )
        val secret2 = ClassifiedSecret("abc".some, "p@ssword123".some)
        val emptyAvitoSecret = AvitoSecret(
          avitoUserId = "",
          avitoClientId = "",
          avitoClientSecret = ""
        )
        val expectedSecret2 = Secret(
          classified = "drom",
          login = "abc",
          password = "p@ssword123",
          secret = Secret.Secret.AvitoSecret(emptyAvitoSecret)
        )
        val secret3 = ClassifiedSecret("abc68".some, "p@ssword".some)
        for {
          _ <- ClassifiedSecretsService.write(clientId1, Classified("avito"), secret1)
          _ <- ClassifiedSecretsService.write(clientId1, Classified("drom"), secret2)
          _ <- ClassifiedSecretsService.write(clientId2, Classified("drom"), secret3)
          read <- ClassifiedSecretsService.read(clientId1)
        } yield assert(read)(hasSameElements(List(expectedSecret1, expectedSecret2)))
      },
      testM("delete secret") {
        val clientId = ClientId(20101)
        val secret =
          ClassifiedSecret("abc".some, "p@ssword".some, "avito_user".some, "avito_client".some, "avito_secret".some)
        for {
          _ <- ClassifiedSecretsService.write(clientId, Classified("avito"), secret)
          _ <- ClassifiedSecretsService.delete(clientId, Classified("avito"))
          read <- ClassifiedSecretsService.readCleared(clientId)
        } yield assert(read)(isEmpty)
      },
      testM("update secret") {
        val clientId = ClientId(20101)
        val secret =
          ClassifiedSecret("abc".some, "p@ssword".some, "avito_user".some, "avito_client".some, "avito_secret".some)
        for {
          _ <- ClassifiedSecretsService.write(clientId, Classified("avito"), secret)
          _ <- ClassifiedSecretsService.write(clientId, Classified("avito"), secret.copy(login = "updated_login".some))
          read <- ClassifiedSecretsService.readCleared(clientId)
        } yield assert(read.length)(equalTo(1)) && assert(read.head.login)(equalTo("updated_login"))
      },
      testM("clear some field") {
        val clientId = ClientId(20101)
        val secret =
          ClassifiedSecret("abc".some, "p@ssword".some, "avito_user".some, "avito_client".some, "avito_secret".some)
        val updated = ClassifiedSecret("abc".some, "".some, "avito_user".some, "avito_client".some, "avito_secret".some)
        val expected = PalmaClassifiedSecret(
          "20101_avito",
          "20101",
          "avito",
          "abc",
          "",
          "avito_client",
          "avito_secret",
          "avito_user"
        )
        for {
          _ <- ClassifiedSecretsService.write(clientId, Classified("avito"), secret)
          _ <- ClassifiedSecretsService.write(clientId, Classified("avito"), updated)
          read <- ClassifiedSecretsDictionaryTest.get(clientId, Classified("avito"))
        } yield assertTrue(read.contains(expected))
      },
      testM("preserve non-updated fields") {
        val clientId = ClientId(20101)
        val secret =
          ClassifiedSecret("abc".some, "p@ssword".some, "avito_user".some, "avito_client".some, "avito_secret".some)
        val updated = ClassifiedSecret("abc".some, "newp@ssword".some)
        val expected = PalmaClassifiedSecret(
          "20101_avito",
          "20101",
          "avito",
          "abc",
          "newp@ssword",
          "avito_client",
          "avito_secret",
          "avito_user"
        )
        for {
          _ <- ClassifiedSecretsService.write(clientId, Classified("avito"), secret)
          _ <- ClassifiedSecretsService.write(clientId, Classified("avito"), updated)
          read <- ClassifiedSecretsDictionaryTest.get(clientId, Classified("avito"))
        } yield assertTrue(read.contains(expected))
      }
    ) @@ sequential @@ after(ClassifiedSecretsDictionaryTest.clean)).provideLayer(
      ZLayer.fromSomeMagic[TestEnvironment, Has[MockPalma] with Palma with ClassifiedSecretsService](
        Logging.live,
        ClassifiedSecretsDictionaryTest.layer,
        ClassifiedSecretsService.live,
        ContextHolder.live[RequestPayload],
        ClientActionLoggerDummy.test
      )
    )
}
