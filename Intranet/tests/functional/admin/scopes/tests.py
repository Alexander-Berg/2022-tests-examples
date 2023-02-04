# -*- coding: utf-8 -*-
import re

from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
)

from testutils import (
    TestCase,
    create_organization,
    scopes,
    tvm2_auth,
)
from intranet.yandex_directory.src.yandex_directory.auth.service import Service
from intranet.yandex_directory.src.yandex_directory.core.models import (
    ServiceModel,
    OrganizationServiceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import INTERNAL_ADMIN_SERVICE_SLUG
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope


class TestInternalAdminScope(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestInternalAdminScope, self).setUp(*args, **kwargs)
        tvm2_client_id = 42

        self.organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        self.service = ServiceModel(self.meta_connection).create(
            slug=INTERNAL_ADMIN_SERVICE_SLUG,
            name='Internal Admin Section',
            tvm2_client_ids=[tvm2_client_id],
            scopes=[scope.internal_admin],
            internal=True,
        )

        OrganizationServiceModel(self.main_connection).create(
            self.organization['id'],
            self.service['id'],
            True,
        )

        self.tvm2_service = Service(
            id=self.service['id'],
            name=self.service['name'],
            identity=self.service['slug'],
            is_internal=True,
            ip='127.0.0.1',
        )

    def test_that_admin_handlers_unavailable_without_internal_admin_scope(self):
        methods_map = {
            'get': self.get_json,
            'post': self.post_json,
            'patch': self.patch_json,
            'delete': self.delete_json,
        }

        headers = {
            'X-Ya-Service-Ticket': 'qqq',
            'X-ORG-ID': self.organization['id'],
        }
        with tvm2_auth(
                100700,
                [],
                self.organization['id'],
                self.tvm2_service,
        ):
            for route, view in app.ADMIN_ROUTES:
                # правило замены конструкций <int:something> на число
                route = re.sub('<int:[a-z_]*>', '88005553535', route)
                with scopes(scope.work_with_any_organization, scope.work_on_behalf_of_any_user):
                    method = list(view.methods)[0].lower()
                    args = (route, headers,) if method == 'get' else (route, {}, headers)
                    response = methods_map[method](*args, expected_code=403)
                assert_that(
                    response,
                    has_entries(
                        message=equal_to('This operation requires one of scopes: {scopes}.'),
                        code=equal_to('no-required-scopes'),
                    )
                )
