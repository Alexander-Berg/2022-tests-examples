from maps.bizdir.sps.sprav_loader.sprav_loader import (
    SpravLoader,
    _get_relevant_features,
    _get_rubric_ids,
    _refuse_signal,
    _update_permalinks,
    SqlAlchemyClient,
    YtLoader,
)

import maps.bizdir.sps.db.tables as db
import maps.bizdir.sps.sprav_loader.pb as pb

import pytest

from collections.abc import Generator
from unittest import mock


PATH = "maps.bizdir.sps.sprav_loader.sprav_loader"


def test_get_rubric_ids__given_approved_sps_business__returns_sps_rubrics() -> None:
    sps_business = pb.BusinessInternal()
    sps_business.rubrics.value.rubric.add(id="1")
    sps_business.rubrics.value.rubric.add(id="2")
    sps_business.rubrics.metadata.approval_timestamp = 111

    sprav_business = pb.Business()
    sprav_business.rubrics.rubric.add(id="3")

    assert _get_rubric_ids(sps_business, sprav_business) == {"1", "2"}


def test_get_rubric_ids__given_unapproved_sps_business__returns_sprav_rubrics() -> None:
    sps_business = pb.BusinessInternal()
    sps_business.rubrics.value.rubric.add(id="1")
    sps_business.rubrics.value.rubric.add(id="2")

    sprav_business = pb.Business()
    sprav_business.rubrics.rubric.add(id="3")

    assert _get_rubric_ids(sps_business, sprav_business) == {"3"}


def test_get_rubric_ids__given_no_sps_rubrics__returns_sprav_rubrics() -> None:
    sps_business = pb.BusinessInternal()

    sprav_business = pb.Business()
    sprav_business.rubrics.rubric.add(id="3")

    assert _get_rubric_ids(sps_business, sprav_business) == {"3"}


@mock.patch(f"{PATH}._get_rubric_ids")
def test_get_relevant_features__always__returns_them(
    _get_rubric_ids: mock.Mock,
) -> None:
    _get_rubric_ids.return_value = {"1", "2"}
    session = mock.Mock()
    session.query().filter.return_value = [("wifi",), ("average_bill2",)]

    assert _get_relevant_features(
        session, pb.BusinessInternal(), pb.Business()
    ) == {"wifi", "average_bill2"}


def test_update_permalinks__when_db_has_some__adds_missing() -> None:
    session = mock.Mock()
    org_info = db.OrgInfo(
        id="1",
        permalinks=[db.Permalink2OrgId(permalink=it) for it in ["1", "2"]],
    )

    _update_permalinks(session, org_info, {"1", "2", "3"})

    permalinks = session.add_all.call_args.args[0]
    assert {it.permalink for it in permalinks} == {"3"}


def test_update_permalinks__when_db_has_nothing__adds_all() -> None:
    session = mock.Mock()
    org_info = db.OrgInfo(id="1", permalinks=[])

    _update_permalinks(session, org_info, {"1", "2", "3"})

    permalinks = session.add_all.call_args.args[0]
    assert {it.permalink for it in permalinks} == {"1", "2", "3"}


def test_refuse_signal__always__sets_refused_status() -> None:
    signal = db.Signal()

    _refuse_signal(signal)

    assert signal.status
    assert pb.SignalResult.FromString(signal.status) == pb.SignalResult(
        refused=pb.SignalResult.Refuse()
    )


class Fixture:
    def __init__(
        self,
        _get_next_row: mock.Mock,
        _yt_loader: mock.Mock,
        _refuse_signal: mock.Mock,
    ) -> None:
        self.db_client = mock.Mock(spec=SqlAlchemyClient)
        self.session = mock.MagicMock()
        self.db_client.session.return_value = self.session
        self.yt_loader = _yt_loader.return_value
        self.sprav_loader = SpravLoader(
            yt_clients=[],
            geodata="",
            db_client=self.db_client,
        )
        self.signal = db.Signal(permalink="1")
        self._get_next_row = _get_next_row
        self._refuse_signal = _refuse_signal


@pytest.fixture
def f() -> Generator:
    with (
        mock.patch(f"{PATH}.SpravLoader._get_next_row") as f1,
        mock.patch(f"{PATH}.YtLoader", autospec=YtLoader) as f2,
        mock.patch(f"{PATH}._refuse_signal") as f3,
    ):
        yield Fixture(f1, f2, f3)


def test_do_process__when_no_company_in_export__refuses_signal(
    f: Fixture,
) -> None:
    f.yt_loader.get_business_info.return_value = None

    f.sprav_loader._do_process(f.session, f.signal)

    f._refuse_signal.assert_called_once()


def test_do_process__given_several_org_ids__refuses_signal(f: Fixture) -> None:
    f.session.query().filter.return_value = [("1",), ("2",)]

    f.sprav_loader._do_process(f.session, f.signal)

    f._refuse_signal.assert_called_once()


def test_do_process__for_some_other_error__raises(f: Fixture) -> None:
    f.signal.permalink = None

    with pytest.raises(Exception):
        f.sprav_loader._do_process(f.session, f.signal)


@mock.patch(f"{PATH}._update_org_info")
def test_do_process__given_one_org_id__updates(
    _update_org_info: mock.Mock, f: Fixture
) -> None:
    org_info = db.OrgInfo(id="1")
    f.session.query().filter.return_value = [("1",)]
    f.session.query().filter_by().with_for_update().one.return_value = org_info

    f.sprav_loader._do_process(f.session, f.signal)

    assert f.signal.org_id == org_info.id
    _update_org_info.assert_called_once()
    f.session.add.assert_not_called()


@mock.patch(f"{PATH}._update_org_info")
@mock.patch(f"{PATH}.uuid")
def test_do_process__given_no_org_id__adds(
    uuid: mock.Mock, _update_org_info: mock.Mock, f: Fixture
) -> None:
    f.session.query().filter.return_value = []
    uuid.uuid4.return_value = "uuid4"

    f.sprav_loader._do_process(f.session, f.signal)

    assert f.signal.org_id == "uuid4"
    _update_org_info.assert_called_once()
    f.session.add.assert_called_once()
