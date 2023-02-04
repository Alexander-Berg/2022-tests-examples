# -*- coding: utf-8 -*-

import datetime
import mock
import copy
import StringIO
import json
from decimal import Decimal as D

import sqlalchemy as sa

import balance.constants as const
import balance.muzzle_util as ut
from balance import mapper
from balance.scheme import meta
from tests import object_builder as ob
from balance.completions_fetcher.configurable_partner_completion import ProcessorFactory, BaseFetcher,\
    TVMTicketParameter, CompletionConfig, get_table


# disable TVM for tests
TVMTicketParameter.handle = mock.MagicMock(name='handle', side_effect=lambda x: x)


class TestBase(object):
    _class_id = None
    _start_dt = None
    _end_dt = None

    def prepare_client(self, session):
        return None

    def prepare_completions_func(self, completions):
        return lambda *args, **kwargs: completions

    def prepare_completions(self, *args, **kwargs):
        raise NotImplementedError

    def get_inserted_totals(self, *args, **kwargs):
        raise NotImplementedError

    def get_expected_totals(self, *args, **kwargs):
        raise NotImplementedError

    def do_test(self, session):
        client = self.prepare_client(session)
        completions = self.prepare_completions(client=client)
        completions_func = self.prepare_completions_func(completions=completions)
        cfg = CompletionConfig(self._class_id, 'PARTNER_COMPL', session)
        fetcher = BaseFetcher.children[cfg.fetcher['type']]
        with mock.patch.object(fetcher, 'process', completions_func):
            processor = ProcessorFactory.get_instance(session, cfg, self._start_dt, self._end_dt)
            processor.process(False)
            totals = self.get_inserted_totals(session, cfg.tables['table'], client=client)
            expected_totals = self.get_expected_totals()
            assert totals == expected_totals

            # check regetting completions get same result
            # Rewind StringIO:
            completions.seek(0)
            processor = ProcessorFactory.get_instance(session, cfg, self._start_dt, self._end_dt)
            processor.process(False)
            totals = self.get_inserted_totals(session, cfg.tables['table'], client=client)
            assert totals == expected_totals


class TestAddappter2CompletionsBase(TestBase):
    _class_id = 'addappter2'
    _start_dt = ut.trunc_date(datetime.datetime.now()) + datetime.timedelta(days=666)
    _end_dt = _start_dt

    def prepare_client(self, session):
        return ob.ClientBuilder().build(session).obj

    def prepare_completions(self, *args, **kwargs):
        client = kwargs.get('client')
        compl_tmplt = '{dt};{client_id};{_type};{qty}\r\n'
        default_params = {'dt': self._start_dt.replace(hour=5).strftime('%Y%m%d'),
                          'client_id': client.id,
                          '_type': 'install_count',
                          'qty': 25}
        completions = compl_tmplt.format(**default_params) * 3

        return StringIO.StringIO(completions)

    def get_inserted_totals(self, session, table, *args, **kwargs):
        client = kwargs.get('client')
        t = get_table(table)
        query = sa.select(
            [
                sa.func.sum(t.c.qty).label('qty'),
                sa.func.count().label('count')
            ],
            (t.c.client_id == client.id) & (t.c.dt >= self._start_dt)
            & (t.c.dt < self._end_dt + datetime.timedelta(days=1)),
            t
        )
        return session.execute(query).fetchall()[0]

    def get_expected_totals(self):
        return D(75), 3

    def test_execute(self, session):
        self.do_test(session)


