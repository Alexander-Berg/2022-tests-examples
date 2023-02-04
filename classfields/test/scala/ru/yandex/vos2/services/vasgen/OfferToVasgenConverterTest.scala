package ru.yandex.vos2.services.vasgen

import com.google.protobuf.Timestamp
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.sraas
import ru.yandex.vertis.sraas.SRaaSType
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.auto.api.ApiOfferModel.{AdditionalInfo, Offer => ApiOffer}
import ru.yandex.vos2.util.http.MockHttpClientHelper
import vertis.vasgen.options.{EqualityIndex, StorageIndex, VasgenFieldOptions}
import vertis.vasgen.{Action, IntegerValue, PrimaryKey, RawDocument, RawField, RawValue}

import java.time.Instant

/**
  * Author: Dmitrii Kariaev (lesser-daemon@yandex-team.ru)
  * Created: 20.10.2021
  */
@RunWith(classOf[JUnitRunner])
class OfferToVasgenConverterTest extends AnyFunSuite with MockHttpClientHelper with Matchers {
  implicit val trace = Traced.empty

  test("Convert update sample1") {

    OfferToVasgenConverter.add(sample1, apiOffer1) shouldBe upsert1
    OfferToVasgenConverter.remove(sample1) shouldBe delete1
  }

  val sample1: Offer = Offer
    .newBuilder()
    .setOfferID("one")
    .setOfferIRef(1)
    .setTimestampUpdate(1000001L)
    .setOfferService(OfferService.OFFER_AUTO)
    .setUserRef("Odin")
    .build()

  val apiOffer1: ApiOffer = ApiOffer
    .newBuilder()
    .setId("one")
    .setAdditionalInfo(AdditionalInfo.newBuilder().setUpdateDate(1000001L).build())
    .build()

  val upsert1 = RawDocument
    .newBuilder()
    .setPk(PrimaryKey.newBuilder().setStr("one"))
    .addFields(
      RawField
        .newBuilder()
        .setMetadata(VasgenFieldOptions.newBuilder().setName("offer_IRef").setEquality(EqualityIndex.newBuilder()))
        .addValues(RawValue.newBuilder().setInteger(IntegerValue.newBuilder().setUint64(1L)))
    )
    .addFields(
      RawField
        .newBuilder()
        .setMetadata(VasgenFieldOptions.newBuilder().setName("Offer").setStorage(StorageIndex.newBuilder()))
        .addValues(
          RawValue
            .newBuilder()
            .setAny(
              sraas.Any
                .newBuilder()
                .setValue(apiOffer1.toByteString)
                .setTypeId(
                  SRaaSType
                    .newBuilder()
                    .setVersion(OfferToVasgenConverter.schemaVersion)
                    .setMessageName("auto.api.Offer")
                )
            )
        )
    )
    .setModifiedAt(Timestamp.newBuilder().setSeconds(1000).setNanos(1000000))
    .setAction(Action.UPSERT)
    .setEpoch(0)
    .setVersion(0L)
    .build()

  val delete1 = RawDocument
    .newBuilder()
    .setPk(PrimaryKey.newBuilder().setStr("one"))
    .setModifiedAt(Timestamp.newBuilder().setSeconds(1000).setNanos(1000000))
    .setAction(Action.DELETE)
    .setEpoch(0)
    .setVersion(0L)
    .build()
}
