package ru.yandex.realty.util.concurrent

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase

import scala.concurrent.duration._
import scala.concurrent.Future

/**
  * Runnable specs on [[BatchOperation]].
  *
  * @author abulychev
  */
@RunWith(classOf[JUnitRunner])
class BatchOperationSpec extends AsyncSpecBase {

  "BatchOperationSpec" should {
    "run on batch overflow" in {
      val batchOp = new BatchOperation[Int, Int](100, 1.hour)(sum)

      val s1 = (0 until 50).map(_ => 1)
      val s2 = (0 until 50).map(_ => 2)
      val result = 50 * 1 + 50 * 2

      val f1 = batchOp(s1)
      val f2 = batchOp(s2)

      f1.futureValue should be(result)
      f2.futureValue should be(result)
    }

    "run on batch on timeout" in {
      val batchOp = new BatchOperation[Int, Int](100, 1.second)(sum)

      val s1 = (0 until 50).map(_ => 1)
      val result = 50 * 1

      val f1 = batchOp(s1)

      f1.futureValue should be(result)
    }
  }

  private def sum(seq: Seq[Int]): Future[Int] = {
    Future.successful(seq.sum)
  }

}
