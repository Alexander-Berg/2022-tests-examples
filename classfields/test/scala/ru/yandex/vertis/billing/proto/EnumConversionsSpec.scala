package ru.yandex.vertis.billing.proto

import com.google.protobuf.ProtocolMessageEnum
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.model_core.ExternalCallSettings.RedirectSources
import ru.yandex.vertis.billing.model_core.ServiceObject.Kinds
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.billing.model_core.{
  BindingSources,
  CostTypes,
  GoodTypes,
  InactiveReasons,
  NotificationEvent,
  Platforms
}

/**
  * Specs on enum protobuf conversions
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
class EnumConversionsSpec extends AnyWordSpec with Matchers {

  "RedirectSources" should {
    "be convertible to protobuf" in {
      checkToProtoByIdMapping(RedirectSources, Model.CampaignSettings.CallSettings.RedirectSource.forNumber)
    }
    "be readable from protobuf" in {
      checkFromProtoByIdMapping(Model.CampaignSettings.CallSettings.RedirectSource.values(), RedirectSources)
    }
  }

  "Platforms" should {
    "be convertible to protobuf" in {
      checkToProtoByIdMapping(Platforms, Model.CampaignSettings.Platform.forNumber)
    }
    "be readable from protobuf" in {
      checkFromProtoByIdMapping(Model.CampaignSettings.Platform.values(), Platforms)
    }
  }

  "BindingSources" should {
    import BindingSources._
    import Model.BindingSource._
    "be convertible to protobuf" in {
      BindingSources.values.foreach(Conversions.toMessage)

      Conversions.toMessage(Api) should be(API)
      Conversions.toMessage(Feed) should be(FEED)
    }
    "be readable from protobuf" in {
      Conversions.bindingSourceFromMessage(API) should be(Api)
      Conversions.bindingSourceFromMessage(FEED) should be(Feed)
      intercept[IllegalArgumentException] {
        Conversions.bindingSourceFromMessage(UNKNOWN)
      }
    }
  }

  "InactiveReasons" should {
    "be convertible to protobuf" in {
      checkToProtoByIdMapping(InactiveReasons, Model.InactiveReason.forNumber)
    }
    "be readable from protobuf" in {
      checkFromProtoByIdMapping(Model.InactiveReason.values(), InactiveReasons)
    }
  }

  "Kinds" should {
    "be convertible to protobuf" in {
      checkToProtoByIdMapping(Kinds, Model.OfferId.ServiceObject.ServiceObjectKind.forNumber)
    }
    "be readable from protobuf" in {
      checkFromProtoByIdMapping(Model.OfferId.ServiceObject.ServiceObjectKind.values(), Kinds)
    }
  }

  "NotificationEvent" should {
    "be convertible to protobuf" in {
      checkToProtoByIdMapping(NotificationEvent, Model.NotificationEvent.NotificationEventType.forNumber)
    }
    "be readable from protobuf" in {
      checkFromProtoByIdMapping(Model.NotificationEvent.NotificationEventType.values(), NotificationEvent)
    }
  }

  "GoodTypes" should {
    "be convertible to protobuf" in {
      checkToProtoByIdMapping(GoodTypes, Model.GoodType.forNumber)
    }
    "be readable from protobuf" in {
      checkFromProtoByIdMapping(Model.GoodType.values(), GoodTypes)
    }
  }

  "CostTypes" should {
    "be convertible to protobuf" in {
      checkToProtoByIdMapping(CostTypes, Model.CostType.forNumber)
    }
    "be readable from protobuf" in {
      checkFromProtoByIdMapping(Model.CostType.values(), CostTypes)
    }
  }

  def checkToProtoByIdMapping[A](`enum`: Enumeration, mapper: Int => A): Assertion = {
    val protoValues = enum.values.toVector.map(v => mapper(v.id))
    protoValues.exists(_ == null) should be(false)
  }

  def checkFromProtoByIdMapping[A <: ProtocolMessageEnum](protoValues: Array[A], `enum`: Enumeration): Assertion = {
    val modelValues = protoValues.map(v => enum.apply(v.getNumber))
    modelValues.contains(null) should be(false)
  }
}
