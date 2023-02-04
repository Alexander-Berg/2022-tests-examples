package ru.yandex.vertis.feedprocessor.autoru.scheduler.util.unificator

import java.io._
import java.net.URL
import java.util.Scanner

import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.nio.client.{CloseableHttpAsyncClient, HttpAsyncClients}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper.{CarsUnificator, CarsUnificatorWithAlternatives}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClientImpl
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.unificator.DataPreparer.InputUserData
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.unificator.UnificatorChecker.Arguments
import ru.yandex.vertis.feedprocessor.http.{ApacheHttpClient, DisableSSL, HttpClient, HttpClientConfig}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by sievmi on 20.04.18
  */

class DataPreparer(args: Arguments) {

  val userData: InputUserData = getUserInputData
  var httpClient: CloseableHttpAsyncClient = _
  var writer: CSVWriter = _

  def getParsedCatalog: ListBuffer[CatalogXmlIterator.CatalogData] = {
    new CatalogXmlIterator(createInputStream()).parserCatalog()
  }

  private def createInputStream(): InputStream = {
    if (userData.needDownload) {
      new URL(userData.inputPath).openStream()
    } else {
      new FileInputStream(userData.inputPath)
    }
  }

  def createCSVWriter(): CSVWriter = {
    writer = new CSVWriter(createOutputStream())
    writer
  }

  private def createOutputStream(): OutputStream = {
    if (userData.outputPath.endsWith("/")) {
      new FileOutputStream(userData.outputPath + "results.csv")
    } else {
      new FileOutputStream(userData.outputPath)
    }
  }

  def createCarsUnificator: CarsUnificator = {
    httpClient = HttpAsyncClients
      .custom()
      .setSSLContext(DisableSSL.context)
      .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
      .build()

    httpClient.start()
    val http: HttpClient with Closeable = new ApacheHttpClient(httpClient, config)
    val client = new UnificatorClientImpl(http)
    new CarsUnificatorWithAlternatives(client) with CarsUnificatorWithAlternatives.AutomaticTransmissionFix
  }

  def closeResources(): Unit = {
    httpClient.close()
    writer.finish()
  }

  def getUnificatorUrl: String = userData.unificatorHost

  def getOutputPath: String = userData.outputPath

  private def getUserInputData: InputUserData = {
    val consoleReader = new Scanner(System.in)

    println(s"Enter full path to catalog or url: [${DataPreparer.DefaultCatalogPath}]")
    val inputPath0 = args.catalogPath.getOrElse(consoleReader.nextLine().trim)
    val inputPath = if (inputPath0.isEmpty) DataPreparer.DefaultCatalogPath else inputPath0

    println("Enter unificator url: ")
    val unificatorUrl = args.unificatorUrl.getOrElse(consoleReader.nextLine().trim)

    println("Enter full path to output filepath: ")
    val outputPath = args.outPath.getOrElse(consoleReader.nextLine().trim)

    val unificatorUrlObject = if (unificatorUrl.startsWith("http")) {
      new URL(unificatorUrl)
    } else {
      new URL("http://" + unificatorUrl)
    }

    InputUserData(
      isValidUrl(inputPath),
      inputPath,
      outputPath,
      unificatorUrlObject.getHost,
      unificatorUrlObject.getPort
    )
  }

  private def isValidUrl(url: String): Boolean = {
    try {
      new URL(url).toURI
      true
    } catch {
      case _: Exception => false
    }
  }

  def config: HttpClientConfig =
    HttpClientConfig(userData.unificatorHost, userData.unificatorPort)
}

object DataPreparer {

  case class InputUserData(
      needDownload: Boolean,
      inputPath: String,
      outputPath: String,
      unificatorHost: String,
      unificatorPort: Int)

  val DefaultCatalogPath = "https://helpdesk.auto.ru/catalog/cars"
}
