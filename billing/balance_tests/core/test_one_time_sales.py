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
    ConstraintTypes,
    PermissionCode,
)
from balance.core import OneTimeSales

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


def test_base(session, client):
    docs_dt = (datetime.datetime.now() + datetime.timedelta(66)).replace(microsecond=0)

    raw_order_data = [
        {
            'product_id': DIRECT_PRODUCT_ID,
            'quantity': 1,
            'discount': 10,
            'memo': 'абыр',
            'order_client_id': client.id,
        },
        {
            'product_id': DIRECT_PRODUCT_RUB_ID,
            'quantity': 30,
            'discount': 20,
            'memo': 'валг',
            'order_client_id': client.id,
        },
    ]

    request = OneTimeSales(session).get_request(
        client.id, FirmId.YANDEX_OOO, docs_dt, raw_order_data
    )

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
                        text='абыр'
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
                        text='валг'
                    )
                )
            )
        )
    )


@pytest.mark.permissions
@pytest.mark.parametrize(
    'perm, perm_firm_id',
    [
        (PermissionCode.CREATE_REQUESTS_SHOP, None),
        (PermissionCode.CREATE_REQUESTS_SHOP, FirmId.YANDEX_OOO),
    ]
)
def test_perms_ok(session, client, perm, perm_firm_id):
    role = ob.create_role(session, (perm, {ConstraintTypes.firm_id: None}))
    ob.create_passport(session, (role, perm_firm_id), patch_session=True)

    raw_order_data = [
        {
            'product_id': DIRECT_PRODUCT_ID,
            'quantity': 1,
            'discount': 10,
            'memo': 'абыр',
            'order_client_id': client.id,
        }
    ]

    request_id = OneTimeSales(session).get_request(
        client.id, FirmId.YANDEX_OOO, None, raw_order_data
    )

    assert request_id


@pytest.mark.permissions
@pytest.mark.parametrize(
    'perm, perm_firm_id',
    [
        (PermissionCode.ADMIN_ACCESS, None),
        (PermissionCode.CREATE_REQUESTS_SHOP, FirmId.TAXI),
    ]
)
def test_perms_fail(session, client, perm, perm_firm_id):
    role = ob.create_role(session, (perm, {ConstraintTypes.firm_id: None}))
    ob.create_passport(session, (role, perm_firm_id), patch_session=True)

    with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
        raw_order_data = [
            {
                'product_id': DIRECT_PRODUCT_ID,
                'quantity': 1,
                'discount': 10,
                'memo': 'абыр',
                'order_client_id': client.id,
            }
        ]

        OneTimeSales(session).get_request(
            client.id, FirmId.YANDEX_OOO, None, raw_order_data
        )

    assert 'has no permission CreateRequestsShop' in str(exc_info.value)


@pytest.mark.permissions
def test_perms_owner(session, client):
    role = ob.create_role(session)
    passport = ob.create_passport(session, role, patch_session=True)
    passport.client = client
    session.flush()

    with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
        raw_order_data = [
            {
                'product_id': DIRECT_PRODUCT_ID,
                'quantity': 1,
                'discount': 10,
                'memo': 'абыр',
                'order_client_id': client.id,
            }
        ]

        OneTimeSales(session).get_request(
            client.id, FirmId.YANDEX_OOO, None, raw_order_data
        )

    assert 'has no permission CreateRequestsShop' in str(exc_info.value)


def test_agency(session, agency, client):
    client.agency = agency
    session.flush()

    raw_order_data = [
        {
            'product_id': DIRECT_PRODUCT_ID,
            'quantity': 1,
            'discount': 10,
            'memo': 'абыр',
            'order_client_id': client.id,
        }
    ]

    request = OneTimeSales(session).get_request(
            agency.id, FirmId.YANDEX_OOO, None, raw_order_data
        )

    hamcrest.assert_that(
        request,
        hamcrest.has_properties(
            client_id=agency.id,
            rows=hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    order=hamcrest.has_properties(
                        client_id=client.id,
                        agency_id=agency.id,
                    )
                )
            )
        )
    )


def test_manager(session, client, manager):
    raw_order_data = [
        {
            'product_id': DIRECT_PRODUCT_ID,
            'quantity': 1,
            'discount': 10,
            'memo': 'абыр',
            'order_client_id': client.id,
        }
    ]

    request = OneTimeSales(session).get_request(
        client.id, FirmId.YANDEX_OOO, None, raw_order_data, manager.manager_code
    )

    hamcrest.assert_that(
        request,
        hamcrest.has_properties(
            client_id=client.id,
            rows=hamcrest.contains_inanyorder(
                hamcrest.has_properties(
                    order=hamcrest.has_properties(
                        manager=manager
                    )
                )
            )
        )
    )
