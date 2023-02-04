# -*- coding: utf-8 -*-

import datetime as dt
from decimal import Decimal as D

import balance.muzzle_util as ut
from balance.processors.partner_netting import ContractNettingLogicBase
from butils import logger

log = logger.get_logger('test_netting')


def test_netting_periods():
    col0 = ut.Struct(netting_pct=D(0))
    col100 = ut.Struct(netting_pct=D(100))
    col200 = ut.Struct(netting_pct=D(200))
    colNone = ut.Struct(netting_pct=None)
    colNoAttr = ut.Struct()

    test_data0 = [
        (dt.datetime(2017, 5,  8), 1, col100),
        (dt.datetime(2017, 8, 22), 0, colNoAttr),
        (dt.datetime(2017, 8, 30), 1, col200),
        (dt.datetime(2017, 9,  8), 0, colNone),
        (dt.datetime(2017, 9, 11), 1, col0),
    ]

    res0 = ContractNettingLogicBase.construct_netting_periods(test_data0)

    assert res0 == [
        ut.Struct(start_dt=dt.datetime(2017, 5,  8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9,  8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 11), end_dt=None,                     netting_pct=D(0)),
    ], res0


    test_data1 = [
        (dt.datetime(2017, 5,  8), 1, col100),
        (dt.datetime(2017, 8, 22), 0, colNoAttr),
        (dt.datetime(2017, 8, 30), 1, col200),
        (dt.datetime(2017, 9,  8), 0, colNone),
    ]

    res1 = ContractNettingLogicBase.construct_netting_periods(test_data1)

    assert res1 == [
        ut.Struct(start_dt=dt.datetime(2017, 5,  8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9,  8), netting_pct=D(200)),
    ], res1


    test_data2 = [
        (dt.datetime(2017, 5,  8), 0, col100),
        (dt.datetime(2017, 8, 22), 0, col200),
    ]

    res2 = ContractNettingLogicBase.construct_netting_periods(test_data2)

    assert res2 == [], res2


    test_data3 = [
        (dt.datetime(2017, 5,  8), 0, col100),
        (dt.datetime(2017, 8, 22), 0, colNoAttr),
        (dt.datetime(2017, 8, 30), 0, col200),
        (dt.datetime(2017, 9,  8), 1, colNone),
    ]

    res3 = ContractNettingLogicBase.construct_netting_periods(test_data3)

    assert res3 == [
        ut.Struct(start_dt=dt.datetime(2017, 9,  8), end_dt=None, netting_pct=D(0)),
    ], res3


    test_data4 = [
        (dt.datetime(2017, 5,  8), 1, col100),
        (dt.datetime(2017, 8, 22), 1, col200),
    ]

    res4 = ContractNettingLogicBase.construct_netting_periods(test_data4)

    assert res4 == [
        ut.Struct(start_dt=dt.datetime(2017, 5,  8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 22), end_dt=None,                     netting_pct=D(200)),
    ], res4


    test_data5 = [
        (dt.datetime(2017, 5,  8), 0, col100),
        (dt.datetime(2017, 8, 22), 0, colNoAttr),
        (dt.datetime(2017, 8, 30), 1, col200),
        (dt.datetime(2017, 9,  8), 0, colNone),
        (dt.datetime(2017, 9, 10), 0, colNoAttr),
        (dt.datetime(2017, 9, 12), 1, col100),
        (dt.datetime(2017, 9, 12), 1, col200),
    ]

    res5 = ContractNettingLogicBase.construct_netting_periods(test_data5)

    assert res5 == [
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9,  8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 12), end_dt=dt.datetime(2017, 9, 12), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 12), end_dt=None,                     netting_pct=D(200)),
    ], res5