class TestTLogSubventionsBase(TestBase):
    _class_id = None
    _start_dt = None
    _end_dt = None
    _service_id = None

    def prepare_client(self, session):
        return ob.ClientBuilder().build(session).obj

    def prepare_context(self, session):
        current = session.query(mapper.PartnerCompletionsResource) \
            .filter(mapper.PartnerCompletionsResource.dt == self._start_dt) \
            .filter(mapper.PartnerCompletionsResource.source_name == self._class_id).all()

        if not current:
            current = mapper.PartnerCompletionsResource(dt=self._start_dt, source_name=self._class_id)
            session.add(current)
        else:
            current, = current
            current.additional_params = None
        session.flush()

        previous = self.session.query(mapper.PartnerCompletionsResource) \
            .filter(mapper.PartnerCompletionsResource.dt == self._start_dt - datetime.timedelta(days=1)) \
            .filter(mapper.PartnerCompletionsResource.source_name == self._class_id).all()

        if not previous:
            previous = mapper.PartnerCompletionsResource(dt=self._start_dt - datetime.timedelta(days=1),
                                                         source_name=self._class_id,
                                                         additional_params={'finished': session.now()})
            session.add(previous)
        else:
            previous, = previous
            previous.additional_params = {'finished': session.now()}
        session.flush()

    def get_inserted_sidepayments(self, client, table):
        session = client.session
        t = get_table(table)
        query = sa.select(
            [
                t.c.id, t.c.client_id, t.c.service_id, t.c.transaction_id, t.c.transaction_type,
                t.c.payment_type, t.c.dt, t.c.transaction_dt, t.c.currency, t.c.price,
                t.c.extra_dt_0, t.c.extra_str_1, t.c.orig_transaction_id, t.c.extra_str_0, t.c.payload
            ],
            (t.c.client_id == client.id),
            t,
            order_by=t.c.transaction_id
        )
        return session.execute(query).fetchall()

    def _compare_compl_and_sidepayment(self, compl, sidepayment, session):
        raise NotImplementedError

    def do_test(self, session):
        client = self.prepare_client(session)
        self.prepare_context(session)
        completions = self.prepare_completions(client=client)
        completions_func = self.prepare_completions_func(completions=copy.deepcopy(completions))
        cfg = CompletionConfig(self._class_id, 'PARTNER_COMPL', session)
        fetcher = BaseFetcher.children[cfg.fetcher['type']]
        with mock.patch.object(fetcher, 'process', completions_func):
            processor = ProcessorFactory.get_instance(session, cfg, self._start_dt, self._end_dt)
            with mock.patch.object(processor, 'after_process', lambda *args, **kwargs: None):
                processor.process(False)
                session.flush()

        inserted_sidepayments = self.get_inserted_sidepayments(client, cfg.tables['table'])
        completions = (c for c in completions if not c.get('ignore_in_balance'))
        for compl, sidepayment in zip(completions, inserted_sidepayments):
            self._compare_compl_and_sidepayment(compl, sidepayment, session)


