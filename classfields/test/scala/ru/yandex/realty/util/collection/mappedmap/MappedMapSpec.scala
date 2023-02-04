package ru.yandex.realty.util.collection.mappedmap

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}

/**
  * Created by asoboll on 03.04.17.
  */
@RunWith(classOf[JUnitRunner])
class MappedMapSpec extends WordSpecLike with Matchers {
  implicit val serializeInt = (x: Int) => ByteBuffer.allocate(Integer.BYTES).putInt(x).array()
  implicit val deserializeInt = (bb: ByteBuffer) => bb.getInt
  implicit val serializeString = (s: String) => s.getBytes
  implicit val deserializeString = (bb: ByteBuffer) => new String(ByteString.copyFrom(bb).toByteArray)

  "MappedSeq" should {
    "write and read int sequence" in {
      val seq = Seq(1, 2, 3, 5, 8)
      val baos = new ByteArrayOutputStream()

      val intWriter = new MappedSeqWriter[Int](baos)
      intWriter.write(seq)
      val length = intWriter.finish()
      val bytes = baos.toByteArray
      bytes.length shouldBe length

      (the[IllegalStateException] thrownBy {
        intWriter.write(566)
      } should have).message("Already finished writing")

      val mappedSeq = MappedSeq.from[Int](ByteBuffer.wrap(bytes))
      mappedSeq.isFixedSizeRecords shouldBe true
      mappedSeq.size shouldBe seq.size
      mappedSeq shouldEqual seq
    }

    "write and read string sequence" in {
      val seq = "Curiosity Killed The Cat".split(' ').toSeq
      val baos = new ByteArrayOutputStream()

      val stringWriter = new MappedSeqWriter[String](baos)
      stringWriter.write(seq)
      val length = stringWriter.finish()
      val bytes = baos.toByteArray
      bytes.length shouldBe length

      (the[IllegalStateException] thrownBy {
        stringWriter.write("And once again")
      } should have).message("Already finished writing")

      val mappedSeq = MappedSeq.from[String](ByteBuffer.wrap(bytes))
      mappedSeq.isFixedSizeRecords shouldBe false
      mappedSeq.size shouldBe seq.size
      mappedSeq shouldEqual seq
    }
  }

  "MappedMap" should {
    "write and read sorted [Int, String] pairs" in {
      val data = Seq(1, 11, 111, 1111).zip("Curiosity Killed The Cat".split(' '))
      val baos = new ByteArrayOutputStream()

      val mapWriter = new MappedMapWriter[Int, String](baos)
      mapWriter.write(data)
      val length = mapWriter.finish()
      val bytes = baos.toByteArray
      bytes.length shouldBe length

      (the[IllegalStateException] thrownBy {
        mapWriter.write((13, "And once again"))
      } should have).message("Already finished writing")

      val mappedMap = MappedMap.from[Int, String](ByteBuffer.wrap(bytes))
      mappedMap.isFixedSizeRecords shouldBe false
      mappedMap.size shouldBe data.size
      mappedMap shouldEqual data.toMap
      mappedMap.get(11) shouldBe Some("Killed")
      mappedMap.get(1111) shouldBe Some("Cat")
      mappedMap.get(13) shouldBe None
      mappedMap.get(-1) shouldBe None
      mappedMap.get(1111111) shouldBe None
    }
  }
}
