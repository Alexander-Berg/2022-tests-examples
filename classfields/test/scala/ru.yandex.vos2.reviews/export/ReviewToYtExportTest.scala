package ru.yandex.vos2.reviews.export

import ru.auto.api.reviews.ReviewModel.Review
import ru.yandex.inside.yt.kosher.cypress.{CypressNodeType, YPath}
import ru.yandex.inside.yt.kosher.impl.YtUtils
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.bolts.collection.Cf
import ru.yandex.inside.yt.kosher.ytree.YTreeNode
import ru.yandex.yt.ytclient.tables.{ColumnValueType, TableSchema}
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.dao.{MySql, MySqlConfig}
import ru.yandex.vos2.prometheus.Operational
import ru.yandex.vos2.reviews.dao.navigation.ReviewsMySqlNavigation
import ru.yandex.vos2.reviews.dao.offer.DefaultReviewsDao
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.vertis.scheduler.model.Schedule.EveryMinutes
import ru.yandex.vos2.features.SimpleFeatures
import ru.yandex.vos2.reviews.BaseReviewsTest
import ru.yandex.vos2.reviews.export.writer.YtExportWriter
import ru.yandex.vos2.reviews.features.ReviewsFeatures

import scala.collection.JavaConverters._
import ru.yandex.vos2.reviews.utils.YtClientUtils._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 06/12/2018.
  */
class ReviewToYtExportTest extends BaseReviewsTest with MockitoSupport {

  val configStr: String =
    s"""{
       |vos2 {
       |  default-revisit = 1 minute
       |  data.path = "./"
       |}
       |
       |vos2.reviews.mysql.vos {
       |shards = [
       |       {
       |          master.url = "jdbc:mysql://mysql.dev.vertis.yandex.net:3414/vos2_reviews"
       |          slave.url = "jdbc:mysql://mysql.dev.vertis.yandex.net:3414/vos2_reviews"
       |          username = "auto"
       |          password = "KiX1euph"
       |        }
       |        ]
       |}}""".stripMargin


  val config: Config = ConfigFactory.parseString(configStr)
  val mysqlConfig = new MySqlConfig(config.getConfig("vos2.reviews.mysql.vos"))
  val mysql = new MySql(mysqlConfig) with ReviewsMySqlNavigation
  val metrics: MetricsSupport = Operational.default("test", "test", "test", 1000)("test")
  val prometheus: PrometheusRegistry = metrics.prometheusRegistry

  val dao = new DefaultReviewsDao(mysql, config, metrics)


  ignore("writer test") {
    val features = new SimpleFeatures with ReviewsFeatures

    features.YtExport.setNewState(true)

    val proxy = "hahn.yt.yandex.net"
    val token = "AQAD-qJSJsSUAAAOyi9SiZnqb0PDtGF7s-KmRd8"

    val yt: Yt = YtUtils.http(proxy, token)

    val task = new YtExportWriter(yt, dao, "test", "//home/verticals/_dev", features, EveryMinutes(10))

    task.run()
  }

  ignore("create table") {
    val proxy = "hahn.yt.yandex.net"
    val token = "AQAD-qJSJsSUAAAOyi9SiZnqb0PDtGF7s-KmRd8"

    val path = "//tmp/knkmx/test111"
    val ytPath = YPath.simple(path)
    val yt: Yt = YtUtils.http(proxy, token)

    val tableSchema = new TableSchema.Builder()
      .addKey("id", ColumnValueType.STRING)
      .addValue("data", ColumnValueType.STRING)
      .build()
    if (yt.cypress().exists(ytPath)) yt.cypress().remove(ytPath)
    yt.cypress().create(ytPath,
      CypressNodeType.TABLE,
      Cf.hashMap[String, YTreeNode](Map(("schema" -> tableSchema.toYTree)).asJava))

    yt.cypress().set(ytPath.attribute("_yql_proto_field_data"),
      buildYqlProtoFieldData(Review.getDescriptor))

    val res = for {
      offer ← dao.traverse()(Traced.empty)
      review = offer.getReview
    } yield YTree.mapBuilder().key("id")
      .value(review.getId)
      .key("data")
      .value(review.toByteArray)
      .buildMap()

    yt.tables().write(ytPath, YTableEntryTypes.YSON, Cf.wrap(res.toIterable.asJavaCollection))

    //yt.tables().insertRows(ytPath, YTableEntryTypes.YSON, Cf.arrayList(row2).iterator())

  }


  ignore("rpc") {
    /*
    val proxy = "hahn.yt.yandex.net"
    val token = "AQAD-qJSJsSUAAAOyi9SiZnqb0PDtGF7s-KmRd8"

    val path = "//tmp/knkmx/test111"
    val ytPath = YPath.simple(path)
    val ytRpc = new YtClient(new DefaultBusConnector(), new YtCluster("hahn"),
      new RpcCredentials("knkmx", token), new RpcOptions)
    val yt = YtUtils.http(proxy, token)

    val tableSchema = new TableSchema.Builder()
      .addKey("id", ColumnValueType.STRING)
      .addValue("data", ColumnValueType.STRING)
      .build()
    if (yt.cypress.exists(ytPath)) yt.cypress().remove(ytPath)
    yt.cypress().create(ytPath,
      CypressNodeType.TABLE,
      Cf.hashMap[String, YTreeNode](Map(("schema" -> tableSchema.toYTree)).asJava))

    yt.cypress().set(ytPath.attribute("_yql_proto_field_data"),
      buildYqlProtoFieldData(Review.getDescriptor))


    val review1 = createOffer1.getReview
    val req = new ModifyRowsRequest(path, tableSchema)
    req.addInsert(List(review1.getId, review1.toByteArray).asJava)
    ytRpc.waitProxies().get(10, TimeUnit.MINUTES)
    val res = toScala(ytRpc.modifyRows(GUID.create(), req))

    Await.result(res, 10.minutes)*/
  }

}
