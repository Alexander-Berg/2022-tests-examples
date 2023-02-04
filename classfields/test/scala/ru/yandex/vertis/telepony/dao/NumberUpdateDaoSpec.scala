package ru.yandex.vertis.telepony.dao

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.dao.NumberUpdateDao.NumberWithOperator
import ru.yandex.vertis.telepony.model.{Operator, Phone}
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}
import ru.yandex.vertis.telepony.generator.Generator.{originOperatorGen, OperatorGen}
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.util.db.SlickUtils.RichDatabasePublisher

import scala.concurrent.ExecutionContextExecutor

/**
  * @author tolmach
  */
trait NumberUpdateDaoSpec[T] extends SpecBase with DatabaseSpec with BeforeAndAfterEach {

  implicit val actorSystem: ActorSystem = ActorSystem("test", ConfigFactory.empty())
  implicit val actorMaterializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = actorMaterializer.executionContext

  def dao: NumberUpdateDao

  def clear(): Unit

  override protected def beforeEach(): Unit = {
    clear()
    super.beforeEach()
  }

  case class NumberWithOperators(number: Phone, operator: Operator, originOperator: Option[Operator])

  def numbersWithOperators(count: Int, operator: Operator, withOriginOperator: Boolean): Seq[T]

  def toNumberWithOperator(number: T): NumberWithOperator

  def addNumbers(numbers: Seq[T]): Unit

  def allNumbers(): Seq[NumberWithOperators]

  "NumberUpdateDao" should {
    "read noting" when {
      "db is empty" in {
        val result = dao.getWithoutOriginOperator().runToSeq().futureValue
        result shouldBe empty
      }
      "all numbers in db have an origin operator" in {
        val count = 10
        val operator = OperatorGen.next
        val numbers = numbersWithOperators(count, operator, withOriginOperator = true)
        addNumbers(numbers)

        val actual = dao.getWithoutOriginOperator().runToSeq().futureValue
        actual shouldBe empty
      }
    }
    "read rows" when {
      "all numbers in db don't have an original operator" in {
        val count = 10
        val operator = OperatorGen.next
        val numbers = numbersWithOperators(count, operator, withOriginOperator = false)
        addNumbers(numbers)

        val actual = dao.getWithoutOriginOperator().runToSeq().futureValue
        val expected = numbers.map(toNumberWithOperator)
        actual should contain theSameElementsAs expected
      }
    }
    "set origin operator" when {
      "pass numbers without origin operator" in {
        val count = 10
        val operator = OperatorGen.next
        val numbers = numbersWithOperators(count, operator, withOriginOperator = false)
        addNumbers(numbers)
        val expected = allNumbers().map { n =>
          val originOperator = originOperatorGen(n.operator).next
          n.copy(originOperator = Some(originOperator))
        }

        val numbersGroupedByOrigin = expected
          .groupBy(_.originOperator.get)
          .view
          .mapValues(_.map(_.number))
          .toMap
        numbersGroupedByOrigin.foreach {
          case (originOperator, numbers) =>
            dao.setOriginOperator(numbers, originOperator).futureValue
        }

        val actual = allNumbers()
        actual should contain theSameElementsAs expected
      }
    }
    "count without origin operator" in {
      val countWithOriginOperator = 10
      val countWithoutOriginOperator = 10
      val operator = OperatorGen.next
      val numbersWithOriginOperator = numbersWithOperators(countWithOriginOperator, operator, withOriginOperator = true)
      addNumbers(numbersWithOriginOperator)
      val numbersWithoutOriginOperator =
        numbersWithOperators(countWithoutOriginOperator, operator, withOriginOperator = false)
      addNumbers(numbersWithoutOriginOperator)

      val actual = dao.countWithoutOriginOperator().futureValue
      actual shouldBe countWithoutOriginOperator
    }
  }

}
