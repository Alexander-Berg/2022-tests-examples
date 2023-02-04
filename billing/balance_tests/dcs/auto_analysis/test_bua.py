# coding: utf-8
import json
import time
import decimal
import datetime as dt

import mock
import pytest
import hamcrest
import requests_mock

from butils.decimal_unit import DecimalUnit

from balance import mapper
from balance import constants
from balance.actions import consumption
from balance.actions import unified_account

from balance.actions.dcs.compare.auto_analysis import bua

from tests import object_builder as ob
from tests.balance_tests.dcs.dcs_common import (
    create_client,
    create_invoice,
    create_order,
    create_overdraft_params,
    create_person,
    create_product,
    create_product_unit,
    migrate_client,
    process_completions,
)


def test_diffs_report(session):
    rows = bua.DiffsReport(session, {}).analyze([{
        'order_id': 1234567890,
        'service_id': 7,
        'service_order_id': 123456,
        't1_value': 25,
        't2_value': 50,
        'hang_correction_qty': 0,
        'reversed_acted_correction_qty': 0,
    }])

    expected = [{
        'Order ID': 1234567890,
        'Order': '7-123456',
        'Shipments, qty': 25,
        'Acts, qty': 50,
        'Difference, qty': 25,
        'Hang correction, qty': 0,
        'Reversed acted correction, qty': 0,
    }]

    assert rows == expected


@pytest.mark.parametrize("service_id, config, overshipment, aa", [
    pytest.param(constants.ServiceId.OFD, {'ofd_overshipment_threshold': 31}, 31,
                 bua.OFDOvershipmentAutoAnalyzer, id='OFDOvershipmentAutoAnalyzer'),
    pytest.param(constants.ServiceId.ADFOX, {'adfox_overshipment_threshold': 1000}, 1000,
                 bua.AdfoxOvershipmentAutoAnalyzer, id='AdfoxOvershipmentAutoAnalyzer'),
])
def test_ofd_overshipment_aa(session, service_id, config, overshipment, aa):
    input_ = [
        {
            # Пропускаем, потому что не подходящий сервис
            'order_id': 123453,
            'service_id': constants.ServiceId.DIRECT,
            't1_value': 0,
            't2_value': 200,
        },
        {
            # Пропускаем, потому что не перекрут
            'order_id': 123454,
            'service_id': service_id,
            't1_value': 50,
            't2_value': 100,
        },
        {
            # Пропускаем, потому что расхождения существенны
            'order_id': 123455,
            'service_id': service_id,
            't1_value': 10000,
            't2_value': 10000 - overshipment * 2,
        },
        {
            # Попадает под авторазбор
            'order_id': 123456,
            'service_id': service_id,
            't1_value': 10000,
            't2_value': 10000 - overshipment / 2,
        },
    ]

    rows = aa(session, config).analyze(input_)
    expected = [{'order_id': 123456}]

    assert rows == expected


def test_log_tariff_invalid_type_aa(session):
    client = create_client(session)
    order = create_order(session, client, constants.DIRECT_SERVICE_ID, product_id=constants.DIRECT_PRODUCT_ID)
    order.child_ua_type = 2
    session.flush()

    rows = bua.LogTariffInvalidTypeAutoAnalyzer(session, {}).analyze({
        'order_id': order.id,
    })
    expected = [{'order_id': order.id}]
    assert rows == expected


