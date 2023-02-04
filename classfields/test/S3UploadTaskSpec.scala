package billing.howmuch.scheduler

import billing.common_model.Project.{AUTORU, REALTY}
import billing.common_model.{Money, Project}
import billing.howmuch.model.RuleCriteria.Value.{DefinedValue, Fallback}
import billing.howmuch.model.{Matrix, RuleContext, RuleCriteria}
import billing.howmuch.{model => proto}
import cats.data.NonEmptyList
import common.time.Interval
import common.zio.s3edr.testkit.TestS3EdrUploader
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.billing.common.money.Kopecks
import ru.yandex.vertis.billing.howmuch.model.core.Source.StartrekTicket
import ru.yandex.vertis.billing.howmuch.model.core._
import ru.yandex.vertis.billing.howmuch.model.core.criteria.CriteriaKey
import ru.yandex.vertis.billing.howmuch.scheduler.S3UploadTask
import ru.yandex.vertis.billing.howmuch.storage.ActualRuleDao
import ru.yandex.vertis.billing.howmuch.storage.ydb.YdbActualRuleDao
import ru.yandex.vertis.s3edr.core.storage.DataType
import zio.ZLayer
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{before, sequential}
import zio.test._
import zio.test.environment.TestEnvironment

import java.time.Instant
import java.time.temporal.ChronoUnit.{DAYS, MILLIS}

object S3UploadTaskSpec extends DefaultRunnableSpec {

