# coding: utf-8

import unittest

from billing.dcs.dcs import constants
from billing.dcs.dcs.compare.uao import convert_child_qty_to_parent_product


class MainTestCase(unittest.TestCase):
    def test_convert_child_qty_to_parent_product(self):
        self.assertEqual(
            convert_child_qty_to_parent_product(
                child_product_id=1,
                root_product_id=1,
                child_qty=666
            ),
            666
        )

        self.assertRaises(
            AssertionError,
            convert_child_qty_to_parent_product,
            constants.DIRECT_RUB_BILLING_PRODUCT_ID + 1,
            constants.DIRECT_PCS_BILLING_PRODUCT_ID + 2,
            666
        )

        self.assertEqual(
            convert_child_qty_to_parent_product(
                child_product_id=constants.DIRECT_RUB_BILLING_PRODUCT_ID,
                root_product_id=constants.DIRECT_PCS_BILLING_PRODUCT_ID,
                child_qty=60
            ),
            60 / constants.PCS_RUB_RATE
        )

        self.assertEqual(
            convert_child_qty_to_parent_product(
                child_product_id=constants.DIRECT_PCS_BILLING_PRODUCT_ID,
                root_product_id=constants.DIRECT_RUB_BILLING_PRODUCT_ID,
                child_qty=2
            ),
            2 * constants.PCS_RUB_RATE
        )
