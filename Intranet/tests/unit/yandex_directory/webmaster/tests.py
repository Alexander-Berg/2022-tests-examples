# -*- coding: utf-8 -*-

from unittest.mock import (
    patch,
    ANY,

)

from hamcrest import (
    assert_that,
    calling,
    raises,
    has_entries,
    contains_inanyorder,
    not_none,
    none,
    empty,
)

from testutils import (
    TestCase,
    create_organization,
    assert_called_once,
    assert_not_called,
)

from intranet.yandex_directory.src.yandex_directory.core.models import (
    ActionModel,
    DomainModel,
)
from intranet.yandex_directory.src.yandex_directory.core.actions import action
from intranet.yandex_directory.src.yandex_directory import webmaster


class TestInfo(TestCase):
    def test_error_in_webmaster(self):
        # если вебмастер вернул ошибку, то кинем исключение

        domain_name = 'some.domain.com'
        self.mocked_webmaster_inner_info.side_effect = webmaster.WebmasterError('SOME_ERROR_CODE', 'Some error message')
        assert_that(
            calling(webmaster.info).with_args(domain_name, self.admin_uid),
            raises(webmaster.WebmasterError)
        )


class TestUpdateDomainStateIfVerified(TestCase):

    def setUp(self):
        super(TestUpdateDomainStateIfVerified, self).setUp()
        self.domain_name = 'some.owned.domain.com'
        self.owned_domain = DomainModel(self.main_connection).create(
            self.domain_name,
            self.organization['id'],
            owned=False,
            via_webmaster=True
        )

    def test_me(self):
        self.mocked_webmaster_inner_info.return_value = {
            'data': {'verificationStatus': 'VERIFIED'},
        }

        org_id = self.organization['id']

        with patch('intranet.yandex_directory.src.yandex_directory.webmaster.sms_domain_confirmed') as mocked_sms_domain_confirmed, \
             patch('intranet.yandex_directory.src.yandex_directory.webmaster.disable_domain_in_organization') as mocked_disable_domain_in_organization, \
             patch('intranet.yandex_directory.src.yandex_directory.webmaster.set_domain_as_owned') as mocked_set_domain_as_owned:
            webmaster.update_domain_state_if_verified(
                self.meta_connection,
                self.main_connection,
                org_id,
                self.admin_uid,
                self.owned_domain,
                send_sms=True
            )

        assert_called_once(
            mocked_sms_domain_confirmed,
            ANY,
            ANY,
            org_id,
            self.admin_uid,
            self.domain_name,
        )

        assert_called_once(
            mocked_disable_domain_in_organization,
            self.domain_name,
            self.organization['id'],
            self.admin_uid,
        )

        assert_called_once(
            mocked_set_domain_as_owned,
            ANY,
            ANY,
            self.organization['id'],
            self.admin_uid,
            self.domain_name,
        )


class TestDisableDomainInOrganization(TestCase):

    def setUp(self):
        super(TestDisableDomainInOrganization, self).setUp()

        second_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_org'
        )

        self.owned_domain = DomainModel(self.main_connection).create(
            name='owned.com',
            org_id=second_org['organization']['id'],
            owned=True,
        )

    def test_disable_alias(self):
        webmaster.disable_domain_in_organization(
            self.owned_domain['name'],
            self.organization['id'],
            123,
        )

        assert_that(
            DomainModel(None).get(self.owned_domain['name'], self.organization['id']),
            empty(),
        )


class TestSetDomainAsOwned(TestCase):

    def test_own_alias(self):
        domain_name = 'new.domain.com',
        DomainModel(self.main_connection).create(
            name=domain_name,
            org_id=self.organization['id'],
            via_webmaster=True
        )
        self.clean_actions_and_events()

        with patch('intranet.yandex_directory.src.yandex_directory.webmaster.create_domain_in_passport') as mocked_create_domain, \
             patch('intranet.yandex_directory.src.yandex_directory.webmaster.after_first_domain_became_confirmed') as mocked_confirm:
            mocked_create_domain.return_value = False
            webmaster.set_domain_as_owned(
                meta_connection=self.meta_connection,
                main_connection=self.main_connection,
                org_id=self.organization['id'],
                admin_id=self.admin_uid,
                domain_name=domain_name
            )

        assert_that(
            DomainModel(self.main_connection).get(domain_name, self.organization['id']),
            has_entries(
                owned=True,
                master=False,
                validated_at=not_none(),
            )
        )

        assert_that(
            ActionModel(self.main_connection).filter(org_id=self.organization['id']).all(),
            contains_inanyorder(
                has_entries(
                    name=action.domain_occupy
                )
            )
        )

        assert_not_called(mocked_confirm)

        assert_called_once(
            mocked_create_domain,
            ANY,
            self.organization['id'],
            domain_name,
            self.admin_uid,
        )
