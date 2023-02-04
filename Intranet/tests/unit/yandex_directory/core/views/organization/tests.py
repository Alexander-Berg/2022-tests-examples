# -*- coding: utf-8 -*-
from unittest.mock import (
    patch,
    ANY,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.views.organization.utils import (
    _process_organizations_heads,
    _delay_welcome_email,
)
from intranet.yandex_directory.src.yandex_directory.core.models.organization import OrganizationModel
from testutils import TestCase
from hamcrest import (
    assert_that,
    has_entry,
    has_entries,
)


class TestLoadOrganization(TestCase):
    def get_organization(self, pass_main_connection=True):
        organizations = OrganizationModel(self.main_connection).find(
            {'id': self.organization['id']},
            fields=['head.nickname'],
        )

        _process_organizations_heads(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection if pass_main_connection else None,
            organizations=organizations,
            api_version=1,
        )
        return organizations[0]

    def test_organization_has_no_head(self):
        # у организации нет руководителя :(

        org = self.get_organization()
        assert_that(
            org,
            has_entry('head', None)
        )


        # А теперь попробуем то же самое, но передав в
        # _process_organizations_heads main_connection=None
        org = self.get_organization(pass_main_connection=False)
        assert_that(
            org,
            has_entry('head', None)
        )

    def test_organization_has_head(self):
        # у организации есть руководитель :)

        OrganizationModel(self.main_connection).update(
            update_data={
                'head_id': self.user['id']
            },
            filter_data={
                'id': self.organization['id']
            }

        )
        org = self.get_organization()
        _process_organizations_heads(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            organizations=[org],
            api_version=1,
        )
        assert_that(
            org,
            has_entries(
                head=has_entries(
                    id=self.user['id'],
                    nickname=self.user['nickname'],
                )
            )
        )


class TestDelayWelcomeEmail(TestCase):

    def test_delay_welcome_email(self):
        # создаем оложенную задачу на отправку
        with patch('threading.Timer') as mock_patch:
            _delay_welcome_email(self.organization['id'], 'admin_nickname', int(self.admin_uid))
            mock_patch.assert_called_once_with(
                app.config['MAIL_SEND_DELAY'],
                ANY,
                kwargs={
                    'org_id':self.organization['id'],
                    'uid': int(self.admin_uid)
                }
            )
            # проверяем что ставим отсрочку на вызоа функции с именем _send_welcome_email
            # это функция обэртка вокру send_welcome_email объявлена внутри delay_welcome_email
            assert_that(
                mock_patch.call_args[0][1].__name__,
                '_send_welcome_email'
            )
