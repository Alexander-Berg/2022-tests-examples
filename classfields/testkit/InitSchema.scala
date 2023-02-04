package common.zio.ydb.testkit

import ru.yandex.vertis.ydb.zio.YdbZioWrapper
import zio.{Has, RIO}

import scala.io.{Codec, Source}

object InitSchema {

  def apply(path: String): RIO[Has[YdbZioWrapper], Unit] = {
    val stream = getClass.getResourceAsStream(path)
    val schema = Source.fromInputStream(stream)(Codec.UTF8).mkString
    RIO.service[YdbZioWrapper].flatMap(_.executeSchema(schema))
  }

}
