package vertis.yql.container

import vertis.zio.test.ZioSpecBase
import doobie.implicits._
import doobie.Fragment._
import ru.yandex.inside.yt.kosher.cypress.YPath
import vertis.yt.model.YPaths._
import zio.duration.{durationInt, Duration}

/** Tests of yql container */
class TestYqlServerIntSpec extends ZioSpecBase with TestYqlServer {

  // yql with python is slow
  override val ioTestTimeout: Duration = 5.minutes

  "TestYqlServer" should {
    "support simple queries" in ioTest {
      val query = sql"SELECT 123"
        .query[Int]
        .unique
      makeYqlClient.use { client =>
        client
          .executeQuery(query)
          .flatMap { result =>
            check {
              result shouldBe 123
            }
          }
      }
    }

    "support yt tables" in ioTest {
      val tablePath = YPath.simple("//tmp/some_test")
      val pathFrag = sql"`${const(tablePath.toYql)}`"
      val insert = sql"INSERT INTO $pathFrag WITH TRUNCATE SELECT 1, 'something new'".update.run
      val select = sql"SELECT * FROM $pathFrag".query[(Int, String)].unique
      makeYqlClient.use { client =>
        client.executeQuery(insert.flatMap(_ => select)).flatMap { result =>
          check {
            result shouldBe ((1, "something new"))
          }
        }
      }
    }

    "support udfs" in ioTest {
      val query = sql"select cast(Digest::CityHash('123') as String)".query[String].unique
      makeYqlClient.use { client =>
        client.executeQuery(query).flatMap { result =>
          check {
            result shouldBe "11844464045149276331"
          }
        }
      }
    }

    "support python udfs" in ioTest {

      val query =
        sql"""|
              |$$script = @@
              |
              |def double(x):
              |    return x * 2
              |
              |@@;
              |
              |$$double = Python3::double(Callable<(Optional<Int32>)->Int32>, $$script);
              |
              |select $$double(21)
              |
         """.stripMargin.query[Int].unique

      makeYqlClient.use { client =>
        client.executeQuery(query).flatMap { result =>
          check {
            result shouldBe 42
          }
        }
      }

    }
  }

}
