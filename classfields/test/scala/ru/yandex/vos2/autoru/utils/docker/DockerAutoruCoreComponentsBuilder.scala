package ru.yandex.vos2.autoru.utils.docker

import com.google.common.io.Closer
import ru.yandex.vertis.baker.util.logging.Logging
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.YdbContainer
import ru.yandex.vertis.ydb.skypper.Using.using
import ru.yandex.vertis.ydb.skypper.YdbWrapperImpl
import ru.yandex.vertis.ydb.skypper.tcl.DefaultTclService
import ru.yandex.vos2.autoru.utils.docker.SingleSqlTestcontainer.ContainerConfig
import ru.yandex.vos2.util.TimeWatcher

import java.io.Closeable
import java.sql.DriverManager
import java.util.concurrent.ForkJoinPool
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  * Created by andrey on 11/3/16.
  */
object DockerAutoruCoreComponentsBuilder extends Logging {
  val containerNameSales = "vos_tests_sales"
  val containerNameOffice = "vos_tests_office"
  val containerNameVos = "vos_tests_vos"

  val dbMapping = Map(
    containerNameSales -> List("all7", "sales_certification"),
    containerNameOffice -> List("office7", "all", "catalog7_yandex", "poi7", "users"),
    containerNameVos -> List("vos2_auto_shard1", "vos2_auto_shard2")
  )

  val dockerImageName = "percona:5.7.15"
  val mySqlPort = 3306

  private val closer = Closer.create()

  implicit protected val ec: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(ForkJoinPool.commonPool())

  def registerToClose(c: Closeable): Unit = {
    closer.register(c)
  }

  def registerCloseCallback(cb: () => Unit): Unit = {
    closer.register(new Closeable {
      override def close(): Unit = cb()
    })
  }

  def stopAll(): Unit = {
    //reaper.stopAll()
    closer.close()
  }

  private def readSqlFile(name: String) = {
    scala.io.Source
      .fromURL(getClass.getResource(name), "UTF-8")
      .getLines
      .filter(s => s.trim.nonEmpty && !s.trim.startsWith("--"))
      .mkString
      .split(";")
  }

  private def prepareDb(jdbcUrl: String, dbNames: List[String]): Unit = {
    val connection = DriverManager.getConnection(jdbcUrl, "root", "sqlsql")
    def executeSql(sql: String): Unit = {
      val ps = connection.prepareStatement(sql)
      try {
        ps.execute()
      } finally {
        ps.close()
      }
    }
    try {
      log.info(s"prepareDbs ${dbNames.mkString(", ")}")
      dbNames.map(dbName => s"create database if not exists `$dbName`").foreach(executeSql)
      val schemaFilename = "/autoruSchema.sql"
      log.info(s"Using schema from $schemaFilename")
      val autoruSchemaFile = readSqlFile(schemaFilename)
      executeSql("SET FOREIGN_KEY_CHECKS=0;")
      autoruSchemaFile
        .filter(sql => dbNames.exists(dbName => sql.contains(s"`$dbName`")))
        .foreach(sql => {
          //log.info(sql)
          executeSql(sql)
        })
      executeSql("SET FOREIGN_KEY_CHECKS=1;")
    } finally {
      connection.close()
    }
  }

  private def getSqlContainerConfig(dbName: String): ContainerConfig = {
    ContainerConfig(
      dockerImageName,
      mySqlPort,
      Map(
        "MYSQL_DATABASE" -> dbName,
        "MYSQL_USER" -> "vos",
        "MYSQL_PASSWORD" -> "sqlsql",
        "MYSQL_ROOT_PASSWORD" -> "sqlsql"
      ),
      Seq(
        "--character-set-server=utf8mb4",
        "--sql-mode=NO_ENGINE_SUBSTITUTION",
        "--datadir=/tmpfs"
      ),
      Map(
        "/tmpfs" -> "rw"
      )
    )
  }

  def readYdbSqlFile(name: String) = {
    using(scala.io.Source.fromURL(getClass.getResource(name))) { source =>
      source.getLines
        .filter(s => s.trim.nonEmpty && !s.trim.startsWith("--"))
        .mkString
        .split(";")
        .map(line => {
          "--!syntax_v1\n" +
            "PRAGMA TablePathPrefix = \"/local\";\n" + line
        })
    }
  }

