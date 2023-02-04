# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from datetime import datetime, timedelta
from decimal import Decimal as D
from future import standard_library

from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
)
import http.client as http
import pytest

from balance import mapper
from balance.constants import PermissionCode, LanguageId
from brest.core.tests import security
from tests import object_builder as ob

from yb_snout_api.tests_unit.base import TestCaseApiAppBase

standard_library.install_aliases()
NOT_EXISTING_PRODUCT_ID = int('9' * 38)


class TestProduct(TestCaseApiAppBase):
    BASE_API = '/v1/product'

    def test_get_not_existing_product_response_code_not_found(self):
        product_id = NOT_EXISTING_PRODUCT_ID
        response = self.test_client.get(self.BASE_API, {'product_id': product_id})
        expected_body = {
            "description": 'Object not found: Product: primary keys: ({product_id},)'.format(
                product_id=product_id,
            ),
            "error": "NOT_FOUND",
        }
        assert_that(
            response.status_code,
            equal_to(http.NOT_FOUND),
        )

        assert_that(
            response.json,
            has_entries(expected_body),
            'response body must be {expected_body}'.format(expected_body=expected_body),
        )

    def test_product_with_params(self):
        now = self.test_session.now()
        DATETIME_FORMAT = '%Y-%m-%dT%H:%M:%S'
        name = 'Product Name'
        service_name = 'Service Name'
        comments = 'Product comments'
        season_coefficient_coeff = 20
        season_coefficient_dt = datetime(year=2010, month=1, day=1)
        season_coefficient_finish_dt = datetime(2050, month=1, day=1)
        markup_description = 'Markup description'
        firm_title = 'Firm Title'
        media_discount_id = 17
        media_discount_name = 'Спецпроекты'
        commission_type_id = 18
        commission_type_name = 'Медийка - Украина без ДКВ устарело не использовать'
        adv_kind_id = 10
        adv_kind_name = 'Маркет'
        fullname = 'Full Name'
        englishname = 'English Name'
        product_firm_id = 1
        product_firm_title = 'ООО «Яндекс»'

        nds_code = 1010211
        nds_description = 'Медицинские услуги'

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
            adv_kind=ob.Getter(mapper.AdvKind, adv_kind_id),
            comments=comments,
            service_code='PENALTY',
            create_taxes=False,
        )
        lang_name = 'ru'
        product_name_fullname = 'Fullname{}'.format(product.id)
        product_name_name = '{name}_{lang_id}'.format(name=product.name, lang_id=LanguageId.RU)

        ob.ProductNameBuilder.construct(
            session=self.test_session,
            product=product,
            lang_id=LanguageId.RU,
            product_name=product_name_name,
            product_fullname=product_name_fullname,
        )
        firm = ob.FirmBuilder.construct(
            session=self.test_session,
            title=firm_title,
        )
        tax_hidden = ob.TaxBuilder.construct(
            session=self.test_session,
            firm_id=firm.id,
            product=product,
            currency=ob.Getter(mapper.Currency, 'RUR'),
            dt=now - timedelta(days=1),
            nds_pct=D('11'),
            hidden=1,
        )
        nds_operation_code = ob.NDSOperationCodeBuilder.construct(
            session=self.test_session,
            code=nds_code,
            description=nds_description,
        )
        tax = ob.TaxBuilder.construct(
            session=self.test_session,
            firm_id=firm.id,
            product=product,
            currency=ob.Getter(mapper.Currency, 'RUR'),
            dt=now,
            nds_pct=D('22'),
            nds_operation_code_id=nds_operation_code.id,
        )
        markup = ob.MarkupBuilder.construct(
            session=self.test_session,
            description=markup_description,
        )
        product_markup = ob.ProductMarkupBuilder.construct(
            session=self.test_session,
            product_id=product.id,
            markup_id=markup.id,
        )
        season_coefficient = ob.ProdSeasonCoeffBuilder.construct(
            session=self.test_session,
            target_id=product.id,
            coeff=season_coefficient_coeff,
            dt=season_coefficient_dt,
            finish_dt=season_coefficient_finish_dt,
        )

        expected_data = {
            'commission_type': {
                'id': commission_type_id,
                'name': commission_type_name,
            },
            'adv_kind': {
                'id': adv_kind_id,
                'name': adv_kind_name,
            },
            'dt': product.dt.strftime(DATETIME_FORMAT),
            'unit': product.unit.name,
            'hidden': False,
            'activ_dt': product.activ_dt.strftime(DATETIME_FORMAT),
            'service': {
                'id': service.id,
                'name': service_name,
            },
            'product_names': [{
                'lang': lang_name,
                'fullname': product_name_fullname,
                'name': product_name_name,
            }],
            'comments': comments,
            'id': product.id,
            'markups': [{
                'code': markup.code,
                'id': product_markup.id,
                'pct': '%.2f' % product_markup.pct,
                'description': markup_description,
            }],
            'show_in_shop': 1,
            'product_group': {
                'id': product.product_group.id,
                'name': product.product_group.name,
            },
            'firm': {
                'id': product_firm_id,
                'title': product_firm_title,
            },
            'season_coefficient': [{
                'finish_dt': season_coefficient_finish_dt.strftime(DATETIME_FORMAT),
                'dt': season_coefficient_dt.strftime(DATETIME_FORMAT),
                'coeff': '%.2f' % season_coefficient_coeff,
                'id': season_coefficient.id,
            }],
            'prices': [{
                'price': '100.000000',
                'iso_currency': 'RUB',
                'dt': '2001-01-01T00:00:00',
                'tax_policy_pct': {
                    'dt': '2019-01-01T00:00:00',
                    'nsp_pct': '0.00',
                    'id': 281,
                    'nds_pct': '20.00',
                    'tax_policy': {
                        'id': 1,
                        'name': 'Стандартный НДС',
                    },
                },
                'id': product.prices[0].id,
            }],
            'englishname': 'English Name',
            'name': name,
            'service_code': {
                'code': 'PENALTY',
                'descr': '\u0428\u0442\u0440\u0430\u0444',
            },
            'taxes': [{
                'firm': {
                    'id': firm.id,
                    'title': firm_title,
                },
                'dt': now.isoformat(),
                'iso_currency': 'RUB',
                'id': tax.id,
                'tax_policy': {
                    'id': None,
                    'name': None,
                },
                'nds_operation_code': {
                    'id': nds_operation_code.id,
                    'code': nds_operation_code.code,
                    'description': nds_operation_code.description,
                }
            }],
            'manual_discount': 0,
            'media_discount': {
                'id': media_discount_id,
                'name': media_discount_name,
            },
            'fullname': fullname,
            'activity_type': {
                'id': product.activity_type.id,
                'name': product.activity_type.name,
            },
        }

        response = self.test_client.get(self.BASE_API, {'product_id': product.id})
        assert_that(
            response.status_code,
            equal_to(http.OK),
        )
        assert_that(
            response.json['data'],
            has_entries(expected_data),
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
