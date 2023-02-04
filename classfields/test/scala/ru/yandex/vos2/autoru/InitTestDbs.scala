package ru.yandex.vos2.autoru

import org.scalatest.{OptionValues, Suite}
import ru.yandex.vertis.baker.util.logging.Logging
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.moderation.proto.Model.{Diff, Opinion}
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2._
import ru.yandex.vos2.autoru.model.{AutoruOfferID, AutoruSale}
import ru.yandex.vos2.autoru.services.moderation.OpinionWriter
import ru.yandex.vos2.autoru.services.moderation.OpinionWriter.OpinionWrapper
import ru.yandex.vos2.autoru.utils.docker.DockerAutoruCoreComponents
import ru.yandex.vos2.autoru.utils.docker.DockerAutoruCoreComponentsBuilder.readYdbSqlFile
import ru.yandex.vos2.services.pica.PicaPicaClient.OfferId
import ru.yandex.vos2.util.{Protobuf, TimeWatcher}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._

/**
  * Created by andrey on 8/22/16.
  */
trait InitTestDbs extends Suite with OptionValues with Logging with TestOperationalSupport {

  protected lazy val components = DockerAutoruCoreComponents

  components.featureRegistry.updateFeature(components.featuresManager.WriteToYdb.name, true)
  components.featureRegistry.updateFeature(components.featuresManager.ReadMotoOffersFromYdb.name, true)
  components.featureRegistry.updateFeature(components.featuresManager.ReadCarOffersFromYdb.name, true)
  components.featureRegistry.updateFeature(components.featuresManager.ReadTruckOffersFromYdb.name, true)

  private def readLines(name: String) = {
    scala.io.Source.fromURL(getClass.getResource(name), "UTF-8").getLines
  }

  protected def migrateOfferToVos(offerId: OfferId, category: String): Unit = {
    val autoruOffer = AutoruOfferID.parse(offerId)
    val readOffer = components.getOfferDao().findById(offerId)(Traced.empty).value
    val offerModel = {
      category match {
        case "cars" =>
          components.carOfferConverter
            .convertStrict(
              components.autoruSalesDao
                .getOfferForMigration(autoruOffer.id)
                .value,
              Some(readOffer)
            )

        case "trucks" =>
          components.truckOfferConverter
            .convertStrict(
              components.autoruTrucksDao
                .getOfferForMigration(autoruOffer.id)
                .value,
              Some(readOffer)
            )

        case "moto" =>
          components.motoOfferConverter
            .convertStrict(
              components.autoruMotoDao
                .getOfferForMigration(autoruOffer.id)
                .value,
              Some(readOffer)
            )

      }
    }
    components
      .getOfferDao()
      .saveMigrated(
        Seq(
          offerModel.converted.value
        )
      )(Traced.empty)
  }

  protected def banOffer(id: String): Unit = {
    val failedOpinion = Opinion.newBuilder().setType(Opinion.Type.FAILED).setVersion(1).build()
    val opinionsMap = Map(id -> Seq(OpinionWrapper(failedOpinion, Seq.empty, Diff.Autoru.getDefaultInstance, getNow)))
    new OpinionWriter(components, prometheusRegistry, components.protectedResellerDecider, components.banStrategy)
      .writeOpinions(opinionsMap)
  }

  private def readSqlFile(name: String) = {
    readLines(name)
      .filter(s => s.trim.nonEmpty && !s.trim.startsWith("--"))
      .map(s => s.split("--").head)
      .mkString("\n")
      .split(";")
  }

  private lazy val dbInstancesMapping = Map(
    "all7" -> components.oldSalesDatabase,
    "sales_certification" -> components.oldSalesDatabase,
    "all" -> components.oldAllDatabase,
    "catalog7_yandex" -> components.oldCatalogDatabase,
    "office7" -> components.oldOfficeDatabase,
    "poi7" -> components.oldOfficeDatabase,
    "users" -> components.oldUsersDatabase,
    "vos2_auto_shard1" -> components.mySql.shards.head,
    "vos2_auto_shard2" -> components.mySql.shards.head
  )

  private def initYdb() = {
    val schemaFile = readYdbSqlFile("/ydb_schema_base.sql")
    components.skypper.rawExecute("ddl") { session =>
      schemaFile.foreach(sql => {
        session.executeSchemeQuery(sql).join()
      })
    }(Traced.empty)
  }

