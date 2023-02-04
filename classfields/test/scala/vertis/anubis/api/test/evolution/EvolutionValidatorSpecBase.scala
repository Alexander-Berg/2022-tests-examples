package vertis.anubis.api.test.evolution

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.{DescriptorProtos, GeneratedMessage}
import vertis.anubis.api.services.ValidationWorld
import vertis.anubis.api.services.validate.validators.ProtoValidator
import vertis.anubis.api.services.validate.validators.evolution.{EvolutionValidator, EvolutionValidatorBase}
import vertis.anubis.api.services.validate.validators.evolution.EvolutionValidatorBase.{
  EvolutionViolation,
  FieldEvolutionViolation
}
import vertis.anubis.api.test.ValidationSpecBase
import vertis.zio.test.ZioSpecBase

/** @author ruslansd
  */
trait EvolutionValidatorSpecBase extends ValidationSpecBase {
  this: ZioSpecBase =>

  protected def validator: ProtoValidator

  protected def testEvolution(
      after: Descriptor,
      expectedErrors: List[EvolutionViolation]): Unit =
    ioTest {
      if (expectedErrors.nonEmpty) {
        logger.info(s"Expected errors: ${expectedErrors.map(_.errorMessage).mkString("\n")}") *>
          checkFail(
            validator,
            after,
            expectedErrors
          )
      } else {
        checkSucceed(validator, after)
      }
    }
}

object EvolutionValidatorSpecBase {

  class MadWorld(newToOld: Map[Descriptor, Descriptor]) extends ValidationWorld {

    override def getDescriptorsByOption[T](
        option: GeneratedMessage.GeneratedExtension[DescriptorProtos.MessageOptions, T],
        changedOnly: Boolean): Seq[Descriptor] = newToOld.keys.toSeq

    override def listDescriptors[T](changedOnly: Boolean): Seq[Descriptor] = Seq.empty

    override def findDescriptorInMaster(d: Descriptor): Option[Descriptor] =
      newToOld.get(d).orElse(Some(d))

    override def findDeliveryInMaster(d: Descriptor): Option[Descriptor] =
      findDescriptorInMaster(d)

    override def findDescriptor(name: String): Option[Descriptor] =
      newToOld.find(_._2.getName == name).map(_._2)

    override def getChanges: Set[String] = Set.empty
  }
}
