package ru.yandex.realty.clusterzier.backend.dao.ydb

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clusterizer.backend.dao.ydb.{YdbClusterOfferActions, YdbClusterOfferTable}
import ru.yandex.realty.clusterzier.backend.dao.TestYdb
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.realty.model.message.RealtySchema.OfferMessage
import ru.yandex.vertis.protobuf.ProtoInstanceProvider

@RunWith(classOf[JUnitRunner])
class YbdClusterOfferActionsSpec
  extends AsyncSpecBase
  with TestYdb
  with ProtobufMessageGenerators
  with ProtoInstanceProvider {

  override def beforeAll(): Unit = {
    YdbClusterOfferTable.initTable(ydbWrapper, ydbConfig.tablePrefix)
  }

  private lazy val actions = new YdbClusterOfferActions(ydbConfig.tablePrefix)

  before {
    clean(ydbConfig.tablePrefix, YdbClusterOfferTable.TableName).futureValue
  }

  private lazy val offerGen: Gen[OfferMessage] =
    generate[OfferMessage](depth = 2).map(o => o.toBuilder.setOfferId(Gen.posNum[Long].next.toString).build())

  "YbdClusterOfferActions" should {
    "get empty list for empty cluster" in {
      val getAction = actions.getClusterOffers(Gen.posNum[Int].next)
      val res = actionsRunner.run(getAction).futureValue
      res.size shouldBe 0
    }

    "read added offers" in {
      val clusterKey = Gen.posNum[Int].next
      val offers =
        Gen
          .listOfN(Gen.chooseNum(1, YdbClusterOfferActions.ClusterPageSize).next, offerGen)
          .next
          .zipWithIndex
          .map(o => o._1.toBuilder.setOfferId(o._2.toString).build())

      val actualOffersAction = for {
        _ <- actions.upsertClusterOffers(clusterKey, offers)
        actualOffers <- actions.getClusterOffers(clusterKey)
      } yield actualOffers
      val actualOffers = actionsRunner.run(actualOffersAction).futureValue
      actualOffers.toSet shouldBe offers.toSet
    }

    "read added offers with pagination" in {
      val clusterKey = Gen.posNum[Int].next
      val offers =
        Gen
          .listOfN((YdbClusterOfferActions.ClusterPageSize * 3.5).toInt, offerGen)
          .next
          .zipWithIndex
          .map(o => o._1.toBuilder.setOfferId(o._2.toString).build())

      val actualOffersAction = for {
        _ <- actions.upsertClusterOffers(clusterKey, offers)
        actualOffers <- actions.getClusterOffers(clusterKey)
      } yield actualOffers
      val actualOffers = actionsRunner.run(actualOffersAction).futureValue
      actualOffers.toSet shouldBe offers.toSet
    }

    "update existing offer" in {
      val offerId = "4242"
      val expectedPreviousOffer = offerGen.next.toBuilder.setOfferId(offerId).build()
      val expectedNewOffer = offerGen.next.toBuilder.setOfferId(offerId).build()
      val clusterKey = 42
      val actualOffersAction = for {
        _ <- actions.upsertClusterOffers(clusterKey, Seq(expectedPreviousOffer))
        previousOffers <- actions.getClusterOffers(clusterKey)
        _ <- actions.upsertClusterOffers(clusterKey, Seq(expectedNewOffer))
        newOffers <- actions.getClusterOffers(clusterKey)
      } yield previousOffers -> newOffers
      val (previousOffers, newOffers) = actionsRunner.run(actualOffersAction).futureValue
      previousOffers shouldBe Seq(expectedPreviousOffer)
      newOffers shouldBe Seq(expectedNewOffer)
    }

    "delete cluster offers" in {
      val clusterKey = Gen.posNum[Int].next
      val offers =
        Gen
          .listOfN(10, offerGen)
          .next
          .zipWithIndex
          .map(o => o._1.toBuilder.setOfferId(o._2.toString).build())

      val retainedOffersAction = for {
        _ <- actions.upsertClusterOffers(clusterKey, offers)
        _ <- actions.deleteClusterOffers(clusterKey, offers.take(5).map(_.getOfferId))
        retainedOffers <- actions.getClusterOffers(clusterKey)
      } yield retainedOffers
      val retainedOffers = actionsRunner.run(retainedOffersAction).futureValue
      retainedOffers.toSet shouldBe offers.drop(5).toSet
    }

  }

}
