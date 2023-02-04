package auto.dealers.trade_in_notifier.storage.managers.test

import auto.dealers.trade_in_notifier.storage.ColorPaletteRepository
import auto.dealers.trade_in_notifier.storage.ColorPaletteRepository._
import auto.dealers.trade_in_notifier.storage.managers.ColorPaletteRepositoryLive
import zio.test._
import zio.test.Assertion._

object ColorPaletteRepositorySpec extends DefaultRunnableSpec {

  def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ColorPaletteRepositorySpec")(
      findGreenTest,
      colorNotFoundTest
    ).provideCustomLayerShared(ColorPaletteRepositoryLive.live)

  val findGreenTest =
    testM("Find green by id: 007F00")(
      assertM(ColorPaletteRepository.getColorById("007F00").map(_.map(_.name)))(isSome(equalTo("зелёный")))
    )

  val colorNotFoundTest =
    testM("No color for id: badcode")(
      assertM(ColorPaletteRepository.getColorById("badcode").map(_.map(_.name)))(isNone)
    )

}
