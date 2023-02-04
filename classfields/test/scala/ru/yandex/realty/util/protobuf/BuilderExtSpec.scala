package ru.yandex.realty.util.protobuf

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.proto.GeoPoint
import ru.yandex.realty.rent.proto.api.moderation.FlatShowing
import ru.yandex.realty.rent.proto.api.showing.FlatShowingTypeNamespace

@RunWith(classOf[JUnitRunner])
class BuilderExtSpec extends FlatSpec with Matchers {

  behavior of classOf[BuilderExt[_]].getName

  it should "modifyPrimitive" in {
    val b = GeoPoint.newBuilder()
    b.getLatitude shouldBe 0f
    b.modify[Float](_.getLatitude, _ setLatitude _)(_ + 1)
    b.getLatitude shouldBe 1f
  }

  it should "setOptField" in {
    val b = FlatShowing.newBuilder()
    val noneShowingType: Option[FlatShowingTypeNamespace.FlatShowingType] = None
    b.setOptField[FlatShowingTypeNamespace.FlatShowingType](noneShowingType, _ setShowingType _)
    b.getShowingType shouldBe FlatShowingTypeNamespace.FlatShowingType.UNKNOWN

    val someShowingType: Option[FlatShowingTypeNamespace.FlatShowingType] =
      Some(FlatShowingTypeNamespace.FlatShowingType.OFFLINE)
    b.setOptField[FlatShowingTypeNamespace.FlatShowingType](someShowingType, _ setShowingType _)
    b.getShowingType shouldBe FlatShowingTypeNamespace.FlatShowingType.OFFLINE
  }

}
