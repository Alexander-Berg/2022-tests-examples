package ru.yandex.vos2.autoru.model

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.AutoruModel.AutoruOffer.SellerType
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruModelRichOffer
import ru.yandex.vos2.services.phone.ToPhoneDelivery
import ru.yandex.vos2.autoru.services.moderation.ProtectedResellerDecider
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Created by andrey on 12/22/17.
  */
@RunWith(classOf[JUnitRunner])
class AutoruModelRichOfferTest extends AnyFunSuite with MockitoSupport {
  private val protectedResellerDecider = mock[ProtectedResellerDecider]
  when(protectedResellerDecider.isProtectedReseller(?)).thenReturn(true)

  private val banStrategy = new BanStrategy(protectedResellerDecider)

  test("toSmsParams") {
    val offerBuilder = TestUtils.createOffer()

    assert(offerBuilder.toSmsParams.isEmpty)
    assert(offerBuilder.toUnconfirmedSmsParams.isEmpty)

    val phone1: String = "79261112233"
    val phone2: String = "79064445566"
    offerBuilder.getOfferAutoruBuilder.setLastPhone(phone1)
    assert(offerBuilder.toSmsParams.isEmpty)
    assert(offerBuilder.toUnconfirmedSmsParams.nonEmpty)

    offerBuilder.getUserBuilder.getUserContactsBuilder.addPhonesBuilder().setNumber(phone2)
    assert(offerBuilder.toSmsParams.contains(ToPhoneDelivery(phone2)))

    offerBuilder.getOfferAutoruBuilder.getSellerBuilder.addPhoneBuilder().setNumber(phone1)
    assert(offerBuilder.toSmsParams.contains(ToPhoneDelivery(phone1)))
  }

  test("isBanned for private") {
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.setSellerType(SellerType.PRIVATE)
    offerBuilder.clearFlag()
    assert(!offerBuilder.isAutoruBanned)
    assert(!offerBuilder.isOrWasBanned(banStrategy))
    offerBuilder.addFlag(OfferFlag.OF_BANNED)
    assert(offerBuilder.isAutoruBanned)
    assert(offerBuilder.isOrWasBanned(banStrategy))
    offerBuilder.clearFlag()
    offerBuilder.addReasonsBan("BLOCKED_IP")
    offerBuilder.addReasonsBan("SOLD")
    assert(!offerBuilder.isAutoruBanned)
    assert(offerBuilder.isOrWasBanned(banStrategy))
    offerBuilder.clearFlag()
    offerBuilder.clearReasonsBan()
    offerBuilder.addReasonsBan("NO_ANSWER")
    offerBuilder.addReasonsBan("SOLD")
    offerBuilder.addReasonsBan("USER_RESELLER")
    assert(!offerBuilder.isAutoruBanned)
    assert(!offerBuilder.isOrWasBanned(banStrategy))
    offerBuilder.clearReasonsBan()
    offerBuilder.addReasonsBan("NO_SHOW")
    offerBuilder.addReasonsBan("DO_NOT_EXIST")
    assert(!offerBuilder.isAutoruBanned)
    assert(offerBuilder.isOrWasBanned(banStrategy))
  }

  test("isBanned for commercial") {
    val offerBuilder = TestUtils.createOffer()
    offerBuilder.getOfferAutoruBuilder.setSellerType(SellerType.COMMERCIAL)
    offerBuilder
      .setUserRef("ac_123")
      .clearFlag()
    assert(!offerBuilder.isAutoruBanned)
    assert(!offerBuilder.isOrWasBanned(banStrategy))
    offerBuilder.addFlag(OfferFlag.OF_BANNED)
    assert(offerBuilder.isAutoruBanned)
    assert(offerBuilder.isOrWasBanned(banStrategy))
    offerBuilder.clearFlag()
    offerBuilder.addReasonsBan("BLOCKED_IP")
    offerBuilder.addReasonsBan("NO_ANSWER")
    assert(!offerBuilder.isAutoruBanned)
    assert(offerBuilder.isOrWasBanned(banStrategy))
    offerBuilder.clearFlag()
    offerBuilder.clearReasonsBan()
    offerBuilder.addReasonsBan("NO_ANSWER")
    assert(!offerBuilder.isAutoruBanned)
    assert(offerBuilder.isOrWasBanned(banStrategy))
    offerBuilder.clearReasonsBan()
    offerBuilder.addReasonsBan("USER_RESELLER")
    offerBuilder.addReasonsBan("SOLD")
    assert(!offerBuilder.isAutoruBanned)
    assert(offerBuilder.isOrWasBanned(banStrategy))
  }
}
