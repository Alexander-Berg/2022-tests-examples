package ru.yandex.vertis.feedprocessor.autoru.scheduler.parser

import org.apache.commons.io.IOUtils
import org.mockito.Mockito
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.ModelUtils._
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.AutoruExternalOffer
import ru.yandex.vertis.mockito.MockitoSupport

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.charset.Charset

/**
  * @author pnaydenov
  */
class AutoruXmlIteratorSpec extends WordSpecBase with MockitoSupport {
  implicit val taskContext = model.TaskContext(newTasksGen.next)

  "AutoruXmlIterator" should {
    "emit only first from offers with identical VIN-s" in {
      val feed = IOUtils.toInputStream(
        """
          |<data>
          |  <cars>
          |    <car>111</car>
          |    <car>111</car>
          |    <car>111</car>
          |    <car>222</car>
          |  </cars>
          |</data>""".stripMargin,
        UTF_8
      )
      val offer1 = Mockito.mock(classOf[AutoruExternalOffer])
      val offer2 = Mockito.mock(classOf[AutoruExternalOffer])
      val offerBuilder = mock[AutoruExternalOfferBuilder[AutoruExternalOffer]]

      Mockito.doNothing().when(offerBuilder).clear()
      when(offerBuilder.result(?, ?, ?)(?)).thenReturn(offer1, offer1, offer1, offer2)
      when(offerBuilder.vin).thenReturn("vin-111")
      when(offerBuilder.uniqueId).thenReturn("id-111")

      when(offer1.vin).thenReturn(Some("111"))
      when(offer1.uniqueId).thenReturn(Some("u11"), Some("u12"), Some("u13"))
      when(offer2.vin).thenReturn(Some("222"))
      when(offer2.uniqueId).thenReturn(Some("u22"))

      val parser = new AutoruXmlIterator[AutoruExternalOffer](feed) {
        override protected def createOfferBuilder() = offerBuilder
      }
      val results = parser.toList
      val erros = results.collect { case Left(error) => error.error.getMessage }
      val offers = results.collect { case Right(o) => o }

      offers should have size (2)
      erros should have size (2)

      assert(erros.forall(_ contains "Повторяющийся уникальный идентификатор"))
    }

    "ignore unique_id doubles in the presence of correct VIN-s" in {
      val feed = IOUtils.toInputStream(
        """
                                         |<data>
                                         |  <cars>
                                         |    <car>111</car>
                                         |    <car>222</car>
                                         |  </cars>
                                         |</data>""".stripMargin,
        UTF_8
      )
      val offer1 = Mockito.mock(classOf[AutoruExternalOffer])
      val offer2 = Mockito.mock(classOf[AutoruExternalOffer])
      val offerBuilder = mock[AutoruExternalOfferBuilder[AutoruExternalOffer]]

      Mockito.doNothing().when(offerBuilder).clear()
      when(offerBuilder.result(?, ?, ?)(?)).thenReturn(offer1, offer2)
      when(offerBuilder.vin).thenReturn("vin-111")
      when(offerBuilder.uniqueId).thenReturn("id-111")

      when(offer1.vin).thenReturn(Some("111"))
      when(offer1.uniqueId).thenReturn(Some("u11"))
      when(offer2.vin).thenReturn(Some("222"))
      when(offer2.uniqueId).thenReturn(Some("u11"))

      val parser = new AutoruXmlIterator[AutoruExternalOffer](feed) {
        override protected def createOfferBuilder() = offerBuilder
      }
      val results = parser.toList
      val erros = results.collect { case Left(error) => error.error.getMessage }
      val offers = results.collect { case Right(o) => o }

      offers should have size (2)
      erros shouldBe empty
    }

    "emit only first from offers with identical unique_ids in case of VIN's abcense" in {
      val feed = IOUtils.toInputStream(
        """
                                         |<data>
                                         |  <cars>
                                         |    <car>111</car>
                                         |    <car>111</car>
                                         |    <car>111</car>
                                         |    <car>222</car>
                                         |  </cars>
                                         |</data>""".stripMargin,
        UTF_8
      )
      val offer1 = Mockito.mock(classOf[AutoruExternalOffer])
      val offer2 = Mockito.mock(classOf[AutoruExternalOffer])
      val offerBuilder = mock[AutoruExternalOfferBuilder[AutoruExternalOffer]]

      Mockito.doNothing().when(offerBuilder).clear()
      when(offerBuilder.result(?, ?, ?)(?)).thenReturn(offer1, offer1, offer1, offer2)
      when(offerBuilder.vin).thenReturn("vin-111")
      when(offerBuilder.uniqueId).thenReturn("id-111")

      when(offer1.vin).thenReturn(None, None, None)
      when(offer1.uniqueId).thenReturn(Some("u11"))
      when(offer2.vin).thenReturn(None)
      when(offer2.uniqueId).thenReturn(Some("u22"))

      val parser = new AutoruXmlIterator[AutoruExternalOffer](feed) {
        override protected def createOfferBuilder() = offerBuilder
      }
      val results = parser.toList
      val erros = results.collect { case Left(error) => error.error.getMessage }
      val offers = results.collect { case Right(o) => o }

      offers should have size (2)
      erros should have size (2)

      assert(erros.forall(_ contains "Повторяющийся уникальный идентификатор"))
    }

    "not allow wrong root tag name" in {
      val feed = IOUtils.toInputStream(
        s"""
                                         |<wrongtag>
                                         |  <cars>
                                         |    <car>
                                         |      <foo>BAR</foo>
                                         |    </car>
                                         |  </cars>
                                         |</wrongtag>""".stripMargin,
        UTF_8
      )
      val offerBuilder = mock[AutoruExternalOfferBuilder[AutoruExternalOffer]]

      val parser = new AutoruXmlIterator[AutoruExternalOffer](feed) {
        override protected def createOfferBuilder() = offerBuilder
      }
      val ex = intercept[RuntimeException] {
        parser.toList
      }
      ex.getMessage should include("empty file or wrong format")
    }

    "trim front and trailing spaces" in {
      val feed = IOUtils.toInputStream(
        """
                                         |    <?xml version="1.0" encoding="UTF-8"?>
                                         |<data>
                                         |      <cars>
                                         |            <car>
                                         |                  <unique_id>JTEHT05J502095974</unique_id>
                                         |                  <description>
                                         |Плюсы машины:
                                         |    * недорогая
                                         |    * неприхотливая
                                         |        1. в том числе
                                         |        2. в том числе 2
                                         |                  </description>
                                         |            </car>
                                         |      </cars>
                                         |</data>     """.stripMargin,
        Charset.forName("UTF-8")
      )
      val offer1 = Mockito.mock(classOf[AutoruExternalOffer])
      val offerBuilder = mock[AutoruExternalOfferBuilder[AutoruExternalOffer]]

      Mockito.doNothing().when(offerBuilder).clear()
      Mockito.doNothing().when(offerBuilder).applyField(?, ?)
      Mockito.doNothing().when(offerBuilder).fieldEnd(?)

      when(offerBuilder.vin).thenReturn(null)
      when(offerBuilder.uniqueId).thenReturn("JTEHT05J502095974")
      when(offer1.vin).thenReturn(None)
      when(offer1.uniqueId).thenReturn(Some("JTEHT05J502095974"))

      when(offerBuilder.result(?, ?, ?)(?)).thenReturn(offer1)

      val parser = new AutoruXmlIterator[AutoruExternalOffer](feed) {
        override protected def createOfferBuilder() = offerBuilder
      }
      val results = parser.toList
      val errors = results.collect { case Left(error) => error.error.getMessage }
      val offers = results.collect { case Right(o) => o }

      offers should have size 1
      errors shouldBe empty
      val order = Mockito.inOrder(offerBuilder)
      order.verify(offerBuilder).clear() // should clear builder BEFORE any other invocation
      order.verify(offerBuilder).applyField("unique_id", "JTEHT05J502095974")
      order.verify(offerBuilder).fieldEnd("unique_id")
      order
        .verify(offerBuilder)
        .applyField(
          "description",
          """Плюсы машины:
                                                             |    * недорогая
                                                             |    * неприхотливая
                                                             |        1. в том числе
                                                             |        2. в том числе 2""".stripMargin
        )
      order.verify(offerBuilder).fieldEnd("description")
      order.verify(offerBuilder).result(taskContext.task.category, taskContext.task.section, 0)
    }

    "fail on empty result" in {
      val feed = IOUtils.toInputStream(
        """
                                         |<?xml version="1.0" encoding="UTF-8"?>
                                         |<data></data>""".stripMargin,
        UTF_8
      )
      val offerBuilder = mock[AutoruExternalOfferBuilder[AutoruExternalOffer]]

      Mockito.doNothing().when(offerBuilder).clear()
      when(offerBuilder.vin).thenReturn(null)
      when(offerBuilder.uniqueId).thenReturn(null)

      val parser = new AutoruXmlIterator[AutoruExternalOffer](feed) {
        override protected def createOfferBuilder() = offerBuilder
      }
      val ex = intercept[RuntimeException] {
        parser.toList
      }
      ex.getMessage should include("empty file or wrong format")
    }

    "return empty result" in {
      val feed = IOUtils.toInputStream(
        """
                                         |<?xml version="1.0" encoding="UTF-8"?>
                                         |<data>
                                         |  <cars>
                                         |  </cars>
                                         |</data>""".stripMargin,
        UTF_8
      )
      val offerBuilder = mock[AutoruExternalOfferBuilder[AutoruExternalOffer]]

      Mockito.doNothing().when(offerBuilder).clear()
      when(offerBuilder.vin).thenReturn(null)
      when(offerBuilder.uniqueId).thenReturn(null)

      val parser = new AutoruXmlIterator[AutoruExternalOffer](feed) {
        override protected def createOfferBuilder() = offerBuilder
      }
      parser.toList shouldBe empty
    }
  }
}
