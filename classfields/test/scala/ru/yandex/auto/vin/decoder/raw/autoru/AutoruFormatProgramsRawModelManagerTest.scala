package ru.yandex.auto.vin.decoder.raw.autoru

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Program, VinInfoHistory}
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoru.programs.AutoruFormatProgramsRawModelManager
import ru.yandex.auto.vin.decoder.raw.autoru.programs.model.AutoruFormatProgramRawModel
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.{DateFieldError, RequiredFieldError}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.protobuf.ProtobufConverterOps._
import ru.yandex.auto.vin.decoder.yt.diff.DbActions.Delete

class AutoruFormatProgramsRawModelManagerTest extends AnyFunSuite {

  private val jsonManager = new AutoruFormatProgramsRawModelManager(EventType.UNDEFINED, FileFormats.Json)

  private val xmlManager = new AutoruFormatProgramsRawModelManager(EventType.UNDEFINED, FileFormats.Xml)

  private val csvManager = new AutoruFormatProgramsRawModelManager(EventType.UNDEFINED, FileFormats.Csv)

  val successParseTestInput = Seq(
    ("json", "/autoru/programs/programs.json", jsonManager),
    ("xml", "/autoru/programs/programs.xml", xmlManager),
    ("csv", "/autoru/programs/programs.csv", csvManager)
  )

  for (in <- successParseTestInput) yield {
    test(s"parse ${in._1}") {
      val rawInputStream = getClass.getResourceAsStream(in._2)

      val parsed = in._3.parseFile(rawInputStream, "").toList.collect { case Right(v) =>
        v
      }

      val converted = parsed.map(in._3.convert).map(_.await)

      assert(parsed.size === 3)
      assert(converted === ParsedProgramsList)
    }
  }

  val failedParseTestInput = Seq(
    ("json", "/autoru/programs/errors.json", jsonManager),
    ("xml", "/autoru/programs/errors.xml", xmlManager),
    ("csv", "/autoru/programs/errors.csv", csvManager)
  )

  for (in <- failedParseTestInput) yield {
    test(s"parse ${in._1} with errors") {
      val rawInputStream = getClass.getResourceAsStream(in._2)

      val parsed = in._3.parseFile(rawInputStream, "").toList

      val errors = parsed.collect { case Left(v) =>
        v
      }

      assert(errors.size == 4)
      assert(errors(0).asInstanceOf[RequiredFieldError].field === "PROGRAM_ACTIVE")
      assert(errors(1).asInstanceOf[RequiredFieldError].field === "PROGRAM_ID")
      assert(errors(2).asInstanceOf[RequiredFieldError].field === "EVENT_DATE")
      assert(errors(3).asInstanceOf[DateFieldError].field === "PROGRAM_START")
    }
  }

  test("build deleted") {
    val vin = VinCode("X4X3D59440J496801")
    val rawInputStream = getClass.getResourceAsStream("/autoru/programs/delete.json")

    val parsed = jsonManager.parseFile(rawInputStream, "").toList.collect { case Right(v) =>
      v
    }

    val nonDeleted = parsed(0)
    val deleted = parsed(1)

    val generated: AutoruFormatProgramRawModel =
      jsonManager.buildDeleted(Delete(vin.toString, "group", 123L, 123L, 123L, nonDeleted.raw))

    assert(generated.raw === deleted.raw)
    assert(generated.model === deleted.model)
    assert(generated.identifier === deleted.identifier)
  }

  private val ParsedProgram1 = {
    val program = Program
      .newBuilder()
      .setProgramId("bmw_service_inclusive")
      .setIsActive(true.toBoolValue)
      .setDate(1587340800000L)
      .setProgramStartTimestamp(1589846400000L)
      .build()

    VinInfoHistory
      .newBuilder()
      .addPrograms(program)
      .setVin("X4X3D59440J496801")
      .setGroupId("1")
      .setEventType(EventType.UNDEFINED)
      .setStatus(VinInfoHistory.Status.OK)
      .build()
  }

  private val ParsedProgram2 = {
    val program = Program
      .newBuilder()
      .setProgramId("bentley_certified")
      .setIsActive(false.toBoolValue)
      .setDate(1587513600000L)
      .setProgramStartTimestamp(1589846400000L)
      .setProgramFinishTimestamp(1621382400000L)
      .build()

    VinInfoHistory
      .newBuilder()
      .addPrograms(program)
      .setVin("X4X3D59440J496802")
      .setGroupId("2")
      .setEventType(EventType.UNDEFINED)
      .setStatus(VinInfoHistory.Status.OK)
      .build()
  }

  private val ParsedProgram3 = {
    val program = Program
      .newBuilder()
      .setProgramId("porche_approved")
      .setIsActive(true.toBoolValue)
      .setDate(1610236800000L)
      .build()

    VinInfoHistory
      .newBuilder()
      .addPrograms(program)
      .setVin("X4X3D59440J496803")
      .setGroupId("3")
      .setEventType(EventType.UNDEFINED)
      .setStatus(VinInfoHistory.Status.OK)
      .build()
  }

  private val ParsedProgramsList = Seq(ParsedProgram1, ParsedProgram2, ParsedProgram3)
}
