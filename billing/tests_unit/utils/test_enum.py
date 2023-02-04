# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

from hamcrest import assert_that, equal_to, calling, raises, not_none

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class TestCaseEnum(TestCaseApiAppBase):
    def test_int_enum_comparison(self):
        from yb_snout_api.utils.enum import IntEnum

        class TestIntType(IntEnum):
            KEY1 = 1
            KEY2 = 2

        def wrong_compare_with_none():
            return TestIntType.KEY1 > None or TestIntType.KEY1 < None

        assert_that(TestIntType.KEY2 > TestIntType.KEY1)
        assert_that(TestIntType.KEY1 < TestIntType.KEY2)
        assert_that(TestIntType.KEY1 != TestIntType.KEY2)
        assert_that(TestIntType.KEY1, not_none())
        assert_that(calling(wrong_compare_with_none), raises(TypeError))

    def test_as_list(self):
        from yb_snout_api.utils.enum import Enum, IntEnum, as_list as enum_as_list

        class TestType(Enum):
            KEY1 = 'VAL_1'
            KEY2 = 'VAL_2'

        class TestIntType(IntEnum):
            KEY1 = 1
            KEY2 = 2

        assert_that(enum_as_list(TestType), equal_to([TestType.KEY1, TestType.KEY2]))
        assert_that(enum_as_list(TestIntType), equal_to([TestIntType.KEY1, TestIntType.KEY2]))

    def test_as_input(self):
        from yb_snout_api.utils.enum import Enum, IntEnum
        from yb_snout_api.utils import inputs as inputs_util

        class TestType(Enum):
            KEY1 = 'VAL_1'

        class TestIntType(IntEnum):
            KEY1 = 1

        assert_that(inputs_util.enum(TestType)('KEY1'), equal_to(TestType.KEY1))
        assert_that(inputs_util.enum(TestIntType)('KEY1'), equal_to(TestIntType.KEY1))

    def test_enum_field(self):
        from yb_snout_api.utils.enum import Enum, IntEnum, as_list as enum_as_list
        from yb_snout_api.utils import fields as fields_util
        from flask_restplus import fields

        class TestType(Enum):
            KEY1 = 'VAL_1'

        class TestIntType(IntEnum):
            KEY1 = 1

        field_enum = fields_util.EnumField(enum=enum_as_list(TestType), required=True)

        assert_that(field_enum.format(TestType.KEY1), equal_to(TestType.KEY1))
        assert_that(calling(field_enum.format).with_args('BAD_KEY'), raises(fields.MarshallingError))

        field_int_enum = fields_util.EnumField(enum=enum_as_list(TestIntType), required=True)

        assert_that(field_int_enum.format(TestIntType.KEY1), equal_to(TestIntType.KEY1))
        assert_that(calling(field_int_enum.format).with_args('BAD_KEY'), raises(fields.MarshallingError))

    def test_converting_enum_field(self):
        from yb_snout_api.utils.enum import Enum, IntEnum
        from yb_snout_api.utils import fields as fields_util

        class TestType(Enum):
            KEY1 = 'VAL_1'

        class TestIntType(IntEnum):
            KEY1 = 1

        field_enum = fields_util.ConvertEnumField(enum=TestType, required=True)

        assert_that(field_enum.format(TestType.KEY1.value), equal_to(TestType.KEY1.name))
        assert_that(field_enum.format('BAD_KEY'), equal_to('UNKNOWN_VALUE'))

        field_int_enum = fields_util.ConvertEnumField(enum=TestIntType, required=True)

        assert_that(field_int_enum.format(TestIntType.KEY1.value), equal_to(TestIntType.KEY1.name))
        assert_that(field_int_enum.format('BAD_KEY'), equal_to('UNKNOWN_VALUE'))

    def test_json_encode(self):
        import json
        from brest.core.json_encoder import BrestJSONEncoder
        from yb_snout_api.utils.enum import Enum, IntEnum

        class TestType(Enum):
            KEY1 = 'VAL_1'

        class TestIntType(IntEnum):
            KEY1 = 1

        json_str = json.dumps({
            'TestEnum.KEY1': TestType.KEY1,
            'TestIntEnum.KEY1': TestIntType.KEY1,
        }, cls=BrestJSONEncoder, sort_keys=True)

        assert_that(json_str, equal_to('{"TestEnum.KEY1": "KEY1", "TestIntEnum.KEY1": "KEY1"}'))
