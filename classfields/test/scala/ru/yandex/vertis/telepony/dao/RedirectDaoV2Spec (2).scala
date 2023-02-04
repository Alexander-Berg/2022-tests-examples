package ru.yandex.vertis.telepony.dao

import java.sql.SQLIntegrityConstraintViolationException

import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.generator.Generator
import ru.yandex.vertis.telepony.generator.Generator.PhoneGen
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.service.RedirectServiceV2.Filter
import ru.yandex.vertis.telepony.util.Range.Full
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}

import scala.concurrent.Await

/**
  * @author evans
  */
trait RedirectDaoV2Spec extends SpecBase with BeforeAndAfterEach with DatabaseSpec {

  def redirectDao: RedirectDaoV2

  def operatorNumberDao: OperatorNumberDaoV2

  private val Moscow = 213
  private val Spb = 1
  private val source = PhoneGen.next
  private val target = PhoneGen.next
  private val target2 = PhoneGen.next

  private def makeNumber =
    OperatorNumber(
      PhoneGen.next,
      OperatorAccounts.MtsShared,
      Operators.Mts,
      Moscow,
      PhoneTypes.Local,
      Status.Ready(None),
      None
    )
  private val source2 = PhoneGen.next

  private val number2 =
    OperatorNumber(source2, OperatorAccounts.MtsShared, Operators.Mts, Spb, PhoneTypes.Local, Status.Ready(None), None)

  def createRedirectForNumber(number: OperatorNumber, target: Phone): ActualRedirect = {
    operatorNumberDao.create(number).databaseValue.futureValue
    val redirect: ActualRedirect = redirectFor(number, target)
    redirectDao.create(redirect).databaseValue.futureValue
    redirect
  }

  private def redirectFor(opn: OperatorNumber, target: Phone) =
    Generator.generateRedirectV2(opn, target).next

