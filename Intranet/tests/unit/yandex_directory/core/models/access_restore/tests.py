# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    has_entries,
    contains_inanyorder,
    not_none,
)

from testutils import TestCase
from intranet.yandex_directory.src.yandex_directory.core.models import OrganizationAccessRestoreModel


class TestOrganizationAccessRestoreModel(TestCase):

    def test_create(self):
        model = OrganizationAccessRestoreModel(self.meta_connection)
        model.delete(force_remove_all=True)

        domain = 'whereismyadmin.com'
        create_params = {
            'domain': domain,
            'new_admin_uid': 777,
            'old_admin_uid': 111,
            'org_id': 10500,
            'ip': '127.0.0.1',
            'control_answers': {'your cat name is': 'kitty'}
        }
        model.create(**create_params)
        result = model.find({'domain': domain}, one=True)

        assert_that(
            result,
            has_entries(**create_params)
        )
        assert_that(
            result,
            has_entries(
                state='in_progress',
            )
        )

    def test_filter(self):
        model = OrganizationAccessRestoreModel(self.meta_connection)
        domain = 'whereismyadmin.com'
        create_params = {
            'domain': domain,
            'new_admin_uid': 777,
            'old_admin_uid': 111,
            'org_id': 10500,
            'ip': '127.0.0.1',
            'control_answers': {'your cat name is': 'kitty'}
        }
        restore_record = model.create(**create_params)

        for key in ['domain', 'org_id', 'new_admin_uid', 'old_admin_uid']:
            result = model.find({key: create_params[key]}, one=True)
            assert_that(
                result,
                has_entries(**create_params)
            )

        result = model.get(id=restore_record['id'])
        assert_that(
            result,
            has_entries(**create_params)
        )
