from unittest.mock import Mock

import pytest

from kkt_srv.cashmachine.hldevice import HLDevice
from kkt_srv.cashmachine.machine import CashMachine

LAST_TIME_THRESHOLD = 0


def mocked_device(name: str, last_time: float, failed_last_op: bool = False, **kwargs):
    return Mock(
        name=name,
        spec=HLDevice,
        _last_time=last_time,
        _last_operation_fail=failed_last_op,
        sn=f"serial_number of {name}",
        **kwargs
    )


FAILED = mocked_device('failed', -10, True)
LONG_BEFORE_THRESHOLD = mocked_device('long before threshold', -5)
BEFORE_THRESHOLD = mocked_device('before threshold', -1)
AFTER_THRESHOLD = mocked_device('after threshold', 1)
LONG_AFTER_THRESHOLD = mocked_device('long after threshold', 5)


@pytest.mark.parametrize(
    ('devices', 'expected_device'),
    (
        ((BEFORE_THRESHOLD, AFTER_THRESHOLD, FAILED), BEFORE_THRESHOLD),
        ((BEFORE_THRESHOLD, FAILED, AFTER_THRESHOLD), BEFORE_THRESHOLD),
        ((AFTER_THRESHOLD, BEFORE_THRESHOLD, FAILED), BEFORE_THRESHOLD),
        ((AFTER_THRESHOLD, FAILED, BEFORE_THRESHOLD), BEFORE_THRESHOLD),
        ((FAILED, BEFORE_THRESHOLD, AFTER_THRESHOLD), BEFORE_THRESHOLD),
        ((FAILED, AFTER_THRESHOLD, BEFORE_THRESHOLD), BEFORE_THRESHOLD),
    )
)
def test_select_device_returns_before_threshold(devices, expected_device):
    assert CashMachine._select_device(devices, None, LAST_TIME_THRESHOLD) == expected_device


@pytest.mark.parametrize(
    ('devices', 'expected_device'),
    (
        ((BEFORE_THRESHOLD, FAILED), BEFORE_THRESHOLD),
        ((AFTER_THRESHOLD, FAILED),  AFTER_THRESHOLD),
        ((FAILED, BEFORE_THRESHOLD), BEFORE_THRESHOLD),
        ((FAILED, AFTER_THRESHOLD), AFTER_THRESHOLD),
    )
)
def test_select_device_prefer_nonfailed(devices, expected_device):
    assert CashMachine._select_device(devices, None, LAST_TIME_THRESHOLD) == expected_device


@pytest.mark.parametrize(
    ('devices', 'expected_device'),
    (
        ((LONG_BEFORE_THRESHOLD, BEFORE_THRESHOLD), LONG_BEFORE_THRESHOLD),
        ((BEFORE_THRESHOLD, LONG_BEFORE_THRESHOLD), BEFORE_THRESHOLD),
    )
)
def test_select_device_before_threshold_returns_first_in_sequence(devices, expected_device):
    assert CashMachine._select_device(devices, None, LAST_TIME_THRESHOLD) == expected_device


@pytest.mark.parametrize(
    ('devices', 'expected_device'),
    (
        ((LONG_AFTER_THRESHOLD, AFTER_THRESHOLD), AFTER_THRESHOLD),
        ((AFTER_THRESHOLD, LONG_AFTER_THRESHOLD), AFTER_THRESHOLD),
    )
)
def test_select_device_after_threshold_returns_smaller_last_time(devices, expected_device):
    assert CashMachine._select_device(devices, None, LAST_TIME_THRESHOLD) == expected_device


@pytest.mark.parametrize(
    ('devices', 'expected_device'),
    (
        ((BEFORE_THRESHOLD, ), BEFORE_THRESHOLD),
        ((AFTER_THRESHOLD, ),  AFTER_THRESHOLD),
        ((FAILED, ),           FAILED),
    )
)
def test_select_device_return_any_single_device(devices, expected_device):
    assert CashMachine._select_device(devices, None, LAST_TIME_THRESHOLD) == expected_device
