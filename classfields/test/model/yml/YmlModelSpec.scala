package ru.vertistraf.common.model.yml

import zio.test.Assertion._
import zio.test._

object YmlModelSpec extends DefaultRunnableSpec {

  private def validateCorrectName(name: String, prefix: String, index: Int) =
    testM(s"`$name` should be valid with prefix `$prefix` and index `$index`") {
      assertM {
        wrapFileName(name).map { wrapped =>
          (getYmlFileNamePrefix(wrapped), getYmlFileNameIndex(wrapped))
        }.run
      }(succeeds(equalTo((prefix, index))))
    }

  private def validateMalformedName(name: String) =
    testM(s"`$name` should be malformed") {
      assertM {
        wrapFileName(name).map { wrapped =>
          (getYmlFileNamePrefix(wrapped), getYmlFileNameIndex(wrapped))
        }.run
      }(fails(anything))
    }

  private def ymlFileNameSpec() = {
    val successTests =
      Seq(
        ("flats_0.yml", "flats", 0),
        ("site_tables_0.yml", "site_tables", 0),
        ("model_filters_hhhhhh_1.yml", "model_filters_hhhhhh", 1)
      ).map(specIn => validateCorrectName(specIn._1, specIn._2, specIn._3))

    val malformedTests =
      Seq(
        " flats_0.yml",
        "flats_0.yml ",
        "flats_0.yaml",
        "flats_0",
        "flats_d0.yml",
        "flats_0d.yml",
        "flats_.yml",
        "flats.yml",
        "/flats_0.yml"
      ).map(validateMalformedName)

    suite("YmlFileName type")(
      successTests ++ malformedTests: _*
    )
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("YmlModel")(
      ymlFileNameSpec()
    )
}
