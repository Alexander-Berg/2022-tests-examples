package ru.auto.catalog.core.model.raw.layers.cars.impl

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw._
import ru.auto.catalog.core.model.raw.layers.cars.CarsSearchTagsInheritanceDecider.Decision
import ru.auto.catalog.core.model.raw.layers.cars.configuration.ConfigurationLayerBuilder.ConfigurationSearchTags
import ru.auto.catalog.core.model.raw.layers.cars.impl.CarsSearchTagsInheritanceDeciderImplSpec._
import ru.auto.catalog.core.model.raw.layers.cars.mark_model.MarkModelLayerBuilder.{MarkSearchTags, ModelSearchTags}
import ru.auto.catalog.core.model.raw.layers.cars.super_gen.SuperGenLayerBuilder.SuperGenerartionSearchTags
import ru.auto.catalog.core.testkit.syntax._

class CarsSearchTagsInheritanceDeciderImplSpec extends BaseSpec {

  "CarsSearchTagsInheritanceDeciderImplSpec.decide" should {
    testCases.foreach { testCase =>
      import testCase._
      description in {
        val decider = new CarsSearchTagsInheritanceDeciderImpl(tagsInheriteByChilds, tagsInheritedByParents)
        val actual = decider.decide(markSearchTags, modelSearchTags, superGenerationSearchTags, configurationSearchTags)
        actual shouldBe expected
      }
    }
  }
}

object CarsSearchTagsInheritanceDeciderImplSpec {

  private val Mark1: MarkId = mark"MERCEDES"
  private val Mark2: MarkId = mark"BMW"
  private val Mark3: MarkId = mark"KIA"

  private val Configuration1: ConfigurationId = configuration"22300940"
  private val SuperGeneration1: SuperGenerationId = superGeneration"22300940"

  private val Model1: ModelId = model"E500"
  private val Model2: ModelId = model"A200"
  private val Model3: ModelId = model"X5"
  private val Model4: ModelId = model"X6"
  private val Model5: ModelId = model"RIO"
  private val Model6: ModelId = model"CEED"

  private val SearchTag1: SearchTag = searchTag"handling"
  private val SearchTag2: SearchTag = searchTag"new4new"
  private val SearchTag3: SearchTag = searchTag"prestige"

  private val configurationSearchTags: Set[ConfigurationSearchTags] = Set(
    ConfigurationSearchTags(Configuration1, SuperGeneration1, Set(SearchTag1))
  )

  private val markSearchTags: Set[MarkSearchTags] = Set(
    MarkSearchTags(Mark1, Set(SearchTag1)),
    MarkSearchTags(Mark2, Set.empty),
    MarkSearchTags(Mark3, Set.empty)
  )

  private val modelSearchTags: Set[ModelSearchTags] = Set(
    ModelSearchTags(Mark1, Model1, Set.empty),
    ModelSearchTags(Mark1, Model2, Set(SearchTag1, SearchTag2)),
    ModelSearchTags(Mark2, Model3, Set(SearchTag1, SearchTag3)),
    ModelSearchTags(Mark2, Model4, Set.empty),
    ModelSearchTags(Mark3, Model5, Set.empty),
    ModelSearchTags(Mark3, Model6, Set.empty)
  )

  private val superGenerationSearchTags: Set[SuperGenerartionSearchTags] = Set(
    SuperGenerartionSearchTags(Mark1, Model1, superGeneration"1.1.1", Set.empty),
    SuperGenerartionSearchTags(Mark1, Model1, superGeneration"1.1.2", Set.empty),
    SuperGenerartionSearchTags(Mark1, Model2, superGeneration"1.2.1", Set.empty),
    SuperGenerartionSearchTags(Mark1, Model2, superGeneration"1.2.2", Set.empty),
    SuperGenerartionSearchTags(Mark2, Model3, superGeneration"2.1.1", Set.empty),
    SuperGenerartionSearchTags(Mark2, Model3, superGeneration"2.1.2", Set.empty),
    SuperGenerartionSearchTags(Mark2, Model4, superGeneration"2.2.1", Set.empty),
    SuperGenerartionSearchTags(Mark2, Model4, superGeneration"2.2.2", Set.empty),
    SuperGenerartionSearchTags(Mark3, Model5, superGeneration"3.1.1", Set.empty),
    SuperGenerartionSearchTags(Mark3, Model5, superGeneration"3.1.2", Set.empty),
    SuperGenerartionSearchTags(Mark3, Model6, superGeneration"3.2.1", Set.empty),
    SuperGenerartionSearchTags(Mark3, Model6, superGeneration"3.2.2", Set(SearchTag2))
  )

  private case class TestCase(
      description: String,
      markSearchTags: Set[MarkSearchTags] = markSearchTags,
      modelSearchTags: Set[ModelSearchTags] = modelSearchTags,
      superGenerationSearchTags: Set[SuperGenerartionSearchTags] = superGenerationSearchTags,
      configurationSearchTags: Set[ConfigurationSearchTags] = configurationSearchTags,
      tagsInheriteByChilds: Set[SearchTag],
      tagsInheritedByParents: Set[SearchTag],
      expected: Set[Decision])

  private val testCases: Seq[TestCase] = Seq(
    TestCase(
      description = "inheritance by childs",
      tagsInheriteByChilds = Set(SearchTag1),
      tagsInheritedByParents = Set.empty,
      expected = Set(
        Decision(Decision.ModelItem(Mark1, Model1), Set(SearchTag1)),
        Decision(Decision.ModelItem(Mark1, Model2), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"1.1.1"), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"1.1.2"), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"1.2.1"), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"1.2.2"), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"2.1.1"), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"2.1.2"), Set(SearchTag1))
      )
    ),
    TestCase(
      description = "inheritance by parents",
      tagsInheriteByChilds = Set.empty,
      tagsInheritedByParents = Set(SearchTag2),
      expected = Set(
        Decision(Decision.MarkItem(Mark1), Set(SearchTag2)),
        Decision(Decision.MarkItem(Mark3), Set(SearchTag2)),
        Decision(Decision.ModelItem(Mark3, Model6), Set(SearchTag2))
      )
    ),
    TestCase(
      description = "inheritance by childs and parents",
      tagsInheriteByChilds = Set(SearchTag1),
      tagsInheritedByParents = Set(SearchTag2),
      expected = Set(
        Decision(Decision.ModelItem(Mark1, Model1), Set(SearchTag1)),
        Decision(Decision.ModelItem(Mark1, Model2), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"1.1.1"), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"1.1.2"), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"1.2.1"), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"1.2.2"), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"2.1.1"), Set(SearchTag1)),
        Decision(Decision.SuperGenerationItem(superGeneration"2.1.2"), Set(SearchTag1)),
        Decision(Decision.MarkItem(Mark1), Set(SearchTag2)),
        Decision(Decision.MarkItem(Mark3), Set(SearchTag2)),
        Decision(Decision.ModelItem(Mark3, Model6), Set(SearchTag2))
      )
    )
  )
}
