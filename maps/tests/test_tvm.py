import maps.bizdir.sps.utils.tvm as tvm
from unittest.mock import Mock, patch
import pytest
from tvmauth import TvmClientStatus
from tvmauth.exceptions import NonRetriableException, TicketParsingException


PATH = "maps.bizdir.sps.utils.tvm"


@pytest.fixture(autouse=True)
def clear_globals() -> None:
    tvm._tvm_client = None
    tvm._in_unrecoverable_state = False


@patch(f"{PATH}.TvmClient")
def test_get_tvm_client__when_it_is_good__returns_tvm_client(
    _tvm_ctor_mock: Mock
) -> None:
    assert tvm.get_tvm_client() is not None


@patch(f"{PATH}.TvmClient")
def test_get_tvm_client__when_called_twice__returns_same_tvm_client(
    _tvm_ctor_mock: Mock
) -> None:
    tvm_client1 = tvm.get_tvm_client()
    tvm_client2 = tvm.get_tvm_client()

    assert tvm_client1 == tvm_client2


@patch(f"{PATH}.TvmClient")
def test_get_tvm_client__when_error_occured__returns_none(
    _tvm_ctor_mock: Mock
) -> None:
    tvm_client = tvm.get_tvm_client()

    assert tvm_client is not None
    tvm_client.status.code = TvmClientStatus.Error

    assert tvm.get_tvm_client() is None
    tvm_client.stop.assert_called_once()


@patch(f"{PATH}.TvmClient")
def test_get_tvm_client__when_exception_occured__returns_none(
    tvm_ctor_mock: Mock
) -> None:
    tvm_ctor_mock.side_effect = Exception()

    assert tvm.get_tvm_client() is None


@patch(f"{PATH}.TvmClient")
def test_get_tvm_client__when_unrecoverable_exception__returns_none_on_second_call(
    tvm_ctor_mock: Mock
) -> None:
    tvm_ctor_mock.side_effect = NonRetriableException()

    assert tvm.get_tvm_client() is None
    tvm_ctor_mock.side_effect = None

    assert tvm.get_tvm_client() is None


@patch(f"{PATH}.TvmClient")
def test_get_tvm_client__when_recoverable_exception__returns_tvm_client_on_second_call(
    tvm_ctor_mock: Mock
) -> None:
    tvm_ctor_mock.side_effect = Exception()
    assert tvm.get_tvm_client() is None
    tvm_ctor_mock.side_effect = None
    assert tvm.get_tvm_client() is not None


def test_check_tvm_ticket__when_check_is_off__returns_ok() -> None:
    @tvm.check_tvm_ticket(Mock(), lambda: False)
    def func() -> tuple[str, int]:
        return "OK", 200

    _, status = func()

    assert status == 200


@patch(f"{PATH}.get_tvm_client", return_value=None)
def test_check_tvm_ticket__when_no_tvm_client__returns_server_error(
    _request: Mock,
) -> None:
    @tvm.check_tvm_ticket(Mock(), lambda: True)
    def func() -> tuple[str, int]:
        return "OK", 200

    _, status = func()

    assert status == 500


@patch(f"{PATH}.get_tvm_client")
def test_check_tvm_ticket__given_no_tvm_header__returns_bad_request(
    _get_tvm_client_mock : Mock,
) -> None:
    @tvm.check_tvm_ticket(
        Mock(**{"headers.get.return_value": None}),
        lambda: True
    )
    def func() -> tuple[str, int]:
        return "OK", 200

    _, status = func()

    assert status == 401


@patch(f"{PATH}.get_tvm_client")
def test_check_tvm_ticket__given_valid_ticket__returns_ok(
    _get_tvm_client_mock : Mock,
) -> None:
    @tvm.check_tvm_ticket(Mock(), lambda: True)
    def func() -> tuple[str, int]:
        return "OK", 200

    _, status = func()

    assert status == 200


@patch(f"{PATH}.get_tvm_client")
def test_check_tvm_ticket__given_forbidden_ticket__returns_bad_request(
    _get_tvm_client_mock : Mock,
) -> None:
    @tvm.check_tvm_ticket(Mock(), lambda: True, lambda _srcid: False)
    def func() -> tuple[str, int]:
        return "OK", 200

    _, status = func()

    assert status == 403


@patch(f"{PATH}.get_tvm_client")
def test_check_tvm_ticket__given_malformed_ticket__returns_bad_request(
    get_tvm_client_mock : Mock,
) -> None:
    get_tvm_client_mock().check_service_ticket.side_effect = TicketParsingException(
        message='Bad ticket',
        status=1,
        debug_info='debug info'
    )

    @tvm.check_tvm_ticket(Mock(), lambda: True)
    def func() -> tuple[str, int]:
        return "OK", 200

    _, status = func()

    assert status == 401


@patch(f"{PATH}.get_tvm_client")
def test_check_tvm_ticket__when_tvm_fails__returns_server_error(
    get_tvm_client_mock : Mock,
) -> None:
    get_tvm_client_mock().check_service_ticket.side_effect = Exception()

    @tvm.check_tvm_ticket(Mock(), lambda: True)
    def func() -> tuple[str, int]:
        return "OK", 200

    _, status = func()

    assert status == 500
