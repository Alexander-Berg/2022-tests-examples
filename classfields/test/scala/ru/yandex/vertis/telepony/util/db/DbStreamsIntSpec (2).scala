package ru.yandex.vertis.telepony.util.db

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.BeforeAndAfterAll
import ru.yandex.vertis.telepony.dao.OperatorNumberDaoV2.Filter
import ru.yandex.vertis.telepony.dao.jdbc.JdbcOperatorNumberDaoV2
import ru.yandex.vertis.telepony.generator.Generator
import ru.yandex.vertis.telepony.generator.Producer.generatorAsProducer
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}
import slick.basic.DatabasePublisher
import slick.dbio.DBIOAction

import scala.annotation.nowarn

/**
  *
  * @author zvez
  */
@nowarn
class DbStreamsIntSpec extends SpecBase with DatabaseSpec with JdbcSpecTemplate with BeforeAndAfterAll {

  val dao = new JdbcOperatorNumberDaoV2(TypedDomains.autoru_def)

  implicit val actorSystem = ActorSystem("test")
  implicit val materializer = ActorMaterializer()(actorSystem)

  override protected def beforeAll() = {
    super.beforeAll()
    val createSeq = Generator.OperatorNumberGen.nextUniqueBy(100)(_.number.value).map(dao.create)
    DBIOAction.sequence(createSeq).databaseValue.futureValue
  }

  override protected def afterAll() = {
    actorSystem.terminate()
    super.afterAll()
  }

  private def consumePublisher[T](pub: DatabasePublisher[T]) =
    consumeSource(Source.fromPublisher(pub))

  private def consumeSource[T](src: Source[T, NotUsed]) =
    src.runWith(Sink.seq).futureValue

  "DbStreams.makePublisher with useDbStream=true" should {
    "work just like non-streaming version" in {
      val list = consumePublisher(
        DbStreams.makePublisher(dualDb.slave, dao.list(Filter.All), useDbStream = true)
      )
      list should not be empty

      val expected = consumePublisher(
        dualDb.master.underlyingDb.stream(dao.list(Filter.All))
      )
      list shouldBe expected
    }
  }

  "DbStreams.makeFromSliced" should {
    "work just like stream" in {
      val list = consumeSource(
        DbStreams.makeFromSliced(dualDb.master.underlyingDb, pageSize = 42)(dao.listSome(Filter.All, _))
      )
      list should not be empty

      val expected = consumePublisher(
        dualDb.master.underlyingDb.stream(dao.list(Filter.All))
      )
      list shouldBe expected
    }
  }

}
