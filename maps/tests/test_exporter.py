from maps.bizdir.sps.exporter.exporter import (
    Exporter,
    _get_diff_status,
    _post_diff,
    monitor_diff_sent_to_sprav,
    monitor_got_sprav_reply,
)
from maps.bizdir.sps.db.client import SqlAlchemyClient

import maps.bizdir.sps.db.tables as db
import maps.bizdir.sps.exporter.pb as pb

import pytest
import requests

from collections.abc import Generator
from unittest import mock


PATH = "maps.bizdir.sps.exporter.exporter"


def make_response(status_code: int, content: bytes = b"") -> requests.Response:
    response = requests.Response()
    response.status_code = status_code
    response._content = content
    return response


@mock.patch(f"{PATH}.requests")
@mock.patch(f"{PATH}.get_tvm_client")
def test_post_diff__for_successful_request__returns_diff_id(
    mock_tvm_client: mock.Mock,
    mock_requests: mock.Mock,
) -> None:
    diff_result = pb.AddBusinessDiffResponse(business_diff_id="id")
    mock_requests.post.return_value = make_response(
        201, diff_result.SerializeToString()
    )

    diff = pb.BusinessDiff(company_id="permalink")

    assert _post_diff("url", diff) == "id"
    mock_tvm_client().get_service_ticket_for.assert_called_once_with("altay-feedback")


@mock.patch(f"{PATH}.requests")
@mock.patch(f"{PATH}.get_tvm_client")
def test_post_diff__for_unsuccessful_request__raises(
    mock_tvm_client: mock.Mock,
    mock_requests: mock.Mock,
) -> None:
    mock_requests.post.return_value = make_response(400)

    diff = pb.BusinessDiff(company_id="permalink")

    with pytest.raises(Exception):
        _post_diff("url", diff)


@mock.patch(f"{PATH}.requests")
@mock.patch(f"{PATH}.get_tvm_client")
def test_post_diff__for_no_tvm_client__raises(
    mock_tvm_client: mock.Mock,
    mock_requests: mock.Mock,
) -> None:
    mock_tvm_client.return_value = None

    diff = pb.BusinessDiff(company_id="permalink")

    with pytest.raises(Exception):
        _post_diff("url", diff)


@mock.patch(f"{PATH}.requests")
def test_get_diff_status__for_finished_task__returns_result(
    mock_requests: mock.Mock,
) -> None:
    diff_result = pb.BusinessDiffResult(rejected=pb.Reject())
    mock_requests.get.return_value = make_response(
        200, diff_result.SerializeToString()
    )

    assert _get_diff_status("url", "task_id") == diff_result


@mock.patch(f"{PATH}.requests")
def test_get_diff_status__for_unfinished_task__returns_none(
    mock_requests: mock.Mock,
) -> None:
    mock_requests.get.return_value = make_response(202)

    assert _get_diff_status("url", "task_id") is None


@mock.patch(f"{PATH}.requests")
def test_get_diff_status__for_unsuccessful_request__raises(
    mock_requests: mock.Mock,
) -> None:
    mock_requests.get.return_value = make_response(400)

    with pytest.raises(Exception):
        _get_diff_status("url", "task_id")


class Fixture:
    def __init__(
        self,
        _post_diff: mock.Mock,
        _get_diff_status: mock.Mock,
        _update_signal_status: mock.Mock,
        _get_next_diff: mock.Mock,
        _update_hypothesis_status: mock.Mock,
    ) -> None:
        self.db_client = mock.Mock(spec=SqlAlchemyClient)
        self.session = mock.MagicMock()
        self.db_client.session = lambda: self.session

        self.exporter = Exporter(
            sprav_url="url",
            sprav_period=10,
            db_client=self.db_client,
        )
        self.diff = db.Diff(
            change=b"", hypothesis_id="hid", sprav_feedback_id="task_id"
        )

        self._post_diff = _post_diff
        self._get_diff_status = _get_diff_status
        self._update_signal_status = _update_signal_status
        self._get_next_diff = _get_next_diff
        self._update_hypothesis_status = _update_hypothesis_status


