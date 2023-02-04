package ru.auto.salesman.service

import ru.auto.salesman.test.BaseSpec

class AutostrategyInstanceGenImplSpec extends BaseSpec {

  val gen = new AutostrategyInstanceGenImpl

  "AutostrategyInstanceGenImpl" should {

    "not throw IllegalArgumentException from Random.nextInt(0) on too many times" in {
      gen.genToday(3000) shouldBe Nil
    }
  }
}
