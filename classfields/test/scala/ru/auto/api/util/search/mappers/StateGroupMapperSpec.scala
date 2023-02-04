package ru.auto.api.util.search.mappers

import org.scalatest.prop.TableDrivenPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.managers.TestRequestWithId
import ru.auto.api.model.CategorySelector
import ru.auto.api.search.SearchModel.{SearchRequestParameters, State}
import ru.auto.api.ui.UiModel.{DamageGroup, StateGroup}

class StateGroupMapperSpec extends BaseSpec with TableDrivenPropertyChecks with TestRequestWithId {

  private val stateGroupMapper = new StateGroupMapper()

  private def newRequest: SearchRequestParameters.Builder = {
    SearchRequestParameters.newBuilder()
  }

  "SellerGroupMapper" should {
    "map to beaten state" in {
      val r = newRequest.setDamageGroup(DamageGroup.BEATEN).setStateGroup(StateGroup.USED)
      val params = stateGroupMapper.fromApiToSearcher(r)
      params.get("state").foreach(v => v.contains(State.BEATEN.toString))
    }

  }

  "DamageGroup" should {
    val req =
      SearchRequestParameters.newBuilder().setDamageGroup(DamageGroup.BEATEN).setStateGroup(StateGroup.USED)

    "pass damage group on through the filter (in weak case)" in {
      SpecialOffersFilterMapper.filter(CategorySelector.Cars, req, strong = false)
      req.getStateGroup shouldBe StateGroup.USED
      req.getDamageGroup shouldBe DamageGroup.BEATEN
    }

    "filter out damage group (in strong case)" in {
      SpecialOffersFilterMapper.filter(CategorySelector.Cars, req, strong = true)
      req.getStateGroup shouldBe StateGroup.USED
      req.getDamageGroup shouldBe DamageGroup.ANY
    }
  }
}
