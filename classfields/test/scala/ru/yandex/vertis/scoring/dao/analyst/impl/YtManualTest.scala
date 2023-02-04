package ru.yandex.vertis.scoring.dao.analyst.impl

import ru.yandex.vertis.quality.test_utils.SpecBase
import ru.yandex.vertis.scoring.dao.config.YqlConfig
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import ru.yandex.vertis.scoring.dao.model.YtTable

class YtManualTest extends SpecBase {
  //use your own credentials
  private val config =
    YqlConfig(
      url = "jdbc:yql://yql.yandex.net:443?syntaxVersion=1",
      user = "",
      token = ""
    )

  "YtAnalystDao" ignore {
    "getDiff" in {
      val table = YtTable.fromFolderAndName("//home/verticals/.tmp/user-scoring", "grouped_2020-11-28_23:00:00")
      val result = execute(_.getDiff(table, 0, 10))
      println(result.await)
    }
    "groupByPuid" in {
      val sourceTable =
        YtTable.fromFolderAndName("//home/verticals/moderation/general/users_score", "2020-11-28_23:00:00")
      val resultTable = YtTable.fromFolderAndName("//home/verticals/.tmp/user-scoring", "grouped_2020-11-28_23:00:00")
      val result =
        execute { dao =>
          dao.groupByPuid(
            sourceTable = sourceTable,
            resultTable = resultTable,
            updateTime = java.time.Instant.now()
          )
        }
      result.await
    }
    "allTablesInFolder" in {
      val result = execute(_.allTablesInFolder("//home/verticals/moderation/general/users_score"))
      println(result.await)
    }
    "calculateDiff" in {
      val previousTable =
        YtTable.fromFolderAndName("//home/verticals/moderation/general/users_score", "2020-12-07_23:00:00")
      val lastTable =
        YtTable.fromFolderAndName("//home/verticals/moderation/general/users_score", "2020-12-08_11:00:00")
      val resultTable =
        YtTable.fromFolderAndName("//home/verticals/.tmp/user-scoring", "diff_2020-12-07_23:00:00_2020-12-08_11:00:00")
      val result =
        execute { dao =>
          dao.calculateDiff(
            previousTable = previousTable,
            lastTable = lastTable,
            resultTable = resultTable,
            updateTime = java.time.Instant.now()
          )
        }
      result.await
    }
    "dropTable" in {
      val table =
        YtTable.fromFolderAndName("//home/verticals/.tmp/user-scoring", "diff_2020-12-07_11:00:00_2020-12-07_23:00:00")
      execute(_.dropTable(table)).await
    }
  }

  private def execute[V](exec: YtAnalystDiffDao[F] => F[V]): F[V] =
    for {
      client <- YqlQueryExecutorImpl.initialize(config)
      dao = new YtAnalystDiffDao[F](client)
      result <- exec(dao)
    } yield result
}
