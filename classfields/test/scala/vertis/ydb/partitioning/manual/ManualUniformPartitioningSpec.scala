package vertis.ydb.partitioning.manual

import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class ManualUniformPartitioningSpec extends AnyWordSpec with Matchers {

  "ManualUniformPartitioning" should {

    (List(2, 30) ++ Iterator.continually(Random.nextInt(27) + 3).take(5).toSet).foreach { bits =>
      s"correctly match hashes to partitions of $bits" in {
        val partitioning = ManualUniformPartitioning.ofBits(bits)
        val partitionsToCheck = Seq(0, 1, partitioning.numberOfPartitions - 1) ++
          Iterator.continually(Random.nextInt(partitioning.numberOfPartitions)).take(10)
        partitionsToCheck.foreach(checkPartition(partitioning))
      }
    }
  }

  private def checkPartition(partitioning: ManualUniformPartitioning)(pIndex: Int): Assertion = {
    val partition = partitioning.getByIndex(pIndex)
    import partition._
    withClue(s"partition $partition start is in partition") {
      partitioning.partitionIndex(id) should be(pIndex)
    }
    withClue(s"partition $partition hash is in partition") {
      val middle = id + (end - id) / 2
      partitioning.partitionIndex(middle) should be(pIndex)
    }
    withClue(s"partition $partition end is in partition") {
      partitioning.partitionIndex(end) should be(pIndex)
    }
  }

}
