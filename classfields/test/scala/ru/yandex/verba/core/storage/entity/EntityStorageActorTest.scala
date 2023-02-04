package ru.yandex.verba.core.storage.entity

import akka.actor._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.core.application.DBInitializer
import ru.yandex.verba.core.dao.simple.SimpleIdGenerator
import ru.yandex.verba.core.model.tree.Path
import ru.yandex.verba.core.util.{Logging, QueryBuilders}
import scalikejdbc._

import scala.concurrent.duration._

/**
  * TODO
  */
class EntityStorageActorTest(system: ActorSystem)
  extends TestKit(system)
  with ImplicitSender
  with AnyFreeSpecLike
  with Matchers
  with Logging { // with H2DBTest
  DBInitializer

  def this() = this(ActorSystem("test", ConfigFactory.empty()))

  implicit val ec = system.dispatcher
  implicit val timeout = 1.second
  implicit val ses = AutoSession

  val idGenerator = new SimpleIdGenerator

  val actor = system.actorOf(
    Props(classOf[ru.yandex.verba.core.storage.entity.EntityMySQLStorageActor])
  )

  "entity storage actor" - {
    "should work" ignore {
      NamedDB(ConnectionPool.DEFAULT_NAME).readOnly { implicit session =>
//        val a = QueryBuilders.selectIdsByPath(Path("/auto/marks/BMW"));

        var counter = 4;

        sql"select from_id, to_id from entity_link_v2".foreach { row =>
          counter = counter + row.int("from_id");
        }


//
//          var N = 0
//
//          sql"select count(from_id) from entity_link_v2".foreach {
//            row =>
//              N = row.int(1)
//          }
//          val froms = new Array[Int](N)
//          val tos = new Array[Int](N)
//
//          var index = 0
//          sql"select from_id, to_id from entity_link_v2".foreach {
//            row =>
//              froms(index) = row.int("from_id")
//              tos(index) = row.int("to_id")
//              index += 1
//          }
//
//          { LowLevelUtils.sortAsHierarchicalTree(froms, tos) }.measured("blabla")

      }
    }
  }

  //
//      import ru.yandex.verba.core.util.VerbaUtils._
//      val lines = scala.io.Source.fromFile("oracache.txt").getLines.toList
//
//      val N = lines.length
//
//
//
//      val froms: Array[Int] = new Array(N);
//      val tos: Array[Int] = new Array(N);
//
//
//      var i = 0
//      lines.foreach(
//        line => {
//          val arr = line.split(' ')
//          froms(i) = arr(0).toInt
//          tos(i) = arr(1).toInt
//          i += 1
//        }
//      )
//
//
//      LowLevelUtils.sortAsHierarchicalTree(froms, tos)

//

//
//  load(Seq("/sql/entity.sql", "/sql/entity_link.sql", "/sql/history.sql", "/sql/history_entity.sql"),
//    'entity -> "entity_v2",
//    'entity_link -> "entity_link_v2"
//  )
//
//  val historyStorage = new OracleHistoryStorage(idGenerator, ConnectionPool.DEFAULT_NAME)
//
//  "entity storage actor" - {
//    "should work" in {
//      val v = historyStorage.newVersion(99L).id
//      actor ! api.Create(Entity("a", "a", Path("/a")), 0L, v)
//      expectMsg(api.Done)
//
//      actor ! api.GetOne.ByPath(Path("/a"))
//      val e = expectMsgType[Entity]
//      e.version shouldEqual v
//
//      actor ! api.GetOne.ById(e.id)
//      expectMsgType[Entity] shouldEqual e
//
//      actor ! api.Get.ById(Seq(1L,2L))
//      expectMsgType[Seq[Entity]] shouldEqual Seq(e)
//
//      actor ! api.Get.ByPath(Seq(Path("/a"), Path("/b")))
//      expectMsgType[Seq[Entity]] shouldEqual Seq(e)
//
//      val v2 = historyStorage.newVersion(98L).id
//      actor ! api.Update(e.copy(name = "b", code = "b", path = Path("/b")), v2)
//      expectMsg(api.Done)
//
//      actor ! api.GetOne.ById(e.id)
//      val e2 = expectMsgType[Entity]
//      e2.version shouldEqual v2
//      e2.path shouldEqual Path("/b")
//
//      actor ! api.GetOne.ByPath(Path("/b"))
//      expectMsgType[Entity] shouldEqual e2
//
//      actor ! api.GetOne.ByPath(Path("/a"))
//      expectMsgType[Status.Failure]
//
//      val v3 = historyStorage.newVersion(98L).id
//      actor ! api.Delete.ById(Seq(e.id), v3)
//      expectMsg(api.Done)
//      actor ! api.GetOne.ById(e.id)
//      val e3 = expectMsgType[Entity]
//      e3.isDeleted shouldEqual true
//      e3.code shouldEqual s"b_deleted_$v3"
//
//      actor ! api.GetOne.ById(e.id, api.version.ById(v))
//      expectMsgType[Entity] shouldEqual e
//
//      actor ! api.GetOne.ById(e.id, api.version.ById(v2))
//      expectMsgType[Entity] shouldEqual e2
//
//      val v4 = historyStorage.newVersion(98L).id
//      actor ! api.Restore.ByPath(Seq(e3.path), v4)
//      expectMsg(api.Done)
//
//      actor ! api.GetOne.ById(e.id)
//      val e4 = expectMsgType[Entity]
//      logger.debug("after restore: {}", e4)
//      e4.isDeleted shouldEqual false
//      e4.code shouldEqual e2.code
//      e4.path shouldEqual e2.path
//    }
//  }

  def dumpTable(table: String)(implicit session: DBSession): Unit = {
    logger.debug(s"Table $table:")
    var first = true
    SQL(s"select * from $table").foreach { row =>
      if (first) {
        val columns = for (i <- 1 to row.metaData.getColumnCount) yield row.metaData.getColumnName(i)
        logger.debug("|\t" + columns.mkString("\t|") + "\t|")
        first = false
      }
      val values = for (i <- 1 to row.metaData.getColumnCount) yield row.string(i)
      logger.debug("|\t" + values.mkString("\t|") + "\t|")
    }
  }
}
