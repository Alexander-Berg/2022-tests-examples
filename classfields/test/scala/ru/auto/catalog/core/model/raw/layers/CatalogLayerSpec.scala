package ru.auto.catalog.core.model.raw.layers

import cats.syntax.option._
import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw.RawFilterWrapper
import ru.auto.catalog.core.model.raw.RawFilterWrapper.Mode
import ru.auto.catalog.core.model.raw.OrderedCatalogLevel
import shapeless.{::, HNil}
import ru.auto.catalog.model.api.ApiModel.ErrorMode
import ru.auto.catalog.core.util.ApiExceptions
import cats.data.Chain
import shapeless.HList
import ru.auto.catalog.core.model.raw.layers.Selection.NoCriteria
import ru.auto.catalog.core.model.raw.LevelRange

import scala.annotation.nowarn

@nowarn
class CatalogLayerSpec extends BaseSpec {

  // We use a tiny catalog tree that allows us to check various edge cases without much boilerplate and without
  // elaborate test resources. Identifiers below the model level are made up, and have a shape that should make it
  // easier to follow Identifiers below the model level are made up, and have structure that should make it easier to
  // follow the behavior.

  // BMW
  // ├── X1
  // |   ├── X1 SG1
  // |   │   ├── X1 SG1 C1
  // |   │   └── X1 SG1 C2
  // |   └── X2 SG2
  // │       └── X2 SG2 C1
  // └── X6
  //     └── X6 SG1
  //         └── X6 SG1 C1
  // FORD
  // └── FOCUS
  //     └── FOCUS SG1
  //         └── FOCUS SG1 C1

  val markLayer = CatalogLayer.Root(
    OrderedCatalogLevel.MARK,
    FakeContent("BMW", "FORD")
  )

  val modelLayer = CatalogLayer.NonRoot(
    OrderedCatalogLevel.MODEL,
    FakeContent("X1", "X6", "FOCUS"),
    markLayer,
    Map("X1" -> Set("BMW"), "X6" -> Set("BMW"), "FOCUS" -> Set("FORD"))
  )

  val superGenLayer = CatalogLayer.NonRoot(
    OrderedCatalogLevel.SUPER_GEN,
    FakeContent("X1 SG1", "X1 SG2", "X6 SG1", "FOCUS SG1"),
    modelLayer,
    Map("X1 SG1" -> Set("X1"), "X1 SG2" -> Set("X1"), "X6 SG1" -> Set("X6"), "FOCUS SG1" -> Set("FOCUS"))
  )

  val configurationLayer = CatalogLayer.NonRoot(
    OrderedCatalogLevel.CONFIGURATION,
    FakeContent("X1 SG1 C1", "X1 SG1 C2", "X1 SG2 C1", "X6 SG1 C1", "FOCUS SG1 C1"),
    superGenLayer,
    Map(
      "X1 SG1 C1" -> Set("X1 SG1"),
      "X1 SG1 C2" -> Set("X1 SG1"),
      "X1 SG2 C1" -> Set("X1 SG2"),
      "X6 SG1 C1" -> Set("X6 SG1"),
      "FOCUS SG1 C1" -> Set("FOCUS SG1")
    ),
    canIgnoreCriterion = true
  )
  val catalog = configurationLayer

  private def checkSelection(
      mark: Option[String => Boolean] = None,
      model: Option[String => Boolean] = None,
      superGen: Option[String => Boolean] = None,
      configuration: Option[String => Boolean] = None,
      errorMode: ErrorMode = ErrorMode.FAIL_FAST
    )(expectSuccess: Option[Selection[String :: String :: String :: String :: HNil]] = None,
      expectWarnings: Chain[LayerWarning] = Chain.empty,
      expectError: Option[ApiExceptions.FilterException => Unit] = None) = {
    // It should be possible to prove this through types, but it's not worth the trouble.
    val expectSuccess0 = expectSuccess.map(_.asInstanceOf[Selection[catalog.PrependIds[HNil]]])
    val criteria = (configuration :: superGen :: model :: mark :: HNil).asInstanceOf[catalog.Criteria]

    val result = catalog.getSelection(criteria, errorMode).run

    expectSuccess0.foreach(expected => result.right.value._2 shouldBe expected)
    result.right.map(_._1).getOrElse(Chain.empty) shouldBe expectWarnings
    expectError.foreach(check => check(result.left.value))
  }

