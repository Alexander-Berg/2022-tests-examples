from checks import walle_tainted_kernel

import random
import pytest
import json
from juggler.bundles import Status, CheckResult, Event
from six import iteritems

WALLE_TAINTED_KERNEL = "walle_tainted_kernel"


ERROR_MAP = {code: letter for letter, code in iteritems(walle_tainted_kernel.err_map)}
CRITICAL_ERRORS = tuple(walle_tainted_kernel.crit_errors)
NON_CRITICAL_ERRORS = tuple(ERROR_MAP.viewkeys() - set(walle_tainted_kernel.crit_errors))


def monkeypatch_sysctl(monkeypatch, *flags):
    flag_value = 0
    for flag_letter in flags:
        flag_value += ERROR_MAP[flag_letter]

    monkeypatch.setattr(walle_tainted_kernel, "get_command_output", lambda filename: str(flag_value))


def monkeypatch_sysctl_to_raise(monkeypatch, error):
    def sysctl_raise(filename):
        raise error

    monkeypatch.setattr(walle_tainted_kernel, "get_command_output", sysctl_raise)


def expected_result(expected_status, expected_metadata):
    return CheckResult([
        Event(expected_status, json.dumps(expected_metadata))
    ]).to_dict(service=WALLE_TAINTED_KERNEL)


def test_ok_when_not_tainted(monkeypatch, manifest):
    monkeypatch_sysctl(monkeypatch)

    check_result = manifest.execute(WALLE_TAINTED_KERNEL)

    assert expected_result(Status.OK, {"result": {"reason": ["Ok"]}}) == check_result


@pytest.mark.parametrize(["flag_letter"], CRITICAL_ERRORS)
def test_crit_when_tainted_with_critical_flag(monkeypatch, manifest, flag_letter):
    monkeypatch_sysctl(monkeypatch, flag_letter)

    check_result = manifest.execute(WALLE_TAINTED_KERNEL)

    expected_reason = "kernel.tainted have statuses {};" \
                      " see https://nda.ya.ru/t/TG8imMBE4JPHAv for check errors".format(flag_letter)
    assert expected_result(Status.CRIT, {"result": {"reason": [expected_reason]}}) == check_result


@pytest.mark.parametrize(["flag_letter"], NON_CRITICAL_ERRORS)
def test_ok_when_non_critical_flags(monkeypatch, manifest, flag_letter):
    monkeypatch_sysctl(monkeypatch, flag_letter)

    check_result = manifest.execute(WALLE_TAINTED_KERNEL)

    expected_reason = "kernel.tainted have statuses {};" \
                      " see https://nda.ya.ru/t/TG8imMBE4JPHAv for check errors".format(flag_letter)
    assert expected_result(Status.OK, {"result": {"reason": [expected_reason]}}) == check_result


def test_crit_when_mix_of_flags(monkeypatch, manifest):
    flags = {
        random.choice(CRITICAL_ERRORS),
        random.choice(CRITICAL_ERRORS),
        random.choice(NON_CRITICAL_ERRORS),
        random.choice(NON_CRITICAL_ERRORS),
    }

    monkeypatch_sysctl(monkeypatch, *flags)

    check_result = manifest.execute(WALLE_TAINTED_KERNEL)

    flag_letters = "".join(sorted(flags, key=ERROR_MAP.get))
    expected_reason = "kernel.tainted have statuses {};" \
                      " see https://nda.ya.ru/t/TG8imMBE4JPHAv for check errors".format(flag_letters)
    assert expected_result(Status.CRIT, {"result": {"reason": [expected_reason]}}) == check_result


def test_warn_when_can_not_get_info(monkeypatch, manifest):
    monkeypatch_sysctl_to_raise(monkeypatch, EnvironmentError("Can not call sysctl: mock error"))

    check_result = manifest.execute(WALLE_TAINTED_KERNEL)

    expected_reason = "Failed to get data for the check: Can not call sysctl: mock error"
    assert expected_result(Status.WARN, {"result": {"reason": [expected_reason]}}) == check_result
