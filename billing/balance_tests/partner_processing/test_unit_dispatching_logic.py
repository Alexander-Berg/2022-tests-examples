# -*- coding: utf-8 -*-

import pytest

import balance.exc as exc
from balance.utils.partner_processing.unit_dispatching_logic import BaseUnit, UnitDispatcher


def test_base_unit_registration():
    MODULE_NAME = 'test_unit_registration'
    TEST_UNIT_TYPE = 'test_unit_type'
    TEST_UNIT_NAME = 'test_unit'

    class _TestUnitBase(BaseUnit):
        module_name = MODULE_NAME

    class _TestUnitType(_TestUnitBase):
        unit_type = TEST_UNIT_TYPE

    class _TestUnit(_TestUnitType):
        unit_name = TEST_UNIT_NAME

    assert len(UnitDispatcher.units_mapping[MODULE_NAME.upper()]) == 1
    assert len(UnitDispatcher.units_mapping[MODULE_NAME.upper()][TEST_UNIT_TYPE.upper()]) == 1
    assert UnitDispatcher.units_mapping[MODULE_NAME.upper()][TEST_UNIT_TYPE.upper()][TEST_UNIT_NAME.upper()] is _TestUnit


def test_unit_dispatcher():
    MODULE_NAME = 'test_unit_dispatcher'
    TEST_UNIT_TYPE = 'test_unit_type'

    # регистрируем первый юнит
    TEST_UNIT_NAME = 'test_unit'
    class _TestUnit(object):
        pass

    UnitDispatcher.register_unit(MODULE_NAME, TEST_UNIT_TYPE, TEST_UNIT_NAME, _TestUnit)
    assert len(UnitDispatcher.units_mapping[MODULE_NAME.upper()]) == 1
    assert len(UnitDispatcher.units_mapping[MODULE_NAME.upper()][TEST_UNIT_TYPE.upper()]) == 1
    assert UnitDispatcher.units_mapping[MODULE_NAME.upper()][TEST_UNIT_TYPE.upper()][TEST_UNIT_NAME.upper()] \
           is _TestUnit

    # регистрируем второй юнит
    NEW_TEST_UNIT_NAME = 'new_test_unit'
    class _NewTestUnit(object):
        pass

    UnitDispatcher.register_unit(MODULE_NAME, TEST_UNIT_TYPE, NEW_TEST_UNIT_NAME, _NewTestUnit)
    assert len(UnitDispatcher.units_mapping[MODULE_NAME.upper()]) == 1
    assert len(UnitDispatcher.units_mapping[MODULE_NAME.upper()][TEST_UNIT_TYPE.upper()]) == 2
    assert \
        UnitDispatcher.units_mapping[MODULE_NAME.upper()][TEST_UNIT_TYPE.upper()][NEW_TEST_UNIT_NAME.upper()] \
        is _NewTestUnit

    # регистрируем юнит с таким же именем
    NEW_TEST_UNIT_NAME = 'new_test_unit'
    class _NewTestUnit2(object):
        pass

    with pytest.raises(exc.COMMON_PARTNER_PROCESSING_DISPATCHER_EXCEPTION,
                       match='Partner processing dispatcher error: module_name: TEST_UNIT_DISPATCHER unit_type: '
                             'TEST_UNIT_TYPE unit_name: NEW_TEST_UNIT is already registered for class _NewTestUnit'):
        UnitDispatcher.register_unit(MODULE_NAME, TEST_UNIT_TYPE, NEW_TEST_UNIT_NAME, _NewTestUnit2)
    assert len(UnitDispatcher.units_mapping[MODULE_NAME.upper()]) == 1
    assert len(UnitDispatcher.units_mapping[MODULE_NAME.upper()][TEST_UNIT_TYPE.upper()]) == 2
    assert \
        UnitDispatcher.units_mapping[MODULE_NAME.upper()][TEST_UNIT_TYPE.upper()][NEW_TEST_UNIT_NAME.upper()] \
        is _NewTestUnit
