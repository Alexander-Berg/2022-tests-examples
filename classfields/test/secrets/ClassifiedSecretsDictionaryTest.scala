package auto.dealers.multiposting.logic.test.secrets

import common.palma.Palma
import common.palma.Palma.{Palma, PalmaError}
import common.palma.testkit.MockPalma
import auto.dealers.multiposting.logic.secrets.ClassifiedSecretsPalma.recordKey
import ru.auto.multiposting.classified_secrets_palma_model.{ClassifiedSecrets => PalmaClassifiedSecret}
import auto.dealers.multiposting.model.{Classified, ClientId}
import zio.{Has, ULayer, URIO, ZIO}

object ClassifiedSecretsDictionaryTest {

  def clean: URIO[Has[MockPalma], Unit] = MockPalma.clean[PalmaClassifiedSecret]

  def get(clientId: ClientId, classified: Classified): ZIO[Palma, PalmaError, Option[PalmaClassifiedSecret]] =
    Palma.get[PalmaClassifiedSecret](recordKey(clientId, classified).value).map(_.map(_.value))

  def layer: ULayer[Has[Palma.Service] with Has[MockPalma]] =
    MockPalma.make.map(p => Has.allOf[Palma.Service, MockPalma](p, p)).toLayerMany

}
