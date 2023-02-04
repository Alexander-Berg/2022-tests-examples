package ru.yandex.realty.flatgeoindex

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.geometry.Geometry
import ru.yandex.realty.model.message.ExtDataSchema.{FlatGeoIndexKey, FlatGeoIndexValue}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

@RunWith(classOf[JUnitRunner])
class FlatGeoIndexProtoConverterSpec extends SpecBase with FlatGeoIndexSpecComponents {
  import FlatGeoIndexProtoConverter._

  "PondIndex" should {
    "serialize and deserialize correctly" in {
      val newIndex = serializeAndDeserialize(pondIndex)
      isIndexesEqual(pondIndex, newIndex) shouldBe true
    }
  }

  "ParkIndex" should {
    "serialize and deserialize correctly" in {
      val newIndex = serializeAndDeserialize(parkIndex)
      isIndexesEqual(parkIndex, newIndex) shouldBe true
    }
  }

  private def serializeAndDeserialize[K, V](
    index: FlatGeoIndex[K, V]
  )(
    implicit toGeometry: V => Geometry,
    serializeKey: K => FlatGeoIndexKey,
    serializeValue: V => FlatGeoIndexValue,
    deserializeKey: FlatGeoIndexKey => K,
    deserializeValue: FlatGeoIndexValue => V
  ): FlatGeoIndex[K, V] = {
    val converter = new FlatGeoIndexProtoConverter[K, V]()
    val messages = converter.serialize(index)
    val os = new ByteArrayOutputStream()
    messages.foreach(_.writeDelimitedTo(os))
    os.close()
    val is = new ByteArrayInputStream(os.toByteArray)
    val newIndex = converter.deserialize(is)
    is.close()
    newIndex
  }

  private def isIndexesEqual[K, V](index1: FlatGeoIndex[K, V], index2: FlatGeoIndex[K, V]): Boolean = {
    index1.elements == index2.elements &&
    index1.index == index2.index &&
    index1.stepLongitude == index2.stepLongitude &&
    index1.stepLatitude == index2.stepLatitude &&
    index1.rows == index2.rows &&
    index1.columns == index2.columns
  }
}
