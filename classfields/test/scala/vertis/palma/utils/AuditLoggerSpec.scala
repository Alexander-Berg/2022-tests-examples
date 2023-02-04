package vertis.palma.utils

import common.zio.logging.Logging
import vertis.palma.StaticDescriptorRepository
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.palma.service.descriptors.DescriptorRepository
import vertis.palma.service.model.RequestContext
import vertis.sraas.model.SchemaVersion
import vertis.zio.test.ZioSpecBase
import zio.ZIO

class AuditLoggerSpec extends ZioSpecBase {

  private val staticDescriptorRepository = {
    val descriptors = Seq(
      vertis.palma.test.test_samples.Mark.javaDescriptor,
      vertis.palma.test.test_samples.EncryptedMark.javaDescriptor
    )
    StaticDescriptorRepository(SchemaVersion(0, 0, 0), descriptors)
  }

  private val auditLogger = new AuditLogger {
    override protected val descriptorRepository: DescriptorRepository = staticDescriptorRepository
  }

  private def checkLoggingContext(expected: Map[String, Any]): ZIO[Logging.Logging, Throwable, Unit] =
    for {
      lCtx <- Logging.mdc.get(expected.keySet)
      _ <- check(lCtx should contain theSameElementsAs expected)
    } yield ()

  private def clearEnv: ZIO[Logging.Logging, Nothing, Unit] = Logging.mdc
    .delete(
      Set(
        AuditLogger.Markers.RequestId,
        AuditLogger.Markers.Encrypted,
        AuditLogger.Markers.EventTime,
        AuditLogger.Markers.Action,
        AuditLogger.Markers.DictionaryId,
        AuditLogger.Markers.ItemId,
        AuditLogger.Markers.SchemaVersion,
        AuditLogger.Markers.ServiceName,
        AuditLogger.Markers.UserId
      )
    )

  private def isolatedIoTest(body: => TestBody): Unit =
    ioTest(for {
      _ <- clearEnv
      res <- body
      _ <- clearEnv
    } yield res)

  "AuditLogger" should {
    "log all ctx" in isolatedIoTest {
      for {
        eventTime <- zio.clock.instant
        _ <- auditLogger.logEvent(
          "test/encrypted/auto/mark",
          Some("bmw"),
          RequestContext(Some("user"), Some("v1"), Some("palma-www"), Some("qwerty")),
          eventTime,
          AuditLogger.Action.Read
        )
        _ <- checkLoggingContext(
          Map(
            AuditLogger.Markers.RequestId -> "qwerty",
            AuditLogger.Markers.Encrypted -> true.toString,
            AuditLogger.Markers.EventTime -> eventTime.toString,
            AuditLogger.Markers.Action -> "Read",
            AuditLogger.Markers.DictionaryId -> "test/encrypted/auto/mark",
            AuditLogger.Markers.ItemId -> "bmw",
            AuditLogger.Markers.SchemaVersion -> "v1",
            AuditLogger.Markers.ServiceName -> "palma-www",
            AuditLogger.Markers.UserId -> "user"
          )
        )
      } yield ()
    }

    "not fail on empty fields" in isolatedIoTest {
      for {
        eventTime <- zio.clock.instant
        _ <- auditLogger.logEvent("test/auto/mark", None, RequestContext.Empty, eventTime, AuditLogger.Action.List)
        _ <- checkLoggingContext(
          Map(
            AuditLogger.Markers.RequestId -> "",
            AuditLogger.Markers.Encrypted -> false.toString,
            AuditLogger.Markers.EventTime -> eventTime.toString,
            AuditLogger.Markers.Action -> "List",
            AuditLogger.Markers.DictionaryId -> "test/auto/mark",
            AuditLogger.Markers.ItemId -> "",
            AuditLogger.Markers.SchemaVersion -> "",
            AuditLogger.Markers.ServiceName -> "",
            AuditLogger.Markers.UserId -> ""
          )
        )
      } yield ()
    }
  }
}
