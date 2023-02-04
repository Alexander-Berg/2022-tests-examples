from datetime import datetime

import pytest

from walle.clients import juggler
from walle.models import monkeypatch_timestamp
from walle.util import cert
from walle.util.misc import datetime_to_timestamp


@pytest.fixture(autouse=True)
def set_cur_datetime(mp):
    monkeypatch_timestamp(mp, datetime_to_timestamp(datetime(2020, 1, 1, 0, 0)))


def get_expiration_str(*dt_args):
    return datetime(*dt_args).strftime("%Y%m%d%H%M%SZ")


@pytest.mark.parametrize(
    "expiration_str, expected_days",
    [
        (get_expiration_str(2020, 1, 2), 1),
        (get_expiration_str(2020, 1, 1), 0),
        (get_expiration_str(2019, 12, 31), -1),
        (get_expiration_str(2021, 1, 1), 366),
    ],
)
def test_days_till_expiration(mp, expiration_str, expected_days):
    mp.function(cert._get_cert_expiration, return_value=expiration_str)

    assert cert._days_till_expiration("some_cert_path") == expected_days


@pytest.mark.parametrize(
    "expires_in, expected_status, expected_msg",
    [
        (100, juggler.JugglerCheckStatus.OK, "mock certificate expires in 100 days"),
        (5, juggler.JugglerCheckStatus.CRIT, "mock certificate expires in 5 days"),
        (-5, juggler.JugglerCheckStatus.CRIT, "mock certificate expires in -5 days"),
    ],
)
def test_notify_if_cert_expires_soon(mp, expires_in, expected_status, expected_msg, send_event_mock):
    mp.config("cauth.cert_path", "some-path")
    mp.function(cert._days_till_expiration, return_value=expires_in)

    cert.notify_if_cert_expires_soon("mock", "cauth.cert_path", "mock-cert-expiration", days_to_warn=7)
    send_event_mock.assert_called_with("wall-e.mock-cert-expiration", status=expected_status, message=expected_msg)
