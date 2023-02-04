package common.graphql.directives.testkit

import caliban.RootResolver
import zio.{Has, ZIO}
import zio.clock.Clock
import zio.query.{TaskQuery, UQuery, ZQuery}
import zio.duration._

object TestApi {

  case class TestQueries(
      pureField: String,
      queryField: UQuery[List[String]],
      slowField: TaskQuery[Long],
      failingField: TaskQuery[Boolean],
      argsField: Int => UQuery[Option[Int]])

  def resolver = for {
    clock <- ZIO.service[Clock.Service]
    resolver = RootResolver(
      TestQueries(
        pureField = "Oh Hi Mark",
        queryField = ZQuery.fromEffect(
          ZIO.effectTotal("Hello" :: "Would" :: "You" :: "Like" :: "To" :: "Speak" :: "About" :: Nil)
        ),
        slowField = ZQuery.fromEffect {
          ZIO.sleep(1000.millis).provide(Has(clock)) *> ZIO(math.pow(2, 63).toLong)
        },
        failingField = ZQuery.fail(new RuntimeException("Something is broken")),
        argsField = arg => ZQuery.succeed(Some(arg).filter(_ % 2 == 0))
      )
    )
  } yield resolver
}
