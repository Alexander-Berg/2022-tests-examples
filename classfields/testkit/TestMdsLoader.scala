package ru.auto.api.testkit

import java.io.InputStream
import java.net.URL

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-15.
  */

object TestMdsLoader {

  def loadData(key: String): InputStream = {
    new URL(s"http://s3.mds.yandex.net/auto/vos2/test-data/$key").openStream()
  }
}
