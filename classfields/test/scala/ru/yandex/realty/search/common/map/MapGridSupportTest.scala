package ru.yandex.realty.search.common.map

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.search.common.request.ViewportInfo

/**
  * @see [[MapGridSupport]]
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class MapGridSupportTest extends FlatSpec with Matchers with MapGridSupport {

  "MapGridSupport" should "correctly calculate map grid size" in {
    val viewportInfo = new ViewportInfo
    val cellSizeInches = 0.7f
    viewportInfo.setCellSizeInches(cellSizeInches)
    viewportInfo.setViewportBottomLatitude(59.32501287756511f)
    viewportInfo.setViewportDPI(480)
    viewportInfo.setViewportHeight(1536)
    viewportInfo.setViewportLeftLongitude(29.310777306359256f)
    viewportInfo.setViewportRightLongitude(30.873866693640764f)
    viewportInfo.setViewportTopLatitude(60.44222092515579f)
    viewportInfo.setViewportWidth(1080)

    val tryMapGrid = calculateGridStep(viewportInfo)

    tryMapGrid.isSuccess should be(true)
    val MapGridStep(latStep, lonStep) = tryMapGrid.get
    val latDiff = viewportInfo.getViewportTopLatitude - viewportInfo.getViewportBottomLatitude

    val verticalCellCount: Int = (latDiff / latStep).toInt
    val actualCellSizeInches = (viewportInfo.getViewportHeight.toFloat / viewportInfo.getViewportDPI.toFloat) /
      verticalCellCount.toFloat
    (actualCellSizeInches >= cellSizeInches) should be(true)

    val nextVerticalCellCount: Int = (latDiff / (latStep / 2.0f)).toInt
    val nextCellSizeInches = (viewportInfo.getViewportHeight.toFloat / viewportInfo.getViewportDPI.toFloat) /
      nextVerticalCellCount.toFloat
    (nextCellSizeInches < cellSizeInches) should be(true)

    viewportInfo.setCellSizeInches(nextCellSizeInches)
    val tryMapGridNext = calculateGridStep(viewportInfo)
    tryMapGridNext.isSuccess should be(true)
    val MapGridStep(nextLatStep, nextLonStep) = tryMapGridNext.get
    nextLatStep * 2.0f should be(latStep)
    nextLonStep * 2.0f should be(lonStep)
  }
}
