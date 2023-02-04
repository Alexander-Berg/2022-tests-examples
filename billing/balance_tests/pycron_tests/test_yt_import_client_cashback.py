# -*- coding: utf-8 -*-
import mock
import pytest
import contextlib
import datetime
from dateutil.relativedelta import relativedelta
import sqlalchemy as sa
import hamcrest as hm
from decimal import Decimal as D

from balance import constants as cst, mapper
from balance import muzzle_util as ut
from cluster_tools.yt_import_client_cashback import ImportClientCashback
from tests import object_builder as ob

pytestmark = [
    pytest.mark.cashback,
]

cst.ServiceId.MARKET = cst.ServiceId.MARKET


@pytest.fixture(name='client')
def create_client(session):
    return ob.ClientBuilder.construct(session)


@pytest.fixture(name='order')
def create_order(session, client, service_id=cst.ServiceId.DIRECT,
                 product_id=cst.DIRECT_PRODUCT_RUB_ID, **kw):
    return ob.OrderBuilder.construct(
        session,
        client=client,
        service_id=service_id,
        product_id=product_id,
        **kw
    )


def create_request(session, client, orders):
    return ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=client,
            rows=[
                ob.BasketItemBuilder(order=o, quantity=qty)
                for o, qty in orders
            ],
        ),
    )


@pytest.fixture(name='invoice')
def create_invoice(session, client, person=None, request_=None, **kwargs):
    request_ = request_ or create_request(session, client, [(create_order(session, client), D('1'))])
    return ob.InvoiceBuilder.construct(
        session,
        request=request_,
        person=person,
        **kwargs
    )


@contextlib.contextmanager
def yt_client_mock(data):
    mock_path = 'yt.wrapper.YtClient'

    def _get_table_data(path):
        path, start_index = path
        res = data.get(path, iter([]))
        if start_index is not None:
            [next(res) for _i in range(start_index)]
        return res

    with mock.patch(mock_path) as m:
        m.return_value.list.return_value = data.keys()
        m.return_value.read_table = _get_table_data
        yield m


@pytest.fixture
def base_data(client):
    return {
        '201901': iter([
            {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'currency': 'RUR', 'reward': '100',
             'auto_consume_enabled': 1}
        ]),
        '201902': iter([
            {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'currency': 'RUR', 'reward': '2',
             'auto_consume_enabled': True, 'cashback_months_to_live': 4},
        ]),
        '202001': iter([
            {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'currency': 'RUR', 'reward': '1',
             'auto_consume_enabled': True, 'cashback_months_to_live': 13},
        ]),
        '202002': iter([
            {'client_id': client.id, 'currency': 'USD', 'reward': '999.99999', 'auto_consume_enabled': 0},
            {'client_id': client.id, 'service_id': cst.ServiceId.MARKET, 'currency': 'RUR', 'reward': '0.0001',
             'auto_consume_enabled': True},
        ]),
    }


@pytest.fixture(autouse=True)
def yt_ypath_join_mock():
    mock_path = 'yt.wrapper.ypath_join'

    def _get_path(dir_path, table):
        return table

    with mock.patch(mock_path, _get_path):
        yield


@pytest.fixture(autouse=True)
def yt_table_path_mock():
    mock_path = 'yt.wrapper.TablePath'

    def _get_path(table, start_index=None):
        return table, start_index

    with mock.patch(mock_path, _get_path) as m:
        yield m


@pytest.fixture(autouse=True)
def yt_helpers_mock():
    mock_path = 'balance.utils.yt_helpers.get_token'
    with mock.patch(mock_path) as m:
        yield m


