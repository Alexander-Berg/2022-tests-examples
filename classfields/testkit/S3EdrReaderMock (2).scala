package vsmoney.auction.services.testkit

import common.zio.s3edr.S3EdrReader
import ru.yandex.extdata.core.DataType
import zio.test.mock._
import zio.test.mock.Mock
import zio.{Has, RIO, URLayer, ZLayer}
import zio.blocking.Blocking
import zio.stream._

object S3EdrReaderMock extends Mock[Has[S3EdrReader]] {
  object Data extends Effect[DataType, Throwable, ZStream[Blocking, Throwable, Byte]]
  object Updates extends Stream[DataType, Throwable, DataType]

  override val compose: URLayer[Has[Proxy], Has[S3EdrReader]] = ZLayer.fromService { proxy =>
    new S3EdrReader {
      override def data(dataType: DataType): RIO[Blocking, ZStream[Blocking, Throwable, Byte]] =
        proxy(Data, dataType)

      override def updates(dataType: DataType): ZStream[Any, Throwable, DataType] =
        ZStream.fromEffect(proxy(Updates, dataType)).flatten
    }
  }
}
