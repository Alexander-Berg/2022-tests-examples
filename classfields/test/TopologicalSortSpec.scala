package common.protobuf.test

import common.protobuf.TopologicalSort
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TopologicalSortSpec extends AnyWordSpec with Matchers {

  //   1
  //   | \
  //   2  3
  //   |  |
  //   4  |
  //   |  /
  //   5
  //
  def getTestData: (Int, Map[Int, Seq[Int]]) =
    (1, Map[Int, Seq[Int]](1 -> Seq[Int](2, 3), 2 -> Seq[Int](4), 3 -> Seq[Int](5), 4 -> Seq[Int](5), 5 -> Seq[Int]()))

  "TopologicalSort" should {
    "sort" must {
      "return sorted graph without cycle" in {
        val (start, graph) = getTestData
        val result = TopologicalSort.sort(start, graph)

        (result should contain).theSameElementsInOrderAs(Seq(5, 4, 2, 3, 1))
      }
    }
  }
}
