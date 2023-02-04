"""Test health reasons function that converts check data into failure reasons."""
import pytest

from walle.expert import juggler
from walle.expert.constants import ACTIVE_CHECK_SUSPECTED_PERIOD
from walle.expert.types import CheckType, CheckStatus
from walle.models import timestamp

MISSING_STATUSES = [CheckStatus.MISSING, CheckStatus.INVALID]
NON_MISSING_STATUSES = sorted(set(CheckStatus.ALL) - set(MISSING_STATUSES) - {CheckStatus.VOID})


def _mock_check_data(check_type, **overrides):
    return dict(
        {
            "type": check_type,
            "status": CheckStatus.PASSED,
            "timestamp": 1,
            "status_mtime": timestamp() - ACTIVE_CHECK_SUSPECTED_PERIOD,
            "effective_timestamp": timestamp() - 1,
            "metadata": {"reason": "Reason mock"},
            "stale_timestamp": timestamp(),
        },
        **overrides
    )


def _mock_failure_reason_data(status=CheckStatus.PASSED, **overrides):
    return dict(
        {
            "status": status,
            "timestamp": 1,
            "status_mtime": timestamp() - ACTIVE_CHECK_SUSPECTED_PERIOD,
            "effective_timestamp": timestamp() - 1,
            "metadata": {"reason": "Reason mock"},
            "stale_timestamp": timestamp(),
        },
        **overrides
    )


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("check_type", CheckType.ALL)
def test_only_allowed_fields_copied(check_type):
    check = _mock_check_data(check_type, extra_field="random-string")

    reasons = juggler.get_health_status_reasons([check], [check_type])

    assert {check_type: _mock_failure_reason_data()} == reasons


@pytest.mark.parametrize("check_type", CheckType.ALL)
def test_missing_checks_are_added(check_type):
    reasons = juggler.get_health_status_reasons([], [check_type])

    assert {check_type: {"status": CheckStatus.VOID}} == reasons


@pytest.mark.parametrize("check_type", CheckType.ALL)
def test_disabled_check_skipped(check_type):
    check = _mock_check_data(check_type)

    reasons = juggler.get_health_status_reasons([check], ["some-other-check-that-is-the-only-enabled-check"])

    assert {"some-other-check-that-is-the-only-enabled-check": {"status": CheckStatus.VOID}} == reasons


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("check_type", CheckType.ALL)
@pytest.mark.parametrize("non_missing_status", NON_MISSING_STATUSES)
def test_old_checks_marked_as_staled(check_type, non_missing_status):
    check = _mock_check_data(check_type, status=non_missing_status, stale_timestamp=10, random_key="extra_data")

    reasons = juggler.get_health_status_reasons([check], [check_type])

    assert {check_type: _mock_failure_reason_data(status=CheckStatus.STALED, stale_timestamp=10)} == reasons


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("check_type", CheckType.ALL)
@pytest.mark.parametrize("missing_status", MISSING_STATUSES)
def test_old_missing_checks_not_marked_as_staled(check_type, missing_status):
    check = _mock_check_data(check_type, status=missing_status, stale_timestamp=10, extra_key="some_data")

    reasons = juggler.get_health_status_reasons([check], [check_type])

    assert {check_type: _mock_failure_reason_data(status=missing_status, stale_timestamp=10)} == reasons


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("active_check_type", CheckType.ALL_ACTIVE)
@pytest.mark.parametrize("non_missing_status", NON_MISSING_STATUSES)
def test_freshly_failed_active_checks_marked_as_suspected(active_check_type, non_missing_status):
    status_mtime = timestamp() - ACTIVE_CHECK_SUSPECTED_PERIOD + 1

    check = _mock_check_data(active_check_type, status=non_missing_status, status_mtime=status_mtime, other_data="mock")

    reasons = juggler.get_health_status_reasons([check], [active_check_type])

    reason_data = _mock_failure_reason_data(status=CheckStatus.SUSPECTED, status_mtime=status_mtime)
    assert {active_check_type: reason_data} == reasons


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("missing_status", MISSING_STATUSES)
@pytest.mark.parametrize("active_check_type", CheckType.ALL_ACTIVE)
def test_missing_and_invalid_active_checks_not_marked_as_suspected(active_check_type, missing_status):
    status_mtime = timestamp() - ACTIVE_CHECK_SUSPECTED_PERIOD + 1

    check = _mock_check_data(active_check_type, status=missing_status, status_mtime=status_mtime)

    reasons = juggler.get_health_status_reasons([check], [active_check_type])

    assert {active_check_type: _mock_failure_reason_data(status=missing_status, status_mtime=status_mtime)} == reasons


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("non_active_check_type", sorted(set(CheckType.ALL) - set(CheckType.ALL_ACTIVE)))
@pytest.mark.parametrize("check_status", CheckStatus.ALL)
def test_freshly_failed_passive_checks_not_marked_as_suspected(non_active_check_type, check_status):
    check = _mock_check_data(non_active_check_type, status=check_status, status_mtime=timestamp())

    reasons = juggler.get_health_status_reasons([check], [non_active_check_type])

    reason_data = _mock_failure_reason_data(status=check_status, status_mtime=timestamp())
    assert {non_active_check_type: reason_data} == reasons


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("check_type", CheckType.ALL)
@pytest.mark.parametrize("non_missing_status", NON_MISSING_STATUSES)
def test_checks_with_not_fresh_enough_fresh_effective_timestamp_are_unsure(check_type, non_missing_status):
    check = _mock_check_data(check_type, status=non_missing_status)

    reasons = juggler.get_health_status_reasons([check], [check_type], check_min_time=timestamp())

    reason_data = _mock_failure_reason_data(status=CheckStatus.UNSURE)
    assert {check_type: reason_data} == reasons


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("check_type", CheckType.ALL)
@pytest.mark.parametrize("non_missing_status", NON_MISSING_STATUSES)
def test_checks_with_fresh_enough_effective_timestamp_are_sure(check_type, non_missing_status):
    effective_timestamp = timestamp()
    check = _mock_check_data(check_type, status=non_missing_status, effective_timestamp=effective_timestamp)

    reasons = juggler.get_health_status_reasons([check], [check_type], check_min_time=timestamp())

    reason_data = _mock_failure_reason_data(status=non_missing_status, effective_timestamp=effective_timestamp)
    assert {check_type: reason_data} == reasons


@pytest.mark.usefixtures("monkeypatch_timestamp")
@pytest.mark.parametrize("missing_status", MISSING_STATUSES)
@pytest.mark.parametrize("check_type", CheckType.ALL)
def test_missing_checks_are_never_unsure(check_type, missing_status):
    check = _mock_check_data(check_type, status=missing_status)

    reasons = juggler.get_health_status_reasons([check], [check_type], check_min_time=timestamp())

    reason_data = _mock_failure_reason_data(status=missing_status)
    assert {check_type: reason_data} == reasons
