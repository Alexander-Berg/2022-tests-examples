from maps.bizdir.sps.workstation.api.session_wrapper import SessionWrapper
from maps.bizdir.sps.workstation.api.hypothesis import (
    _fill_edit_company_hypothesis,
    _build_full_hypothesis,
    _fill_features,
    _fill_georeference,
    _fill_history_field,
    get_hypothesis_by_id,
    get_next_hypothesis,
)

import maps.bizdir.sps.db.tables as db
import maps.bizdir.sps.workstation.api.pb as pb

import pytest

from collections.abc import Generator
from typing import Any
from unittest import mock


PATH = "maps.bizdir.sps.workstation.api.hypothesis"


def test_fill_georeference__given_georef__fills_value() -> None:
    hypothesis = pb.HypothesisGeoreference()

    georef = pb.Georeference()
    georef.point.lon = 37.6
    georef.point.lat = 55.7

    _fill_georeference(hypothesis, georef)

    assert hypothesis.value == georef


def test_fill_georeference__given_no_georef__does_nothing() -> None:
    hypothesis = pb.HypothesisGeoreference()

    _fill_georeference(hypothesis, None)

    assert not hypothesis.HasField("value")


@pytest.mark.parametrize(
    "hypothesis,business_value",
    [
        (
            pb.HypothesisEmails(),
            pb.EmailsInfo(value=pb.Emails(email=[pb.Email(value="email")])),
        ),
        (
            pb.HypothesisCompanyState(),
            pb.CompanyStateInfo(value=pb.CompanyState.OPEN),
        ),
    ],
)
def test_fill_history_field__given_approved_value_with_history__fills_it(
    hypothesis: Any, business_value: Any
) -> None:
    business_value.metadata.approval_timestamp = 1234
    business_value.change_history.add().change.events.event.add().metadata.timestamp = (
        1234
    )

    _fill_history_field(hypothesis, business_value)

    assert hypothesis.value == business_value.value
    assert (
        hypothesis.approval_timestamp
        == business_value.metadata.approval_timestamp
    )
    assert len(hypothesis.change_history) == 1


@pytest.mark.parametrize(
    "hypothesis,business_value",
    [
        (pb.HypothesisEmails(), pb.EmailsInfo()),
        (pb.HypothesisCompanyState(), pb.CompanyStateInfo()),
    ],
)
def test_fill_history_field__given_no_value__doesnt_fill_it(
    hypothesis: Any, business_value: Any
) -> None:
    business_value.ClearField("value")

    _fill_history_field(hypothesis, business_value)

    assert not hypothesis.HasField("value")


def test_fill_history_field__given_no_approval_timestamp__doesnt_fill_it() -> None:
    hypothesis = pb.HypothesisEmails()
    business_value = pb.EmailsInfo()

    _fill_history_field(hypothesis, business_value)

    assert not hypothesis.HasField("approval_timestamp")


def test_fill_history_field__given_no_business_value__does_nothing() -> None:
    hypothesis = pb.HypothesisEmails()

    _fill_history_field(hypothesis, None)

    assert not hypothesis.HasField("value")