class TestTLogTaxiSubventions(TestTLogSubventionsBase):
    _class_id = 'taxi_subvention'
    _start_dt = datetime.datetime(2019, 3, 5)
    _end_dt = _start_dt
    _service_id = const.ServiceId.TAXI_PROMO

    def prepare_completions(self, *args, **kwargs):
        client = kwargs.get('client')
        session = client.session
        compl_dict = {
            u'orig_transaction_id': None,
            u'product': u'subsidy',
            u'event_time': u'2019-03-04T19:17:33.804045+03:00',
            u'transaction_time': u'2019-03-04T23:17:33+00:00',
            u'service_transaction_id': u'ёёё',
            u'transaction_type': u'payment',
            u'currency': u'USD',
            u'amount': u'100500.44',
            u'payment_type': u'unknown',
            u'client_id': u'!REPLACE!',
            u'service_id': self._service_id,
            u'payload': {},  # внимательно - copy (не deepcopy)
            u'transaction_id': u'!REPLACE!',
        }
        cur_transaction_id = session.query(sa.func.nvl(sa.func.max(mapper.SidePayment.id), 0))\
            .filter(mapper.SidePayment.service_id == self._service_id).scalar()

        compl_1 = compl_dict.copy()
        compl_2 = compl_dict.copy()
        compl_3 = compl_dict.copy()
        compl_4 = compl_dict.copy()

        payment1_id = cur_transaction_id + 1
        compl_1.update({
            u'transaction_id': payment1_id,
            u'payload': {u'ProcessThroughTrust': 1},
            u'amount': u'35.672',
            u'client_id': unicode(client.id),
        })

        payment2_id = cur_transaction_id + 2
        compl_2.update({
            u'transaction_id': payment2_id,
            u'payload': {u'alias_id': u'10000'},
            u'amount': u'41',
            u'client_id': unicode(client.id),
            u'product': u'coupon',
        })

        refund1_id = cur_transaction_id + 3
        compl_3.update({
            u'transaction_id': refund1_id,
            u'orig_transaction_id': payment1_id,
            u'payload': {},
            u'amount': u'12',
            u'client_id': unicode(client.id),
            u'transaction_type': u'refund',
        })

        compl_4.update({
            u'ignore_in_balance': u'Y',
            u'client_id': unicode(client.id),
            u'transaction_id': 1000,
        })
        return compl_1, compl_2, compl_3, compl_4

    def _compare_compl_and_sidepayment(self, compl, sidepayment, session):
        assert D(compl[u'amount']) == sidepayment.price
        assert int(compl[u'client_id']) == sidepayment.client_id
        assert compl[u'currency'] == sidepayment.currency
        assert datetime.datetime(2019, 3, 4, 19, 17, 33) == sidepayment.dt
        assert compl[u'orig_transaction_id'] == sidepayment.orig_transaction_id
        assert compl[u'product'] == sidepayment.payment_type
        assert compl[u'service_id'] == sidepayment.service_id
        # assert compl[u'service_transaction_id'] == sidepayment.extra_str_0
        assert compl[u'transaction_id'] == sidepayment.transaction_id
        assert datetime.datetime(2019, 3, 5, 2, 17, 33) == sidepayment.extra_dt_0
        assert compl[u'transaction_type'] == sidepayment.transaction_type
        assert u'2019-03-05' == sidepayment.extra_str_1

        export, = session.query(mapper.Export)\
            .filter(mapper.Export.object_id == sidepayment.id)\
            .filter(mapper.Export.type == 'THIRDPARTY_TRANS')\
            .filter(mapper.Export.classname == 'SidePayment').all()

        if compl[u'payload']:
            if compl[u'payload'].get(u'alias_id'):
                assert sidepayment.extra_str_0 == compl[u'payload'][u'alias_id']

            if u'ProcessThroughTrust' in compl[u'payload']:
                sidepayment_payload = json.loads(sidepayment.payload)
                assert compl[u'payload'][u'ProcessThroughTrust'] == sidepayment_payload[u'ProcessThroughTrust']
                if sidepayment_payload[u'ProcessThroughTrust']:
                    assert export.state == 1
                else:
                    assert export.state == 0
            else:
                assert not sidepayment.payload
                assert export.state == 0
        else:
            assert not sidepayment.payload
            assert export.state == 0

    def test_execute(self, session):
        self.do_test(session)


