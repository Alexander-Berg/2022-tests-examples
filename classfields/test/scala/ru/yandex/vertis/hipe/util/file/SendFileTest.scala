package ru.yandex.vertis.hipe.util.file

import java.io.File
import java.text.SimpleDateFormat

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.hipe.pushes.SeveralDaysInactivity

import scala.io.Source

/**
  * Created by andrey on 8/22/17.
  */
@RunWith(classOf[JUnitRunner])
class SendFileTest extends FunSuite {
  private val now = new DateTime(2018, 1, 25, 12, 0)
  private val timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")
  private val path: String = "target"
  private val sendFileUtils = new SendFile("SeveralDaysInactivity", now)

  test("generate") {
    val nowStr = timestampFormat.format(now.toDate)
    val send = sendFileUtils.file
    send.delete()
    val successPushes = Seq(SeveralDaysInactivity("2", "BMW", "X5"))
    val pushTargets = Seq(
      SeveralDaysInactivity("1", "SKODA", "OCTAVIA"),
      SeveralDaysInactivity("2", "BMW", "X5"),
      SeveralDaysInactivity("3", "AUDI", "A6")
    )
    sendFileUtils.generate(pushTargets.iterator)
    assert(send.exists())
    val sendLines = Source.fromFile(sendFileUtils.file).mkString
    assert(
      sendLines ==
        """{"generated":"2018-01-25 12:00:00.0","pushes":[{"model":"OCTAVIA","type":"SeveralDaysInactivity","uuid":"1","mark":"SKODA"},{"model":"X5","type":"SeveralDaysInactivity","uuid":"2","mark":"BMW"},{"model":"A6","type":"SeveralDaysInactivity","uuid":"3","mark":"AUDI"}]}""".stripMargin
          .replaceAll("\n", "")
    )
  }

  test("load") {
    val now = DateTime.now
    val successPushes = Seq(SeveralDaysInactivity("2", "BMW", "X5"))
    val pushTargets = Seq(
      SeveralDaysInactivity("1", "SKODA", "OCTAVIA"),
      SeveralDaysInactivity("2", "BMW", "X3"),
      SeveralDaysInactivity("3", "AUDI", "A6")
    )

    val send = sendFileUtils.file
    send.delete()

    sendFileUtils.generate(pushTargets.toIterator)
    val sendData = sendFileUtils.load().toIndexedSeq
    assert(sendData.lengthCompare(3) == 0)
    sendData.head == SeveralDaysInactivity("2", "BMW", "X5")
    sendData(1) == SeveralDaysInactivity("1", "SKODA", "OCTAVIA")
    sendData(2) == SeveralDaysInactivity("3", "AUDI", "A6")
  }

}