  private def createAndStartYdbContainer(schemaFilename: String)(
      implicit ec: ExecutionContext
  ): (String, String, Int) = {
    implicit val trace = Traced.empty

    val container = YdbContainer.stable
    container.start()
    val YdbDatabase = "/local"
    val ydb = new YdbWrapperImpl(YdbDatabase, container.tableClient, new DefaultTclService, "")(ec)
    log.info(s"Using schema from $schemaFilename")
    val schemaFile = readYdbSqlFile(schemaFilename)
    ydb.rawExecute("ddl") { session =>
      schemaFile.foreach(sql => {
        session.executeSchemeQuery(sql).join()
      })
    }
    (YdbDatabase, container.container.getContainerIpAddress, container.container.getMappedPort(2135))
  }

  private def createAndStartContainer(containerName: String, dbNames: List[String]): String = {
    val sql = new SingleSqlTestcontainer(getSqlContainerConfig(dbNames.head))
    sql.start()

    registerCloseCallback(() => sql.close())

    val jdbcUrl = sql.jdbcUrlUseSSLFalse

    log.info(s"$containerName jdbc url = $jdbcUrl")

    prepareDb(jdbcUrl, dbNames)

    sql.jdbcUrl
  }

  //scalastyle:off method.length
  def createConfig: String = {
    val sqlTime = TimeWatcher.withNanos()
    val (database, host, port) = createAndStartYdbContainer("/ydb_schema_base.sql")
    val jdbcUrlSales = createAndStartContainer(containerNameSales, dbMapping(containerNameSales))
    val jdbcUrlOffice = createAndStartContainer(containerNameOffice, dbMapping(containerNameOffice))
    val jdbcUrlVos = createAndStartContainer(containerNameVos, dbMapping(containerNameVos))
    log.info(s"schema creation: ${sqlTime.toMillis} ms.")

    s"""vos2-autoru {
        |  ydb2 {
        |     endpoint = ""
        |     database = "$database"
        |     host = "$host"
        |     port = $port
        |     token = ""
        |     table-prefix = ""
        |  }
        |  mysql {
        |    driverClass = "com.mysql.cj.jdbc.Driver"
        |    shards = [
        |    {
        |      master.url = "$jdbcUrlVos/vos2_auto_shard1?useSSL=false"
        |      slave.url = "$jdbcUrlVos/vos2_auto_shard1?useSSL=false"
        |      username = "root"
        |      password = "sqlsql"
        |    }
        |    {
        |      master.url = "$jdbcUrlVos/vos2_auto_shard2?useSSL=false"
        |      slave.url = "$jdbcUrlVos/vos2_auto_shard2?useSSL=false"
        |      username = "root"
        |      password = "sqlsql"
        |    }
        |    ]
        |    username = "root"
        |    password = "sqlsql"
        |
        |    all7 {
        |      driverClass = "com.mysql.cj.jdbc.Driver"
        |      master.url = "$jdbcUrlSales/all7?useSSL=false"
        |      slave.url = "$jdbcUrlSales/all7?useSSL=false"
        |      username = "root"
        |      password = "sqlsql"
        |    }
        |    office7 {
        |      driverClass = "com.mysql.cj.jdbc.Driver"
        |      master.url = "$jdbcUrlOffice/office7?useSSL=false"
        |      slave.url = "$jdbcUrlOffice/office7?useSSL=false"
        |      username = "root"
        |      password = "sqlsql"
        |    }
        |    all{
        |      driverClass = "com.mysql.cj.jdbc.Driver"
        |      master.url = "$jdbcUrlOffice/all?useSSL=false"
        |      slave.url = "$jdbcUrlOffice/all?useSSL=false"
        |      username = "root"
        |      password = "sqlsql"
        |    }
        |    users{
        |      driverClass = "com.mysql.cj.jdbc.Driver"
        |      master.url = "$jdbcUrlOffice/users?useSSL=false"
        |      slave.url = "$jdbcUrlOffice/users?useSSL=false"
        |      username = "root"
        |      password = "sqlsql"
        |    }
        |    catalog7{
        |      driverClass = "com.mysql.cj.jdbc.Driver"
        |      master.url = "$jdbcUrlOffice/catalog7_yandex?useSSL=false"
        |      slave.url = "$jdbcUrlOffice/catalog7_yandex?useSSL=false"
        |      username = "root"
        |      password = "sqlsql"
        |    }
        |
        |    mirror {
        |      master.url = "$jdbcUrlVos/vos2_auto_mirror?useSSL=false"
        |      slave.url = "$jdbcUrlVos/vos2_auto_mirror?useSSL=false"
        |      username = "root"
        |      password = "sqlsql"
        |    }
        |  }
        |}""".stripMargin
  }
}
