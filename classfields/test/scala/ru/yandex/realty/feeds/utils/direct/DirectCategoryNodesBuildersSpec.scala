package ru.yandex.realty.feeds.utils.direct

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.feeds.utils.OfferUtils
import ru.yandex.realty.model.offer.{CommercialType, OfferType}
import ru.yandex.realty.model.sites.{SaleStatus, Site}
import ru.yandex.realty.sites.SitesGroupingService

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class DirectCategoryNodesBuildersSpec extends WordSpec with Matchers with OfferUtils with MockFactory {

  private case class WeakCategory(id: Int, name: String, parentId: Option[Int] = None) {
    override def toString: String = {
      val parent = parentId.map(pId => s"parent with id $pId").getOrElse("no parent")

      s"'$name' with $id and $parent"
    }
  }

  private def produceSite(id: Int, name: String, isSoldOut: Boolean): Site = {
    val res = new Site(id)
    res.setName(name)
    if (isSoldOut) res.setSaleStatus(SaleStatus.SOLD)
    res
  }

  private val siteWithId1OnSale: Site = produceSite(1, "Site 1", isSoldOut = false)
  private val siteWithId1Sold: Site = produceSite(1, "Site 1", isSoldOut = true)

  private val siteWithId2OnSale: Site = produceSite(2, "Site 2", isSoldOut = false)
  private val siteWithId2Sold: Site = produceSite(2, "Site 2", isSoldOut = true)

  private val siteWithId3OnSale: Site = produceSite(3, "Site 3", isSoldOut = false)
  private val siteWithId3Sold: Site = produceSite(3, "Site 3", isSoldOut = true)

  private def siteGroupingService(sites: Seq[Site]): SitesGroupingService = {
    val service = mock[SitesGroupingService]

    (service.getAllSites _)
      .expects()
      .anyNumberOfTimes()
      .returns(sites.asJava)
    service
  }

  private def categoryNodeToWeak(node: DirectCategoryNode[_], prevParentId: Option[Int] = None): Seq[WeakCategory] =
    Seq(WeakCategory(node.id, node.name, prevParentId)) ++
      node.subNodes.flatMap(categoryNodeToWeak(_, Some(node.id)))

  private def compareWeak(actual: Seq[WeakCategory], expected: Seq[WeakCategory]): Unit = {

    def diff(a: Seq[WeakCategory], b: Seq[WeakCategory]) = a.toSet.diff(b.toSet)

    val notFound = diff(actual, expected)
    val redundant = diff(expected, actual)

    if (notFound.nonEmpty || redundant.nonEmpty) {
      val msg = new StringBuilder("expected didn't contain same elements as actual\n")

      if (notFound.nonEmpty) {
        msg ++= "Not found: " + notFound.toSeq.sortBy(_.name).mkString(", ") + System.lineSeparator()
      }
      if (redundant.nonEmpty) {
        msg ++= "Redundant: " + redundant.toSeq.sortBy(_.name).mkString(", ") + System.lineSeparator()
      }

      fail(msg.toString())
    }
  }

  "DirectCategoryNodesBuilders" should {
    "correctly build for offers with type" when {
      "comes unknown offer type" in {
        an[IllegalArgumentException] should be thrownBy DirectCategoryNodesBuilders.buildForOffersWithType(
          OfferType.UNKNOWN
        )
      }

      "comes known type" in {
        for (tp <- Seq(OfferType.SELL, OfferType.RENT)) {
          withClue(s"For type $tp") {
            val res = DirectCategoryNodesBuilders.buildForOffersWithType(tp)
            val expected = Seq(
              WeakCategory(1, if (tp == OfferType.SELL) "Продажа" else "Аренда"),
              WeakCategory(2, "Квартира", Some(1)),
              WeakCategory(3, "1 комнатная", Some(2)),
              WeakCategory(4, "2 комнатная", Some(2)),
              WeakCategory(5, "3 комнатная", Some(2)),
              WeakCategory(6, "Студия", Some(2)),
              WeakCategory(7, "4 и более комнатная", Some(2)),
              WeakCategory(8, getCommercialType(None), Some(1))
            ) ++
              CommercialType
                .values()
                .filter(_ != CommercialType.UNKNOWN)
                .zipWithIndex
                .map {
                  case (x, i) =>
                    WeakCategory(8 + i + 1, getCommercialType(Some(x)), Some(8))
                } ++ Seq(
              WeakCategory(20, "Комната", Some(1)),
              WeakCategory(21, "Гараж", Some(1)),
              WeakCategory(22, "Дом", Some(1)),
              WeakCategory(23, "Участок", Some(1))
            )

            compareWeak(categoryNodeToWeak(res), expected)
          }
        }

      }

    }

    "correctly build for offers in sites" when {
      "comes unknown category" in {
        an[IllegalArgumentException] should be thrownBy DirectCategoryNodesBuilders.buildOfferSiteCategoryTree(
          OfferType.UNKNOWN,
          siteGroupingService(Seq.empty)
        )
      }

      "comes known category and site with sold status" in {
        for {
          tp <- Seq(OfferType.SELL, OfferType.RENT)
        } {
          withClue(s"For $tp with site2 sold") {
            val res = categoryNodeToWeak(
              DirectCategoryNodesBuilders.buildOfferSiteCategoryTree(
                tp,
                siteGroupingService(Seq(siteWithId1OnSale, siteWithId2Sold, siteWithId3OnSale))
              )
            )

            val expected = Seq(
              WeakCategory(2, "ЖК"),
              WeakCategory(1, siteWithId1OnSale.getName, parentId = Some(2)),
              WeakCategory(4, "1 комнатная", Some(1)),
              WeakCategory(5, "2 комнатная", Some(1)),
              WeakCategory(6, "3 комнатная", Some(1)),
              WeakCategory(7, "Студия", Some(1)),
              WeakCategory(8, "4 и более комнатная", Some(1)),
              WeakCategory(3, siteWithId3OnSale.getName, parentId = Some(2)),
              WeakCategory(9, "1 комнатная", Some(3)),
              WeakCategory(10, "2 комнатная", Some(3)),
              WeakCategory(11, "3 комнатная", Some(3)),
              WeakCategory(12, "Студия", Some(3)),
              WeakCategory(13, "4 и более комнатная", Some(3))
            )

            compareWeak(res, expected)
          }

          withClue(s"For $tp with site1 sold") {
            val res = categoryNodeToWeak(
              DirectCategoryNodesBuilders.buildOfferSiteCategoryTree(
                tp,
                siteGroupingService(Seq(siteWithId1Sold, siteWithId2OnSale, siteWithId3OnSale))
              )
            )

            val expected = Seq(
              WeakCategory(1, "ЖК"),
              WeakCategory(2, siteWithId2OnSale.getName, parentId = Some(1)),
              WeakCategory(4, "1 комнатная", Some(2)),
              WeakCategory(5, "2 комнатная", Some(2)),
              WeakCategory(6, "3 комнатная", Some(2)),
              WeakCategory(7, "Студия", Some(2)),
              WeakCategory(8, "4 и более комнатная", Some(2)),
              WeakCategory(3, siteWithId3OnSale.getName, parentId = Some(1)),
              WeakCategory(9, "1 комнатная", Some(3)),
              WeakCategory(10, "2 комнатная", Some(3)),
              WeakCategory(11, "3 комнатная", Some(3)),
              WeakCategory(12, "Студия", Some(3)),
              WeakCategory(13, "4 и более комнатная", Some(3))
            )

            compareWeak(res, expected)
          }
        }
      }

    }

    "correctly build for sites" in {
      for {
        s1 <- Seq(siteWithId1Sold, siteWithId1OnSale)
        s2 <- Seq(siteWithId2Sold, siteWithId2OnSale)
        s3 <- Seq(siteWithId3Sold, siteWithId3OnSale)
      } {
        val sites = Seq(s1, s2, s3)
        val sitesString = sites
          .map { s =>
            s"[#${s.getId}, sold=${s.isSoldOut}]"
          }
          .mkString(", ")

        withClue(s"when sites is $sitesString") {
          val res = categoryNodeToWeak(
            DirectCategoryNodesBuilders.buildSiteCategoryTree(
              siteGroupingService(sites)
            )
          )

          compareWeak(
            actual = res,
            expected = Seq(
              WeakCategory(4, "Новостройка"),
              WeakCategory(1, s1.getName, Some(4)),
              WeakCategory(2, s2.getName, Some(4)),
              WeakCategory(3, s3.getName, Some(4))
            )
          )
        }

      }

    }
  }
}
