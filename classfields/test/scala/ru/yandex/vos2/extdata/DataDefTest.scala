package ru.yandex.vos2.extdata

import java.io.InputStream
import org.apache.commons.io.IOUtils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.extdata.core.DataType

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 2019-07-12
  */
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DataDefTest extends AnyWordSpec with Matchers {

  case class SomeData(value: String)

  object SomeData extends DataDef[SomeData] {
    override def dataType: DataType = new DataType("some", 1)

    /** parse data object from input stream */
    override def parse(is: InputStream): SomeData = SomeData(IOUtils.toString(is))
  }

  "DataDef" should {
    "work with case classes" in {
      var stringData = "abc"
      var forceUpdate: () => Unit = null
      val ede = new ExtDataEngine {
        override def readData(dataType: DataType): InputStream = IOUtils.toInputStream(stringData)
        override def subscribe(dataTypes: DataType*)(action: => Unit): Unit = forceUpdate = () => action
      }
      val data = SomeData.from(ede)
      forceUpdate should not be null
      data.value shouldBe stringData

      stringData = "abc2"
      forceUpdate()

      data.value shouldBe "abc2"
    }
  }

}
