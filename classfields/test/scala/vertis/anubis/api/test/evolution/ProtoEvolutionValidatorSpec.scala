package vertis.anubis.api.test.evolution

import vertis.anubis.api.services.validate.validators.evolution.EvolutionValidator
import vertis.anubis.api.services.validate.validators.evolution.EvolutionValidatorBase.{FieldDeletion, FieldTypeChange}
import vertis.anubis.api.test._
import vertis.zio.test.ZioSpecBase

/** @author reimai
  */
class ProtoEvolutionValidatorSpec extends ZioSpecBase with EvolutionValidatorSpecBase with EvolutionValidatorSpecWorld {

  override protected val validator = new EvolutionValidator(evolutionWorld)

  "ProtoEvolutionValidator" should {
    Iterator(AfterDeleted.getDescriptor).foreach { d =>
      s"detect deleted fields in ${d.getName}" in testEvolution(
        d,
        List(FieldDeletion(originalIndex, root = root))
      )
    }

    Iterator(AfterChanged.getDescriptor).foreach { d =>
      s"allow compatible type change int32 -> uint32 in ${d.getName}" in testEvolution(
        d,
        Nil
      )
    }

    Iterator(AfterReserved.getDescriptor).foreach { d =>
      s"allow reserved deleted fields in ${d.getName}" in testEvolution(d, List.empty)
    }

    "detect field container change for numeric types" in testEvolution(
      AfterRepeated.getDescriptor,
      List(
        FieldTypeChange(
          originalIndex,
          AfterRepeated.getDescriptor.findFieldByName(index),
          root = root
        )
      )
    )

    "allow compatible type change for repeated fields" in testEvolution(
      AfterRepeatedChanged.getDescriptor,
      Nil
    )

    "detect repeated message field type change" in testEvolution(
      AfterRepeatedMessageChanged.getDescriptor,
      List(
        FieldTypeChange(
          Before.getDescriptor.findFieldByNumber(3).getMessageType.findFieldByNumber(1),
          AfterRepeatedMessageChanged.getDescriptor.findFieldByNumber(3).getMessageType.findFieldByNumber(1),
          root = root,
          path = Seq("ts")
        ),
        FieldDeletion(
          Before.getDescriptor.findFieldByNumber(3).getMessageType.findFieldByNumber(2),
          root = root,
          path = Seq("ts")
        )
      )
    )

    "work with recursive proto" in testEvolution(
      RecursiveMessage.getDescriptor,
      Nil
    )

    "not allow wrapping" in testEvolution(
      AfterWrapped.getDescriptor,
      List(
        FieldTypeChange(
          originalIndex,
          AfterWrapped.getDescriptor.findFieldByName(index),
          root = root
        )
      )
    )

    "not allow unwrapping" in testEvolution(
      AfterUnwrapped.getDescriptor,
      List(
        FieldTypeChange(
          Before.getDescriptor.findFieldByName(id),
          AfterUnwrapped.getDescriptor.findFieldByName(id),
          root = root
        )
      )
    )

    "not allow wrapper type change" in testEvolution(
      AfterWrappedChange.getDescriptor,
      List(
        FieldTypeChange(
          Before.getDescriptor.findFieldByName(id).getMessageType.findFieldByName("value"),
          AfterWrappedChange.getDescriptor.findFieldByName(id).getMessageType.findFieldByName("value"),
          root = root,
          path = Seq("id")
        )
      )
    )

    "not allow incompatible message type change" in testEvolution(
      WithInnerBar.getDescriptor,
      List(
        FieldTypeChange(
          Foo.getDescriptor.findFieldByName(id),
          Bar.getDescriptor.findFieldByName(id),
          root = WithInnerFoo.getDescriptor.getFullName,
          path = Seq("inner")
        )
      )
    )

    "allow compatible message type change" in testEvolution(
      WithInnerBaz.getDescriptor,
      List.empty
    )

    "not allow incompatible inner message type change" in testEvolution(
      DeepInnerBar.getDescriptor,
      List(
        FieldTypeChange(
          Foo.getDescriptor.findFieldByName(id),
          Bar.getDescriptor.findFieldByName(id),
          root = DeepInnerFoo.getDescriptor.getFullName,
          path = Seq("inner", "foo")
        )
      )
    )

    "allow compatible inner message type change" in testEvolution(
      DeepInnerBaz.getDescriptor,
      List.empty
    )

    "not allow moving a field into an existing oneof" in testEvolution(
      AfterWithExistingOneOf.getDescriptor,
      List(
        FieldTypeChange(
          AfterWithNewOneOf.getDescriptor.findFieldByName("baz"),
          AfterWithExistingOneOf.getDescriptor.findFieldByName("baz"),
          root = AfterWithNewOneOf.getDescriptor.getFullName
        )
      )
    )
  }
}
