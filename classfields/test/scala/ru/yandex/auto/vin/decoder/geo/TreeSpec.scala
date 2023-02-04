package ru.yandex.auto.vin.decoder.geo

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.auto.vin.decoder.extdata.catalog.region.Tree
import ru.yandex.auto.vin.decoder.extdata.region.{GeoRegion, Tree}

class TreeSpec extends AnyWordSpec with Matchers {

  "regions tree" should {
    "return name" in {
      val tree = new Tree(
        List(
          makeRegion(1, "a"),
          makeRegion(2, "b"),
          makeRegion(3, "c")
        )
      )
      tree.findName(1) should be(Some("a"))
      tree.findName(5) should be(None)
    }

    "return parents" in {
      val tree = new Tree(
        List(
          makeRegion(1, "a", 0),
          makeRegion(2, "b", 1),
          makeRegion(3, "c", 123)
        )
      )
      tree.getParents(1) should be(Some(Seq(0)))
      tree.getParents(2) should be(Some(Seq(1, 0)))
      tree.getParents(3) should be(Some(Seq(123)))
      tree.getParents(6) should be(None)
    }

    "return children" in {
      val tree = new Tree(
        List(
          makeRegion(1, "a", 0),
          makeRegion(2, "b", 1),
          makeRegion(3, "c", 1),
          makeRegion(4, "d", 2)
        )
      )
      tree.getChildren(0) should be(Some(Vector(1)))
      tree.getChildren(1) should be(Some(Vector(2, 3)))
      tree.getChildren(3) should be(Some(Vector.empty))
      tree.getChildren(11) should be(None)
    }

    "return descenants" in {
      val tree = new Tree(
        List(
          makeRegion(1, "a", 0),
          makeRegion(2, "b", 1),
          makeRegion(3, "c", 1),
          makeRegion(4, "d", 2),
          makeRegion(5, "e", 3),
          makeRegion(6, "f", 0)
        )
      )

      tree.getDescendants(0) should be(Some(Vector(1, 6, 2, 3, 4, 5)))
      tree.getDescendants(1) should be(Some(Vector(2, 3, 4, 5)))
      tree.getDescendants(2) should be(Some(Vector(4)))
      tree.getDescendants(3) should be(Some(Vector(5)))
      tree.getDescendants(4) should be(Some(Vector()))
      tree.getDescendants(5) should be(Some(Vector()))
      tree.getDescendants(6) should be(Some(Vector()))
      tree.getDescendants(7) should be(None)
    }

    "check parent" in {
      val tree = new Tree(
        List(
          makeRegion(1, "a", 0),
          makeRegion(2, "b", 1),
          makeRegion(3, "c", 1),
          makeRegion(4, "d", 2)
        )
      )
      tree.isParent(1, 0) should be(Some(true))
      tree.isParent(3, 0) should be(Some(true))
      tree.isParent(3, 4) should be(Some(false))
      tree.isParent(11, 0) should be(None)
      tree.isParent(1, 414) should be(None)
    }
  }

  private def makeRegion(
      id: Long,
      name: String,
      parent: Long = 0,
      genitive: String = "",
      dative: String = "",
      accusative: String = "",
      typeRegion: Int = 0,
      prepositional: String = "",
      preposition: String = "",
      tzOffsetSeconds: Int = 0): GeoRegion = {
    GeoRegion(id, name, parent, genitive, dative, accusative, typeRegion, prepositional, preposition, tzOffsetSeconds)
  }
}
