package ru.yandex.vertis.telepony.properties

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.{Operators, Phone}
import ru.yandex.vertis.telepony.properties.format.{IntFormat, OperatorSetFormat, PhoneSetFormat}

/**
  * @author neron
  */
class PropertySerDeSpec extends SpecBase with ScalaCheckDrivenPropertyChecks {

  "PropertySerDe" should {
    "serialize OperatorSetFormat" in {
      OperatorSetFormat
        .serialize(Set(Operators.Mtt, Operators.Mts)) shouldEqual "Mtt,Mts"
    }
    "deserialize to OperatorSetFormat" in {
      OperatorSetFormat
        .deserialize(" Mtt, Mts") shouldEqual Set(Operators.Mtt, Operators.Mts)
    }
    "ser de OperatorSetFormat" in {
      Operators.values.subsets().foreach { operatorSet =>
        val str = OperatorSetFormat.serialize(operatorSet)
        val model = OperatorSetFormat.deserialize(str)
        operatorSet shouldEqual model
      }
    }

    "ser de Int" in {
      val randomInt = Gen.choose(0, Integer.MAX_VALUE).next
      val str = IntFormat.serialize(randomInt)
      val model = IntFormat.deserialize(str)
      randomInt shouldEqual model
    }

    "serialize PhoneSetFormat" in {
      PhoneSetFormat
        .serialize(Set(Phone("+79123334422"), Phone("+79153334422"))) shouldEqual "+79123334422,+79153334422"
    }
    "deserialize to PhoneSetFormat" in {
      val expectedSet = Set(Phone("+79123334422"), Phone("+79153334422"))
      PhoneSetFormat
        .deserialize(" +79123334422, +79153334422 ") shouldEqual expectedSet
    }
    "ser de PhoneSetFormat" in {
      val set = Set(Phone("+79123334422"), Phone("+79153334422"))
      PhoneSetFormat.deserialize(PhoneSetFormat.serialize(set)) shouldEqual set
    }
  }

}
