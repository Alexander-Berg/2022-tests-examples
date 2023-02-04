package ru.yandex.vertis.clustering.dao

import java.time
import java.time.ZonedDateTime

import ru.yandex.inside.yt.kosher.impl.YtUtils
import ru.yandex.vertis.clustering.config.HttpYtClientConfig
import ru.yandex.vertis.clustering.dao.YtClient._
import ru.yandex.vertis.clustering.dao.YtTableData.Facts._
import ru.yandex.vertis.clustering.dao.impl.HttpYtClientImpl
import ru.yandex.vertis.clustering.model.{AutoruUser, Fact, Suid}

object TestHttpYtClientImpl extends App {

  val config = new HttpYtClientConfig() {
    override def proxyHost: String = "hahn.yt.yandex.net"

    override def token: String = ""

    override def heavyCommandsTimeout: time.Duration = time.Duration.ofSeconds(120)

    override def writeChunkSizeBytes: Int = 512 * 1024 * 1024

    override def retries: Int = 50
  }

  val fact = Fact(
    user = AutoruUser("213123"),
    feature = Suid("suid"),
    dateTime = ZonedDateTime.now()
  )
  /*
  val yt = YtUtils.http(proxy, token, retries + 1, heavyCommandsTimeout, writeChunkSize.b.toInt)
        new HttpYtClient(yt)
   */
  import config._

  val yt = YtUtils.http(proxyHost, token)
  val client = new HttpYtClientImpl(yt)

  val link = "//home/verticals/.tmp/dvarygin/last".ytPath
  val target = "//home/verticals/.tmp/dvarygin/user_clustering_facts_test".ytPath

  println("begin tx")
  val create = client.createTable(target, Attribute.Schema(FactsYtSchema))
  println(create)

  val serializer = factsYtDataSerializer(ZonedDateTime.now())

  var i = 0
  while (i < 10) {
    val facts = Iterable(fact.copy(feature = Suid(s"suid$i")))
    println(s"insert: ${client.appendToTable(target, facts)(serializer)}")
    Thread.sleep(5000L)
    i += 1
  }

//  val target = "//home/verticals/.tmp/dvarygin".ytPath
//  try {
//    val create = client.createTable("//home/verticals/.tmp/dvarygin/test_time2".ytPath, tx)
//    println(create)
//  } finally {
//    println(s"commit: ${client.commit(tx)}")
//  }
//  try {
//    val table = YtTable.fromFolderAndName("//home/verticals/.tmp/dvarygin", "user_clustering_facts_test")
//    println(s"create table: ${client.createTable(table, tx)}")
//    println(s"insert: ${client.insert(table, Iterable(fact), tx)}")
//  } finally {
//    println(s"commit: ${client.commit(tx)}")
//  }

}
