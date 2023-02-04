package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import com.google.protobuf.util.Timestamps
import org.mockito.Mockito.{doNothing, verify}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.passport.model.api.ApiModel
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.dao.old.proxy.OldDbWriter
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.dao.users.UserDao
import ru.yandex.vos2.services.passport.PassportClient

import scala.jdk.CollectionConverters._
import scala.util.Success
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RemovePhonesFromOfferWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val traced: Traced = Traced.empty

  private val oldDbWriter = mock[OldDbWriter]

  private val userPhone1 = "79291112233"
  private val userPhone2 = "79294445566"
  private val unknownSellerPhone = "79297778899"

  abstract private class Fixture {
    val offer: Offer

    def passportUser: ApiModel.User = {
      val offerUser = offer.getUser
      val passportUserBuilder = ApiModel.User.newBuilder

      Option(offerUser.getUserContacts.getEmail).filter(_.nonEmpty).foreach { email =>
        passportUserBuilder.addEmails(ApiModel.UserEmail.newBuilder().setEmail(email))
      }

      val phones = {
        offerUser.getUserContacts.getPhonesList.asScala.filter(_.getNumber.nonEmpty).map { phone =>

          val userPhoneBuilder = ApiModel.UserPhone
            .newBuilder()
            .setPhone(phone.getNumber)

          Option(phone.getTimestampAdded).filter(_ > 0).foreach { added =>
            userPhoneBuilder.setAdded(Timestamps.fromMillis(added))
          }

          userPhoneBuilder.build()
        }
      }
      passportUserBuilder.addAllPhones(phones.asJava)
      passportUserBuilder.build()

    }

    val featureRegistry = FeatureRegistryFactory.inMemory()

    val featuresManager = new FeaturesManager(featureRegistry)

    val userDao = mock[UserDao]
    val passportClient = mock[PassportClient]

    val worker = new RemovePhonesFromOfferWorkerYdb(
      oldDbWriter,
      passportClient,
      userDao
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }
  }
  "shouldProcess: nonPrivate" in new Fixture {
    val offer: OfferModel.Offer = createOffer(dealer = true)
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "shouldProcess: private, no unknown phones" in new Fixture {
    val offer: OfferModel.Offer = createOffer(dealer = false, sellerPhones = Seq(userPhone2))
    when(passportClient.getUser(?)(?)).thenReturn(Success(passportUser))

    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "shouldProcess: private, have unknown phones" in new Fixture {
    val offer: OfferModel.Offer = createOffer(dealer = false)
    when(passportClient.getUser(?)(?)).thenReturn(Success(passportUser))

    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  "process: cars, old db" in new Fixture {
    val offer: OfferModel.Offer = createOffer(dealer = false)
    when(passportClient.getUser(?)(?)).thenReturn(Success(passportUser))

    val result = worker.process(offer, None)
    val offer2 = result.updateOfferFunc.get(offer)
    assert(offer2.getOfferAutoru.getSeller.getPhoneCount == 1)
    assert(offer2.getOfferAutoru.getSeller.getPhone(0).getNumber == userPhone2)
  }

  "process: cars, different passport user" in new Fixture {
    val offer: OfferModel.Offer = createOffer(dealer = false)
    when(passportClient.getUser(?)(?)).thenReturn(Success(passportUser.toBuilder.clearPhones().build()))
    doNothing().when(userDao).update(?, ?)
    val result = worker.process(offer, None)
    assert(result.updateOfferFunc.nonEmpty)
    assert(result.nextCheck.nonEmpty)
  }

  "process: trucks, old db" in new Fixture {
    doNothing().when(oldDbWriter).removePhones(?, ?, ?)(?)

    val offer: OfferModel.Offer = createOffer(dealer = false, category = Category.TRUCKS)
    when(passportClient.getUser(?)(?)).thenReturn(Success(passportUser))

    val result = worker.process(offer, None)
    val offer2 = result.updateOfferFunc.get(offer)
    assert(offer2.getOfferAutoru.getSeller.getPhoneCount == 1)
    assert(offer2.getOfferAutoru.getSeller.getPhone(0).getNumber == userPhone2)
    verify(oldDbWriter).removePhones(eqq(Category.TRUCKS), eqq(offer.getOfferIRef), eqq(Seq(unknownSellerPhone)))(?)
  }

  "protobuf builder clear" in new Fixture {
    val offer: OfferModel.Offer = createOffer(dealer = false)
    when(passportClient.getUser(?)(?)).thenReturn(Success(passportUser))

    val sellerPhones = offer.getOfferAutoru.getSeller.getPhoneList.asScala.map(_.getNumber)
    println(sellerPhones)
    val userPhones = offer.getUser.getUserContacts.getPhonesList.asScala.map(_.getNumber)
    println(userPhones)
    val unknownSellerPhones: Set[String] = sellerPhones.diff(userPhones).toSet
    println(unknownSellerPhones)

    val b2 = offer.toBuilder
    b2.getOfferAutoruBuilder.getSellerBuilder.clearPhone()
    offer.getOfferAutoru.getSeller.getPhoneList.asScala.foreach(phone => {
      if (!unknownSellerPhones.contains(phone.getNumber)) {
        b2.getOfferAutoruBuilder.getSellerBuilder.addPhone(phone)
      }
    })

    val offer2 = b2.build()
    assert(offer2.getOfferAutoru.getSeller.getPhoneCount == 1)
    assert(offer2.getOfferAutoru.getSeller.getPhone(0).getNumber == userPhone2)

  }

  "phones duplication in seller" in new Fixture {

    val offer: OfferModel.Offer =
      createOffer(dealer = false, userPhones = Seq(userPhone1), sellerPhones = Seq(userPhone1, userPhone1))
    when(passportClient.getUser(?)(?)).thenReturn(Success(passportUser))

    val result = worker.process(offer, None)
    assert(result.updateOfferFunc.isEmpty)
  }

  private def createOffer(dealer: Boolean = false,
                          category: Category = Category.CARS,
                          userPhones: Seq[String] = Seq(userPhone1, userPhone2),
                          sellerPhones: Seq[String] = Seq(userPhone2, unknownSellerPhone)) = {
    val b = TestUtils.createOffer(dealer = dealer, category = category)

    b.getUserBuilder.getUserContactsBuilder.clearPhones()
    userPhones.foreach(p => b.getUserBuilder.getUserContactsBuilder.addPhonesBuilder().setNumber(p))

    b.getOfferAutoruBuilder.getSellerBuilder.clearPhone()
    sellerPhones.foreach(p => b.getOfferAutoruBuilder.getSellerBuilder.addPhoneBuilder().setNumber(p))

    val offer = b.build()
    offer
  }
}
