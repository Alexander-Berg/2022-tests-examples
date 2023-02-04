package ru.yandex.vertis.moderation.model.realty

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.proto.RealtyLight.AreaUnit

/**
  * Specs for [[AreaInfo]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class AreaInfoSpec extends SpecBase {

  "AreaInfo" should {

    val Cases: Seq[(AreaInfo, Option[Float])] =
      Seq(
        AreaInfo(None, None) -> None,
        AreaInfo(Some(1.1f), None) -> None,
        AreaInfo(1, AreaUnit.AREA_UNIT_UNKNOWN) -> None,
        AreaInfo(1, AreaUnit.WHOLE_OFFER) -> None,
        AreaInfo(Some(1.0f), None) -> None,
        AreaInfo(1, AreaUnit.SQUARE_METER) -> Some(1),
        AreaInfo(29.3f, AreaUnit.SQUARE_METER) -> Some(29.3f),
        AreaInfo(6, AreaUnit.ARE) -> Some(600),
        AreaInfo(3.3f, AreaUnit.HECTARE) -> Some(33000),
        AreaInfo(4.01f, AreaUnit.SQUARE_KILOMETER) -> Some(4010000.2f),
        AreaInfo(-1.5f, AreaUnit.SQUARE_KILOMETER) -> Some(-1500000),
        AreaInfo(Float.PositiveInfinity, AreaUnit.SQUARE_KILOMETER) -> None,
        AreaInfo(Float.MaxValue, AreaUnit.SQUARE_KILOMETER) -> None
      )

    Cases.foreach { case (area, square) =>
      s"converts $area to square metres correctly" in {
        area.inSquareMetres should be(square)
      }
    }
  }
}
