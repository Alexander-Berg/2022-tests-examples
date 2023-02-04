# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D
import sqlalchemy as sa
import pytest
import hamcrest as hm

from balance import exc
from balance import mapper
from balance import muzzle_util as ut
from balance import constants as cst
from balance.actions import promocodes
from balance.actions.consumption import reverse_consume
from balance.actions.request import RequestAction

from tests import object_builder as ob
from tests.balance_tests.promocode.common import (
    NOW,
    create_promocode,
    create_client,
    reserve_promocode,
    create_invoice,
    create_order,
    complete,
)

pytestmark = [
    pytest.mark.promo_code,
]


class TestReservation(object):
    """Тестирование резервации промокода за клиентом
    """

    def test_reserve_promocode(self, session, promocode, client):
        reserve_promocode(session, promocode, client)
        session.flush()
        assert len(promocode.reservations) == 1

        reservation = promocode.reservations[0]
        assert reservation.client_id == client.id
        assert reservation.promocode_id == promocode.id
        assert reservation.begin_dt == ut.trunc_date(NOW)
        assert reservation.end_dt == promocode.group.end_dt

    def test_reserve_promocode_fail(self, session, promocode, client):
        reserve_promocode(session, promocode, client)
        hm.assert_that(
            hm.calling(reserve_promocode).with_args(session, promocode, client),
            hm.raises(exc.INVALID_PROMO_CODE)
        )

    @pytest.mark.parametrize(
        'dt, answer',
        [
            pytest.param(NOW + datetime.timedelta(days=2), True, id='active'),
            pytest.param(NOW + datetime.timedelta(days=14), False, id='not active'),
        ],
    )
    def test_reservation_is_active(self, reservation, dt, answer):
        assert reservation.is_active(on_dt=dt) == answer

    def test_check_fail(self, session, client):
        pc1 = create_promocode(session, params={'promocode_info_list': [{'code': '666', 'client_id': None}]})
        pc2 = create_promocode(session, params={'promocode_info_list': [{'code': '777', 'client_id': None}]})

        reserve_promocode(session, pc1, client)
        with pytest.raises(exc.CANT_RESERVE_PROMOCODE) as exc_info:
            reserve_promocode(session, pc2, client)

        assert pc1.code == exc_info.value.promocode
        assert [pc1] == [pcr.promocode for pcr in client.promocode_reservations]

    def test_check_ok_new(self, session, client):
        pc1 = create_promocode(session, params={'promocode_info_list': [{'code': '666', 'client_id': None}]})
        pc2 = create_promocode(session, params={'promocode_info_list': [{'code': '777', 'client_id': None}],
                                                'skip_reservation_check': 1})

        reserve_promocode(session, pc1, client)
        reserve_promocode(session, pc2, client)

        assert [pc1, pc2] == [pcr.promocode for pcr in client.promocode_reservations]

    def test_check_ok_old(self, session, client):
        pc1 = create_promocode(session, params={'promocode_info_list': [{'code': '666', 'client_id': None}],
                                                'skip_reservation_check': 1})
        pc2 = create_promocode(session, params={'promocode_info_list': [{'code': '777', 'client_id': None}]})

        reserve_promocode(session, pc1, client)
        reserve_promocode(session, pc2, client)

        assert [pc1, pc2] == [pcr.promocode for pcr in client.promocode_reservations]

    def test_reservation_for_request__first(self, session, client, promocode):
        """Резервируем промокод с пометкой for_request"""
        res = reserve_promocode(session, promocode, client, for_request=True)
        assert res.for_request == 1

    def test_reservation_for_request__second(self, session, client, promocode):
        """Т.к. мы можем создать несколько одинаковых реквестов,
         то не должны падать при повторной резервации промокода с пометкой for_request,
         но и плодить резервации не будем
         """
        reserve_promocode(session, promocode, client, for_request=True)
        reserve_promocode(session, promocode, client, for_request=True)
        reservations = (
            session.query(mapper.PromoCodeReservation)
            .filter(
                mapper.PromoCodeReservation.client == client,
                mapper.PromoCodeReservation.promocode == promocode,
                mapper.PromoCodeReservation.for_request == sa.text('1'),
            )
            .all()
        )
        assert len(reservations) == 1

    @pytest.mark.parametrize(
        'first_for_request',
        [True, False],
    )
    def test_reservation_for_request__second_reservation_wo_request(self, session, client, promocode, first_for_request):
        """1. Если есть реквестная резервация, то не даем зарезервировать его обычно
        2. Если уже есть обычная резервация, то не даем зарезервировать с for_request
        """
        reserve_promocode(session, promocode, client, for_request=first_for_request)
        with pytest.raises(exc.CANT_RESERVE_PROMOCODE) as exc_info:
            reserve_promocode(session, promocode, client, for_request=not first_for_request)
        assert exc_info.value.msg == 'Can\'t reserve promocode. Already has reservation for {}'.format(promocode.code)

    def test_reservation_for_request__other_client(self, session, client, promocode):
        """Не даем зарезервировать промокод c for_request, если он уже зарезервирован за другим клиентом
        """
        other_client = create_client(session)
        reserve_promocode(session, promocode, other_client, for_request=True)
        with pytest.raises(exc.INVALID_PC_RESERVED_ON_ANOTHER_CLIENT) as exc_info:
            reserve_promocode(session, promocode, client, for_request=True)
        assert exc_info.value.msg == 'Invalid promo code: ID_PC_RESERVED_ON_ANOTHER_CLIENT'


