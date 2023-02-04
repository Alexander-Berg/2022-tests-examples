import pytest

from walle.errors import UnauthorizedError
from walle.scenario.authorization import authorize_scenario, list_idm_role_members

SCENARIO_TO_ISSUERS = {
    "scenario_mock_1": [],
    "scenario_mock_2": ["mock_issuer_1"],
    "scenario_mock_3": ["mock_issuer_1", "mock_issuer_2"],
}


@pytest.fixture(autouse=True)
def scenario_authorization_config(mp):
    mp.function(list_idm_role_members, side_effect=lambda st: SCENARIO_TO_ISSUERS.get(st, []))


def test_user_is_authorized_for_one_scenario(walle_test):
    issuer = "mock_issuer_2@"
    authorize_scenario("scenario_mock_3", issuer)
    for scenario_type in ("scenario_mock_1", "scenario_mock_2"):
        with pytest.raises(UnauthorizedError):
            authorize_scenario(scenario_type, issuer)


def test_user_is_authorized_for_two_scenarios(walle_test):
    issuer = "mock_issuer_1@"
    authorize_scenario("scenario_mock_3", issuer)
    authorize_scenario("scenario_mock_2", issuer)
    with pytest.raises(UnauthorizedError):
        authorize_scenario("scenario_mock_1", issuer)


def test_user_is_not_authorized_for_any_scenario(walle_test):
    issuer = "mock_issuer_3@"
    for scenario_type in SCENARIO_TO_ISSUERS.keys():
        with pytest.raises(UnauthorizedError):
            authorize_scenario(scenario_type, issuer)