class TestTLogMarketSubventions(TestTLogSubventionsBase):
    _class_id = 'market_subvention'
    _start_dt = datetime.datetime(2019, 3, 5)
    _end_dt = _start_dt
    _service_id = const.ServiceId.PVZ

    def prepare_completions(self, *args, **kwargs):
        client = kwargs.get('client')
        session = client.session
        compl_dict = {
            u'orig_transaction_id': None,
            u'product': u'subsidy',
            u'event_time': u'2019-03-04T19:17:33.804045+03:00',
            u'transaction_time': u'2019-03-04T23:17:33+00:00',
            u'service_transaction_id': u'ёёё',
            u'transaction_type': u'payment',
            u'currency': u'USD',
            u'amount': u'100500.44',
            u'client_id': u'!REPLACE_int!',
            u'service_id': self._service_id,
            u'payload': None,
            u'transaction_id': u'!REPLACE_int!',
            u'ignore_in_balance': False
        }
        cur_transaction_id = session.query(sa.func.nvl(sa.func.max(mapper.SidePayment.id), 0))\
            .filter(mapper.SidePayment.service_id == self._service_id).scalar()

        compl_1 = compl_dict.copy()
        compl_2 = compl_dict.copy()
        compl_3 = compl_dict.copy()
        compl_4 = compl_dict.copy()

        payment1_id = cur_transaction_id + 1
        compl_1.update({
            u'transaction_id': payment1_id,
            u'amount': u'35.672',
            u'client_id': unicode(client.id),
        })

        payment2_id = cur_transaction_id + 2
        compl_2.update({
            u'transaction_id': payment2_id,
            u'service_transaction_id': u'10000ё',
            u'amount': u'41',
            u'client_id': unicode(client.id),
            u'product': u'coupon',
        })

        refund1_id = cur_transaction_id + 3
        compl_3.update({
            u'transaction_id': refund1_id,
            u'orig_transaction_id': payment1_id,
            u'amount': u'12',
            u'client_id': unicode(client.id),
            u'transaction_type': u'refund',
        })

        compl_4.update({
            u'client_id': unicode(client.id),
            u'transaction_id': 1000,
            u'ignore_in_balance': True,
        })
        return compl_1, compl_2, compl_3, compl_4

    def _compare_compl_and_sidepayment(self, compl, sidepayment, session):
        assert D(compl[u'amount']) == sidepayment.price
        assert int(compl[u'client_id']) == sidepayment.client_id
        assert compl[u'currency'] == sidepayment.currency
        assert datetime.datetime(2019, 3, 4, 19, 17, 33) == sidepayment.dt
        assert compl[u'orig_transaction_id'] == sidepayment.orig_transaction_id
        assert compl[u'product'] == sidepayment.payment_type
        assert compl[u'service_id'] == sidepayment.service_id
        assert compl[u'service_transaction_id'] == sidepayment.extra_str_0
        assert compl[u'transaction_id'] == sidepayment.transaction_id
        assert datetime.datetime(2019, 3, 5, 2, 17, 33) == sidepayment.transaction_dt
        assert compl[u'transaction_type'] == sidepayment.transaction_type
        assert u'2019-03-05' == sidepayment.extra_str_1

        export, = session.query(mapper.Export)\
            .filter(mapper.Export.object_id == sidepayment.id)\
            .filter(mapper.Export.type == 'THIRDPARTY_TRANS')\
            .filter(mapper.Export.classname == 'SidePayment').all()

        # if compl[u'payload']:
        #     if compl[u'payload'].get(u'alias_id'):
        #         assert sidepayment.extra_str_0 == compl[u'payload'][u'alias_id']
        #
        #     if u'ProcessThroughTrust' in compl[u'payload']:
        #         sidepayment_payload = json.loads(sidepayment.payload)
        #         assert compl[u'payload'][u'ProcessThroughTrust'] == sidepayment_payload[u'ProcessThroughTrust']
        #         if sidepayment_payload[u'ProcessThroughTrust']:
        #             assert export.state == 1
        #         else:
        #             assert export.state == 0
        #     else:
        #         assert not sidepayment.payload
        #         assert export.state == 0
        # else:
        #     assert not sidepayment.payload
        assert export.state == 0

    def test_execute(self, session):
        self.do_test(session)


class TestTLogMarketSortCenters(TestTLogMarketSubventions):
    _service_id = const.ServiceId.SORT_CENTER


class TestTLogMarketDeliveryServices(TestTLogMarketSubventions):
    _service_id = const.ServiceId.MARKET_DELIVERY_SERVICES


class TestTLogAggr(TestBase):

    def prepare_completions(self, *args, **kwargs):
        raise NotImplementedError

    def get_inserted_aggrs(self, session, table):
        raise NotImplementedError

    def _compare_compl_and_aggr(self, compl, aggr, session):
        raise NotImplementedError

    def prepare_completions_func(self, completions):
        return lambda *args, **kwargs: completions

    def prepare_partner_resource(self, session):
        current = session.query(mapper.PartnerCompletionsResource) \
            .filter(mapper.PartnerCompletionsResource.dt == self._start_dt) \
            .filter(mapper.PartnerCompletionsResource.source_name == self._class_id).all()

        if not current:
            current = mapper.PartnerCompletionsResource(dt=self._start_dt, source_name=self._class_id)
            session.add(current)
        else:
            current, = current
            current.additional_params = None

        previous = self.session.query(mapper.PartnerCompletionsResource) \
            .filter(mapper.PartnerCompletionsResource.dt == self._start_dt - datetime.timedelta(days=1)) \
            .filter(mapper.PartnerCompletionsResource.source_name == self._class_id).all()

        if not previous:
            previous = mapper.PartnerCompletionsResource(dt=self._start_dt - datetime.timedelta(days=1),
                                                         source_name=self._class_id,
                                                         additional_params={'finished': session.now()})
            session.add(previous)
        else:
            previous, = previous
            previous.additional_params = {'finished': session.now()}
        return current, previous

    def prepare_context(self, session):
        self.prepare_partner_resource(session)
        session.flush()

    def prepare_force_context(self, session):
        current, previous = self.prepare_partner_resource(session)
        current.additional_params = {'force': True}
        previous.additional_params = {'finished': False}
        session.flush()

    def do_test(self, session):
        completions = self.prepare_completions()
        completions_func = self.prepare_completions_func(completions=copy.deepcopy(completions))
        cfg = CompletionConfig(self._class_id, 'PARTNER_COMPL', session)
        fetcher = BaseFetcher.children[cfg.fetcher['type']]
        with mock.patch.object(fetcher, 'process', completions_func):
            processor = ProcessorFactory.get_instance(session, cfg, self._start_dt, self._end_dt)
            with mock.patch.object(processor, 'after_process', lambda *args, **kwargs: None):
                processor.process(False)
                session.flush()

        inserted_aggrs = self.get_inserted_aggrs(session, cfg.tables['table'])
        for compl, aggr in zip(completions, inserted_aggrs):
            self._compare_compl_and_aggr(compl, aggr, session)


