package ru.yandex.vertis.ydb.skypper

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.tracing.Traced
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class DoctorShiftsTest extends AnyFunSuite with Matchers with InitTestYdb {
  implicit private val trace: Traced = Traced.empty

  private val names =
    List("Alice", "Bob", "John", "Christopher", "Elliot", "Perry", "Neil", "Judy", "Ken", "Christa", "Sam", "Elizabeth")

  test("concurrent doctor shift changes") {
    (1 to 3).foreach(_ => {
      initData()
      runTest()
    })
  }

  test("concurrent doctor shift changes: async version") {
    (1 to 10).foreach(_ => {
      initData()
      runTestAsync()
    })
  }

  private def initData(): Unit = {
    names.foreach { name =>
      ydb.update("insert_doctor_to_shift") {
        s"upsert into doctors (shift_id, name, on_call) values (1234, '$name', true)"
      }
    }

  }

  private def runTest(): Unit = {
    val start = System.currentTimeMillis() + 3000
    val threads = names.map(name => dropShift(name, start))
    threads.foreach(_.join())
    implicit val countReader: YdbReads[String] = YdbReads(rs => rs.getColumn(0).getUtf8)
    val doctorsOnCall = ydb
      .query("count_on_call")(
        "select name from doctors where shift_id = 1234 and on_call = true"
      )
      .toList
    doctorsOnCall should have size 2
    println(s"stay on shift: ${doctorsOnCall.mkString(", ")}")
  }

  private def runTestAsync(): Unit = {
    val start = System.currentTimeMillis() + 3000
    val futures = names.map(name => dropShiftAsync(name, start))
    val res = Future.sequence(futures)
    Await.result(res, 10.seconds)
    implicit val countReader: YdbReads[String] = YdbReads(rs => rs.getColumn(0).getUtf8)
    val doctorsOnCall = ydb
      .query("count_on_call")(
        "select name from doctors where shift_id = 1234 and on_call = true"
      )
      .toList
    doctorsOnCall should have size 2
    println(s"stay on shift: ${doctorsOnCall.mkString(", ")}")
  }

  private def dropShift(name: String, start: Long): Thread = {
    val t = new Thread(new Runnable {
      override def run(): Unit = {
        Thread.sleep(start - System.currentTimeMillis())
        ydb.transaction("drop_shift") { executor =>
          val sql = "select name from doctors where shift_id = 1234 and on_call = true"
          val doctorsOnCall = executor.query("count_on_call")(sql)(YdbReads(rs => rs.getColumn(0).getUtf8)).toList
          if (doctorsOnCall.length > 2) {
            executor.update("drop_shift_request") {
              s"update doctors set on_call = false where shift_id = 1234 and name = '$name'"
            }
          }
        }
      }
    })
    t.start()
    t
  }

  private def dropShiftAsync(name: String, start: Long): Future[Unit] = {
    ydb.transactionAsync("drop_shift_async") { executor =>
      implicit val countReader: YdbReads[String] = YdbReads(rs => rs.getColumn(0).getUtf8)
      val sql = "select name from doctors where shift_id = 1234 and on_call = true"
      for {
        doctorsOnCall <- executor.queryAsync("count_on_call_async")(sql)
        _ <- if (doctorsOnCall.length > 2) executor.updateAsync("drop_shift_async_request") {
          s"update doctors set on_call = false where shift_id = 1234 and name = '$name'"
        }
        else Future.unit
      } yield ()
    }
  }
}
