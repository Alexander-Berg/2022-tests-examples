# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    equal_to,
    contains_inanyorder,
    has_entries,
    contains,
    not_none,
    has_key,
    has_item,
    has_items,
)
from unittest.mock import patch

from testutils import (
    TestCase,
    set_auth_uid,
    get_auth_headers,
)
from .... import webmaster_responses
from intranet.yandex_directory.src.yandex_directory import webmaster
from intranet.yandex_directory.src.yandex_directory.access_restore.views.webmaster import AccessRestoreTask
from intranet.yandex_directory.src.yandex_directory.core.models import OrganizationAccessRestoreModel


class TestRestoreOwnershipView(TestCase):
    def setUp(self):
        super(TestRestoreOwnershipView, self).setUp()
        self.old_admin_uid = 1231231223
        self.new_admin_uid = 12345678
        self.domain = 'whereismyadmin.com'
        create_params = {
            'domain': self.domain,
            'new_admin_uid': self.new_admin_uid,
            'old_admin_uid': 111,
            'org_id': 10500,
            'ip': '127.0.0.1',
            'control_answers': {'your cat name is': 'kitty'}
        }
        self.first_restore = OrganizationAccessRestoreModel(self.meta_connection).create(**create_params)
        set_auth_uid(self.new_admin_uid)

    def test_other_uid(self):
        headers = get_auth_headers(as_org=None, as_uid=123)
        self.get_json(
            '/restore/{}/'.format(self.first_restore['id']),
            headers=headers,
            expected_code=404

        )

    def test_unknown_restore_id(self):
        self.get_json('/restore/{}/ownership/'.format('23rhff74bergeubg854'), expected_code=404)

    def test_get_not_verified(self):
        # не удалось верефицировать домен  в webmaster
        # получаем данные об этом в ручке

        self.mocked_webmaster_inner_list_applicable.side_effect = webmaster_responses.applicable()
        self.mocked_webmaster_inner_info.side_effect = webmaster_responses.verification_failed()

        response = self.get_json('/restore/{}/ownership/'.format(self.first_restore['id']))
        assert_that(
            response,
            has_entries(
                domain=self.domain,
                owned=False,
                last_check= has_entries(
                    date=not_none(),
                    method=not_none(),
                    fail_reason=not_none(),
                    fail_type=not_none(),
                ),
            )
        )

    def test_get_verication_in_progress(self):
        # идет процесс подтверждения в webmaster
        # получаем данные о текущем способе подтверждения

        self.mocked_webmaster_inner_list_applicable.side_effect = webmaster_responses.applicable()
        self.mocked_webmaster_inner_info.side_effect = webmaster_responses.verification_in_progress()

        response = self.get_json('/restore/{}/ownership/'.format(self.first_restore['id']))
        assert_that(
            response,
            has_entries(
                domain=self.domain,
                owned=False,
                last_check=has_entries(
                    date=not_none(),
                    method=not_none(),
                ),
            )
        )


class TestRestoreVerifyView(TestCase):
    def setUp(self):
        super(TestRestoreVerifyView, self).setUp()
        self.old_admin_uid = 1231231223
        self.new_admin_uid = 12345678
        self.domain = 'whereismyadmin.com'
        create_params = {
            'domain': self.domain,
            'new_admin_uid': self.new_admin_uid,
            'old_admin_uid': 111,
            'org_id': 10500,
            'ip': '127.0.0.1',
            'control_answers': {'your cat name is': 'kitty'}
        }
        self.first_restore = OrganizationAccessRestoreModel(self.meta_connection).create(**create_params)
        set_auth_uid(self.new_admin_uid)

    def test_verified(self):
        # домен уже подтвержден в webmaster

        self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=True)

        with patch.object(AccessRestoreTask, 'delay') as delay_task:
            response = self.post_json(
                '/restore/{}/ownership/verify/'.format(self.first_restore['id']),
                {'verification_type': 'dns'},
                expected_code=200,
            )
            # ставим задачу на передачу владения
            delay_task.assert_called_once_with(
                restore_id=self.first_restore['id'],
                org_id=self.first_restore['org_id'],
            )
        assert_that(
            response,
            has_entries(
                domain=self.domain,
                owned=True,
            )
        )

    def test_not_verified(self):
        # домен ещё  не подтвержден в webmaster

        self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)

        response = self.post_json(
            '/restore/{}/ownership/verify/'.format(self.first_restore['id']),
            {'verification_type': 'dns'},
            expected_code=200,
        )

        assert_that(
            response,
            has_entries(
                domain=self.domain,
                owned=False,
            )
        )

    def test_in_progress(self):
        # домен в процессе подтверждения в webmaster

        self.mocked_webmaster_inner_verify.side_effect = webmaster.WebmasterError(
            webmaster_code='verify_host__verification_is_in_progress',
            webmaster_message='',
        )

        response = self.post_json(
            '/restore/{}/ownership/verify/'.format(self.first_restore['id']),
            {'verification_type': 'dns'},
            expected_code=409,
        )

        assert_that(
            response,
            has_entries(
                code='already_in_progress',
            )
        )
