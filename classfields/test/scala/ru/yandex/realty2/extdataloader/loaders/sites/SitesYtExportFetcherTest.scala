package ru.yandex.realty2.extdataloader.loaders.sites

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 30.03.2018
  */
class SitesYtExportFetcherTest extends FlatSpec with Matchers {
  private def normalize(s: String) = s.replaceAll("[,;]+\\s*http", " http").replaceAll("\\s{2,}http", " http")
  "Normalizer" should "correct process urls" in {
    normalize("http://www.gskufa.ru/index.php?oid=238; http://letchikov.gskufa.ru") should be(
      "http://www.gskufa.ru/index.php?oid=238 http://letchikov.gskufa.ru"
    )
    normalize("http://www.gskufa.ru/index.php?oid=238,http://letchikov.gskufa.ru") should be(
      "http://www.gskufa.ru/index.php?oid=238 http://letchikov.gskufa.ru"
    )
    normalize("http://www.gskufa.ru/index.php?,oid=238; http://letchikov.gskufa.ru") should be(
      "http://www.gskufa.ru/index.php?,oid=238 http://letchikov.gskufa.ru"
    )
    normalize("http://www.gskufa.ru/index.php?oid=238  http://letchikov.gskufa.ru") should be(
      "http://www.gskufa.ru/index.php?oid=238 http://letchikov.gskufa.ru"
    )
    normalize("http://www.gskufa.ru/index.php?oid=238,  http://letchikov.gskufa.ru") should be(
      "http://www.gskufa.ru/index.php?oid=238 http://letchikov.gskufa.ru"
    )
    normalize("http://www.gskufa.ru/index.php?oid=238, http://letchikov.gskufa.ru") should be(
      "http://www.gskufa.ru/index.php?oid=238 http://letchikov.gskufa.ru"
    )
  }
}
