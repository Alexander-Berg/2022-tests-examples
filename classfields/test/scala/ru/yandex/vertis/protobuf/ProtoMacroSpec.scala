package ru.yandex.vertis.protobuf

import com.google.protobuf.Int32Value
import org.scalatest.{FunSuite, Matchers}
import ru.yandex.vertis.protobuf.test.{ComplexMessage, NonPrimitiveData}


/**
  * @author azakharov
*/
class ProtoMacroSpec extends FunSuite with Matchers {

  import ProtoMacro._

  test("get optional for non primitive data type field gives correct answer") {
    val b = ComplexMessage.newBuilder
    b.setNonPrimitive(NonPrimitiveData.newBuilder().setA(1).setB(2))
    val msg = b.build

    opt(msg.getNonPrimitive) should be (Some(NonPrimitiveData.newBuilder().setA(1).setB(2).build()))
    opt(msg.getInt32Value) should be (None)
    opt(msg.getInt64Value) should be (None)
    opt(msg.getFloatValue) should be (None)
    opt(msg.getDoubleValue) should be (None)
    opt(msg.getBoolValue) should be (None)
    opt(msg.getStringValue) should be (None)
  }

  test("get optional value for Int32Value data type field gives correct answer") {
    val b = ComplexMessage.newBuilder
    b.setInt32Value(Int32Value.newBuilder.setValue(42))
    val msg = b.build

    opt(msg.getNonPrimitive) should be (None)
    opt(msg.getInt32Value) should be (Some(Int32Value.newBuilder.setValue(42).build()))
    opt(msg.getInt64Value) should be (None)
    opt(msg.getFloatValue) should be (None)
    opt(msg.getDoubleValue) should be (None)
    opt(msg.getBoolValue) should be (None)
    opt(msg.getStringValue) should be (None)
  }
}
