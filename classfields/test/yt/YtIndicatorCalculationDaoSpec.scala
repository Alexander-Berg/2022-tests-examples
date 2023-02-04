package auto.dealers.dealer_stats.storage.test.yt

import auto.dealers.dealer_stats.storage.dao.IndicatorCalculationDao
import auto.dealers.dealer_stats.storage.yt.YtIndicatorCalculationDao

import java.sql.Connection
import cats.effect.{Async, Blocker, Resource}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import common.db.config.DbConfig
import doobie.free.KleisliInterpreter
import doobie.util.transactor.{Strategy, Transactor}

import javax.sql.DataSource
import pureconfig.ConfigSource
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test.Assertion.isNonEmpty
import zio.test._
import zio._
import zio.interop.catz._
import zio.test.TestAspect.ignore

object YtTestTransactor {

  private def toHikariConfig(name: String, config: DbConfig): HikariConfig = {
    val res = new HikariConfig()
    res.setDriverClassName(config.driver)
    res.setJdbcUrl(config.url)
    res.setUsername(config.user)
    res.setPassword(config.password)
    res.setConnectionTimeout(config.pool.connectionTimeout.toMillis)
    res.setMaximumPoolSize(config.pool.maxSize)
    res.setMinimumIdle(config.pool.minSize)
    res.setIdleTimeout(config.pool.idleTimeout.toMillis)
    res.setMaxLifetime(config.pool.maxLifetime.toMillis)
    res.setPoolName(name)

    config.properties.foreach { case (key, value) =>
      res.addDataSourceProperty(key, value)
    }

    res
  }

  def fromDataSource(dataSource: DataSource): URIO[Blocking, Transactor[Task]] = {
    for {
      blocking <- ZIO.access[Blocking](_.get[Blocking.Service].blockingExecutor)
      blocker = Blocker.liftExecutionContext(blocking.asEC)

    } yield {

      val connect = (dataSource: DataSource) => {
        val acquire: Task[Connection] = ZIO.effect(dataSource.getConnection)
        def release(c: Connection): Task[Unit] = blocker.blockOn(Async[Task].delay {
          c.close()
        })
        Resource.make(acquire)(release)
      }
      val interp = KleisliInterpreter[Task](blocker).ConnectionInterpreter

      Transactor(dataSource, connect, interp, Strategy.default)
    }
  }

  def fromHikariConfig(cfg: HikariConfig) = ZManaged
    .makeEffect(new HikariDataSource(cfg))(_.close())
    .mapM { dataSource =>
      fromDataSource(dataSource)

    }

  def apply(name: String, config: DbConfig) = {
    val hikariCfg = toHikariConfig(name, config)
    fromHikariConfig(hikariCfg)
  }
}

object YtIndicatorCalculationDaoSpec extends DefaultRunnableSpec {

  val daoLayer = {

    val dbConfig =
      ConfigSource
        .resources("application.development.conf")
        .at("yt-db")
        .loadOrThrow[DbConfig]

    val ytTransactor = YtTestTransactor(
      "yt-db",
      dbConfig
    ).toLayer

    Blocking.live >>> ytTransactor >+> Clock.live >>> YtIndicatorCalculationDao.live

  }

  val testRevaluationsDataMapping = testM("map data from yt to model for revaluations") {

    val task = IndicatorCalculationDao
      .revaluations()
      .tap(el => ZIO.effectTotal(println(el)))
      .provideSomeLayer(daoLayer)

    assertM(task)(isNonEmpty)
  }

  val testFastSellsDataMapping = testM("map data from yt to model for fast sells") {

    val task = IndicatorCalculationDao.fastSellIndicators
      .tap(el => ZIO.effectTotal(println(el)))
      .provideSomeLayer(daoLayer)

    assertM(task)(isNonEmpty)
  }

  val toxicStockIndicators = testM("calculate toxic stock") {
    val indicators = IndicatorCalculationDao.toxicStocks

    assertM(indicators)(isNonEmpty).provideSomeLayer(daoLayer)
  }

  override def spec = suite("yt dao")(
    testRevaluationsDataMapping,
    testFastSellsDataMapping,
    toxicStockIndicators
  ) @@ ignore

}