  private def truncateSchema(): Unit = {
    val sqlTime = TimeWatcher.withNanos()
    val schemaFilename = "/autoruSchemaTruncate.sql"
    log.info(s"Using schema from $schemaFilename")
    val autoruSchemaFile = readSqlFile(schemaFilename)
    autoruSchemaFile
      .filter(_.trim.nonEmpty)
      .groupBy(sql => {
        sql.trim.replace("truncate table ", "").split("\\.", 2).head
      })
      .foreach {
        case (dbName, requests) =>
          //log.info(s"truncating db $dbName")
          val jdbc = dbInstancesMapping(dbName).master.jdbc
          jdbc.update("SET FOREIGN_KEY_CHECKS=0")
          requests.foreach(sql => {
            //log.info(sql)
            jdbc.update(sql)
          })
          jdbc.update("SET FOREIGN_KEY_CHECKS=1")
      }
    log.info(s"schema truncation: ${sqlTime.toMillis} ms.")
  }

  private def parseSqlArgs(str: String): Array[AnyRef] = {
    val res = collection.mutable.ArrayBuffer[AnyRef]()
    val buf = new StringBuilder
    var quote = false
    var isString: Boolean = false
    def addArg(): Unit = {
      val arg = buf.toString
      if (isString) {
        isString = false
        res += arg
      } else if (arg.equalsIgnoreCase("null")) {
        res += null
      } else if (arg.equalsIgnoreCase("true") || arg.equalsIgnoreCase("false")) {
        res += new java.lang.Boolean(arg)
      } else {
        res += (if (arg.contains(".")) Double.box(arg.toDouble) else Long.box(arg.toLong))
      }
      buf.clear()
    }
    str.foreach(c => {
      if (c == '\'') {
        quote = !quote
        if (quote) isString = true
      } else {
        if (c == ',') {
          if (!quote) {
            addArg()
          } else buf.append(c)
        } else if (c == ' ') {
          if (buf.nonEmpty) {
            buf.append(c)
          }
        } else {
          buf.append(c)
        }
      }
      //println(buf.toString)
    })
    addArg()
    res.toArray
  }

  private def batchInserts(): Unit = {
    val autoruOfferInsertsFile = readLines("/autoruOfferBatchInserts.sql")
    var batchMode: Boolean = true
    var sql: String = ""
    val args = ListBuffer[Array[AnyRef]]()
    // sql, args
    val parsedResult = ListBuffer[(String, List[Array[AnyRef]])]()
    autoruOfferInsertsFile.foreach(line => {
      if (batchMode) {
        if (line == "--") {
          parsedResult += (sql -> args.toList)
          sql = ""
          args.clear()
        } else if (line.startsWith("INSERT INTO ")) {
          sql = line
        } else if (line == "-- no batch") {
          batchMode = false
        } else {
          args += parseSqlArgs(line)
        }
      } else {
        parsedResult += ((line, Nil))
      }
    })
    parsedResult.groupBy(x => x._1.trim.replace("INSERT INTO ", "").split("\\.", 2).head).foreach {
      case (dbName, requests) =>
        //log.info(s"inserting test data to $dbName")
        requests.foreach {
          case (requestSql, requestArgs) =>
            val jdbc = dbInstancesMapping(dbName).master.jdbc
            jdbc.update("SET FOREIGN_KEY_CHECKS=0")
            val sqlTime = TimeWatcher.withNanos()
            //println(s"[${args.length} rows]: $sql")
            if (requestArgs.isEmpty) jdbc.update(requestSql)
            else jdbc.batchUpdate(requestSql, requestArgs)
            if (sqlTime.duration > 500.millis) {
              log.warn(s"Long query: [$requestSql], ${sqlTime.toMillis} ms.")
            }
            jdbc.update("SET FOREIGN_KEY_CHECKS=1")

        }
    }
  }

  def initOldSalesDbs(withData: Boolean = true): Unit = {
    components.oldSalesDatabase.toString //init
    val time = TimeWatcher.withNanos()
    log.info("initOldSalesDbs started")
    truncateSchema()
    if (withData) {
      batchInserts()
    }
    //simpleInserts()
    log.info(s"initOldSalesDbs done in ${time.toMillis} ms.")
  }