class TestActions(object):
    """Тестирование базовых действий над промокодами"""

    @pytest.mark.usefixtures('reservation')
    def test_get_promo_code(self, promocode, invoice):
        invoice_pc = promocodes.get_promo_code_for_invoice(invoice)
        assert invoice_pc == promocode

    def test_reserve_promo_code(self, promocode, client):
        promocodes.reserve_promo_code(client, promocode, NOW)
        get_current_reservation = promocode.get_current_reservation(NOW)
        assert get_current_reservation.client == client

    @pytest.mark.usefixtures('reservation')
    def test_calc_for_invoice(self, invoice):
        pc, discount_info = promocodes.calc_promo_code_for_invoice(invoice, cst.PromocodeApplyTypes.ON_TURN_ON)
        assert isinstance(pc, mapper.PromoCode)
        assert discount_info.discount

    @pytest.mark.parametrize(
        'apply_type, is_applied',
        [
            (cst.PromocodeApplyTypes.ON_TURN_ON, True),
            (cst.PromocodeApplyTypes.ON_CREATE, False)
        ]
    )
    @pytest.mark.usefixtures('reservation')
    def test_calc_for_invoice_w_promocode(self, session, invoice, apply_type, is_applied):
        invoice.promo_code = create_promocode(session)
        session.flush()
        io, = invoice.invoice_orders

        res = promocodes.calc_promo_code_for_invoice(invoice, apply_type)
        if is_applied:
            assert res[0] == invoice.promo_code
            assert res[1] is not None
            assert res[1].discount(io) == D('66')
            assert invoice.promo_code is not None
        else:
            assert (None, None) == res
            assert invoice.promo_code is not None

    @pytest.mark.usefixtures('reservation')
    def test_calc_for_invoice_credit(self, session, invoice):
        invoice.credit = 1
        session.flush()

        res = promocodes.calc_promo_code_for_invoice(invoice, cst.PromocodeApplyTypes.ON_TURN_ON)
        assert (None, None) == res


