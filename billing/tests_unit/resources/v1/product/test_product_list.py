# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

from datetime import datetime
import http.client as http
import uuid

from hamcrest import (
    assert_that,
    equal_to,
    contains,
    has_entries,
)
import pytest

from balance.constants import PermissionCode
from brest.core.tests import security
from tests import object_builder as ob
from yb_snout_api.tests_unit.base import TestCaseApiAppBase

standard_library.install_aliases()

NOT_EXISTING_ID = int('9' * 38)


class TestProductList(TestCaseApiAppBase):
    BASE_API = u'/v1/product/list'

    def test_get_existing_product_by_product_id_response_code_ok_one_item(self):
        DATETIME_FORMAT = '%Y-%m-%dT%H:%M:%S'
        name = 'Product Name'
        fullname = 'Full Name'
        englishname = 'English Name'
        service_name = 'Service Name'
        media_discount_id = 17
        media_discount_name = 'Спецпроекты'
        commission_type_id = 18
        commission_type_name = 'Медийка - Украина без ДКВ устарело не использовать'

        service = ob.ServiceBuilder.construct(
            session=self.test_session,
            name=service_name,
        )

        product = ob.ProductBuilder.construct(
            session=self.test_session,
            name=name,
            media_discount=media_discount_id,  # Problem with insert into bo.t_discount_type
            commission_type=commission_type_id,  # Problem with insert into bo.t_discount_type
            engine_id=service.id,
            fullname=fullname,
            englishname=englishname,
            create_taxes=False,
        )

        expected_items_len = 1
        expected_item = {
            'id': product.id,
            'dt': product.dt.strftime(DATETIME_FORMAT),
            'name': name,
            'fullname': fullname,
            'englishname': englishname,
            'service': {
                'id': service.id,
                'name': service_name,
            },
            'activity_type': {
                'id': product.activity_type.id,
                'name': product.activity_type.name,
            },
            'product_group': {
                'id': product.product_group.id,
                'name': product.product_group.name,
            },
            'unit': product.unit.name,
            'show_in_shop': 1,
            'manual_discount': 0,
            'commission_type': {
                'id': commission_type_id,
                'name': commission_type_name,
            },
            'media_discount': {
                'id': 17,
                'name': media_discount_name,
            },
        }
        response = self.test_client.get(self.BASE_API, {'product_id': product.id})
        assert_that(
            response.status_code,
            equal_to(http.OK),
        )
        assert_that(
            response.json['data']['items'][0],
            equal_to(expected_item),
        )
        assert_that(
            len(response.json['data']['items']),
            equal_to(expected_items_len),
            'response list length must be {length}'.format(length=expected_items_len),
        )

    @pytest.mark.parametrize('request_params', [
        {'product_id': NOT_EXISTING_ID},
        {'service_id': NOT_EXISTING_ID},
        {'product_name': '_' * 239},
    ])
    def test_get_not_existing_product_response_code_ok_empty_items(self, request_params):
        expected_items_len = 0
        response = self.test_client.get(self.BASE_API, request_params)
        assert_that(
            response.status_code,
            equal_to(http.OK),
        )
        assert_that(
            len(response.json['data']['items']),
            equal_to(expected_items_len),
            'response list length must be {length}'.format(length=expected_items_len),
        )

    @pytest.mark.parametrize('amount', [0, 3])
    def test_service_id_filter(self, amount):
        service = ob.ServiceBuilder.construct(self.test_session)
        # Products with expected service
        products = [
            ob.ProductBuilder.construct(
                session=self.test_session,
                service=service,
            ) for _ in xrange(amount)
        ]
        # Products without expected service
        products.extend([
            ob.ProductBuilder.construct(
                session=self.test_session,
            ) for _ in xrange(3)
        ])

        response = self.test_client.get(self.BASE_API, {
            'service_id': service.id,
            'sort_key': 'ID',
            'sort_order': 'ASC',
        })
        items = response.json['data']['items']
        products.sort(key=lambda x: x.id)
        expected_result = [
            has_entries(id=p.id)
            for p in products
            if p.service == service
        ]
        assert_that(
            items,
            contains(*expected_result),
        )

    @pytest.mark.parametrize('amount', [0, 3])
    def test_product_name_filter(self, amount):
        # Products with expected name
        product_name_startswith = 'Yandex.Test.Product.Expected.Name'
        products = [
            ob.ProductBuilder.construct(
                session=self.test_session,
                name='{product_name_startswith}.{hex}'.format(
                    product_name_startswith=product_name_startswith,
                    hex=uuid.uuid4().hex,
                ),
            ) for _ in xrange(amount)
        ]
        # Products without expected name
        products.extend([
            ob.ProductBuilder.construct(
                session=self.test_session,
                name='{hex}'.format(
                    hex=uuid.uuid4().hex,
                ),
            ) for _ in xrange(3)
        ])

        response = self.test_client.get(self.BASE_API, {
            'product_name': product_name_startswith,
            'sort_key': 'ID',
            'sort_order': 'ASC',
        })
        items = response.json['data']['items']
        products.sort(key=lambda x: x.id)
        expected_result = [
            has_entries(id=p.id)
            for p in products
            if p.name.startswith(product_name_startswith)
        ]
        assert_that(
            items,
            contains(*expected_result),
        )

    @pytest.mark.parametrize('pagination_pn', [1, 2, 1000])
    @pytest.mark.parametrize('pagination_ps', [1, 3, 1000])
    @pytest.mark.parametrize('sort_key', ['ID', 'NAME', 'DT'])
    @pytest.mark.parametrize('sort_order', ['ASC', 'DESC'])
    def test_pagination_with_sorting(self, pagination_ps, pagination_pn, sort_key, sort_order):
        product_name_startswith = 'Yandex.Test.Product.Expected.Name'
        names_order = (2, 5, 3, 1, 4)
        years_order = (2001, 2003, 2005, 2004, 2002)

        products = [
            ob.ProductBuilder.construct(
                session=self.test_session,
                name='{product_name_startswith}.{name}'.format(
                    product_name_startswith=product_name_startswith,
                    name=name,
                ),
                dt=datetime(year=year, month=1, day=1),
            ) for name, year in zip(names_order, years_order)
        ]
        products.sort(
            key=lambda x: getattr(x, sort_key.lower()),
            reverse=True if sort_order == 'DESC' else False,
        )
        start = (pagination_pn - 1) * pagination_ps
        end = start + pagination_ps

        response = self.test_client.get(
            self.BASE_API,
            {
                'product_name': product_name_startswith,
                'sort_order': sort_order.encode('utf-8'),
                'sort_key': sort_key.encode('utf-8'),
                'pagination_pn': pagination_pn,
                'pagination_ps': pagination_ps,
            },
        )

        assert_that(
            response.json['data']['items'],
            contains(*[
                has_entries(id=p.id)
                for p in products[start:end]
            ]),
        )

    @pytest.mark.parametrize('permissions, expected_status_code', [
        ((PermissionCode.VIEW_PRODUCT, PermissionCode.ADMIN_ACCESS), http.OK),
        ((PermissionCode.ADMIN_ACCESS,), http.FORBIDDEN),
        ((PermissionCode.VIEW_PRODUCT,), http.FORBIDDEN),
        ((), http.FORBIDDEN),
    ])
    def test_permission_constraints(self, permissions, expected_status_code):
        """
        Проверяем права на просмотр номенклатуры
        """
        product = ob.ProductBuilder.construct(self.test_session)
        role = ob.create_role(self.test_session, *permissions)

        security.set_roles(role)
        response = self.test_client.get(self.BASE_API, {u'product_id': product.id})
        assert_that(
            response.status_code,
            equal_to(expected_status_code),
        )
