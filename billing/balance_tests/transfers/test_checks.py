# -*- coding: utf-8 -*-

import decimal
import datetime

import pytest

from balance import exc
from balance.actions.promocodes import reserve_promo_code
from balance.actions.consumption import reverse_consume
from balance.actions.process_completions import ProcessCompletions
from balance.actions.transfers_qty.interface import (
    TransferMultiple,
    SrcItem,
    DstItem,
)
from balance.constants import (
    ServiceId,
    FirmId,
    ContractTypeId,
    DIRECT_PRODUCT_RUB_ID,
    DIRECT_PRODUCT_ID,
)

from tests import object_builder as ob
from tests.balance_tests.transfers.common import (
    create_dst,
    create_src,
)

D = decimal.Decimal


def test_unpaid_invoice(session, client, direct_product):
    (src_order,), src_list = create_src(client, direct_product, [D('6')], [D('3')])
    dst_orders, dst_list = create_dst(client, direct_product, [D('1')])

    consume, = src_order.consumes
    consume.invoice.create_receipt(-1)

    with pytest.raises(exc.NOT_ENOUGH_FUNDS_FOR_TRANSFER):
        TransferMultiple(session, src_list, dst_list).do()


def test_services(session, client, direct_product):
    src_orders, src_list = create_src(client, direct_product, [D('6')], [D('3')])
    (dst_order,), dst_list = create_dst(client, direct_product, [D('1')])

    dst_order.service_id = ServiceId.MKB
    session.flush()

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        TransferMultiple(session, src_list, dst_list).do()
    assert 'Transfer not allowed from services' in exc_info.value.msg


class TestClients(object):
    def test_different_direct_clients(self, session, direct_product):
        src_client = ob.ClientBuilder.construct(session)
        src_orders, src_list = create_src(src_client, direct_product, [D('6')], [D('3')])

        dst_client = ob.ClientBuilder.construct(session)
        dst_orders, dst_list = create_dst(dst_client, direct_product, [D('1')])

        with pytest.raises(exc.CLIENTS_NOT_MATCH):
            TransferMultiple(session, src_list, dst_list).do()

    def test_different_agencies(self, session, direct_product):
        agency1 = ob.ClientBuilder.construct(session, is_agency=1)
        client1 = ob.ClientBuilder.construct(session, agency=agency1)
        agency2 = ob.ClientBuilder.construct(session, is_agency=1)
        client2 = ob.ClientBuilder.construct(session, agency=agency2)

        src_orders, src_list = create_src(client1, direct_product, [10], [0])
        dst_orders, dst_list = create_dst(client2, direct_product, [D('1')])

        with pytest.raises(exc.CLIENTS_NOT_MATCH):
            TransferMultiple(session, src_list, dst_list).do()

    def test_agency_direct_client(self, session, direct_product):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session)

        src_orders, src_list = create_src(client1, direct_product, [10], [0])
        dst_orders, dst_list = create_dst(client2, direct_product, [D('1')])

        with pytest.raises(exc.CLIENTS_NOT_MATCH):
            TransferMultiple(session, src_list, dst_list).do()

    def test_same_agency_different_subclients(self, session, direct_product):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        src_orders, src_list = create_src(client1, direct_product, [10], [0])
        dst_orders, dst_list = create_dst(client2, direct_product, [D('1')])

        TransferMultiple(session, src_list, dst_list).do()
        assert dst_orders[0].consume_qty == 10

    @pytest.mark.parametrize(
        'is_docs_detailed, is_docs_separated',
        [
            pytest.param(True, True, id='both'),
            pytest.param(True, False, id='separated'),
            pytest.param(False, True, id='detailed'),
            pytest.param(False, False, id='none'),
        ]
    )
    def test_same_agency_separated_subclients(self, session, direct_product, is_docs_detailed, is_docs_separated):
        agency = ob.ClientBuilder.construct(
            session,
            is_agency=1,
            is_docs_separated=is_docs_separated,
            is_docs_detailed=is_docs_detailed,
        )
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        src_orders, src_list = create_src(client1, direct_product, [10], [0])
        dst_orders, dst_list = create_dst(client2, direct_product, [D('1')])

        TransferMultiple(session, src_list, dst_list).do()
        assert dst_orders[0].consume_qty == 10

    @pytest.mark.parametrize(
        'is_docs_detailed, is_docs_separated, has_overact',
        [
            pytest.param(True, True, True, id='both'),
            pytest.param(True, False, True, id='separated'),
            pytest.param(False, True, True, id='detailed'),
            pytest.param(False, False, True, id='none'),
            pytest.param(True, True, False, id='both_no_overact'),
        ]
    )
    def test_same_agency_separated_overacted_subclients(self, session, direct_product,
                                                        is_docs_detailed, is_docs_separated, has_overact):

        agency = ob.ClientBuilder.construct(
            session,
            is_agency=1,
            is_docs_separated=is_docs_separated,
            is_docs_detailed=is_docs_detailed,
        )
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        if has_overact:
            (overact_order, ), _ = create_src(client1, direct_product, [10], [0])
            consume, = overact_order.consumes
            consume.invoice.close_invoice(datetime.datetime.now())
            ProcessCompletions(overact_order).calculate_consumption({overact_order.shipment_type: 0})
            reverse_consume(consume, None, consume.current_qty)

        src_orders, src_list = create_src(client1, direct_product, [10], [0])
        dst_orders, dst_list = create_dst(client2, direct_product, [D('1')])

        TransferMultiple(session, src_list, dst_list).do()
        assert dst_orders[0].consume_qty == 10

    def test_no_dst(self, session, direct_product):
        # Очень странно что падает тут, но как есть так есть
        client = ob.ClientBuilder.construct(session)

        src_orders, src_list = create_src(client, direct_product, [10], [0])

        with pytest.raises(exc.CLIENTS_NOT_MATCH):
            TransferMultiple(session, src_list, []).do()


