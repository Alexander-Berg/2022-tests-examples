# coding: utf-8

from unittest.mock import (
    patch,
)
from hamcrest import (
    assert_that,
    contains,
    has_entries,
)

from testutils import (
    TestCase,
    create_outer_uid,
    assert_called_once,
    create_yandex_user,
    create_organization,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    ActionModel,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.users.base import (
    add_user_by_invite,
    UserAlreadyMemberOfOrganization,
    UserAlreadyInThisOrganization,
)


class TestAddUserByInvite(TestCase):

    def test_user_not_dismissed(self):
        create_yandex_user(
            self.meta_connection, self.main_connection,
            uid='100', org_id=self.organization['id'],

        )

        with self.assertRaises(UserAlreadyInThisOrganization):
            add_user_by_invite(
                self.meta_connection, self.main_connection,
                uid='100',
                user_ip='127.0.0.1',
                invite=dict(dummy=True, org_id=self.organization['id'], department_id=1)
            )


    def test_user_dismissed(self):
        uid = create_outer_uid()
        create_yandex_user(
            self.meta_connection, self.main_connection,
            uid=uid, org_id=self.organization['id'],

        )

        UserModel(self.main_connection).dismiss(
            org_id=self.organization['id'],
            user_id=uid,
            author_id=100,
        )
        ActionModel(self.main_connection).filter(org_id=self.organization['id']).delete()

        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.users.base.use_invite', return_value=None) as invite_func:
            dummy_invite = dict(org_id=self.organization['id'], department_id=1, add_license=False)
            add_user_by_invite(
                self.meta_connection, self.main_connection,
                uid=uid,
                user_ip='127.0.0.1',
                invite=dummy_invite
            )
            assert_called_once(invite_func, self.meta_connection, dummy_invite, uid)
        restored_user = UserModel(self.main_connection) \
            .filter(id=uid, org_id=self.organization['id'], is_dismissed=False) \
            .one()
        self.assertTrue(restored_user)
        # Проверим, что сгенерилось событие
        actions = ActionModel(self.main_connection)\
            .filter(org_id=self.organization['id'])\
            .fields('name')\
            .all()
        assert_that(
            actions,
            contains(
                has_entries(name='user_add'),
            )
        )

