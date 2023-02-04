package ru.yandex.realty.buildinginfo.converter

import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.{BoolValue, FloatValue, StringValue, UInt32Value}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.buildinginfo.model.importmodel.ImportBuilding

import scala.collection.JavaConverters.asScalaBufferConverter

@RunWith(classOf[JUnitRunner])
class ConvertRawBuildingFromImportSpec extends FlatSpec with PropertyChecks with Matchers {

  behavior.of(ConvertRawBuildingFromImport.getClass.getName)

  it should "import all fields in ImportBuilding" in {
    val emptyConversionResult = ConvertRawBuildingFromImport(ImportBuilding.getDefaultInstance)
    val fieldsTable = Table(
      "field",
      ImportBuilding.getDescriptor.getFields.asScala
        .filterNot(f => f.getName == "latitude" || f.getName == "longitude"): _*
    )
    forAll(fieldsTable) { field =>
      val filledFieldBuilder = ImportBuilding.newBuilder()
      field.getType match {
        case FieldDescriptor.Type.MESSAGE if field.getMessageType.getFullName == "google.protobuf.BoolValue" =>
          filledFieldBuilder.setField(field, BoolValue.of(true))
        case FieldDescriptor.Type.MESSAGE if field.getMessageType.getFullName == "google.protobuf.UInt32Value" =>
          filledFieldBuilder.setField(field, UInt32Value.of(1))
        case FieldDescriptor.Type.MESSAGE if field.getMessageType.getFullName == "google.protobuf.StringValue" =>
          filledFieldBuilder.setField(field, StringValue.of("1"))
        case FieldDescriptor.Type.MESSAGE if field.getMessageType.getFullName == "google.protobuf.FloatValue" =>
          filledFieldBuilder.setField(field, FloatValue.of(1))
        case FieldDescriptor.Type.ENUM =>
          filledFieldBuilder.setField(field, field.getEnumType.getValues.asScala.filter(_.getNumber != 0).head)
      }
      ConvertRawBuildingFromImport(filledFieldBuilder.build()) shouldNot be(emptyConversionResult)
    }
  }

}
