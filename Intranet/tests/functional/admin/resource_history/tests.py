# coding: utf-8
from hamcrest import (
    assert_that,
    has_item,
    has_entries,
    has_length,
    contains,
)
from unittest.mock import patch
from sqlalchemy.orm import Session

from testutils import TestCase, tvm2_auth_success
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.common.components import component_registry
from intranet.yandex_directory.src.yandex_directory.meta.models.resource_history import ResourceHistory


class TestResourceHistoryView(TestCase):
    enable_admin_api = True

    def _get_random_service(self):
        return self.meta_connection.execute('select id, slug from services limit 1').fetchone()

    def setUp(self):
        self.session = Session(self.meta_connection)
        super(TestResourceHistoryView, self).setUp()

        self.service = self._get_random_service()

        resource_histories = [
            ResourceHistory(service_id=self.service['id'], resource_id='test1.com', name='test1.com'),
            ResourceHistory(service_id=self.service['id'], resource_id='test2.com', name='test2.com'),
            ResourceHistory(service_id=self.service['id'], resource_id='test3.com', name='test3.com'),
            ResourceHistory(service_id=self.service['id'], resource_id='test4.com', name='test4.com'),
            ResourceHistory(service_id=self.service['id'], resource_id='test5.com', name='test5.com'),
            ResourceHistory(service_id=self.service['id'], resource_id='test6.com', name='test6.com'),
            ResourceHistory(service_id=self.service['id'], resource_id='test7.com', name='test7.com'),
            ResourceHistory(service_id=self.service['id'], resource_id='test8.com', name='test8.com'),
            ResourceHistory(service_id=self.service['id'], resource_id='test9.com', name='test9.com'),
            ResourceHistory(service_id=self.service['id'], resource_id='test10.com', name='test10.com'),
        ]

        self.session.bulk_save_objects(resource_histories)

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_without_service_slug(self):
        with patch.object(component_registry(), 'meta_session', self.session):
            self.get_json('/admin/resources/history/', query_string={
                'resource_id': 'test1.com',
            }, expected_code=422)

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_with_resource_id(self):
        with patch.object(component_registry(), 'meta_session', self.session):
            response = self.get_json('/admin/resources/history/', query_string={
                'service_slug': self.service['slug'],
                'resource_id': 'test1.com',
            })

            assert_that(
                response,
                has_entries(
                    result=has_item(
                        has_entries(
                            service_id=self.service['id'],
                            resource_id='test1.com',
                            name='test1.com',
                        ),
                    ),
                ),
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_find_not_existent_by_resource_id(self):
        with patch.object(component_registry(), 'meta_session', self.session):
            response = self.get_json('/admin/resources/history/', query_string={
                'service_slug': self.service['slug'],
                'resource_id': 'not_existent_domain.com',
            })

            assert_that(
                response,
                has_entries(
                    result=has_length(0),
                ),
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_with_resource_name(self):
        with patch.object(component_registry(), 'meta_session', self.session):
            response = self.get_json('/admin/resources/history/', query_string={
                'service_slug': self.service['slug'],
                'name': 'test1.com',
            })

            assert_that(
                response,
                has_entries(
                    result=has_item(
                        has_entries(
                            service_id=self.service['id'],
                            resource_id='test1.com',
                            name='test1.com',
                        ),
                    ),
                ),
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_find_not_existent_by_name(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            response = self.get_json('/admin/resources/history/', query_string={
                'service_slug': self.service['slug'],
                'name': 'not_existent_domain.com',
            })

            assert_that(
                response,
                has_entries(
                    result=has_length(0),
                ),
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_with_service_slug(self):
        with patch.object(component_registry(), 'meta_session', self.session):
            response = self.get_json('/admin/resources/history/', query_string={
                'service_slug': self.service['slug'],
            })

            assert_that(
                response,
                has_entries(
                    result=has_item(
                        has_entries(
                            service_id=self.service['id'],
                            resource_id='test1.com',
                            name='test1.com',
                        ),
                    ),
                ),
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_with_service_slug_and_pagination(self):
        with patch.object(component_registry(), 'meta_session', self.session):
            response = self.get_json('/admin/resources/history/', query_string={
                'service_slug': self.service['slug'],
                'page': 2,
                'per_page': 3,
            })

            assert_that(
                response,
                has_entries(
                    result=contains(
                        has_entries(
                            service_id=self.service['id'],
                            resource_id='test7.com',
                            name='test7.com',
                        ),
                        has_entries(
                            service_id=self.service['id'],
                            resource_id='test6.com',
                            name='test6.com',
                        ),
                        has_entries(
                            service_id=self.service['id'],
                            resource_id='test5.com',
                            name='test5.com',
                        ),
                    ),
                ),
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_find_not_existent_by_service_slug(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            response = self.get_json('/admin/resources/history/', query_string={
                'service_slug': 'not_existent_service_slug',
            })

            assert_that(
                response,
                has_entries(
                    result=has_length(0),
                ),
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_with_full_last_page(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            response = self.get_json('/admin/resources/history/', query_string={
                'service_slug': self.service['slug'],
                'page': 5,
                'per_page': 2,
            })

            assert_that(
                response,
                has_entries(
                    result=has_length(2),
                ),
            )

            assert 'next' not in response['links']

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_with_not_full_last_page(self):
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            response = self.get_json('/admin/resources/history/', query_string={
                'service_slug': self.service['slug'],
                'page': 4,
                'per_page': 3,
            })

            assert_that(
                response,
                has_entries(
                    result=has_length(1),
                ),
            )

            assert 'next' not in response['links']
