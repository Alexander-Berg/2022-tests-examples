# -*- coding: utf-8 -*-
import pytest
import datetime
import xml.etree.ElementTree as et

from balance.paystep import (
    PayPreviewInfo,
    PayChooseInfo
)
from balance.actions import promocodes as a_pmc

from tests.object_builder import (
    OrderBuilder,
    BasketItemBuilder,
    BasketBuilder,
    RequestBuilder,
    ClientBuilder,
    PersonBuilder,
    PromoCodeGroupBuilder,
    InvoiceBuilder,
)

pytestmark = [
    pytest.mark.paystep,
    pytest.mark.usefixtures('switch_new_paystep_flag'),
]


class BaseXmlizePromocodeTest(object):
    @pytest.fixture
    def client(self, session):
        return ClientBuilder().build(session).obj

    @pytest.fixture
    def promocode(self, session, client):
        promocode, = PromoCodeGroupBuilder(
            start_dt=datetime.datetime.now() - datetime.timedelta(1),
        ).build(session).obj.promocodes
        a_pmc.reserve_promo_code(client, promocode)
        session.flush()
        return promocode

    @pytest.fixture
    def request_obj(self, session, client):
        return RequestBuilder(
            basket=BasketBuilder(
                client=client,
                rows=[BasketItemBuilder(
                    quantity=10,
                    order=OrderBuilder(client=client)
                )]
            )
        ).build(session).obj

    @pytest.fixture
    def xml(self):
        return et.Element('client')


@pytest.mark.promo_code
class TestPaychooseXmlizePromocode(BaseXmlizePromocodeTest):
    @pytest.fixture
    def info(self, request_obj):
        return PayChooseInfo(request=request_obj)

    def test_no_reservation(self, client, info, xml):
        info.xmlize_promocode_reservation(xml)

        assert xml.find('promocode-reservation') is None

    def test_ok(self, client, promocode, info, xml):
        info.xmlize_promocode_reservation(xml)

        xml_pcr = xml.find('promocode-reservation')
        assert xml_pcr.findtext('client-id') == str(client.id)
        assert xml_pcr.findtext('is-used') == '0'
        assert xml_pcr.findtext('promocode/id') == str(promocode.id)
        assert xml_pcr.findtext('promocode/code') == str(promocode.code)

    def test_check_fail(self, session, client, promocode, request_obj, info, xml):
        # Ломаем проверку на фирмы - это означает что модуль проверки промокода был вызван
        promocode.group.firm_id = 2
        request_obj.firm_id = 1
        session.flush()

        info.xmlize_promocode_reservation(xml)

        assert xml.find('promocode-reservation') is None


@pytest.mark.promo_code
class TestPaypreviewXmlizePromocode(BaseXmlizePromocodeTest):
    @pytest.fixture
    def invoice(self, session, request_obj):
        return InvoiceBuilder(
            request=request_obj,
            person=PersonBuilder(client=request_obj.client),
        ).build(session).obj

    @pytest.fixture
    def info(self, invoice):
        return PayPreviewInfo(fake_invoice=invoice)

    def test_no_reservation(self, client, info, xml):
        info.xmlize_promocode_reservation(xml)

        assert xml.find('promocode-reservation') is None

    def test_applicable_bonus(self, session, client, invoice, info, xml):
        io, = invoice.invoice_orders
        promocode, = PromoCodeGroupBuilder(
            calc_class_name='FixedQtyBonusPromoCodeGroup',
            calc_params={'product_bonuses': {io.order.service_code: '1.5'}},
            start_dt=datetime.datetime.now() - datetime.timedelta(1),
        ).build(session).obj.promocodes
        a_pmc.reserve_promo_code(client, promocode)
        session.flush()

        info.xmlize_promocode_reservation(xml)

        xml_pcr = xml.find('promocode-reservation')
        assert xml_pcr.findtext('client-id') == str(client.id)
        assert xml_pcr.findtext('is-used') == '0'
        assert xml_pcr.findtext('promocode/id') == str(promocode.id)
        assert xml_pcr.findtext('promocode/code') == str(promocode.code)
        assert xml_pcr.findtext('promocode/is-applicable') == '1'
        assert xml_pcr.findtext('promocode/category') == 'BONUS'
        assert xml_pcr.findtext('promocode/minimal_money') == '0'
        assert xml_pcr.findtext('promocode/bonus') == '150.00'

    def test_discount_calc_fail(self, session, client, invoice, info, xml):
        promocode, = PromoCodeGroupBuilder(
            calc_class_name='FixedSumBonusPromoCodeGroup',
            calc_params={'currency_bonuses': {'EUR': 666}},
            start_dt=datetime.datetime.now() - datetime.timedelta(1),
        ).build(session).obj.promocodes
        a_pmc.reserve_promo_code(client, promocode)
        session.flush()

        info.xmlize_promocode_reservation(xml)

        assert xml.find('promocode-reservation') is None

    def test_check_fail(self, session, client, invoice, promocode, info, xml):
        promocode.group.firm_id = 2
        invoice.firm_id = 1
        session.flush()

        info.xmlize_promocode_reservation(xml)

        assert xml.find('promocode-reservation') is None
