package ru.yandex.vos2.autoru.utils

import java.io.{File, FileInputStream, InputStream}
import java.net.URL

import org.apache.commons.io.FileUtils

import scala.concurrent.duration._

/**
  * Created by mcsim-gr on 13.10.17.
  */
object TestMdsLoader {

  def loadData(key: String): InputStream = {
    val cacheFile = new File(".cache", key)
    if (!cacheFile.exists() || (System.currentTimeMillis() - cacheFile.lastModified() > 1.hour.toMillis)) {
      val url = new URL(s"https://s3.mds.yandex.net/testing-auto/vos2/test-data/$key")
      FileUtils.copyURLToFile(url, cacheFile)
    }
    new FileInputStream(cacheFile)
  }
}
