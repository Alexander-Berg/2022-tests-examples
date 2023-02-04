package ru.auto.cabinet.service

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, Uri}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.exceptions.TestFailedException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Minute, Span}
import org.scalatest.{BeforeAndAfterAll, Ignore}
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.{Offer, OfferStatus}
import ru.auto.cabinet.TestActorSystem
import ru.auto.cabinet.model.offer.OfferTag
import ru.auto.cabinet.model.offer.VosOfferCategories._
import ru.auto.cabinet.service.instr.EmptyInstr
import ru.auto.cabinet.service.vos.{
  HttpVosClient,
  VosBadRequestException,
  VosConfig,
  VosException
}
import ru.auto.cabinet.trace.Context

import java.time.OffsetDateTime
import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

// check AUTORUOFFICE-4246 for reasons to ignore this test
@Ignore
class HttpVosClientSpec
    extends AnyFlatSpec
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll
    with TestActorSystem {

  implicit private val instr = new EmptyInstr("test")
  implicit private val rc = Context.unknown

  System.setProperty("config.resource", "test.conf")

  private val vosConfig =
    VosConfig(
      ConfigFactory.load().getString("cabinet.autoru.office.vos.autoru.uri"),
      rps = 10)
  private val client = new HttpVosClient(vosConfig)

  private val OfferId = "1054870438-560ec"
  private val ClientId = 2316
  private val BadOfferId = "1-a"
  private val BadClientId = -1L
  private val OfferIdWithUndefinedHash = "1054870438-undefined"
  private val Tag1 = "test-tag1"
  private val Tag2 = "test-tag2"
  private val Tag3 = "test-tag3"

  implicit override val patienceConfig = PatienceConfig(Span(1, Minute))

  "HttpVosClient" should "get offer without test tags" in {
    client.getOffer(OfferId).futureValue match {
      case Some(offer) =>
        val tags = offer.getTagsList
        tags should not contain Tag1
        tags should not contain Tag2
      case None =>
        fail(s"Offer $OfferId not found")
    }
  }

  it should "not exclude offer without test tags" in {
    val offers = client
      .getOffers(ClientId, status = None, excludeTags = Seq(Tag1))
      .futureValue
    offers.exists(_.getId == OfferId) shouldBe true
  }

  //noinspection ScalaUnnecessaryParentheses
  it should "put tags successfully" in {
    client.putTag(OfferId, Tag1).futureValue shouldBe (())
    client.putTag(OfferId, Tag2).futureValue shouldBe (())
    client.getOffer(OfferId).futureValue match {
      case Some(offer) =>
        val tags = offer.getTagsList
        tags should contain(Tag1)
        tags should contain(Tag2)
      case None =>
        fail(s"Offer $OfferId not found")
    }
  }

  it should "exclude offer with tag" in {
    val offersWithoutTag1 =
      client
        .getOffers(ClientId, status = None, excludeTags = Seq(Tag1))
        .futureValue
    offersWithoutTag1.exists(_.getId == OfferId) shouldBe false
    val offersWithoutTag2 =
      client
        .getOffers(ClientId, status = None, excludeTags = Seq(Tag2))
        .futureValue
    offersWithoutTag2.exists(_.getId == OfferId) shouldBe false
    val offersWithoutBothTags =
      client
        .getOffers(ClientId, status = None, excludeTags = Seq(Tag1, Tag2))
        .futureValue
    offersWithoutBothTags.exists(_.getId == OfferId) shouldBe false
  }

  it should "not exclude offer with tag if no filtering by its tags provided" in {
    val offersWithoutTag3 =
      client
        .getOffers(ClientId, status = None, excludeTags = Seq(Tag3))
        .futureValue
    offersWithoutTag3.exists(_.getId == OfferId) shouldBe true
    val offers = client.getOffers(ClientId, status = None).futureValue
    offers.exists(_.getId == OfferId) shouldBe true
  }

  it should "get zero offers for wrong client id" in {
    val offers = client.getOffers(BadClientId).futureValue
    offers.size shouldBe 0
  }

  it should "fail to get offer with bad offer id" in {
    client.getOffer(BadOfferId).futureValue.isDefined shouldBe false
  }

  it should "fail to get offer with wrong offer id format" in {
    try client.getOffer("1").futureValue
    catch {
      case e: TestFailedException =>
        e.getCause shouldBe a[VosException]
    }
  }

  it should "fail to put tag for offer with bad offer id" in {
    try client.putTag(BadOfferId, Tag1).futureValue
    catch {
      case e: TestFailedException =>
        e.getCause shouldBe a[VosException]
    }
  }

  it should "get all client offers and then offers filtered by different params" in {
    val time = OffsetDateTime.parse("2016-09-01T00:00+03:00")
    val createdAfterTime =
      (_: Offer).getAdditionalInfo.getCreationDate >= time.toInstant.toEpochMilli
    val createdBeforeTime =
      (_: Offer).getAdditionalInfo.getCreationDate <= time.toInstant.toEpochMilli
    val isActive = (_: Offer).getStatus == OfferStatus.ACTIVE
    val hasNoServices = !(_: Offer).getServicesList.asScala.exists { service =>
      !Set("all_sale_activate", "all_sale_add")(service.getService)
    }
    val offers = client.getOffers(ClientId, status = None).futureValue
    offers.size should be >= 200
    offers.exists(createdAfterTime) shouldBe true
    offers.exists(createdBeforeTime) shouldBe true
    offers.forall(isActive) shouldBe false
    val offersAfter =
      client.getOffers(ClientId, after = Some(time), status = None).futureValue
    offersAfter.size should be < offers.size
    offersAfter.forall(createdAfterTime) shouldBe true
    val offersBefore =
      client.getOffers(ClientId, before = Some(time), status = None).futureValue
    offersBefore.size should be < offers.size
    offersBefore.forall(createdBeforeTime) shouldBe true
    val activeOffers = client.getOffers(ClientId).futureValue
    activeOffers.size should be < offers.size
    activeOffers.forall(isActive) shouldBe true
    val carOffers = client.getOffers(ClientId, Cars, status = None).futureValue
    carOffers.size should be < offers.size
    carOffers.size should be >= 15
    carOffers.forall(_.getCategory == ApiOfferModel.Category.CARS) shouldBe true
    val motoOffers = client.getOffers(ClientId, Moto, status = None).futureValue
    motoOffers.size should be < offers.size
    motoOffers.size should be >= 190
    motoOffers.forall(
      _.getCategory == ApiOfferModel.Category.MOTO) shouldBe true
    val trucksOffers =
      client.getOffers(ClientId, Trucks, status = None).futureValue
    trucksOffers.size should be < offers.size
    trucksOffers.size should be >= 1
    trucksOffers.forall(
      _.getCategory == ApiOfferModel.Category.TRUCKS) shouldBe true
    val offersWithoutServices = client
      .getOffers(ClientId, withoutServices = true, status = None)
      .futureValue
    offersWithoutServices.size should be < offers.size
    offersWithoutServices.size should be > 0
    offersWithoutServices.forall(hasNoServices) shouldBe true
  }

  it should "delete tags" in {
    client.deleteTag(OfferId, Tag1).futureValue
    client.getOffer(OfferId).futureValue match {
      case Some(offer) =>
        offer.getTagsList should not contain Tag1
      case None =>
        fail(s"Offer $OfferId not found")
    }
  }

  it should "not throw error while putting tag for unexisting offer" in {
    //noinspection ScalaUnnecessaryParentheses
    client.putTag(BadOfferId, Tag1).futureValue shouldBe (())
  }

  it should "not throw error while deleting tag for unexisting offer" in {
    //noinspection ScalaUnnecessaryParentheses
    client.deleteTag(BadOfferId, Tag1).futureValue shouldBe (())
  }

  it should "throw error while deleting tag for offer with undefined hash" in {
    val result =
      Try(client.deleteTag(OfferIdWithUndefinedHash, Tag1).futureValue)
    result.failed.get.getCause shouldBe a[VosBadRequestException]
  }

  it should "get offerId for vin" in {
    val clientId = 27878
    val notExistingVin = "1"
    val existingVin = "XWBTA69V9HA028160"
    val offerId = "1097540431-22444fd4"
    client
      .getOfferIds(clientId, Set(notExistingVin, existingVin))
      .futureValue shouldBe Map(existingVin -> offerId)
  }

  override def afterAll(): Unit = {
    def deleteTag(tag: OfferTag) =
      Http()
        .singleRequest(
          HttpRequest(
            HttpMethods.DELETE,
            Uri(s"${vosConfig.uri}/api/v1/offer/all/$OfferId/tags/$tag")
          ))
        .futureValue

    deleteTag(Tag1)
    deleteTag(Tag2)
  }
}