def test_import_new_data(session, client, base_data):
    session.config.__dict__['ALLOWED_CASHBACK_NOTIFICATIONS'] = 1

    with yt_client_mock(base_data):
        ImportClientCashback(session).import_from_yt()

    cashbacks = client.cashbacks.values()
    hm.assert_that(
        cashbacks,
        hm.contains_inanyorder(
            hm.has_properties(
                {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'RUB', 'bonus': D('100')}),
            hm.has_properties(
                {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'RUB', 'bonus': D('2'),
                 'start_dt': ut.trunc_date(datetime.datetime.now()),
                 'finish_dt': ut.trunc_date(datetime.datetime.now()) + relativedelta(months=4)}),
            hm.has_properties(
                {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'RUB', 'bonus': D('1'),
                 'start_dt': ut.trunc_date(datetime.datetime.now()),
                 'finish_dt': ut.trunc_date(datetime.datetime.now()) + relativedelta(months=13)}),
            hm.has_properties({'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'USD',
                               'bonus': D('999.99999')}),
            hm.has_properties({'client_id': client.id, 'service_id': cst.ServiceId.MARKET, 'iso_currency': 'RUB',
                               'bonus': D('0.0001')}),
        ),
    )

    hm.assert_that(
        client.cashback_settings.values(),
        hm.contains_inanyorder(
            hm.has_properties({'service_id': cst.ServiceId.DIRECT, 'is_auto_charge_enabled': False}),
            hm.has_properties({'service_id': cst.ServiceId.MARKET, 'is_auto_charge_enabled': True})
        )
    )

    assert session.config.get('CLIENT_CASHBACK_LAST_IMPORTED_TABLE_NAME', '') == '202002'
    assert session.config.get('CLIENT_CASHBACK_LAST_IMPORTED_ROW') is None

    notification = (
        session
            .execute(
            'select * from bo.t_object_notification where opcode = :opcode and object_id in (:id_1, :id_2, :id_3)',
            {
                'opcode': cst.NOTIFY_CLIENT_CASHBACK_OPCODE,
                'id_1': cashbacks[0].id,
                'id_2': cashbacks[1].id,
                'id_3': cashbacks[2].id,
            },
        )
            .fetchall()
    )
    assert len(notification) == 3


def test_import_existing_cashback(session, client, base_data):
    ob.ClientCashbackBuilder.construct(
        session, client=client, service_id=cst.ServiceId.DIRECT, iso_currency='RUB', bonus=D('666')
    )
    ob.ClientCashbackBuilder.construct(
        session, client=client, service_id=cst.ServiceId.DIRECT, iso_currency='RUB', bonus=D('20'),
        start_dt=datetime.date.today() - relativedelta(months=2),
        finish_dt=datetime.date.today() + relativedelta(months=4)
    )  # finish_dt совпадает с первым из импортируемых
    ob.ClientCashbackBuilder.construct(
        session, client=client, service_id=cst.ServiceId.DIRECT, iso_currency='RUB', bonus=D('100'),
        start_dt=datetime.date.today(), finish_dt=datetime.date.today() + relativedelta(months=13)
    )  # start_dt совпадает с первым из импортируемых, со вторым импортируемым совпадает start_dt и finish_dt
    ob.ClientCashbackBuilder.construct(
        session, client=client, service_id=cst.ServiceId.DIRECT, iso_currency='USD', bonus=D('0.00001')
    )

    dt_before_update = datetime.datetime.now().replace(microsecond=0)

    with yt_client_mock(base_data):
        ImportClientCashback(session).import_from_yt()

    hm.assert_that(
        client.cashbacks.values(),
        hm.contains_inanyorder(
            hm.has_properties(
                {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'RUB', 'bonus': D('766'),
                 'update_dt': hm.greater_than_or_equal_to(dt_before_update)}
            ),
            hm.has_properties(
                {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'RUB', 'bonus': D('20'),
                 'start_dt': ut.trunc_date(datetime.datetime.now()) - relativedelta(months=2),
                 'finish_dt': ut.trunc_date(datetime.datetime.now()) + relativedelta(months=4)}),
            hm.has_properties(
                {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'RUB', 'bonus': D('2'),
                 'start_dt': ut.trunc_date(datetime.datetime.now()),
                 'finish_dt': ut.trunc_date(datetime.datetime.now()) + relativedelta(months=4)}),
            hm.has_properties(
                {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'RUB', 'bonus': D('101'),
                 'start_dt': ut.trunc_date(datetime.datetime.now()),
                 'finish_dt': ut.trunc_date(datetime.datetime.now()) + relativedelta(months=13)}),
            hm.has_properties(
                {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'USD', 'bonus': D('1000'),
                 'update_dt': hm.greater_than_or_equal_to(dt_before_update)}
            ),
            hm.has_properties(
                {'client_id': client.id, 'service_id': cst.ServiceId.MARKET, 'iso_currency': 'RUB',
                 'bonus': D('0.0001'), 'update_dt': hm.greater_than_or_equal_to(dt_before_update)}
            ),
        ),
    )


def test_out_of_range(session, client, base_data):
    session.config.set('CLIENT_CASHBACK_LAST_IMPORTED_TABLE_NAME', '202003', column_name='value_str', can_create=True)
    session.flush()

    with yt_client_mock(base_data):
        ImportClientCashback(session).import_from_yt()

    assert client.cashbacks == {}


