package ru.yandex.auto.extdata.jobs.feeds.feed.utils

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.extdata.jobs.feeds.feed.writers.cars.{
  ClassifierTreeNode,
  LimitedBranchingNode,
  LimitedClassifierNodeWithFallback,
  LimitedLeaf,
  SearchWithNFound
}

@RunWith(classOf[JUnitRunner])
class ClassifierTreeSpec extends WordSpecLike with Matchers {

  case class TestData(name: String, value: Int, classifier: Int)

  case class CarSearchQuery(override val count: Int, searchType: Int, msg: TestData) extends SearchWithNFound

  class ForValLeaf extends LimitedLeaf[TestData, Int, CarSearchQuery] {
    override def mapData(inputData: TestData): Int = inputData.value

    override protected def getStoredDataFilterFunc(filter: CarSearchQuery): Int => Boolean =
      id => id != mapData(filter.msg)
  }

  trait LeafForValCreator {
    def createChild: ForValLeaf = new ForValLeaf
  }

  /***
    * standard filter keys
    * @tparam C = key for map
    */
  trait RegularQueryableDataStorageKeyFilter[C]
      extends LimitedClassifierNodeWithFallback[TestData, Int, CarSearchQuery, C] {
    override protected def categoriesFromQuery(query: CarSearchQuery): Set[C] = Set(categoryFromData(query.msg))
  }

  class NameSlicer extends RegularQueryableDataStorageKeyFilter[String] with LeafForValCreator {
    override def categoryFromData(inputData: TestData): String = inputData.name
  }

  class CategorizingClassifierSlicer extends RegularQueryableDataStorageKeyFilter[Int] with LeafForValCreator {
    override def categoryFromData(inputData: TestData): Int = inputData.classifier
  }

  class SlicingTree(override val childrenMap: Map[Int, ClassifierTreeNode[TestData, Int, CarSearchQuery]])
      extends LimitedBranchingNode[TestData, Int, CarSearchQuery, Int] {
    override protected def categoriesFromQuery(query: CarSearchQuery): Set[Int] = Set(query.searchType)
  }

  private def createTree: SlicingTree = {
    val searchTree = new SlicingTree(Map(1 -> new NameSlicer, 2 -> new CategorizingClassifierSlicer))
    searchTree.addData(TestData("a", 1, 1))
    searchTree.addData(TestData("b", 1, 1))
    searchTree.addData(TestData("c", 3, 1))
    searchTree.addData(TestData("a", 2, 1))
    searchTree.addData(TestData("b", 2, 1))
    searchTree.addData(TestData("c", 3, 2))
    searchTree
  }

  "SlicingTree" should {
    "find nothing if empty" in {
      val searchTree = new SlicingTree(Map(1 -> new NameSlicer, 2 -> new CategorizingClassifierSlicer))

      val searcher = CarSearchQuery(1, 1, TestData("a", 1, 1))

      Set[Int]() shouldEqual searchTree.getForQuery(searcher)
    }

    "add elements" in {
      val searchTree = createTree

      3 shouldEqual searchTree.getAll.size // number of distinct values is 3: (1,2,3)
    }

    "search" in {
      val searchTree = createTree

      1 shouldEqual searchTree.getForQuery(CarSearchQuery(1, 1, TestData("d", 1, 1))).size

      3 shouldEqual searchTree.getForQuery(CarSearchQuery(3, 2, TestData("d", 4, 1))).size
    }

    "exclude searching element" in {
      val searchTree = createTree

      val data = searchTree.getForQuery(CarSearchQuery(8, 2, TestData("d", 1, 1)))
      (data should contain).noneOf(1, 4)
      (data should contain).allOf(2, 3)

    }

    "find best match first" in {
      val searchTree = createTree

      val data = searchTree.getForQuery(CarSearchQuery(1, 1, TestData("a", 1, 1)))
      (data should contain) noneOf (1, 3, 4)
      data should contain(2)
    }

  }
}