  "RedirectDao" should {
    "create redirect" in {
      val r = createRedirectForNumber(makeNumber, target)
      val actual: ActualRedirect = redirectDao.get(r.id).databaseValue.futureValue.get
      r shouldEqual actual
    }
    "get antiFraud when enabled" in {
      val hn = makeNumber
      operatorNumberDao.create(hn).databaseValue.futureValue
      val r = redirectFor(hn, target)

      redirectDao.create(r).databaseValue.futureValue
      val actual = redirectDao.antiFraud(r.source.number).databaseValue.futureValue
      r.antiFraud shouldEqual actual
    }
    "get antiFraud when blacklisted" in {
      val hn = makeNumber
      operatorNumberDao.create(hn).databaseValue.futureValue
      val r = redirectFor(hn, target).copy(antiFraud = Set(AntiFraudOptions.Blacklist))

      redirectDao.create(r).databaseValue.futureValue
      val actual = redirectDao.antiFraud(r.source.number).databaseValue.futureValue
      r.antiFraud shouldEqual actual
    }
    "get antiFraud when disabled" in {
      val hn = makeNumber
      operatorNumberDao.create(hn).databaseValue.futureValue
      val r = redirectFor(hn, target).copy(antiFraud = Set.empty)

      redirectDao.create(r).databaseValue.futureValue
      val actual = redirectDao.antiFraud(r.source.number).databaseValue.futureValue
      r.antiFraud shouldEqual actual
    }
    "get antiFraud when not exist" in {
      val actual = redirectDao.antiFraud(target).databaseValue.futureValue
      actual shouldEqual Set.empty
    }
    "set antiFraud when differs" in {
      val hn = makeNumber
      operatorNumberDao.create(hn).databaseValue.futureValue
      val r = redirectFor(hn, target).copy(antiFraud = Set.empty)

      redirectDao.create(r).databaseValue.futureValue
      val result = redirectDao
        .setAntiFraud(r.id, AntiFraud.All)
        .databaseValue
        .futureValue
      result should ===(true)
      val actual = redirectDao.antiFraud(r.source.number).databaseValue.futureValue
      actual shouldEqual AntiFraud.All
    }
    "list all redirects" in {
      val r = createRedirectForNumber(makeNumber, target)
      val r2 = createRedirectForNumber(makeNumber, target)

      val actualResult = redirectDao.list(Filter.Empty, Full).databaseValue.futureValue

//      actualResult.total shouldEqual 2
      actualResult.toSet should contain allElementsOf Set(r, r2)
    }

    "list redirects by source filter" in {
      val number = makeNumber
      val r = createRedirectForNumber(number, target)
      createRedirectForNumber(makeNumber, target)
      val actualResult = redirectDao.list(Filter.BySource(number.number), Full).databaseValue.futureValue

      actualResult.total shouldEqual 1
      actualResult.head shouldEqual r
    }

    "list redirects by target filter" in {
      val someTarget = PhoneGen.next
      val r = createRedirectForNumber(makeNumber, someTarget)
      createRedirectForNumber(makeNumber, target2)
      val actualResult = redirectDao.list(Filter.ByTarget(someTarget), Full).databaseValue.futureValue

      actualResult.total shouldEqual 1
      actualResult.head shouldEqual r
    }

    "list redirects by object and phone prefix filter" in {
      val number = makeNumber
      val r = createRedirectForNumber(number, target)
      createRedirectForNumber(number2, target2)

      val targetPrefix = PhonePrefix(target.value.substring(0, 6))
      val numberPrefix = PhonePrefix(number.number.value.substring(0, 4))
      val filter = Filter.ByObjectIdAndPhonePrefixes(r.objectId, Some(targetPrefix), Some(numberPrefix))

      val actualResult = redirectDao.list(filter, Full).databaseValue.futureValue

      actualResult.total shouldEqual 1
      actualResult.head shouldEqual r
    }

    "delete redirect" in {
      val hn = makeNumber
      operatorNumberDao.create(hn).databaseValue.futureValue
      val redirect = redirectFor(hn, target)

      redirectDao.create(redirect).databaseValue.futureValue
      redirectDao.delete(redirect.id).databaseValue.futureValue
      redirectDao.get(redirect.id).databaseValue.futureValue shouldBe None
    }

    "fail on creating same redirect" in {
      val hn = makeNumber
      operatorNumberDao.create(hn).databaseValue.futureValue
      val redirect = redirectFor(hn, target)

      redirectDao.create(redirect).databaseValue.futureValue

      intercept[SQLIntegrityConstraintViolationException] {
        import scala.concurrent.duration._
        Await.result(redirectDao.create(redirect).databaseValue, 3.seconds)
      }
    }

    "get redirect" in {
      val hn = makeNumber
      operatorNumberDao.create(hn).databaseValue.futureValue
      val redirect = redirectFor(hn, target)
      redirectDao.create(redirect).databaseValue.futureValue

      val found = redirectDao.get(redirect.id).databaseValue.futureValue.get
      found shouldEqual redirect
    }

    "get redirect by phone" in {
      val hn = makeNumber
      operatorNumberDao.create(hn).databaseValue.futureValue
      val redirect = redirectFor(hn, target)
      redirectDao.create(redirect).databaseValue.futureValue

      val found = redirectDao.get(redirect.source.number).databaseValue.futureValue
      found shouldEqual Some(redirect)
    }

    "update options caller id mode" in {
      val hn = makeNumber
      operatorNumberDao.create(hn).databaseValue.futureValue
      val redirect = redirectFor(hn, target)
      redirectDao.create(redirect).databaseValue.futureValue

      val found = redirectDao.get(redirect.source.number).databaseValue.futureValue.get
      val existAndTrue = found.options.flatMap(_.callerIdMode).getOrElse(false)
      val inverted = !existAndTrue
      redirectDao
        .updateOptions(found.id, Some(RedirectOptions.Empty.copy(callerIdMode = Some(inverted))))
        .databaseValue
        .futureValue
      val changed = redirectDao.get(redirect.source.number).databaseValue.futureValue.get
      changed.options shouldEqual Some(RedirectOptions.Empty.copy(callerIdMode = Some(inverted)))
    }
  }

  override protected def beforeEach(): Unit = {
//    redirectDao.clear().databaseValue.futureValue
//    operatorNumberDao.clear().databaseValue.futureValue
    super.beforeEach()
  }
}
