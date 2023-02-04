# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals
from future import standard_library
standard_library.install_aliases()

from datetime import datetime
import http.client as http
import uuid

from hamcrest import (
    assert_that,
    equal_to,
    contains,
    contains_inanyorder,
    has_entries,
)
import pytest

from brest.core.tests import security
from tests import object_builder as ob
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.common import not_existing_id
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role



class TestProductList(TestCaseApiAppBase):
    BASE_API = u'/v1/product/short-list'

    def test_get_existing_product_by_product_id_response_code_ok_one_item(self, admin_role):
        security.set_roles([admin_role])

        DATETIME_FORMAT = '%Y-%m-%dT%H:%M:%S'
        name = 'Product Name'
        fullname = 'Full Name'
        englishname = 'English Name'
        service_name = 'Service Name'
        media_discount_id = 17
        commission_type_id = 18


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
            'englishname': englishname
        }
        response = self.test_client.get(self.BASE_API, {'product_id': product.id})
        assert_that(response.status_code, equal_to(http.OK))
        assert_that(
            response.json['data'],
            has_entries({'total_count': expected_items_len, 'items': contains(has_entries(expected_item))})
        )

    @pytest.mark.parametrize('request_params', [
        {'product_id': not_existing_id(ob.ProductBuilder)},
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
            response.json['data']['total_count'],
            equal_to(expected_items_len),
            'response list length must be {length}'.format(length=expected_items_len),
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

    @pytest.mark.parametrize(
        'as_list',
        [False, True],
    )
    def test_product_ids(self, as_list):
        products = [ob.ProductBuilder.construct(self.test_session) for _ in range(3)]
        product_ids = [str(p.id) for p in products]
        response = self.test_client.get(
            self.BASE_API,
            [
                ('product_ids', p)
                for p in (product_ids if as_list else [','.join(product_ids)])
            ],
        )
        assert_that(response.status_code, equal_to(http.OK))

        assert_that(
            response.get_json()['data']['items'],
            contains_inanyorder(*[
                has_entries(id=p.id)
                for p in products
            ]),
        )
