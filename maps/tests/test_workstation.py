from typing import Any
import maps.bizdir.sps.db.tables as db
import maps.bizdir.sps.workstation.api.pb as pb
from maps.bizdir.sps.db.client import Session
from maps.bizdir.sps.workstation.api.workstation import (
    InvalidDataError,
    calculate_statistics,
    find_hypothesis,
    find_hypothesis_by_signal,
    postpone_hypothesis,
    process_verdict,
)

import pytest
from collections.abc import Generator
from unittest import mock
from google.protobuf.json_format import MessageToJson


PATH = "maps.bizdir.sps.workstation.api.workstation"


def set_rejected_status(
    _1: Any, hypothesis: db.Hypothesis, _2: Any, _3: Any
) -> None:
    hypothesis.status = db.HypothesisStatus.rejected


class Fixture:
    def __init__(
        self,
        get_hypothesis_by_id: mock.Mock,
        get_next_hypothesis: mock.Mock,
        finish_hypothesis: mock.Mock,
        session_wrapper: mock.Mock,
        process_edit_verdict: mock.Mock,
        get_config: mock.Mock,
        monitor_hypothesis_processed: mock.Mock,
    ) -> None:
        self.session = mock.Mock(autospec=Session)
        self.session.begin = mock.MagicMock()

        self.get_hypothesis_by_id = get_hypothesis_by_id
        self.get_next_hypothesis = get_next_hypothesis
        self.finish_hypothesis = finish_hypothesis
        self.finish_hypothesis.side_effect = set_rejected_status
        self.process_edit_verdict = process_edit_verdict
        self.get_config = get_config
        self.monitor_hypothesis_processed = monitor_hypothesis_processed

        self.get_config.return_value = {"workstation": {"whitelist": ["user"]}}

        self.hypothesis = db.Hypothesis(
            status=db.HypothesisStatus.assigned,
            content_manager_id="user_id",
        )

        self.wrapper = session_wrapper.return_value
        self.wrapper.hypothesis_by_id_for_update.return_value = self.hypothesis


@pytest.fixture
def f() -> Generator:
    with (
        mock.patch(f"{PATH}.get_hypothesis_by_id") as f1,
        mock.patch(f"{PATH}.get_next_hypothesis") as f2,
        mock.patch(f"{PATH}.finish_hypothesis") as f3,
        mock.patch(f"{PATH}.SessionWrapper") as f4,
        mock.patch(f"{PATH}.process_edit_verdict") as f5,
        mock.patch(f"{PATH}.get_config") as f6,
        mock.patch(f"{PATH}.monitor_hypothesis_processed") as f7,
    ):
        yield Fixture(f1, f2, f3, f4, f5, f6, f7)


def test_find_hypothesis__given_existing_hypothesis_id__returns_ok(
    f: Fixture,
) -> None:
    f.get_hypothesis_by_id.return_value = pb.Hypothesis()

    _, status = find_hypothesis(f.session, "hypothesis_id", "user")

    assert status == 200
    f.get_hypothesis_by_id.assert_called_once()


def test_find_hypothesis__given_unknown_hypothesis_id__returns_not_found(
    f: Fixture,
) -> None:
    f.get_hypothesis_by_id.return_value = None

    _, status = find_hypothesis(f.session, "hypothesis_id", "user")

    assert status == 404


def test_find_hypothesis__for_next_hypothesis_when_user_not_in_whitelist__returns_403(
    f: Fixture,
) -> None:
    f.get_config.return_value["workstation"]["whitelist"] = []

    _, status = find_hypothesis(f.session, None, "user")

    assert status == 403


def test_find_hypothesis__given_no_hypothesis_id__calls_get_next_hypothesis(
    f: Fixture,
) -> None:
    _, status = find_hypothesis(f.session, None, "user")

    f.get_next_hypothesis.assert_called_once()


def test_process_verdict__given_no_hypothesis_id__returns_400(
    f: Fixture,
) -> None:
    _, status = process_verdict(f.session, None, "user_id", "")

    assert status == 400
    f.monitor_hypothesis_processed.assert_not_called()


def test_process_verdict__given_unknown_hypothesis_id__returns_not_found(
    f: Fixture,
) -> None:
    f.wrapper.hypothesis_by_id_for_update.return_value = None

    _, status = process_verdict(f.session, "hid", "user_id", "")

    assert status == 404
    f.monitor_hypothesis_processed.assert_not_called()


def test_process_verdict__given_finished_hypothesis__returns_400(
    f: Fixture,
) -> None:
    f.hypothesis.status = db.HypothesisStatus.finished

    _, status = process_verdict(f.session, "hid", "user_id", "")

    assert status == 400
    f.monitor_hypothesis_processed.assert_not_called()


def test_process_verdict__given_different_user_id__returns_403(
    f: Fixture,
) -> None:
    f.hypothesis.content_manager_id = "user_id"

    _, status = process_verdict(f.session, "hid", "other_user_id", "")

    assert status == 403
    f.monitor_hypothesis_processed.assert_not_called()


def test_process_verdict__given_invalid_data__returns_400(
    f: Fixture,
) -> None:
    _, status = process_verdict(f.session, "hid", "user_id", "invalid")

    assert status == 400
    f.monitor_hypothesis_processed.assert_not_called()


def test_process_verdict__given_reject_verdict__returns_200(
    f: Fixture,
) -> None:
    pbverdict = pb.Verdict(reject=pb.RejectVerdict())
    verdict = MessageToJson(pbverdict)

    _, status = process_verdict(f.session, "hid", "user_id", verdict)

    f.finish_hypothesis.assert_called_once_with(
        f.wrapper, f.hypothesis, pbverdict, "user_id"
    )
    assert status == 200
    f.monitor_hypothesis_processed.assert_called_once()


