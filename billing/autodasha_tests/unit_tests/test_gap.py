# -*- coding: utf-8 -*-

import pytest

from tests.autodasha_tests.common import staff_utils


def test_wo_absence():
    gap = staff_utils.GapMock()
    assert gap.is_avaliable('anyone') is True


@pytest.mark.parametrize(['full_day', 'will_work', 'result'], [
    (True, True, True),
    (True, False, False),
    (False, True, True),
    (False, False, True),
])
def test_absent(full_day, will_work, result):
    person = staff_utils.PersonGap('anyone', full_day=full_day, work_in_absence=will_work)
    gap = staff_utils.GapMock([person])
    assert gap.is_avaliable('anyone') is result
