package ru.yandex.auto.eds.service.trucks.tree

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TrucksCatalogTreeSpec extends FlatSpec with DataPreparer {

  "empty TrucksCatalogTree" should "return empty results" in {
    val tree = TrucksCatalogTree.empty
    assert(tree.allMarks == Set.empty)
    assert(tree.allModels == Set.empty)
    assert(tree.allGenerations == Set.empty)
  }

  "TrucksCatalogTree " should "correctly add one card" in {
    val card = mkCatalogCard(
      mark = mkMark("AUDI", "ауди"),
      model = mkModel("Q3", "ку3"),
      generation = mkGeneration(123, "первое поколение")
    )

    var tree = TrucksCatalogTree.empty
    tree = tree.updated(card)

    assert(tree.allMarks.size == 1)
    assert(tree.allMarks.find(_.key == "AUDI").map(_.value.getCyrillicName).contains("ауди"))

    assert(tree.allModels.size == 1)
    assert(tree.allModels.find(_.key("AUDI") == "AUDI.Q3").map(_.value.getCyrillicName).contains("ку3"))

    assert(tree.allGenerations.size == 1)
    assert(tree.allGenerations.find(_.key == 123).map(_.value.getCyrillicName).contains("первое поколение"))
  }

  "TrucksCatalogTree" should "correctly merge generations" in {
    val card1 = mkCatalogCard(
      mark = mkMark("AUDI", "ауди"),
      model = mkModel("Q3", "ку3"),
      generation = mkGeneration(123, "первое поколение")
    )
    val card2 = mkCatalogCard(
      mark = mkMark("AUDI", "ауди"),
      model = mkModel("Q3", "ку3"),
      generation = mkGeneration(456, "второе поколение")
    )

    var tree = TrucksCatalogTree.empty
    tree = tree.updated(card1)
    tree = tree.updated(card2)

    assert(tree.allMarks.size == 1)
    assert(tree.allMarks.find(_.key == "AUDI").map(_.value.getCyrillicName).contains("ауди"))

    assert(tree.allModels.size == 1)
    assert(tree.allModels.find(_.key("AUDI") == "AUDI.Q3").map(_.value.getCyrillicName).contains("ку3"))

    assert(tree.allGenerations.size == 2)
    assert(tree.allGenerations.map(_.value.getCyrillicName) == Set("первое поколение", "второе поколение"))
  }

  "TrucksCatalogTree" should "correctly merge models" in {
    val card1 = mkCatalogCard(
      mark = mkMark("AUDI", "ауди"),
      model = mkModel("Q3", "ку3"),
      generation = mkGeneration(123, "первое поколение Q3")
    )
    val card2 = mkCatalogCard(
      mark = mkMark("AUDI", "ауди"),
      model = mkModel("Q5", "ку5"),
      generation = mkGeneration(456, "первое поколение Q5")
    )

    var tree = TrucksCatalogTree.empty
    tree = tree.updated(card1)
    tree = tree.updated(card2)

    assert(tree.allMarks.size == 1)
    assert(tree.allMarks.find(_.key == "AUDI").map(_.value.getCyrillicName).contains("ауди"))

    assert(tree.allModels.size == 2)
    assert(tree.allModels.find(_.key("AUDI") == "AUDI.Q3").map(_.value.getCyrillicName).contains("ку3"))
    assert(tree.allModels.find(_.key("AUDI") == "AUDI.Q5").map(_.value.getCyrillicName).contains("ку5"))

    assert(tree.allGenerations.size == 2)
    assert(tree.allGenerations.map(_.value.getCyrillicName) == Set("первое поколение Q3", "первое поколение Q5"))
  }

  "TrucksCatalogTree" should "correctly merge marks" in {
    val card1 = mkCatalogCard(
      mark = mkMark("AUDI", "ауди"),
      model = mkModel("Q3", "ку3"),
      generation = mkGeneration(123, "первое поколение Q3")
    )
    val card2 = mkCatalogCard(
      mark = mkMark("BMW", "бмв"),
      model = mkModel("X3", "икс3"),
      generation = mkGeneration(456, "первое поколение X3")
    )

    var tree = TrucksCatalogTree.empty
    tree = tree.updated(card1)
    tree = tree.updated(card2)

    assert(tree.allMarks.size == 2)
    assert(tree.allMarks.find(_.key == "AUDI").map(_.value.getCyrillicName).contains("ауди"))
    assert(tree.allMarks.find(_.key == "BMW").map(_.value.getCyrillicName).contains("бмв"))

    assert(tree.allModels.size == 2)
    assert(tree.allModels.find(_.key("AUDI") == "AUDI.Q3").map(_.value.getCyrillicName).contains("ку3"))
    assert(tree.allModels.find(_.key("BMW") == "BMW.X3").map(_.value.getCyrillicName).contains("икс3"))

    assert(tree.allGenerations.size == 2)
    assert(tree.allGenerations.map(_.value.getCyrillicName) == Set("первое поколение Q3", "первое поколение X3"))
  }

  "TrucksCatalogTree" should "have correct links" in {
    val card1 = mkCatalogCard(
      mark = mkMark("AUDI", "ауди"),
      model = mkModel("Q3", "ку3"),
      generation = mkGeneration(123, "первое")
    )
    val card2 = mkCatalogCard(
      mark = mkMark("AUDI", "ауди"),
      model = mkModel("Q5", "ку5"),
      generation = mkGeneration(456, "первое")
    )
    val card3 = mkCatalogCard(
      mark = mkMark("AUDI", "ауди"),
      model = mkModel("Q5", "ку5"),
      generation = mkGeneration(789, "второе")
    )

    var tree = TrucksCatalogTree.empty
    tree = tree.updated(card1)
    tree = tree.updated(card2)
    tree = tree.updated(card3)

    assert(tree.allMarks.size == 1)
    assert(tree.allModels.size == 2)
    assert(tree.allGenerations.size == 3)

    assert(tree.findMark("AUDI").get.children.map(_.key("AUDI")) == Set("AUDI.Q3", "AUDI.Q5"))
    assert(tree.findMark("AUDI").get.children.map(_.value.getCode) == Set("Q3", "Q5"))
    assert(tree.findMark("AUDI").get.children.forall(_.parent.value.getCode == "AUDI"))

    assert(tree.findModel("AUDI", "Q5").get.children.map(_.value.getCyrillicName) == Set("первое", "второе"))
    assert(tree.findModel("AUDI", "Q5").get.children.forall(_.parent.value.getCode == "Q5"))
    assert(tree.findModel("AUDI", "Q5").get.children.forall(_.parent.parent.value.getCode == "AUDI"))

    assert(tree.findGeneration(123).get.parent.value.getCode == "Q3")
    assert(tree.findGeneration(123).get.parent.parent.value.getCode == "AUDI")
  }

  "TrucksCatalogTree" should "support models without generation" in {
    val card1 = mkCatalogCard(
      mark = mkMark("AUDI", "ауди"),
      model = mkModel("Q3", "ку3"),
      generation = mkGeneration(123, "первое поколение")
    )
    val card2 = mkCatalogCard(mark = mkMark("AUDI", "ауди"), model = mkModel("Q3", "ку3"))

    var tree = TrucksCatalogTree.empty
    tree = tree.updated(card1)
    tree = tree.updated(card2)

    assert(tree.allMarks.size == 1)
    assert(tree.allModels.size == 1)
    assert(tree.allGenerations.size == 1)
    assert(tree.findMark("AUDI").get.children.map(_.key("AUDI")) == Set("AUDI.Q3"))
    assert(tree.findModel("AUDI", "Q3").get.children.map(_.key) == Set(123L))
  }
}
