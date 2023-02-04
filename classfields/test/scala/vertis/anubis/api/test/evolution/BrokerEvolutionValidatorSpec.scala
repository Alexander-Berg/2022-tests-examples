package vertis.anubis.api.test.evolution

import vertis.anubis.api.services.validate.validators.broker.BrokerEvolutionValidator
import vertis.anubis.api.services.validate.validators.evolution.EvolutionValidatorBase.{FieldDeletion, FieldTypeChange}
import vertis.anubis.api.test._
import vertis.zio.test.ZioSpecBase

/** @author reimai
  */
class BrokerEvolutionValidatorSpec
  extends ZioSpecBase
  with EvolutionValidatorSpecBase
  with EvolutionValidatorSpecWorld {

  override protected val validator = new BrokerEvolutionValidator(evolutionWorld)

  "BrokerEvolutionValidator" should {
    Iterator(AfterDeleted.getDescriptor).foreach { d =>
      s"detect deleted fields in ${d.getName}" in testEvolution(
        d,
        List(FieldDeletion(originalIndex, root = root))
      )
    }

    Iterator(AfterChanged.getDescriptor).foreach { d =>
      s"detect changed fields in ${d.getName}" in testEvolution(
        d,
        List(
          FieldTypeChange(
            originalIndex,
            AfterChanged.getDescriptor.findFieldByName(index),
            root = root
          )
        )
      )
    }

    Iterator(AfterReserved.getDescriptor).foreach { d =>
      s"allow reserved deleted fields in ${d.getName}" in testEvolution(d, List.empty)
    }

    "detect field container change" in testEvolution(
      AfterRepeated.getDescriptor,
      List(
        FieldTypeChange(
          originalIndex,
          AfterRepeated.getDescriptor.findFieldByName(index),
          root = root
        )
      )
    )

    "forbid proto compatible changes in repeated fields" in testEvolution(
      AfterRepeatedChanged.getDescriptor,
      List(
        FieldTypeChange(
          AfterRepeated.getDescriptor.findFieldByName(index),
          AfterRepeatedChanged.getDescriptor.findFieldByName(index),
          root = AfterRepeated.getDescriptor.getFullName
        )
      )
    )

    "detect repeated message field type change" in testEvolution(
      AfterRepeatedMessageChanged.getDescriptor,
      List(
        FieldDeletion(
          Before.getDescriptor.findFieldByName(ts).getMessageType.findFieldByName("seconds"),
          root = root,
          path = Seq("ts")
        ),
        FieldDeletion(
          Before.getDescriptor.findFieldByName(ts).getMessageType.findFieldByName("nanos"),
          root = root,
          path = Seq("ts")
        )
      )
    )

    "work with recursive proto" in testEvolution(
      RecursiveMessage.getDescriptor,
      List(
        FieldTypeChange(
          originalIndex,
          RecursiveMessage.getDescriptor.findFieldByName(index),
          root = root
        )
      )
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

    "forbid changing inner message to repeated" in testEvolution(
      RichEventAfter.getDescriptor,
      List(
        FieldTypeChange(
          PayloadBefore.getDescriptor.findFieldByName(data),
          PayloadAfter.getDescriptor.findFieldByName(data),
          root = RichEventBefore.getDescriptor.getFullName,
          path = Seq("payload")
        )
      )
    )

    "allow moving a field into a new oneof" in testEvolution(
      AfterWithNewOneOf.getDescriptor,
      List.empty
    )

    "forbid delivery evolution through new message creation" in testEvolution(
      DescendantEvent.getDescriptor,
      List(
        FieldTypeChange(
          AncestorEvent.getDescriptor.findFieldByName(id),
          DescendantEvent.getDescriptor.findFieldByName(id),
          root = AncestorEvent.getDescriptor.getFullName
        )
      )
    )
  }
}
