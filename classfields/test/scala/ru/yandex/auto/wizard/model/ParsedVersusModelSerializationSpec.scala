package ru.yandex.auto.wizard.model

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner

import scala.util.Random

@RunWith(classOf[JUnitRunner])
class ParsedVersusModelSerializationSpec extends WordSpec with Matchers {

  private def nextString: String = Random.alphanumeric.take(10).mkString("")

  private def simpleParsedModel(withNameplate: Boolean) = {
    val np = Some(nextString).filter(_ => withNameplate)
    SimpleVersusParsedModel(
      nextString,
      nextString,
      np,
      np
    )
  }

  private def runSpec(pvm: VersusModels) = {
    val res = VersusModels.deserializeFromString(VersusModels.serializeToString(pvm))

    res shouldBe pvm
  }

  "ParsedVersusModel" should {

    "correctly work when comes parsed without nameplates" in runSpec(
      VersusModels(simpleParsedModel(withNameplate = false), simpleParsedModel(withNameplate = false))
    )

    "correctly work when comes parsed with nameplates" in runSpec(
      VersusModels(simpleParsedModel(withNameplate = true), simpleParsedModel(withNameplate = true))
    )

    "correctly work when comes parsed with single nameplates" in {
      runSpec(
        VersusModels(simpleParsedModel(withNameplate = false), simpleParsedModel(withNameplate = true))
      )

      runSpec(
        VersusModels(simpleParsedModel(withNameplate = true), simpleParsedModel(withNameplate = false))
      )
    }

  }
}
