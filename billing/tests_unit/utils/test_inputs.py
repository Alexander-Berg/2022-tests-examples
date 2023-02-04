# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from builtins import str as text
from future import standard_library

standard_library.install_aliases()

import datetime

import pytest
from hamcrest import assert_that, equal_to
from typing import NewType
from decimal import Decimal as D

from balance import exc

from brest.core.typing import ObjectId, ObjectCharCode
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.utils import inputs as inputs_util


class TestCaseInputs(TestCaseApiAppBase):

    def test_object_id(self):
        TestId = NewType('TestId', ObjectId)
        test_id = 123
        assert_that(inputs_util.object_id(TestId)(test_id), equal_to(test_id))

    def test_object_char_code(self):
        TestCC = NewType('TestCC', ObjectCharCode)
        test_cc = 'abc'
        assert_that(inputs_util.object_char_code(TestCC)(test_cc), equal_to(test_cc))

    @pytest.mark.parametrize(
        'date',
        [
            '20180701000000',
            '20180701T00:00:00',
            '20180701 00:00:00',
            '2018-07-01T00:00:00',
            '2018-07-01 00:00:00',
            '01.07.2018 00:00:00',
            '2018-07-01',
            '20180701',
            '01.07.2018',
            '01.07.2018',
        ],
    )
    def test_date(self, date):
        base_date = datetime.datetime(2018, 7, 1, 0, 0, 0)
        assert_that(inputs_util.date(date), base_date)

    @pytest.mark.parametrize(
        'input_val, output_val, separator, ignored',
        [
            ('val1, val2, val3', ['val1', 'val2', 'val3'], ',', [' ']),
            ('val1, val2, val3', ['val1', 'val2', 'val3'], ' ', [',']),
            ('val1, val2, val3', ['val1,', 'val2,', 'val3'], ' ', []),
        ],
    )
    def test_list_from_str(self, input_val, output_val, separator, ignored):
        assert_that(inputs_util.list_from_str(text, separator, ignored)(input_val), equal_to(output_val))

    def test_phone_number(self):
        phone = '+7 999 888 9988'
        assert_that(inputs_util.phone_number(phone), phone)

    @pytest.mark.parametrize(
        'in_val, out_val',
        [
            ('12.34', D('12.34')),
            ('12,34', D('12.34')),
            (' 12,34 ', D('12.34')),
            (' 12,34a ', None),
            ('12 34', None),
            ('1.2a', None),
            ('balance', None),
        ],
    )
    def test_decimal(self, in_val, out_val):
        if out_val is not None:
            assert_that(inputs_util.decimal(in_val), out_val)
        else:
            with pytest.raises(exc.INVALID_PARAM) as exc_info:
                inputs_util.decimal(in_val)
            assert exc_info.value.msg == 'Invalid parameter for function: value %s is not Decimal' % in_val
