package ru.yandex.verba.scheduler.tasks.export

import org.scalatest.Ignore
import org.scalatest.freespec.AnyFreeSpec
import ru.yandex.verba.core.application.DBInitializer
import ru.yandex.verba.core.manager.ServiceManager
import ru.yandex.verba.core.util.VerbaUtils

import java.io.File
import scala.concurrent.duration.Duration

/**
 * Author: Evgeny Vanslov (evans@yandex-team.ru)
 * Created: 15.10.14
 */
@Ignore
class ExportTest extends AnyFreeSpec with VerbaUtils {
  DBInitializer
  implicit val tout = Duration("1 hour")
  import ru.yandex.verba.core.application.system.dispatcher

  def export(service: String) = {
    {
      for {
        service <- ServiceManager.ref.getService(service)
        _ <- new ExportTask().doExport(service)
      } yield ()
    }.await
  }
  "test export" - {
    "realty" ignore {
      export("realty")
    }

    "auto" ignore {
      export("auto")
    }

    "write export" ignore {
      new ExportTask().makeFile(ServiceManager.ref.getService("auto").await, new File("auto.xml.gz")).await
    }
  }
}