@pytest.fixture
def f() -> Generator:
    with (
        mock.patch(f"{PATH}._post_diff") as f1,
        mock.patch(f"{PATH}._get_diff_status") as f2,
        mock.patch(f"{PATH}._update_signal_status") as f3,
        mock.patch(f"{PATH}._get_next_diff") as f4,
        mock.patch(f"{PATH}._update_hypothesis_status") as f5,
    ):
        yield Fixture(f1, f2, f3, f4, f5)


def test_add_new_diff__for_successful_post_request__updates_fields(
    f: Fixture,
) -> None:
    business_diff = pb.BusinessDiff(company_id="permalink")
    f.diff.change = business_diff.SerializeToString()
    f._post_diff.return_value = "diff_id"

    f.exporter._add_new_diff(f.session, f.diff)

    assert f.diff.sprav_feedback_id == "diff_id"
    f._update_hypothesis_status.assert_called_once_with(
        f.session, f.diff.hypothesis_id, db.HypothesisStatus.wait_sprav
    )


def test_add_new_diff__for_bad_request__raises(
    f: Fixture,
) -> None:
    f._post_diff.side_effect = requests.HTTPError(
        response=mock.Mock(status_code=400)
    )

    with pytest.raises(requests.HTTPError):
        f.exporter._add_new_diff(f.session, f.diff)


def test_add_new_diff__given_features__removes_them(
    f: Fixture,
) -> None:
    diff = pb.BusinessDiff(company_id="permalink")
    diff.feature.add(approval_timestamp=12345)
    diff.feature[0].value.id = "wifi"
    f.diff.change = diff.SerializeToString()

    f.exporter._add_new_diff(f.session, f.diff)

    f._post_diff.assert_called_once_with(
        mock.ANY, pb.BusinessDiff(company_id="permalink")
    )


@pytest.mark.parametrize(
    "diff_result,diff_status,signal_result,hypothesis_status",
    [
        (
            pb.BusinessDiffResult(accepted=pb.Accept()),
            db.DiffStatus.accepted,
            pb.SignalResult(accepted=pb.SignalAccept()),
            db.HypothesisStatus.accepted_by_sprav,
        ),
        (
            pb.BusinessDiffResult(rejected=pb.Reject()),
            db.DiffStatus.rejected,
            pb.SignalResult(rejected=pb.SignalReject()),
            db.HypothesisStatus.rejected_by_sprav,
        ),
    ],
)
def test_check_status__for_finished_task__finishes(
    diff_result: pb.BusinessDiffResult,
    diff_status: db.DiffStatus,
    signal_result: pb.SignalResult,
    hypothesis_status: db.HypothesisStatus,
    f: Fixture,
) -> None:
    f._get_diff_status.return_value = diff_result

    f.exporter._check_status(f.session, f.diff)

    assert f.diff.status == diff_status
    f._update_signal_status.assert_called_once_with(
        f.session, f.diff.hypothesis_id, signal_result
    )
    assert f.session.monitoring_func == monitor_got_sprav_reply
    f._update_hypothesis_status.assert_called_once_with(
        f.session, f.diff.hypothesis_id, hypothesis_status
    )


def test_check_status__for_unfinished_task__doesnt_update_status(
    f: Fixture,
) -> None:
    f._get_diff_status.return_value = None

    f.exporter._check_status(f.session, f.diff)

    assert f.diff.status is None
    f._update_signal_status.assert_not_called()
    assert f.session.monitoring_func != monitor_got_sprav_reply


def test_check_status__for_not_found__raises(
    f: Fixture,
) -> None:
    f.diff.sprav_feedback_id = "task_id"
    f._get_diff_status.side_effect = requests.HTTPError(
        response=mock.Mock(status_code=404)
    )

    with pytest.raises(requests.HTTPError):
        f.exporter._check_status(f.session, f.diff)


@mock.patch(f"{PATH}.Exporter._check_status")
def test_do_process__for_diff_with_task_id__calls_check_status(
    _check_status: mock.Mock,
    f: Fixture,
) -> None:
    f.diff.sprav_feedback_id = "task id"

    f.exporter._do_process(f.session, f.diff)

    _check_status.assert_called_once_with(f.session, f.diff)


