import json

import pytest
from checks import walle_reboots
from checks.walle_reboots import RESULT, COUNT, REASON

from juggler.bundles import Status, Event, CheckResult

from utils import make_canonization

WALLE_REBOOTS = "walle_reboots"


def create_mocked_count_reboots(reboots_per_two_days, reboots_per_week):

    class Context:
        index = 0
        REBOOTS_AMOUNT = [reboots_per_two_days, reboots_per_week]

        @classmethod
        def get_reboot(cls):
            result = cls.REBOOTS_AMOUNT[cls.index]
            cls.index += 1
            return result

    def mocked_find_reboots(*args):
        return Context.get_reboot()

    return mocked_find_reboots


def expected_result(expected_status, reboots_count, reason_str):
    return CheckResult([
        Event(expected_status, json.dumps({
            walle_reboots.RESULT: {
                walle_reboots.COUNT: reboots_count,
                walle_reboots.REASON: reason_str,
            }}))
    ]).to_dict(service=WALLE_REBOOTS)


@pytest.mark.parametrize("reboots_per_two_days, reboots_per_week, reboots_count, status",
                         [(1, 2, 2, Status.OK),
                          (3, 3, 3, Status.CRIT),
                          (3, 4, 3, Status.CRIT),
                          (2, 5, 5, Status.CRIT),
                          (3, 5, 5, Status.CRIT)]
                         )
def test_check_expected_status_reboot(monkeypatch, manifest,
                                      reboots_per_two_days, reboots_per_week,
                                      reboots_count, status):

    monkeypatch.setattr(walle_reboots, "count_reboots",
                        create_mocked_count_reboots(reboots_per_two_days, reboots_per_week))

    expected_reason_str = "Reboot counts: {} per two days, {} per week.".format(
        reboots_per_two_days, reboots_per_week
    )

    check_result = manifest.execute(WALLE_REBOOTS)
    return make_canonization(check_result, expected_result(status, reboots_count, expected_reason_str))


@pytest.mark.parametrize(["reboots_per_two_days", "reboots_per_week", "reboots_count"],
                         [(-1, 0, 0),
                          (-1, -1, 0),
                          (0, -1, 0),
                          (1, -1, 1)]
                         )
def test_check_negative_reboot_count(monkeypatch, manifest, reboots_per_two_days, reboots_per_week, reboots_count):

    monkeypatch.setattr(walle_reboots, "count_reboots",
                        create_mocked_count_reboots(reboots_per_two_days, reboots_per_week))

    expected_reason_str = "Reboot counts: {} per two days, {} per week.".format(
        max(reboots_per_two_days, 0), max(reboots_per_week, 0)
    )

    check_result = manifest.execute(WALLE_REBOOTS)
    return make_canonization(check_result, expected_result(Status.OK, reboots_count, expected_reason_str))