class TestTLogTaxiAggr(TestTLogAggr):
    _class_id = 'taxi_aggr_tlog'
    _start_dt = datetime.datetime(2019, 3, 5)
    _end_dt = _start_dt

    def prepare_completions(self, *args, **kwargs):
        compl_dict = {
            u'amount': u'100500.44',
            u'client_id': u'666',
            u'currency': u'USD',
            u'last_transaction': u'123',
            u'product': u'order',
            u'service_id': u'111',
            u'event_time': u'2019-03-04T23:17:33.804045+00:00',
            u'tlog_version': u'1',
        }
        last_transaction_id = 1010
        compl_1 = compl_dict.copy()
        compl_2 = compl_dict.copy()
        compl_3 = compl_dict.copy()
        compl_4 = compl_dict.copy()

        last1 = last_transaction_id + 1
        compl_1.update({
            u'service_id': u'128',
            u'last_transaction': last1,
            u'amount': u'35.672',
            u'client_id': u'667',
        })

        last2 = last_transaction_id + 2
        compl_2.update({
            u'last_transaction': last2,
            u'amount': u'41',
            u'currency': u'RUB',
            u'product': u'subvention',
        })

        last3 = last_transaction_id + 3
        compl_3.update({
            u'last_transaction': last3,
            u'amount': u'12',
        })

        last4 = last_transaction_id + 4
        compl_4.update({
            u'last_transaction': last4,
            u'service_id': u'668',
            u'product': u'food_payment',
        })

        return compl_1, compl_2, compl_3, compl_4

    def test_execute(self, session):
        self.prepare_context(session)
        self.do_test(session)

    def test_force_execute(self, session):
        self.prepare_force_context(session)
        self.do_test(session)

    def get_inserted_aggrs(self, session, table):
        t = get_table(table)
        query = sa.select(
            [
                t.c.transaction_dt, t.c.dt, t.c.client_id, t.c.commission_currency, t.c.service_id,
                t.c.amount, t.c.type, t.c.last_transaction_id
            ],
            (t.c.transaction_dt == self._start_dt),
            t,
            order_by=t.c.last_transaction_id
        )
        return session.execute(query).fetchall()

    def _compare_compl_and_aggr(self, compl, aggr, session):
        assert self._start_dt == aggr.transaction_dt
        assert datetime.datetime(2019, 3, 5, 2, 17, 33) == aggr.dt
        assert int(compl[u'client_id']) == aggr.client_id
        assert compl[u'currency'] == aggr.commission_currency
        assert int(compl[u'service_id']) == aggr.service_id
        assert D(compl[u'amount']) == aggr.amount
        assert compl[u'product'] == aggr.type
        assert int(compl[u'last_transaction']) == aggr.last_transaction_id


