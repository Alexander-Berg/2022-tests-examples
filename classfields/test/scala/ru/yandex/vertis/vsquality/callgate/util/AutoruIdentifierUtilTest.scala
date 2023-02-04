package ru.yandex.vertis.vsquality.callgate.util

import com.softwaremill.tagging._
import ru.yandex.vertis.vsquality.callgate.Globals.AutoruIdentifierTag
import ru.yandex.vertis.vsquality.callgate.model.AutoruCategory
import ru.yandex.vertis.vsquality.callgate.util.AutoruIdentifierUtil.RichAutoruIdentifier
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoReads._
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase

/**
  * @author mpoplavkov
  */
class AutoruIdentifierUtilTest extends SpecBase {

  val offerId = "1234567"

  "RichAutoruIdentifier" should {
    "correctly parse car" in {
      val identifier = s"CARS:$offerId".taggedWith[AutoruIdentifierTag]
      val (parsedCategory, parsedOfferId) = identifier.extractCategoryAndOfferId.toOption.get
      parsedCategory shouldBe AutoruCategory.Car
      parsedOfferId shouldBe offerId
    }

    "fail on nonexistent category" in {
      val identifier = s"NONEXISTENT:$offerId".taggedWith[AutoruIdentifierTag]
      val parsedValidated = identifier.extractCategoryAndOfferId
      val err = parsedValidated.toEither.swap.getOrElse(???).head
      err.isInstanceOf[Error.InvalidValue] shouldBe true
    }

    "fail if no category present" in {
      val identifier = offerId.taggedWith[AutoruIdentifierTag]
      val parsedValidated = identifier.extractCategoryAndOfferId
      val err = parsedValidated.toEither.swap.getOrElse(???).head
      err.isInstanceOf[Error.MissingValue] shouldBe true
    }
  }
}
