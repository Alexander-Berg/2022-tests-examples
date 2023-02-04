package common.db.datasource.fallback

import common.db.datasource.fallback.AutoPingDataSource.QualifiedDataSource
import common.db.datasource.fallback.AutoPingDataSourceSpec.{BadTestingDataSource, TestingDataSource}
import common.db.datasource.fallback.selector.{BalancedDataSourceSelector, DataSourceSelectorWithFallback}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.{Eventually, PatienceConfiguration}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

import java.io.PrintWriter
import java.sql._
import java.util.logging.Logger
import javax.sql.DataSource
import scala.concurrent.duration.DurationDouble

/** @author zvez
  */
class AutoPingDataSourceSpec
  extends AnyWordSpec
  with Matchers
  with Eventually
  with PatienceConfiguration
  with BeforeAndAfter {

  private val SleepPeriod = 100.millis

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(SleepPeriod.length, Millis))

  @volatile
  private var closer: () => Unit = _

  after {
    if (closer != null) closer.apply()
  }

  trait Test {
    val mainDs = new TestingDataSource("main")

    val goodFallbackDs = Seq(
      new TestingDataSource("good_fallback"),
      new TestingDataSource("other_good_fallback"),
      new TestingDataSource("third_good_fallback")
    )
    val badFallbackDs = new BadTestingDataSource("bad_fallback")

    def goodFallbackConnections: Seq[TestConnection] = goodFallbackDs.map(_.connection)

    lazy val datasource = {
      val main = QualifiedDataSource("main", mainDs)
      val candidates = main +:
        goodFallbackDs.map(QualifiedDataSource("fb", _)) :+
        QualifiedDataSource("fb", badFallbackDs, weight = 0)
      new AutoPingDataSource(
        "test",
        candidates,
        new DataSourceSelectorWithFallback(Seq(main), BalancedDataSourceSelector),
        pingPeriod = SleepPeriod / 2,
        failingPingPeriod = SleepPeriod / 4
      )
    }

    closer = () => datasource.close()
  }

  "DataSourceWithFallback" should {
    "return main connection if it is available" in new Test {
      for (_ <- 1 to 10) {
        datasource.getConnection shouldBe mainDs.connection
      }
    }

    "return fallback connection if main is not available" in new Test {
      datasource.getConnection shouldBe mainDs.connection
      mainDs.disable()
      eventually {
        goodFallbackConnections should contain(datasource.getConnection)
      }
    }

    "go back to main connection when it becomes available" in new Test {
      datasource.getConnection shouldBe mainDs.connection
      mainDs.disable()
      eventually {
        goodFallbackConnections should contain(datasource.getConnection)
      }
      mainDs.enable()
      eventually {
        datasource.getConnection shouldBe mainDs.connection
      }
    }

    "return main connection if nothing is available" in new Test {
      datasource.getConnection shouldBe mainDs.connection
      mainDs.disable()
      goodFallbackDs.foreach(_.disable())
      eventually {
        datasource.getConnection shouldBe mainDs.connection
      }
    }

    "distribute fallback according to weights" in new Test {
      datasource.getConnection shouldBe mainDs.connection
      mainDs.disable()
      eventually {
        datasource.getConnection should not be mainDs.connection
      }
      val distribution = (0 until 100)
        .map(_ => datasource.getConnection)
        .groupMapReduce(identity)(_ => 1)(_ + _)
      distribution.keys should contain theSameElementsAs goodFallbackConnections
    }

    "work with no fallback ds" in new Test {
      override val goodFallbackDs: Seq[TestingDataSource] = Nil

      datasource.getConnection shouldBe mainDs.connection
      mainDs.disable()
      eventually {
        datasource.getConnection shouldBe mainDs.connection
      }
    }
  }

}

object AutoPingDataSourceSpec {

  class BadTestingDataSource(name: String) extends TestingDataSource(name) {

    override def getConnection: Connection = throw new UnsupportedOperationException(
      "Can't get connection for a bad ds"
    )
  }

  class TestingDataSource(name: String) extends DataSource {

    val connection = new TestConnection(name)

    def disable(): Unit = connection.available.set(false)

    def enable(): Unit = connection.available.set(true)

    override def getConnection: Connection = connection

    override def getConnection(username: String, password: String): Connection = ???

    override def getLogWriter: PrintWriter = ???

    override def setLogWriter(out: PrintWriter): Unit = ???

    override def setLoginTimeout(seconds: Int): Unit = ???

    override def getLoginTimeout: Int = ???

    override def getParentLogger: Logger = ???

    override def unwrap[T](iface: Class[T]): T = ???

    override def isWrapperFor(iface: Class[_]): Boolean = ???
  }

}
