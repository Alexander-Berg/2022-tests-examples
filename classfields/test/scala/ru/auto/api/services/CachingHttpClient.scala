package ru.auto.api.services

import java.io.{ObjectInputStream, ObjectOutputStream}
import java.nio.file.{Files, Path, Paths, StandardOpenOption}

import akka.testkit.SocketUtil
import io.specto.hoverfly.junit.core.HoverflyConfig._
import io.specto.hoverfly.junit.core.{Hoverfly, HoverflyMode, SimulationSource}
import org.apache.http.HttpHost
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.GeneratorUtils

import scala.jdk.CollectionConverters._
import scala.collection.mutable

trait CachingHttpClient extends AnyFunSuite with BeforeAndAfterAll {
  import CachingHttpClient._

  private val simulationFolder: String = s"src/test/resources/integration/${getClass.getSimpleName}"
  private val mode: MockMode = defineMockMode

  private def withRetries[R](retries: Int)(a: => R): R = {
    try {
      a
    } catch {
      case e: Exception if retries <= 1 => throw e
      case _: Exception => withRetries(retries - 1)(a)
    }
  }

  protected def cachingProxy: Option[HttpHost] = mode.cachingProxy

  protected def defineMockMode: MockMode = withRetries(3) {
    System.getenv.asScala.get("MOCK_MODE") match {
      case Some("CAPTURE") => new CaptureMockMode(simulationFolder)
      case Some("SIMULATE") => new SimulateMockMode(simulationFolder)
      case Some("SIMULATE_SOFT") =>
        if (Files.exists(responsesPath(simulationFolder)) && Files.exists(dataPath(simulationFolder))) {
          new SimulateMockMode(simulationFolder)
        } else {
          new CaptureMockMode(simulationFolder)
        }
      case _ => new DefaultMockMode()
    }
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    mode.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    mode.afterAll()
  }

  implicit class RichGen[T](gen: Gen[T]) {
    def next: T = mode.next(gen)
  }
}

object CachingHttpClient {

  def generatePort(): Int = {
    SocketUtil.temporaryLocalPort(false)
  }

  def responsesPath(basePath: String): Path = Paths.get(s"$basePath/responses.json")
  def dataPath(basePath: String): Path = Paths.get(s"$basePath/generatedData")

  trait MockMode {
    def cachingProxy: Option[HttpHost]
    def next[T](gen: Gen[T]): T
    def beforeAll(): Unit
    def afterAll(): Unit
  }

  class DefaultMockMode extends MockMode {
    override def next[T](gen: Gen[T]): T = GeneratorUtils.RichGen(gen).next

    override def beforeAll(): Unit = ()

    override def afterAll(): Unit = ()

    override def cachingProxy: Option[HttpHost] = None
  }

  class CaptureMockMode(simulationFolder: String) extends MockMode {
    private val proxy: HttpHost = new HttpHost("localhost", generatePort())

    private val hoverfly: Hoverfly = new Hoverfly(
      localConfigs()
        .disableTlsVerification()
        .enableStatefulCapture()
        .proxyPort(proxy.getPort),
      HoverflyMode.CAPTURE
    )
    private val captureBuffer: mutable.Buffer[Any] = mutable.Buffer.empty

    private def captureNext(t: Any): Unit = synchronized {
      captureBuffer.append(t)
    }

    private def exportGeneratedData(path: Path): Unit = {
      val stream = Files.newOutputStream(path, StandardOpenOption.CREATE)
      val objectStream = new ObjectOutputStream(stream)
      objectStream.writeObject(captureBuffer.toList)
      stream.close()
    }

    override def cachingProxy: Option[HttpHost] = Some(proxy)

    override def next[T](gen: Gen[T]): T = {
      val next = GeneratorUtils.RichGen(gen).next
      captureNext(next)
      next
    }

    override def beforeAll(): Unit = {
      hoverfly.start()
    }

    override def afterAll(): Unit = {
      hoverfly.exportSimulation(responsesPath(simulationFolder))
      exportGeneratedData(dataPath(simulationFolder))
    }
  }

  class SimulateMockMode(simulationFolder: String) extends MockMode {
    private val proxy: HttpHost = new HttpHost("localhost", generatePort())

    private val hoverfly = new Hoverfly(
      localConfigs()
        .disableTlsVerification()
        .disableProxySystemProperties()
        .proxyPort(proxy.getPort)
        .adminPort(generatePort()),
      HoverflyMode.SIMULATE
    )
    hoverfly.start()

    private val dataIterator: Iterator[Any] = readData()

    override def cachingProxy: Option[HttpHost] = Some(proxy)

    override def next[T](gen: Gen[T]): T = dataIterator.next().asInstanceOf[T]

    def readData(): Iterator[Any] = {
      val stream = Files.newInputStream(dataPath(simulationFolder))
      val objectStream = new ObjectInputStream(stream)
      val iterator = objectStream.readObject().asInstanceOf[List[Any]].iterator
      stream.close()
      iterator
    }

    override def beforeAll(): Unit = {
      hoverfly.simulate(SimulationSource.file(responsesPath(simulationFolder)))
    }

    override def afterAll(): Unit = ()
  }
}
