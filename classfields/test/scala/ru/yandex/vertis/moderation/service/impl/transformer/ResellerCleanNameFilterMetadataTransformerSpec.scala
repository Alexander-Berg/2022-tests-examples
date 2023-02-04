package ru.yandex.vertis.moderation.service.impl.transformer

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.{
  AutoruEssentialsGen,
  InstanceGen,
  ResellerCleanNameMetadataGen
}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.meta.MetadataSet

@RunWith(classOf[JUnitRunner])
class ResellerCleanNameFilterMetadataTransformerSpec extends SpecBase {
  private val vin1 = "VIN1"
  private val vin2 = "VIN2"

  "ResellerCleanNameFilterMetadataTransformer" should {
    "passes meta when VINs are equal" in {
      filter(instanceVin = Some(vin1), metaVin = Some(vin1)) shouldBe true
    }

    "does not pass meta when VINs are different" in {
      filter(instanceVin = Some(vin1), metaVin = Some(vin2)) shouldBe false
    }

    "does not pass meta when meta's VIN is not set" in {
      filter(instanceVin = Some(vin1), metaVin = None) shouldBe false
    }

    "does not pass meta when instance's VIN is not set" in {
      filter(instanceVin = None, metaVin = Some(vin1)) shouldBe false
    }
  }

  private def filter(instanceVin: Option[String], metaVin: Option[String]) = {
    val metadata = ResellerCleanNameMetadataGen.next.copy(vin = metaVin)
    val instance =
      InstanceGen.next.copy(
        essentials = AutoruEssentialsGen.next.copy(vin = instanceVin),
        metadata = MetadataSet(metadata)
      )
    ResellerCleanNameFilterMetadataTransformer.apply(instance, metadata)
  }
}
