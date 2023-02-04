package ru.yandex.complaints.dao.complaints

import java.sql.Timestamp
import java.util.concurrent.{CountDownLatch, Executors}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import org.springframework.dao.DuplicateKeyException
import ru.yandex.complaints.api.util.ScheduledVisitor
import ru.yandex.complaints.dao.offers.{OfferNotFoundException, OffersDao}
import ru.yandex.complaints.dao.utils.DaoCorruptException
import ru.yandex.complaints.dao.{AutoruSto, Plain, getNow}
import ru.yandex.complaints.model.Offer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration.{Duration, DurationInt}

/**
  * Specs for [[OffersDao]]
  *
  * @author alesavin
  */
trait OffersDaoSpec
  extends WordSpec
    with Matchers
    with ScalaFutures {

  private val DefaultPatienceConfig =
    PatienceConfig(Span(10, Seconds), Span(1, Seconds))

  override implicit def patienceConfig: PatienceConfig =
    DefaultPatienceConfig

  def offersDao: OffersDao

  "OffersDao" should {

    val UserId = "123"
    val OfferId = Plain("456")
    val OfferId2 = AutoruSto("1")
    val AuthorId = "author"

    "return empty on start" in {
      offersDao.findById(OfferId) match {
        case Success(None) => ()
        case other => fail(s"Unexpected $other")
      }
    }
    "create offers" in {
      offersDao.create(OfferId, AuthorId) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.findById(OfferId) match {
        case Success(Some(Offer(OfferId, _, _, AuthorId, _))) => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.create(OfferId, s"${AuthorId}_2") match {
        case Failure(_: DuplicateKeyException) => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.findById(OfferId) match {
        case Success(Some(Offer(OfferId, _, _, AuthorId, _))) => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.create(OfferId2, AuthorId) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.findById(OfferId2) match {
        case Success(Some(Offer(OfferId2, _, _, AuthorId, _))) => ()
        case other => fail(s"Unexpected $other")
      }
    }
    "set author id" in {
      offersDao.setAuthorId(AuthorId, Plain("unknown")) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.setAuthorId(s"${AuthorId}_2", OfferId2) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.findById(OfferId2) match {
        case Success(Some(Offer(OfferId2, _, _, author, _))) =>
          author should be(s"${AuthorId}_2")
        case other => fail(s"Unexpected $other")
      }
    }
    "remove" in {
      offersDao.plainRemove(OfferId2) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.findById(OfferId2) match {
        case Success(None) => ()
        case other => fail(s"Unexpected $other")
      }
    }
    "schedule" in {
      offersDao.schedule(Seq(Plain("unknown")), 1L, clearHash = true) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.findById(OfferId) match {
        case Success(Some(Offer(OfferId, _, scheduled, _, hash))) =>
          scheduled should be(empty)
          hash should be("")
        case other => fail(s"Unexpected $other")
      }
      offersDao.schedule(Seq(OfferId), 1000L, clearHash = true) match {
        case Success(_) => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.findById(OfferId) match {
        case Success(Some(Offer(OfferId, _, scheduled, _, hash))) =>
          scheduled should be(Some(new Timestamp(1000L)))
          hash should be("0")
        case other => fail(s"Unexpected $other")
      }
    }
    "count scheduled correct" in {
      offersDao.countScheduled() shouldBe Success(1)
    }
    "lock" in {

      implicit val ec = ExecutionContext.fromExecutor(
        Executors.newScheduledThreadPool(
          10,
          new ThreadFactoryBuilder()
            .setNameFormat("OffersDaoSpec-%d")
            .build()))

      val latch = new CountDownLatch(10)
      val fs = (0 to 9) map { _ =>
        Future {
          latch.countDown()
          latch.await()
          offersDao.lock(OfferId) { offer =>
            offersDao.setAuthorId(offer.authorId + "a", offer.id)
          }
        }
      }
      Future.sequence(fs).futureValue
      offersDao.findById(OfferId) match {
        case Success(Some(Offer(OfferId, _, _, author, _))) =>
          author should be(s"${AuthorId}aaaaaaaaaa")
        case other => fail(s"Unexpected $other")
      }
    }
    "grap watch list" in {
      offersDao.grabWatchList(3) match {
        case Success(sq) =>
          sq.size should be(1)
          sq.head._1 should be(OfferId)
        case other => fail(s"Unexpected $other")
      }
    }
    "use ref" in {
      offersDao.useRef(OfferId, None)(o =>
        new ScheduledVisitor[Offer](Some(1.second), Some(o))) match {
        case true => ()
        case other => fail(s"Unexpected $other")
      }
      intercept[DaoCorruptException] {
        offersDao.useRef(OfferId, None)(o =>
          new ScheduledVisitor[Offer](Some(1.second), Some(o.copy(id = Plain("unknown")))))
      }
      intercept[OfferNotFoundException] {
        offersDao.useRef(OfferId2, None)(o =>
          new ScheduledVisitor[Offer](Some(1.second), Some(o)))
      }
      offersDao.useRef(OfferId2, Some(oid =>
        Offer(oid, new Timestamp(getNow), None, "aaa", "h1")))(o =>
        new ScheduledVisitor[Offer](Some(1.second), Some(o))) match {
        case true => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.findById(OfferId2) match {
        case Success(Some(Offer(OfferId2, _, scheduled, author, hash))) =>
          scheduled should not be (empty)
          author should be("aaa")
          hash should be("")
        case other => fail(s"Unexpected $other")
      }
      offersDao.useRef(OfferId2, None)(o =>
        new ScheduledVisitor[Offer](Some(Duration.Inf), Some(o))) match {
        case true => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.findById(OfferId2) match {
        case Success(Some(Offer(OfferId2, _, scheduled, author, hash))) =>
          scheduled should be(empty)
          author should be("aaa")
          hash should be("")
        case other => fail(s"Unexpected $other")
      }
      offersDao.useRef(OfferId2, None)(o =>
        new ScheduledVisitor[Offer](Some(Duration.Inf), Some(o))) match {
        case true => ()
        case other => fail(s"Unexpected $other")
      }
      offersDao.useRef(OfferId2, None)(o =>
        new ScheduledVisitor[Offer](None, None)) match {
        case true => ()
        case other => fail(s"Unexpected $other")
      }
    }
  }
}
