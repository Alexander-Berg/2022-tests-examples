# -*- coding: utf-8 -*-

import datetime

import pytest
import hamcrest

from balance import mapper
from balance import exc
from balance.constants import (
    SENTINEL,
    DIRECT_PRODUCT_ID,
    DIRECT_PRODUCT_RUB_ID,
    ServiceId,
    FirmId,
)
from balance.corba_buffers import StateBuffer
from tests import object_builder as ob


@pytest.fixture
def client(session):
    return ob.ClientBuilder().build(session).obj


@pytest.fixture
def agency(session):
    return ob.ClientBuilder(is_agency=1).build(session).obj


@pytest.fixture
def manager(session):
    return ob.SingleManagerBuilder().build(session).obj


class TestCreateRequest(object):
    def test_base(self, session, client, muzzle_logic):
        docs_dt = (datetime.datetime.now() + datetime.timedelta(66)).replace(microsecond=0)
        state_obj = StateBuffer(
            params={
                'req_client_id': str(client.id),
                'req_firm_id': str(FirmId.YANDEX_OOO),
                'req_documents_dt': docs_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'req_id_1': str(DIRECT_PRODUCT_ID),
                'req_quantity_1': '1',
                'req_discount_1': '10',
                'req_memo_1': u'абыр',
                'req_order_client_id_1': str(client.id),
                'req_id_2': str(DIRECT_PRODUCT_RUB_ID),
                'req_quantity_2': '30',
                'req_discount_2': '20',
                'req_memo_2': u'валг',
                'req_order_client_id_2': str(client.id),
            },
        )
        res = muzzle_logic.ctl_create_request(session, state_obj)
        request = session.query(mapper.Request).get(res.findtext('request-id'))

        hamcrest.assert_that(
            request,
            hamcrest.has_properties(
                client_id=client.id,
                desired_invoice_dt=docs_dt,
                firm_id=FirmId.YANDEX_OOO,
                rows=hamcrest.contains_inanyorder(
                    hamcrest.has_properties(
                        quantity=1,
                        u_discount_pct=10,
                        discount_pct=0,
                        order=hamcrest.has_properties(
                            service_id=ServiceId.ONE_TIME_SALE,
                            manager_code=None,
                            client_id=client.id,
                            agency_id=None,
                            service_code=DIRECT_PRODUCT_ID,
                            text=u'абыр'
                        )
                    ),
                    hamcrest.has_properties(
                        quantity=30,
                        u_discount_pct=20,
                        discount_pct=0,
                        order=hamcrest.has_properties(
                            service_id=ServiceId.ONE_TIME_SALE,
                            manager_code=None,
                            client_id=client.id,
                            agency_id=None,
                            service_code=DIRECT_PRODUCT_RUB_ID,
                            text=u'валг'
                        )
                    )
                )
            )
        )

    def test_no_firm(self, session, muzzle_logic, client):
        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            state_obj = StateBuffer(
                params={
                    'req_client_id': str(client.id),
                    'req_id_1': str(DIRECT_PRODUCT_ID),
                    'req_quantity_1': '1',
                    'req_discount_1': '10',
                    'req_memo_1': u'абыр',
                    'req_order_client_id_1': str(client.id),
                },
            )
            muzzle_logic.ctl_create_request(session, state_obj)
        assert '"firm_id" parameter must be specified' in str(exc_info.value)

    @pytest.mark.parametrize('client_id', [-1, 0, SENTINEL])
    def test_invalid_client(self, session, muzzle_logic, client, client_id):
        params = {
            'req_firm_id': str(FirmId.YANDEX_OOO),
            'req_id_1': str(DIRECT_PRODUCT_ID),
            'req_quantity_1': '1',
            'req_discount_1': '10',
            'req_memo_1': u'абыр',
            'req_order_client_id_1': str(client.id),
        }
        state_obj = StateBuffer(
            params=params,
        )
        if client_id is not SENTINEL:
            params['req_client_id'] = str(client_id)

        with pytest.raises(exc.NOT_FOUND):
            muzzle_logic.ctl_create_request(session, state_obj)

    @pytest.mark.parametrize(
        'skipped_param',
        [
            'req_id',
            'req_quantity',
            'req_discount',
            'req_memo',
            'req_order_client_id',
        ]
    )
    def test_invalid_rows(self, session, muzzle_logic, client, skipped_param):
        params = {
            'req_client_id': client.id,
            'req_firm_id': FirmId.YANDEX_OOO,
            'req_id_1': DIRECT_PRODUCT_ID,
            'req_quantity_1': 1,
            'req_discount_1': 10,
            'req_memo_1': u'абыр',
            'req_order_client_id_1': client.id,
            'req_id_2': DIRECT_PRODUCT_RUB_ID,
            'req_quantity_2': 30,
            'req_discount_2': 20,
            'req_memo_2': u'валг',
            'req_order_client_id_2': client.id,
        }
        params.pop('%s_2' % skipped_param)
        state_obj = StateBuffer(params=params)

        with pytest.raises(exc.INVALID_PARAM) as exc_info:
            muzzle_logic.ctl_create_request(session, state_obj)
        assert 'Either empty list of orders or incompatible size of order params' in str(exc_info.value)