def test_trunc_netting_periods():
    test_data0 = [
        ut.Struct(start_dt=dt.datetime(2017, 5, 8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9, 8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 11), end_dt=None, netting_pct=D(0)),
    ]

    res0 = ContractNettingLogicBase.trunc_netting_periods(test_data0, start_dt=dt.datetime(2017, 5, 8),
                                                          end_dt=dt.datetime(2017, 9, 12))

    assert res0 == [
        ut.Struct(start_dt=dt.datetime(2017, 5, 8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9, 8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 11), end_dt=dt.datetime(2017, 9, 12), netting_pct=D(0)),
    ], res0

    test_data1 = [
        ut.Struct(start_dt=dt.datetime(2017, 5, 8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9, 8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 11), end_dt=None, netting_pct=D(0)),
    ]

    res1 = ContractNettingLogicBase.trunc_netting_periods(test_data1, start_dt=dt.datetime(2017, 5, 9),
                                                          end_dt=dt.datetime(2017, 9, 8))

    assert res1 == [
        ut.Struct(start_dt=dt.datetime(2017, 5, 9), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9, 8), netting_pct=D(200)),
    ], res1

    test_data2 = [
        ut.Struct(start_dt=dt.datetime(2017, 5, 8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9, 8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 11), end_dt=None, netting_pct=D(0)),
    ]

    res2 = ContractNettingLogicBase.trunc_netting_periods(test_data2, start_dt=dt.datetime(2017, 3, 9),
                                                          end_dt=dt.datetime(2017, 5, 9))

    assert res2 == [
        ut.Struct(start_dt=dt.datetime(2017, 5, 8), end_dt=dt.datetime(2017, 5, 9), netting_pct=D(100)),
    ], res2

    test_data3 = [
        ut.Struct(start_dt=dt.datetime(2017, 5, 8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9, 8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 11), end_dt=None, netting_pct=D(0)),
    ]

    res3 = ContractNettingLogicBase.trunc_netting_periods(test_data3, start_dt=dt.datetime(2017, 9, 12),
                                                          end_dt=dt.datetime(2017, 9, 13))

    assert res3 == [
        ut.Struct(start_dt=dt.datetime(2017, 9, 12), end_dt=dt.datetime(2017, 9, 13), netting_pct=D(0)),
    ], res3

    test_data4 = [
        ut.Struct(start_dt=dt.datetime(2017, 5, 8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9, 8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 11), end_dt=None, netting_pct=D(0)),
    ]

    res4 = ContractNettingLogicBase.trunc_netting_periods(test_data4, start_dt=dt.datetime(2017, 5, 7),
                                                          end_dt=dt.datetime(2017, 5, 8))

    assert res4 == [], res4

    test_data5 = [
        ut.Struct(start_dt=dt.datetime(2017, 5, 8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9, 8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 11), end_dt=dt.datetime(2017, 9, 15), netting_pct=D(0)),
    ]

    res5 = ContractNettingLogicBase.trunc_netting_periods(test_data5, start_dt=dt.datetime(2017, 9, 16),
                                                          end_dt=dt.datetime(2017, 9, 17))

    assert res5 == [], res5

    test_data6 = [
        ut.Struct(start_dt=dt.datetime(2017, 5, 8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9, 8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 11), end_dt=None, netting_pct=D(0)),
    ]

    res6 = ContractNettingLogicBase.trunc_netting_periods(test_data6, start_dt=dt.datetime(2017, 8, 22),
                                                          end_dt=dt.datetime(2017, 8, 24))

    assert res6 == [], res6

    test_data7 = [
        ut.Struct(start_dt=dt.datetime(2017, 5, 8), end_dt=dt.datetime(2017, 8, 22), netting_pct=D(100)),
        ut.Struct(start_dt=dt.datetime(2017, 8, 30), end_dt=dt.datetime(2017, 9, 8), netting_pct=D(200)),
        ut.Struct(start_dt=dt.datetime(2017, 9, 11), end_dt=dt.datetime(2017, 9, 15), netting_pct=D(0)),
    ]

    res7 = ContractNettingLogicBase.trunc_netting_periods(test_data7, start_dt=dt.datetime(2017, 9, 14),
                                                          end_dt=dt.datetime(2017, 9, 16))

    assert res7 == [
        ut.Struct(start_dt=dt.datetime(2017, 9, 14), end_dt=dt.datetime(2017, 9, 15), netting_pct=D(0)),
    ], res7
