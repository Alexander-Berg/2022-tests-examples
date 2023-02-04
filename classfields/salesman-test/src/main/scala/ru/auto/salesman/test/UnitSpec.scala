package ru.auto.salesman.test

trait UnitSpec {

  /** Раньше приходилось писать такую конструкцию:
    * service.run().success.value shouldBe (())
    *
    * 1. Она плохо читается
    * 2. IDEA подкрашивает (()) с предупреждением "unnecessary parenthesis",
    * хотя при убирании внешних скобок код не компилируется.
    *
    * Лучше писать так:
    * service.run().success.value shouldBe unit
    *
    * В VSMONEY-437 начнём использовать её во всех тестах.
    */
  lazy val unit: Unit = ()
}
