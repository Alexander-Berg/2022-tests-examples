package ru.yandex.realty.takeout.processor.security.impl

import com.amazonaws.services.s3.model.PutObjectResult
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.application.ng.s3.ExtendedS3Client
import ru.yandex.realty.tracing.Traced

import java.io.File
import java.util.Date

@RunWith(classOf[JUnitRunner])
class SecurityTaskS3CsvExporterSpec extends SpecBase {

  "SecurityTaskS3CsvExporter" should {
    "exportCsvToS3" in new SecurityTaskS3CsvExporterFixture {
      val taskId = "123124_2"
      val fileName = taskId + ".csv"

      (s3Client
        .writeFile(_: String, _: File, _: Boolean, _: Option[String], _: Option[Date]))
        .expects(fileName, *, *, *, *)
        .returns(new PutObjectResult())

      exporter.exportCsvToS3(taskId, Seq("header"), Seq(Seq("r1", "r2")))(Traced.empty)
    }

    "fileAlreadyExists returns true " in new SecurityTaskS3CsvExporterFixture {
      (s3Client
        .objectExists(_: String))
        .expects("myFile.csv")
        .returns(true)

      val myFileExists = exporter.fileAlreadyExists("myFile")(Traced.empty)
      myFileExists shouldBe true
    }
    "fileAlreadyExists returns false " in new SecurityTaskS3CsvExporterFixture {
      (s3Client
        .objectExists(_: String))
        .expects("yourFile.csv")
        .returns(false)

      val yourFileExists = exporter.fileAlreadyExists("yourFile")(Traced.empty)
      yourFileExists shouldBe false
    }
  }

  trait SecurityTaskS3CsvExporterFixture {
    val s3Client = mock[ExtendedS3Client]
    val exporter = new SecurityTaskS3CsvExporter(s3Client)
  }
}
