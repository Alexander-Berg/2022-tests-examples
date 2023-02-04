package ru.yandex.vertis.feedprocessor.autoru.parser

import org.apache.commons.io.IOUtils
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.{ExternalOffer, ExternalOfferError, TaskContext}

import java.nio.charset.StandardCharsets.UTF_8
import scala.annotation.nowarn

/**
  * @author pnaydenov
  */
@nowarn("msg=deprecated")
class FlatXmlIteratorSpec extends WordSpecBase {
  "FlatXmlIterator" should {
    "throw error if root tags not complete" in {
      val feed =
        """
          |<foo>
          |  <bar>
          |    <car><vin>123</vin></car>
          |  </bar>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIterator(feed) {
        override protected val rootTagNames: Array[String] = Array("foo", "bar", "baz")
      }
      val ex = intercept[RuntimeException] {
        parser.toList
      }

      ex.getMessage should include("empty file or wrong format")
    }

    "throw error if root tags in wrong order" in {
      val feed =
        """
          |<foo>
          |  <baz>
          |    <bar>
          |      <car><vin>123</vin></car>
          |    </bar>
          |  </baz>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIterator(feed) {
        override protected val rootTagNames: Array[String] = Array("foo", "bar", "baz")
      }
      val ex = intercept[RuntimeException] {
        parser.toList
      }

      ex.getMessage should include("empty file or wrong format")
    }

    "throw error in case of unexpected root tag" in {
      val feed =
        """
          |<foo>
          |  <bar>
          |    <kaz>
          |      <baz>
          |        <car><vin>123</vin></car>
          |      </baz>
          |    </kaz>
          |  </bar>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIterator(feed) {
        override protected val rootTagNames: Array[String] = Array("foo", "bar", "baz")
      }
      val ex = intercept[RuntimeException] {
        parser.toList
      }

      ex.getMessage should include("empty file or wrong format")
    }

    "correctly parse multiple root tags" in {
      val feed =
        """
          |<foo>
          |  <bar>
          |    <baz>
          |      <car><vin>123</vin></car>
          |      <car><vin>321</vin></car>
          |    </baz>
          |  </bar>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIterator(feed) {
        override protected val rootTagNames: Array[String] = Array("foo", "bar", "baz")
      }
      val result = parser.toList
      result should have size (2)
      result.head should be('right)
      result.last should be('right)
    }

    "correctly parse single root tag" in {
      val feed =
        """
          |<foo>
          |  <car><vin>123</vin></car>
          |  <car><vin>321</vin></car>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIterator(feed) {
        override protected val rootTagNames: Array[String] = Array("foo")
      }
      val result = parser.toList
      result should have size (2)
      result.head should be('right)
      result.last should be('right)
    }

    "return error offer in case of exception in resultFromBuilder" in {
      val feed =
        """
          |<foo>
          |  <car><vin>1</vin></car>
          |  <car><vin>wrong_result_build</vin></car>
          |  <car><vin>3</vin></car>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIteratorWithErrorChecks(feed) {
        override protected val rootTagNames: Array[String] = Array("foo")
      }
      val result = parser.toList
      result should have size (3)
      result(0).right.get shouldEqual TestExternalOffer(0, "1")
      result(1).left.get.error.getMessage should include("Wrong VIN, result build")
      result(2).right.get shouldEqual TestExternalOffer(2, "3")
    }

    "return error offer in case of exception in checkResult" in {
      val feed =
        """
          |<foo>
          |  <car><vin>0</vin></car>
          |  <car><vin>1</vin></car>
          |  <car><vin>2</vin></car>
          |  <car><vin>3</vin></car>
          |  <car><vin>4</vin></car>
          |  <car><vin>5</vin></car>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIteratorWithErrorChecks(feed) {
        override protected val rootTagNames: Array[String] = Array("foo")
      }
      val result = parser.toList
      result should have size (6)
      result(0).right.get shouldEqual TestExternalOffer(0, "0")
      result(1).right.get shouldEqual TestExternalOffer(1, "1")
      result(2).right.get shouldEqual TestExternalOffer(2, "2")
      result(3).left.get.error.getMessage should include("Don't allow offer pos. 3")
      result(3).left.get.position shouldEqual 3
      result(4).left.get.error.getMessage should include("Can't check offer pos. 4")
      result(4).left.get.position shouldEqual 4
      result(5).right.get shouldEqual TestExternalOffer(5, "5")
    }

    "return error offer in case of exception in applyField" in {
      val feed =
        """
          |<foo>
          |  <car><vin>1</vin></car>
          |  <car><vin>exception_in_apply</vin></car>
          |  <car><vin>3</vin></car>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIteratorWithErrorChecks(feed) {
        override protected val rootTagNames: Array[String] = Array("foo")
      }
      val result = parser.toList
      result should have size (3)
      result(0).right.get shouldEqual TestExternalOffer(0, "1")
      result(1).left.get.error.getMessage should include("Wrong VIN, tag apply")
      result(2).right.get shouldEqual TestExternalOffer(2, "3")
    }

    "return error offer in case of exception in fieldEnd" in {
      val feed =
        """
          |<foo>
          |  <car><vin>1</vin></car>
          |  <car><vin>exception_in_tag_end</vin></car>
          |  <car><vin>3</vin></car>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIteratorWithErrorChecks(feed) {
        override protected val rootTagNames: Array[String] = Array("foo")
      }
      val result = parser.toList
      result should have size (3)
      result(0).right.get shouldEqual TestExternalOffer(0, "1")
      result(1).left.get.error.getMessage should include("Wrong VIN, tag end")
      result(2).right.get shouldEqual TestExternalOffer(2, "3")
    }

    "allow entry without inner tags" in {
      val feed =
        """
          |<foo>
          |  <car><vin>1</vin></car>
          |  <car></car>
          |  <car><vin>3</vin></car>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIteratorWithErrorChecks(feed) {
        override protected val rootTagNames: Array[String] = Array("foo")
      }
      val result = parser.toList
      result should have size (3)
      result(0).right.get shouldEqual TestExternalOffer(0, "1")
      result(1).right.get shouldEqual TestExternalOffer(1, "")
      result(2).right.get shouldEqual TestExternalOffer(2, "3")
    }

    "can repeat hasNext method without side effect" in {
      val feed =
        """
          |<foo>
          |  <car><vin>1</vin></car>
          |  <car><vin>2</vin></car>
          |  <car><vin>3</vin></car>
          |</foo>
        """.stripMargin
      val parser = new TestFlatXmlIteratorWithErrorChecks(feed) {
        override protected val rootTagNames: Array[String] = Array("foo")
      }
      parser.hasNext shouldBe true
      parser.hasNext shouldBe true
      parser.next.right.get shouldEqual TestExternalOffer(0, "1")
      parser.hasNext shouldBe true
      parser.hasNext shouldBe true
      parser.hasNext shouldBe true
      parser.next.right.get shouldEqual TestExternalOffer(1, "2")
      parser.hasNext shouldBe true
      parser.next.right.get shouldEqual TestExternalOffer(2, "3")
      parser.hasNext shouldBe false
      parser.hasNext shouldBe false
      intercept[NoSuchElementException] {
        parser.next()
      }
    }
  }

  private class TestExternalOfferBuilder extends FlatXmlIterator.Builder {
    override def applyField(field: String, value: String): Unit = {}

    override def fieldEnd(field: String): Unit = {}

    override def clear(): Unit = {}
  }

  abstract private class TestFlatXmlIterator(feed: String)
    extends FlatXmlIterator[TestExternalOffer, TestExternalOfferBuilder](IOUtils.toInputStream(feed, UTF_8)) {
    override protected val carTagName: String = "car"

    override protected def createOfferBuilder(): TestExternalOfferBuilder = new TestExternalOfferBuilder

    override protected def resultFromBuilder(builder: TestExternalOfferBuilder, position: Int): TestExternalOffer =
      TestExternalOffer(position, null)

    override protected def checkResult(result: TestExternalOffer): Either[ExternalOfferError, TestExternalOffer] =
      Right(result)

    override protected def builderToOfferError(
        exception: Throwable,
        builder: TestExternalOfferBuilder,
        position: Int): ExternalOfferError =
      TestExternalOfferError(exception, position, null)

    override protected def offerToOfferError(exception: Throwable, offer: TestExternalOffer): ExternalOfferError =
      TestExternalOfferError(exception, offer.position, null)
  }

  abstract private class TestFlatXmlIteratorWithErrorChecks(feed: String) extends TestFlatXmlIterator(feed) {
    override protected def createOfferBuilder(): TestExternalOfferBuilder = new TestExternalOfferBuilderWithErrorChecks

    override protected def resultFromBuilder(builder: TestExternalOfferBuilder, position: Int): TestExternalOffer = {
      val vin = builder.asInstanceOf[TestExternalOfferBuilderWithErrorChecks].vin
      if (vin == "wrong_result_build") {
        throw new RuntimeException("Wrong VIN, result build")
      }
      TestExternalOffer(position, vin)
    }

    override protected def checkResult(result: TestExternalOffer): Either[ExternalOfferError, TestExternalOffer] = {
      if (result.position == 3) {
        throw new RuntimeException("Don't allow offer pos. 3")
      } else if (result.position == 4) {
        Left(TestExternalOfferError(new RuntimeException("Can't check offer pos. 4"), result.position, null))
      } else {
        Right(result)
      }
    }
  }

  private class TestExternalOfferBuilderWithErrorChecks extends TestExternalOfferBuilder {
    var vin: String = _

    override def applyField(field: String, value: String): Unit = {
      if (field == "vin" && value == "exception_in_apply") {
        throw new RuntimeException("Wrong VIN, tag apply")
      }
      vin = value
    }

    override def fieldEnd(field: String): Unit = {
      if (field == "vin" && vin == "exception_in_tag_end") {
        throw new RuntimeException("Wrong VIN, tag end")
      }
    }

    override def clear(): Unit = {
      vin = ""
    }
  }

  private case class TestExternalOfferError(error: Throwable, position: Int, taskContext: TaskContext)
    extends ExternalOfferError

  private case class TestExternalOffer(position: Int, vin: String) extends ExternalOffer {
    override def taskContext: TaskContext = ???

    override def toString: String = s"[${getClass.getSimpleName} position=$position, vin=$vin]"
  }

}
