package ru.yandex.extdata.core.actor

import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.extdata.core.gens.Generator
import ru.yandex.extdata.core.gens.Producer._
import ru.yandex.extdata.core.storage.MetaDao

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * @author evans
  */
trait MetaDaoSpecBase extends WordSpecLike with Matchers {

  def metaDao: MetaDao

  "Meta storage" should {
    "save meta" in {
      metaDao.clear()
      val meta = Generator.MetaGen.next
      metaDao.save(meta)
      metaDao.get shouldEqual Seq(meta)
    }
    "delete meta" in {
      metaDao.clear()
      val meta = Generator.MetaGen.next
      metaDao.save(meta)
      metaDao.get shouldEqual Seq(meta)
      metaDao.delete(meta)
      metaDao.get shouldEqual Seq.empty
    }
    "get same if nothing was changed" in {
      metaDao.clear()
      val meta = Generator.MetaGen.next
      metaDao.save(meta)
      metaDao.get shouldEqual Seq(meta)
      metaDao.get shouldEqual Seq(meta)
    }
    "work concurrently" in {
      def f() = {
        val meta = Generator.MetaGen.next
        metaDao.save(meta)
        metaDao.get()
        metaDao.delete(meta)
        metaDao.get()
      }

      import ExecutionContext.Implicits.global
      val work = Future.sequence((1 to 1000).map(_ => Future(f())))
      Await.result(work, 10.seconds)
    }
  }
}
