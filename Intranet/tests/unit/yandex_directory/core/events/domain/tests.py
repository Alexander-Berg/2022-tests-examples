# -*- coding: utf-8 -*-
import responses
from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
    not_none,
)
from unittest.mock import patch
from sqlalchemy.orm import Session

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.common.components import component_registry
from intranet.yandex_directory.src.yandex_directory.core.actions.domain import (
    on_domain_delete,
    on_domain_occupy,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DomainModel,
    EventModel,
    OrganizationModel,
)
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.connect_services.domenator import setup_domenator_client


class BaseTestCase(TestCase):
    pdd_id = 1
    pdd_version = 'new'
    registrar_id = 123

    def get_registrar_data(self, pdd_id=None, pdd_version=None, admin_id=None, registrar_id=None):
        return {
            'password': 'p$wd',
            'id': registrar_id or self.registrar_id,
            'pdd_id': pdd_id or self.pdd_id,
            'name': 'Name',
            'admin_id': admin_id or self.admin_uid,
            'pdd_version': pdd_version or self.pdd_version,
            'oauth_client_id': '321',
            'validate_domain_url': 'http://validate_domain_url.com/',
            'domain_added_callback_url': 'http://domain_added_callback_url.com/',
            'domain_verified_callback_url': 'http://domain_verified_callback_url.com/',
            'domain_deleted_callback_url': 'http://domain_deleted_callback_url.com/',
            'payed_url': 'http://validate_domain_url.com/',
            'added_init': 'http://domain_added_callback_url.com/',
            'added': 'http://domain_verified_callback_url.com/',
            'delete_url': 'http://domain_deleted_callback_url.com/',
        }

    def add_domenator_registrar_response(self, pdd_id=None, pdd_version=None, admin_id=None, registrar_id=None, url_by_pdd=False):
        registrar_id = registrar_id or self.registrar_id
        pdd_version = pdd_version or self.pdd_version
        pdd_id = pdd_id or self.pdd_id

        registrar_id_url_param = registrar_id
        if url_by_pdd:
            registrar_id_url_param = f'{pdd_version}:{pdd_id}'

        registrar_data = self.get_registrar_data(pdd_id, pdd_version, admin_id, registrar_id)
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/registrar/{registrar_id_url_param}/',
            json=registrar_data,
        )

    def setUp(self):
        super(BaseTestCase, self).setUp()
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'
        self.registrar = self.get_registrar_data()
        setup_domenator_client(app)
        app.domenator.private_get_domains = lambda *args, **kwargs: []


class TestDomaintEvents(BaseTestCase):
    def setUp(self):
        super(TestDomaintEvents, self).setUp()
        OrganizationModel(self.main_connection).filter(
            id=self.organization['id']
        ).update(
            registrar_id=self.registrar_id
        )

    @responses.activate
    def test_on_domain_occupy(self):
        EventModel(self.main_connection).delete(force_remove_all=True)
        revision = 1

        master_domain = DomainModel(self.main_connection).get_master(self.organization['id'])
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            self.add_domenator_registrar_response()

            on_domain_occupy(
                self.main_connection,
                self.organization['id'],
                revision,
                master_domain,
                'domain',
                {},
                123,
            )
            self.process_tasks()

        assert_that(
            self.mocked_zora_client.get.call_args_list[0][0],
            equal_to(
                (self.registrar['domain_verified_callback_url'],)
            )
        )
        assert_that(
            self.mocked_zora_client.get.call_args_list[0][1],
            has_entries(
                params=has_entries(
                    domain=master_domain['name'],
                    sign=not_none(),
                )
            )
        )

    @responses.activate
    def test_on_delete_registrar_domain(self):
        EventModel(self.main_connection).delete(force_remove_all=True)
        revision = 1

        master_domain = DomainModel(self.main_connection).get_master(self.organization['id'])
        with patch.object(component_registry(), 'meta_session', Session(self.meta_connection)):
            self.add_domenator_registrar_response()

            on_domain_delete(
                self.main_connection,
                self.organization['id'],
                revision,
                master_domain,
                'domain',
                {},
                123,
            )
            self.process_tasks()

        assert_that(
            self.mocked_zora_client.get.call_args_list[0][0],
            equal_to(
                (self.registrar['domain_deleted_callback_url'],)
            )
        )
        assert_that(
            self.mocked_zora_client.get.call_args_list[0][1],
            has_entries(
                params=has_entries(
                    domain=master_domain['name'],
                    sign=not_none(),
                )
            )
        )
