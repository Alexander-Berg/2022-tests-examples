# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import json
from decimal import Decimal

from butils.decimal_unit import DecimalUnit
from hamcrest import assert_that, equal_to, calling, raises

from brest.core.json_encoder import BrestJSONEncoder, FakeFloat
from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class TestCaseJsonEncoder(TestCaseApiAppBase):
    def test_all_data_types(self):
        from yb_snout_api.utils.enum import Enum

        class TestType(Enum):
            KEY1 = 'VAL_1'

        class InvalidObject(object):
            to_dict = None

        class ValidObject(object):
            f = 'some value'

            def to_dict(self):
                return u'{}'.format(self.f)

        def serialize(o):
            return json.dumps(o, cls=BrestJSONEncoder)

        d1 = DecimalUnit('123.456')

        assert_that(
            serialize(d1),
            equal_to(json.dumps(FakeFloat(Decimal(d1)))),
        )

        d2 = Decimal('123.456')

        assert_that(
            serialize(d2),
            equal_to(json.dumps(FakeFloat(d2))),
        )

        assert_that(
            serialize(TestType.KEY1),
            equal_to('"KEY1"'),
        )

        assert_that(
            calling(serialize).with_args(InvalidObject()),
            raises(TypeError),
        )

        valid_obj = ValidObject()

        assert_that(
            serialize(valid_obj),
            equal_to(u'"{}"'.format(valid_obj.f)),
        )
