package vertis.anubis.api.test.evolution

import com.google.protobuf.Descriptors
import vertis.anubis.api.test.evolution.EvolutionValidatorSpecBase.MadWorld
import vertis.anubis.api.test._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait EvolutionValidatorSpecWorld {

  protected val id = "id"
  protected val data = "data"
  protected val index = "index"
  protected val ts = "ts"
  protected val originalIndex: Descriptors.FieldDescriptor = Before.getDescriptor.findFieldByName(index)
  protected val root: String = Before.getDescriptor.getFullName

  protected val evolutionWorld = new MadWorld(
    Map(
      AfterDeleted.getDescriptor -> Before.getDescriptor,
      AfterChanged.getDescriptor -> Before.getDescriptor,
      AfterReserved.getDescriptor -> Before.getDescriptor,
      AfterRepeated.getDescriptor -> Before.getDescriptor,
      AfterWrapped.getDescriptor -> Before.getDescriptor,
      AfterUnwrapped.getDescriptor -> Before.getDescriptor,
      AfterRepeatedChanged.getDescriptor -> AfterRepeated.getDescriptor,
      AfterRepeatedMessageChanged.getDescriptor -> Before.getDescriptor,
      AfterRepeatedMessageInnerChanged.getDescriptor -> Before.getDescriptor,
      RecursiveMessage.getDescriptor -> Before.getDescriptor,
      AfterWrappedChange.getDescriptor -> Before.getDescriptor,
      WithInnerBar.getDescriptor -> WithInnerFoo.getDescriptor,
      WithInnerBaz.getDescriptor -> WithInnerBar.getDescriptor,
      DeepInnerBar.getDescriptor -> DeepInnerFoo.getDescriptor,
      DeepInnerBaz.getDescriptor -> DeepInnerBar.getDescriptor,
      PayloadAfter.getDescriptor -> PayloadBefore.getDescriptor,
      RichEventAfter.getDescriptor -> RichEventBefore.getDescriptor,
      AfterWithExistingOneOf.getDescriptor -> AfterWithNewOneOf.getDescriptor,
      AfterWithNewOneOf.getDescriptor -> BeforeWoOneOf.getDescriptor,
      DescendantEvent.getDescriptor -> AncestorEvent.getDescriptor
    )
  )
}
