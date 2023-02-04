from maps.bizdir.sps.utils.yasm import send_yasm_signal
from unittest.mock import Mock, patch


PATH = "maps.bizdir.sps.utils.yasm"


@patch(f"{PATH}.get_config", return_value={"yasm_monitoring": True})
@patch(f"{PATH}.requests")
def test_send_yasm_signal__given_signal_with_ttl__posts(
    requests_mock: Mock, _: Mock
) -> None:
    send_yasm_signal("new_signal_summ", 1, 10)

    requests_mock.post.assert_called_once_with(
        "http://localhost:11005",
        json=[{"name": "new_signal_summ", "ttl": 10, "val": 1}],
        timeout=0.1,
    )
    requests_mock.post().raise_for_status.assert_called_once()


@patch(f"{PATH}.get_config", return_value={"yasm_monitoring": True})
@patch(f"{PATH}.requests")
def test_send_yasm_signal__given_signal_without_ttl__posts(
    requests_mock: Mock, _: Mock
) -> None:
    send_yasm_signal("new_signal_summ", 1)

    requests_mock.post.assert_called_once_with(
        "http://localhost:11005",
        json=[{"name": "new_signal_summ", "val": 1}],
        timeout=0.1,
    )
    requests_mock.post().raise_for_status.assert_called_once()


@patch(f"{PATH}.get_config", return_value={"yasm_monitoring": True})
@patch(f"{PATH}.requests")
def test_send_yasm_signal__when_post_fails__exception_catched(
    requests_mock: Mock, _: Mock
) -> None:
    response_mock = Mock()
    requests_mock.post.return_value = response_mock
    response_mock.raise_for_status.side_effect = Exception()
    send_yasm_signal("new_signal_summ", 1)

    requests_mock.post.assert_called_once()
    response_mock.raise_for_status.assert_called_once()


@patch(f"{PATH}.get_config", return_value={"yasm_monitoring": False})
@patch(f"{PATH}.requests")
def test_send_yasm_signal__when_monitoring_turned_off__does_nothing(
    requests_mock: Mock, _: Mock
) -> None:
    send_yasm_signal("new_signal_summ", 1)

    requests_mock.post.assert_not_called()
