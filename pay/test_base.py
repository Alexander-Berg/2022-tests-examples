from .test_data_fields import test_clients, test_payments, test_data

from payplatform.spirit.match_receipts.lib.base import HAHN
from payplatform.spirit.match_receipts.lib.datatables.base import TablesJoin
from payplatform.spirit.match_receipts.lib.datatables.utils import eq


def test_join_tables():
    tables_join = TablesJoin(HAHN, test_payments)\
        .inner_join(test_clients, eq(test_payments.client_id, test_clients.id))\
        .left_join(test_data, eq(test_data.id, test_payments.payment_id))

    assert str(tables_join) == (
        '`//home_hahn/execution_history/time_string/test_payments` as test_payments\n'
        'inner join `//home_hahn/execution_history/time_string/test_clients` as test_clients on test_payments.`client_id` = test_clients.`id`\n'
        'left join `//home_hahn/execution_history/time_string/test_data` as test_data on test_data.`id` = test_payments.`payment_id`'
    )
