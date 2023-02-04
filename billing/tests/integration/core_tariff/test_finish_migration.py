import pytest
import itertools
import arrow

from yt.wrapper import (
    ypath_join,
)

from billing.log_tariffication.py.jobs.core_tariff import finish_migration
from billing.log_tariffication.py.lib.constants import (
    LOG_TARIFF_META_ATTR,
    LOG_INTERVAL_KEY,
    CORRECTIONS_LOG_INTERVAL_KEY,
    PREVIOUS_RUN_ID_KEY,
    RUN_ID_KEY,
)
from billing.library.python.yt_utils.test_utils.utils import (
    create_subdirectory,
)
from billing.library.python.logfeller_utils.log_interval import (
    LogInterval,
    Subinterval,
)
from billing.log_tariffication.py.tests.constants import (
    PREV_RUN_ID,
    CURR_RUN_ID,
)

TO_DT = arrow.Arrow(2020, 7, 7, tzinfo='Europe/Moscow').int_timestamp
TABLE = '//home/balance/migration/initial/2020-12-15'


class OrderData:
    def __init__(self, orders, log_interval, correction_interval):
        self.orders = orders
        self.log_interval = log_interval
        self.correction_interval = correction_interval

    @property
    def dyntable(self):
        return [o.dyntable for o in self.orders]

    @property
    def untariffed(self):
        return list(itertools.chain(*[o.untariffed for o in self.orders]))

    @property
    def unprocessed(self):
        return list(itertools.chain(*[o.unprocessed for o in self.orders]))

    def for_request(self):
        return {
            LOG_TARIFF_META_ATTR: {
                LOG_INTERVAL_KEY: self.log_interval.to_meta(),
                CORRECTIONS_LOG_INTERVAL_KEY: self.correction_interval.to_meta(),
                RUN_ID_KEY: CURR_RUN_ID,
                PREVIOUS_RUN_ID_KEY: PREV_RUN_ID,
                'table': TABLE,
            },
            'dyntable': self.dyntable,
            'untariffed': self.untariffed,
            'unprocessed_events': self.unprocessed,
        }


class OrderBuilder:
    def __init__(self, s_id, so_id, eso_id):
        self.s_id = s_id
        self.so_id = so_id
        self.eso_id = eso_id

        self.untariffed = []
        self.unprocessed = []

    @property
    def dyntable(self):
        return {
            'ServiceID': self.s_id,
            'EffectiveServiceOrderID': self.eso_id,
            'state': f'0:{self.so_id}',
        }

    def add_untariffed(self, cur_id, qty):
        self.untariffed.append({
            'ServiceID': self.s_id,
            'ServiceOrderID': self.so_id,
            'EffectiveServiceOrderID': self.eso_id,
            'CurrencyID': cur_id,
            'ProductID': 123,
            'BillableEventCostCur': qty,
            'EventTime': TO_DT,
        })

    def add_unprocessed(self, c_id, t_qty, t_sum, c_qty, c_sum):
        self.unprocessed.append({
            'ServiceID': self.so_id,
            'ServiceOrderID': self.so_id,
            'EffectiveServiceOrderID': self.eso_id,
            'consume_id': c_id,
            'ProductID': 123,
            'tariff_dt': TO_DT,
            'tariffed_qty': t_qty,
            'tariffed_sum': t_sum,
            'coeff_qty': c_qty,
            'coeff_sum': c_sum,
        })


@pytest.fixture(name='dyntable_dir')
def create_dyntable_dir(yt_client, migration_results_dir):
    return create_subdirectory(yt_client, migration_results_dir, 'dyntable')


@pytest.fixture(name='untariffed_dir')
def create_untariffed_dir(yt_client, migration_results_dir):
    return create_subdirectory(yt_client, migration_results_dir, 'untariffed')


@pytest.fixture(name='unprocessed_dir')
def create_unprocessed_dir(yt_client, migration_results_dir):
    return create_subdirectory(yt_client, migration_results_dir, 'unprocessed')


@pytest.fixture(name='correction_interval')
def create_correction_interval():
    return LogInterval([Subinterval('migr', 'direct', 0, 1, 2)])


@pytest.fixture(name='log_interval')
def create_log_interval():
    return LogInterval([Subinterval('hahn', 'direct', 0, 10, 20)])


def get_results(dyntable_path, processed_params, yt_client, yt_root,
                dyntable_dir=None, untariffed_dir=None, unprocessed_dir=None,
                w_metadata=False):
    res = {
        'dyntable_path': dyntable_path.split(yt_root)[-1],
        'processed_params': processed_params,
    }
    if dyntable_dir:
        res['dyntable'] = list(yt_client.read_table(ypath_join(dyntable_dir, CURR_RUN_ID)))
    if untariffed_dir:
        res['untariffed'] = list(yt_client.read_table(ypath_join(untariffed_dir, CURR_RUN_ID)))
    if unprocessed_dir:
        res['unprocessed'] = list(yt_client.read_table(ypath_join(unprocessed_dir, CURR_RUN_ID)))
    if w_metadata:
        res['dyntable_metadata'] = yt_client.get(ypath_join(dyntable_dir, CURR_RUN_ID, '@' + LOG_TARIFF_META_ATTR))
        res['untariffed_metadata'] = yt_client.get(ypath_join(untariffed_dir, CURR_RUN_ID, '@' + LOG_TARIFF_META_ATTR))
        res['unprocessed_metadata'] = yt_client.get(ypath_join(unprocessed_dir, CURR_RUN_ID, '@' + LOG_TARIFF_META_ATTR))
    return res


