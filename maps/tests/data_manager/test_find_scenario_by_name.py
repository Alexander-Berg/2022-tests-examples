import pytest

from maps_adv.geosmb.scenarist.server.lib.data_manager.scenarios import (
    DiscountForDisloyalScenario,
    DiscountForLostScenario,
    EngageProspectiveScenario,
    ThankTheLoyalScenario,
    find_scenario_by_name,
)
from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName
from maps_adv.geosmb.scenarist.server.lib.exceptions import UnknownScenario


@pytest.mark.parametrize(
    "name,  scenario_cls",
    (
        [ScenarioName.DISCOUNT_FOR_LOST, DiscountForLostScenario],
        [ScenarioName.DISCOUNT_FOR_DISLOYAL, DiscountForDisloyalScenario],
        [ScenarioName.THANK_THE_LOYAL, ThankTheLoyalScenario],
        [ScenarioName.ENGAGE_PROSPECTIVE, EngageProspectiveScenario],
    ),
)
def test_returns_expected_scenario(name, scenario_cls):
    got = find_scenario_by_name(name)

    assert got is scenario_cls


def test_raises_for_unknown_scenario():
    with pytest.raises(UnknownScenario, match="azaza"):
        find_scenario_by_name("azaza")
