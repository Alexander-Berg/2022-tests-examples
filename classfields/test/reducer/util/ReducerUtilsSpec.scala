package ru.vertistraf.cost_plus.builder.reducer.util

import ru.vertistraf.cost_plus.builder.model.reducer.Collapsed
import ru.vertistraf.cost_plus.builder.reducer.utils.ReducerUtils
import zio.ZIO
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._

object ReducerUtilsSpec extends DefaultRunnableSpec {

  case class Elem(by: Int)

  object Elem {
    def makeSeq(ints: Seq[Int]): Seq[Elem] = ints.map(Elem(_))

    def makeRepeat(value: Int, times: Int): Seq[Elem] =
      makeSeq(
        Iterator.continually(value).take(times).toSeq
      )
  }

  case class TakeTopNSpec(
      input: Seq[Elem],
      expected: Seq[Elem],
      topTake: Int,
      overrideName: String = "")

  case class CollapseSpec(
      input: Seq[Elem],
      expected: Seq[Collapsed[Int, Int]],
      overrideName: String = "")

  private def collapseTests(specs: CollapseSpec*) =
    suite("should correctly do collapse")(
      specs.zipWithIndex
        .map { case (CollapseSpec(input, expected, on), index) =>
          testM(Some(on).filterNot(_.isBlank).getOrElse(s"spec #$index")) {
            ZStream
              .fromIterable(input)
              .run(
                ReducerUtils.collapse[Elem, Int, Int](_.by)(
                  newInstance = _ => 1,
                  reduce = (x, _) => x + 1
                )
              )
              .map(actual => assert(actual)(hasSameElements(expected)))
          }
        }: _*
    )

  private def takeTopNTests(specs: TakeTopNSpec*) =
    suite("should correctly do takeTopN")(
      specs.zipWithIndex
        .map { case (TakeTopNSpec(input, expected, topTake, on), index) =>
          testM(Some(on).filterNot(_.isBlank).getOrElse(s"spec #$index")) {
            ZStream
              .fromIterable(input)
              .run(ReducerUtils.takeTopNSink[Elem, Int](topTake)(_.by))
              .map(actual => assertTrue(actual == expected))
          }
        }: _*
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ReducerUtils")(
      takeTopNTests(
        TakeTopNSpec(
          Elem.makeSeq(1 to 10),
          Elem.makeSeq(10 to 1 by -1),
          topTake = 11,
          overrideName = "return all sorted"
        ),
        TakeTopNSpec(
          Elem.makeSeq(1 to 10),
          Elem.makeSeq(10 to 6 by -1),
          topTake = 5
        ),
        TakeTopNSpec(
          Elem.makeRepeat(2, 3) ++
            Elem.makeRepeat(1, 2) ++
            Elem.makeRepeat(2, 3),
          Elem.makeRepeat(2, 6) :+ Elem(1),
          topTake = 7
        )
      ),
      collapseTests(
        CollapseSpec(
          Elem.makeSeq(1 to 5) ++
            Elem.makeRepeat(1, 2) ++
            Elem.makeRepeat(4, 3),
          Seq[Collapsed[Int, Int]](
            Collapsed(1, 3),
            Collapsed(2, 1),
            Collapsed(3, 1),
            Collapsed(4, 4),
            Collapsed(5, 1)
          )
        )
      ),
      testM("collapse should not die on error from instance and reduce") {
        val zSinks = Iterable(
          ReducerUtils.collapse[Elem, Int, Int](_.by)(_ => throw new RuntimeException, _ + _.by),
          ReducerUtils.collapse[Elem, Int, Int](_.by)(_.by, (_, _) => throw new RuntimeException)
        )

        checkAllM(Gen.fromIterable(zSinks)) { t =>
          ZStream
            .fromIterable(Elem.makeRepeat(1, 10))
            .run(t)
            .map(Some(_))
            .catchAll(_ => ZIO.none)
            .map { res =>
              assertTrue(res.isEmpty)
            }
        }

      }
    )

}
