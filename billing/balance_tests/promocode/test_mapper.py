# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import pytest
from hamcrest import assert_that, raises, calling

from balance import exc
from balance import mapper
from balance import muzzle_util as ut
from balance import constants as cst
from balance.actions import promocodes

from tests import object_builder as ob
from tests.balance_tests.promocode.common import (
    NOW,
    create_promocode,
    create_client,
    reserve_promocode,
    create_order,
    create_invoice,
)

pytestmark = [
    pytest.mark.promo_code,
]


class TestPromoCode(object):
    """Тестирование маппера промокодов и групп промокодов
    """

    today = datetime.datetime.today().replace(microsecond=0)
    start_dt = today
    end_dt = start_dt + datetime.timedelta(days=10)
    calc_class_name = 'FixedDiscountPromoCodeGroup'
    event_name_1 = u'Unit Test Event 1 %s' % today.isoformat()
    event_name_2 = u'Unit Test Event 2 %s' % today.isoformat()
    event_names = [event_name_1, event_name_2]
    firm_id = cst.FirmId.YANDEX_OOO
    tickets_ids = ['ticket unit test %s' % ind for ind in xrange(3)]

    data = {
        tickets_ids[0]:  # уникальное имя для поиска по промокодам
            {
                'calc_class_name': calc_class_name,
                'calc_params': {
                    'discount_pct': 66,
                },
                'checks': {'new_clients_only': 1, 'skip_reservation_check': 2, 'service_ids': [7]},
                'promocode_info_list': [
                    {'code': 'UNITTEST11', 'client_id': None},
                    {'code': 'UNITTEST12', 'client_id': None},
                    {'code': 'UNITTEST13', 'client_id': None},
                    {'code': 'UNITTEST14', 'client_id': None}
                ],
                'event_name': event_name_1,
                'firm_id': firm_id,
                'ticket_id': tickets_ids[0],
            },
        tickets_ids[1]:
            {
                'calc_class_name': calc_class_name,
                'calc_params': {
                    'discount_pct': 66,
                },
                'checks': {'new_clients_only': 3, 'skip_reservation_check': 0, 'service_ids': [7]},
                'promocode_info_list': [
                    {'code': 'UNITTEST21', 'client_id': None},
                    {'code': 'UNITTEST22', 'client_id': None},
                    {'code': 'UNITTEST23', 'client_id': None},
                    {'code': 'UNITTEST24', 'client_id': None}
                ],
                'event_name': event_name_1,
                'firm_id': firm_id,
                'ticket_id': tickets_ids[1],
            },
        tickets_ids[2]:
            {
                'calc_class_name': calc_class_name,
                'calc_params': {
                    'discount_pct': 66,
                },
                'checks': {'need_unique_urls': 0, 'service_ids': [7], '_product_ids': [cst.DIRECT_PRODUCT_ID]},
                'promocode_info_list': [
                    {'code': 'UNITTEST31', 'client_id': None},
                    {'code': 'UNITTEST32', 'client_id': None},
                    {'code': 'UNITTEST33', 'client_id': None},
                    {'code': 'UNITTEST34', 'client_id': None}
                ],
                'event_name': event_name_2,
                'firm_id': firm_id,
                'ticket_id': tickets_ids[2],
            },
    }

    codes = [promocode_info['code']
             for ticket_id, item in data.iteritems()
             for promocode_info in item['promocode_info_list']]

    def _create_promocodes(self, session):
        for _i, promocode_data in self.data.iteritems():
            promocode_data.update({'session': session, 'start_dt': self.start_dt, 'end_dt': self.end_dt})
            session.add(mapper.PromoCodeGroup.create_promocodes(**promocode_data))
            session.flush()

    def test_create_promocodes(self, session):
        self._create_promocodes(session)
        promocodes = session.query(mapper.PromoCode).filter(mapper.PromoCode.code.in_(self.codes)).all()
        promocode_groups = {promocode.group for promocode in promocodes}
        events = session.query(mapper.PromoCodeEvent).filter(mapper.PromoCodeEvent.event.in_(self.event_names)).all()

        assert len(promocodes) == len(self.codes)
        assert len(promocode_groups) == len(self.data)
        assert len(events) == len(self.event_names)

        for group in promocode_groups:
            primary_promocode_data = self.data[group.ticket_id]

            assert group.start_dt.date() == self.start_dt.date()
            assert group.end_dt.date() == self.end_dt.date()
            assert group.calc_params == primary_promocode_data.get('calc_params', None)
            assert group.firm_id == primary_promocode_data.get('firm_id', None)
            assert group.event.event == primary_promocode_data.get('event_name', None)
            promocode_info_list = primary_promocode_data.get('promocode_info_list', None)
            assert sorted(group.codes) == sorted([promocode_info['code'] for promocode_info in promocode_info_list])
            assert group.new_clients_only == primary_promocode_data['checks'].get('new_clients_only', None)
            assert group.valid_until_paid == primary_promocode_data['checks'].get('valid_until_paid', None)
            assert group.need_unique_urls == primary_promocode_data['checks'].get('need_unique_urls', None)
            assert group.skip_reservation_check == primary_promocode_data['checks'].get('skip_reservation_check', None)
            assert group.minimal_amounts == primary_promocode_data['checks'].get('minimal_amounts', None)
            assert group.calc_class_name == 'FixedDiscountPromoCodeGroup'

    def test_create_promocodes_fail(self, session):
        data = self.data[self.tickets_ids[0]]
        data['session'] = session
        data['start_dt'] = self.start_dt
        data['end_dt'] = self.end_dt
        data['calc_class_name'] = 'FailClassName'
        assert_that(
            calling(mapper.PromoCodeGroup.create_promocodes).with_args(**data),
            raises(exc.INVALID_PROMOCODE_CLASS),
        )

    def test_promocode_normalize(self, session):
        promocode_info_list = [{'code': 'Normalise-123', 'client_id': None}]
        pc_group = ob.PromoCodeGroupBuilder(promocode_info_list=promocode_info_list).build(session).obj
        assert pc_group.codes == ['NORMALISE123']

    @pytest.mark.parametrize('dt, answer',
                             [
                                 (NOW + datetime.timedelta(days=2), True),
                                 (NOW + datetime.timedelta(days=14), False),
                             ],
                             ids=['reserved', 'not reserved']
                             )
    def test_reservation_is_reserved(self, session, promocode, client, dt, answer):
        reserve_promocode(session, promocode, client)
        session.flush()
        assert promocode.is_reserved(current_dt=dt) == answer

    def test_current_reservation(self, session, promocode):
        client_1 = create_client(session)
        client_2 = create_client(session)

        for dt, client in [
            (NOW, client_1),
            (NOW + datetime.timedelta(days=10), client_2),
        ]:
            reserve_promocode(session, promocode, client, dt)
            session.flush()
        assert len(promocode.reservations) == 2

        current_reservation = promocode.get_current_reservation(current_dt=NOW + datetime.timedelta(days=6))
        assert current_reservation.begin_dt == ut.trunc_date(NOW)
        assert current_reservation.client == client_1


