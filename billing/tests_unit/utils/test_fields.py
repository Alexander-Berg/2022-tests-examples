# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

from decimal import Decimal as D
import pytest
from hamcrest import assert_that, equal_to

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.utils import fields as yb_fields


class TestCaseNormalFixedField(TestCaseApiAppBase):
    @pytest.mark.parametrize(
        'decimals, in_val, out_val',
        [
            (6, D('0.001'), '0.001'),
            (6, D('0.001000'), '0.001'),
            (6, D('01'), '1'),
            (6, D('1.00'), '1'),
            (6, D('0.0000001'), '0'),
            (6, D('0.000001'), '0.000001'),
            (6, D('1e-7'), '0'),
            (6, D('1e-6'), '0.000001'),
            (6, D('10e4'), '100000'),
            (6, D('55.5555555'), '55.555556'),
            (6, D('55.5555545'), '55.555555'),
            (2, D('11.845'), '11.85'),
        ],
    )
    def test_normal_fixed_field(self, decimals, in_val, out_val):
        assert_that(
            yb_fields.NormalFixed(decimals).format(in_val),
            equal_to(out_val),
        )
