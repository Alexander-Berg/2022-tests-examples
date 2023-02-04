import unittest
from decimal import Decimal

from billing.dcs.dcs.compare.bua import DiffsFetcher
from billing.dcs.dcs.utils.common import Struct
from billing.dcs.dcs import constants


# noinspection PyTypeChecker
def row(ships_qty, act_qty, unified_account_id,
        service_code=constants.DIRECT_PCS_BILLING_PRODUCT_ID,
        is_converted=0, old_correction_qty=0, hang_correction_qty=0,
        reversed_acted_correction_qty=0):
    return Struct({'unified_account_id': unified_account_id,
                   'service_code': service_code,
                   'ships_qty': ships_qty, 'act_qty': act_qty,
                   'is_converted': is_converted,
                   'old_correction_qty': old_correction_qty,
                   'hang_correction_qty': hang_correction_qty,
                   'reversed_acted_correction_qty': reversed_acted_correction_qty})


class FilterResultRowsTestCase(unittest.TestCase):
    def perform_test(self, input_rows, expected_result):
        self.assertListEqual(
            list(DiffsFetcher._filter_result_rows(input_rows)),
            expected_result
        )

    def test_is_coincide(self):
        self.perform_test(
            [row(10, 15, 1), row(10, 15, 1), row(20, 10, 1)],
            []
        )

    def test_partly_coincide(self):
        self.perform_test(
            [row(10, 15, 1), row(10, 15, 1), row(20, 10, 1), row(3, 5, 2)],
            [row(3, 5, 2)]
        )

    def test_not_coincide_within_unified_account(self):
        rows = [row(10, 15, 1), row(10, 15, 1), row(10, 20, 1)]
        self.perform_test(rows, rows)

    def test_not_coincide_without_unified_accounts(self):
        rows = [row(10, 15, None), row(10, 15, None), row(20, 10, None)]
        self.perform_test(rows, rows)

    def test_mixed_products(self):
        self.perform_test(
            [row(10, 15, 1,
                 service_code=constants.DIRECT_PCS_BILLING_PRODUCT_ID),
             row(15 * constants.PCS_RUB_RATE, 10 * constants.PCS_RUB_RATE, 1,
                 service_code=constants.DIRECT_RUB_BILLING_PRODUCT_ID)],
            []
        )

    def test_rounding_error_coincide(self):
        rub_to_pcs = lambda qty: \
            (Decimal(qty) / constants.PCS_RUB_RATE).quantize(Decimal('0.000001'))
        self.perform_test(
            [row(rub_to_pcs(10), rub_to_pcs(15), 1,
                 service_code=constants.DIRECT_PCS_BILLING_PRODUCT_ID,
                 is_converted=1),
             row(15, 10, 1,
                 service_code=constants.DIRECT_RUB_BILLING_PRODUCT_ID)],
            []
        )

    def test_all_rub_orders_no_qty_conversion(self):
        """ Based on real data """
        rows = [row(Decimal('4091.0334'), Decimal('4087.7294'), 1,
                    service_code=constants.DIRECT_RUB_BILLING_PRODUCT_ID),
                row(Decimal('1173.274'), Decimal('1176.696'), 1,
                    service_code=constants.DIRECT_RUB_BILLING_PRODUCT_ID)]
        self.perform_test(rows, rows)

    def test_unmanageable_products_combination(self):
        rows = [
            row(10, 15, 1, service_code=constants.DIRECT_PCS_BILLING_PRODUCT_ID),
            row(15, 10, 1, service_code=666)
        ]
        self.perform_test(rows, rows)

    def test_is_coincide_with_corrections(self):
        self.perform_test(
            [row(10, 15, 1, hang_correction_qty=10),
             row(10, 15, 1, reversed_acted_correction_qty=-10),
             row(20, 10, 1)],
            []
        )
