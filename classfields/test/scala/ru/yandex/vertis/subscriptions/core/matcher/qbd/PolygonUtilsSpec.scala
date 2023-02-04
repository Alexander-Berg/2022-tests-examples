package ru.yandex.vertis.subscriptions.core.matcher.qbd

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

/**
  * Tests on [[PolygonUtils]]
  */
@RunWith(classOf[JUnitRunner])
class PolygonUtilsSpec extends FunSuite with Matchers {

  def v(x: Float, y: Float) = Vertex(x, y)

  test("test simple polygon") {
    val p = Polygon(v(0, 0), v(0, 10), v(10, 10), v(10, 0))

    PolygonUtils.isPointInside(p, v(1, 1)) should be(true)
    PolygonUtils.isPointInside(p, v(11, 11)) should be(false)
    PolygonUtils.isPointInside(p, v(9, 9)) should be(true)
    PolygonUtils.isPointInside(p, v(-10, 0)) should be(false)
    PolygonUtils.isPointInside(p, v(9.5f, 10)) should be(false)
    PolygonUtils.isPointInside(p, v(5, 0)) should be(true)
  }

  test("test slightly complex polygon") {
    val p = Polygon(v(0, 0), v(0, 10), v(10, 10), v(10, 5))

    PolygonUtils.isPointInside(p, v(1, 1)) should be(true)
    PolygonUtils.isPointInside(p, v(11, 11)) should be(false)
    PolygonUtils.isPointInside(p, v(0, 0)) should be(true)
    PolygonUtils.isPointInside(p, v(10, 0)) should be(false)
  }

  test("test more slightly complex polygon") {
    val p = Polygon(v(10, 10), v(20, 20), v(30, 10), v(20, 15))

    PolygonUtils.isPointInside(p, v(1, 1)) should be(false)
    PolygonUtils.isPointInside(p, v(11, 11)) should be(false)
    PolygonUtils.isPointInside(p, v(11.1f, 10.9f)) should be(true)
    PolygonUtils.isPointInside(p, v(10, 20)) should be(false)
    PolygonUtils.isPointInside(p, v(10, 10)) should be(false)
    PolygonUtils.isPointInside(p, v(19, 17)) should be(true)
  }

  test("test singular polygon") {
    val p = Polygon(v(0, 0), v(10, 10), v(5, 5))

    PolygonUtils.isPointInside(p, v(1, 1)) should be(false)
    PolygonUtils.isPointInside(p, v(11, 11)) should be(false)
    PolygonUtils.isPointInside(p, v(-10, 0)) should be(false)
    PolygonUtils.isPointInside(p, v(10, 10)) should be(false)
    PolygonUtils.isPointInside(p, v(6, 6.5f)) should be(false)
  }
}
