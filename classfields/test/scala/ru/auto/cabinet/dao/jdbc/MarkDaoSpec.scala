package ru.auto.cabinet.dao.jdbc

import org.scalatest.wordspec.AsyncWordSpec
import ru.auto.cabinet.model.Mark
import ru.auto.cabinet.test.JdbcSpecTemplate

class MarkDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {

  val markDao = new MarkDao(office7Database, office7Database)

  val expectedMarks = Seq(
    Mark(1, Some("TEST1"), "test1"),
    Mark(3, Some("TEST3"), "test3")
  )

  "MarkDao" should {
    "find marks by ids" in {
      for {
        marks <- markDao.findByIds(Set(1, 3))
      } yield marks shouldBe expectedMarks
    }
  }
}
