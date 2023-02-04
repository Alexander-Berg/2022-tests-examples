package ru.auto.cabinet.dao

import org.scalatest.flatspec.AsyncFlatSpec
import ru.auto.cabinet.dao.jdbc.BuyoutTargetingDao
import ru.auto.cabinet.model.{
  BuyoutFilter,
  BuyoutTargeting,
  BuyoutTargetingMark
}
import ru.auto.cabinet.service.instr.EmptyInstr
import ru.auto.cabinet.test.JdbcSpecTemplate

/** Specs [[BuyoutTargetingDao]]
  */
class BuyoutDaoSpec extends AsyncFlatSpec with JdbcSpecTemplate {

  implicit val instr = new EmptyInstr("test")
  val dao = new BuyoutTargetingDao(office7Database, office7Database)

  private def mark1 = "GAZ"
  private def mark2 = "BMW"
  private def model1 = "2110"

  val marks = Vector(
    BuyoutTargetingMark(-1, mark1, Some(model1)),
    BuyoutTargetingMark(-1, mark2, None)
  )

  val dummy = truncDate(
    BuyoutTargeting(
      -1,
      client1Id,
      Some(0),
      Some(2000),
      None,
      None,
      None,
      Some("some@email"),
      marks))

  "JdbcBuyoutTargetingDao" should "create, get & drop buyout targeting" in {
    for {
      tgt <- dao.create(dummy)
      got <- dao.get(tgt.id, dummy.clientId)
      removed <- dao.remove(tgt.id, tgt.clientId)
    } yield {
      tgt.id should be > 0L
      got should be(tgt)
      removed.rowCount should be(1)
    }
  }

  it should "deny getting buyout targeting with wrong client" in {
    for {
      tgt <- dao.create(dummy)
      failed <- dao.get(tgt.id, wrongClientId).failed
      _ <- dao.remove(tgt.id, tgt.clientId)
    } yield {
      tgt.id should be > 0L
      failed shouldBe a[NoSuchElementException]
    }
  }

  it should "find buyout targeting by client" in {
    for {
      tgt <- dao.create(dummy)
      list <- dao.list(dummy.clientId)
      _ <- dao.remove(tgt.id, tgt.clientId)
    } yield {
      tgt.id should be > 0L
      list should contain(tgt)
    }
  }

  it should "not drop buyout targeting for wrong client" in {
    for {
      tgt <- dao.create(dummy)
      failed <- dao.remove(tgt.id, wrongClientId).failed
      _ <- dao.remove(tgt.id, tgt.clientId)
    } yield {
      tgt.id should be > 0L
      failed shouldBe a[NoSuchElementException]
    }
  }

  it should "update buyout targeting" in {
    val updatedAddress = "upd@email"
    for {
      tgt <- dao.create(dummy)
      _ <- dao.update(tgt.copy(emailAddress = Some(updatedAddress)))
      got <- dao.get(tgt.id, tgt.clientId)
      _ <- dao.remove(tgt.id, tgt.clientId)
    } yield {
      tgt.id should be > 0L
      got.emailAddress should be(Some(updatedAddress))
      got.dateCreated should be <= got.dateUpdated
    }
  }

  it should "not change buyout targeting client" in {
    for {
      tgt <- dao.create(dummy)
      failed <- dao.update(tgt.copy(clientId = wrongClientId)).failed
      _ <- dao.remove(tgt.id, tgt.clientId)
    } yield {
      tgt.id should be > 0L
      failed shouldBe a[NoSuchElementException]
    }
  }

  it should "search car sale info by markId & modelid" in {
    val filter = BuyoutFilter(0, mark1, model1, 2015, 600000)
    for {
      tgt <- dao.create(dummy)
      matched <- dao.search(filter)
      _ <- dao.remove(tgt.id, tgt.clientId)
    } yield {
      tgt.id should be > 0L
      all(matched) should have(Symbol("email")(dummy.emailAddress))
      matched should have size 1
    }
  }

  it should "search car sale info by markId only" in {
    val filter = BuyoutFilter(0, mark2, "randomModelId", 2015, 600000)
    for {
      tgt <- dao.create(dummy)
      matched <- dao.search(filter)
      _ <- dao.remove(tgt.id, tgt.clientId)
    } yield {
      tgt.id should be > 0L
      all(matched) should have(Symbol("email")(dummy.emailAddress))
      matched should have size 1
    }
  }

  it should "remove duplicate mark matches form search" in {
    val modelId = "certainModel"
    val filter = BuyoutFilter(0, mark1, modelId, 2015, 600000)
    val dummy2 = dummy.copy(
      marks = dummy.marks :+ BuyoutTargetingMark(-1, mark1, Some(modelId)))
    for {
      tgt <- dao.create(dummy2)
      matched <- dao.search(filter)
      _ <- dao.remove(tgt.id, tgt.clientId)
    } yield {
      tgt.id should be > 0L
      all(matched) should have(Symbol("email")(dummy.emailAddress))
      matched should have size 1
    }
  }

  it should "remove duplicate targeting matches form search" in {
    val filter = BuyoutFilter(0, mark1, model1, 2015, 600000)
    for {
      tgt1 <- dao.create(dummy)
      tgt2 <- dao.create(dummy)
      matched <- dao.search(filter)
      _ <- dao.remove(tgt1.id, tgt1.clientId)
      _ <- dao.remove(tgt2.id, tgt2.clientId)
    } yield {
      tgt1.id should be > 0L
      tgt2.id should be > 0L
      all(matched) should have(Symbol("email")(dummy.emailAddress))
      matched should have size 1
    }
  }

  private def truncDate(tgt: BuyoutTargeting) =
    tgt.copy(
      dateCreated = tgt.dateCreated.withNano(0),
      dateUpdated = tgt.dateUpdated.withNano(0))
}
