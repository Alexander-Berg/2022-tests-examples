package ru.yandex.vertis.billing

import java.io.IOException

import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType._
import com.google.protobuf.{ByteString, Message}
import org.scalacheck.Gen
import ru.yandex.vertis.billing.Model.CampaignHeader
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.JavaConversions._
import scala.util.{Failure, Success}

/**
  * Test gens and mocks
  *
  * @author alesavin
  */
package object gens extends MockitoSupport {

  /**
    * Generates [[Message]] instances for specified builder
    * Set base type fields by arbitrary values, variate optional and repeated
    * @param builder message builder
    */
  def get[T <: Message.Builder](builder: T): Gen[Message] = {

    def _getRepeated(field: FieldDescriptor): Gen[Any] =
      if (field.isRepeated) {
        Gen.choose(0, 15).flatMap(i => Gen.sequence(Iterable.fill(i)(_getTyped(field))))
      } else if (field.isOptional) {
        Gen.oneOf(_getTyped(field), Gen.fail)
      } else {
        _getTyped(field)
      }

    def _getTyped(field: FieldDescriptor): Gen[Any] =
      field.getType.getJavaType match {
        case BOOLEAN => Gen.oneOf(true, false)
        case STRING => Gen.alphaStr
        case INT => Gen.posNum[Int]
        case LONG => Gen.posNum[Long]
        case BYTE_STRING => Gen.alphaStr.map(s => ByteString.copyFrom(s.getBytes))
        case DOUBLE => Gen.posNum[Double]
        case FLOAT => Gen.posNum[Float]
        case ENUM => Gen.oneOf(field.getEnumType.getValues)
        case MESSAGE => get(builder.newBuilderForField(field))
      }

    builder.getDescriptorForType.getFields.foreach { field =>
      val gen = _getRepeated(field)
      gen.sample match {
        case Some(value) => builder.setField(field, value)
        case None => Unit
      }
    }

    Gen.const(builder.build())
  }

  def campaignHeaderProducer =
    Iterator.continually(get(Model.CampaignHeader.newBuilder()).sample.asInstanceOf[Option[CampaignHeader]]).flatten

  val CampaignsClientMock = {
    val m = mock[CampaignsClient]
    when(m.getCampaignHeaders).thenReturn(Success(campaignHeaderProducer.take(10).toList))
    when(m.getCampaignHeaders(?)).thenReturn(Success(campaignHeaderProducer.take(10).toList))

    when(m.getValuableCampaigns()).thenReturn(Success(campaignHeaderProducer.take(10).toList))
    when(m.getValuableCampaigns(?)).thenReturn(Success(campaignHeaderProducer.take(10).toList))

    stub(m.getCampaignHeader _) {
      case ("1", _) => Success(Some(campaignHeaderProducer.next()))
      case ("2", _) => Success(None)
      case ("3", _) => Failure(new IOException("io"))
      case ("4", _) => Failure(new IllegalArgumentException("illegal argument"))
    }

    m
  }
}
