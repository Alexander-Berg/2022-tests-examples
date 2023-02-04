package ru.auto.cabinet.dao

import java.sql.{BatchUpdateException, SQLIntegrityConstraintViolationException}

import org.scalatest.flatspec.AsyncFlatSpec
import ru.auto.cabinet.dao.jdbc.SubscriptionDao
import ru.auto.cabinet.model._
import ru.auto.cabinet.service.instr.EmptyInstr
import ru.auto.cabinet.test.JdbcSpecTemplate

/** Specs [[SubscriptionDao]]
  */
class SubscriptionDaoSpec extends AsyncFlatSpec with JdbcSpecTemplate {

  implicit val instr = new EmptyInstr("test")
  val dao = new SubscriptionDao(office7Database, office7Database)

  val dummy =
    ClientSubscription(-1, client1Id, SubscriptionCategory.info, "info@me")

  val agentDummy =
    ClientSubscription(-1, agent1.agencyId, dummy.category, "info@agency")

  val seqDummy = Seq(
    ClientSubscription(
      -1,
      client1Id,
      SubscriptionCategory.cabinet,
      "cabinet@me"),
    ClientSubscription(-1, client1Id, SubscriptionCategory.money, "money@me"),
    ClientSubscription(-1, client1Id, SubscriptionCategory.legal, "legal@me"),
    dummy
  )

  "SubscriptionDao" should "create & get single subscription" in {
    for {
      inserted <- dao.create(dummy)
      got <- dao.get(inserted.id, dummy.clientId)
      _ <- dao.remove(inserted.id, dummy.clientId)
    } yield {
      inserted.id should be > 0L
      got should be(inserted)
    }
  }

  it should "forbid duplicate subscriptions" in {
    for {
      inserted <- dao.create(dummy)
      failed <- dao.create(dummy).failed
      _ <- dao.remove(inserted.id, dummy.clientId)
    } yield failed shouldBe an[SQLIntegrityConstraintViolationException]
  }

  it should "create & get batch client subscriptions" in {
    for {
      _ <- dao.createOrUpdate(dummy.clientId, seqDummy)
      got <- dao.list(dummy.clientId)
      _ <- dao.removeAll(dummy.clientId)
    } yield {
      got.map(_.category) should contain theSameElementsAs seqDummy.map(
        _.category)
      got.map(_.emailAddress) should contain theSameElementsAs seqDummy.map(
        _.emailAddress)
    }
  }

  it should "not create batch client subscriptions with wrong ClientId" in {
    for {
      failed <- dao.createOrUpdate(wrongClientId, seqDummy).failed
    } yield failed shouldBe an[IllegalArgumentException]
  }

  it should "forbid duplicate batches with duplicate subscptitions" in {
    for {
      failed <- dao.createOrUpdate(client1Id, seqDummy :+ dummy).failed
    } yield failed shouldBe an[BatchUpdateException]
  }

  it should "not get subscription by wrong clientId" in {
    for {
      inserted <- dao.create(dummy)
      failed <- dao.get(inserted.id, wrongClientId).failed
      _ <- dao.remove(inserted.id, inserted.clientId)
    } yield failed shouldBe a[NoSuchElementException]
  }

  it should "get client subscriptions by category" in {
    for {
      _ <- dao.createOrUpdate(client1Id, seqDummy)
      got <- dao.list(agent1, dummy.category)
      _ <- dao.removeAll(client1Id)
    } yield {
      all(got) should have(Symbol("category")(dummy.category))
      got should have size 1
    }
  }

  it should "get client & agent subscriptions by category" in {
    for {
      _ <- dao.createOrUpdate(client1Id, seqDummy)
      insertedAgent <- dao.create(agentDummy)
      got <- dao.list(agent1, dummy.category)
      _ <- dao.removeAll(client1Id)
      _ <- dao.remove(insertedAgent.id, insertedAgent.clientId)
    } yield {
      got should contain(insertedAgent)
      all(got) should have(Symbol("category")(dummy.category))
      got should have size 2
    }
  }

  it should "get subscriptions by category when created single" in {
    for {
      inserted <- dao.create(dummy)
      got <- dao.list(dummy.clientId, dummy.category)
      _ <- dao.remove(inserted.id, inserted.clientId)
    } yield got should be(Vector(inserted))
  }

  it should "get bulk subscriptions when created single" in {
    for {
      inserted <- dao.create(dummy)
      got <- dao.list(dummy.clientId)
      _ <- dao.remove(inserted.id, inserted.clientId)
    } yield got should be(Vector(inserted))
  }

  it should "get single subscriptions when created batch" in {
    for {
      _ <- dao.createOrUpdate(dummy.clientId, seqDummy)
      insertedSeq <- dao.list(dummy.clientId)
      inserted = insertedSeq.head
      got <- dao.get(inserted.id, inserted.clientId)
      _ <- dao.removeAll(dummy.clientId)
    } yield got should be(inserted)
  }

  it should "get subscriptions by category when created batch" in {
    for {
      _ <- dao.createOrUpdate(dummy.clientId, seqDummy)
      got <- dao.list(dummy.clientId, dummy.category)
      _ <- dao.removeAll(dummy.clientId)
    } yield {
      all(got) should have(Symbol("emailAddress")(dummy.emailAddress))
      all(got) should have(Symbol("category")(dummy.category))
      got should have size 1
    }
  }

  it should "drop single subscription" in {
    for {
      inserted <- dao.create(dummy)
      removed <- dao.remove(inserted.id, inserted.clientId)
    } yield removed.rowCount should be(1)
  }

  it should "drop batch client subscriptions by category" in {
    for {
      _ <- dao.create(dummy)
      removed <- dao.removeByCategory(dummy.clientId, dummy.category)
    } yield removed.rowCount should be(1)
  }

  it should "drop batch client subscriptions" in {
    for {
      _ <- dao.createOrUpdate(dummy.clientId, seqDummy)
      removed <- dao.removeAll(dummy.clientId)
    } yield removed.rowCount should be(seqDummy.size)
  }

  it should "update single subscription" in {
    val updatedAddress = "upd@email"
    for {
      inserted <- dao.create(dummy)
      updated <- dao.update(inserted.copy(emailAddress = updatedAddress))
      got <- dao.get(inserted.id, inserted.clientId)
      _ <- dao.remove(inserted.id, inserted.clientId)
    } yield {
      updated.rowCount should be(1)
      got.emailAddress should be(updatedAddress)
    }
  }

  it should "update batch client subscriptions" in {
    val updatedAddress = "upd@email"
    val addedCategory = SubscriptionCategory.autoload
    val updatedSeq = seqDummy.tail.tail
      .map(_.copy(emailAddress = updatedAddress)) :+
      ClientSubscription(789, dummy.clientId, addedCategory, updatedAddress)
    for {
      _ <- dao.createOrUpdate(dummy.clientId, seqDummy)
      _ <- dao.createOrUpdate(dummy.clientId, updatedSeq)
      got <- dao.list(dummy.clientId)
      _ <- dao.removeAll(dummy.clientId)
    } yield {
      got should have size updatedSeq.size
      all(got) should have(Symbol("emailAddress")(updatedAddress))
      exactly(1, got) should have(Symbol("category")(addedCategory))
    }
  }

}