class TestLogTariffCheckOldOrders(object):
    @staticmethod
    def prepare(session, consume_qty, completion_qty):
        client = create_client(session)
        person = create_person(session, client)

        parent_order = create_order(session, client, constants.ServiceId.DIRECT,
                                    product_id=constants.DIRECT_PRODUCT_RUB_ID)
        child_order = create_order(session, client, constants.ServiceId.DIRECT,
                                   product_id=constants.DIRECT_MEDIA_PRODUCT_RUB_ID,
                                   group_order_id=parent_order.id,
                                   child_ua_type=constants.UAChildType.TRANSFERS)

        invoice = create_invoice(
            session, client, person,
            rows=[(parent_order, consume_qty), ]
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        process_completions(child_order, {'Money': completion_qty})

        ua = unified_account.UnifiedAccount(session, parent_order)
        ua.transfer2group()

        invoice.generate_act(force=1)

        parent_order.is_ua_optimize = True
        child_order.child_ua_type = constants.UAChildType.LOG_TARIFF
        session.flush()

        return parent_order, child_order

    def test_log_tariff_check_transfers_aa(self, session):
        _, order = self.prepare(session, 100, 200)
        rows = bua.LogTariffCheckTransfersAutoAnalyzer(session, {}).analyze([{
            'order_id': order.id,
        }])
        expected = [{'order_id': order.id}]
        assert rows == expected

    def test_log_tariff_unexpected_free_funds_aa(self, session):
        _, order = self.prepare(session, 100, 200)

        consume = order.consumes[0]
        consumption.reverse_consume(consume, None, -200)

        rows = bua.LogTariffUnexpectedFreeFundsAutoAnalyzer(session, {}).analyze([{
            'order_id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
        }])
        expected = [{
            'order_id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            'date_of_transfer_client_to_log_tariff': None,
            'consume_qty': 300,
            'completion_qty': 100,
            'act_qty': 100,
            'diff_qty': -200,
        }]
        assert rows == expected

    def test_log_tariff_overacted_aa(self, session):
        _, order = self.prepare(session, 200, 200)

        consume = order.consumes[0]
        reverse = consumption.reverse_consume(consume, None, 100)
        process_completions(order, force=True)
        session.flush()

        rows = bua.LogTariffOveractedAutoAnalyzer(session, {}).analyze([{
            'order_id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
        }])
        expected = [{
            'order_id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            'date_of_transfer_client_to_log_tariff': None,
            'reverse_dt': reverse.dt,
            'reverse_qty': 100,
            'consume_qty': 100,
            'completion_qty': 100,
            'act_qty': 200,
            'overacted': 100,
        }]
        assert rows == expected

    def test_log_tariff_invalid_children_aa(self, session):
        _, order = self.prepare(session, 100, 100)

        consume = order.consumes[0]
        consumption.reverse_consume(consume, None, -100)
        process_completions(order, {'Money': 200}, force=True)

        rows = bua.LogTariffInvalidChildrenAutoAnalyzer(session, {}).analyze([{
            'order_id': order.id,
        }])
        expected = [{
            'order_id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            'date_of_transfer_client_to_log_tariff': None,
            'consume_qty': 200,
            'completion_qty': 200,
            'act_qty': 100,
            'diff_qty': -100,
        }]
        assert rows == expected


class TestLogTariffShipmentUpdateAfterAct(object):
    mr = None

    @pytest.fixture(autouse=True)
    def setup(self, session):
        session.config.__dict__['DCS_REACTOR_HOST'] = 'https://reactor'
        session.config.__dict__['DCS_NIRVANA_HOST'] = 'nirvana'

        self.reactor_url = 'https://reactor/api/v1'
        self.nirvana_url = 'https://nirvana/api'

        with mock.patch.object(bua.LogTariffShipmentUpdateAfterAct, 'nirvana_token'), \
            requests_mock.Mocker() as mr:
            self.mr = mr

            yield

    def test_artifact_not_exists(self, session):
        self.mr.post(self.reactor_url + '/a/get', status_code=404, json={
            'codes': [{
                'code': 'ARTIFACT_DOES_NOT_EXIST_ERROR',
            }]
        })
        assert not bua.LogTariffShipmentUpdateAfterAct(session, {}).artifact_exists

        self.mr.post(self.reactor_url + '/a/get', status_code=200, json={
            'artifact': {
                'id': '123456',
                'artifactTypeId': '17',
                'namespaceId': '123456',
                'projectId': '123',
            }
        })
        self.mr.post(self.reactor_url + '/a/i/get/last', status_code=200, json={})
        assert not bua.LogTariffShipmentUpdateAfterAct(session, {}).artifact_exists

    def test_success(self, session):
        self.mr.post(self.reactor_url + '/a/get', status_code=200, json={
            'artifact': {
                'id': '123456',
                'artifactTypeId': '17',
                'namespaceId': '123456',
                'projectId': '123',
            }
        })

        self.mr.post(self.reactor_url + '/a/i/get/last', status_code=200, json={
            'result': {
                'id': '123456',
                'artifactId': '123456',
                'status': 'ACTIVE',
                'source': 'MANUAL',
                'attributes': {
                    'keyValue': {}
                },
                'metadata': {
                    '@type': '/yandex.reactor.artifact.StringArtifactValueProto',
                    'value': '65f68af0-77a6-4e18-8c56-101545f63b48',
                },
                'creatorLogin': 'srg91',
                'creationTimestamp': '2021-06-21T17:58:38.455',
                'userTimestamp': '2021-06-21T17:58:38.455',
            }
        })

        data_id = '65f68af0-77a6-4e18-8c56-101545f63b48'
        self.mr.post(self.nirvana_url + '/public/v1/getData', status_code=200, json={
            'jsonrpc': '2.0',
            'id': '3998132f-46cd-478a-9d55-a469fd5eab44',
            'result': {
                'dataId': data_id,
                'storageUrl': self.nirvana_url + '/storedData/' + data_id + '/data'
            }
        })
        self.mr.get(self.nirvana_url + '/storedData/' + data_id + '/data', status_code=200, json={
            'document_dt': '2021-05-31',
            'run_dt': '2021-02-01T02:30:55',
        })

        invoice_dt = dt.datetime(2021, 1, 1)
        act_dt = dt.datetime(2021, 1, 31)
        shipment_update_dt = dt.datetime(2021, 2, 1, 10, 0, 0)

        client = create_client(session)
        person = create_person(session, client)

        service = ob.Getter(mapper.Service, constants.ServiceId.DIRECT).build(session).obj
        order = create_order(session, client, service.id,
                             product_id=constants.DIRECT_PRODUCT_ID)

        invoice = create_invoice(
            session, client, person,
            dt=invoice_dt,
            rows=[(order, 200)]
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows(on_dt=invoice_dt)

        process_completions(order, {'Bucks': 200}, on_dt=invoice_dt)
        invoice.generate_act(backdate=act_dt, force=1)

        process_completions(order, {'Bucks': 150}, on_dt=act_dt)

        update_shipment_dt_query = """
            update bo.t_shipment
            set update_dt = :update_dt
            where service_id = :service_id and service_order_id = :service_order_id
        """
        session.execute(
            update_shipment_dt_query,
            {'service_id': order.service_id, 'service_order_id': order.service_order_id,
             'update_dt': shipment_update_dt}
        )

        order._is_log_tariff = constants.OrderLogTariffState.MIGRATED
        session.flush()

        config = {'acts_dt': dt.date(2021, 1, 31)}
        aa = bua.LogTariffShipmentUpdateAfterAct(session, config)
        assert aa.artifact_exists
        assert aa.run_dt == dt.datetime(2021, 2, 1, 2, 30, 55)

        rows = aa.analyze([{
            'order_id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            't1_value': 150,
            't2_value': 200,
        }])
        expected = [{
            'order_id': order.id,
            'Shipments, qty': 150,
            'Acts, qty': 200,
            'Shipment DT': act_dt,
            'Shipment Update DT': shipment_update_dt,
            'Run DT': aa.run_dt,
        }]
        assert rows == expected


def test_auto_overdraft_aa(session):
    client = create_client(session)
    person = create_person(session, client)

    client_limit = 100000
    service = ob.Getter(mapper.Service, constants.ServiceId.DIRECT).build(session).obj
    create_overdraft_params(session, service, client, person, client_limit)

    order = create_order(session, client, service.id, product_id=constants.DIRECT_PRODUCT_ID)
    invoice = create_invoice(session, client, person, [(order, 100)])
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=dt.datetime(2021, 1, 1))

    session.flush()

    config = {'acts_dt': dt.date(2021, 1, 31)}
    rows = bua.AutoOverdraftAutoAnalyzer(session, config).analyze([{
        'order_id': order.id,
        't1_value': 200,
        't2_value': 0,
    }])

    expected = [{'order_id': order.id}]
    assert rows == expected


def test_reverse_aa(session):
    invoice_dt = dt.datetime(2021, 1, 1)
    migrate_dt = dt.datetime(2021, 1, 2)
    reverse_dt = dt.datetime(2021, 1, 3)
    act_dt = dt.datetime(2021, 1, 31)

    client = create_client(session)
    person = create_person(session, client)

    service = ob.Getter(mapper.Service, constants.ServiceId.DIRECT).build(session).obj

    order = create_order(session, client, service.id, product_id=constants.DIRECT_PRODUCT_ID)
    invoice = create_invoice(
        session, client, person,
        dt=invoice_dt,
        rows=[(order, 300)]
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice_dt)

    process_completions(order, {'Bucks': 200}, on_dt=invoice_dt)

    migrate_client(
        client,
        service_id=service.id,
        on_dt=migrate_dt
    )
    process_completions(order, {'Bucks': 200, 'Money': 3000}, on_dt=migrate_dt)

    invoice.generate_act(backdate=act_dt, force=1)

    process_completions(order, {'Bucks': 200, 'Money': 2340}, on_dt=reverse_dt)

    config = {'acts_dt': act_dt}
    rows = bua.ReverseAutoAnalyzer(session, config).analyze([{
        'order_id': order.id,
        't1_value': float(order.consumes[0].completion_qty),
        't2_value': float(order.consumes[0].act_qty),
        'hang_correction_qty': 0,
    }])
    expected = [{
        'order_id': order.id,
    }]
    assert rows == expected


class TestShipmentUpdateAfterLastActAutoAnalyzer(object):
    check_dt = dt.datetime(2021, 1, 31)

    daily_act_dt = dt.datetime(2021, 1, 2)
    monthly_act_dt = dt.datetime(2020, 12, 31)

    def prepare(self, session, act_dt):
        invoice_dt = shipment_dt = act_dt.replace(day=1)

        client = create_client(session)
        person = create_person(session, client)

        self.order = create_order(session, client,
                                  service_id=constants.ServiceId.DIRECT,
                                  product_id=constants.DIRECT_PRODUCT_ID)
        invoice = create_invoice(
            session, client, person,
            dt=invoice_dt,
            rows=[(self.order, 100)]
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows(on_dt=invoice_dt)

        process_completions(self.order, {'Bucks': 100}, on_dt=shipment_dt)
        invoice.generate_act(backdate=act_dt, force=1)

    def update_shipment_dt(self, session, shipment_dt):
        update_shipment_dt_query = """
            update bo.t_shipment
            set update_dt = :update_dt
            where service_id = :service_id and service_order_id = :service_order_id
        """

        session.execute(
            update_shipment_dt_query,
            {
                'service_id': self.order.service_id,
                'service_order_id': self.order.service_order_id,
                'update_dt': shipment_dt
            }
        )

    @pytest.mark.parametrize('act_dt, is_log_tariff, shipment_dt_before, shipment_dt_after', [
        (
            daily_act_dt,
            constants.OrderLogTariffState.MIGRATED,
            daily_act_dt + dt.timedelta(hours=20),
            daily_act_dt + dt.timedelta(hours=22),
        ),
        (
            monthly_act_dt,
            constants.OrderLogTariffState.MIGRATED,
            monthly_act_dt + dt.timedelta(days=1, hours=1),
            monthly_act_dt + dt.timedelta(days=1, hours=4),
        ),
        (
            daily_act_dt,
            None,
            daily_act_dt,
            daily_act_dt + dt.timedelta(days=1, hours=22),
        ),
        (
            monthly_act_dt,
            None,
            monthly_act_dt + dt.timedelta(days=1, hours=1),
            monthly_act_dt + dt.timedelta(days=1, hours=4),
        ),
    ], ids=[
        'is_log_tariff_daily',
        'is_log_tariff_monthly',
        'daily',
        'monthly',
    ])
    def test_auto_analyzer(self, session, act_dt, is_log_tariff, shipment_dt_before, shipment_dt_after):
        self.prepare(session, act_dt)

        self.order._is_log_tariff = is_log_tariff
        session.flush()

        self.update_shipment_dt(session, shipment_dt_before)
        config = {'acts_dt': self.check_dt}
        rows = bua.ShipmentUpdateAfterLastActAutoAnalyzer(session, config).analyze([{
            'order_id': self.order.id,
        }])
        hamcrest.assert_that(rows, hamcrest.empty())

        self.update_shipment_dt(session, shipment_dt_after)
        rows = bua.ShipmentUpdateAfterLastActAutoAnalyzer(session, config).analyze([{
            'order_id': self.order.id,
        }])
        expected = [{'order_id': self.order.id}]
        assert rows == expected


def test_overacted_transfer_aa(session):
    invoice_dt = dt.datetime(2021, 1, 1)
    act_dt = dt.datetime(2021, 1, 2)
    shipment_dt = dt.datetime(2021, 1, 3)

    client = create_client(session)
    person = create_person(session, client)

    order1 = create_order(session, client,
                          service_id=constants.ServiceId.DIRECT,
                          product_id=constants.DIRECT_PRODUCT_ID)
    order2 = create_order(session, client,
                          service_id=constants.ServiceId.DIRECT,
                          product_id=constants.DIRECT_PRODUCT_ID)
    invoice = create_invoice(
        session, client, person,
        dt=invoice_dt,
        rows=[(order1, 200), (order2, 200)]
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice_dt)

    process_completions(order1, {'Bucks': 100}, on_dt=invoice_dt)
    process_completions(order2, {'Bucks': 100}, on_dt=invoice_dt)

    invoice.generate_act(backdate=act_dt, force=1)

    process_completions(order1, {'Bucks': 150}, on_dt=shipment_dt)
    process_completions(order2, {'Bucks': 50}, on_dt=shipment_dt)

    rows = bua.OveractedTransferAutoAnalyzer(session, {}).analyze([{
        'order_id': order1.id,
        't1_value': 150,
        't2_value': 100,
    }])
    expected = [{'order_id': order1.id}]

    assert rows == expected


def test_days_whole_bundle_aa(session):
    invoice_dt = dt.datetime.today() - dt.timedelta(days=1)

    product_type = ob.Getter(mapper.ProductType, constants.ProductTypeId.days).build(session).obj
    product_unit = create_product_unit(
        session,
        name=u'Неделя',
        type_rate=7,
        precision=0,
        product_type=product_type,
    )
    product = create_product(session, product_unit.id)

    client = create_client(session)
    person = create_person(session, client)

    order = create_order(session, client,
                         product=product,
                         service_id=constants.ServiceId.DIRECT)
    invoice = create_invoice(
        session, client, person,
        rows=[(order, 35)]
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice_dt)

    process_completions(order, {'Days': 24}, on_dt=invoice_dt)
    invoice.generate_act(force=1)

    rows = bua.DaysWholeBundleAutoAnalyzer(session, {}).analyze([{
        'order_id': order.id,
        't1_value': int(order.consumes[0].completion_qty),
        't2_value': int(order.consumes[0].act_qty),
    }])
    expected = [{'order_id': order.id}]
    assert rows == expected


def test_group_order_overshipment_aa(session):
    client = create_client(session)
    person = create_person(session, client)

    parent_order = create_order(session, client, constants.ServiceId.DIRECT,
                                product_id=constants.DIRECT_PRODUCT_ID)
    child_order = create_order(session, client, constants.ServiceId.DIRECT,
                               product_id=constants.DIRECT_PRODUCT_ID,
                               group_order_id=parent_order.id,
                               child_ua_type=constants.UAChildType.TRANSFERS)
    invoice = create_invoice(
        session, client, person,
        rows=[(parent_order, 100), ]
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows()

    process_completions(child_order, {'Bucks': 200})

    ua = unified_account.UnifiedAccount(session, parent_order)
    ua.transfer2group()

    invoice.generate_act(force=1)

    config = {'acts_dt': dt.datetime.today() + dt.timedelta(days=1)}
    _, result = bua.GroupOrderOvershipmentAutoAnalyzer(session, config).analyze([{
        'order_id': child_order.id,
        'service_id': child_order.service_id,
        't1_value': 200,
        't2_value': 100,
        'hang_correction_qty': 0,
        'reversed_acted_correction_qty': 0,
    }])
    expected = [{
        'Order ID': child_order.id,
        'Order': '{}-{}'.format(child_order.service_id, child_order.service_order_id),
        'Overshipment, qty': 100,
    }]
    assert result == expected


def test_late_shipment_update_aa(session):
    client = create_client(session)
    order = create_order(session, client,
                         service_id=constants.ServiceId.DIRECT,
                         product_id=constants.DIRECT_PRODUCT_ID)

    config = {'acts_dt': dt.datetime(2021, 1, 31)}
    rows = bua.LateShipmentUpdateAutoAnalyzer(session, config).analyze([{
        'order_id': order.id,
        'shipment_update_dt': '2021-02-01T20:00:00Z',
    }])
    expected = [{'order_id': order.id}]
    assert rows == expected


def test_overshipment_caused_by_reverse_aa(session):
    invoice_dt = dt.datetime(2021, 1, 1)
    act_dt = dt.datetime(2021, 1, 31)

    client = create_client(session)
    person = create_person(session, client)

    order = create_order(session, client, constants.ServiceId.DIRECT,
                         product_id=constants.DIRECT_PRODUCT_ID)

    invoice = create_invoice(
        session, client, person,
        dt=invoice_dt,
        rows=[(order, 100), ]
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice_dt)

    process_completions(order, {'Bucks': 200}, on_dt=invoice_dt)
    invoice.generate_act(backdate=act_dt, force=1)

    consume = order.consumes[0]
    operation = mapper.Operation(constants.OperationTypeIDs.withdraw_debts, invoice=invoice, dt=invoice_dt)
    consumption.reverse_consume(consume, operation, 50)

    config = {'acts_dt': act_dt}
    rows = bua.OvershipmentCausedByReverseAutoAnalyzer(session, config).analyze([{
        'order_id': order.id,
        'service_id': order.service_id,
        't1_value': 200,
        't2_value': 100,
    }])
    expected = [{'order_id': order.id}]
    assert rows == expected


@pytest.mark.parametrize("service_id, aa", [
    pytest.param(constants.ServiceId.GEOCON, bua.GeoOvershipmentAutoAnalyzer,
                 id='GeoOvershipmentAutoAnalyzer'),
    pytest.param(constants.ServiceId.NAVIGATOR_ADV, bua.NavigatorOvershipmentAutoAnalyzer,
                 id='NavigatorOvershipmentAutoAnalyzer'),
])
def test_common_overshipment_aa(session, service_id, aa):
    rows = aa(session, {}).analyze([{
        'order_id': 123456,
        'service_id': service_id,
        'service_order_id': 123456,
        't1_value': 50,
        't2_value': 100,
    }])
    hamcrest.assert_that(rows, hamcrest.empty())

    rows = aa(session, {}).analyze([{
        'order_id': 123456,
        'service_id': service_id,
        'service_order_id': 123456,
        't1_value': 100,
        't2_value': 50,
    }])
    expected = [{
        'order_id': 123456,
        'order': '{}-{}'.format(service_id, 123456),
        'overshipment': 50,
    }]
    assert rows == expected


@pytest.mark.parametrize("service_id, aa", [
    pytest.param(constants.ServiceId.MARKET, bua.MarketOvershipmentAutoAnalyzer,
                 id='MarketOvershipmentAutoAnalyzer'),
    pytest.param(constants.ServiceId.MARKET_VENDORS, bua.MarketVendorsOvershipmentAutoAnalyzer,
                 id='MarketVendorsOvershipmentAutoAnalyzer'),
])
def test_market_overshipment_aa(session, service_id, aa):
    config = {
        'market_overshipment_approved_threshold': 100,
    }
    _, result = aa(session, config).analyze([{
        'order_id': 123456,
        'service_id': service_id,
        'service_order_id': 123456,
        't1_value': 100,
        't2_value': 50,
    }])
    expected = {
        'is_threshold_exceeded': False,
        'sum': 50,
        'rows': [{
            'order_id': 123456,
            'order': '{}-{}'.format(service_id, 123456),
            'ships_qty': 100,
            'act_qty': 50,
        }]
    }
    assert result == expected

    _, result = aa(session, config).analyze([{
        'order_id': 123456,
        'service_id': service_id,
        'service_order_id': 123456,
        't1_value': 200,
        't2_value': 50,
    }])
    expected = {
        'is_threshold_exceeded': True,
        'sum': 150,
        'rows': [{
            'order_id': 123456,
            'order': '{}-{}'.format(service_id, 123456),
            'ships_qty': 200,
            'act_qty': 50
        }]
    }
    assert result == expected


def test_ofd_orders_overshipment_aa(session):
    client = create_client(session)
    product = create_product(session, constants.AUCTION_UNIT_ID)
    order = create_order(session, client, constants.ServiceId.OFD, product=product)

    config = {'ofd_products_overshipment': product.id}
    rows = bua.OFDOrdersOvershipmentAutoAnalyzer(session, config).analyze([{
        'order_id': order.id,
        'service_id': order.service_id,
        't1_value': 100,
        't2_value': 50,
    }])
    expected = [{'order_id': order.id}]
    assert rows == expected


def test_ofd_significant_overshipment_aa(session):
    client = create_client(session)
    product = create_product(session, constants.AUCTION_UNIT_ID)
    order = create_order(session, client, constants.ServiceId.OFD, product=product)

    config = {
        'ofd_overshipment_threshold': 30,
        'ofd_overshipment_approved_threshold': 100,
    }
    _, result = bua.OFDSignificantOvershipmentAutoAnalyzer(session, config).analyze([{
        'order_id': order.id,
        'service_id': order.service_id,
        'service_order_id': order.service_order_id,
        't1_value': 60,
        't2_value': 50,
    }])
    expected = {'is_threshold_exceeded': False, 'sum': 0, 'rows': []}
    assert result == expected

    _, result = bua.OFDSignificantOvershipmentAutoAnalyzer(session, config).analyze([{
        'order_id': order.id,
        'service_id': order.service_id,
        'service_order_id': order.service_order_id,
        't1_value': 100,
        't2_value': 50,
    }])
    expected = {
        'is_threshold_exceeded': False,
        'sum': 50,
        'rows': [{
            'order_id': order.id,
            'order': '{}-{}'.format(order.service_id, order.service_order_id),
            'ships_qty': 100,
            'act_qty': 50,
        }]
    }
    assert result == expected

    _, result = bua.OFDSignificantOvershipmentAutoAnalyzer(session, config).analyze([{
        'order_id': order.id,
        'service_id': order.service_id,
        'service_order_id': order.service_order_id,
        't1_value': 200,
        't2_value': 50,
    }])
    expected = {
        'is_threshold_exceeded': True,
        'sum': 150,
        'rows': [{
            'order_id': order.id,
            'order': '{}-{}'.format(order.service_id, order.service_order_id),
            'ships_qty': 200,
            'act_qty': 50
        }]
    }
    assert result == expected


def test_adfox_significant_overshipment_aa(session):
    client = create_client(session)
    product = create_product(session, constants.AUCTION_UNIT_ID)
    order = create_order(session, client, constants.ServiceId.ADFOX, product=product)

    config = {
        'adfox_overshipment_threshold': 1000,
    }
    rows = bua.AdfoxSignificantOvershipmentAutoAnalyzer(session, config).analyze([{
        'order_id': order.id,
        'service_id': order.service_id,
        'service_order_id': order.service_order_id,
        't1_value': 60,
        't2_value': 50,
    }])
    expected = []
    assert rows == expected

    rows = bua.AdfoxSignificantOvershipmentAutoAnalyzer(session, config).analyze([{
        'order_id': order.id,
        'service_id': order.service_id,
        'service_order_id': order.service_order_id,
        't1_value': 2000,
        't2_value': 1000,
    }])
    expected = [{
        'order_id': order.id,
        'order': '{}-{}'.format(order.service_id, order.service_order_id),
        'overshipment': 1000,
    }]
    assert rows == expected


def test_overshipment_without_money_on_old_main_order_aa(session):
    invoice_dt = dt.datetime(2021, 1, 1)

    client = create_client(session)
    person = create_person(session, client)

    parent_order = create_order(session, client, constants.ServiceId.DIRECT,
                                product_id=constants.DIRECT_PRODUCT_ID)
    child_order = create_order(session, client, constants.ServiceId.DIRECT,
                               product_id=constants.DIRECT_PRODUCT_ID,
                               group_order_id=parent_order.id,
                               child_ua_type=constants.UAChildType.TRANSFERS)

    invoice = create_invoice(
        session, client, person,
        dt=invoice_dt,
        rows=[(child_order, 100), (parent_order, 100)]
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice_dt)

    consume = parent_order.consumes[0]
    consumption.reverse_consume(consume, None, consume.current_qty)

    process_completions(child_order, {'Bucks': 200}, on_dt=invoice_dt)

    rows = bua.OvershipmentWithoutMoneyOnOldMainOrderAutoAnalyzer(session, {}).analyze([{
        'order_id': child_order.id,
        'unified_account_id': parent_order.id,
        't1_value': 200,
        't2_value': 50,
    }])
    expected = [{'order_id': child_order.id}]
    assert rows == expected


def test_overshipment_without_money_on_new_main_order_aa(session):
    client = create_client(session)
    client.set_currency(constants.ServiceId.DIRECT, 'RUB', dt.datetime(2000, 1, 1), None)

    parent_order = create_order(session, client, constants.ServiceId.DIRECT,
                                product_id=constants.DIRECT_PRODUCT_RUB_ID)
    child_order = create_order(session, client, constants.ServiceId.DIRECT,
                               product_id=constants.DIRECT_PRODUCT_RUB_ID)
    unified_account.UnifiedAccountRelations().link(parent_order, [child_order, ])
    parent_order.turn_on_optimize()
    session.flush()

    rows = bua.OvershipmentWithoutMoneyOnNewMainOrderAutoAnalyzer(session, {}).analyze([{
        'order_id': parent_order.id,
        't1_value': 100,
        't2_value': 50,
    }])
    expected = [{'order_id': parent_order.id}]
    assert rows == expected


def test_forced_overshipment_by_service_aa(session):
    invoice_dt = dt.datetime(2021, 1, 1)

    client = create_client(session)
    person = create_person(session, client)

    order = create_order(session, client,
                         service_id=constants.ServiceId.DIRECT,
                         product_id=constants.DIRECT_PRODUCT_ID)
    invoice = create_invoice(
        session, client, person,
        dt=invoice_dt,
        rows=[(order, 100), ]
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice_dt)

    process_completions(order, {'Bucks': 100}, on_dt=invoice_dt)
    invoice.generate_act(force=1)

    rows = bua.ForcedOvershipmentByServiceAutoAnalyzer(session, {}).analyze([{
        'order_id': order.id,
        't1_value': 150,
        't2_value': 100,
    }])
    expected = [{'order_id': order.id}]
    assert rows == expected


def test_late_payment_on_old_consume_aa(session):
    invoice_dt = dt.datetime(2021, 1, 1)

    client = create_client(session)
    person = create_person(session, client)

    parent_order = create_order(session, client, constants.ServiceId.DIRECT,
                                product_id=constants.DIRECT_PRODUCT_ID)
    child_order = create_order(session, client, constants.ServiceId.DIRECT,
                               product_id=constants.DIRECT_PRODUCT_ID,
                               group_order_id=parent_order.id,
                               child_ua_type=constants.UAChildType.TRANSFERS)

    invoice = create_invoice(
        session, client, person,
        dt=invoice_dt,
        rows=[(child_order, 100), (parent_order, 100)]
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(on_dt=invoice_dt)
    invoice.generate_act(force=1)

    consume = parent_order.consumes[0]
    consumption.reverse_consume(consume, None, -consume.current_qty)
    # Небольшая пауза, чтобы запрос подхватил возврат
    time.sleep(1)

    process_completions(child_order, {'Bucks': 200}, on_dt=invoice_dt)

    with mock.patch('datetime.datetime', mock.Mock(today=mock.Mock(return_value=dt.datetime(2021, 1, 1)))):
        rows = bua.LatePaymentOnOldConsumeAutoAnalyzer(session, {}).analyze([{
            'order_id': child_order.id,
            'unified_account_id': parent_order.id,
            't1_value': 200,
            't2_value': 100,
        }])
    expected = [{'order_id': child_order.id}]
    assert rows == expected


class TestApprovedThresholdAA(object):
    @staticmethod
    def format_row(order, ships_qty=100, act_qty=50):
        return {
            'order_id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            'client_id': order.client.id,
            'client_name': order.client.name,
            'agency_id': order.agency and order.agency.id,
            'agency_name': order.agency and order.agency.name,
            't1_value': ships_qty,
            't2_value': act_qty,
            'hang_correction_qty': 0,
            'reversed_acted_correction_qty': 0,
        }

    def test_product_without_price(self, session):
        client = create_client(session)
        product = create_product(session, constants.AUCTION_UNIT_ID)
        order = create_order(session, client, constants.ServiceId.DIRECT, product)
        price = product.prices[0]
        session.delete(price)
        session.flush()

        result = bua.ApprovedThresholdAutoAnalyzer(session, {}).analyze([{
            'order_id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            't1_value': 100,
            't2_value': 50,
            'hang_correction_qty': 0,
            'reversed_acted_correction_qty': 0,
        }])

        expected = {
            'products': [{'Product ID': product.id}],
            'orders': [{
                'Product ID': product.id,
                'Order ID': order.id,
                'Order': '{}-{}'.format(order.service_id, order.service_order_id),
                'Difference, qty': 50,
                'Shipments, qty': 100,
                'Acts, qty': 50,
                'Hang correction, qty': 0,
                'Reversed acted correction, qty': 0,
            }],
        }

        assert result['wo_product_price'] == expected
        assert result['context']['keep_report_issue_open']

    def test_threshold_not_exceeded(self, session):
        client = create_client(session)
        agency = create_client(session, is_agency=True)
        order = create_order(session, client, constants.ServiceId.DIRECT,
                             product_id=constants.DIRECT_PRODUCT_ID, agency=agency)

        config = {'approved_threshold_threshold': 10000}
        result = bua.ApprovedThresholdAutoAnalyzer(session, config).analyze([
            self.format_row(order, 100, 50)
        ])

        expected = [{
            'Order ID': order.id,
            'Order': '{}-{}'.format(order.service_id, order.service_order_id),
            'Agency ID': order.agency.id,
            'Agency name': order.agency.name,
            'Client ID': order.client.id,
            'Client name': order.client.name,
            'Product ID': constants.DIRECT_PRODUCT_ID,
            'Difference, RUB': 50 * constants.DIRECT_PRODUCT_PRICE,
            'Difference, qty': 50,
            'Shipments, qty': 100,
            'Acts, qty': 50,
            'Hang correction, qty': 0,
            'Reversed acted correction, qty': 0,
        }]

        assert result['context']['approved_threshold_not_exceeded']
        assert not result['context']['keep_report_issue_open']
        assert result['with_product_price']['orders'] == expected

    def test_threshold_exceeded(self, session):
        client = create_client(session)
        order = create_order(session, client, constants.ServiceId.DIRECT,
                             product_id=constants.DIRECT_PRODUCT_ID)

        config = {'approved_threshold_threshold': 1000}
        result = bua.ApprovedThresholdAutoAnalyzer(session, config).analyze([
            self.format_row(order, 100, 50)
        ])

        assert not result['context']['approved_threshold_not_exceeded']
        assert result['context']['keep_report_issue_open']

    def test_important_agencies(self, session):
        client = create_client(session)
        agency = create_client(session, is_agency=True)
        order = create_order(session, client, constants.ServiceId.DIRECT,
                             product_id=constants.DIRECT_PRODUCT_ID, agency=agency)

        config = {
            'approved_threshold_important_agencies_enabled': True,
            'approved_threshold_important_agencies': [agency.id, ]
        }
        result = bua.ApprovedThresholdAutoAnalyzer(session, config).analyze([
            self.format_row(order)
        ])

        expected = [agency.name, ]
        assert result['with_product_price']['important_agencies'] == expected


class TestCalculatePartnerSumsAA(object):
    @staticmethod
    def format_row(order, ships_qty=100, act_qty=50):
        return {
            'order_id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            't1_value': ships_qty,
            't2_value': act_qty,
            'hang_correction_qty': 0,
            'reversed_acted_correction_qty': 0,
        }

    @staticmethod
    def format_row_pretty(order, ships_qty=100, act_qty=50):
        return {
            'Order ID': order.id,
            'Order': '{}-{}'.format(order.service_id, order.service_order_id),
            'Product ID': order.service_code,
            'Difference, RUB': ships_qty - act_qty,
            'Difference, qty': ships_qty - act_qty,
            'Shipments, qty': ships_qty,
            'Acts, qty': act_qty,
            'Hang correction, qty': 0,
            'Reversed acted correction, qty': 0,
        }

    def test_unknown_services(self, session):
        client = create_client(session)
        product = create_product(session, constants.AUCTION_UNIT_ID)
        order = create_order(session, client, constants.ServiceId.ADFOX, product=product)

        result = bua.CalculatePartnerSums(session, {}).analyze([{
            'order_id': order.id,
            'service_id': order.service_id,
            'service_order_id': order.service_order_id,
            't1_value': 100,
            't2_value': 50,
        }])

        expected = {
            'can_set_need_info': False,
            'groups': {
                'orders': [],
                'services': {},
            },
            'unknown': [u'ADFox.ru'],
        }
        assert result == expected

    def test_threshold_not_exceeded(self, session):
        client = create_client(session)
        product = create_product(session, constants.RUB_UNIT_ID)
        order = create_order(session, client, constants.ServiceId.TAXI_CASH, product=product)

        config = {
            'calculate_partner_sum_min_threshold': 1000,
            'calculate_partner_sum_max_threshold': 10000,

        }

        result = bua.CalculatePartnerSums(session, config).analyze([
            self.format_row(order)
        ])

        expected = {
            'can_set_need_info': True,
            'groups': {
                'orders': [
                    self.format_row_pretty(order)
                ],
                'services': {
                    str(constants.ServiceId.TAXI_CASH): {
                        'name': u'Яндекс.Такси',
                        'sum': 50.0,
                        'is_max_threshold_exceeded': False,
                        'is_min_threshold_exceeded': False,
                    },
                },
            },
            'unknown': [],
        }

        assert result == expected

    def test_threshold_exceeded(self, session):
        client = create_client(session)
        product = create_product(session, constants.RUB_UNIT_ID)
        order = create_order(session, client, constants.ServiceId.TAXI_CASH, product=product)

        config = {
            'calculate_partner_sum_min_threshold': 100,
            'calculate_partner_sum_max_threshold': 1000,
        }

        result = bua.CalculatePartnerSums(session, config).analyze([
            self.format_row(order, 1000, 500),
        ])

        expected = {
            'can_set_need_info': False,
            'groups': {
                'orders': [
                    self.format_row_pretty(order, 1000, 500),
                ],
                'services': {
                    str(constants.ServiceId.TAXI_CASH): {
                        'name': u'Яндекс.Такси',
                        'sum': 500.0,
                        'is_max_threshold_exceeded': False,
                        'is_min_threshold_exceeded': True,
                    },
                },
            },
            'unknown': [],
        }
        assert result == expected

        result = bua.CalculatePartnerSums(session, config).analyze([
            self.format_row(order, 2000, 500),
        ])

        expected = {
            'can_set_need_info': False,
            'groups': {
                'orders': [
                    self.format_row_pretty(order, 2000, 500),
                ],
                'services': {
                    str(constants.ServiceId.TAXI_CASH): {
                        'name': u'Яндекс.Такси',
                        'sum': 1500.0,
                        'is_max_threshold_exceeded': True,
                        'is_min_threshold_exceeded': True,
                    },
                },
            },
            'unknown': [],
        }
        assert result == expected


class TestLogTariffOveractedOrders(object):
    @pytest.fixture(autouse=True)
    def setup(self, session):
        self.session = session
        self.config = {'acts_dt': dt.date.today()}

        self.order_with_act_in_other_period = self.prepare_order_with_act_in_other_period()
        self.order_with_act_in_current_period = self.prepare_order_with_act_in_current_period()

    def test_log_tariff_overacted_orders_in_period_aa(self):
        self.do(
            self.order_with_act_in_current_period,
            self.order_with_act_in_other_period,
            bua.LogTariffOveractedOrdersInPeriodAutoAnalyzer,
        )

    def test_log_tariff_overacted_orders_aa(self):
        self.do(
            self.order_with_act_in_other_period,
            self.order_with_act_in_current_period,
            bua.LogTariffOveractedOrdersAutoAnalyzer,
        )

    def do(self, good_order, bad_order, aa):
        completion_qty = 100
        act_qty = 200

        rows = aa(self.session, self.config).analyze([
            {'order_id': good_order.id, 't1_value': completion_qty, 't2_value': act_qty},
            {'order_id': bad_order.id, 't1_value': completion_qty, 't2_value': act_qty},
        ])
        expected = [{
            'order_id': good_order.id,
            'service_id': good_order.service_id,
            'service_order_id': good_order.service_order_id,
            'completion_qty': 100,
            'act_qty': 200,
        }]
        assert rows == expected

    def prepare_order_with_act_in_current_period(self):
        act_dt = dt.datetime.today()
        return self._prepare(act_dt)

    def prepare_order_with_act_in_other_period(self):
        act_dt = dt.datetime.today() - dt.timedelta(days=66)
        return self._prepare(act_dt)

    def _prepare(self, act_dt):
        consume_qty = completion_qty = 666

        client = create_client(self.session)
        person = create_person(self.session, client)

        order = create_order(self.session, client, constants.ServiceId.DIRECT,
                             product_id=constants.DIRECT_PRODUCT_RUB_ID)

        invoice = create_invoice(
            self.session, client, person, rows=[(order, consume_qty)]
        )
        invoice.create_receipt(invoice.effective_sum)
        invoice.turn_on_rows()

        process_completions(order, {'Money': completion_qty})
        acts = invoice.generate_act(force=1)

        acts[0].dt = act_dt
        order._is_log_tariff = constants.OrderLogTariffState.MIGRATED
        self.session.flush()

        return order


def test_json_default():
    result = json.dumps({
        'date': dt.datetime(2000, 1, 1, 10, 11, 12),
        'set': {1, 2, 3},
        'list': [
            decimal.Decimal('1.12'),
            DecimalUnit('1', 'USD'),
        ]
    }, default=bua.json_default)
    expected = json.dumps({
        'date': '2000-01-01 10:11:12',
        'set': [1, 2, 3],
        'list': [
            1.12,
            1
        ]
    })
    assert result == expected


class TestProcess(object):
    class Geo(object):
        aa_name = 'GeoOvershipmentAutoAnalyzer'

        order_id = 1
        service_id = constants.ServiceId.GEOCON
        service_order_id = 123456
        order = '{}-{}'.format(service_id, service_order_id)

    class Adfox(object):
        aa_name = 'AdfoxSignificantOvershipmentAutoAnalyzer'

        order_id = 2
        service_id = constants.ServiceId.ADFOX
        service_order_id = 123457
        order = '{}-{}'.format(service_id, service_order_id)

    @property
    def rows(self):
        acts_dt = '2021-01-31'
        return [
            {
                'order_id': self.Geo.order_id,
                'service_id': self.Geo.service_id,
                'service_order_id': self.Geo.service_order_id,
                't1_value': 200,
                't2_value': 100,
                'acts_dt': acts_dt,
                'hang_correction_qty': 0,
                'reversed_acted_correction_qty': 0,
            },
            {
                'order_id': self.Adfox.order_id,
                'service_id': self.Adfox.service_id,
                'service_order_id': self.Adfox.service_order_id,
                't1_value': 2000,
                't2_value': 0,
                'acts_dt': acts_dt,
                'hang_correction_qty': 0,
                'reversed_acted_correction_qty': 0,
            },
        ]

    def test_process_bua_aa(self, session):
        matched = bua.process(session, 'bua', {}, self.rows)
        assert len(matched) == 29

        matched = bua.process(session, 'bua', {}, self.rows, aa_name=self.Geo.aa_name)
        assert len(matched) == 1

        actual = matched[self.Geo.aa_name]
        expected = [{'order_id': 1, 'overshipment': 100.0, 'order': self.Geo.order}]
        assert actual == expected

    def test_process_bua_partners_aa(self, session):
        matched = bua.process(session, 'bua_partners', {}, self.rows)
        assert len(matched) == 3

        actual = matched[self.Adfox.aa_name]
        expected = [{'order_id': 2, 'overshipment': 2000.0, 'order': self.Adfox.order}]
        assert actual == expected