  private def dropDb(name: String): String = {
    s"drop database if exists $name"
  }

  def initNewOffersDbs(): Unit = {
    val time = TimeWatcher.withNanos()
    log.info("initNewOffersDbs started")
    val newAutoruSchema = readSqlFile("/schema_base.sql")
    components.mySql.shards.map(shard => (shard.master, shard.index + 1)).foreach {
      case (database, index) =>
        // TODO дропать не базы, а таблицы
        val sql1 = dropDb(s"vos2_auto_shard$index")
        database.jdbc.update(sql1)
        val sql2 = s"create database if not exists vos2_auto_shard$index"
        database.jdbc.update(sql2)

        val sql3 = s"use vos2_auto_shard$index"
        database.jdbc.update(sql3)
        newAutoruSchema.foreach(sql => {
          //log.info(s"${database.url} $sql")
          database.jdbc.update(sql)
        })
    }
    log.info(s"initNewOffersDbs done in ${time.toMillis} ms.")
  }

  def initDbs(withData: Boolean = true): Unit = {
    initOldSalesDbs(withData)
    initNewOffersDbs()
    initYdb()
  }

  //scalastyle:off line.size.limit
  val saleIds = List(
    1043270830L, // 1043270830-6b56a  a_18318774 объявление с турбо-пакетом и стикерами,                      t_offers_users шард 0, t_offers шард 0
    1043140898L, // 1043140898-81de   ac_10600   ???,                                                         t_offers_users шард 1, t_offers шард 0
    1042409964L, // 1042409964-038a   ac_10086   объявление от имени автосалона,                              t_offers_users шард 0, t_offers шард 0
    1043045004L, // 1043045004-977b3  a_10591660 объявление с сертификатом, без сервисов, от частного лица,   t_offers_users шард 0, t_offers шард 0
    1037186558L, // 1037186558-e06d8  ac_21029   объявление от автосалона с непустым хешом,                   t_offers_users шард 1, t_offers шард 0
    1043026846L, // 1043026846-83484c ac_8514    объявление с активным ютуб-видео и панорамой,                t_offers_users шард 0, t_offers шард 1
    1043211458L, // 1043211458-fbbd39 ac_15936   объявление с активным яндекс-видео,                          t_offers_users шард 1, t_offers шард 0
    1044216699L, // 1044216699-0f1a0  a_18740415 объявление с sales_phones_redirect = 1 и 1044216699 % 8 = 3, t_offers_users шард 0, t_offers шард 0
    1044214673L, // 1044214673-960f   a_18605844 объявление с sales_phones_redirect = 0 и 1044214673 % 8 = 1, t_offers_users шард 0, t_offers шард 1
    1044159039L // 1044159039-33be8  ac_10086   удаленное объявление пользвователя ac_10086, для которого у нас также есть неудаленное объявление 1044159039
  )

  val offersTruckId = List(
    15709759L // 15709759-dcd18741 a_25438808 trucks
  )

  lazy val getSalesFromDb: List[AutoruSale] = {
    saleIds.map(saleId => getSaleByIdFromDb(saleId))
  }

  def getSaleByIdFromDb(saleId: Long): AutoruSale = components.autoruSalesDao.getOffer(saleId)(Traced.empty).value

  private def getOfferByIdFromJson(saleId: Long): Offer = {
    val json = readLines(s"/offerJsons/$saleId.json").mkString
    Protobuf.fromJson[Offer](json)
  }

  lazy val getOffersFromJsonMap: Map[Long, Offer] = saleIds
    .map(saleId => {
      (saleId, getOfferByIdFromJson(saleId))
    })
    .toMap

  lazy val getOffersFromJson: List[Offer] = saleIds.map(saleId => {
    getOfferByIdFromJson(saleId)
  })

  lazy val getOffersTruckFromJsonMap: Map[Long, Offer] = offersTruckId
    .map(offerId => {
      (offerId, getOfferByIdFromJson(offerId))
    })
    .toMap

  def getOfferById(saleId: Long): Offer = {
    getOffersFromJsonMap(saleId)
  }

  def getOfferTruckById(id: Long): Offer = {
    getOffersTruckFromJsonMap(id)
  }
}