def test_process_verdict__given_refuse_verdict__returns_200(
    f: Fixture,
) -> None:
    pbverdict = pb.Verdict(refuse=pb.RefuseVerdict())
    verdict = MessageToJson(pbverdict)

    _, status = process_verdict(f.session, "hid", "user_id", verdict)

    f.finish_hypothesis.assert_called_once_with(
        f.wrapper, f.hypothesis, pbverdict, "user_id"
    )
    assert status == 200
    f.monitor_hypothesis_processed.assert_called_once()


def test_process_verdict__given_on_edit_verdict__returns_200(
    f: Fixture,
) -> None:
    verdict = pb.Verdict(on_edit=pb.HypothesisVerdict())
    verdict_json = MessageToJson(verdict)

    _, status = process_verdict(f.session, "hid", "user_id", verdict_json)

    f.process_edit_verdict.assert_called_once_with(
        f.wrapper, f.hypothesis, verdict.on_edit, "user_id"
    )
    assert status == 200
    f.monitor_hypothesis_processed.assert_not_called()


def test_process_verdict__given_verdict_for_postponed_hypothesis__returns_200(
    f: Fixture,
) -> None:
    f.hypothesis.status = db.HypothesisStatus.postponed
    verdict = MessageToJson(pb.Verdict(refuse=pb.RefuseVerdict()))

    _, status = process_verdict(f.session, "hid", "user_id", verdict)

    assert status == 200


def test_process_verdict__given_verdict_for_open_hypothesis__returns_200(
    f: Fixture,
) -> None:
    f.hypothesis.status = db.HypothesisStatus.open
    verdict = MessageToJson(pb.Verdict(refuse=pb.RefuseVerdict()))

    _, status = process_verdict(f.session, "hid", "user_id", verdict)

    assert status == 200


def test_process_verdict__for_invalid_data_error__returns_400(
    f: Fixture,
) -> None:
    verdict = MessageToJson(pb.Verdict(on_edit=pb.HypothesisVerdict()))

    f.process_edit_verdict.side_effect = InvalidDataError("")

    _, status = process_verdict(f.session, "hid", "user_id", verdict)

    assert status == 400
    f.monitor_hypothesis_processed.assert_not_called()


def test_postpone_hypothesis__given_no_hypothesis_id__returns_400(
    f: Fixture,
) -> None:
    _, status = postpone_hypothesis(f.session, None, "user_id")

    assert status == 400


def test_postpone_hypothesis__given_unknown_hypothesis_id__returns_404(
    f: Fixture,
) -> None:
    f.wrapper.hypothesis_by_id_for_update.return_value = None

    _, status = postpone_hypothesis(f.session, "hid", "user_id")

    assert status == 404


def test_postpone_hypothesis__given_finished_hypothesis__returns_400(
    f: Fixture,
) -> None:
    f.hypothesis.status = db.HypothesisStatus.finished

    _, status = postpone_hypothesis(f.session, "hid", "user_id")

    assert status == 400


def test_postpone_hypothesis__given_different_user_id__returns_403(
    f: Fixture,
) -> None:
    f.hypothesis.content_manager_id = "other user id"

    _, status = postpone_hypothesis(f.session, "hid", "user_id")

    assert status == 403


def test_postpone_hypothesis__given_valid_hypothesis__postpones_it(
    f: Fixture,
) -> None:
    _, status = postpone_hypothesis(f.session, "hid", "user_id")

    assert status == 200
    assert f.hypothesis.status == db.HypothesisStatus.postponed


def test_postpone_hypothesis__given_open_hypothesis__postpones_it(
    f: Fixture,
) -> None:
    f.hypothesis.status = db.HypothesisStatus.open

    _, status = postpone_hypothesis(f.session, "hid", "user_id")

    assert status == 200
    assert f.hypothesis.status == db.HypothesisStatus.postponed


def test_find_hypothesis_by_signal__given_no_signal_id__returns_400(
    f: Fixture,
) -> None:
    _, status = find_hypothesis_by_signal(f.session, None)

    assert status == 400


def test_find_hypothesis_by_signal__given_unknown_signal_id__returns_404(
    f: Fixture,
) -> None:
    f.wrapper.signal_by_id.return_value = None

    _, status = find_hypothesis_by_signal(f.session, "sid")

    assert status == 404


def test_find_hypothesis_by_signal__given_no_hypothesis_id__returns_200(
    f: Fixture,
) -> None:
    f.wrapper.signal_by_id.return_value = db.Signal()

    data, status = find_hypothesis_by_signal(f.session, "sid")

    assert status == 200
    assert data == "{}"


def test_find_hypothesis_by_signal__given_valid_signal__returns_200(
    f: Fixture,
) -> None:
    f.wrapper.signal_by_id.return_value = db.Signal(hypothesis_id="id")

    data, status = find_hypothesis_by_signal(f.session, "sid")

    assert status == 200
    assert "hypothesisId" in data


def test_calculate_statistics__when_user_in_whitelist__returns_200_and_count(
    f: Fixture,
) -> None:
    f.get_config.return_value["workstation"]["whitelist"] = ["user_id"]
    f.wrapper.count_unreviewed_hypotheses.return_value = 33

    data, status = calculate_statistics(f.session, "user_id")

    assert status == 200
    assert 'unreviewedHypothesesCount": 33' in data


def test_calculate_statistics__when_user_not_in_whitelist__returns_200_and_zero_count(
    f: Fixture,
) -> None:
    f.get_config.return_value["workstation"]["whitelist"] = []

    data, status = calculate_statistics(f.session, "user_id")

    assert status == 200
    assert 'unreviewedHypothesesCount": 0' in data
