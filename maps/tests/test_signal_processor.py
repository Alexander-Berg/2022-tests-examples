from typing import Generator
from maps.bizdir.sps.signal_processor.signal_processor import SignalProcessor
import maps.bizdir.sps.db.tables as db
from maps.bizdir.sps.proto.business_internal_pb2 import BusinessInternal
from yandex.maps.proto.bizdir.sps.signal_pb2 import Signal
import pytest
from unittest.mock import Mock, MagicMock, patch


PATH = "maps.bizdir.sps.signal_processor.signal_processor"


class Fixture:
    def __init__(
        self,
        hypothesis_creator_mock: Mock,
        signal_count_mock: Mock,
        hypothesis_count_mock: Mock,
    ) -> None:
        self.sql_client = Mock()
        self.hypothesis_creator_mock = hypothesis_creator_mock.return_value
        self.signal_count_mock = signal_count_mock
        self.hypothesis_count_mock = hypothesis_count_mock
        self.signal_processor = SignalProcessor(self.sql_client)
        self.session_mock = Mock(
            spec=["begin", "query", "add"],
            **{
                "begin": MagicMock(),
            },
        )
        self.sql_client.session.return_value = self.session_mock
        self.hypothesis_mock = Mock()


@pytest.fixture(autouse=True)
def f() -> Generator:
    with (
        patch(f"{PATH}.HypothesisCreator") as hypothesis_creator_mock,
        patch(f"{PATH}._get_unprocessed_signal_count") as signal_count_mock,
        patch(f"{PATH}._get_unprocessed_hypothesis_count") as hypothesis_count_mock,
    ):
        yield Fixture(
            hypothesis_creator_mock,
            signal_count_mock,
            hypothesis_count_mock
        )


def test_get_next_row__for_some_signal__returns_it(f: Fixture) -> None:
    f.sql_client.session().query().join().filter().filter().with_for_update().first.return_value = (
        "sig1",
        "org1",
    )

    assert f.signal_processor._get_next_row(f.session_mock) == ("sig1", "org1")


@patch(f"{PATH}.monitor_hypothesis_created")
@patch(f"{PATH}.SignalProcessor._get_next_row")
def test_step__given_org_info__creates_edit_company_hypothesis_and_stores_it_to_db(
    get_next_row_mock: Mock,
    monitor_hypothesis_created_mock: Mock,
    f: Fixture,
) -> None:
    get_next_row_mock.side_effect = [
        (
            db.Signal(org_id="123", signal=Signal().SerializeToString()),
            db.OrgInfo(info=BusinessInternal().SerializeToString()),
        ),
        None,
    ]
    f.hypothesis_creator_mock.create_hypothesis.return_value = f.hypothesis_mock
    f.signal_processor._step()

    f.session_mock.add.assert_called_once()
    assert f.session_mock.add.call_args[0][0].org_id == "123"
    f.hypothesis_mock.SerializeToString.assert_called_once()
    monitor_hypothesis_created_mock.assert_called_once()


@patch(f"{PATH}.monitor_hypothesis_created")
@patch(f"{PATH}.SignalProcessor._get_next_row")
def test_step__given_no_org_info__creates_new_company_hypothesis_and_stores_it_to_db(
    get_next_row_mock: Mock,
    monitor_hypothesis_created_mock: Mock,
    f: Fixture,
) -> None:
    get_next_row_mock.side_effect = [
        (db.Signal(signal=Signal().SerializeToString()), None),
        None,
    ]
    f.hypothesis_creator_mock.create_hypothesis.return_value = f.hypothesis_mock

    f.signal_processor._step()

    f.session_mock.add.assert_called_once()
    assert f.session_mock.add.call_args[0][0].org_id is None
    f.hypothesis_mock.SerializeToString.assert_called_once()
    monitor_hypothesis_created_mock.assert_called_once()


@patch(f"{PATH}.monitor_unprocessed_signal_count")
@patch(f"{PATH}.monitor_unprocessed_hypothesis_count")
def test_after_step__sends_yasm_signal(
    monitor_hypotheses_mock: Mock, monitor_signals_mock: Mock, f: Fixture
) -> None:
    f.signal_count_mock.return_value = 100
    f.hypothesis_count_mock.return_value = 80
    f.signal_processor._after_step()

    monitor_signals_mock.assert_called_once_with(
        100, f.signal_processor._interval
    )
    monitor_hypotheses_mock.assert_called_once_with(
        80, f.signal_processor._interval
    )


@patch(f"{PATH}.monitor_unprocessed_signal_count")
@patch(f"{PATH}.monitor_unprocessed_hypothesis_count")
def test_after_step_process__when_yasm_fails__exception_catched(
    monitor_hypotheses_mock: Mock, monitor_signals_mock: Mock, f: Fixture
) -> None:
    f.signal_count_mock.return_value = 100
    monitor_signals_mock.side_effect = Exception()
    f.signal_processor._after_step()

    monitor_signals_mock.assert_called_once()
    monitor_hypotheses_mock.assert_not_called()
