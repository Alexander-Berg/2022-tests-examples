import pytest
from yb_darkspirit import scheme
from hamcrest import assert_that, equal_to, has_entries
import json

# typing only
from flask.wrappers import Response
from flask.testing import FlaskClient

from yb_darkspirit.api.schemas import FirmSchema, FirmPutSchema
from yb_darkspirit.application.plugins.dbhelper import Session


FIRM_INN = "12345"
DEFAULT_STRING = "string"
DEFAULT_INT = 1


def test_put(test_client, session):
    firm = _make_firm()
    response = test_client.put(
        '/v1/firms/{inn}'.format(inn=firm.inn),
        json=FirmPutSchema().dump(firm)[0],
    )  # type: Response
    assert_that(response.status_code, equal_to(200))

    response_content = json.loads(response.data)
    assert_that(response_content, has_entries(FirmSchema().dump(firm)[0]))

    inserted_firm = session.query(scheme.Firm).get(firm.inn)  # type: scheme.Firm
    assert_that(FirmSchema().dump(inserted_firm), equal_to(FirmSchema().dump(firm)))


@pytest.mark.parametrize("initial_agent,in_request_agent,final_agent", [
    (False, False, False),
    (True, False, False),
    (False, True, True),
    (True, True, True),
])
def test_patch(test_client, session, initial_agent, in_request_agent, final_agent):
    # type: (FlaskClient, Session, bool, bool, bool) -> None
    session.add(_make_firm(agent=initial_agent))
    session.flush()

    response = test_client.patch(
        "/v1/firms/{inn}".format(inn=FIRM_INN),
        json={"agent": in_request_agent},
    )  # type: Response

    assert_that(response.status_code, equal_to(200))
    response_content = json.loads(response.data)
    assert_that(response_content, has_entries({
        "inn": FIRM_INN,
        "agent": final_agent,
    }))

    firm = session.query(scheme.Firm).get(FIRM_INN)  # type: scheme.Firm
    assert_that(firm.agent, equal_to(final_agent))
    assert_that(firm.title, equal_to(DEFAULT_STRING))
    assert_that(firm.kpp, equal_to(DEFAULT_STRING))
    assert_that(firm.ogrn, equal_to(DEFAULT_STRING))
    assert_that(firm.sono_initial, equal_to(DEFAULT_INT))
    assert_that(firm.sono_destination, equal_to(DEFAULT_INT))


def test_patch_empty_parameters_returns_validation_error(test_client, session):
    session.add(_make_firm(agent=False))
    session.flush()

    response = test_client.patch(
        "/v1/firms/{inn}".format(inn=FIRM_INN),
        json={},
    )  # type: Response

    assert_that(response.status_code, equal_to(400))


def _make_firm(agent=False):
    return scheme.Firm(inn=FIRM_INN, title=DEFAULT_STRING, kpp=DEFAULT_STRING, ogrn=DEFAULT_STRING,
                       agent=agent, sono_initial=DEFAULT_INT, sono_destination=DEFAULT_INT, hidden=False)
