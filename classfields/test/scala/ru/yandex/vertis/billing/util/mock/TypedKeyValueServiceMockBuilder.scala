package ru.yandex.vertis.billing.util.mock

import ru.yandex.vertis.billing.service.TypedKeyValueService
import ru.yandex.vertis.billing.util.KeyValueSerDe

import scala.util.Success

/**
  * @author tolmach
  */
case class TypedKeyValueServiceMockBuilder() extends MockBuilder[TypedKeyValueService] {

  private val m: TypedKeyValueService = mock[TypedKeyValueService]

  def withGetMock[T](key: String, value: T)(implicit c: KeyValueSerDe[T]): TypedKeyValueServiceMockBuilder = {
    when(m.get[T](key)).thenReturn(Success(value))
    this
  }

  def withSetMock[T](key: String, value: T)(implicit c: KeyValueSerDe[T]): TypedKeyValueServiceMockBuilder = {
    when(m.set[T](key, value)).thenReturn(Success(()))
    this
  }

  def build: TypedKeyValueService = m

}
