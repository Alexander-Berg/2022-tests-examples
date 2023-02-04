package auto.dealers.application.api.test

import common.palma.Palma
import common.palma.testkit.MockPalma
import zio.{Has, ULayer}

object CreditConfigurationDictionaryServiceTest {

  def layer: ULayer[Has[Palma.Service] with Has[MockPalma]] =
    MockPalma.make.map(palma => Has.allOf[Palma.Service, MockPalma](palma, palma)).toLayerMany

}
