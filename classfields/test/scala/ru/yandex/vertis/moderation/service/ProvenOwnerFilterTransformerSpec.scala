package ru.yandex.vertis.moderation.service

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  AutoruEssentialsGen,
  DealerMetadataGen,
  InstanceGen,
  ProvenOwnerMetadataGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.model.meta.{Metadata, MetadataSet}
import ru.yandex.vertis.moderation.service.ProvenOwnerFilterTransformerSpec.ProvenOwnerFilterTestCase
import ru.yandex.vertis.moderation.service.impl.transformer.{
  FilterMetadataTransformer,
  ProvenOwnerFilterMetadataTransformer
}

/**
  * Specs for [[ProvenOwnerFilterMetadataTransformer]]
  *
  * @author molokovskikh
  */
@RunWith(classOf[JUnitRunner])
class ProvenOwnerFilterTransformerSpec extends SpecBase {

  private val transformer: FilterMetadataTransformer = ProvenOwnerFilterMetadataTransformer
  private val vin1 = "one_vin"
  private val vin2 = "another_vin"

  private val provenOwnerFilterTestCases: Seq[ProvenOwnerFilterTestCase] =
    Seq(
      {
        val autoruEssentials = AutoruEssentialsGen.next.copy(vin = Some(vin1))
        val provenOwnerMetadata = ProvenOwnerMetadataGen.next.copy(vin = Some(vin1))
        val instance =
          InstanceGen.next.copy(
            essentials = autoruEssentials,
            metadata = MetadataSet(provenOwnerMetadata)
          )
        ProvenOwnerFilterTestCase(
          description = "pass meta if vin in Metadata is the same as vin from essentials",
          instance = instance,
          metadata = provenOwnerMetadata,
          expectedResult = true
        )
      }, {
        val autoruEssentials = AutoruEssentialsGen.next.copy(vin = Some(vin2))
        val provenOwnerMetadata = ProvenOwnerMetadataGen.next.copy(vin = Some(vin1))
        val instance =
          InstanceGen.next.copy(
            essentials = autoruEssentials,
            metadata = MetadataSet(provenOwnerMetadata)
          )
        ProvenOwnerFilterTestCase(
          description = "skip meta if vin in Metadata is different from vin from essentials",
          instance = instance,
          metadata = provenOwnerMetadata,
          expectedResult = false
        )
      }, {
        val autoruEssentials = AutoruEssentialsGen.next.copy(vin = None)
        val provenOwnerMetadata = ProvenOwnerMetadataGen.next.copy(vin = Some(vin1))
        val instance =
          InstanceGen.next.copy(
            essentials = autoruEssentials,
            metadata = MetadataSet(provenOwnerMetadata)
          )
        ProvenOwnerFilterTestCase(
          description = "skip meta if there is no vin in essentials",
          instance = instance,
          metadata = provenOwnerMetadata,
          expectedResult = false
        )
      }, {
        val autoruEssentials = AutoruEssentialsGen.next.copy(vin = Some(vin1))
        val anotherMetadata = DealerMetadataGen.next
        val instance =
          InstanceGen.next.copy(
            essentials = autoruEssentials,
            metadata = MetadataSet(anotherMetadata)
          )
        ProvenOwnerFilterTestCase(
          description = "pass meta if there is no proven_owner metadata",
          instance = instance,
          metadata = anotherMetadata,
          expectedResult = true
        )
      }
    )

  "ProvenOwnerFilterMetadataTransformer" should {
    provenOwnerFilterTestCases.foreach {
      case ProvenOwnerFilterTestCase(description, instance, metadata, expectedResult) =>
        description in {
          transformer(instance, metadata) shouldBe expectedResult
        }
    }
  }
}

object ProvenOwnerFilterTransformerSpec {

  case class ProvenOwnerFilterTestCase(description: String,
                                       instance: Instance,
                                       metadata: Metadata,
                                       expectedResult: Boolean
                                      )
}
