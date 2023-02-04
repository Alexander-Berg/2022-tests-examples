package ru.auto.tests.utils

object ParameterizedUtils {

  def parameterize[T <: Product](gen: Array[T]): Array[Array[Any]] = {
    gen.map(_.productIterator.toArray)
  }

}
