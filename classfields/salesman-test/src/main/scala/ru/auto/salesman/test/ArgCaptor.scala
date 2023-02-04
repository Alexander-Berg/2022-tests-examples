package ru.auto.salesman.test

import org.mockito.ArgumentCaptor

import scala.reflect.ClassTag

/** Enables short syntax for creation of mockito [[ArgumentCaptor]]s:
  * ```
  * val captor = ArgCaptor[MyClass]
  * ```
  */
object ArgCaptor {

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def apply[A](implicit ct: ClassTag[A]): ArgumentCaptor[A] =
    ArgumentCaptor.forClass(ct.runtimeClass.asInstanceOf[Class[A]])
}
