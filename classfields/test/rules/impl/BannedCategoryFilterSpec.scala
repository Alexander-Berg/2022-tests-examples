package ru.yandex.vertis.general.wizard.meta.rules.impl

import org.mockito.invocation.InvocationOnMock
import ru.yandex.vertis.general.wizard.core.service.CategoryTagsService
import ru.yandex.vertis.general.wizard.model.{CategoryTag, MetaWizardRequest, ParseState, ParseStateInfo, RequestMatch}
import ru.yandex.vertis.mockito.MockitoSupport
import zio.Task
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object BannedCategoryFilterSpec extends DefaultRunnableSpec with MockitoSupport {

  private val bannedCategoryId = "bannedCategory"

  private val tagsService = mock[CategoryTagsService.Service]

  when(tagsService.noTag(?, eq(CategoryTag.BannedCategory))).thenAnswer { (invocationOnMock: InvocationOnMock) =>
    val id = invocationOnMock.getArgument[String](0)
    if (id == bannedCategoryId) {
      Task.succeed(false)
    } else {
      Task.succeed(true)
    }
  }

  private val bannedParseState =
    ParseState
      .empty(MetaWizardRequest.empty("Купить забаненное"))
      .copy(categoryMatch =
        Some(
          RequestMatch.Category(RequestMatch.Source.UserRequestTokens(Set(1), false), bannedCategoryId, 0)
        )
      )

  private val emptyParseState = ParseState.empty(MetaWizardRequest.empty("пустой запрос"))

  private val validCategoryParseState =
    ParseState
      .empty(MetaWizardRequest.empty("Купить валидное"))
      .copy(categoryMatch =
        Some(
          RequestMatch.Category(RequestMatch.Source.UserRequestTokens(Set(1), false), "validCategoryId", 0)
        )
      )

  private val bannedCategoryFilter = BannedCategoryFilter(tagsService)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("BannedCategoryFilter")(
      testM("should filter banned category")(
        for {
          parseStateInfo <- bannedCategoryFilter.validate(bannedParseState)
        } yield assert(parseStateInfo)(equalTo(ParseStateInfo.Discarded("Category banned!")))
      ),
      testM("should not filter state with no category")(
        for {
          parseStateInfo <- bannedCategoryFilter.validate(emptyParseState)
        } yield assert(parseStateInfo)(equalTo(ParseStateInfo.Valid))
      ),
      testM("should not filter valid category")(
        for {
          parseStateInfo <- bannedCategoryFilter.validate(validCategoryParseState)
        } yield assert(parseStateInfo)(equalTo(ParseStateInfo.Valid))
      )
    )
}
