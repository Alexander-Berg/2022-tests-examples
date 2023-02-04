package ru.yandex.vertis.general.category_matcher.logic.test

import common.zio.clients.s3.testkit.TestS3
import common.zio.doobie.testkit.TestPostgresql
import general.bonsai.category_model.{Category, CategoryState}
import general.category_matcher.api.{
  Match => ApiMatch,
  MatchWithNamespace => ApiMatchWithNamespace,
  Namespace => ApiNamespace,
  ValidationFailed,
  ValidationReport
}
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.category_matcher.logic.{CategoryMatchManager, ValidationManager}
import ru.yandex.vertis.general.category_matcher.model._
import ru.yandex.vertis.general.category_matcher.storage.postgresql.{PgCategoryDao, PgMatchDao, PgTagDao, PgTagValueDao}
import zio.{Ref, ZLayer}
import zio.test.TestAspect.{sequential, shrinks}
import zio.test.{suite, testM, DefaultRunnableSpec, ZSpec}
import zio.test._
import zio.test.Assertion._

object ValidationManagerSpec extends DefaultRunnableSpec {
  def matchKey(id: String) = Match.Key(CategoryId(id), Map.empty)
  def matchKeyApi(id: String) = ApiMatch.Key(id, Map.empty)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("ValidationManager")(
      testM("Находит ошибки") {
        for {
          _ <- CategoryMatchManager.update(Namespace.Avito, Match(matchKey("1"), List("missing")))
          _ <- CategoryMatchManager.update(Namespace.Avito, Match(matchKey("2"), List("missing", "correct")))
          _ <- CategoryMatchManager.update(Namespace.Avito, Match(matchKey("3"), List("archived")))
          _ <- CategoryMatchManager.update(Namespace.Avito, Match(matchKey("4"), List("archived", "correct")))
          _ <- CategoryMatchManager.update(Namespace.Avito, Match(matchKey("5"), List("symlink")))
          _ <- CategoryMatchManager.update(Namespace.Avito, Match(matchKey("6"), List("symlink", "correct")))
          report <- ValidationManager.validate
        } yield assert(report)(
          equalTo(
            ValidationReport(
              ValidationReport.Result.Failed(
                ValidationFailed(
                  missingCategories = Seq(
                    ApiMatchWithNamespace(ApiNamespace.AVITO, Some(ApiMatch(Some(matchKeyApi("1")), List("missing")))),
                    ApiMatchWithNamespace(ApiNamespace.AVITO, Some(ApiMatch(Some(matchKeyApi("2")), List("missing"))))
                  ),
                  archivedCategories = Seq(
                    ApiMatchWithNamespace(ApiNamespace.AVITO, Some(ApiMatch(Some(matchKeyApi("3")), List("archived")))),
                    ApiMatchWithNamespace(ApiNamespace.AVITO, Some(ApiMatch(Some(matchKeyApi("4")), List("archived"))))
                  ),
                  symlinkCategories = Seq(
                    ApiMatchWithNamespace(ApiNamespace.AVITO, Some(ApiMatch(Some(matchKeyApi("5")), List("symlink")))),
                    ApiMatchWithNamespace(ApiNamespace.AVITO, Some(ApiMatch(Some(matchKeyApi("6")), List("symlink"))))
                  )
                )
              )
            )
          )
        )
      }
    ) @@ sequential @@ shrinks(0)).provideCustomLayerShared {
      val dao = PgCategoryDao.live ++ PgMatchDao.live ++ PgTagDao.live ++ PgTagValueDao.live
      val categoryMatchManager = (TestPostgresql.managedTransactor >+> dao) >>> CategoryMatchManager.live
      val s3 = TestS3.mocked

      val bonsaiRef = Ref
        .make(
          BonsaiSnapshot(
            Seq(
              Category("archived", state = CategoryState.ARCHIVED),
              Category("correct", state = CategoryState.DEFAULT),
              Category("symlink", symlinkToCategoryId = "correct", state = CategoryState.DEFAULT)
            ),
            Seq.empty
          )
        )
        .toLayer
      val validationConfig = ZLayer.succeed(ValidationManager.Config("bucket", "key"))

      val validationManager = (categoryMatchManager ++ s3 ++ bonsaiRef ++ validationConfig) >>> ValidationManager.live
      categoryMatchManager ++ validationManager
    }
  }
}
