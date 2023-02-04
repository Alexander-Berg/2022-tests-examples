package ru.yandex.vertis.moderation.dao.impl.mysql

import com.google.protobuf.ByteString
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.converters.Protobuf
import ru.yandex.vertis.moderation.model.context.Context
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.protobuf.ProtobufUtils

/**
  * Spec on [[ProtobufUtils]] related to bytes
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class ProtobufUtilsBytesSpec extends SpecBase {

  "ProtobufUtils" should {

    import ProtobufUtils._

    def serializeInstance(instance: Instance): Model.SerializedInstance = {
      val instanceBytes = serializeInstanceOnce(instance).toByteArray
      Model.SerializedInstance.newBuilder.setVersion(1).setInstance(ByteString.copyFrom(instanceBytes)).build()
    }
    def serializeInstanceOnce(instance: Instance): Model.Instance = {
      val instanceWithoutSignalsAndContext =
        instance.copy(
          signals = SignalSet.Empty,
          context = Context.Default
        )
      Protobuf.toMessage(instanceWithoutSignalsAndContext)
    }

    "serialize instance bytes correctly" in {
      val instance = serializeInstance(InstanceGen.next)
      val json = toJson(instance)
      val actualResult = fromJson(Model.SerializedInstance.getDefaultInstance, json)
      val expectedResult = instance
      actualResult should be(expectedResult)
    }
    "serialize max long correctly" in {
      val i =
        InstanceGen.next.copy(
          essentials =
            RealtyEssentialsGen.next.copy(
              clusterId = Some(Long.MaxValue)
            )
        )
      val instance = serializeInstanceOnce(i)
      val json = toJson(instance) // "clusterId": "9223372036854775807",
      val actualResult = fromJson(Model.Instance.getDefaultInstance, json)
      val expectedResult = instance
      actualResult should be(expectedResult)
    }
    "serialize double correctly" in {
      val signal = WarnSignalGen.next.copy(weight = 7.7459938252615705d)
      val serialized = Protobuf.toMessage(signal)
      val json = toJson(serialized)
      val actualResult = fromJson(Model.WarnSignal.getDefaultInstance, json)
      val expectedResult = serialized
      actualResult should be(expectedResult)
    }

  }
}
