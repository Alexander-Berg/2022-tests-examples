package ru.auto.salesman.test

import scala.util.Random

object RandomUtil {

  def chooseRandom[A](in: Iterable[A]): Iterable[A] =
    Random.shuffle(in).take(Random.nextInt(in.size + 1))
}
