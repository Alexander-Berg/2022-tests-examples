package vertis.broker.api.parse

import com.google.protobuf.DynamicMessage
import vertis.broker.api.parse.Parser._
import vertis.broker.api.validate.{AnotherMessage, Bar, SimpleMessage}
import Generators.{genProtoFor, next}
import vertis.broker.api.produce.ProducerSessionManager.SessionError.InvalidSchema
import zio.test.Assertion._
import zio.test._

/** @author kusaeva
  */
object ParserSpec extends DefaultRunnableSpec {

  override def spec =
    suite("ParserSpec")(
      testM("successfully parse descriptor") {
        val instance = SimpleMessage.getDefaultInstance
        val protoGen = genProtoFor(instance)

        checkM(protoGen) { proto =>
          for {
            parser <- Parser.make(SimpleMessage.getDescriptor)
            _ <- parser.parseProto(proto.toByteString)
          } yield assertCompletes
        }
      },
      testM("fail with InvalidProtoBuffer error when parsing wrong proto") {
        val descriptor = SimpleMessage.getDescriptor
        val badDescriptor = AnotherMessage.getDescriptor
        val instance = DynamicMessage.getDefaultInstance(badDescriptor)
        val protoGen = genProtoFor(instance)

        for {
          bin <- next(protoGen.sample)
          parser <- Parser.make(descriptor)
          result <- parser.parseProto(bin).run
        } yield assert(result)(fails(isSubtype[InvalidProtoBuffer](Assertion.anything)))
      },
      testM("fail with InvalidSchema error when parsing message without broker annotation") {
        val descriptor = Bar.getDescriptor
        for {
          parser <- Parser.make(descriptor).run
        } yield assert(parser)(fails(isSubtype[InvalidSchema](Assertion.anything)))
      }
    )
}
