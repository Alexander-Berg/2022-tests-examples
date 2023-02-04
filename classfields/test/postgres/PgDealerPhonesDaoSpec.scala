package auto.dealers.dealer_pony.storage.postgres.test

import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestPostgresql
import doobie._
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import auto.dealers.dealer_pony.model.{DealerId, PhoneNumber}
import auto.dealers.dealer_pony.storage.dao.DealerPhonesDao
import auto.dealers.dealer_pony.storage.dao.DealerPhonesDao._
import auto.dealers.dealer_pony.storage.postgres.PgDealerPhonesDao
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.TestAspect.{after, beforeAll, failing, sequential}
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.{Has, Task, URIO, ZIO}

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

object PgDealerPhonesDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    val suit = suite("PgDealerPhonesDaoSpec")(
      singleInsertDealersPhone,
      doubleInsertionSameDealer,
      insertDeletedNumber,
      doubleDeletionInDealersPhone,
      updatePhoneNumberWithIncreasingCounter,
      updatePhoneNumberWithExceededCounter,
      updatePhoneNumberWithNotUpsertedPhones,
      nonEmptyListWithGettingPhonesToDeleteFalse,
      emptyListWithGettingPhonesToDeleteTrue,
      markToBeDeleteToTrue,
      delete,
      markToBeDeletedForNotLoyal,
      markToBeDeleteNotFoundDealer,
      entriesLeftForDealer,
      nonEmptyListWithGettingPhonesToDeleteTrue,
      setExpirationDateForDealer,
      resetExpirationDateForDealer,
      getExpired
    ) @@
      beforeAll(dbInit) @@
      after(dbClean) @@
      sequential

    suit.provideCustomLayerShared(TestPostgresql.managedTransactor >+> PgDealerPhonesDao.live)
  }

  val now = OffsetDateTime
    .now()
    .truncatedTo(ChronoUnit.DAYS)
    .plusHours(1)

  val phoneNumber0: PhoneNumber = PhoneNumber.fromString("+78009990010").toOption.get
  val phoneNumber1: PhoneNumber = PhoneNumber.fromString("+78009990011").toOption.get
  val phoneNumber2: PhoneNumber = PhoneNumber.fromString("+78009990012").toOption.get
  val phoneNumber3: PhoneNumber = PhoneNumber.fromString("+78009990013").toOption.get

  private val selectDealersPhones = sql"select dealer_id, phone from dealers_phone_numbers"
    .query[(Long, String)]

  private val selectDealersCounters = sql"select dealer_id, counter from dealers_phone_counters"
    .query[(Long, Int)]

  private val singleInsertDealersPhone = testM("insert single phone number and counter") {
    val dealerId: DealerId = 0L
    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- DealerPhonesDao.insert(dealerId, Set(phoneNumber0))
      resultDealersPhone <- selectDealersPhones.unique
        .transact(xa)
      resultCounter <- selectDealersCounters.unique
        .transact(xa)
    } yield assert(resultCounter)(equalTo((dealerId, 1))) && assert(resultDealersPhone)(
      equalTo((dealerId, phoneNumber0.number))
    )
  }

  private val doubleInsertionSameDealer =
    testM("insert same phone number twice doesn't cause error or affect counter") {
      val dealerId: DealerId = 0L
      for {
        xa <- ZIO.service[Transactor[Task]]
        _ <- DealerPhonesDao.insert(dealerId, Set(phoneNumber0))
        _ <- DealerPhonesDao.insert(dealerId, Set(phoneNumber0))
        resultDealersPhone <- selectDealersPhones.unique
          .transact(xa)
        resultCounter <- selectDealersCounters.unique
          .transact(xa)
      } yield assert(resultCounter)(equalTo((dealerId, 1))) && assert(resultDealersPhone)(
        equalTo((dealerId, phoneNumber0.number))
      )
    }

  private val insertDeletedNumber = testM("insert phone number which was deleted") {
    val dealerId: DealerId = 0L
    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- DealerPhonesDao.insert(dealerId, Set(phoneNumber0))
      _ <- DealerPhonesDao.markToBeDeleted(dealerId, Set(phoneNumber0))
      _ <- DealerPhonesDao.insert(dealerId, Set(phoneNumber0))
      resultDealersPhone <- selectDealersPhones.unique
        .transact(xa)
      resultCounter <- selectDealersCounters.unique
        .transact(xa)
    } yield assert(resultCounter)(equalTo((dealerId, 1))) && assert(resultDealersPhone)(
      equalTo((dealerId, phoneNumber0.number))
    )
  }

  private val doubleDeletionInDealersPhone = testM("second mark for deletion of the same number causes error") {
    val dealerId: DealerId = 0L
    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- DealerPhonesDao.insert(dealerId, Set(phoneNumber0))
      _ <- DealerPhonesDao.markToBeDeleted(dealerId, Set(phoneNumber0))
      res <- DealerPhonesDao.markToBeDeleted(dealerId, Set(phoneNumber0))
    } yield assert(res)(
      hasSameElements(Seq(phoneNumber0 -> Left(MarkToBeDeletedException(phoneNumber0))))
    )
  }

  private val updatePhoneNumberWithIncreasingCounter = testM("update phone number with increase counter") {
    val dealerId: DealerId = 0L
    for {
      xa <- ZIO.service[Transactor[Task]]

      _ <- DealerPhonesDao.insert(dealerId, Set(phoneNumber0))

      resultDealersPhone1 <- selectDealersPhones
        .to[Seq]
        .transact(xa)
      resultCounter1 <- selectDealersCounters
        .to[Seq]
        .transact(xa)

      _ <- DealerPhonesDao.insert(dealerId, Set(phoneNumber1))

      resultDealersPhone2 <- selectDealersPhones
        .to[Seq]
        .transact(xa)
      resultCounter2 <- selectDealersCounters
        .to[Seq]
        .transact(xa)
    } yield assert(resultDealersPhone1)(hasSameElements(Seq((dealerId, phoneNumber0.number)))) &&
      assert(resultCounter1)(hasSameElements(Seq((dealerId, 1)))) &&
      assert(resultDealersPhone2)(
        hasSameElements(Seq((dealerId, phoneNumber0.number), (dealerId, phoneNumber1.number)))
      ) &&
      assert(resultCounter2)(hasSameElements(Seq((dealerId, 2))))
  }

  private val updatePhoneNumberWithExceededCounter = testM("update phone number with exceeded counter") {
    val dealerId = 0L
    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- sql"""insert into dealers_phone_numbers(dealer_id, phone)
                 values($dealerId, ${phoneNumber0.number})""".update.run.transact(xa)
      _ <- sql"""insert into dealers_phone_counters(dealer_id, counter)
                 values($dealerId, 200)""".update.run.transact(xa)

      res <- DealerPhonesDao.insert(dealerId, Set(phoneNumber1)).run
      checkCounter <- sql"""select counter from dealers_phone_counters where dealer_id = $dealerId"""
        .query[Int]
        .unique
        .transact(xa)
    } yield assert(res)(
      fails(equalTo(InsertedExceededCounterException))
    ) && assert(checkCounter)(equalTo(200))
  }

  private val updatePhoneNumberWithNotUpsertedPhones =
    testM("update phone number with not affected row in upserting phone numbers") {
      val dealerId0 = 0L
      val dealerId1 = 1L

      for {
        xa <- ZIO.service[Transactor[Task]]
        _ <- sql"""insert into dealers_phone_numbers(dealer_id, phone)
                 values($dealerId0, $phoneNumber0)""".update.run.transact(xa)
        _ <- sql"""insert into dealers_phone_counters(dealer_id, counter)
                 values($dealerId0, 1)""".update.run.transact(xa)

        res <- DealerPhonesDao.insert(dealerId1, Set(phoneNumber0))
      } yield assert(res)(
        hasSameElements(Seq(phoneNumber0 -> Left(DuplicatePhoneException(phoneNumber0))))
      )
    }

  private val nonEmptyListWithGettingPhonesToDeleteFalse =
    testM("get none empty list of dealers phone number with to_be_deleted == FALSE") {
      val dealerId = 0L
      for {
        xa <- ZIO.service[Transactor[Task]]
        _ <- sql"""insert into dealers_phone_numbers(dealer_id, phone)
                   values ($dealerId, ${phoneNumber0.number})""".update.run
          .transact(xa)
          .unit
        result <- DealerPhonesDao.get(dealerId)
      } yield assert(result.head.number)(equalTo(phoneNumber0.number))
    }

  private val emptyListWithGettingPhonesToDeleteTrue =
    testM("get empty list of dealers phone number with to_be_deleted == true") {
      val dealerId = 0L
      for {
        xa <- ZIO.service[Transactor[Task]]
        _ <-
          sql"""insert into dealers_phone_numbers(dealer_id, phone, to_be_deleted)
              values ($dealerId, ${phoneNumber0.number}, TRUE)""".update.run
            .transact(xa)
            .unit
        result <- DealerPhonesDao.get(dealerId)
      } yield assert(result)(isEmpty)
    }

  private val nonEmptyListWithGettingPhonesToDeleteTrue =
    testM("get none empty list of dealers phone number with to_be_deleted == TRUE") {
      val dealerId0 = 0L
      val dealerId1 = 1L
      type DealerPhoneNumber = (DealerId, PhoneNumber, Boolean)

      val insertDealerPhoneNumber =
        """insert into dealers_phone_numbers(dealer_id, phone, to_be_deleted)
         values(?, ?, ?)"""

      val dealerPhoneNumbers: Seq[DealerPhoneNumber] = Seq(
        (dealerId0, phoneNumber0, true),
        (dealerId0, phoneNumber1, true),
        (dealerId1, phoneNumber2, true),
        (dealerId1, phoneNumber3, true)
      )

      val expectedDealerToPhones: Seq[(DealerId, Seq[PhoneNumber])] = Seq(
        (dealerId0, Seq(phoneNumber0, phoneNumber1)),
        (dealerId1, Seq(phoneNumber2, phoneNumber3))
      )

      for {
        xa <- ZIO.service[Transactor[Task]]
        _ <- Update[DealerPhoneNumber](insertDealerPhoneNumber).updateMany(dealerPhoneNumbers).transact(xa).unit
        result <- DealerPhonesDao.getToBeDeleted(2)
      } yield assert(result.size)(equalTo(2)) && assert(result)(hasSameElements(expectedDealerToPhones))
    }

  private val markToBeDeleteToTrue = testM("mark phone as to_be_deleted") {
    val dealerId: DealerId = 0L
    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- sql"""insert into dealers_phone_counters(dealer_id, counter)
              values ($dealerId, 1)""".update.run
        .transact(xa)
        .unit
      _ <- sql"""insert into dealers_phone_numbers(dealer_id, phone)
              values ($dealerId, ${phoneNumber0.number})""".update.run
        .transact(xa)
        .unit
      res <- DealerPhonesDao.markToBeDeleted(dealerId, Set(phoneNumber0))
      result <- sql"select dealer_id, phone, to_be_deleted from dealers_phone_numbers where dealer_id = $dealerId"
        .query[(Long, String, Boolean)]
        .unique
        .transact(xa)
    } yield assert(result)(equalTo((dealerId, phoneNumber0.number, true)))
  }

  private val markToBeDeleteNotFoundDealer = testM("mark phone as to_be_deleted not found dealer") {
    val dealerId: DealerId = 0L
    for {
      res <- DealerPhonesDao.markToBeDeleted(dealerId, Set(phoneNumber0))
    } yield assert(res)(hasSameElements(Seq(phoneNumber0 -> Left(MarkToBeDeletedException(phoneNumber0)))))
  }

  private val delete = testM("delete phone number") {
    val dealerId: DealerId = 0L

    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- sql"""insert into dealers_phone_numbers(dealer_id, phone)
              values ($dealerId, ${phoneNumber0.number})""".update.run
        .transact(xa)
        .unit
      _ <- sql"""insert into dealers_phone_numbers(dealer_id, phone)
              values ($dealerId, ${phoneNumber1.number})""".update.run
        .transact(xa)
        .unit
      _ <- DealerPhonesDao.delete(dealerId, Seq(phoneNumber0, phoneNumber1))
      result <- sql"""select phone from dealers_phone_numbers where dealer_id = $dealerId"""
        .query[String]
        .to[Seq]
        .transact(xa)
    } yield assert(result)(isEmpty)
  }

  private val markToBeDeletedForNotLoyal = testM("mark to_be_deleted for not loyal dealers") {
    val dealerId0: DealerId = 0L
    val dealerId1: DealerId = 1L

    type DealerStatus = (DealerId, OffsetDateTime, Int, Int, Boolean, Boolean)
    type DealerPhoneNumber = (DealerId, String, Boolean)
    type DealerPhoneCounter = (DealerId, Int)

    val now = OffsetDateTime
      .now()
      .truncatedTo(ChronoUnit.DAYS)
      .plusHours(1)

    val dealersStatus: Seq[DealerStatus] = Seq(
      (dealerId0, now, 12, 0, true, false),
      (dealerId1, now, 12, 0, true, true)
    )

    val insertDealersStatus =
      """insert into dealers_status(dealer_id, updated_at, loyalty_level, region_id, has_full_stock, wl_available)
         values(?, ?, ?, ?, ?, ?)"""

    val dealerPhoneNumbers: Seq[DealerPhoneNumber] = Seq(
      (dealerId0, phoneNumber0.number, false),
      (dealerId1, phoneNumber1.number, false)
    )

    val insertDealerPhoneNumber =
      """insert into dealers_phone_numbers(dealer_id, phone, to_be_deleted)
         values(?, ?, ?)"""

    val dealerPhoneCounters: Seq[DealerPhoneCounter] = Seq(
      (dealerId0, 11),
      (dealerId1, 11)
    )

    val insertDealerPhoneCounters =
      """insert into dealers_phone_counters(dealer_id, counter)
         values(?, ?)"""

    val expectedDealerPhoneNumbers: Seq[DealerPhoneNumber] = Seq(
      (dealerId0, phoneNumber0.number, true),
      (dealerId1, phoneNumber1.number, false)
    )

    val expectedDealerPhoneCounters: Seq[DealerPhoneCounter] = Seq(
      (dealerId1, 11),
      (dealerId0, 0)
    )

    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- Update[DealerStatus](insertDealersStatus)
        .updateMany(dealersStatus)
        .transact(xa)
        .unit
      _ <- Update[DealerPhoneNumber](insertDealerPhoneNumber)
        .updateMany(dealerPhoneNumbers)
        .transact(xa)
        .unit
      _ <- Update[DealerPhoneCounter](insertDealerPhoneCounters)
        .updateMany(dealerPhoneCounters)
        .transact(xa)
        .unit

      _ <- DealerPhonesDao.markToBeDeletedForNotLoyal

      resultNumbers <- sql"""select dealer_id, phone, to_be_deleted from dealers_phone_numbers"""
        .query[DealerPhoneNumber]
        .to[Seq]
        .transact(xa)
      resultCounters <- sql"""select dealer_id, counter from dealers_phone_counters"""
        .query[DealerPhoneCounter]
        .to[Seq]
        .transact(xa)
    } yield assert(resultNumbers)(hasSameElements(expectedDealerPhoneNumbers)) &&
      assert(resultCounters)(equalTo(expectedDealerPhoneCounters))
  }

  private val entriesLeftForDealer = testM("get number of resting phones to insert") {
    val dealerId: DealerId = 0L

    val expectedPhoneNumbers = 198

    for {
      _ <- DealerPhonesDao.insert(dealerId, Set(phoneNumber0, phoneNumber1))

      result <- DealerPhonesDao.entriesLeftForDealer(dealerId)
    } yield assert(result)(equalTo(expectedPhoneNumbers))
  }

  private val setExpirationDateForDealer =
    testM("Expiration date setting") {
      for {
        xa <- ZIO.access[Has[Transactor[Task]]](_.get)
        _ <- DealerPhonesDao.insert(0, Set(phoneNumber0))
        _ <- DealerPhonesDao.updateExpirationDates(0, Seq((phoneNumber0, now)))
        result <- sql"SELECT expiration_date FROM dealers_phone_numbers WHERE dealer_id = ${0}"
          .query[Option[OffsetDateTime]]
          .unique
          .transact(xa)
      } yield assert(result)(isSome(equalTo(now)))
    }

  private val resetExpirationDateForDealer =
    testM("Expiration date resetting") {
      for {
        xa <- ZIO.access[Has[Transactor[Task]]](_.get)
        _ <- DealerPhonesDao.insert(0, Set(phoneNumber0))
        _ <- DealerPhonesDao.updateExpirationDates(0, Seq((phoneNumber0, now)))
        _ <- DealerPhonesDao.markToBeDeleted(0, Set(phoneNumber0))
        _ <- DealerPhonesDao.insert(0, Set(phoneNumber0))
        result <- sql"SELECT expiration_date FROM dealers_phone_numbers WHERE dealer_id = 0"
          .query[Option[OffsetDateTime]]
          .unique
          .transact(xa)
      } yield assert(result)(isNone)
    }

  private val getExpired = testM("get expired dealers phone") {
    val now = OffsetDateTime
      .now()
      .truncatedTo(ChronoUnit.DAYS)
      .plusDays(1)

    val dealerId0 = 0L
    val dealerId1 = 1L
    val dealerId2 = 2L
    type DealerPhoneNumber = (DealerId, PhoneNumber, Boolean, OffsetDateTime)

    val insertDealerPhoneNumber =
      """insert into dealers_phone_numbers(dealer_id, phone, to_be_deleted, expiration_date)
         values(?, ?, ?, ?)"""

    val dealerPhoneNumbers: Seq[DealerPhoneNumber] = Seq(
      (dealerId0, phoneNumber0, true, now),
      (dealerId1, phoneNumber1, false, now),
      (dealerId2, phoneNumber2, true, now.minusDays(2))
    )

    val expected = Seq((dealerId1, Seq(phoneNumber1)))

    for {
      xa <- ZIO.service[Transactor[Task]]
      _ <- Update[DealerPhoneNumber](insertDealerPhoneNumber)
        .updateMany(dealerPhoneNumbers)
        .transact(xa)
        .unit

      result <- DealerPhonesDao.getExpiring(
        OffsetDateTime
          .now()
          .truncatedTo(ChronoUnit.DAYS),
        2
      )
    } yield assert(result)(equalTo(expected))
  }

  private val dbInit: URIO[Has[doobie.Transactor[Task]], Unit] = ZIO
    .service[Transactor[Task]]
    .flatMap(InitSchema("/schema.sql", _))
    .orDie

  private val dbClean = ZIO
    .service[Transactor[Task]]
    .flatMap { xa =>
      sql"DELETE FROM dealers_privileged".update.run.transact(xa) *>
        sql"DELETE FROM dealers_status".update.run.transact(xa) *>
        sql"DELETE FROM dealers_phone_numbers".update.run.transact(xa) *>
        sql"DELETE FROM dealers_phone_counters".update.run.transact(xa)
    }

}
