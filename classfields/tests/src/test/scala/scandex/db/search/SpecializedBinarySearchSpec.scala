package scandex.db.search

import alien.memory.*
import scandex.*
import scandex.db.search.SpecializedBinarySearch.Int64BinarySearch
import zio.Scope
import zio.test.*
import zio.test.ZIOSpecDefault

object SpecializedBinarySearchSpec extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Search long ")(
      test("should find index") {
        check(LongSequence()) { longs: Vector[Long] =>
          val sortedLongs   = longs.+:(100L).+:(101L).sorted
          val expectedIndex = sortedLongs.indexOf(100L).toLong
          val mem           = Memory.ofArray(sortedLongs.toArray)
          val mh =
            BoundedSequence(sortedLongs.length.toLong, Values.Long) / % / $

          val foundIndex = Int64BinarySearch
            .search(mh.get(mem)(_), 100L, 0, sortedLongs.length.toLong)
          assertTrue(foundIndex == expectedIndex)
        }
      },
    )

}
