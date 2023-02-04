import maps.bizdir.sps.db.tables as db
import maps.bizdir.sps.validator.pb as pb
import maps.bizdir.sps.validator.validator as validator
import yandex.maps.proto.bizdir.callcenter.callcenter_pb2 as callcenter_pb2

from maps.bizdir.sps.db.client import SqlAlchemyClient

from collections.abc import Generator
from typing import Optional
from unittest import mock

import pytest


VALIDATOR_PATH = "maps.bizdir.sps.validator.validator"
YANG_PATH = "maps.bizdir.sps.yang"


def _get_hypothesis_row(content_manager: Optional[str] = None) -> db.Hypothesis:
    hypothesis_id = "42"
    hypothesis = pb.Hypothesis(
        hypothesis_id=hypothesis_id,
        edit_company=pb.EditCompanyHypothesis(company_id="1"),
    )
    return db.Hypothesis(
        id=hypothesis_id,
        status=db.HypothesisStatus.need_info,
        hypothesis=hypothesis.SerializeToString(),
        org_info=db.OrgInfo(info=pb.BusinessInternal().SerializeToString()),
        content_manager_id=content_manager,
    )


class Fixture:
    def __init__(
        self,
        submit_task: mock.Mock,
        poll_task: mock.Mock,
        _get_hypothesis_by_id: mock.Mock,
        _all_validation_completed: mock.Mock,
    ) -> None:
        self.db_client = mock.Mock(spec=SqlAlchemyClient)
        self.session = mock.MagicMock()
        self.db_client.session.return_value = self.session
        self.validation_acceptor = validator.ValidationAcceptor(
            60 * 60, db_client=self.db_client
        )
        self.validation_requester = validator.ValidationRequester(
            "1", db_client=self.db_client
        )
        self.task_row = db.ValidationTask(
            id="555",
            task=pb.Task(
                business_submitted=pb.BusinessSubmitted()
            ).SerializeToString(),
            external_id="1",
        )
        self.submit_task = submit_task
        self.poll_task = poll_task
        self._get_hypothesis_by_id = _get_hypothesis_by_id
        self._all_validation_completed = _all_validation_completed


@pytest.fixture()
def f() -> Generator:
    with (
        mock.patch(f"{YANG_PATH}.submit_task") as f1,
        mock.patch(f"{YANG_PATH}.poll_task") as f2,
        mock.patch(f"{VALIDATOR_PATH}._get_hypothesis_by_id") as f3,
        mock.patch(f"{VALIDATOR_PATH}._all_validation_completed") as f4,
    ):
        yield Fixture(f1, f2, f3, f4)


def test_validation_requester_do_process__when_submit_task_returns_none__sets_error_status_for_task_and_hypothesis(
    f: Fixture,
) -> None:
    f.submit_task.return_value = None
    hypothesis_row = db.Hypothesis()
    f._get_hypothesis_by_id.return_value = hypothesis_row
    f.validation_requester._do_process(
        f.session,
        f.task_row,
    )
    assert hypothesis_row.status == db.HypothesisStatus.error
    assert f.task_row.status == db.ValidationTaskStatus.error


def test_validation_requester_do_process__when_exception_raised__sets_error_status_for_task_and_hypothesis(
    f: Fixture,
) -> None:
    f.submit_task.side_effect = Exception()
    hypothesis_row = db.Hypothesis()
    f._get_hypothesis_by_id.return_value = hypothesis_row
    f.validation_requester._do_process(
        f.session,
        f.task_row,
    )
    assert hypothesis_row.status == db.HypothesisStatus.error
    assert f.task_row.status == db.ValidationTaskStatus.error


def test_validation_requester_do_process__when_submit_task_returns_add_task_response__stores_external_id_and_sets_requested_status(
    f: Fixture,
) -> None:
    f.task_row.external_id = None
    external_id = "1"
    f.submit_task.return_value = callcenter_pb2.AddTaskResponse(
        task_id=external_id,
    )
    f.validation_requester._do_process(
        f.session,
        f.task_row,
    )
    assert f.task_row.status == db.ValidationTaskStatus.requested
    assert f.task_row.external_id == external_id


def test_validation_acceptor_do_process__when_result_is_not_ready__does_nothing(
    f: Fixture,
) -> None:
    f.poll_task.return_value = None
    f.validation_acceptor._do_process(
        f.session,
        f.task_row,
    )
    f.poll_task.assert_called_once()
    f.session.query.assert_not_called()


@mock.patch(f"{VALIDATOR_PATH}.ValidationAcceptor._process_task")
def test_validation_acceptor_do_process__when_process_task_raises_exception__sets_error_status_for_task_and_hypothesis(
    _process_task_mock: mock.Mock,
    f: Fixture,
) -> None:
    _process_task_mock.side_effect = Exception()
    hypothesis_row = db.Hypothesis()
    f._get_hypothesis_by_id.return_value = hypothesis_row
    f.validation_acceptor._do_process(
        f.session,
        f.task_row,
    )
    _process_task_mock.assert_called_once()
    assert hypothesis_row.status == db.HypothesisStatus.error
    assert f.task_row.status == db.ValidationTaskStatus.error


def test_validation_acceptor_do_process__given_task_result__updates_hypothesis_and_stores_it_and_task_result_to_db(
    f: Fixture,
) -> None:
    f.poll_task.return_value = pb.TaskResult(
        task=pb.Task(business_submitted=pb.BusinessSubmitted())
    )
    f._all_validation_completed.return_value = False
    hypothesis_row = _get_hypothesis_row()
    f._get_hypothesis_by_id.return_value = hypothesis_row
    f.validation_acceptor._do_process(
        f.session,
        f.task_row,
    )
    f.poll_task.assert_called_once()
    assert f.task_row.task is not None
    assert f.task_row.status == db.ValidationTaskStatus.completed
    assert hypothesis_row.status == db.HypothesisStatus.need_info


def test_validation_acceptor_do_process__when_completed_for_assigned_hypothesis__sets_assigned_status(
    f: Fixture,
) -> None:
    f.poll_task.return_value = pb.TaskResult(
        task=pb.Task(business_submitted=pb.BusinessSubmitted())
    )
    f._all_validation_completed.return_value = True
    hypothesis_row = _get_hypothesis_row(content_manager="user")
    f._get_hypothesis_by_id.return_value = hypothesis_row
    f.validation_acceptor._do_process(
        f.session,
        f.task_row,
    )
    assert hypothesis_row.status == db.HypothesisStatus.assigned


def test_validation_acceptor_do_process__when_compeleted_for_not_assigned_hypothesis__sets_open_status(
    f: Fixture,
) -> None:
    f.poll_task.return_value = pb.TaskResult(
        task=pb.Task(business_submitted=pb.BusinessSubmitted())
    )
    f._all_validation_completed.return_value = True
    hypothesis_row = _get_hypothesis_row(content_manager=None)
    f._get_hypothesis_by_id.return_value = hypothesis_row
    f.validation_acceptor._do_process(
        f.session,
        f.task_row,
    )
    assert hypothesis_row.status == db.HypothesisStatus.open
