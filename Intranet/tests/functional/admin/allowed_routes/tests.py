# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    equal_to,
    contains_inanyorder,
)
from unittest.mock import (
    patch,
)

from testutils import (
    TestCase,
    tvm2_auth_success,
)
from intranet.yandex_directory.src.yandex_directory.admin.views.allowed_routes import ROLES_TO_METHODS
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope

TVM2_HEADERS = {'X-Ya-Service-Ticket': 'qqq'}


class TestAdminAllowedRoutesView(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminAllowedRoutesView, self).setUp(*args, **kwargs)

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_allowed_roles(self):
        with patch('intranet.yandex_directory.src.yandex_directory.admin.views.allowed_routes.get_internal_roles_by_uid') as get_roles:
            get_roles.return_value = ['support']
            response = self.get_json(
                '/admin/allowed-routes/',
                headers=TVM2_HEADERS,
            )
        assert_that(
            response,
            contains_inanyorder(*ROLES_TO_METHODS['support'])
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_allowed_roles_empty_roles(self):
        with patch('intranet.yandex_directory.src.yandex_directory.admin.views.allowed_routes.get_internal_roles_by_uid') as get_roles:
            get_roles.return_value = []
            response = self.get_json(
                '/admin/allowed-routes/',
                headers=TVM2_HEADERS,
            )
        assert_that(
            response,
            equal_to([])
        )
