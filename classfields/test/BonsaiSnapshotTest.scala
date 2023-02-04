package ru.yandex.vertis.general.bonsai.public.test

import general.bonsai.category_model.{Category, CategoryState}
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import zio.test.{suite, test, DefaultRunnableSpec, ZSpec}
import zio.test.Assertion._
import zio.test.assert

object BonsaiSnapshotTest extends DefaultRunnableSpec {

  val leaf1 = Category("leaf1", parentId = "root1")
  val leaf2 = Category("leaf2", parentId = "root2")

  val testSnaphot = BonsaiSnapshot(
    Seq(
      Category("root1"),
      leaf1,
      Category("not-leaf1", state = CategoryState.ARCHIVED, parentId = "root1"),
      Category("root2"),
      leaf2,
      Category("root-symlink", symlinkToCategoryId = "leaf1")
    ),
    Seq.empty
  )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("BonsaiSnapshot")(
      test("Находит листьевые категории из корня") {
        assert(testSnaphot.leafCategories(""))(hasSameElements(Seq(leaf1, leaf2)))
      },
      test("Находит листьевые категории от не листьевой категории") {
        assert(testSnaphot.leafCategories("root1"))(hasSameElements(Seq(leaf1)))
      },
      test("Находит листьевые категории от листьевой категории") {
        assert(testSnaphot.leafCategories("leaf1"))(hasSameElements(Seq(leaf1)))
      },
      test("Находит листьевые категории от категории-симлинка") {
        assert(testSnaphot.leafCategories("root-symlink"))(hasSameElements(Seq(leaf1)))
      },
      test("Не находит листьевые категории, если их нет") {
        assert(testSnaphot.leafCategories("not-leaf1"))(isEmpty)
      }
    )
  }
}