class TestGetOrderPromocodes(object):
    def test_all_funds_to_overact(self, session, promocode):
        """
        invoice1
        order1
        - w_pc 10/5 10/5 20/10
        order2
        - w_pc 10/5 5/2.5 0/0
        Overact: 5
        На переакт уходят все свободные средства, нотификация уходит пустой
        """
        order = create_order(session)
        invoice = create_invoice(session, qty=10, orders=[order])
        invoice.create_receipt(invoice.effective_sum)
        invoice.promo_code = promocode
        session.flush()

        invoice.transfer(order, discount_obj=mapper.DiscountObj(0, 50, invoice.promo_code))
        complete(order, order.consume_qty)
        invoice.generate_act(force=1, backdate=datetime.datetime.now())
        complete(order, 10)

        order_alt = create_order(session, order.client, mapper.DIRECT_PRODUCT_RUB_ID)
        order.transfer(order_alt)
        complete(order_alt, 5)
        session.flush()

        with pytest.raises(exc.CANT_TEAR_PC_OFF_NO_FREE_CONSUMES) as exc_info:
            promocodes.tear_promocode_off(session, invoice)
        assert exc_info.value.msg == "Can't tear promocode off: PC_TEAR_OFF_NO_FREE_CONSUMES"

        for o in [order, order_alt]:
            res = promocodes.get_order_promocodes(o)
            params = {
                'order': hm.has_properties('id', o.id),
                'invoice': hm.has_property('id', invoice.id),
            }
            if o is order:
                params = None
            else:
                params.update({
                    'promo_code': hm.has_property('id', promocode.id),
                    'available_promocode_qty': D(0),
                    'unused_promocode_qty': D('2.5'),
                })
            hm.assert_that(res, hm.contains(hm.has_properties(params)) if params else hm.empty())

    def test_free_funds_minus_overact(self, session, promocode):
        """invoice1
        order1
        - 0/0 0/0 90/90
        - 200/100 0/0 0/0
        Overact: 90
        Free funds: 10
        """
        order = create_order(session)

        invoice = create_invoice(session, qty=50, orders=[order])
        invoice.promo_code = promocode
        session.flush()

        invoice.create_receipt(100)
        invoice.transfer(order)
        complete(order, 90)
        invoice.generate_act(force=1, backdate=datetime.datetime.now())
        complete(order, 0)
        reverse_consume(invoice.consumes[0], None, 100)

        invoice.transfer(order, mode=1, sum=100, discount_obj=mapper.DiscountObj(0, 50, invoice.promo_code))
        session.flush()

        res = promocodes.get_order_promocodes(order)
        params = {
            'order': hm.has_properties('id', order.id),
            'invoice': hm.has_property('id', invoice.id),
        }
        params.update({
            'promo_code': hm.has_property('id', promocode.id),
            'available_promocode_qty': D('10'),
            'unused_promocode_qty': D('100'),
        })
        hm.assert_that(res, hm.contains(hm.has_properties(params)))

    def test_all_overacted_on_different_consume(self, session, promocode):
        """
        invoice1
        order1
        - w_pc 0/0 0/0 100/50
        order2
        - w_pc 100/50 0/0 0/0
        """
        order = create_order(session)
        invoice = create_invoice(session, qty=50, orders=[order])
        invoice.create_receipt(invoice.effective_sum)
        invoice.promo_code = promocode
        session.flush()

        invoice.transfer(order, discount_obj=mapper.DiscountObj(0, 50, invoice.promo_code))
        complete(order, 100)
        invoice.generate_act(force=1, backdate=datetime.datetime.now())
        complete(order, 0)

        order_alt = create_order(session, order.client, mapper.DIRECT_PRODUCT_RUB_ID)
        order.transfer(order_alt)
        session.flush()

        for o in [order, order_alt]:
            res = promocodes.get_order_promocodes(o)
            params = {
                'order': hm.has_properties('id', o.id),
                'invoice': hm.has_property('id', invoice.id),
            }
            if o is order:
                params = None
            else:
                params.update({
                    'promo_code': hm.has_property('id', promocode.id),
                    'available_promocode_qty': D(0),
                    'unused_promocode_qty': D('50'),
                })
            hm.assert_that(res, hm.contains(hm.has_properties(params)) if params else hm.empty())

    def test_overact_on_different_invoice(self, session, promocode):
        """Один заказ - два счета
        invoice1
        - wo_pc 0/0 0/0 90/90
        invoice2
        - w_pc 200/200 0/0 0/0
        """
        order = create_order(session)

        invoice1 = create_invoice(session, qty=50, orders=[order])
        invoice1.create_receipt(100)
        invoice1.transfer(order)
        complete(order, 90)
        invoice1.generate_act(force=1, backdate=datetime.datetime.now())
        complete(order, 0)
        reverse_consume(invoice1.consumes[0], None, 100)
        session.flush()

        invoice2 = create_invoice(session, qty=50, orders=[order])
        invoice2.promo_code = promocode
        invoice2.create_receipt(100)
        session.flush()
        invoice2.transfer(order, mode=1, sum=100, discount_obj=mapper.DiscountObj(0, 50, invoice2.promo_code))

        res = promocodes.get_order_promocodes(order)
        params = {
            'order': hm.has_properties('id', order.id),
            'invoice': hm.has_property('id', invoice2.id),
            'promo_code': hm.has_property('id', promocode.id),
            'available_promocode_qty': D('100'),
            'unused_promocode_qty': D('100'),
        }
        hm.assert_that(res, hm.contains(hm.has_properties(params)))

    def test_overact_w_several_consumes(self, session, promocode):
        """invoice1
        order1
        - wo_pc 0/0 0/0 60/60
        order2
        - w_pc 100/50 0/0 0/0
        - wo_pc 50/50 0/0 0/0
        Overact: 50
        Free funds: 80 (снимаем 10), 0 (все снимаем)
        """
        order = create_order(session)
        invoice = create_invoice(session, qty=50, orders=[order])
        invoice.promo_code = promocode
        session.flush()

        invoice.create_receipt(60)
        invoice.transfer(order)
        complete(order, 60)
        invoice.generate_act(force=1, backdate=datetime.datetime.now())
        complete(order, 0)
        reverse_consume(invoice.consumes[0], None, 60)

        order_alt = create_order(session, order.client, mapper.DIRECT_PRODUCT_RUB_ID)
        invoice.create_receipt(50)
        invoice.transfer(order_alt, mode=1, sum=50, discount_obj=mapper.DiscountObj(0, 50, invoice.promo_code))
        invoice.create_receipt(50)
        invoice.transfer(order_alt, mode=1, sum=50)

        session.flush()

        for o in [order, order_alt]:
            res = promocodes.get_order_promocodes(o)
            params = {
                'order': hm.has_properties('id', o.id),
                'invoice': hm.has_property('id', invoice.id),
            }
            if o is order:
                params = None
            else:
                params.update({
                    'promo_code': hm.has_property('id', promocode.id),
                    'available_promocode_qty': D('40'),
                    'unused_promocode_qty': D('50'),
                })
            hm.assert_that(res, hm.contains(hm.has_properties(params)) if params else hm.empty())

    def test_overact_compensate_w_other_consume(self, session, promocode):
        """ Весь переакт компенсируется средствами с конзюма без промокода
        invoice1
        order1
        - wo_pc 0/0 0/0 60/60
        order2
        - w_pc 60/30 0/0 0/0
        - wo_pc 60/60 0/0 0/0
        Overacted: 60
        Free funds: 60, 0
        """
        order = create_order(session)
        invoice = create_invoice(session, qty=50, orders=[order])
        invoice.promo_code = promocode
        session.flush()

        invoice.create_receipt(60)
        invoice.transfer(order)
        complete(order, 60)
        invoice.generate_act(force=1, backdate=datetime.datetime.now())
        complete(order, 0)
        reverse_consume(invoice.consumes[0], None, 60)

        order_alt = create_order(session, order.client, mapper.DIRECT_PRODUCT_RUB_ID)
        invoice.create_receipt(30)
        invoice.transfer(order_alt, mode=1, sum=30, discount_obj=mapper.DiscountObj(0, 50, invoice.promo_code))
        invoice.create_receipt(60)
        invoice.transfer(order_alt, mode=1, sum=60)

        session.flush()

        for o in [order, order_alt]:
            res = promocodes.get_order_promocodes(o)
            params = {
                'order': hm.has_properties('id', o.id),
                'invoice': hm.has_property('id', invoice.id),
            }
            if o is order:
                params = None
            else:
                params.update({
                    'promo_code': hm.has_property('id', promocode.id),
                    'available_promocode_qty': D('30'),
                    'unused_promocode_qty': D('30'),
                })
            hm.assert_that(res, hm.contains(hm.has_properties(params)) if params else hm.empty())

    def test_overact_compensate_w_other_order(self, session, promocode):
        """Переакт компенсируется свободными средствами с другого счета
        invoice1
        order1
        - wo_pc 0/0 0/0 140/140
        order2
        - wo_pc 100/100 0/0 0/0
        order1
        - w_pc 100/50 0/0 0/0
         Overact: 140
         Free funds: 10
        """
        order = create_order(session)
        order_alt = create_order(session, order.client, mapper.DIRECT_PRODUCT_RUB_ID)
        invoice = create_invoice(session, qty=10000, orders=[order])
        invoice.create_receipt(invoice.effective_sum)
        invoice.promo_code = promocode
        session.flush()

        co = invoice.transfer(order_alt, cst.TransferMode.dst, 140).consume
        complete(order_alt, 140)
        invoice.generate_act(force=1, backdate=datetime.datetime.now())
        complete(order_alt, 0)
        reverse_consume(co, None, 140)

        invoice.transfer(order, cst.TransferMode.dst, 100)
        invoice.transfer(order_alt, cst.TransferMode.dst, 100,
                         discount_obj=mapper.DiscountObj(0, 50, invoice.promo_code))

        session.flush()

        for o in [order, order_alt]:
            res = promocodes.get_order_promocodes(o)
            params = {
                'order': hm.has_properties('id', o.id),
                'invoice': hm.has_property('id', invoice.id),
            }
            if o is order:
                params = None
            else:
                params.update({
                    'promo_code': hm.has_property('id', promocode.id),
                    'available_promocode_qty': D('10'),
                    'unused_promocode_qty': D('50'),
                })
            hm.assert_that(res, hm.contains(hm.has_properties(params)) if params else hm.empty())

    def test_overact_and_several_consumes(self, session, promocode):
        """Переакт компенсируется несколькими конзюмами с промокодами
        invoice1
        order1
        - wo_pc 0/0 0/0 100/100
        - w_pc 120/60 0/0 0/0
        - w_pc 100/50 0/0 0/0
        - w_pc 2/1 0/0 0/0
         Overact: 100
         Free funds: 0, 20, 2
        """
        order = create_order(session)
        invoice = create_invoice(session, qty=10000, orders=[order])
        invoice.create_receipt(invoice.effective_sum)
        invoice.promo_code = promocode
        session.flush()

        pc1 = create_promocode(session)
        pc2 = create_promocode(session)
        pc3 = create_promocode(session)

        co = invoice.transfer(order, cst.TransferMode.dst, 100).consume
        complete(order, 100)
        invoice.generate_act(force=1, backdate=datetime.datetime.now())
        complete(order, 0)
        reverse_consume(co, None, 100)

        invoice.transfer(order, cst.TransferMode.dst, 120, discount_obj=mapper.DiscountObj(0, 50, pc1))
        invoice.transfer(order, cst.TransferMode.dst, 100, discount_obj=mapper.DiscountObj(0, 50, pc2))
        invoice.transfer(order, cst.TransferMode.dst, 2, discount_obj=mapper.DiscountObj(0, 50, pc3))

        session.flush()

        res = promocodes.get_order_promocodes(order)
        items = [
            hm.has_properties({
                'promo_code': hm.has_property('id', pc1.id),
                'invoice': hm.has_property('id', invoice.id),
                'available_promocode_qty': D(0),
                'unused_promocode_qty': D('60'),
            }),
            hm.has_properties({
                'promo_code': hm.has_property('id', pc2.id),
                'invoice': hm.has_property('id', invoice.id),
                'available_promocode_qty': D('10'),
                'unused_promocode_qty': D('50'),
            }),
            hm.has_properties({
                'promo_code': hm.has_property('id', pc3.id),
                'invoice': hm.has_property('id', invoice.id),
                'available_promocode_qty': D('1'),
                'unused_promocode_qty': D('1'),
            }),
        ]
        hm.assert_that(res, hm.contains_inanyorder(*items))

    @pytest.mark.cashback
    def test_free_funds_minus_cashback(self, session, order):
        """Тест без учета переакта.
        Сначала отнимаем кешбэк и потом считаем сколько из оставшегося промокод.
        кешбэк = 100 при этом на итоговую цифру он как раз не должен повлиять.
        """
        _cashback = ob.ClientCashbackBuilder.construct(
            session,
            client=order.client,
            bonus=D('100'),
            iso_currency='RUB',
        )
        pc_group = ob.PromoCodeGroupBuilder.construct(
            session,
            calc_class_name='FixedSumBonusPromoCodeGroup',
            calc_params={
                # adjust_quantity и apply_on_create общие для всех типов промокодов
                'adjust_quantity': 1,  # увеличиваем количество (иначе уменьшаем сумму)
                'apply_on_create': 0,  # применяем при создании счёта иначе при включении (оплате)
                # остальные зависят от типа
                'currency_bonuses': {"RUB": D('100')},
                'reference_currency': 'RUB',
            },
        )
        pc = pc_group.promocodes[0]
        promocodes.reserve_promo_code(order.client, pc)

        invoice = create_invoice(session, qty=120, orders=[order])
        invoice.turn_on_rows(apply_promocode=True)

        co = invoice.consumes[0]
        assert co.current_qty == D('340')  # qty + promo + cashback

        complete(order, 170)  # откручиваем половину

        res = promocodes.get_order_promocodes(order)
        params = {
            'order': hm.has_properties('id', order.id),
            'invoice': hm.has_property('id', invoice.id),
            'promo_code': hm.has_property('id', pc.id),
            'available_promocode_qty': D('60'),
            'unused_promocode_qty': D('60'),
        }
        hm.assert_that(res, hm.contains(hm.has_properties(params)))


