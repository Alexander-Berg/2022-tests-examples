package ru.yandex.realty.clusterization

import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.model.offer.Offer
import ru.yandex.realty.persistence.cassandra.OfferSupport

/**
  * Created by abulychev on 18.07.16.
  */
class ClusterIdDistributorTest extends FlatSpec with Matchers with OfferSupport {

  val o1 = buildOffer("1")
  val o2 = buildOffer("2")
  val o3 = buildOffer("3")
  val o4 = buildOffer("4")

  def cluster(head: Offer, offers: Offer*) =
    OfferCluster(head, offers.toSeq)

  def clusterIdSetter(o: Offer, clusterId: Long): Unit = {
    o.setClusterId(clusterId)
  }

  "ClusterIdDistributor" should "work fine without history" in {
    ClusterIdDistributor.distribute(
      Seq(
        cluster(head = o1, o1),
        cluster(head = o2, o2),
        cluster(head = o3, o3),
        cluster(head = o4, o4)
      ),
      Map.empty,
      clusterIdSetter
    )

    o1.getClusterId.toString should be(o1.getId)
    o2.getClusterId.toString should be(o2.getId)
    o3.getClusterId.toString should be(o3.getId)
    o4.getClusterId.toString should be(o4.getId)
  }

  it should "work fine without history (2)" in {
    ClusterIdDistributor.distribute(
      Seq(
        cluster(head = o2, o1, o2),
        cluster(head = o3, o3),
        cluster(head = o4, o4)
      ),
      Map.empty,
      clusterIdSetter
    )

    o1.getClusterId.toString should be(o2.getId)
    o2.getClusterId.toString should be(o2.getId)
    o3.getClusterId.toString should be(o3.getId)
    o4.getClusterId.toString should be(o4.getId)
  }

  it should "work fine with history" in {
    ClusterIdDistributor.distribute(
      Seq(
        cluster(head = o1, o1),
        cluster(head = o2, o2),
        cluster(head = o3, o3),
        cluster(head = o4, o4)
      ),
      Map(
        o1 -> "111",
        o2 -> "222"
      ),
      clusterIdSetter
    )

    o1.getClusterId.toString should be("111")
    o2.getClusterId.toString should be("222")
    o3.getClusterId.toString should be(o3.getId)
    o4.getClusterId.toString should be(o4.getId)
  }

  it should "work fine with history (2)" in {
    ClusterIdDistributor.distribute(
      Seq(
        cluster(head = o1, o1, o2),
        cluster(head = o4, o3, o4)
      ),
      Map(
        o1 -> o2.getId,
        o2 -> "1111"
      ),
      clusterIdSetter
    )

    o1.getClusterId.toString should be(o2.getId)
    o2.getClusterId.toString should be(o2.getId)
    o3.getClusterId.toString should be(o4.getId)
    o4.getClusterId.toString should be(o4.getId)
  }

  it should "work fine with history (3)" in {
    ClusterIdDistributor.distribute(
      Seq(
        cluster(head = o1, o1, o2)
      ),
      Map(
        o2 -> "1111"
      ),
      clusterIdSetter
    )

    o1.getClusterId.toString should be("1111")
    o2.getClusterId.toString should be("1111")
  }

}