@pytest.mark.parametrize(
    'batch_size, broken_idx, res_table_name, res_idx',
    [
        pytest.param(2, 2, '202002', 2, id='first table batch'),
        pytest.param(5, 5, '202002', None, id='first table'),
        pytest.param(3, 9, '202003', 3, id='second table batch'),
        pytest.param(5, None, '202003', None, id='second table'),
    ],
)
def test_save_client_id_into_config(session, batch_size, broken_idx, res_table_name, res_idx):
    clients = [create_client(session) for _i in range(10)]

    items = [{'client_id': c.id, 'currency': 'RUB', 'reward': D('1')} for c in clients]
    if broken_idx is not None:
        items[broken_idx]['service_id'] = -1  # имитируем поломку просле обработки одного из батчей

    batch_1, batch_2 = items[:5], items[5:]

    data = {
        '202002': iter(batch_1),
        '202003': iter(batch_2),
    }

    class MockConfig(object):
        def __init__(self):
            self.client_id = 0
            self.table_name = ''

        def _set_config(self, table, row_idx):
            self.row_idx = row_idx
            self.table_name = table

    conf = MockConfig()

    with mock.patch('balance.constants.ORACLE_MAX_IN_CONDITION_ENTRIES', batch_size), yt_client_mock(data):
        with mock.patch('cluster_tools.yt_import_client_cashback.ImportClientCashback._set_configs', conf._set_config):
            try:
                ImportClientCashback(session).import_from_yt()
            except sa.exc.IntegrityError:
                pass

    assert conf.table_name == res_table_name
    assert conf.row_idx == res_idx


@pytest.mark.parametrize(
    'config_table, config_row_idx, bonuses',
    [
        pytest.param('0', None, [11, 11, 11], id='all'),
        pytest.param('202002', 1, [1, 11, 11], id='half of first table'),
        pytest.param('202002', 3, [1, 1, 1], id='end of first table'),
        pytest.param('202003', 2, [0, 0, 1], id='half of second table'),
        pytest.param('202003', 3, [0, 0, 0], id='end of second table'),
        pytest.param('202002', None, [1, 1, 1], id='only second table'),
        pytest.param('202003', None, [0, 0, 0], id='all are imported'),
    ],
)
def test_continue_loading(session, config_table, config_row_idx, bonuses):
    clients = [create_client(session) for _i in range(3)]
    cashbacks = [ob.ClientCashbackBuilder.construct(session, client=c, bonus=D('0')) for c in clients]

    session.config.set('CLIENT_CASHBACK_LAST_IMPORTED_TABLE_NAME', config_table, column_name='value_str',
                       can_create=True)
    session.config.set('CLIENT_CASHBACK_LAST_IMPORTED_ROW', config_row_idx, column_name='value_num', can_create=True)
    session.flush()

    data = {
        '202002': iter([{'client_id': c.id, 'currency': 'RUB', 'reward': D('10')} for c in clients]),
        '202003': iter([{'client_id': c.id, 'currency': 'RUB', 'reward': D('1')} for c in clients]),
    }

    with yt_client_mock(data):
        ImportClientCashback(session).import_from_yt()

    assert session.config.get('CLIENT_CASHBACK_LAST_IMPORTED_TABLE_NAME') == '202003'
    assert session.config.get('CLIENT_CASHBACK_LAST_IMPORTED_ROW') is None

    hm.assert_that(
        cashbacks,
        hm.contains(*[
            hm.has_properties({'bonus': D(bonus)})
            for bonus in bonuses
        ]),
    )


