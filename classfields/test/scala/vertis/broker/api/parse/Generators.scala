package vertis.broker.api.parse

import com.google.protobuf.{ByteString, DynamicMessage, Message}
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import zio.random.Random
import zio.stream.ZStream
import zio.test.{Gen, Sample}
import zio.{UIO, ZIO}

/** @author kusaeva
  */
object Generators {

  final def next[R](sample: ZStream[R, Nothing, Sample[R, DynamicMessage]]): ZIO[R, Nothing, ByteString] =
    sample.map(_.value).runHead.map(x => x.map(_.toByteString).getOrElse(ByteString.EMPTY))

  def genProtoFor[T <: Message](instance: T): Gen[Random, T] = {
    val generator = new RandomProtobufGenerator(() => instance.newBuilderForType())
    Gen.fromEffect(UIO(generator.generate().asInstanceOf[T]))
  }
}
