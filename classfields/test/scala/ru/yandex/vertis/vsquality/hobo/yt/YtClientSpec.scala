package ru.yandex.vertis.vsquality.hobo.yt

import org.scalatest.Ignore
import ru.yandex.inside.yt.kosher.impl.{YtConfiguration, YtUtils}
import ru.yandex.vertis.vsquality.hobo.YtClient
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators
import ru.yandex.vertis.vsquality.hobo.util.SpecBase
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.YtClient.RichString
import ru.yandex.vertis.vsquality.hobo.YtTableData.YtUsers

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class YtClientSpec extends SpecBase {

  "YtClient" in {
    val yt = {
      val ytConf =
        YtConfiguration
          .builder()
          .withApiHost("hahn.yt.yandex.net")
          .withToken("")
          .withHeavyCommandsRetries(22)
          .withSimpleCommandsRetries(22)
          .build()

      YtUtils.http(ytConf)
    }

    val ytClient = new HttpYtClientImpl(yt)

    val target = "//home/verticals/.tmp/users_export".ytPath
    val users = CoreGenerators.UserGen.next(10)

    (for {
      _ <- ytClient.createTable(target, Seq(YtClient.Attribute.Schema(YtUsers.schema)))
      _ <- ytClient.appendToTable(target, users)
    } yield ()).futureValue
  }

}
