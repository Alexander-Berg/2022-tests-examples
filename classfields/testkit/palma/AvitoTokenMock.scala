package auto.dealers.multiposting.logic.testkit.palma

import java.time.Instant

import common.palma.Palma
import common.palma.testkit.MockPalma
import ru.auto.api.api_offer_model.Multiposting.Classified
import auto.dealers.multiposting.logic.secrets.ClassifiedSecretsPalma
import auto.dealers.multiposting.logic.secrets.ClassifiedSecretsPalma.recordKey
import ru.auto.multiposting.classified_secrets_palma_model.{ClassifiedSecrets => PalmaClassifiedSecret}
import auto.dealers.multiposting.model.{Classified => LocalClassified, ClientId}
import ru.auto.multiposting.wallet_model_palma.AvitoToken
import zio.{Has, IO, ZIO, ZLayer}

object AvitoTokenMock {

  def addAvitoToken(
      clientId: Long,
      fluctuation: Int = 2000
    )(palma: ClassifiedSecretsPalma.PalmaService with MockPalma): IO[Palma.PalmaError, Palma.Item[AvitoToken]] = {
    palma
      .create(
        AvitoToken(
          clientId = clientId.toString,
          accessToken = s"granted_$clientId",
          expiredAt = Instant.now().getEpochSecond + fluctuation,
          tokenType = "type 1"
        )
      )
  }

  def addClassifiedSecrets(
      clientId: Long
    )(palma: ClassifiedSecretsPalma.PalmaService
        with MockPalma): IO[Palma.PalmaError, Palma.Item[PalmaClassifiedSecret]] = {
    val avitoClassified = LocalClassified(Classified.ClassifiedName.AVITO.toString.toLowerCase)
    val clientIdIn = ClientId(clientId)
    val record = recordKey(clientIdIn, avitoClassified)
    val secret = PalmaClassifiedSecret(
      id = record.value,
      clientId = clientIdIn.value.toString,
      classified = avitoClassified.value,
      login = "testPalma",
      password = "qwerty",
      avitoClientId = clientIdIn.value.toString,
      avitoClientSecret = "secret",
      avitoUserId = clientId.toString
    )
    palma.create(secret)
  }

  private def avitoTokenPalma: ZIO[Any, Palma.PalmaError, Palma.Service with MockPalma] = {
    for {
      palma <- MockPalma.make
      _ <- addAvitoToken(clientId = 42, fluctuation = 100000)(palma)
      _ <- addClassifiedSecrets(clientId = 42)(palma)
      _ <- addClassifiedSecrets(clientId = 43)(palma)
      _ <- addAvitoToken(clientId = 44, fluctuation = -2000)(palma)
      _ <- addClassifiedSecrets(clientId = 44)(palma)
    } yield palma
  }

  def layer: ZLayer[Any, Palma.PalmaError, Has[Palma.Service]] = avitoTokenPalma.toLayer
}
