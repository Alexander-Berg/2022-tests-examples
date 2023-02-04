# coding: utf-8

from intranet.yandex_directory.src.yandex_directory.core.models.user import UserModel
from intranet.yandex_directory.src.yandex_directory.common.db import catched_sql_queries
from intranet.yandex_directory.src.yandex_directory.core.utils import create_user
from testutils import TestCase, create_outer_uid, create_inner_uid, create_organization
from intranet.yandex_directory.src.yandex_directory.core.views.organization.utils import remove_orgs_with_deleted_portal_user
from hamcrest import (
    assert_that,
    has_length,
)


def int_seq():
    i = 0
    while True:
        yield i
        i += 1


creation_sequence = int_seq()
label_seq = int_seq()


class TestRemoveOrgsOfDeletedUser(TestCase):
    create_organization = False

    def _create_user(self, uid, dismiss):
        random_organization = create_organization(
                self.meta_connection,
                self.main_connection,
                label='random {}'.format(next(label_seq)),
                domain_part='yandex{}.ru'.format(next(creation_sequence)),
                language=self.language,
                root_dep_label=self.root_dep_label,
                tld=self.tld,
            )['organization']
        user_data = {
            'id': uid,
            'name': {
                'first': {
                    'ru': 'Gena'
                },
                'last': {
                    'ru': 'Chibisov'
                }
            },
            'gender': 'male',
            'nickname': 'web-chib',
            'email': 'web-chib@ya.ru',
        }
        created_user = create_user(
            self.meta_connection,
            self.main_connection,
            org_id=random_organization['id'],
            user_data=user_data,
            nickname='Arkadiy'
        )

        if dismiss:
            UserModel(self.main_connection).dismiss(
                org_id=random_organization['id'],
                user_id=uid,
                author_id=uid,
            )
        return random_organization

    def assertEqualOrgs(self, list1, list2):
        self.assertEqual(
            [org['id'] for org in list1],
            [org['id'] for org in list2],
        )

    def test_it_deletes(self):
        the_uid = create_outer_uid()
        org1 = self._create_user(uid=the_uid, dismiss=False)
        org2 = self._create_user(uid=the_uid, dismiss=False)
        orgs_of_user = [
            org1,
            org2,
            self._create_user(uid=the_uid, dismiss=True)
        ]
        self._create_user(
            uid=create_outer_uid(),
            dismiss=False
        )
        self._create_user(
            uid=create_inner_uid(1),
            dismiss=False
        )
        with catched_sql_queries() as queries:
            orgs = remove_orgs_with_deleted_portal_user(
                orgs_of_user,
                uid=the_uid
            )
            assert_that(
                queries,
                has_length(1)
            )

        self.assertEqualOrgs(orgs,
                             [org1, org2])

    def test_it_does_nothing_on_inner_uid(self):
        self.assertEqual(
            [1, 2],
            remove_orgs_with_deleted_portal_user([1, 2], uid=create_inner_uid(1)))
