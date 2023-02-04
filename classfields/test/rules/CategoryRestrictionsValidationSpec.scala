package ru.yandex.vertis.general.wizard.meta.rules

import general.bonsai.category_model.{Category, CategoryState}
import org.mockito.invocation.InvocationOnMock
import ru.yandex.vertis.general.wizard.core.service.BonsaiService
import ru.yandex.vertis.general.wizard.meta.rules.impl.CategoryRestrictionsValidation
import ru.yandex.vertis.general.wizard.model.ParseStateInfo.{Discarded, Valid}
import ru.yandex.vertis.general.wizard.model.{MetaWizardRequest, ParseState, RequestMatch}
import ru.yandex.vertis.mockito.MockitoSupport
import zio.Task
import zio.test.Assertion._
import zio.test._

object CategoryRestrictionsValidationSpec extends DefaultRunnableSpec with MockitoSupport {

  private val categories: Seq[Category] =
    Seq(
      Category(id = "hidden", state = CategoryState.FORBIDDEN),
      Category(id = "archived", state = CategoryState.ARCHIVED),
      Category(id = "symlink", symlinkToCategoryId = "some category", state = CategoryState.DEFAULT),
      Category(
        id = "valid_category",
        symlinkToCategoryId = "",
        state = CategoryState.DEFAULT
      )
    )

  private val bonsaiService: BonsaiService.Service = mock[BonsaiService.Service]

  when(bonsaiService.categoryById(?)).thenAnswer((invocationOnMock: InvocationOnMock) =>
    Task.succeed(categories.find(_.id == invocationOnMock.getArgument[String](0)))
  )

  private val validationNode = CategoryRestrictionsValidation(bonsaiService)

  private def stateWithCategory(categoryIdOpt: Option[String]) =
    ParseState
      .empty(
        MetaWizardRequest.empty("")
      )
      .copy(categoryMatch = categoryIdOpt.map(id => RequestMatch.Category.userInputIndices(Set.empty[Int], id, 0)))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CategoryRestrictionsValidation RuleNode")(
      testM("return valid state for valid category") {
        for {
          state <- validationNode.validate(
            stateWithCategory(Some("valid_category"))
          )
        } yield assert(state)(equalTo(Valid))
      },
      testM("return discarded for hidden category") {
        for {
          state <- validationNode.validate(
            stateWithCategory(Some("hidden"))
          )
        } yield assert(state)(equalTo(Discarded("Category is forbidden")))
      },
      testM("return discarded for archived category") {
        for {
          state <- validationNode.validate(
            stateWithCategory(Some("archived"))
          )
        } yield assert(state)(equalTo(Discarded("Category is archived")))
      },
      testM("return discarded for symlink category") {
        for {
          state <- validationNode.validate(
            stateWithCategory(Some("symlink"))
          )
        } yield assert(state)(equalTo(Discarded("Category is symlink to another category with id: some category")))
      },
      testM("fail on unknown category") {
        val result = validationNode
          .validate(
            stateWithCategory(Some("some_unknown"))
          )
          .run

        assertM(result)(fails(hasMessage(containsString("Category not found n bonsai!"))))
      }
    )
}
