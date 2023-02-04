package auto.dealers.match_maker.util

import java.util.UUID

import ru.auto.api.search.SearchModel.{CatalogFilter, SearchRequestParameters}
import ru.auto.match_maker.model.api.ApiModel._
import auto.dealers.match_maker.util.ApiExceptions.MatchApplicationCreationException
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object MatchApplicationValidatorSpec extends DefaultRunnableSpec {

  def spec =
    suite("MatchApplicationValidator")(
      testM("Success when everything OK") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map(_.build())
          result <- MatchApplicationValidator.validate(app).either
        } yield assert(result)(isRight(anything))
      },
      testM("fail when user info is empty") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map(_.clearUserInfo().build())
          result <- MatchApplicationValidator.validate(app).either
        } yield MatchApplicationValidatorSpecUtils
          .defaultTestFailureAssert(
            result,
            "Отсутствует информация о пользователе"
          )
      },
      testM("fail when user phone has wrong format") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map { default =>
                default.getUserInfoBuilder.setPhone("wrong")
                default.build()
              }
          result <- MatchApplicationValidator.validate(app).either
        } yield MatchApplicationValidatorSpecUtils
          .defaultTestFailureAssert(
            result,
            "Телефон пользователя имеет неправильный формат"
          )
      },
      testM("fail when user id is empty") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map { default =>
                default.getUserInfoBuilder.clearUserId()
                default.build()
              }
          result <- MatchApplicationValidator.validate(app).either
        } yield MatchApplicationValidatorSpecUtils
          .defaultTestFailureAssert(
            result,
            "Отсутствует идентификатор пользователя"
          )
      },
      testM("fail when user phone is empty") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map { default =>
                default.getUserInfoBuilder.clearPhone()
                default.build()
              }
          result <- MatchApplicationValidator.validate(app).either
        } yield MatchApplicationValidatorSpecUtils
          .defaultTestFailureAssert(
            result,
            "Телефон пользователя не указан"
          )
      },
      testM("fail when target list non empty") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map(_.addTarget(Target.getDefaultInstance).build())
          result <- MatchApplicationValidator.validate(app).either
        } yield MatchApplicationValidatorSpecUtils
          .defaultTestFailureAssert(
            result,
            "Не пустой список возможных офферов при создании заявки"
          )
      },
      testM("fail when info about user proposal is empty") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map(_.clearUserProposal().build())
          result <- MatchApplicationValidator.validate(app).either
        } yield MatchApplicationValidatorSpecUtils
          .defaultTestFailureAssert(
            result,
            "Отсутствует информация о пожеланиях пользователя"
          )
      },
      testM("fail when user proposal is empty") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map { default =>
                default.getUserProposalBuilder.clearSearchParams()
                default.build()
              }
          result <- MatchApplicationValidator.validate(app).either
        } yield MatchApplicationValidatorSpecUtils
          .defaultTestFailureAssert(
            result,
            "Отсутствуют пожелания пользователя"
          )
      },
      testM("fail when rid list is empty") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map { default =>
                default.getUserProposalBuilder.getSearchParamsBuilder.clearRid()
                default.build()
              }
          result <- MatchApplicationValidator.validate(app).either
        } yield MatchApplicationValidatorSpecUtils
          .defaultTestFailureAssert(
            result,
            "Отсутствует регион поиска"
          )
      },
      testM("fail when user proposal mark is not present") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map { default =>
                default.getUserProposalBuilder.getSearchParamsBuilder.clearCatalogFilter()
                default.build()
              }
          result <- MatchApplicationValidator.validate(app).either
        } yield MatchApplicationValidatorSpecUtils
          .defaultTestFailureAssert(
            result,
            "Отсутствует желаемая марка автомобиля"
          )
      },
      testM("fail when user proposal mark is empty") {
        for {
          app <-
            MatchApplicationValidatorSpecUtils.getDefaultMatchApplicationBuilder
              .map { default =>
                default.getUserProposalBuilder.getSearchParamsBuilder.getCatalogFilterBuilder(0).setMark("")
                default.build()
              }
          result <- MatchApplicationValidator.validate(app).either
        } yield MatchApplicationValidatorSpecUtils
          .defaultTestFailureAssert(
            result,
            "Марка не может быть пустой"
          )
      },
      test("valid phone validation returns true") {
        assertTrue(MatchApplicationValidator.validPhone("+71234567890") == true)
      },
      test("invalid phone validation returns false") {
        assertTrue(MatchApplicationValidator.validPhone("321-kaboom") == false)
      }
    )
}

object MatchApplicationValidatorSpecUtils {

  def getDefaultMatchApplicationBuilder =
    for {
      id <- ZIO.effectTotal(UUID.randomUUID().toString)
      userInfo <- ZIO.effectTotal(
        UserInfo
          .newBuilder()
          .setName("someName")
          .setUserId(72)
          .setPhone("+79999999999")
      )
      userProposal <- ZIO.effectTotal(
        UserProposal
          .newBuilder()
          .setSearchParams(
            SearchRequestParameters
              .newBuilder()
              .addCatalogFilter(CatalogFilter.newBuilder().setMark("BMW"))
              .addRid(213)
          )
      )
      app <- ZIO.effectTotal(
        MatchApplication
          .newBuilder()
          .setId(id)
          .setUserInfo(userInfo)
          .setUserProposal(userProposal)
      )
    } yield app

  def defaultTestFailureAssert(result: Either[Throwable, Unit], errorMessage: String) =
    assert(result)(
      isLeft(
        isSubtype[MatchApplicationCreationException](
          hasField(
            "errors",
            _.errors,
            exists(hasField[CreationError, String]("message", _.getMessage, equalTo(errorMessage)))
          )
        )
      )
    )
}