  private def checkExtract(
      mode: Mode.Value,
      selection: Selection[String :: String :: String :: String :: HNil],
      from: OrderedCatalogLevel = OrderedCatalogLevel.MARK,
      to: OrderedCatalogLevel = OrderedCatalogLevel.CONFIGURATION
    )(expectedMarks: Set[String],
      expectedModels: Set[String] = Set.empty,
      expectedSuperGens: Set[String] = Set.empty,
      expectedConfigurations: Set[String] = Set.empty) = {
    // It should be possible to prove this through types, but it's not worth the trouble.
    val selection0 = selection.asInstanceOf[Selection[catalog.PrependIds[HNil]]]

    val configurationsMap :: superGensMap :: modelsMap :: marksMap :: HNil =
      catalog.extract(selection0, mode, LevelRange(from = from, to = to))

    marksMap.keySet shouldBe expectedMarks
    modelsMap.keySet shouldBe expectedModels
    superGensMap.keySet shouldBe expectedSuperGens
    configurationsMap.keySet shouldBe expectedConfigurations
  }

  private def sel[Id, LowerIds <: HList](pairs: (Id, Selection[LowerIds])*): Selection[Id :: LowerIds] =
    Selection.SelectedIds(pairs.toMap)

  "CatalogLayer#getSelection" should {
    // This is handled separately.
    "succeed for empty criteria" in checkSelection()(NoCriteria.some)

    "select paths through layers that don't have criteria" in checkSelection(
      superGen = Set("X1 SG1").some
    )(sel("BMW" -> sel("X1" -> sel("X1 SG1" -> NoCriteria))).some)

    "select independent paths" in checkSelection(
      superGen = Set("X1 SG1", "FOCUS SG1").some
    )(
      sel(
        "BMW" -> sel("X1" -> sel("X1 SG1" -> NoCriteria)),
        "FORD" -> sel("FOCUS" -> sel("FOCUS SG1" -> NoCriteria))
      ).some
    )

    "select overlapping paths (Root)" in checkSelection(
      model = Set("X1", "X6").some
    )(sel("BMW" -> sel("X1" -> NoCriteria, "X6" -> NoCriteria)).some)

    "select overlapping paths (NonRoot)" in checkSelection(
      superGen = Set("X1 SG1", "X1 SG2").some
    )(sel("BMW" -> sel("X1" -> sel("X1 SG1" -> NoCriteria, "X1 SG2" -> NoCriteria))).some)

    "select paths through layers that have criteria, but only on match (Root)" in checkSelection(
      mark = Set("BMW").some,
      superGen = Set("X1 SG1", "FOCUS SG1").some
    )(sel("BMW" -> sel("X1" -> sel("X1 SG1" -> NoCriteria))).some)

    "select paths through layers that have criteria, but only on match (NonRoot)" in checkSelection(
      model = Set("X1").some,
      superGen = Set("X1 SG1", "FOCUS SG1").some
    )(sel("BMW" -> sel("X1" -> sel("X1 SG1" -> NoCriteria))).some)

    "fail if a criterion produces an empty set (Root)" in checkSelection(
      mark = Set.empty.some
    )(expectError = Some { e =>
      e shouldBe a[ApiExceptions.NotFoundFilterException]
      e.level shouldBe OrderedCatalogLevel.MARK
    })

    "fail if a criterion produces an empty set (NonRoot, FAIL_FAST)" in checkSelection(
      model = Set.empty.some,
      errorMode = ErrorMode.FAIL_FAST
    )(expectError = Some { e =>
      e shouldBe a[ApiExceptions.NotFoundFilterException]
      e.level shouldBe OrderedCatalogLevel.MODEL
    })

    "fail if a criterion produces an empty set (NonRoot, FAIL_NEVER)" in checkSelection(
      model = Set.empty.some,
      errorMode = ErrorMode.FAIL_NEVER
    )(expectError = Some { e =>
      e shouldBe a[ApiExceptions.NotFoundFilterException]
      e.level shouldBe OrderedCatalogLevel.MODEL
    })

    "fail if the criteria produce an empty set (Root)" in checkSelection(
      mark = Set("FORD").some,
      model = Set("X1").some
    )(expectError = Some { e =>
      e shouldBe a[ApiExceptions.BadRequestFilterException]
      e.level shouldBe OrderedCatalogLevel.MARK
    })

    "fail if the criteria produce an empty set (NonRoot)" in checkSelection(
      model = Set("FOCUS").some,
      superGen = Set("X1 SG1").some
    )(expectError = Some { e =>
      e shouldBe a[ApiExceptions.BadRequestFilterException]
      e.level shouldBe OrderedCatalogLevel.MODEL
    })

    "recover with canIgnoreCriterion with FAIL_NEVER" in checkSelection(
      model = Set("X1").some,
      configuration = Set("no such configuration").some,
      errorMode = ErrorMode.FAIL_NEVER
    )(Some(sel("BMW" -> sel("X1" -> NoCriteria))), Chain.one(LayerWarning(OrderedCatalogLevel.CONFIGURATION)))

    "ignore canIgnoreCriterion with FAIL_FAST" in checkSelection(
      model = Set("X1").some,
      configuration = Set("no such configuration").some,
      errorMode = ErrorMode.FAIL_FAST
    )(expectError = Some(_ shouldBe a[ApiExceptions.NotFoundFilterException]))
  }

