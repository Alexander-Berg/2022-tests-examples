package ru.yandex.vertis.picture.client

import java.io.{File, FileInputStream}

import com.codahale.metrics.MetricRegistry
import com.typesafe.config.ConfigFactory
import org.asynchttpclient.AsyncHttpClient
import org.joda.time.DateTime
import org.scalatest._
import org.slf4j.LoggerFactory
import ru.yandex.common.monitoring.{CompoundHealthCheckRegistry, HealthChecks, Metrics}
import ru.yandex.vertis.ning.{FullTruncatedAccess, LoggedPipeline, MeteredPipeline, Pipeline, ThrottledPipeline}
import ru.yandex.vertis.picture.Mds.{GroupId, ImageOutput, Size}
import ru.yandex.vertis.picture.excepton.TooManyConcurrentConnections
import ru.yandex.vertis.picture.util.{AccessLogging, MdsMetricsTemplate}
import ru.yandex.vertis.picture.{AlreadyExists, HttpSettings, NotFound, PictureSettings, UrlContainsBadSymbols}
import spray.json.{DefaultJsonProtocol, JsValue, RootJsonReader}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}

/**
  * @author evans
  */
//@Ignore
// scalastyle:off
class MdsClientImplSpec
    extends FlatSpecLike
        with BeforeAndAfter
        with Matchers
        with Inspectors
        with BeforeAndAfterAll {

  implicit val ec = new ExecutionContext {
    override def reportFailure(cause: Throwable): Unit = {}

    override def execute(runnable: Runnable): Unit = runnable.run()
  }

  private val settings = PictureSettings(
    uploadUrl = "http://avatars-int.mdst.yandex.net:13000",
    getUrl = "http://avatars.mdst.yandex.net:80",
    //    uploadUrl = "http://localhost:13000",
    //    getUrl = "http://localhost:13001",
    namespace = "autoru-vos"
  )

  private val httpSettings = HttpSettings(ConfigFactory.parseString(
    """
      |connection-timeout = 200 ms
      |request-timeout = 20 s
      |num-retries = 0
      |max-connections = 5
    """.stripMargin))

  import Pipeline.createClient
  import settings._

  private val client: AsyncHttpClient = createClient(httpSettings)

  val mdsMetricsTemplate = new MdsMetricsTemplate {
    override def namespace: String = settings.namespace

    override def metricRegistry: MetricRegistry = Metrics.defaultRegistry()
  }

  trait DefaultPipelineMixin extends LoggedPipeline with MeteredPipeline {
    protected lazy val metricsTemplate = mdsMetricsTemplate
    override protected lazy val logging: FullTruncatedAccess = AccessLogging

    override protected def executionContext: ExecutionContext = ec
  }

  private def sendRecieve(client: AsyncHttpClient): Pipeline =
    new Pipeline(client) with DefaultPipelineMixin

  private def throttledSendReceive(client: AsyncHttpClient, l: Int) =
    new Pipeline(client) with DefaultPipelineMixin with ThrottledPipeline {
      override protected def limit: Int = l
    }

  def createMdsClient(pictureSettings: PictureSettings) =
    new MdsClientImpl(pictureSettings, sendRecieve(client)) with MonitoredMdsClient {
      override protected def executionContext: ExecutionContext = ec

      override def serviceName: String = "test"

      override def healthChecks: CompoundHealthCheckRegistry = HealthChecks.compoundRegistry()
    }

  val mdsClient: MdsClientImpl = createMdsClient(settings)

  val img = new File(getClass.getClassLoader.getResource("data/test.jpeg").getFile)

  def await[T](f: Future[T]): T = Await.result(f, 20.second)

  def nextName(): String = {
    val i = Math.abs(Random.nextLong())
    s"test-$i"
  }

  val log = LoggerFactory.getLogger(getClass)
  var groupId: String = _
  var name: String = _

  override def afterAll() = client.close()

  after(
    Try(delete()).failed.foreach(println)
  )

  def delete(name: String = name, groupId: String = groupId): Boolean = {
    await(mdsClient.delete(groupId, name))
  }

  "throttled mds client" should "handle too many connections" in {
    val httpClient = createClient(httpSettings.copy(maxConnections = 1))
    val client = new MdsClientImpl(settings, sendRecieve(httpClient))
        with MonitoredMdsClient {
      override protected def executionContext: ExecutionContext = ec

      override def serviceName: String = "test"

      override def healthChecks: CompoundHealthCheckRegistry = HealthChecks.compoundRegistry()
    }
    try {
      name = nextName()
      groupId = await(client.put(name, img)).groupId
      val results = (1 to 10) map {
        _ =>
          client.put(name, img)
      } map {
        o =>
          Try {await(o).groupId}
      }
      val groupIds: Set[String] = results.flatMap(_.toOption).toSet[String]
      groupIds.foreach(o => delete(groupId = o))
      results.forall({
        case Success(_) => true
        case Failure(AlreadyExists(_, _, _)) => true
        case Failure(TooManyConcurrentConnections(_)) => true
        case th =>
          log.error("Unexpected", th)
          false
      }) shouldEqual true
      results.exists({
        case Failure(TooManyConcurrentConnections(_)) => true
        case _ => false
      })
    } finally {
      httpClient.close()
    }
  }

  it should "throttle rps" in {
    val httpClient = createClient(httpSettings.copy(maxConnections = 1))
    val receive = throttledSendReceive(httpClient, 1)
    val client = new MdsClientImpl(settings, receive)
    try {
      name = nextName()
      groupId = await(client.put(name, img)).groupId
      val results = (1 to 10) map {
        _ =>
          client.put(name, img)
      } map {
        o =>
          Try {await(o).groupId}
      }
      val groupIds: Set[String] = results.flatMap(_.toOption).toSet[String]
      groupIds.foreach(o => delete(groupId = o))
      results.forall({
        case Success(_) => true
        case Failure(AlreadyExists(_, _, _)) => true
        case Failure(TooManyConcurrentConnections(_)) =>
          false
        case th =>
          log.error("Unexpected", th)
          false
      }) shouldEqual true
    } finally {
      httpClient.close()
    }
  }

  it should "not throttle" in {
    val httpClient: AsyncHttpClient = createClient(httpSettings.copy(maxConnections = 1))
    val client = new MdsClientImpl(settings, sendRecieve(httpClient))
        with MonitoredMdsClient {
      override protected def executionContext: ExecutionContext = ec

      override def serviceName: String = "test"

      override def healthChecks: CompoundHealthCheckRegistry = HealthChecks.compoundRegistry()
    }
    try {
      name = nextName()
      groupId = await(client.put(name, img)).groupId
      val results = (1 to 20) map {
        _ =>
          Try {await(client.put(name, img)).groupId}
      }
      val groupIds: Set[String] = results.flatMap(_.toOption).toSet[String]
      groupIds.foreach(o => delete(groupId = o))
      results.forall({
        case Success(_) => true
        case Failure(AlreadyExists(_, _, _)) => true
        case Failure(TooManyConcurrentConnections(_)) =>
          false
        case th =>
          log.error("Unexpected", th)
          false
      }) shouldEqual true
    } finally {
      httpClient.close()
    }
  }

  "mds client" should "upload image" in {
    name = nextName()
    groupId = await(mdsClient.put(name, img)).groupId
    println(groupId)
    println(s"$getUrl/get-$groupId/$name")
  }

  it should "upload image with ttl" in {
    name = nextName()
    val imgInfo = await(mdsClient.put(name, img, 3.days))
    groupId = imgInfo.groupId
    println(groupId)
    println(s"$getUrl/get-$groupId/$name")
    imgInfo.expiresAt should be (defined)
  }

  it should "fail to upload image" in {
    val name = nextName()
    intercept[NotFound] {
      await(mdsClient.put(name, "http://yandex.ru/nonexistentPicture.jpg")).groupId
    }
  }

  it should "upload img with weired url" in {
    name = nextName()
    groupId = await(mdsClient.put(name, "http://2rrealty.ru/sites/default/files/03/26/2017 - 17:08/cam9.jpg")).groupId
  }

  it should "fail with UrlContainsBadSymbols if url is invalid" in {
    name = nextName()
    an[UrlContainsBadSymbols] should be thrownBy {
      await(mdsClient.put(name, "http://шрус) Opel Meriva A - 223110 - 50a8882d-60a0-4ba8-b168-671a4c8f01c3-DSCN0471.JPG")).groupId
    }
  }

  it should "upload bytes" in {
    name = nextName()
    val bytes = new Array[Byte](img.length().toInt)
    val fis = new FileInputStream(img)
    fis.read(bytes)
    fis.close()
    groupId = await(mdsClient.put(name, bytes)).groupId
    println(groupId)
    println(s"$getUrl/get-$groupId/$name")
  }

  it should "fail to upload twice" in {
    name = nextName()
    val bytes = new Array[Byte](img.length().toInt)
    val fis = new FileInputStream(img)
    fis.read(bytes)
    fis.close()
    groupId = await(mdsClient.put(name, bytes)).groupId
    val imgUrl = s"$getUrl/get-$namespace/$groupId/$name/orig"

    val results = (1 to 20) map {
      i =>
        Try {await(mdsClient.put(name, imgUrl)).groupId}
    }
    val groupIds: Seq[String] = results.flatMap(_.toOption)
    groupIds.foreach(o => delete(groupId = o))
    val alreadyExists: Seq[GroupId] = results.collect({
      case Failure(AlreadyExists(msg, gid, _)) => gid
    })
    alreadyExists should contain only(groupId +: groupIds: _*)
  }

  it should "return expiration time when already exists" in {
    name = nextName()
    val bytes = new Array[Byte](img.length().toInt)
    val fis = new FileInputStream(img)
    fis.read(bytes)
    fis.close()

    val ttl = 123.hours
    val queryStart = System.currentTimeMillis()
    groupId = await(mdsClient.put(name, bytes, ttl)).groupId
    val collisions = List.fill(20) {
      Try(await(mdsClient.put(name, bytes, ttl)))
    }.flatMap {
      case Success(info) =>
        await(mdsClient.delete(info.groupId, name))
        Nil
      case Failure(e: AlreadyExists) => List(e)
      case Failure(_) => Nil
    }
    collisions shouldNot be (empty)
    forAll(collisions) { c =>
      c should matchPattern {
        case AlreadyExists(_, _, Some(expiresAt)) =>
      }
      val diff = c.expiresAt.get.getMillis - queryStart
      diff should be (ttl.toMillis +- 10.minutes.toMillis)
    }
  }

  it should "delete image" in {
    name = nextName()
    groupId = await(mdsClient.put(name, img)).groupId

    val f = delete()
    f shouldBe true
  }

  it should "get image" in {
    name = nextName()
    groupId = await(mdsClient.put(name, img)).groupId

    val imageOutput: ImageOutput = await(mdsClient.get(groupId, name))
    imageOutput.length shouldBe img.length()
  }

  it should "get image from put" in {
    // can't get image from put path for realty, use autoru-all instead
    val autoruSettings = settings.copy(namespace = "autoru-all")
    val autoruMdsClient = createMdsClient(autoruSettings)

    name = nextName()
    groupId = await(autoruMdsClient.put(name, img)).groupId

    val imageOutput = await(autoruMdsClient.getFromUploadUrl(groupId, name))
    imageOutput.length shouldBe img.length()
  }

  it should "get info image" in {
    name = nextName()
    groupId = await(mdsClient.put(name, img)).groupId
    implicit val sizeProtocol = new RootJsonReader[Size] with DefaultJsonProtocol {
      override def read(json: JsValue): Size = try {
        jsonFormat(Size,"x","y").read(json.asJsObject.fields("orig-size"))
      } catch {
        case e: Exception =>
          throw new IllegalArgumentException("failed to parse size")
      }
    }
    val imageInfo = await(mdsClient.getInfo(groupId, name))
    imageInfo shouldBe Size(259, 194)
  }

  it should "encode malformed URL addresses" in {
    Seq(
      "http://a.b.ru/hello" -> "http://a.b.ru/hello",
      "http://a.b.ru/д д/hello" -> "http://a.b.ru/%D0%B4%20%D0%B4/hello",
      "http://a.b.ru/д д/hello?key=д д" -> "http://a.b.ru/%D0%B4%20%D0%B4/hello?key=%D0%B4%20%D0%B4",
      "http://2rrealty.ru/sites/default/files/03/26/2017 - 17:08/cam9.jpg" -> "http://2rrealty.ru/sites/default/files/03/26/2017%20-%2017:08/cam9.jpg",
      "http://жкленинскиегорки.рф/sites/all/themes/gorki/promo/gorki-photo2.jpg" -> "http://xn--c1adaclackcdezbs4ak.xn--p1ai/sites/all/themes/gorki/promo/gorki-photo2.jpg"
    ).foreach { case (source, dest) =>
      val tryUri = MdsClientImpl.tryEncodeURLAddress(source)
      tryUri.isSuccess shouldEqual true
      tryUri.get.toString shouldEqual dest
    }
  }

  it should "put malformed URL" in {
    name = nextName()
    val malformedUrl = "https://imi-auto.ru/image/cache/catalog/citroen/bu/ апр 2016/img_20160402_132435_imi-100x100.jpg"
    await(mdsClient.put(name, malformedUrl))
  }

  it should "put URL with cyrillic domain" in {
    name = nextName()
    val malformedUrl = "http://жкленинскиегорки.рф/sites/all/themes/gorki/promo/gorki-photo2.jpg"
    await(mdsClient.put(name, malformedUrl))
  }
}

// scalastyle:on
