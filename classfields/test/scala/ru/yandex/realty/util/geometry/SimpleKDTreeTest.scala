package ru.yandex.realty.util.geometry

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.model.location.GeoPoint

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class SimpleKDTreeTest extends FlatSpec with Matchers {
  import SimpleKDTree._

  "kdtree" should "correctly insert one point" in {
    val tree = Empty.add(GeoPoint.getPoint(0.0f, 0.0f))
    tree should be(KDTree(left = Empty, right = Empty, point = GeoPoint.getPoint(0.0f, 0.0f), compareX = true))
  }

  "kdtree" should "correctly insert two points" in {
    val tree = SimpleKDTree.Empty.add(GeoPoint.getPoint(1.0f, 2.0f))
    val tree1 = tree.add(GeoPoint.getPoint(1.0f, 1.5f))
    tree1 should be(
      KDTree(
        left = KDTree(left = Empty, right = Empty, point = GeoPoint.getPoint(1.0f, 1.5f), compareX = false),
        right = SimpleKDTree.Empty,
        point = GeoPoint.getPoint(1.0f, 2.0f),
        compareX = true
      )
    )
    val tree2 = tree.add(GeoPoint.getPoint(1.0f, 3.0f))
    tree2 should be(
      KDTree(
        left = SimpleKDTree.Empty,
        right = KDTree(
          left = SimpleKDTree.Empty,
          right = SimpleKDTree.Empty,
          point = GeoPoint.getPoint(1.0f, 3.0f),
          compareX = false
        ),
        point = GeoPoint.getPoint(1.0f, 2.0f),
        compareX = true
      )
    )
  }

  "kdtree" should "not insert the same point twice" in {
    val testPoint = GeoPoint.getPoint(10.0f, 20.0f)
    val tree = Empty.add(testPoint).add(testPoint)
    tree should be(KDTree(left = Empty, right = Empty, point = testPoint, compareX = true))
    tree.add(GeoPoint.getPoint(10.0f, 20.0f)) shouldEqual tree
  }

  "kdtree" should "correct insert three points" in {
    val point1 = GeoPoint.getPoint(1.0f, 2.0f)
    val point2 = GeoPoint.getPoint(2.0f, 1.0f)
    val point3 = GeoPoint.getPoint(3.0f, 0.0f)
    val tree123 = Empty.add(point1).add(point2).add(point3)
    tree123 should be(
      KDTree(
        left = KDTree(
          left = Empty,
          right = KDTree(
            left = Empty,
            right = Empty,
            point = point3,
            compareX = true
          ),
          point = point2,
          compareX = false
        ),
        right = Empty,
        point = point1,
        compareX = true
      )
    )
    val pointA = GeoPoint.getPoint(1.0f, 2.0f)
    val pointB = GeoPoint.getPoint(2.0f, 1.0f)
    val pointC = GeoPoint.getPoint(1.0f, 1.0f)
    val treeABC = Empty.add(pointA).add(pointB).add(pointC)

    treeABC should be(
      KDTree(
        left = KDTree(
          left = KDTree(
            left = Empty,
            right = Empty,
            point = pointC,
            compareX = true
          ),
          right = Empty,
          point = pointB,
          compareX = false
        ),
        right = Empty,
        point = pointA,
        compareX = true
      )
    )

    val pointX = GeoPoint.getPoint(2.0f, 2.0f)
    val pointY = GeoPoint.getPoint(1.0f, 3.0f)
    val pointZ = GeoPoint.getPoint(0.0f, 4.0f)
    val treeXYZ = Empty.add(pointX).add(pointY).add(pointZ)
    treeXYZ should be(
      KDTree(
        left = Empty,
        right = KDTree(
          left = KDTree(
            left = Empty,
            right = Empty,
            point = pointZ,
            compareX = true
          ),
          right = Empty,
          point = pointY,
          compareX = false
        ),
        point = pointX,
        compareX = true
      )
    )

    val pointP = GeoPoint.getPoint(2.0f, 2.0f)
    val pointQ = GeoPoint.getPoint(1.0f, 3.0f)
    val pointR = GeoPoint.getPoint(2.0f, 4.0f)
    val treePQR = Empty.add(pointP).add(pointQ).add(pointR)
    treePQR should be(
      KDTree(
        left = Empty,
        right = KDTree(
          left = Empty,
          right = KDTree(
            left = Empty,
            right = Empty,
            point = pointR,
            compareX = true
          ),
          point = pointQ,
          compareX = false
        ),
        point = pointP,
        compareX = true
      )
    )
  }

  "kdtree" should "return all elements for covering rectangle" in {
    val points = Seq(
      GeoPoint.getPoint(59.940209f, 30.316630f),
      GeoPoint.getPoint(59.939445f, 30.317713f),
      GeoPoint.getPoint(59.938250f, 30.317048f),
      GeoPoint.getPoint(59.938357f, 30.314291f),
      GeoPoint.getPoint(59.939030f, 30.313347f)
    )

    var tree: SimpleKDTree = Empty
    for (point <- points) tree = tree.add(point)

    val foundPoints =
      tree.range(lt = GeoPoint.getPoint(59.940517f, 30.311966f), rb = GeoPoint.getPoint(59.937867f, 30.320424f))
    foundPoints shouldEqual points.toSet
  }

  "kdtree" should "find point by covering rectangle" in {
    val selectedPoint = GeoPoint.getPoint(59.938357f, 30.314291f)
    val points = Seq(
      GeoPoint.getPoint(59.940209f, 30.316630f),
      GeoPoint.getPoint(59.939445f, 30.317713f),
      GeoPoint.getPoint(59.938250f, 30.317048f),
      selectedPoint,
      GeoPoint.getPoint(59.939030f, 30.313347f)
    )

    var tree: SimpleKDTree = Empty
    for (point <- points) tree = tree.add(point)
    val res = tree.range(lt = GeoPoint.getPoint(59.938358f, 30.314290f), rb = GeoPoint.getPoint(59.938356f, 30.314292f))
    res shouldEqual Set(selectedPoint)
  }
}