  def spec: ZSpec[TestEnvironment, Any] = (suite("S3UploadTaskSpec")(
    testM("upload rule to s3") {
      for {
        testFrom <- testFromInPast
        testRule =
          rule(AUTORU, matrixId = "test_matrix", context = "column1=value1&column2->*", from = testFrom, Kopecks(3000))
        _ <- runTx(ActualRuleDao.upsert(NonEmptyList.of(testRule)))
        _ <- S3UploadTask.run
        uploaded <- getMatrixDataType("AUTORU:test_matrix")
      } yield {
        val expectedCriteriaList =
          List(RuleCriteria("column1", DefinedValue("value1")), RuleCriteria("column2", Fallback(true)))
        val expectedContext = RuleContext(expectedCriteriaList)
        assert(uploaded)(
          isSome(equalTo(Matrix(AUTORU, "test_matrix", Seq(proto.Rule(Some(expectedContext), Some(Money(3000)))))))
        )
      }
    },
    testM("upload several rules for same matrix to s3") {
      for {
        testFrom <- testFromInPast
        testRule1 =
          rule(REALTY, matrixId = "test_matrix", context = "column1=value1&column2->*", from = testFrom, Kopecks(4000))
        testRule2 =
          rule(REALTY, matrixId = "test_matrix", context = "column1=value2&column2->*", from = testFrom, Kopecks(5000))
        _ <- runTx(ActualRuleDao.upsert(NonEmptyList.of(testRule1, testRule2)))
        _ <- S3UploadTask.run
        uploaded <- getMatrixDataType("REALTY:test_matrix")
      } yield {
        val expectedCriteriaList1 =
          List(RuleCriteria("column1", DefinedValue("value1")), RuleCriteria("column2", Fallback(true)))
        val expectedCriteriaList2 =
          List(RuleCriteria("column1", DefinedValue("value2")), RuleCriteria("column2", Fallback(true)))
        val expectedContext1 = RuleContext(expectedCriteriaList1)
        val expectedContext2 = RuleContext(expectedCriteriaList2)
        // при загрузке правил в S3 они сортируются по (key.context.toString, interval.from)
        // здесь надо сохранять этот порядок, так как здесь ещё проверяется, в каком порядке они туда (в S3) легли
        val expectedRules = Seq(
          proto.Rule(Some(expectedContext1), Some(Money(4000))),
          proto.Rule(Some(expectedContext2), Some(Money(5000)))
        )
        val expectedMatrix = Matrix(REALTY, "test_matrix", expectedRules)
        assert(uploaded)(isSome(equalTo(expectedMatrix)))
      }
    },
    testM("upload all of a lot of rules for same matrix to s3") {
      for {
        testFrom <- testFromInPast
        testRules = (1 to 10000)
          .map(i =>
            rule(REALTY, matrixId = "test_matrix", context = s"column1=$i&column2->*", from = testFrom, Kopecks(4000))
          )
          .toList
        _ <- runTx(ActualRuleDao.upsert(NonEmptyList.fromListUnsafe(testRules)))
        _ <- S3UploadTask.run
        uploaded <- getMatrixDataType("REALTY:test_matrix")
      } yield {
        val expectedRules = (1 to 10000)
          .map(i => List(RuleCriteria("column1", DefinedValue(s"$i")), RuleCriteria("column2", Fallback(true))))
          .map(RuleContext(_))
          .map(context => proto.Rule(Some(context), Some(Money(4000))))
        uploaded match {
          case Some(uploaded) =>
            assert(uploaded.project)(equalTo(REALTY)) &&
              assert(uploaded.matrixId)(equalTo("test_matrix")) &&
              assert(uploaded.rules)(hasSameElements(expectedRules))
          case None =>
            val fail = assert(uploaded)(nothing)
            fail
        }
      }
    },
    testM("upload rules from different matrices to different s3 files") {
      for {
        testFrom <- testFromInPast
        testRule1 =
          rule(AUTORU, matrixId = "test_matrix", context = "column1=value1&column2->*", from = testFrom, Kopecks(4000))
        testRule2 =
          rule(REALTY, matrixId = "test_matrix", context = "column1=value2&column2->*", from = testFrom, Kopecks(5000))
        _ <- runTx(ActualRuleDao.upsert(NonEmptyList.of(testRule1, testRule2)))
        _ <- S3UploadTask.run
        uploaded1 <- getMatrixDataType("AUTORU:test_matrix")
        uploaded2 <- getMatrixDataType("REALTY:test_matrix")
      } yield {
        val expectedCriteriaList1 =
          List(RuleCriteria("column1", DefinedValue("value1")), RuleCriteria("column2", Fallback(true)))
        val expectedCriteriaList2 =
          List(RuleCriteria("column1", DefinedValue("value2")), RuleCriteria("column2", Fallback(true)))
        val expectedContext1 = RuleContext(expectedCriteriaList1)
        val expectedContext2 = RuleContext(expectedCriteriaList2)
        val expectedRule1 = proto.Rule(Some(expectedContext1), Some(Money(4000)))
        val expectedRule2 = proto.Rule(Some(expectedContext2), Some(Money(5000)))
        assert(uploaded1)(isSome(equalTo(Matrix(AUTORU, "test_matrix", Seq(expectedRule1))))) &&
        assert(uploaded2)(isSome(equalTo(Matrix(REALTY, "test_matrix", Seq(expectedRule2)))))
      }
    },
    testM("upload empty matrix if it exists in schema, but doesn't have any rules") {
      for {
        _ <- S3UploadTask.run
        uploaded <- getMatrixDataType("AUTORU:test_matrix")
      } yield {
        assert(uploaded)(isSome(equalTo(Matrix(AUTORU, "test_matrix", rules = Nil))))
      }
    }
  ) @@ sequential @@ before(TestYdb.clean("actual_rule")))
    .provideCustomLayer {
      TestYdb.ydb >+> YdbActualRuleDao.live ++ TestS3EdrUploader.live[Matrix] ++ testSchema >+>
        S3UploadTask.live ++ Ydb.txRunner ++ Clock.live
    }

  // чтобы случайно не попасть на TTL записей по actual_rule.to в YDB, отсчитываем от текущего времени
  // YDB обрубает timestamp до микросекунд, поэтому здесь тоже обрубаем, чтобы тесты не падали из-за различия в мс
  private val testFromInPast =
    zio.clock.instant
      .map(_.minus(1, DAYS).truncatedTo(MILLIS))

  private def rule(project: Project, matrixId: String, context: String, from: Instant, price: Kopecks) =
    Rule(
      RuleKey(MatrixId(project, matrixId), Context.fromString(context)),
      Interval(from, to = None),
      price,
      StartrekTicket("VSMONEY-2750")
    )

  private val testSchema =
    ZLayer.succeed(
      Schema(
        List(AUTORU, REALTY)
          .map(MatrixId(_, "test_matrix"))
          .map { matrixId =>
            TimedSchema(
              matrixId,
              Interval(Instant.ofEpochMilli(0), to = None),
              ContextSchema(List(CriteriaKey("test"))),
              allowFallback = false
            )
          },
        historyUploadAllowed = Set()
      )
    )

  private def getMatrixDataType(dataType: String) =
    TestS3EdrUploader.get[Matrix](DataType(dataType, format = 1))
}
