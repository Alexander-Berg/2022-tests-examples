package ru.yandex.complaints.api.directives.api.modobjs

import com.typesafe.config.{Config, ConfigFactory}
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.complaints.api.directives.api.modobjs.OpinionsDataDirectiveSpec.{instanceOpinionGen, Producer}
import ru.yandex.complaints.api.util.DomainExceptionHandler
import ru.yandex.complaints.util.ProtobufUtils
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.{InstanceOpinion, Opinion, Reason}
import ru.yandex.vertis.moderation.proto.{Model, ModelFactory}
import spray.http.StatusCodes
import spray.routing.{HttpService, Route}
import spray.testkit.ScalatestRouteTest

/**
  * Specs for [[OpinionsDataDirective]]
  *
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class OpinionsDataDirectiveSpec
  extends WordSpec
  with Matchers
  with HttpService
  with ScalatestRouteTest {

  implicit val actorRefFactory = system
  override def testConfig: Config = ConfigFactory.parseResources("spray.conf")

  import OpinionsDataDirective.instance

  val testRoute: Route =
    handleExceptions(DomainExceptionHandler.specificExceptionHandler) {
      (put & instance) { opinionsData =>
        complete(s"Ok, ${opinionsData.size}")
      }
    }

  "OpinionsDataDirective" should {
    "fail to handle get" in {
      Get("/") ~> testRoute ~> check {
        handled shouldBe false
      }
    }
    "handle empty put" in {
      Put("/") ~> testRoute ~> check {
        handled shouldBe true
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual "Ok, 0"
      }
    }
    "fail to handle one opinion" in {
      val op = instanceOpinionGen.next.toByteArray
      Put("/", op) ~> testRoute ~> check {
        handled shouldBe true
        status shouldEqual StatusCodes.BadRequest
      }
    }
    "handle one write delimited opinion" in {
      val bytes =
        ProtobufUtils.serializeMessages(instanceOpinionGen.next(1).toSeq)
      Put("/", bytes) ~> testRoute ~> check {
        status === StatusCodes.OK
        responseAs[String] shouldEqual "Ok, 1"
      }
    }
    "handle multiple write delimited opinion" in {
      val opinions = instanceOpinionGen.next(100).toSeq
      val countUniqInstanceIds = opinions.map(_.getInstanceId).toSet.size
      val bytes = ProtobufUtils.serializeMessages(opinions)
      Put("/", bytes) ~> testRoute ~> check {
        status === StatusCodes.OK
        responseAs[String] shouldEqual s"Ok, $countUniqInstanceIds"
      }
    }
    "handle one write delimited opinion with unknown type" in {
      val instanceOpinion = {
        val b = instanceOpinionGen.next.toBuilder
        val existOpinions = b.getOpinions.toBuilder
        val existOpinionEntry = b.getOpinions.getEntries(0).toBuilder
        val existOpinion = existOpinionEntry.getOpinion.toBuilder
        existOpinion.setType(Opinion.Type.UNKNOWN)
        existOpinionEntry.setOpinion(existOpinion.build())
        existOpinions.clearEntries().addEntries(existOpinionEntry.build())
        b.setOpinions(existOpinions.build()).build()
      }
      val bytes = ProtobufUtils.serializeMessages(Seq(instanceOpinion))
      Put("/", bytes) ~> testRoute ~> check {
        status === StatusCodes.OK
        responseAs[String] shouldEqual "Ok, 0"
      }
    }
    "handle data without single opinion (obsolete)" in {
      val instanceOpinion = {
        val b = instanceOpinionGen.next.toBuilder
        b.clearOpinion().build()
      }
      val bytes = ProtobufUtils.serializeMessages(Seq(instanceOpinion))
      Put("/", bytes) ~> testRoute ~> check {
        status === StatusCodes.OK
        responseAs[String] shouldEqual "Ok, 1"
      }
    }
    "fail to handle data without opinions" in {
      val instanceOpinion = {
        val b = instanceOpinionGen.next.toBuilder
        b.clearOpinions().build()
      }
      val bytes = ProtobufUtils.serializeMessages(Seq(instanceOpinion))
      Put("/", bytes) ~> testRoute ~> check {
        status === StatusCodes.BadRequest
      }
    }
    "fail to handle data with multiple entries" in {
      val instanceOpinion = {
        val b = instanceOpinionGen.next.toBuilder
        val existOpinions = b.getOpinions.toBuilder
        val existOpinionEntry = b.getOpinions.getEntries(0).toBuilder
        val existOpinion = existOpinionEntry.getOpinion.toBuilder
        existOpinion.setType(Opinion.Type.OK)
        existOpinionEntry.setOpinion(existOpinion.build())
        existOpinions.clearEntries().
          addEntries(existOpinionEntry.build()).
          addEntries(existOpinionEntry.build())
        b.setOpinions(existOpinions.build()).build()
      }
      val bytes = ProtobufUtils.serializeMessages(Seq(instanceOpinion))
      Put("/", bytes) ~> testRoute ~> check {
        status === StatusCodes.BadRequest
      }
    }
    "fail to handle data with entry without opinion" in {
      val instanceOpinion = {
        val b = instanceOpinionGen.next.toBuilder
        val existOpinions = b.getOpinions.toBuilder
        val existOpinionEntry = b.getOpinions.getEntries(0).toBuilder
        existOpinionEntry.clearOpinion()
        existOpinions.clearEntries().
          addEntries(existOpinionEntry.build())
        b.setOpinions(existOpinions.build()).build()
      }
      val bytes = ProtobufUtils.serializeMessages(Seq(instanceOpinion))
      Put("/", bytes) ~> testRoute ~> check {
        status === StatusCodes.BadRequest
      }
    }
    "fail to handle data without entry opinions type" in {
      val instanceOpinion = {
        val b = instanceOpinionGen.next.toBuilder
        val existOpinions = b.getOpinions.toBuilder
        val existOpinionEntry = b.getOpinions.getEntries(0).toBuilder
        val existOpinion = existOpinionEntry.getOpinion.toBuilder
        existOpinion.clearType()
        existOpinionEntry.setOpinion(existOpinion.build())
        existOpinions.clearEntries().
          addEntries(existOpinionEntry.build())
        b.setOpinions(existOpinions.build()).build()
      }
      val bytes = ProtobufUtils.serializeMessages(Seq(instanceOpinion))
      Put("/", bytes) ~> testRoute ~> check {
        status === StatusCodes.BadRequest
      }
    }
  }

}

object OpinionsDataDirectiveSpec {

  import scala.collection.JavaConverters._

  def opinionGen: Gen[Opinion] = for {
    t <- Gen.oneOf(Opinion.Type.values).suchThat(_ != Opinion.Type.UNKNOWN)
    reasonGen = Gen.oneOf(Reason.values)
    automaticSource = ModelFactory.newSourceBuilder().
      setAutomaticSource(ModelFactory.newAutomaticSourceBuilder().setApplication(Application.COMPLAINTS))
    manualSource = ModelFactory.newSourceBuilder().
      setManualSource(ModelFactory.newManualSourceBuilder().setUserId("user1"))
    sourceGen = Gen.oneOf(automaticSource, manualSource)
    numReasons <- Gen.choose(0, 2)
    reasons <- Gen.listOfN(numReasons, reasonGen)
    sources <- Gen.listOfN(numReasons, Gen.option(sourceGen))
    reasonsAndSources = reasons.zip(sources).map { p =>
      val b = ModelFactory.newReasonAndSource().setReason(p._1)
      p._2.foreach(b.setSource)
      b.build()
    }
    b = ModelFactory.newOpinionBuilder().
        setType(t)
    _ = t match {
      case Opinion.Type.FAILED =>
        b.addAllReasons(reasons.asJava).
          addAllReasonAndSource(reasonsAndSources.asJava)
      case _ => ()
    }
  } yield b.build()

  def instanceOpinionGen: Gen[InstanceOpinion] = for {
    objectId <- Gen.alphaStr
    partner <- Gen.posNum[Int]
    user = ModelFactory.newUserBuilder().setPartnerUser(partner.toString)
    externalId = ModelFactory.newExternalIdBuilder().
      setObjectId(objectId).
      setUser(user)
    instanceId <- Gen.alphaStr
    opinion <- opinionGen
    domain = ModelFactory.newDomainBuilder().
      setRealty(Model.Domain.Realty.DEFAULT_REALTY)
    opinions = ModelFactory.newOpinionsBuilder().
      addAllEntries(Iterable(ModelFactory.newOpinionsEntryBuilder.
        setDomain(domain).
        setOpinion(opinion).build()).asJava)
    b = ModelFactory.newInstanceOpinion().
      setExternalId(externalId).
      setInstanceId(instanceId).
      setOpinion(opinion).
      setOpinions(opinions)
  } yield b.build()

  implicit class Producer[T](gen: Gen[T]) {

    def values = Iterator.continually(gen.sample).flatten
    def next(n: Int): Iterable[T] = values.take(n).toIterable
    def nextIterator(n: Int): Iterator[T] = values.take(n)
    def next: T = next(1).head
  }


}