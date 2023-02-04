package ru.yandex.vertis.vsquality.utils.test_utils

import org.mockito.stubbing.OngoingStubbing

object MockitoUtil {

  implicit class RichOngoingStubbing[T](val value: OngoingStubbing[T]) extends AnyVal {

    /**
      * In cases when type may be ambigous, because overloaded method value thenReturn has alternatives
      */
    def thenReturnOnly(t: T): OngoingStubbing[T] = value.thenReturn(t, Nil: _*)
  }
}