class TestTLogBlueMarketAggr(TestTLogAggr):
    _class_id = 'blue_market_aggr_tlog'
    _start_dt = datetime.datetime(2019, 3, 5)
    _end_dt = _start_dt
    _completion_src = 'blue_market_aggr_tlog'

    def prepare_completions(self, *args, **kwargs):
        compl_dict = {
            u'amount': u'100500.44',
            u'client_id': u'666',
            u'currency': u'USD',
            u'last_transaction_id': u'123',
            u'product': u'fee',
            u'service_id': u'612',
            u'event_time': u'2019-03-04T23:17:33.804045+00:00',
            u'nds': True,
        }
        last_transaction_id = 1010
        compl_1 = compl_dict.copy()
        compl_2 = compl_dict.copy()
        compl_3 = compl_dict.copy()
        compl_4 = compl_dict.copy()

        last1 = last_transaction_id + 1
        compl_1.update({
            u'service_id': u'612',
            u'last_transaction_id': last1,
            u'amount': u'35.672',
            u'client_id': u'667',
        })

        last2 = last_transaction_id + 2
        compl_2.update({
            u'last_transaction_id': last2,
            u'amount': u'41',
            u'currency': u'RUB',
            u'product': u'sorting',
        })

        last3 = last_transaction_id + 3
        compl_3.update({
            u'last_transaction_id': last3,
            u'amount': u'12',
        })

        last4 = last_transaction_id + 4
        compl_4.update({
            u'last_transaction_id': last4,
            u'service_id': u'618',
            u'product': u'trololo',
        })

        return compl_1, compl_2, compl_3, compl_4

    def test_execute(self, session):
        self.prepare_context(session)
        self.do_test(session)

    def test_force_execute(self, session):
        self.prepare_force_context(session)
        self.do_test(session)

    def get_inserted_aggrs(self, session, table):
        t = get_table(table)
        query = sa.select(
            [
                t.c.dt,
                t.c.client_id,
                t.c.currency,
                t.c.service_id,
                t.c.amount,
                t.c.type,
                t.c.last_transaction_id,
                t.c.nds,
            ],
            sa.and_(
                t.c.src_dt == self._start_dt, t.c.completion_src == self._completion_src
            ),
            t,
            order_by=t.c.last_transaction_id
        )
        return session.execute(query).fetchall()

    def _compare_compl_and_aggr(self, compl, aggr, session):
        assert datetime.datetime(2019, 3, 5, 2, 17, 33) == aggr.dt
        assert int(compl[u'client_id']) == aggr.client_id
        assert compl[u'currency'] == aggr.currency
        assert int(compl[u'service_id']) == aggr.service_id
        assert D(compl[u'amount']) == aggr.amount
        assert compl[u'product'] == aggr.type
        assert int(compl[u'last_transaction_id']) == aggr.last_transaction_id
        assert 1 == aggr.nds


class TestTLogBlueMarketingServicesAggr(TestTLogBlueMarketAggr):
    _class_id = 'blue_marketing_services_aggr_tlog'
    _completion_src = 'blue_marketing_services_aggr_tlog'

    def prepare_completions(self, *args, **kwargs):
        compl_dict = {
            u'amount': u'100500.44',
            u'client_id': u'666',
            u'currency': u'RUB',
            u'last_transaction_id': u'123',
            u'product': u'marketing_promo_tv',
            u'service_id': u'1126',
            u'event_time': u'2019-03-04T23:17:33.804045+00:00',
            u'nds': True,
        }
        last_transaction_id = 1010
        compl_1 = compl_dict.copy()
        compl_2 = compl_dict.copy()
        compl_3 = compl_dict.copy()
        compl_4 = compl_dict.copy()

        last1 = last_transaction_id + 1
        compl_1.update({
            u'service_id': u'1126',
            u'last_transaction_id': last1,
            u'amount': u'35.672',
            u'client_id': u'667',
        })

        last2 = last_transaction_id + 2
        compl_2.update({
            u'last_transaction_id': last2,
            u'amount': u'41',
            u'currency': u'RUB',
            u'product': u'marketing_promo_web',
        })

        last3 = last_transaction_id + 3
        compl_3.update({
            u'last_transaction_id': last3,
            u'amount': u'12',
        })

        last4 = last_transaction_id + 4
        compl_4.update({
            u'last_transaction_id': last4,
            u'service_id': u'618',
            u'product': u'trololo',
        })

        return compl_1, compl_2, compl_3, compl_4
