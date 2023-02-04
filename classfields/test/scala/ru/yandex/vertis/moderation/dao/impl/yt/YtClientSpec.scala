package ru.yandex.vertis.moderation.dao.impl.yt

import org.junit.Ignore
import ru.yandex.inside.yt.kosher.impl.YtUtils
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.client.YtClient.Attributes
import ru.yandex.vertis.moderation.client.impl.http.HttpYtClient
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.util.yt.ValueNode

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author potseluev
  */
@Ignore("For manually run")
class YtClientSpec extends SpecBase {

  private lazy val ytClient =
    new HttpYtClient(
      YtUtils.http(
        "hahn.yt.yandex.net",
        "<your-token-here>"
      )
    )

  "YtClient" should {
    "get node and attributes by path correctly" in {
      val path = "//home/vertis-moderation"
      val node = ytClient.get(path, Attributes.CreateTime).futureValue
      val realtyNodeCreateTime = node / "testing" / "REALTY" / Attributes.CreateTime
      val createTime =
        realtyNodeCreateTime
          .as[ValueNode[String]]
          .map(DateTimeUtil.parse)
          .map(_.get)
          .value

      createTime shouldBe DateTimeUtil.parse("2018-06-28T16:02:28.410686Z").get
    }
  }

}
