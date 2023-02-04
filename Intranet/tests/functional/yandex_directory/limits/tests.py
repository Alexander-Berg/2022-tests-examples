# encoding: UTF-8

from contextlib import closing

from hamcrest import *
from unittest.mock import patch
from sqlalchemy.orm import Session

from testutils import get_auth_headers
from testutils import patched_admin_permissions
from testutils import patched_scopes
from testutils import TestCase
from testutils import tvm2_auth_success
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.common.components import component_registry
from intranet.yandex_directory.src.yandex_directory.core.models import SupportActionMetaModel
from intranet.yandex_directory.src.yandex_directory.core.models.action import SupportActions
from intranet.yandex_directory.src.yandex_directory.limits.models import OrganizationLimit


class TestAdminOrganizationLimitsViewCase(TestCase):
    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_get_limit(self):
        with patch.object(
                component_registry(),
                'meta_session',
                Session(self.meta_connection),
        ) as session, closing(session):
            response = self.get_json(
                '/admin/organizations/%d/limits/' % self.organization['id'],
            )

            assert_that(
                response,
                has_entries(
                    limits=has_entries(
                        user_aliases_max_count=10,
                    )
                )
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_set_limit(self):
        with patch.object(
                component_registry(),
                'meta_session',
                Session(self.meta_connection),
        ) as session, closing(session):
            response = self.patch_json(
                '/admin/organizations/%d/limits/' % self.organization['id'],
                data={
                    'limits': {'user_aliases_max_count': 100500},
                    'comment': 'set user_aliases_max_count to 100500',
                },
            )

            assert_that(
                response,
                has_entries(
                    limits=has_entries(
                        user_aliases_max_count=100500,
                    )
                )
            )

            response = self.get_json(
                '/admin/organizations/%d/limits/' % self.organization['id'],
            )

            assert_that(
                response,
                has_entries(
                    limits=has_entries(
                        user_aliases_max_count=100500,
                    )
                )
            )

            actions = SupportActionMetaModel(self.meta_connection).all()
            assert_that(
                actions,
                has_item(
                    has_entries(
                        org_id=self.organization['id'],
                        name=SupportActions.change_organization_limits,
                        author_id=100700,
                        object=equal_to(response['limits']),
                        object_type='organization_limits',
                        comment='set user_aliases_max_count to 100500',
                    )
                )
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_reset_limit(self):
        with patch.object(
                component_registry(),
                'meta_session',
                Session(self.meta_connection),
        ) as session, closing(session):
            component_registry().organization_limiter.set_limit(
                self.organization['id'],
                OrganizationLimit.USER_ALIASES_MAX_COUNT,
                100500,
            )

            response = self.get_json(
                '/admin/organizations/%d/limits/' % self.organization['id'],
            )

            assert_that(
                response,
                has_entries(
                    limits=has_entries(
                        user_aliases_max_count=100500,
                    )
                )
            )

            response = self.patch_json(
                '/admin/organizations/%d/limits/' % self.organization['id'],
                data={
                    'limits': {'user_aliases_max_count': None},
                    'comment': 'reset user_aliases_max_count to default',
                },
            )

            assert_that(
                response,
                has_entries(
                    limits=has_entries(
                        user_aliases_max_count=10,
                    )
                )
            )

            response = self.get_json(
                '/admin/organizations/%d/limits/' % self.organization['id'],
            )

            assert_that(
                response,
                has_entries(
                    limits=has_entries(
                        user_aliases_max_count=10,
                    )
                )
            )

            actions = SupportActionMetaModel(self.meta_connection).all()
            assert_that(
                actions,
                has_item(
                    has_entries(
                        org_id=self.organization['id'],
                        name=SupportActions.change_organization_limits,
                        author_id=100700,
                        object=equal_to(response['limits']),
                        object_type='organization_limits',
                        comment='reset user_aliases_max_count to default',
                    )
                )
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_return_422_on_invalid_limit(self):
        with patch.object(
                component_registry(),
                'meta_session',
                Session(self.meta_connection),
        ) as session, closing(session):
            response = self.patch_json(
                '/admin/organizations/%d/limits/' % self.organization['id'],
                data={
                    'limits': {'user_aliases_max_count_abyrvalg': 100500},
                    'comment': 'set user_aliases_max_count to 100500',
                },
                expected_code=422,
            )

            assert_that(
                response,
                has_entries(
                    code='schema_validation_error',
                    message='schema_validation_error',
                    description=has_entry(
                        'fields',
                        has_entry(
                            'limits.user_aliases_max_count_abyrvalg._key',
                            contains_string('Must be one of:'),
                        ),
                    ),
                    params=empty(),
                )
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_return_404_for_unknown_org(self):
        with patch.object(
                component_registry(),
                'meta_session',
                Session(self.meta_connection),
        ) as session, closing(session):
            response = self.get_json(
                '/admin/organizations/%d/limits/' % 100500,
                expected_code=404,
            )

            assert_that(
                response,
                has_entries(
                    code='not_found',
                    message='not_found',
                    description=empty(),
                    params=has_entries(
                        entity_id=100500,
                    ),
                )
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_return_400_for_bad_data(self):
        with patched_scopes(), patched_admin_permissions():
            response = self.client.patch(
                '/admin/organizations/%d/limits/' % 100500,
                data='abcd',
                content_type='application/json',
                headers=get_auth_headers()
            )

            assert_that(
                response,
                has_properties(
                    status_code=400,
                    json=has_entries(
                        code='bad_request',
                        message='400 Bad Request: The browser (or proxy) sent '
                                'a request that this server could not '
                                'understand.',
                        params=empty(),
                    )
                )
            )


class TestOrganizationLimitsViewCase(TestCase):

    @tvm2_auth_success(100700, scopes=[scope.manage_limits])
    def test_set_limit(self):
        with patch.object(
                component_registry(),
                'meta_session',
                Session(self.meta_connection),
        ) as session, closing(session):
            response = self.patch_json(
                '/organizations/%d/limits/' % self.organization['id'],
                data={
                    'limits': {'user_aliases_max_count': 100500},
                },
            )

            assert_that(
                response,
                has_entries(
                    limits=has_entries(
                        user_aliases_max_count=100500,
                    )
                )
            )

            response = self.get_json(
                '/organizations/%d/limits/' % self.organization['id'],
            )

            assert_that(
                response,
                has_entries(
                    limits=has_entries(
                        user_aliases_max_count=100500,
                    )
                )
            )
