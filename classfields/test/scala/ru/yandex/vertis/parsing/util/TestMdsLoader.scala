package ru.yandex.vertis.parsing.util

import java.io.InputStream
import java.net.URL

/**
  * Created by mcsim-gr on 13.10.17.
  */
object TestMdsLoader {

  def loadData(key: String): InputStream = {
    new URL(s"http://s3.mdst.yandex.net/auto/vos2/test-data/$key").openStream()
  }
}