@pytest.mark.parametrize('src, dst', [[0, 1], [1, 0], [1, 1]])
def test_cross_non_resident(session, direct_product, src, dst):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    src_client = ob.ClientBuilder.construct(session, agency=agency, fullname='123123', currency_payment='RUB')
    dst_client = ob.ClientBuilder.construct(session, agency=agency, fullname='123123', currency_payment='RUB')

    src_orders, src_list = create_src(src_client, direct_product, [D('6')], [D('3')])
    dst_orders, dst_list = create_dst(dst_client, direct_product, [D('1')])

    src_client.is_non_resident = src
    dst_client.is_non_resident = dst
    session.flush()

    with pytest.raises(exc.NON_RESIDENT_TRANSFER):
        TransferMultiple(session, src_list, dst_list).do()


def test_non_resident_ok(session, direct_product):
    agency = ob.ClientBuilder.construct(session, is_agency=1)
    client = ob.ClientBuilder.construct(session, agency=agency, fullname='123123', currency_payment='RUB')

    src_orders, src_list = create_src(client, direct_product, [D('6')], [D('3')])
    dst_orders, dst_list = create_dst(client, direct_product, [D('1')])

    client.is_non_resident = 1
    session.flush()

    TransferMultiple(session, src_list, dst_list).do()

    assert dst_orders[0].consume_qty == 3


@pytest.mark.parametrize('src, dst', [[0, 1], [1, 0], [1, 1]])
def test_trp(session, client, direct_product, src, dst):
    (src_order,), src_list = create_src(client, direct_product, [D('6')], [D('3')])
    (dst_order,), dst_list = create_dst(client, direct_product, [D('1')])

    src_order.need_actual_completions = src
    dst_order.need_actual_completions = dst
    session.flush()

    with pytest.raises(exc.INVALID_PARAM) as exc_info:
        TransferMultiple(session, src_list, dst_list).do()
    assert 'Source or destination orders with TRP' in exc_info.value.msg


def test_old_qty(session, client, direct_product):
    src_orders, (src_item,) = create_src(client, direct_product, [D('6')], [D('3')])
    dst_orders, dst_list = create_dst(client, direct_product, [D('1')])

    src_item.qty_old = 666

    with pytest.raises(exc.ORDERS_NOT_SYNCHRONIZED):
        TransferMultiple(session, [src_item], dst_list).do()


@pytest.mark.promo_code
def test_promo_services(session, client, product):
    promo_code, = ob.PromoCodeGroupBuilder.construct(
        session,
        service_ids=[ServiceId.TAXI_CASH],
        firm_id=FirmId.TAXI,
    ).promocodes
    reserve_promo_code(client, promo_code)

    src_order = ob.OrderBuilder.construct(session, client=client, product=product, service_id=ServiceId.TAXI_CASH)
    dst_order = ob.OrderBuilder.construct(session, client=client, product=product, service_id=ServiceId.TAXI_CARD)

    invoice = ob.InvoiceBuilder.construct(
        session,
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=src_order, quantity=10)]
            )
        )
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(apply_promocode=True)

    with pytest.raises(exc.TRANSFER_FROBIDDEN_BY_PROMO_CODE) as exc_info:
        TransferMultiple(
            session,
            [SrcItem(src_order.consume_qty, src_order.consume_qty, src_order)],
            [DstItem(1, dst_order)]
        ).do()

    assert exc_info.value.reason == 'wrong service'


