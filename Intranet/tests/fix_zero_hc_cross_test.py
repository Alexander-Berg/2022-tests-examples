from functools import partial

import pytest
from django.db.models import Max

from staff.lib.testing import DepartmentFactory

from staff.departments.models import HeadcountPosition
from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.oebs.constants import REPLACEMENT_TYPE


@pytest.fixture()
def wrong_positions():
    dep = DepartmentFactory()
    HeadcountPositionFactory(department=dep)
    HeadcountPositionFactory(department=dep)

    base_id = int(HeadcountPosition.objects.all().aggregate(max_id=Max('id')).get('max_id')) + 1
    wrong_positions = []
    wpa = wrong_positions.append

    wpa(HeadcountPositionFactory(
        id=base_id + 100,
        code=100,
        headcount=0,
        prev_index=None,
        index=1,
        next_index=2,
        replacement_type=REPLACEMENT_TYPE.HAS_REPLACEMENT,
        department=dep,
    ))
    wpa(HeadcountPositionFactory(
        id=base_id + 200,
        code=100,
        headcount=0,
        prev_index=1,
        index=2,
        next_index=None,
        replacement_type=REPLACEMENT_TYPE.HC_IS_BUSY,
        department=dep,
    ))

    wpa(HeadcountPositionFactory(
        id=base_id + 101,
        code=101,
        headcount=0,
        prev_index=None,
        index=1,
        next_index=2,
        replacement_type=REPLACEMENT_TYPE.HAS_REPLACEMENT,
        department=dep,
    ))
    wpa(HeadcountPositionFactory(
        id=base_id + 201,
        code=101,
        headcount=1,
        prev_index=1,
        index=2,
        next_index=None,
        replacement_type=REPLACEMENT_TYPE.HC_IS_BUSY,
        department=dep,
    ))

    wpa(HeadcountPositionFactory(
        id=base_id + 110,
        code=110,
        headcount=1,
        prev_index=None,
        index=1,
        next_index=2,
        replacement_type=REPLACEMENT_TYPE.HAS_REPLACEMENT,
        department=dep,
    ))
    wpa(HeadcountPositionFactory(
        id=base_id + 210,
        code=110,
        headcount=0,
        prev_index=1,
        index=2,
        next_index=None,
        replacement_type=REPLACEMENT_TYPE.HC_IS_BUSY,
        department=dep,
    ))

    hp_1 = partial(
        HeadcountPositionFactory,
        prev_index=None,
        index=1,
        next_index=2,
        replacement_type=REPLACEMENT_TYPE.HAS_REPLACEMENT,
        department=dep,
    )
    hp_2 = partial(
        HeadcountPositionFactory,
        prev_index=1,
        index=2,
        next_index=3,
        replacement_type=REPLACEMENT_TYPE.HAS_REPLACEMENT_AND_HC_IS_BUSY,
        department=dep,
    )
    hp_3 = partial(
        HeadcountPositionFactory,
        prev_index=2,
        index=3,
        next_index=None,
        replacement_type=REPLACEMENT_TYPE.HC_IS_BUSY,
        department=dep,
    )

    wpa(hp_1(id=base_id + 1001, code=1001, headcount=0))
    wpa(hp_2(id=base_id + 2001, code=1001, headcount=0))
    wpa(hp_3(id=base_id + 3001, code=1001, headcount=1))

    wpa(hp_1(id=base_id + 1010, code=1010, headcount=0))
    wpa(hp_2(id=base_id + 2010, code=1010, headcount=1))
    wpa(hp_3(id=base_id + 3010, code=1010, headcount=0))

    wpa(hp_1(id=base_id + 1011, code=1011, headcount=0))
    wpa(hp_2(id=base_id + 2011, code=1011, headcount=1))
    wpa(hp_3(id=base_id + 3011, code=1011, headcount=1))

    wpa(hp_1(id=base_id + 1100, code=1100, headcount=1))
    wpa(hp_2(id=base_id + 2100, code=1100, headcount=0))
    wpa(hp_3(id=base_id + 3100, code=1100, headcount=0))

    wpa(hp_1(id=base_id + 1101, code=1101, headcount=1))
    wpa(hp_2(id=base_id + 2101, code=1101, headcount=0))
    wpa(hp_3(id=base_id + 3101, code=1101, headcount=1))

    wpa(hp_1(id=base_id + 1110, code=1110, headcount=1))
    wpa(hp_2(id=base_id + 2110, code=1110, headcount=1))
    wpa(hp_3(id=base_id + 3110, code=1110, headcount=0))

    wpa(hp_1(id=base_id + 11101, code=11101, headcount=1))
    wpa(hp_2(id=base_id + 21101, code=11101, headcount=1))
    wpa(HeadcountPositionFactory(
        id=base_id + 31101,
        code=11101,
        headcount=0,
        prev_index=2,
        index=3,
        next_index=4,
        replacement_type=REPLACEMENT_TYPE.HAS_REPLACEMENT_AND_HC_IS_BUSY,
        department=dep,
    ))
    wpa(HeadcountPositionFactory(
        id=base_id + 41101,
        code=11101,
        headcount=1,
        prev_index=3,
        index=4,
        next_index=None,
        replacement_type=REPLACEMENT_TYPE.HC_IS_BUSY,
        department=dep,
    ))

    return wrong_positions


@pytest.mark.django_db
def test_get_wrong_positions(wrong_positions):
    from staff.oebs.controllers.rolluppers.fix_zero_hc_cross import _get_wrong_positions

    result = _get_wrong_positions()
    assert len(result) == 28


@pytest.mark.django_db
def test_fixed_zero_hc_cross_gen(wrong_positions):
    from staff.oebs.controllers.rolluppers.fix_zero_hc_cross import _fixed_zero_hc_cross_gen

    wps = list(_fixed_zero_hc_cross_gen(wrong_positions))

    def get_tuple(p):
        return p.code, p.headcount, p.prev_index, p.index, p.next_index, p.replacement_type

    wps = set(get_tuple(p) for p in wps)

    result = {
        (100, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (100, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),

        (101, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (101, 1, None, 1, None, REPLACEMENT_TYPE.WO_REPLACEMENT),

        (110, 1, None, 1, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (110, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),

        (1001, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (1001, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (1001, 1, None, 1, None, REPLACEMENT_TYPE.WO_REPLACEMENT),

        (1010, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (1010, 1, None, 1, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (1010, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),

        (1011, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (1011, 1, None, 1, 2, REPLACEMENT_TYPE.HAS_REPLACEMENT),
        (1011, 1, 1, 2, None, REPLACEMENT_TYPE.HC_IS_BUSY),

        (1100, 1, None, 1, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (1100, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (1100, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),

        (1101, 1, None, 1, 2, REPLACEMENT_TYPE.HAS_REPLACEMENT),
        (1101, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (1101, 1, 1, 2, None, REPLACEMENT_TYPE.HC_IS_BUSY),

        (1110, 1, 1, 2, None, REPLACEMENT_TYPE.HC_IS_BUSY),
        (1110, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),

        (11101, 1, 1, 2, 3, REPLACEMENT_TYPE.HAS_REPLACEMENT_AND_HC_IS_BUSY),
        (11101, 0, None, None, None, REPLACEMENT_TYPE.WO_REPLACEMENT),
        (11101, 1, 2, 3, None, REPLACEMENT_TYPE.HC_IS_BUSY),
    }

    for pt in sorted(wps, key=lambda p: p[0]):
        assert pt in result

    assert len(wps) == len(result)