  "CatalogLayer#extract" should {
    "produce expected results (Exact, empty)" in checkExtract(
      Mode.Exact,
      NoCriteria
    )(Set.empty)

    "produce expected results (Exact, nonempty)" in checkExtract(
      Mode.Exact,
      sel("BMW" -> sel("X1" -> sel("X1 SG1" -> NoCriteria)))
    )(Set("BMW"), Set("X1"), Set("X1 SG1"))

    "produce expected results (SubTree, empty)" in checkExtract(
      Mode.SubTree,
      NoCriteria
    )(
      // The whole tree
      Set("BMW", "FORD"),
      Set("X1", "X6", "FOCUS"),
      Set("X1 SG1", "X1 SG2", "X6 SG1", "FOCUS SG1"),
      Set("X1 SG1 C1", "X1 SG1 C2", "X1 SG2 C1", "X6 SG1 C1", "FOCUS SG1 C1")
    )

    "produce expected results (SubTree, nonempty)" in checkExtract(
      Mode.SubTree,
      sel("BMW" -> sel("X1" -> NoCriteria))
    )(
      Set("BMW"),
      Set("X1"),
      Set("X1 SG1", "X1 SG2"),
      Set("X1 SG1 C1", "X1 SG1 C2", "X1 SG2 C1")
    )

    "produce expected results (Siblings, empty)" in checkExtract(
      Mode.Siblings,
      NoCriteria
    )(
      // The whole tree
      Set("BMW", "FORD"),
      Set("X1", "X6", "FOCUS"),
      Set("X1 SG1", "X1 SG2", "X6 SG1", "FOCUS SG1"),
      Set("X1 SG1 C1", "X1 SG1 C2", "X1 SG2 C1", "X6 SG1 C1", "FOCUS SG1 C1")
    )

    "produce expected results (Siblings, nonempty)" in checkExtract(
      Mode.Siblings,
      sel("BMW" -> sel("X1" -> NoCriteria))
    )(
      Set("BMW", "FORD"),
      Set("X1", "X6"),
      Set("X1 SG1", "X1 SG2"),
      Set("X1 SG1 C1", "X1 SG1 C2", "X1 SG2 C1")
    )

    "produce expected results (level range below selection)" in checkExtract(
      Mode.Siblings,
      sel("BMW" -> sel("X1" -> NoCriteria)),
      from = OrderedCatalogLevel.SUPER_GEN,
      to = OrderedCatalogLevel.SUPER_GEN
    )(
      Set.empty,
      Set.empty,
      Set("X1 SG1", "X1 SG2"),
      Set.empty
    )
  }

  case class FakeContent(items: String*)
    extends AbstractCatalogLayerContent[String, String, String => Boolean](items.map(s => s -> s).toMap) {

    override def getCriterion(
        filter: RawFilterWrapper): Option[Criterion] =
      sys.error("Not implemented")

    override def lookup(criterion: String => Boolean): Set[String] = items.filter(criterion).toSet
  }
}