class TestActionsGetRequestPromocode(object):
    def _get_pc_params(self, apply_on_create, discount):
        return {
            'calc_class_name': 'FixedDiscountPromoCodeGroup',
            'calc_params': {'apply_on_create': apply_on_create, 'discount_pct': discount, 'adjust_quantity': True},
            'skip_reservation_check': True,  # надо, чтобы резервация промокодов проходила успешно
        }

    @pytest.mark.parametrize(
        'apply_on_create',
        [True, False],
    )
    def test_get_promocode_from_request(self, session, order, apply_on_create):
        """Если есть промокод, привязанный к реквесту, то
        его применяем в счете, а не тот, что резервировали ранее
        """

        pc1 = create_promocode(session, self._get_pc_params(apply_on_create, 15))
        pc2 = create_promocode(session, self._get_pc_params(apply_on_create, 50))

        client = order.client

        reserve_promocode(session, pc1, client)
        request_id, _ = RequestAction.create(
            session, client.id,
            [{'ServiceID': order.service_id, 'ServiceOrderID': order.service_order_id, 'Qty': 100}],
            promo_code=pc2.code,
        )
        request = session.query(mapper.Request).getone(request_id)
        invoice = create_invoice(session, client=client, request=request)

        io, = invoice.invoice_orders
        if apply_on_create:
            assert invoice.promo_code is io.promo_code is io.discount_obj.promo_code is pc2
            assert io.discount_obj.promo_code_pct == D('50')

        else:
            assert invoice.promo_code is io.promo_code is io.discount_obj.promo_code is None
            assert io.discount_obj.promo_code_pct == D('0')

            invoice.turn_on_rows(apply_promocode=True)
            co, = invoice.consumes
            assert invoice.promo_code is co.promo_code is co.discount_obj.promo_code is pc2
            assert co.discount_obj.promo_code_pct == D('50')

    def test_get_promocode_from_request__failed_on_create(self, session, order):
        """При создании счёта падаем, если промокод не применить"""
        pc = create_promocode(session, self._get_pc_params(True, 15))
        pc.group.firm_id = cst.FirmId.DRIVE
        session.flush()
        client = order.client

        request_id, _ = RequestAction.create(
            session, client.id,
            [{'ServiceID': order.service_id, 'ServiceOrderID': order.service_order_id, 'Qty': 100}],
            promo_code=pc.code,
        )
        request = session.query(mapper.Request).getone(request_id)

        with pytest.raises(exc.INVALID_PC_FIRM_MISMATCH) as exc_info:
            create_invoice(session, client=client, firm_id=cst.FirmId.YANDEX_OOO, request=request)
        assert exc_info.value.msg == 'Invalid promo code: ID_PC_FIRM_MISMATCH'

    def test_get_promocode_from_request__failed_on_turn_on(self, session, order):
        """При включении счёта, даже если промокод не подходит, включаемся без него"""
        pc = create_promocode(session, self._get_pc_params(False, 15))
        client = order.client

        request_id, _ = RequestAction.create(
            session, client.id,
            [{'ServiceID': order.service_id, 'ServiceOrderID': order.service_order_id, 'Qty': 100}],
            promo_code=pc.code,
        )
        request = session.query(mapper.Request).getone(request_id)
        invoice = create_invoice(session, client=client, request=request)

        # ломаем промокод
        pc.group.firm_id = cst.FirmId.DRIVE
        session.flush()

        invoice.turn_on_rows(apply_promocode=True)
        assert request.promo_code is pc
