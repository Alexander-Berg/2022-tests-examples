# coding: utf-8

import unittest
from decimal import Decimal

from billing.dcs.dcs.compare.zbb import RecentOperationsQtyDiffAutoAnalyzer
from billing.dcs.dcs.utils.common import Struct


DiffChecker = RecentOperationsQtyDiffAutoAnalyzer.DiffChecker


def diff(balance_qty=0,
         bk_qty=0,
         balance_money=0,
         bk_money=0,
         balance_is_converted=False,
         balance_is_money_unit=False):
    return Struct(
        balance_qty=Decimal(balance_qty),
        bk_qty=Decimal(bk_qty),
        balance_money=Decimal(balance_money),
        bk_money=Decimal(bk_money),
        balance_is_converted=balance_is_converted,
        balance_is_money_unit=balance_is_money_unit
    )


class DiffCheckerTestCase(unittest.TestCase):
    def test_absolute_diff_amount_is_absolute(self):
        self.assertEquals(
            DiffChecker(
                diff(balance_qty=10, bk_qty=15),
                []
            ).absolute_diff_amount,
            5
        )
        self.assertEquals(
            DiffChecker(
                diff(balance_qty=15, bk_qty=10),
                []
            ).absolute_diff_amount,
            5
        )

    def test_absolute_diff_amount_converted_order(self):
        self.assertEquals(
            DiffChecker(
                diff(balance_qty=10, bk_qty=15,
                     balance_money=0, bk_money=0,
                     balance_is_converted=True),
                []
            ).absolute_diff_amount,
            5
        )
        self.assertEquals(
            DiffChecker(
                diff(balance_qty=10, bk_qty=15,
                     balance_money=30, bk_money=45,
                     balance_is_converted=True),
                []
            ).absolute_diff_amount,
            Decimal('5.5')
        )

    def test_absolute_diff_amount_is_money_unit(self):
        # qty must be ignored
        self.assertEquals(
            DiffChecker(
                diff(balance_qty=100, bk_qty=500,
                     balance_money=10, bk_money=10,
                     balance_is_money_unit=True),
                []
            ).absolute_diff_amount,
            0
        )
        self.assertEquals(
            DiffChecker(
                diff(balance_money=10, bk_money=15,
                     balance_is_money_unit=True),
                []
            ).absolute_diff_amount,
            5
        )

    def test_is_covered(self):
        # operations are summing
        self.assertTrue(
            DiffChecker(
                diff(balance_qty=10, bk_qty=15), [2, 3]
            ).is_covered()
        )
        # abs is used
        self.assertTrue(
            DiffChecker(
                diff(balance_qty=10, bk_qty=15), [-5]
            ).is_covered()
        )
        # abs after summing, not before
        self.assertFalse(
            DiffChecker(
                diff(balance_qty=10, bk_qty=15), [2, -3]
            ).is_covered()
        )

    def test_acceptable_pcs_diff(self):
        self.assertTrue(
            DiffChecker(
                diff(balance_qty=10, bk_qty=15, balance_is_converted=True),
                [Decimal(5) - DiffChecker._acceptable_pcs_diff * Decimal('0.9')]
            ).is_covered()
        )
        self.assertFalse(
            DiffChecker(
                diff(balance_qty=10, bk_qty=15, balance_is_converted=True),
                [Decimal(5) - DiffChecker._acceptable_pcs_diff * Decimal('1.1')]
            ).is_covered()
        )
        # used only for converted orders
        self.assertFalse(
            DiffChecker(
                diff(balance_qty=10, bk_qty=15,
                     balance_is_converted=False,
                     balance_is_money_unit=True),
                [Decimal(5) - DiffChecker._acceptable_pcs_diff * Decimal('0.9')]
            ).is_covered()
        )
