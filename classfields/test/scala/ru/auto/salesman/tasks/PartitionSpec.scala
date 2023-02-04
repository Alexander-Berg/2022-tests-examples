package ru.auto.salesman.tasks

import org.scalatest.Inspectors
import ru.auto.salesman.test.BaseSpec

class PartitionSpec extends BaseSpec {

  private val partitions = Partition.all(4)

  "Partition.contains" should {

    "distribute every key to one partition" in {
      Inspectors.forEvery(1 to 100) { i =>
        Inspectors.forExactly(1, partitions) { partition =>
          partition.contains(i) shouldBe true
        }
      }
    }

    "distribute keys ~ evenly across partitions even if for all n mod % 2 == 0" in {
      val keys = 2 to 100 by 2
      val partitionSizes =
        partitions.map(partition => keys.count(partition.contains(_)))
      // Total keys count = 50
      // Partitions count = 4
      // So, each partition should have from 7 to 20 elements to consider
      // distribution ~ even.
      Inspectors.forEvery(partitionSizes)(_ should be >= 7)
      Inspectors.forEvery(partitionSizes)(_ should be <= 20)
    }
  }
}
