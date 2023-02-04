package vertis.broker.pipeline.ch.testkit

import vertis.broker.pipeline.ch.sink.converter._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait ChTestDescriptors {

  lazy val descriptors = Seq(
    WithMessage.getDescriptor,
    WithBytes.getDescriptor,
    WithEnum.getDescriptor,
    WithEnumInProto.getDescriptor,
    WithTimestamp.getDescriptor,
    WithAllWrapped.getDescriptor,
    WithOneOfMessage.getDescriptor,
    TypesMessage.getDescriptor,
    WithRepeated.getDescriptor,
    WithRepeatedWrappedPrimitive.getDescriptor,
    WithRepeatedMessage.getDescriptor,
    WithMap.getDescriptor,
    WithMapOfWrapped.getDescriptor,
    WithMapOfMessage.getDescriptor,
    TestMessage.getDescriptor,
    WithChKeyWord.getDescriptor
  )
}
