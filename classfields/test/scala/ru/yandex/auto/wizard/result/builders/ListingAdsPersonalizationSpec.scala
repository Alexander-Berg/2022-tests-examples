package ru.yandex.auto.wizard.result.builders

import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.core.wizard.WizardRearr
import ru.yandex.auto.searcher.catalog.MinMax
import ru.yandex.auto.searcher.configuration.SearchConfiguration
import ru.yandex.auto.wizard.AdSnippet
import ru.yandex.auto.wizard.logic.{ModelListingPersonalState, ModelListingSeen}
import ru.yandex.auto.wizard.search.ModelWizardSearcher.CarsAdSnippets
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.auto.core.model.enums.State

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ListingAdsPersonalizationSpec extends WordSpecLike with Matchers with MockitoSupport {

  private val MaxThumbs = 30

  private def car(id: String, isNew: Boolean) = {
    val ad = mock[AdSnippet]

    when(ad.id).thenReturn(id)

    when(ad.isNewState).thenReturn(isNew)
    ad
  }

  private def scWithRearrs(rearrs: WizardRearr*) = {
    val sc = mock[SearchConfiguration]

    when(sc.hasRearr(?)).thenAnswer(new Answer[Boolean] {
      override def answer(invocation: InvocationOnMock): Boolean =
        rearrs.contains(invocation.getArgument(0))
    })

    when(sc.getBbNewAutoInterestedFactor).thenReturn(0.97f)
    when(sc.getMaxWizardThumbsCount).thenReturn(MaxThumbs)
    when(sc.getCorrectedStates).thenReturn(Seq(State.Search.NEW, State.Search.USED).asJava)
    sc
  }

  private def carAdsByNewSeenCnt(newSeenCnt: Int) = {
    val seen = (1 to 10).map { id =>
      car(id.toString, isNew = id <= newSeenCnt)
    }

    val newAds = seen.filter(_.isNewState) ++ (11 to 15).map(id => car(id.toString, isNew = true))
    val allAds = newAds ++ (16 to MaxThumbs).map(id => car(id.toString, isNew = false)) ++ seen.filterNot(_.isNewState)

    CarsAdSnippets(
      newAdSnippets = newAds.toBuffer,
      adSnippets = allAds.asJava,
      seenAds = seen,
      usedAdSnippetsCount = allAds.count(!_.isNewState),
      minMaxPrice = new MinMax(0),
      inStockMinPrice = 0,
      totalAds = allAds.length,
      recommendedAds = Seq.empty,
      Seq.empty,
      isFilterDropped = false,
      isSeenMixed = false,
      isPersonalState = false
    )
  }

  private def allNotInResult(toCheck: Seq[AdSnippet], ads: CarsAdSnippets) =
    toCheck.forall(ad => !ads.adSnippets.asScala.contains(ad)) shouldBe true

  private def allInResult(toCheck: Seq[AdSnippet], ads: CarsAdSnippets) =
    toCheck.forall(ads.adSnippets.asScala.contains) shouldBe true

  "ModelListingPersonalState" should {
    "use only new cars" in {
      val sc = scWithRearrs(WizardRearr.PERSONAL_AUTO)
      val ads = carAdsByNewSeenCnt(8)

      val res = ModelListingPersonalState.withPersonalState(ads, sc)
      res.adSnippets.asScala.forall(_.isNewState) shouldBe true
    }

    "use only new cars by bb factor" in {
      val sc = scWithRearrs(WizardRearr.PERSONAL_AUTO, WizardRearr.CRYPTA_NEW_AUTO_FOLD_97)
      val ads = carAdsByNewSeenCnt(0)
      val res = ModelListingPersonalState.withPersonalState(ads, sc)
      res.adSnippets.asScala.forall(_.isNewState) shouldBe true
    }

    "skip use only new cars by bb factor if not enough threshold" in {
      val sc = scWithRearrs(WizardRearr.PERSONAL_AUTO, WizardRearr.CRYPTA_NEW_AUTO_FOLD_98)
      val ads = carAdsByNewSeenCnt(0)
      val res = ModelListingPersonalState.withPersonalState(ads, sc)
      res.adSnippets.asScala.forall(_.isNewState) shouldBe false
    }

    "use only used cars" in {
      val sc = scWithRearrs(WizardRearr.PERSONAL_AUTO)
      val ads = carAdsByNewSeenCnt(0)

      val res = ModelListingPersonalState.withPersonalState(ads, sc)
      res.adSnippets.asScala.forall(ad => !ad.isNewState) shouldBe true
    }
  }

  "ModelListingSeen" should {
    "correctly enrich withSeen" in {
      val sc = scWithRearrs(WizardRearr.MODEL_PERS_100)
      val ads = carAdsByNewSeenCnt(5)

      val withSeenNotInRes =
        ads.copy(
          seenAds = ads.seenAds ++ Seq(car("10000", isNew = false))
        )

      val res = ModelListingSeen.enrichWithSeen(withSeenNotInRes, sc)
      withSeenNotInRes.seenAds.forall(res.adSnippets.asScala.contains) shouldBe true
    }

    "correctly work in pair with MODEL_PERS_100 and PERSONAL_AUTO" in {

      def spec(newCnt: Int, seenOnlyUsed: Boolean = false) = {
        val ads = carAdsByNewSeenCnt(newCnt)
        val withSeenNotInRes =
          ads.copy(
            seenAds = (ads.seenAds ++ Seq(car("10000", isNew = false), car("10001", isNew = true)))
              .filter(ad => !seenOnlyUsed || seenOnlyUsed && !ad.isNewState)
          )

        val sc = scWithRearrs(WizardRearr.PERSONAL_AUTO, WizardRearr.MODEL_PERS_100)

        val persState = ModelListingPersonalState.withPersonalState(withSeenNotInRes, sc)
        ModelListingSeen.enrichWithSeen(persState, sc)
      }

      val onlyNewRes = spec(8)
      val onlyNewResSeen = onlyNewRes.seenAds

      onlyNewRes.adSnippets.asScala.forall(_.isNewState) shouldBe true
      allInResult(onlyNewResSeen.filter(_.isNewState), onlyNewRes)
      allNotInResult(onlyNewResSeen.filterNot(_.isNewState), onlyNewRes)

      val onlyUsedRes = spec(0, seenOnlyUsed = true)
      val onlyUsedResSeen = onlyUsedRes.seenAds

      onlyUsedRes.adSnippets.asScala.forall(ad => !ad.isNewState) shouldBe true
      allNotInResult(onlyUsedResSeen.filter(_.isNewState), onlyUsedRes)
      allInResult(onlyUsedResSeen.filterNot(_.isNewState), onlyUsedRes)
    }
  }

}
