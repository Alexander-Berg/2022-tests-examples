package ru.yandex.realty.ci.backend.flow.stage.io

import ru.yandex.realty.AsyncSpecBase

/**
  * @author abulychev
  */
class StageEvalSpec extends AsyncSpecBase {

  "StageEval" should {
    "correctly work when values are provided after definition" in {
      val x = StageEval[Int]()
      val y = StageEval[String]()
      val z = StageEval[Int]()

      val l = for {
        vx <- x
        vy <- y
        vz <- z
      } yield vx + vy.length + vz

      x <<= 1
      y <<= "abc"
      z <<= 5

      l.getOrElse(0) should be(9)
    }

    "correctly work when values are provided before definition" in {
      val x = StageEval[Int]()
      val y = StageEval[String]()
      val z = StageEval[Int]()

      x <<= 1
      y <<= "abc"
      z <<= 5

      val l = for {
        vx <- x
        vy <- y
        vz <- z
      } yield vx + vy.length + vz

      l.getOrElse(0) should be(9)
    }
  }

}
