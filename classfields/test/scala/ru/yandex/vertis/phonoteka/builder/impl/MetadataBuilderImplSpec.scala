package ru.yandex.vertis.phonoteka.builder.impl

import ru.yandex.vertis.phonoteka.builder.{MetadataBuilder, MetadataMiner}
import ru.yandex.vertis.phonoteka.model.Arbitraries._
import ru.yandex.vertis.phonoteka.model.Phone
import ru.yandex.vertis.phonoteka.model.metadata.{Metadata, MetadataType, OfMetadata}
import ru.yandex.vertis.quality.test_utils.SpecBase

class MetadataBuilderImplSpec extends SpecBase {

  private case class MinerSourceTestCase(description: String,
                                         phones: Set[Phone],
                                         properties: MetadataBuilder.MetadataPropertiesMap,
                                         stored: Set[Metadata],
                                         expected: Set[MetadataMiner.Source]
                                        )

  private val phone1: Phone = generate[Phone]()
  private val phone2: Phone = generate[Phone]()
  private val metadata1 = generate[OfMetadata]().copy(phone = phone1)

  private val minerSourceTestCases: Seq[MinerSourceTestCase] =
    Seq(
      MinerSourceTestCase(
        description = "with phone & greedy, without updateSince & stored",
        phones = Set(phone1),
        properties =
          Map(
            MetadataType.Of -> MetadataBuilder.MetadataTypeProperty(None, greedy = true)
          ),
        stored = Set(metadata1),
        expected = Set.empty
      ),
      MinerSourceTestCase(
        description = "with phone & stored, without greedy",
        phones = Set(phone1),
        properties =
          Map(
            MetadataType.Of -> MetadataBuilder.MetadataTypeProperty(None, greedy = false)
          ),
        stored = Set(metadata1),
        expected = Set.empty
      ),
      MinerSourceTestCase(
        description = "whithout phone",
        phones = Set.empty,
        properties =
          Map(
            MetadataType.Of -> MetadataBuilder.MetadataTypeProperty(None, greedy = true)
          ),
        stored = Set(metadata1),
        expected = Set.empty
      ),
      MinerSourceTestCase(
        description = "whith mismatched phone",
        phones = Set(phone2),
        properties =
          Map(
            MetadataType.Of -> MetadataBuilder.MetadataTypeProperty(None, greedy = true)
          ),
        stored = Set(metadata1),
        expected = Set(MetadataMiner.Source(phone2, MetadataType.Of))
      ),
      MinerSourceTestCase(
        description = "with phone & greedy, without stored",
        phones = Set(phone1, phone2),
        properties =
          Map(
            MetadataType.Of -> MetadataBuilder.MetadataTypeProperty(None, greedy = true)
          ),
        stored = Set.empty,
        expected = Set(MetadataMiner.Source(phone1, MetadataType.Of), MetadataMiner.Source(phone2, MetadataType.Of))
      ),
      MinerSourceTestCase(
        description = "with phone & greedy & stored",
        phones = Set(phone1),
        properties =
          Map(
            MetadataType.Of -> MetadataBuilder.MetadataTypeProperty(None, greedy = true)
          ),
        stored = Set(metadata1),
        expected = Set.empty
      )
    )

  "MetadataBuilderImpl.sourceForMetadataMiner" should {
    minerSourceTestCases.foreach { case MinerSourceTestCase(description, phones, properties, stored, expected) =>
      s"$description" in {
        MetadataBuilderImpl.sourceForMetadataMiner(phones, properties, stored) shouldBe expected
      }
    }
  }
}
