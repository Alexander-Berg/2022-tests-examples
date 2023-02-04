package ru.yandex.vertis.parsing.clients.downloader

import java.io.File

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.clients.MockedHttpClientSupport
import ru.yandex.vertis.parsing.util.{FileUtils, TestUtils}

/**
  * Created by andrey on 1/9/18.
  */
@RunWith(classOf[JUnitRunner])
class DefaultUrlDownloaderTest extends FunSuite with MockedHttpClientSupport {
  private val urlDownloader = new DefaultUrlDownloader(http)

  test("download: zero file size") {
    http.expect("GET", "http://example.com/file.txt")
    http.respondWith(200, "")
    val file = new File("file.txt")
    try {
      http.respondWithFile(file)
      intercept[RuntimeException] {
        TestUtils.cause(urlDownloader.downloadFromUrl("http://example.com/file.txt").futureValue)
      }
    } finally {
      file.delete()
    }
  }

  test("download: nonempty file") {
    http.expect("GET", "http://example.com/file2.txt")
    http.respondWith(200, "")
    val file = new File("file2.txt")
    FileUtils.save(file.getAbsolutePath) { write =>
      write("test")
    }
    try {
      http.respondWithFile(file)
      val resFile = urlDownloader.downloadFromUrl("http://example.com/file2.txt").futureValue
      assert(resFile == DownloadedFile(file, ContentEncoding.None))
    } finally {
      file.delete()
    }
  }

  test("download: nonempty gzip file") {
    http.expect("GET", "http://example.com/file2.txt")
    http.respondWith(200, "")
    http.respondWithHeader("content-encoding", "gzip")
    val file = new File("file2.txt")
    FileUtils.save(file.getAbsolutePath) { write =>
      write("test")
    }
    try {
      http.respondWithFile(file)
      val resFile = urlDownloader.downloadFromUrl("http://example.com/file2.txt").futureValue
      assert(resFile == DownloadedFile(file, ContentEncoding.Gzip))
    } finally {
      file.delete()
    }
  }
}