@pytest.mark.promo_code
def test_promo_products(session, client):
    promo_code, = ob.PromoCodeGroupBuilder.construct(
        session,
        calc_class_name='FixedQtyBonusPromoCodeGroup',
        calc_params={
            'product_bonuses': {DIRECT_PRODUCT_RUB_ID: 10},
        },
    ).promocodes
    reserve_promo_code(client, promo_code)

    src_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_RUB_ID)
    dst_order = ob.OrderBuilder.construct(session, client=client, product_id=DIRECT_PRODUCT_ID)

    invoice = ob.InvoiceBuilder.construct(
        session,
        request=ob.RequestBuilder(
            basket=ob.BasketBuilder(
                client=client,
                rows=[ob.BasketItemBuilder(order=src_order, quantity=10)]
            )
        )
    )
    invoice.create_receipt(invoice.effective_sum)
    invoice.turn_on_rows(apply_promocode=True)

    with pytest.raises(exc.TRANSFER_FROBIDDEN_BY_PROMO_CODE) as exc_info:
        TransferMultiple(
            session,
            [SrcItem(src_order.consume_qty, src_order.consume_qty, src_order)],
            [DstItem(1, dst_order)]
        ).do()

    assert exc_info.value.reason == 'wrong product'


class TestBannedAgency(object):
    @staticmethod
    def _mk_contract(agency):
        contract_dt = datetime.datetime.now() - datetime.timedelta(days=66)
        return ob.ContractBuilder(
            dt=contract_dt,
            client=agency,
            person=ob.PersonBuilder(client=agency, type='ur'),
            commission=ContractTypeId.WITHOUT_PARTICIPATION,
            services={ServiceId.DIRECT},
            is_signed=contract_dt,
            firm=FirmId.YANDEX_OOO,
        ).build(agency.session).obj

    def test_base(self, session, direct_product):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        self._mk_contract(agency)

        src_orders, src_list = create_src(client1, direct_product, [10], [0])
        dst_orders, dst_list = create_dst(client2, direct_product, [D('1')])

        with pytest.raises(exc.BANNED_AGENCY_TRANSFER):
            TransferMultiple(session, src_list, dst_list).do()

    @pytest.mark.parametrize(
        'delta, is_fail',
        [
            pytest.param(1, True, id='future'),
            pytest.param(-1, False, id='past')
        ]
    )
    def test_finish_dt(self, session, direct_product, delta, is_fail):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        contract = self._mk_contract(agency)
        contract.col0.finish_dt = datetime.datetime.now() + datetime.timedelta(delta)

        src_orders, src_list = create_src(client1, direct_product, [10], [0])
        dst_orders, dst_list = create_dst(client2, direct_product, [D('1')])

        if is_fail:
            with pytest.raises(exc.BANNED_AGENCY_TRANSFER):
                TransferMultiple(session, src_list, dst_list).do()
        else:
            TransferMultiple(session, src_list, dst_list).do()
            assert dst_orders[0].consume_qty == 10

    def test_inactive(self, session, direct_product):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        client1 = ob.ClientBuilder.construct(session, agency=agency)
        client2 = ob.ClientBuilder.construct(session, agency=agency)

        contract = self._mk_contract(agency)
        contract.col0.is_signed = None

        src_orders, src_list = create_src(client1, direct_product, [10], [0])
        dst_orders, dst_list = create_dst(client2, direct_product, [D('1')])

        TransferMultiple(session, src_list, dst_list).do()
        assert dst_orders[0].consume_qty == 10

    def test_direct_client(self, session, direct_product):
        client = ob.ClientBuilder.construct(session)
        self._mk_contract(client)

        src_orders, src_list = create_src(client, direct_product, [10], [0])
        dst_orders, dst_list = create_dst(client, direct_product, [D('1')])

        TransferMultiple(session, src_list, dst_list).do()
        assert dst_orders[0].consume_qty == 10

    def test_single_subclient(self, session, direct_product):
        agency = ob.ClientBuilder.construct(session, is_agency=1)
        client = ob.ClientBuilder.construct(session, agency=agency)
        self._mk_contract(agency)

        src_orders, src_list = create_src(client, direct_product, [10], [0])
        dst_orders, dst_list = create_dst(client, direct_product, [D('1')])

        TransferMultiple(session, src_list, dst_list).do()
        assert dst_orders[0].consume_qty == 10
