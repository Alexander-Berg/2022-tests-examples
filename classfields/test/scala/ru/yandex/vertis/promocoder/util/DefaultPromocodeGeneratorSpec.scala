package ru.yandex.vertis.promocoder.util

import ru.yandex.vertis.promocoder.WordSpecBase
import ru.yandex.vertis.promocoder.model.{PromocodeId, PromocodeParameters}

/** Specs on [[DefaultPromocodeGenerator]] invariants
  *
  * @author vbachinskiy
  */
class DefaultPromocodeGeneratorSpec extends WordSpecBase {

  val defaultCodes: Set[PromocodeId] = Set("code1", "code2")
  val sampleCodes: Set[PromocodeId] = Set("samplecode1", "samplecode2")
  val samplePrefix = "sample"
  val codesCountForSample = 10

  def newGenerator(charsGen: CharsGenerator): PromocodeGenerator =
    new DefaultPromocodeGenerator(charsGen)

  "Promocode generator" must {
    "not generate codes if passed count 0" in {
      val gen = newGenerator(CharsGenerator.Default)
      val params = PromocodeParameters(None, None, sampleCodes)
      val set = gen.generateCodes(params)

      set shouldBe sampleCodes
    }

    "consider prefix and size" in {
      val gen = newGenerator(CharsGenerator.Default)
      val params = PromocodeParameters(
        Some(codesCountForSample),
        Some(samplePrefix),
        Set.empty
      )
      val set = gen.generateCodes(params)

      set should have size codesCountForSample

      set.foreach(_ should startWith(samplePrefix))
    }

    "merge sample codes with generated" in {
      val gen = newGenerator(CharsGenerator.Default)
      val params = PromocodeParameters(
        Some(codesCountForSample),
        Some(samplePrefix),
        defaultCodes
      )
      val set = gen.generateCodes(params)
      val count = codesCountForSample + defaultCodes.size

      val (generated, copied) = set.partition(p => p.startsWith(samplePrefix))

      set should have size count
      generated should have size codesCountForSample
      copied shouldBe defaultCodes
    }

    "throw an exception when attempts are exhausted" in {
      val generator = new DefaultPromocodeGenerator(CharsGenerator.Default, 1, 1)

      val exception = intercept[RuntimeException] {
        generator.generateCodes(PromocodeParameters(Some(1), None, Set.empty))
      }
      assert(exception.getMessage.contains("Attempts was exhausted"))
    }

    "return only lowercase when accepted LowerCase chars gen" in {
      val gen = newGenerator(CharsGenerator.LowerCase)

      val params = PromocodeParameters(count = Some(10))
      val codes = gen.generateCodes(params)
      codes.flatMap(_.toCharArray).foreach { c =>
        c.isLetterOrDigit shouldBe true
        (c.isDigit || c.isLower) shouldBe true
      }
    }
  }
}
