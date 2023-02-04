package vertis.broker.model

import com.google.protobuf.{ByteString, Message}
import org.scalacheck.Gen
import ru.yandex.vertis.broker.requests.WriteRequest.WriteData
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import vertis.broker.api.model.ProducerMessage

import scala.reflect.ClassTag

/** @author zvez
  */
object ModelGenerators extends ProducerProvider {

  val readableString: Gen[String] = for {
    length <- Gen.choose(5, 10)
    s <- Gen.listOfN(length, Gen.alphaNumChar)
  } yield s.mkString

  val producerMessage: Gen[ProducerMessage] = for {
    id <- readableString
    data <- Gen.nonEmptyListOf(Gen.choose[Byte](0, Byte.MaxValue)).map(_.toArray)
  } yield ProducerMessage(id, 0, data, data.size)

  def messageSequence: Iterator[ProducerMessage] = producerMessage.values.zipWithIndex.map { case (msg, idx) =>
    msg.copy(seqNo = idx.toLong)
  }

  def writeDataSequence[T <: Message: ClassTag]: Iterator[WriteData] =
    RandomProtobufGenerator.genForAuto[T].values.zipWithIndex.map { case (msg, seqNo) =>
      WriteData(
        seqNo = seqNo + 1L,
        data = ByteString.copyFrom(msg.toByteArray),
        createTimeMs = System.currentTimeMillis()
      )
    }

}
