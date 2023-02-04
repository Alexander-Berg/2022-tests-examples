# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
import hamcrest as hm

from balance.constants import PermissionCode

from brest.core.tests import security
from yb_snout_api.utils.field_description import FieldDescription
from yb_snout_api.utils import (
    deco,
    inputs as inputs_util,
)
from yb_snout_api.utils.enum import IntEnum
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.resource import URL, build_custom_resource_cxt
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_support_role, create_admin_role


pytestmark = [
    pytest.mark.permissions,
]


class TestCaseResource(TestCaseApiAppBase):
    def test_method_args_choices_with_enum(self):
        from flask_restplus import Namespace

        ns = Namespace('fake')

        class TestIntType(IntEnum):
            KEY1 = 1
            KEY2 = 2

        def method_wrapper():
            @deco.add_doc(
                ns,
                enum_arg={
                    'type': inputs_util.enum(TestIntType),
                    'choices': [],
                },
            )
            def method():
                pass

        hm.assert_that(hm.calling(method_wrapper), hm.raises(AssertionError))


@pytest.mark.smoke
class TestRequestFromAdminUI(TestCaseApiAppBase):
    """Тесты на запросы, пришедшие из АИ"""
    BASE_API = URL

    @pytest.mark.parametrize(
        'w_role',
        [True, False],
    )
    def test_admin_ui_access(self, support_role, w_role):
        get_params_description = {
            'test_id': FieldDescription(field_type=int),
        }

        test_id = 1234
        with build_custom_resource_cxt(get_params_description=get_params_description):
            security.set_roles([support_role] if w_role else [])
            response = self.test_client.get(self.BASE_API, data={'test_id': test_id})

        hm.assert_that(response.status_code, hm.equal_to(http.OK if w_role else http.FORBIDDEN))
        if w_role:
            hm.assert_that(response.get_json().get('data'), hm.has_entries({'test_id': test_id}))

    def test_admin_ui_403(self, admin_role):
        """Недостаточно прав для просмотра АИ ручки"""
        ui_permissions = [PermissionCode.BILLING_SUPPORT]

        with build_custom_resource_cxt(ui_permissions=ui_permissions):
            security.set_roles([admin_role])
            response = self.test_client.get(self.BASE_API)

        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')
        error_data = response.get_json()
        hm.assert_that(
            error_data,
            hm.has_entries({
                'error': 'SnoutPermissionDeniedException',
                'description': 'User has no permissions.',
            }),
        )

    @pytest.mark.parametrize(
        'is_admin_request',
        [True, False],
    )
    def test_admin_access(self, is_admin_request):
        """Надо проверять AdminAccess, когда заходишь из АИ"""
        with build_custom_resource_cxt(use_client_mixin=True):
            security.set_roles([])
            response = self.test_client.get(self.BASE_API, is_admin=is_admin_request)

        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN if is_admin_request else http.OK))

    def test_filter_out_undescriptive_params(self):
        """
        Все параметры, которые не описаны для сваггера, не доходят до ручки
        """
        client_id = 1234
        with build_custom_resource_cxt(use_client_mixin=True):
            response = self.test_client.get(self.BASE_API, data={'client_id': client_id})

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(response.get_json().get('data'), hm.empty())

    def test_client_resource(self):
        """
        От админа из АИ пришел запрос в ручку ClientMixin
        """
        with build_custom_resource_cxt(use_client_mixin=True):
            response = self.test_client.get(self.BASE_API)

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')


@pytest.mark.smoke
class TestRequestFromClientUI(TestCaseResource):
    """Тесты для запросов, пришедших из КИ"""
    BASE_API = URL

    def test_resource_forbidden_for_client_ui(self):
        """
        Для ручки не унаследованной от ClientMixin доступ не через АИ закрыт.
        Чтобы не показывть, что ручка вообще существует, показываем 404.
        """
        with build_custom_resource_cxt():
            response = self.test_client.get(self.BASE_API, is_admin=False)

        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND), 'response code must be NOT_FOUND')
        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'code': 404,
                'error': 'NotFound',
                # стандартная ошибка 404, не должна быть заменена на внутренние исключения
                'description': 'The requested URL was not found on the server. '
                               ' If you entered the URL manually please check your spelling and try again.',
            }),
        )

    def test_client_resource(self):
        """
        Запрос из КИ в ручку ClientMixin
        """
        get_params_description = {
            'test_id': FieldDescription(field_type=int),
        }
        test_id = 1234

        with build_custom_resource_cxt(
                use_client_mixin=True,
                get_params_description=get_params_description,
        ):
            security.set_roles([])
            response = self.test_client.get(
                self.BASE_API,
                data={'test_id': test_id},
                is_admin=False,
            )

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')
        hm.assert_that(
            response.get_json().get('data'),
            hm.has_entries({
                'test_id': test_id,
            }),
        )

    def test_admins_only_403(self, client):
        with build_custom_resource_cxt(
                use_client_mixin=True,
                admins_only=True,
        ):
            security.set_roles([])
            security.set_passport_client(client)
            response = self.test_client.get(
                self.BASE_API,
                is_admin=False,
            )

        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')
        error_data = response.get_json()
        hm.assert_that(
            error_data,
            hm.has_entries({
                'error': 'SnoutPermissionDeniedException',
                'description': 'User has no permissions.',
            }),
        )

    def test_admins_only(self, support_role):
        ui_permissions = [PermissionCode.BILLING_SUPPORT]

        with build_custom_resource_cxt(ui_permissions=ui_permissions):
            security.set_roles([support_role])
            response = self.test_client.get(self.BASE_API)

        hm.assert_that(response.status_code, hm.equal_to(http.OK))
