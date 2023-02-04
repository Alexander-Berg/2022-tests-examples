package ru.yandex.vertis.util.exenum

/**
  * @author semkagtn
  */
object Enums {

  sealed trait EmptyEnum extends ExEnum
  object EmptyEnum extends ExEnumCompanion[EmptyEnum] {
    override def values: Set[EmptyEnum] = ExEnumCompanion.valuesOf
  }

  sealed abstract class TestEnum(val id: Int) extends ExEnum
  object TestEnum extends ExEnumCompanion[TestEnum] {
    case object Val1 extends TestEnum(1)
    case object Val2 extends TestEnum(2) {
      override def name: String = "two"
    }
    override def values: Set[TestEnum] = ExEnumCompanion.valuesOf
  }
}
