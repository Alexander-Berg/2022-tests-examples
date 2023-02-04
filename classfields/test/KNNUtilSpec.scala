package ru.yandex.vertis.general.classifiers.models.test

import ru.yandex.vertis.general.classifiers.models.KNNUtil
import zio.test._
import zio.test.Assertion._

object KNNUtilSpec extends DefaultRunnableSpec {

  private def calc(data: (Int, Float)*): Map[Int, Float] = {
    KNNUtil.calcProbabilities(
      data,
      (_: (Int, Float))._2,
      (_: (Int, Float))._1
    )
  }

  def spec =
    suite("KNNUtil")(
      test("Пустая коллекция") {
        val res = calc()
        assert(res)(isEmpty)
      },
      test("Вероятность одного элемента") {
        val res = calc(1 -> 1f)
        assert(res(1))(equalTo(1f))
      },
      test("Нулевая дистанция") {
        val res = calc(1 -> 0f)
        assert(res(1))(equalTo(1f))
      },
      test("Нулевая дистанция #2") {
        val res = calc(1 -> 0f, 2 -> 3f)
        assert(res(1))(equalTo(1f))
      },
      test("Чем ближе тем выше вероятность") {
        val res = calc(1 -> 1f, 2 -> 10f)
        assert(res(1))(isGreaterThan(res(2)))
      },
      test("Одинаковая вероятность на одном расстоянии") {
        val res = calc(1 -> 3f, 2 -> 3f)
        assert(res(1))(equalTo(res(2)))
      },
      test("Вероятности одинаковых элементов складываются") {
        val res = calc(1 -> 3f, 2 -> 3f, 1 -> 4f)
        assert(res)(hasSize(equalTo(2))) &&
        assert(res(1))(isGreaterThan(res(2)))
      },
      test("Вероятности одинаковых элементов") {
        val res = calc(1 -> 10f, 1 -> 10f, 2 -> 1f)
        assert(res(2))(isGreaterThan(res(1)))
      },
      test("Вероятности одинаковых элементов #2") {
        val res = calc(1 -> 1f, 1 -> 1f, 2 -> 1f)
        assert(res(1))(isGreaterThan(res(2)))
      }
    )
}
