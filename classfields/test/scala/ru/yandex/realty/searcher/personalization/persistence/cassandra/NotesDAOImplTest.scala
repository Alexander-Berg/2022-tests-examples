package ru.yandex.realty.searcher.personalization.persistence.cassandra

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Span}
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.application.ng.cassandra.CassandraSessionConfig
import ru.yandex.realty.application.RealtimeCassandraSession

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 21.12.16
  */
class NotesDAOImplTest extends FlatSpec with Matchers with ScalaFutures with RealtimeCassandraSession {

  override def realtimeCassandraSessionConfig: CassandraSessionConfig = ???

  implicit val config = PatienceConfig(timeout = scaled(Span(1000, Millis)), interval = scaled(Span(300, Millis)))
  private val session = realtimeCassandraSession
  private val api = new CassandraPersonalizationApi(session) {
    override def maxNoteLength: Int = 10
  }

  "NotesDAO" should "correct work in base case" in {
    api.addNote("a", "1", "шашлык").futureValue
    api.addNote("a", "1", "note").futureValue
    val n1 = api.findNotes("a", Seq("1", "unknown")).futureValue
    n1 should be(Map("1" -> "note"))
    val beforeCount = api.findNotesCount("a").futureValue
    beforeCount should be >= (1L)
    api.deleteNote("a", "fake_offer").futureValue
    api.deleteNote("a", "1").futureValue
    api.findNotesCount("a").futureValue should be(beforeCount - 1)
    api.findNotes("a", Seq("1")).futureValue.size should be(0)
  }

  it should "correct shrink long notes" in {
    api.addNote("b", "1", "very long note").futureValue
    api.findNotes("b", Seq("1")).futureValue.get("1") should be(Some("very long "))
  }

  it should "correct move notes" in {
    api.addNote("a", "1", "note").futureValue
    api.addNote("b", "1", "old").futureValue
    api.addNote("b", "2", "second").futureValue

    api.moveNotes("a", "b").futureValue

    api.findNotesCount("a").futureValue should be(0)
    val notes = api.findNotes("b", Seq("1", "2")).futureValue
    notes.get("1") should be(Some("note"))
    notes.get("2") should be(Some("second"))
  }
}