def test_base(yt_client, yt_root, dyntable_dir, untariffed_dir, unprocessed_dir, correction_interval, log_interval):
    order_1 = OrderBuilder(7, 1, 10)
    order_1.add_untariffed(1, 5.66)
    order_1.add_untariffed(2, 3.1234)
    order_1.add_unprocessed(12345678, 3.333, 1.0, 3.333, 1.0)
    order_1.add_unprocessed(87654321, 6.666666, 2.0, 6.666666, 2.0)

    order_2 = OrderBuilder(7, 2, 10)
    order_2.add_untariffed(2, 10.0)
    order_2.add_untariffed(3, 20.0)
    order_2.add_untariffed(3, 30.0)
    order_2.add_unprocessed(99999999, 1.0, 2.0, 3.0, 4.0)
    order_2.add_unprocessed(11111111, 5.0, 6.0, 7.0, 8.0)

    order_3 = OrderBuilder(7, 1, 20)
    order_3.add_untariffed(1, 10.0)

    data = OrderData([order_1, order_2, order_3], log_interval, correction_interval)

    with yt_client.Transaction(ping=False):
        dyntable_path, processed_params = finish_migration.run_job(
            yt_client,
            data=data.for_request(),
            res_dyntable_dir=dyntable_dir,
            res_untariffed_dir=untariffed_dir,
            res_unprocessed_events_dir=unprocessed_dir,
        )
    return get_results(dyntable_path, processed_params, yt_client, yt_root,
                       dyntable_dir, untariffed_dir, unprocessed_dir,
                       w_metadata=True)


def test_wo_data(yt_client, yt_root, dyntable_dir, untariffed_dir, unprocessed_dir, correction_interval, log_interval):
    order_1 = OrderBuilder(7, 1, 10)
    order_2 = OrderBuilder(7, 2, 10)
    order_3 = OrderBuilder(7, 1, 20)
    data = OrderData([order_1, order_2, order_3], log_interval, correction_interval)

    with yt_client.Transaction(ping=False):
        dyntable_path, processed_params = finish_migration.run_job(
            yt_client,
            data=data.for_request(),
            res_dyntable_dir=dyntable_dir,
            res_untariffed_dir=untariffed_dir,
            res_unprocessed_events_dir=unprocessed_dir,
        )

    return get_results(dyntable_path, processed_params, yt_client, yt_root,
                       dyntable_dir, untariffed_dir, unprocessed_dir)


def test_wo_dyntable(yt_client, yt_root, dyntable_dir, untariffed_dir, unprocessed_dir, correction_interval, log_interval):
    data = OrderData([], log_interval, correction_interval)

    with yt_client.Transaction(ping=False):
        dyntable_path, processed_params = finish_migration.run_job(
            yt_client,
            data=data.for_request(),
            res_dyntable_dir=dyntable_dir,
            res_untariffed_dir=untariffed_dir,
            res_unprocessed_events_dir=unprocessed_dir,
        )

    return get_results(dyntable_path, processed_params, yt_client, yt_root,
                       dyntable_dir, untariffed_dir, unprocessed_dir)


@pytest.mark.parametrize(
    'keys, exc_msg',
    [
        pytest.param((LOG_TARIFF_META_ATTR,), 'There is no log_tariff_meta in oltp_out.', id='log_tariff_meta'),
        pytest.param(('dyntable',), 'Dyntable data is required.', id='dyntable_data'),
        pytest.param((LOG_TARIFF_META_ATTR, LOG_INTERVAL_KEY), 'There is not log interval.', id='log_interval'),
        pytest.param((LOG_TARIFF_META_ATTR, CORRECTIONS_LOG_INTERVAL_KEY), 'There is not correction interval.', id='corrections'),
        pytest.param((LOG_TARIFF_META_ATTR, RUN_ID_KEY), 'There is not run_id.', id='run_id'),
        pytest.param((LOG_TARIFF_META_ATTR, 'table'), 'There is not migration table in metadata.', id='table'),
    ],
)
def test_wo_meta_keys(yt_client,
                      dyntable_dir, untariffed_dir, unprocessed_dir,
                      correction_interval, log_interval,
                      keys, exc_msg):
    data = OrderData([], log_interval, correction_interval).for_request()

    def _del_elem(data_item, keys):
        if len(keys) == 1:
            del data_item[keys[0]]
        else:
            _del_elem(data_item[keys[0]], keys[1:])

    _del_elem(data, keys)

    with pytest.raises(AssertionError) as exc_info:
        with yt_client.Transaction(ping=False):
            finish_migration.run_job(
                yt_client,
                data=data,
                res_dyntable_dir=dyntable_dir,
                res_untariffed_dir=untariffed_dir,
                res_unprocessed_events_dir=unprocessed_dir,
            )
    assert exc_msg in exc_info.value.args[0]
