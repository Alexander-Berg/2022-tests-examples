# -*- coding: utf-8 -*-
from hamcrest import (
    assert_that,
    equal_to,
    not_none,
    ends_with,
    calling,
    raises,
    has_entries,
    has_key,
    not_,
    contains,
    none,
)

from unittest.mock import patch
from intranet.yandex_directory.src.yandex_directory.core.utils import create_maillist
from intranet.yandex_directory.src.yandex_directory.core.models.domain import DomainModel
from testutils import (
    TestCase,
)

from intranet.yandex_directory.src.yandex_directory.passport.exceptions import (
    LoginNotavailable,
)


class Test_create_maillist(TestCase):

    def test_should_create_maillist_instance(self):
        # успешно создаем рассылку в паспорте
        uid = create_maillist(
            main_connection=self.main_connection,
            org_id=self.organization['id'],
            label='label'
        )
        assert_that(uid, not_none())

    def test_should_create_maillist_if_ignore_login_not_available_for_ml(self):
        # если учетка уже есть в паспорте и это рассылка, то просте вернем её uid
        expected_uid = 12312321
        self.mocked_passport.maillist_add.side_effect = LoginNotavailable
        
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_data_from_blackbox_by_login',
                   return_value={'is_maillist': True, 'uid': expected_uid}):
            uid = create_maillist(
                main_connection=self.main_connection,
                org_id=self.organization['id'],
                label='label',
                # Хотя логин уже занят, мы это игнорируем
                ignore_login_not_available=True
            )
        assert_that(uid, expected_uid)

    def test_should_create_maillist_if_ignore_login_not_available_for_user(self):
        # если учетка уже есть в паспорте и у нее нет атрибута что это рассылка кинем исключение
        self.mocked_passport.maillist_add.side_effect = LoginNotavailable
        
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_data_from_blackbox_by_login',
                   return_value={'is_maillist': False}):
            assert_that(
                calling(create_maillist).with_args(
                    self.main_connection,
                    self.organization['id'],
                    'label',
                    True
                ),
                raises(LoginNotavailable)
            )

    def test_without_master_domain(self):
        org_id = self.organization['id']
        # успешно создаем рассылку в паспорте
        DomainModel(self.main_connection).update(
            update_data=dict(master=False),
            filter_data=dict(org_id=org_id))
        uid = create_maillist(
            main_connection=self.main_connection,
            org_id=org_id,
            label='label'
        )
        self.assertEqual(uid, None)
