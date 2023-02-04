package ru.yandex.vertis.vsquality.hobo.util

import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

import org.scalatestplus.scalacheck.Checkers.check
import ru.yandex.vertis.vsquality.hobo.model.{Crosscheck, UserId}
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer._
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.vsquality.hobo.service.TaskServiceHelper

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

/**
  * @author potseluev
  */

@nowarn
class DaoUtilSpec extends SpecBase {

  import DaoUtil._
  import TaskServiceHelper.CrosscheckTaskVerdictComparator

  private def crosscheckGen(queue: QueueId, user: UserId): Gen[Crosscheck] =
    for {
      initialUser    <- Gen.oneOf(Gen.const(user), UserIdGen)
      checkUser      <- if (initialUser == user) UserIdGen else Gen.const(user)
      initialTask    <- TaskGen.map(_.copy(owner = Some(initialUser), queue = queue))
      checkTask      <- TaskGen.map(_.copy(owner = Some(checkUser), queue = queue))
      validationTask <- Gen.option(Gen.const(if (initialUser == user) checkTask else initialTask))
    } yield Crosscheck(initialTask, checkTask, validationTask)

  case class TransformTestCase[T, A](
      description: String,
      gen: Gen[T],
      mapper: Seq[T] => A,
      reducer: (A, A) => A,
      expectedResultCalculator: Option[Seq[T] => A] = None)

  private val transformTestCases =
    Seq(
      TransformTestCase(
        description = "correctly count sum",
        gen = Gen.chooseNum(-1000, 1000),
        mapper = (_: Seq[Int]).sum,
        reducer = (a: Int, b: Int) => a + b
      ),
      TransformTestCase[String, Map[String, Int]](
        description = "correctly count number of words",
        gen = Gen.alphaChar.map(_.toString),
        mapper = (_: Seq[String]).groupBy(identity).view.mapValues(_.size).toMap,
        reducer = (a: Map[String, Int], b: Map[String, Int]) =>
          (a.keySet ++ b.keySet)
            .map(key => key -> (a.getOrElse(key, 0) + b.getOrElse(key, 0)))
            .toMap
      ), {
        val queue = QueueIdGen.next
        val user = UserIdGen.next
        TransformTestCase(
          description = "correctly build crosscheck report",
          gen = crosscheckGen(queue, user),
          mapper = TaskServiceHelper.buildReport(user, queue),
          reducer = TaskServiceHelper.mergeReports
        )
      }
    )

  "transform" should {

    def seqGen[T](elemGen: Gen[T]): Gen[Seq[T]] =
      for {
        n      <- Gen.chooseNum(0, 1000)
        seqGen <- Gen.listOfN(n, elemGen)
      } yield seqGen

    def toScannable[T](source: Seq[T])(slice: Slice): Future[SlicedResult[T]] =
      Future.fromTry(
        Try(source.slice(slice.from, slice.to))
          .map(SlicedResult(_, source.size, slice))
      )

    def getBatchSize(sourceSize: Int): Int = Gen.chooseNum(1, sourceSize + 1).next

    transformTestCases.foreach { case TransformTestCase(description, elemGen, mapper, reducer, optExpectedResult) =>
      description in {
        val sourceGen = seqGen(elemGen)
        check(forAll(sourceGen) { source =>
          val batchSize = getBatchSize(source.size)
          val actualResult =
            transform(
              scannable = toScannable(source),
              mapper = mapper.andThen(Future.successful),
              reducer = reducer,
              batchSize = batchSize
            ).futureValue
          val expectedResultCalculator = optExpectedResult.getOrElse(mapper)
          actualResult == expectedResultCalculator(source)
        })
      }
    }

  }

}
