package ru.auto.salesman.dao.yt.user

import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.dao.impl.yt.user.YtUserExclusionsDao
import ru.auto.salesman.dao.user.UserExclusionsDao.UserExclusion
import ru.auto.salesman.exception.user.NoSuchFolderException
import ru.auto.salesman.model.user.product.ProductProvider
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.userExclusionsGen
import ru.auto.salesman.test.template.YtSpecTemplate
import ru.yandex.bolts.collection.Cf
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.{CypressNodeType, YPath}
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes
import ru.yandex.inside.yt.kosher.ytree.YTreeNode
import ru.yandex.vertis.generators.NetGenerators.asProducer
import ru.yandex.yt.ytclient.tables.{ColumnValueType, TableSchema}
import zio.blocking.effectBlocking

import java.util.NoSuchElementException
import scala.collection.JavaConverters._

class YtUserExclusionsDaoSpec
    extends BaseSpec
    with YtUserExclusionsDaoSpec.YtTemplate
    with YtSpecTemplate {

  import YtUserExclusionsDaoSpec._

  private val yt = client

  private val dao = new YtUserExclusionsDao(yt, cfg)

  "should read right data if data exists" in {
    val testData = userExclusionsGen().next
    val now = DateTime.now()

    val result = (for {
      _ <- createData(testData, now, yt)
      res <- dao.getExclusionsForDate(now)
    } yield res).success.value

    result should contain theSameElementsAs testData
  }

  "should read nothing if no data for this day" in {
    val now = DateTime.now()
    val testData =
      List(testDataSample)

    val result = (for {
      _ <- createTable(now, yt)
      _ <- writeData(testData, now, yt)
      res <- dao.getExclusionsForDate(now.plusDays(1))
    } yield res).success.value

    result shouldEqual List.empty
  }

  "should fail if userId in wrong format" in {
    val now = DateTime.now()
    val wrongData = List(testDataSample.copy(userId = "123"))
    val result = (for {
      _ <- createTable(now, yt)
      _ <- writeData(wrongData, now, yt)
      res <- dao.getExclusionsForDate(now)
    } yield res).failure.exception
    result shouldBe an[IllegalArgumentException]
  }

  "should fail on unknown productId" in {
    val now = DateTime.now()
    val wrongData = List(testDataSample.copy(product = "bla-bla"))
    val result = (for {
      _ <- createTable(now, yt)
      _ <- writeData(wrongData, now, yt)
      res <- dao.getExclusionsForDate(now)
    } yield res).failure.exception
    result shouldBe an[NoSuchElementException]
  }

  "should fail if folder not exists in yt" in {
    val wrongDao = new YtUserExclusionsDao(yt, cfg.copy("//tmp/not_existed"))
    val result = wrongDao.getExclusionsForDate(DateTime.now()).failure.exception
    result shouldBe an[NoSuchFolderException]
  }

}

object YtUserExclusionsDaoSpec {

  private val testDataSample =
    YtTemplate.TestData(
      AutoruUser(123).toString,
      ProductProvider.AutoruGoods.Boost.name
    )

  trait YtTemplate {

    import YtUserExclusionsDao._
    import YtTemplate._

    protected val cfg: YtUserExclusionsSettings = YtUserExclusionsSettings(
      "//tmp"
    )

    protected def createPath(date: DateTime): YPath =
      createPathToFileByDate(cfg.folder, date)

    protected def createData(
        testData: Seq[UserExclusion],
        date: DateTime,
        yt: Yt
    ): Task[Unit] =
      for {
        _ <- createTable(date, yt)
        data = testData.map(d => TestData(d.userId.toString, d.product.name))
        _ <- writeData(data, date, yt)
      } yield ()

    protected def createTable(date: DateTime, yt: Yt): Task[Unit] =
      effectBlocking {
        val ytPath = createPath(date)

        val tableSchema = new TableSchema.Builder()
          .addKey(userIdColumnName, ColumnValueType.STRING)
          .addValue(productColumnName, ColumnValueType.STRING)
          .build()
        if (yt.cypress().exists(ytPath))
          yt.cypress().remove(ytPath)

        yt.cypress()
          .create(
            ytPath,
            CypressNodeType.TABLE,
            Cf.toHashMap[String, YTreeNode](
              Map("schema" -> tableSchema.toYTree).asJava
            )
          )
      }

    protected def writeData(
        testData: Seq[TestData],
        date: DateTime,
        yt: Yt
    ): Task[Unit] = effectBlocking {
      val ytPath = createPath(date)

      val data = testData
        .sortWith((l, r) => l.userId.compareTo(r.userId) < 0)
        .map(v =>
          YTree
            .mapBuilder()
            .key(userIdColumnName)
            .value(v.userId)
            .key(productColumnName)
            .value(v.product)
            .buildMap()
        )

      yt.tables()
        .write(ytPath, YTableEntryTypes.YSON, Cf.wrap(data.asJavaCollection))
    }
  }

  object YtTemplate {

    case class TestData(userId: String, product: String)

  }

}
