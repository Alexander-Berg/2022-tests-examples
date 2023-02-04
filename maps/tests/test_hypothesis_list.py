from maps.bizdir.sps.workstation.api.session_wrapper import SessionWrapper
from maps.bizdir.sps.workstation.api.hypothesis_list import (
    _get_company_names,
    get_hypotheses_info_list,
)

import maps.bizdir.sps.db.tables as db
import maps.bizdir.sps.workstation.api.pb as pb

import datetime
import pytest

from unittest import mock


class Fixture:
    def __init__(self) -> None:
        self.session = mock.Mock(autospec=SessionWrapper)
        self.hypothesis = db.Hypothesis(
            id="id",
            status=db.HypothesisStatus.open,
            created_at=datetime.datetime.now(),
            updated_at=datetime.datetime.now(),
        )
        self.pb_hypothesis = pb.Hypothesis(hypothesis_id="id")


@pytest.fixture
def f() -> Fixture:
    return Fixture()


def test_get_company_names__given_org_info_company_names__returns_them(
    f: Fixture,
) -> None:
    business = pb.BusinessInternal()
    business.names.value.lang_to_name["EN"].name = "Yandex"

    f.hypothesis.org_info = db.OrgInfo(info=business.SerializeToString())

    assert _get_company_names(f.hypothesis) == business.names.value


@pytest.mark.parametrize(
    "org_info",
    [
        db.OrgInfo(info=pb.BusinessInternal().SerializeToString()),
        db.OrgInfo(
            info=pb.BusinessInternal(
                names=pb.CompanyNamesInfo()
            ).SerializeToString()
        ),
    ],
)
def test_get_company_names__given_no_business_names_and_edit_hypothesis__returns_first_hypothesis_names(
    org_info: db.OrgInfo,
    f: Fixture,
) -> None:
    f.hypothesis.org_info = org_info
    f.pb_hypothesis.edit_company.company_id = "permalink"
    event = f.pb_hypothesis.edit_company.names.hypothesis.event.add(
        metadata=pb.EventMetadata(timestamp=111),
    )
    event.new_value.lang_to_name["EN"].name = "Yandex"
    f.hypothesis.hypothesis = f.pb_hypothesis.SerializeToString()

    assert _get_company_names(f.hypothesis) == event.new_value


def test_get_company_names__given_no_business_names_and_new_hypothesis__returns_first_hypothesis_names(
    f: Fixture,
) -> None:
    event = f.pb_hypothesis.new_company.names_events.event.add(
        metadata=pb.EventMetadata(timestamp=111),
    )
    event.new_value.lang_to_name["EN"].name = "Yandex"
    f.hypothesis.hypothesis = f.pb_hypothesis.SerializeToString()

    assert _get_company_names(f.hypothesis) == event.new_value


def test_get_company_names__given_no_names_in_edit_hypothesis__returns_none(
    f: Fixture,
) -> None:
    f.pb_hypothesis.edit_company.company_id = "permalink"
    f.pb_hypothesis.edit_company.ClearField("names")
    f.hypothesis.hypothesis = f.pb_hypothesis.SerializeToString()

    assert _get_company_names(f.hypothesis) is None


def test_get_company_names__given_no_names_in_new_hypothesis__returns_none(
    f: Fixture,
) -> None:
    f.pb_hypothesis.new_company.ClearField("names_events")
    f.hypothesis.hypothesis = f.pb_hypothesis.SerializeToString()

    assert _get_company_names(f.hypothesis) is None


def test_get_hypotheses_info_list__given_no_hypotheses__returns_empty(
    f: Fixture,
) -> None:
    f.session.filter_hypotheses.return_value = []

    result = get_hypotheses_info_list(
        f.session, logins=[], statuses=[], results=10, offset=0
    )

    assert result == pb.HypothesisInfoList()


def test_get_hypotheses_info_list__given_hypothesis__returns_it(
    f: Fixture,
) -> None:
    f.hypothesis.hypothesis = f.pb_hypothesis.SerializeToString()
    f.session.filter_hypotheses.return_value = [f.hypothesis]

    result = get_hypotheses_info_list(
        f.session, logins=[], statuses=[], results=10, offset=0
    ).hypothesis

    assert len(result) == 1
    assert result[0].hypothesis_id == f.pb_hypothesis.hypothesis_id
    assert result[0].status == pb.HypothesisStatus.OPEN


def test_get_hypotheses_info_list__given_uppercase_login__lowers_it(
    f: Fixture,
) -> None:
    f.session.filter_hypotheses.return_value = []

    get_hypotheses_info_list(
        f.session, logins=["USER"], statuses=[], results=10, offset=0
    )

    f.session.filter_hypotheses.assert_called_once_with({"user"}, set(), 10, 0)


def test_get_hypotheses_info_list__given_valid_status__converts_to_db_enum(
    f: Fixture,
) -> None:
    f.session.filter_hypotheses.return_value = []

    get_hypotheses_info_list(
        f.session, logins=[], statuses=["OPEN"], results=10, offset=0
    )

    f.session.filter_hypotheses.assert_called_once_with(
        set(), {db.HypothesisStatus.open}, 10, 0
    )


def test_get_hypotheses_info_list__given_invalid_status__skips_it(
    f: Fixture,
) -> None:
    f.session.filter_hypotheses.return_value = []

    get_hypotheses_info_list(
        f.session, logins=[], statuses=["INVALID"], results=10, offset=0
    )

    f.session.filter_hypotheses.assert_called_once_with(set(), set(), 10, 0)