class TestLegacyPromoCodeGroup(object):
    """Тестирование маппера LegacyPromoCodeGroup"""

    start_dt = NOW
    end_dt = start_dt + datetime.timedelta(days=10)

    @pytest.fixture()
    def pc_params(self):
        return {
            'start_dt': NOW,
            'end_dt': NOW + datetime.timedelta(days=10),
            'calc_class_name': 'LegacyPromoCodeGroup',
            'calc_params': {
                'bonus1': D('100'),
                'bonus2': D('200'),
                'middle_dt': NOW + datetime.timedelta(days=5),
                'minimal_qty': 10,
                'multicurrency_bonuses': {'RUB': {'bonus1': 1000, 'bonus2': 2000, 'minimal_qty': 10}},
                'discount_pct': 10,
            },
            'service_ids': [cst.ServiceId.DIRECT, cst.ServiceId.MARKET_PARTNERS],
        }

    @pytest.fixture
    def promocode(self, session):
        return create_promocode(session, {'calc_class_name': 'LegacyPromoCodeGroup'})

    def test_create(self, session, pc_params):
        pc = create_promocode(session, params=pc_params)
        session.expire_all()

        pc_group = pc.group
        req_params = pc_params['calc_params']
        assert pc_group.__class__.__name__ == pc_params['calc_class_name']
        assert pc_group.bonus1 == req_params['bonus1']
        assert pc_group.bonus2 == req_params['bonus2']
        assert pc_group.middle_dt == req_params['middle_dt']
        assert pc_group.calc_params['middle_dt'] == req_params['middle_dt'].strftime('%Y-%m-%d %H:%M:%S')
        assert pc_group.discount_pct == req_params['discount_pct']
        assert pc_group.multicurrency_bonuses == req_params['multicurrency_bonuses']
        assert pc_group.service_ids == pc_params['service_ids']
        assert pc_group.minimal_amounts == {'FISH': 10, 'RUB': 10}

    def test_category_bonus(self, session, pc_params):
        pc_params['calc_params'].pop('discount_pct')
        pc = create_promocode(session, params=pc_params)
        assert pc.group.category == mapper.LegacyPromoCodeGroup.BONUS

    def test_category_discount(self, session, pc_params):
        pc_params['calc_params']['middle_dt'] = pc_params['end_dt']
        pc_params['calc_params']['bonus1'] = 0
        pc_params['calc_params']['bonus2'] = 0
        pc_params['calc_params']['discount_pct'] = 10
        pc = create_promocode(session, params=pc_params)
        assert pc.group.category == mapper.LegacyPromoCodeGroup.DISCOUNT

    def test_category_fail(self, session, pc_params):
        pc = create_promocode(session, params=pc_params)
        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            r = pc.group.category
        error_text = 'Invalid parameter for function: Invalid category for group promo id %s' % pc.group.id
        assert exc_info.value.msg == error_text

    @pytest.mark.parametrize(
        'on_dt, result',
        [
            [datetime.datetime.now() + datetime.timedelta(1), True],
            [datetime.datetime.now() - datetime.timedelta(666), False]
        ]
    )
    def test_is_active(self, promocode, on_dt, result):
        assert result == promocode.group.is_active(on_dt)

    @pytest.mark.parametrize(
        'invoice_dt, product_id, res_qty, res_money',
        [
            (NOW + datetime.timedelta(days=1), cst.DIRECT_PRODUCT_ID, D(10), D(300)),
            (NOW + datetime.timedelta(days=1), cst.DIRECT_PRODUCT_RUB_ID, D(120), D(120)),
            (NOW + datetime.timedelta(days=3), cst.DIRECT_PRODUCT_ID, D(20), D(600)),
            (NOW + datetime.timedelta(days=3), cst.DIRECT_PRODUCT_RUB_ID, D(240), D(240)),
        ],
        ids=['bonus1-fishes', 'bonus1-RUB', 'bonus2-fishes', 'bonus2-RUB'],
    )
    def test_get_bonus(self, session, promocode, client, invoice_dt, product_id, res_qty, res_money):
        qty = 1000
        pc_group = promocode.group

        pc_group.calc_params = {
            'middle_dt': NOW + datetime.timedelta(days=2),
            'bonus1': D(10),
            'bonus2': D(20),
            'multicurrency_bonuses': {'RUB': {'bonus1': D(100), 'bonus2': D(200)}}
        }
        session.flush()

        order = create_order(session, client, product_id)
        invoice = create_invoice(session, qty, client, [order])
        invoice.dt = invoice_dt
        session.flush()

        invoice_ns = promocodes.InvoicePromocodeNamespace(invoice)
        assert (res_qty, res_money) == pc_group.get_bonus(invoice_ns)

    @pytest.mark.parametrize('product_id, calc_params, res',
                             [
                                 (cst.DIRECT_PRODUCT_ID, {'discount_pct': D(99)}, D(99)),
                                 (cst.DIRECT_PRODUCT_ID, {'bonus1': 3000, 'bonus2': 1}, D(75)),
                                 (cst.DIRECT_PRODUCT_RUB_ID,
                                  {'bonus1': 1, 'bonus2': 1, 'multicurrency_bonuses': {'RUB': {'bonus1': D(1000)}}},
                                  D('54.55')),
                             ],
                             ids=['discount_pct', 'bonus-fishes', 'bonus-RUB'],
                             )
    def test_get_discount_pct(self, session, client, promocode, product_id, calc_params, res):
        qty = 1000

        pc_group = promocode.group
        pc_group.calc_params = calc_params
        session.flush()

        order = create_order(session, client, product_id)
        invoice = create_invoice(session, qty, client, [order])
        io, = invoice.invoice_orders

        invoice_ns = promocodes.InvoicePromocodeNamespace(invoice)
        assert ut.round00(pc_group.get_discount_info(invoice_ns, promocode).discount(io)) == ut.round00(res)

    @pytest.mark.parametrize('new_calc_params',
                             [
                                 {'service_ids': 7},
                                 {'multicurrency_bonuses': []},
                                 {'middle_dt': end_dt + datetime.timedelta(days=1)},
                                 {'bonus1': 1, 'bonus2': 1, 'discount_pct': 1},
                                 {'bonus1': 1, 'bonus2': 1, 'discount_pct': 0, 'middle_dt': start_dt},
                                 {'bonus1': 0, 'bonus2': 0, 'discount_pct': 1, 'middle_dt': end_dt},
                             ],
                             ids=[
                                 'invalid_service',
                                 'invalid_multicurrency_bonuses',
                                 'invalid_middle_dt',
                                 'bonus_and_discount',
                                 'invalid_bonus_condition',
                                 'invalid_discount_condition',
                             ],
                             )
    def test_validate_group(self, promocode, new_calc_params):
        pc_group = promocode.group
        pc_group.start_dt = self.start_dt
        pc_group.end_dt = self.end_dt
        calc_params = {
                    "service_ids": [7],
                    "middle_dt": self.start_dt + datetime.timedelta(days=1),
                    "multicurrency_bonuses": {"RUB": {"bonus1": 15000, "bonus2": 15000}},
                    "discount_pct": u"0",
                    "bonus1": u"500",
                    "bonus2": u"500"
                }
        calc_params.update(new_calc_params)
        pc_group.calc_params = calc_params

        assert_that(calling(pc_group.validate), exc.INVALID_PROMOCODE_PARAMS)
