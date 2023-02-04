package ru.yandex.auto.vin.decoder.scheduler.local.utils

import auto.carfax.common.utils.app.TestJaegerTracingSupport
import auto.carfax.common.utils.tracing.Traced
import org.apache.commons.lang3.time.DurationFormatUtils
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.util.concurrent.atomic.AtomicInteger
import java.util.{Timer, TimerTask}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait LocalScript extends TestJaegerTracingSupport {

  implicit protected val ops: TestOperationalSupport = TestOperationalSupport
  implicit protected val traced: Traced = Traced.empty

  protected val itemsWord: String = "items"

  def action: Future[Any]

  def main(args: Array[String]): Unit = {
    val start = System.currentTimeMillis
    try {
      println(s"Script $scriptName STARTED")
      Await.result(action, Duration.Inf)
      progressBar.finish()
    } catch {
      case ex: Throwable =>
        progressBar.finish()
        System.err.println(ex.getMessage)
        ex.printStackTrace()
        println(s"Script $scriptName FAILED after ${formatIntervalVerbose(System.currentTimeMillis - start)}")
        System.exit(1)
    }
    println(s"Script $scriptName completed OK in ${formatIntervalVerbose(System.currentTimeMillis - start)}")
    System.exit(0)
  }

  protected val scriptName: String = this.getClass.getSimpleName.replaceAll("\\$+$", "")
  protected val progressBar: ProgressBar = new ProgressBar

  protected def formatIntervalVerbose(millis: Long): String = {
    val words = List("millisecond", "second", "minute", "hour", "day")
    val milliz = millis                 % 1000
    val seconds = millis / 1000         % 60
    val minutes = millis / 1000 / 60    % 60
    val hours = millis / 1000 / 60 / 60 % 24
    val days = millis / 1000 / 60 / 60 / 24
    List(milliz, seconds, minutes, hours, days)
      .filterNot(_ == 0)
      .zip(words)
      .map { case (n, word) => s"$n $word${if (n == 1) "" else "s"}" }
      .reverse
      .mkString(" ")
  }

  protected def formatInterval(millis: Long): String = {
    val tokens = DurationFormatUtils.formatDuration(millis, "dd:HH:mm:ss").split(":")
    val ddHH = tokens.take(2).dropWhile(_ == "00")
    val mmss = tokens.takeRight(2)
    (ddHH ++ mmss).mkString(":")
  }

  protected def formatLong(n: Long): String = {
    n.toString.reverse.sliding(3, 3).toList.reverse.map(_.reverse).mkString(" ")
  }

  protected def pause(seconds: Int, msg: String): Unit = {
    (seconds to 0 by -1).foreach { sec =>
      print(s"\r$msg${if (sec != 0) s" in $sec seconds" else ""}...")
      Thread.sleep(1000)
    }
    println()
  }

  /* PROGRESS BAR */

  protected class ProgressBar(visualSteps: Int = 32) {

    private var totalItems = -1
    private val readyItems = new AtomicInteger(0)
    private var start = 0L

    def start(totalItems: Int = -1): Unit = {
      this.totalItems = totalItems
      this.start = System.currentTimeMillis
      printCurProgress()
      if (!totalItemsKnown) {
        initSpinner()
      }
    }

    def inc(items: Int): Unit = {
      readyItems.addAndGet(items)
      printCurProgress()
    }

    def finish(): Unit = {
      stopSpinner()
      printCurProgress(finish = true)
      println()
    }

    private def totalItemsKnown = {
      totalItems != -1
    }

    private def printCurProgress(finish: Boolean = false): Unit = {
      val suffix = if (finish) "" else "..."
      print(s"\r$curProgress$suffix")
    }

    private def curProgress: String = {
      val timeSpent = if (start != 0) formatInterval(System.currentTimeMillis - start) else ""
      if (totalItemsKnown) {
        val progress = readyItems.doubleValue / totalItems
        val readySteps = Math.floor(visualSteps * progress).toInt
        val notReadySteps = visualSteps - readySteps
        val bar = s"[${"#" * readySteps}${"-" * notReadySteps}]"
        s"$bar $timeSpent  ${formatLong(readyItems.get)} of ${formatLong(totalItems)} $itemsWord processed"
      } else {
        s"$curSpinner $timeSpent  ${formatLong(readyItems.get)} $itemsWord processed"
      }
    }

    /* SPINNER STUFF */

    private val spinnerTimer = new Timer("progress-spinner")

    private val spinnerTimerTask = new TimerTask() {

      override def run(): Unit = {
        curSpinnerCharInd = (curSpinnerCharInd + 1) % spinnerChars.size
        printCurProgress()
      }
    }
    private var isSpinningNow = false
    private var curSpinnerCharInd = 0
    private val spinnerChars = List("|", "/", "-", "\\")

    private def initSpinner(): Unit = {
      spinnerTimer.schedule(spinnerTimerTask, 0, 500)
      isSpinningNow = true
    }

    private def curSpinner: String = {
      if (isSpinningNow) {
        spinnerChars(curSpinnerCharInd)
      } else {
        ""
      }
    }

    private def stopSpinner(): Unit = {
      spinnerTimerTask.cancel()
      isSpinningNow = false
    }
  }
}
