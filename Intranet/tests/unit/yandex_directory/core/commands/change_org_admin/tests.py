# -*- coding: utf-8 -*-
from intranet.yandex_directory.src.yandex_directory.core.commands.change_org_admin import Command as ChangeOrgAdminCommand

from hamcrest import (
    assert_that,
    equal_to,
    has_entries,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationModel,
    UserMetaModel,
)


from testutils import (
    TestCase,
    create_organization,
    PASSPORT_TEST_OUTER_UID,
)


class TestChangeOrgAdmin(TestCase):

    create_organization = False

    def setUp(self):
        super(TestChangeOrgAdmin, self).setUp()
        organization_info = create_organization(
            self.meta_connection,
            self.main_connection,
            label=self.label,
            domain_part=self.domain_part,
            language=self.language,
            admin_uid=PASSPORT_TEST_OUTER_UID,
            root_dep_label=self.root_dep_label,
            tld=self.tld,
        )
        self.organization = organization_info['organization']

    def check_admin(self, admin_uid):
        assert_that(
            OrganizationModel(self.main_connection).get(self.organization['id']),
            has_entries(admin_uid=admin_uid)
        )

        assert_that(
            UserMetaModel(self.meta_connection).filter(org_id=self.organization['id'], is_outer=True).one(),
            has_entries(id=admin_uid)
        )

    def test_only_outer_admin_allowed(self):
        # передача возможна только внешнему админу
        new_admin = 111 * 10 ** 13 + 100
        assert_that(
            ChangeOrgAdminCommand().try_run(
                org_id=self.organization['id'],
                old_admin_uid=PASSPORT_TEST_OUTER_UID,
                new_admin_uid=new_admin,
            ),
            equal_to(False)
        )
        self.check_admin(PASSPORT_TEST_OUTER_UID)

    def test_success_change_outer_admin(self):
        # удачно меняем вненего админа
        new_admin = PASSPORT_TEST_OUTER_UID + 1
        assert_that(
            ChangeOrgAdminCommand().try_run(
                org_id=self.organization['id'],
                old_admin_uid=PASSPORT_TEST_OUTER_UID,
                new_admin_uid=new_admin,
            ),
            equal_to(True)
        )
        self.check_admin(new_admin)