def test_many_cashback_for_one_client(session, client):
    data = {
        '202002': iter([
            {'client_id': client.id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT},
            {'client_id': client.id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT},
            {'client_id': client.id, 'currency': 'RUB', 'reward': D('10'), 'service_id': cst.ServiceId.DIRECT},
            {'client_id': client.id, 'currency': 'RUR', 'reward': D('100'), 'service_id': cst.ServiceId.DIRECT},
            {'client_id': client.id, 'currency': 'USD', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT},
            {'client_id': client.id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.MARKET},
            {'client_id': client.id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.MARKET,
             'cashback_months_to_live': 7},
            {'client_id': client.id, 'currency': 'RUB', 'reward': D('110'), 'service_id': cst.ServiceId.MARKET,
             'cashback_months_to_live': 7},
            {'client_id': client.id, 'currency': 'RUB', 'reward': D('222'), 'service_id': cst.ServiceId.MARKET,
             'cashback_months_to_live': 14},
            {'client_id': client.id, 'reward': D('1'), 'service_id': cst.ServiceId.MARKET},
        ])
    }

    with yt_client_mock(data):
        ImportClientCashback(session).import_from_yt()

    hm.assert_that(
        client.cashbacks.values(),
        hm.contains_inanyorder(
            hm.has_properties({'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'RUB', 'bonus': D('112')}),
            hm.has_properties({'service_id': cst.ServiceId.DIRECT, 'iso_currency': 'USD', 'bonus': D('1')}),
            hm.has_properties({'service_id': cst.ServiceId.MARKET, 'iso_currency': 'RUB', 'bonus': D('1')}),
            hm.has_properties({'service_id': cst.ServiceId.MARKET, 'iso_currency': 'RUB', 'bonus': D('111'),
                               'start_dt': ut.trunc_date(datetime.datetime.now()),
                               'finish_dt': ut.trunc_date(datetime.datetime.now()) + relativedelta(months=7)}),
            hm.has_properties({'service_id': cst.ServiceId.MARKET, 'iso_currency': 'RUB', 'bonus': D('222'),
                               'start_dt': ut.trunc_date(datetime.datetime.now()),
                               'finish_dt': ut.trunc_date(datetime.datetime.now()) + relativedelta(months=14)}),
            hm.has_properties({'service_id': cst.ServiceId.MARKET, 'iso_currency': None, 'bonus': D('1')}),
        ),
    )


def test_null_currency_new(session, client):
    _rub_cashback = ob.ClientCashbackBuilder.construct(session, client=client, service_id=cst.ServiceId.MARKET,
                                                       iso_currency='RUB', bonus=D('10'))

    data = {
        '202002': iter([
            {'client_id': client.id, 'currency': None, 'reward': D('1'), 'service_id': cst.ServiceId.MARKET},
        ])
    }

    with yt_client_mock(data):
        ImportClientCashback(session).import_from_yt()

    hm.assert_that(
        client.cashbacks.values(),
        hm.contains_inanyorder(
            hm.has_properties({'service_id': cst.ServiceId.MARKET, 'iso_currency': 'RUB', 'bonus': D('10')}),
            hm.has_properties({'service_id': cst.ServiceId.MARKET, 'iso_currency': None, 'bonus': D('1')}),
        ),
    )


def test_null_currency_existing(session, client):
    _rub_cashback = ob.ClientCashbackBuilder.construct(session, client=client, service_id=cst.ServiceId.MARKET,
                                                       iso_currency='RUB', bonus=D('10'))
    _null_cashback = ob.ClientCashbackBuilder.construct(session, client=client, service_id=cst.ServiceId.MARKET,
                                                        iso_currency=None, bonus=D('10'))

    data = {
        '202002': iter([
            {'client_id': client.id, 'currency': None, 'reward': D('1'), 'service_id': cst.ServiceId.MARKET},
            {'client_id': client.id, 'currency': 'RUB', 'reward': D('2'), 'service_id': cst.ServiceId.MARKET},
        ])
    }

    with yt_client_mock(data):
        ImportClientCashback(session).import_from_yt()

    hm.assert_that(
        client.cashbacks.values(),
        hm.contains_inanyorder(
            hm.has_properties({'service_id': cst.ServiceId.MARKET, 'iso_currency': 'RUB', 'bonus': D('12')}),
            hm.has_properties({'service_id': cst.ServiceId.MARKET, 'iso_currency': None, 'bonus': D('11')}),
        ),
    )


def test_cashback_autocharging(session):
    clients = [create_client(session) for _ in range(6)]

    data = {
        '202001': iter([
            {'client_id': clients[0].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT},
            {'client_id': clients[1].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT, 'auto_consume_enabled': True},
            {'client_id': clients[2].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT}
        ])
    }
    with yt_client_mock(data):
        ImportClientCashback(session).import_from_yt()

    exported = session.query(mapper.Export).filter(
        mapper.Export.classname.in_(['Client']),
        mapper.Export.object_id.in_([client.id for client in clients]),
        mapper.Export.type == 'AUTO_CASHBACK_CHARGE'
    ).all()
    # export is processed imitation
    for obj in exported:
        obj.state = cst.ExportState.exported

    data = {
        '202002': iter([
            {'client_id': clients[0].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT},
            {'client_id': clients[1].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT},
            {'client_id': clients[2].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT}
        ]),
        '202003': iter([
            {'client_id': clients[2].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT, 'auto_consume_enabled': True},
            {'client_id': clients[3].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT},
            {'client_id': clients[3].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT, 'auto_consume_enabled': True},
            {'client_id': clients[3].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT, 'auto_consume_enabled': True},
            {'client_id': clients[4].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT, 'auto_consume_enabled': True}
        ]),
        '202004': iter([
            {'client_id': clients[4].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT, 'auto_consume_enabled': True},
            {'client_id': clients[5].id, 'currency': 'RUB', 'reward': D('1'), 'service_id': cst.ServiceId.DIRECT, 'auto_consume_enabled': False},
        ])
    }

    with yt_client_mock(data):
        ImportClientCashback(session).import_from_yt()

    for client in clients:
        session.refresh(client)

    exported = session \
        .query(mapper.Export) \
        .filter(
            mapper.Export.classname.in_(['Client']),
            mapper.Export.object_id.in_([client.id for client in clients]),
            mapper.Export.type == 'AUTO_CASHBACK_CHARGE',
            mapper.Export.state == cst.ExportState.enqueued
        ).all()

    # auto_consume_enabled was never True
    assert clients[0].cashback_settings.get(cst.ServiceId.DIRECT) is None
    # auto_consume_enabled changed from True to False
    assert clients[1].cashback_settings.get(cst.ServiceId.DIRECT).is_auto_charge_enabled is False
    # auto_consume_enabled changed from False to True
    assert clients[2].cashback_settings.get(cst.ServiceId.DIRECT).is_auto_charge_enabled is True
    # several rows have auto_consume_enabled == True
    assert clients[3].cashback_settings.get(cst.ServiceId.DIRECT).is_auto_charge_enabled is True
    # auto_consume_enabled has True values in several batches
    assert clients[4].cashback_settings.get(cst.ServiceId.DIRECT).is_auto_charge_enabled is True
    # auto_consume_enabled is False
    assert clients[5].cashback_settings.get(cst.ServiceId.DIRECT) is None

    hm.assert_that(exported, hm.contains_inanyorder(*[hm.has_properties(object_id=client.id) for client in clients[2:5]]))


def test_tear_cashback_off(session):
    session.config.__dict__['CASHBACK_TEAR_OFF'] = 1

    client = ob.ClientBuilder().build(session).obj
    client_finish_dt_today = ob.ClientBuilder().build(session).obj
    client_finish_dt_in_future = ob.ClientBuilder().build(session).obj
    client_no_time_limit = ob.ClientBuilder().build(session).obj

    ob.ClientCashbackBuilder.construct(
        session, client=client, service_id=cst.ServiceId.DIRECT, iso_currency='RUB', bonus=D('100')
    )
    ob.ClientCashbackBuilder.construct(
        session, client=client, service_id=cst.ServiceId.DIRECT, iso_currency='RUB', bonus=D('100'),
        start_dt=datetime.date.today() - relativedelta(months=3),
        finish_dt=datetime.date.today() - relativedelta(months=2)
    )
    ob.ClientCashbackBuilder.construct(
        session, client=client_finish_dt_today, service_id=cst.ServiceId.DIRECT, iso_currency='RUB', bonus=D('100'),
        start_dt=datetime.date.today() - relativedelta(months=3),
        finish_dt=datetime.date.today()
    )
    ob.ClientCashbackBuilder.construct(
        session, client=client_finish_dt_in_future, service_id=cst.ServiceId.DIRECT, iso_currency='RUB', bonus=D('100'),
        start_dt=datetime.date.today(),
        finish_dt=datetime.date.today() + relativedelta(months=1)
    )
    ob.ClientCashbackBuilder.construct(
        session, client=client_no_time_limit, service_id=cst.ServiceId.DIRECT, iso_currency='RUB', bonus=D('100')
    )

    ImportClientCashback(session).tear_off_expired_cashback()

    exported = session.query(mapper.Export).filter(
        mapper.Export.classname.in_(['Client']),
        mapper.Export.object_id.in_([
            client.id, client_finish_dt_today.id, client_finish_dt_in_future.id, client_no_time_limit.id
        ]),
        mapper.Export.type == 'TEAR_CASHBACK_OFF'
    ).all()

    assert len(exported) == 1
    assert exported[0].object_id == client_finish_dt_today.id
