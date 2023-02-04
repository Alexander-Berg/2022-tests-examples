from typing import Generator
import maps.bizdir.sps.signal_acceptor.signal_acceptor as signal_acceptor
from maps.bizdir.sps.db.tables import Signal as DbSignal
from yandex.maps.proto.bizdir.sps.signal_pb2 import Signal, SignalResult
from yandex.maps.proto.bizdir.sps.business_pb2 import Business
from yandex.maps.proto.bizdir.common.business_pb2 import OPEN
from sqlalchemy.exc import IntegrityError
from psycopg2.errors import UniqueViolation
import pytest
from unittest.mock import Mock, MagicMock, patch


class Fixture:
    def __init__(self, monitor_signal_added_mock: Mock) -> None:
        self.session = Mock(
            spec=["begin", "query", "add"],
            **{"begin.return_value": MagicMock()},
        )
        self.request = Mock()
        self.monitor_signal_added_mock = monitor_signal_added_mock


@pytest.fixture(autouse=True)
def f() -> Generator:
    with patch(
        "maps.bizdir.sps.signal_acceptor.signal_acceptor.monitor_signal_added"
    ) as monitor_signal_added_mock:
        yield Fixture(monitor_signal_added_mock)


def test_add_signal__given_no_signal_id__returns_bad_request(
    f: Fixture,
) -> None:
    f.request.args.get.return_value = None
    _, status = signal_acceptor.add_signal(f.session, f.request)

    assert 400 == status
    f.monitor_signal_added_mock.assert_not_called()


@pytest.mark.parametrize(
    "signal_id",
    ["123", "sps1://123", "sps1://123?", "sps1://?123", "sps1://"],
)
def test_add_signal__given_incorrect_signal_id__returns_bad_request(
    signal_id: str,
    f: Fixture,
) -> None:
    f.request.args.get.return_value = signal_id
    _, status = signal_acceptor.add_signal(f.session, f.request)

    assert 400 == status
    f.monitor_signal_added_mock.assert_not_called()


def test_add_signal__given_valid_signal__saves_it_to_db(
    f: Fixture,
) -> None:
    signal = Signal(company=Business(company_state=OPEN))
    f.request.args.get.return_value = "sps1://feedback?id=882871297"
    f.request.get_data.return_value = signal.SerializeToString()
    _, status = signal_acceptor.add_signal(f.session, f.request)

    assert f.session.add.call_args[0][0].signal == signal.SerializeToString()
    assert 201 == status
    f.monitor_signal_added_mock.assert_called_once()


def test_add_signal__given_unparsable_signal__returns_bad_request(
    f: Fixture,
) -> None:
    f.request.args.get.return_value = "sps1://feedback?id=882871297"
    f.request.get_data.return_value = "unparsable".encode("utf-8")
    _, status = signal_acceptor.add_signal(f.session, f.request)
    assert 400 == status
    f.monitor_signal_added_mock.assert_not_called()


def test_add_signal__given_dup_signal_id__returns_bad_request(
    f: Fixture,
) -> None:
    signal = Signal(company=Business(company_state=OPEN))
    f.request.args.get.return_value = "sps1://feedback?id=882871297"
    f.request.get_data.return_value = signal.SerializeToString()
    f.session.add.side_effect = IntegrityError("", "", orig=UniqueViolation())
    _, status = signal_acceptor.add_signal(f.session, f.request)
    assert 409 == status
    f.monitor_signal_added_mock.assert_not_called()


def test_get_signal__given_no_signal_id__returns_bad_request(
    f: Fixture,
) -> None:
    f.request.args.get.return_value = None
    _, status = signal_acceptor.get_signal(f.session, f.request)
    assert 400 == status


def test_get_signal_status__for_known_accepted_signal__returns_accepted_status(
    f: Fixture,
) -> None:
    signal_result = SignalResult(accepted=SignalResult.Accept(company_id="123"))
    f.session.query().filter().one_or_none.return_value = DbSignal(
        status=signal_result.SerializeToString()
    )
    f.request.args.get.return_value = "sps1://feedback?id=882871297"
    data, status = signal_acceptor.get_signal(f.session, f.request)
    assert signal_result.SerializeToString() == data
    assert 200 == status


def test_get_signal_status__for_known_not_processed_signal__returns_not_processed(
    f: Fixture,
) -> None:
    f.session.query().filter().one_or_none.return_value = DbSignal(status=None)
    f.request.args.get.return_value = "sps1://feedback?id=882871297"
    _, status = signal_acceptor.get_signal(f.session, f.request)
    assert 202 == status


def test_get_signal_status__for_unknown_signal__returns_not_found(
    f: Fixture,
) -> None:
    f.session.query().filter().one_or_none.return_value = None
    f.request.args.get.return_value = "sps1://feedback?id=882871297"
    _, status = signal_acceptor.get_signal(f.session, f.request)
    assert 404 == status