@mock.patch(f"{PATH}.Exporter._add_new_diff")
def test_do_process__for_diff_without_task_id__calls_add_new_diff(
    _add_new_diff: mock.Mock,
    f: Fixture,
) -> None:
    f.diff.sprav_feedback_id = None

    f.exporter._do_process(f.session, f.diff)

    _add_new_diff.assert_called_once_with(f.session, f.diff)
    assert f.session.monitoring_func == monitor_diff_sent_to_sprav


@mock.patch(f"{PATH}.Exporter._check_status")
def test_do_process__for_bad_request__updates_status(
    _check_status: mock.Mock,
    f: Fixture,
) -> None:
    f.diff.sprav_feedback_id = "task id"
    _check_status.side_effect = requests.HTTPError(
        response=mock.Mock(status_code=400)
    )

    f.exporter._do_process(f.session, f.diff)

    assert f.diff.status == db.DiffStatus.error
    f._update_hypothesis_status.assert_called_once_with(
        f.session, f.diff.hypothesis_id, db.HypothesisStatus.error
    )


@mock.patch(f"{PATH}.Exporter._check_status")
def test_do_process__for_not_found__drops_sprav_feedback_id(
    _check_status: mock.Mock,
    f: Fixture,
) -> None:
    f.diff.sprav_feedback_id = "task id"
    _check_status.side_effect = requests.HTTPError(
        response=mock.Mock(status_code=404)
    )

    f.exporter._do_process(f.session, f.diff)

    assert f.diff.sprav_feedback_id is None


@mock.patch(f"{PATH}.Exporter._check_status")
def test_do_process__for_internal_error__logs_it(
    _check_status: mock.Mock,
    f: Fixture,
) -> None:
    f.diff.sprav_feedback_id = "task id"
    _check_status.side_effect = requests.HTTPError(
        response=mock.Mock(status_code=500)
    )
    with mock.patch.object(f.exporter, "_logger") as logger_mock:
        f.exporter._do_process(f.session, f.diff)

        logger_mock.exception.assert_called_once()


@mock.patch(f"{PATH}.Exporter._add_new_diff")
def test_do_process__for_some_other_error__logs_it(
    _add_new_diff: mock.Mock,
    f: Fixture,
) -> None:
    f.diff.sprav_feedback_id = None
    _add_new_diff.side_effect = Exception()

    with mock.patch.object(f.exporter, "_logger") as logger_mock:
        f.exporter._do_process(f.session, f.diff)

        logger_mock.exception.assert_called_once()


@mock.patch(f"{PATH}.Exporter._do_process")
def test_step__given_next_diff__processes_it(
    _do_process: mock.Mock,
    f: Fixture,
) -> None:
    f._get_next_diff.side_effect = [f.diff, None]

    f.exporter._step()

    _do_process.assert_called_once_with(mock.ANY, f.diff)


@mock.patch(f"{PATH}._get_unanswered_diff_count", return_value=150)
@mock.patch(f"{PATH}.monitor_unanswered_diff_count")
def test_after_step__sends_yasm_signal(
    monitor_mock: mock.Mock, _get_unanswered_mock: mock.Mock, f: Fixture
) -> None:
    f.exporter._after_step()

    monitor_mock.assert_called_once_with(150, f.exporter._interval)


@mock.patch(f"{PATH}._get_unanswered_diff_count", return_value=150)
@mock.patch(f"{PATH}.monitor_unanswered_diff_count")
def test_after_step__when_yasm_fails__exception_catched(
    monitor_mock: mock.Mock, _get_unanswered_mock: mock.Mock, f: Fixture
) -> None:
    monitor_mock.side_effect = Exception()
    with mock.patch.object(f.exporter, "_logger") as logger_mock:
        f.exporter._after_step()

        monitor_mock.assert_called_once_with(150, f.exporter._interval)
        logger_mock.exception.assert_called_once()