@mock.patch(f"{PATH}._fill_history_field")
def test_fill_features__given_only_business_feature__fills_it(
    _fill_history_field: mock.Mock,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()

    business = pb.BusinessInternal()
    business.feature.add().value.id = "wifi"

    _fill_features(hypothesis.feature, business.feature)

    _fill_history_field.assert_called_once_with(
        pb.HypothesisFeature(),
        business.feature[0],
    )


@mock.patch(f"{PATH}._fill_history_field")
def test_fill_features__given_only_hypothesis_feature__doesnt_fill_it(
    _fill_history_field: mock.Mock,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    hypothesis.feature.add().hypothesis.event.add().new_value.id = "wifi"

    _fill_features(hypothesis.feature, pb.BusinessInternal().feature)

    _fill_history_field.assert_not_called()


@mock.patch(f"{PATH}._fill_history_field")
def test_fill_features__given_both_features__fills_it(
    _fill_history_field: mock.Mock,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    hypothesis.feature.add().hypothesis.event.add().new_value.id = "wifi"

    sps_business = pb.BusinessInternal()
    sps_business.feature.add().value.id = "wifi"

    _fill_features(hypothesis.feature, sps_business.feature)

    _fill_history_field.assert_called_once_with(
        hypothesis.feature[0],
        sps_business.feature[0],
    )


class Fixture:
    def __init__(
        self,
        _fill_features: mock.Mock,
        _fill_georeference: mock.Mock,
        _fill_history_field: mock.Mock,
    ) -> None:
        self.session = mock.Mock(autospec=SessionWrapper)
        self.session.session.begin.return_value = mock.MagicMock()
        self._fill_features = _fill_features
        self._fill_georeference = _fill_georeference
        self._fill_history_field = _fill_history_field


@pytest.fixture
def f() -> Generator:
    with (
        mock.patch(f"{PATH}._fill_features") as f1,
        mock.patch(f"{PATH}._fill_georeference") as f2,
        mock.patch(f"{PATH}._fill_history_field") as f3,
    ):
        yield Fixture(f1, f2, f3)


@mock.patch(f"{PATH}._build_full_hypothesis")
def test_get_hypothesis_by_id__when_hypothesis_exists__returns_it(
    _build_full_hypothesis: mock.Mock, f: Fixture
) -> None:
    f.session.hypothesis_by_id.return_value = db.Hypothesis()
    hypothesis = pb.Hypothesis(hypothesis_id="id")
    _build_full_hypothesis.return_value = hypothesis

    assert get_hypothesis_by_id(f.session, "123") == hypothesis


def test_get_hypothesis_by_id__when_hypothesis_doesnt_exist__returns_none(
    f: Fixture,
) -> None:
    f.session.hypothesis_by_id.return_value = None

    assert get_hypothesis_by_id(f.session, "123") is None


def test_get_next_hypothesis__when_no_hypothesis__returns_none(
    f: Fixture,
) -> None:
    f.session.next_hypothesis.return_value = None

    assert get_next_hypothesis(f.session, "user") is None


@mock.patch(f"{PATH}._build_full_hypothesis")
def test_get_next_hypothesis__when_next_hypothesis_exists__returns_it(
    _build_full_hypothesis: mock.Mock, f: Fixture
) -> None:
    db_hypothesis = db.Hypothesis()
    f.session.next_hypothesis.return_value = db_hypothesis
    hypothesis = pb.Hypothesis(hypothesis_id="id")
    _build_full_hypothesis.return_value = hypothesis

    assert get_next_hypothesis(f.session, "user") == hypothesis
    assert db_hypothesis.content_manager_id == "user"
    assert db_hypothesis.status == db.HypothesisStatus.assigned


def test_get_next_hypothesis__when_postponed_hypothesis_exists__doesnt_change_status(
    f: Fixture,
) -> None:
    db_hypothesis = db.Hypothesis(
        hypothesis=b"",
        status=db.HypothesisStatus.postponed,
    )
    f.session.next_hypothesis.return_value = db_hypothesis

    assert get_next_hypothesis(f.session, "user") is not None
    assert db_hypothesis.status == db.HypothesisStatus.postponed


@mock.patch(f"{PATH}._build_full_hypothesis")
def test_get_next_hypothesis__when_build_full_hypothesis_raises__marks_as_error_and_raises(
    _build_full_hypothesis: mock.Mock, f: Fixture
) -> None:
    db_hypothesis = db.Hypothesis()
    f.session.next_hypothesis.return_value = db_hypothesis
    _build_full_hypothesis.side_effect = Exception()

    with pytest.raises(Exception):
        get_next_hypothesis(f.session, "user")
    assert db_hypothesis.status == db.HypothesisStatus.error


def test_fill_edit_company_hypothesis__for_features__calls_fill_features(
    f: Fixture,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    hypothesis.feature.add().hypothesis.event.add().new_value.id = "wifi"

    business = pb.BusinessInternal()
    business.feature.add().value.id = "average_bill2"

    _fill_edit_company_hypothesis(hypothesis, business)

    f._fill_features.assert_called_once_with(
        hypothesis.feature, business.feature
    )


def test_fill_edit_company_hypothesis__given_no_georef__doesnt_fill_it(
    f: Fixture,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    business = pb.BusinessInternal()

    _fill_edit_company_hypothesis(hypothesis, business)

    f._fill_georeference.assert_called_once_with(hypothesis.georeference, None)


def test_fill_edit_company_hypothesis__given_business_georef__fills_it(
    f: Fixture,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()

    business = pb.BusinessInternal()
    business.georeference.point.lon = 37.6
    business.georeference.point.lat = 55.7

    _fill_edit_company_hypothesis(hypothesis, business)

    f._fill_georeference.assert_called_once_with(
        hypothesis.georeference, business.georeference
    )


def test_fill_edit_company_hypothesis__given_no_emails__doesnt_fill_it(
    f: Fixture,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    business = pb.BusinessInternal()

    _fill_edit_company_hypothesis(hypothesis, business)

    f._fill_history_field.assert_any_call(hypothesis.emails, None)


def test_fill_edit_company_hypothesis__given_business_emails__fills_it(
    f: Fixture,
) -> None:
    hypothesis = pb.EditCompanyHypothesis()
    business = pb.BusinessInternal()
    business.emails.value.email.add(value="email")

    _fill_edit_company_hypothesis(hypothesis, business)

    f._fill_history_field.assert_any_call(hypothesis.emails, business.emails)


@mock.patch(f"{PATH}._fill_edit_company_hypothesis")
def test_build_full_hypothesis__given_org_id__fills_edit_company(
    _fill_edit_company_hypothesis: mock.Mock,
) -> None:
    hypothesis = db.Hypothesis(
        org_id="123",
        hypothesis=b"",
        org_info=db.OrgInfo(info=b""),
        status=db.HypothesisStatus.assigned,
    )

    assert _build_full_hypothesis(hypothesis) is not None
    _fill_edit_company_hypothesis.assert_called_once()


@mock.patch(f"{PATH}._fill_edit_company_hypothesis")
def test_build_full_hypothesis__given_no_org_id__fills_status(
    _fill_edit_company_hypothesis: mock.Mock,
) -> None:
    pb_hypothesis = pb.Hypothesis(hypothesis_id="1")
    hypothesis = db.Hypothesis(
        hypothesis=pb_hypothesis.SerializeToString(),
        status=db.HypothesisStatus.open,
    )

    assert _build_full_hypothesis(hypothesis) == pb.Hypothesis(
        hypothesis_id="1",
        status=pb.HypothesisStatus.OPEN,
    )
    _fill_edit_company_hypothesis.assert_not_called()


def test_build_full_hypothesis__given_content_manager_id__fills_it() -> None:
    hypothesis = db.Hypothesis(
        content_manager_id="user_id",
        status=db.HypothesisStatus.assigned,
        hypothesis=b"",
    )

    assert _build_full_hypothesis(hypothesis).content_manager_id == "user_id"


def test_build_full_hypothesis__given_no_content_manager_id__doesnt_fill_it() -> None:
    hypothesis = db.Hypothesis(
        status=db.HypothesisStatus.assigned,
        hypothesis=b"",
    )

    assert not _build_full_hypothesis(hypothesis).HasField("content_manager_id")
