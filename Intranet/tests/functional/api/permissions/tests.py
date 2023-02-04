# coding: utf-8
from copy import copy
from hamcrest import (
    assert_that,
    contains,
    contains_inanyorder,
    equal_to,
    has_entries,
    has_items,
)

from testutils import (
    TestCase,
    create_organization,
    get_auth_headers,
    get_all_permissions_without_current_user,
    get_all_admin_permssions,
    get_outer_admin_permissions,
)
from intranet.yandex_directory.src.yandex_directory.core.features import (
    set_feature_value_for_organization,
    CAN_WORK_WITHOUT_OWNED_DOMAIN,
)

from intranet.yandex_directory.src.yandex_directory.common.models.types import TYPE_USER
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DomainModel,
    OrganizationMetaModel,
    OrganizationModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.user import UserModel
from intranet.yandex_directory.src.yandex_directory.core.permission.permissions import (
    global_permissions,
    group_permissions,
    organization_permissions,
    user_permissions,
)
from intranet.yandex_directory.src import settings


class TestPermissions(TestCase):
    def setUp(self):
        super(TestPermissions, self).setUp()

        self.vasya = self.create_user(nickname='vasya')['id']
        self.petya = self.create_user(nickname='petya')['id']
        self.admin_permissions_free_org = get_all_admin_permssions()
        self.outer_admin_permissions_free_org = get_outer_admin_permissions()

    def test_global_permissions(self):
        # проверяем, что у админа есть право изменять данные про организацию,
        # а у простых пользователей этого права нет

        # по умолчанию, мы идем туда от имени админа
        response = self.get_json('/permissions/')
        perms = copy(self.admin_permissions_free_org)
        perms.remove(global_permissions.leave_organization)
        assert_that(
            response,
            contains_inanyorder(*perms)
        )

        # но у простого пользователя этих прав быть не должно
        response = self.get_json('/permissions/', as_uid=self.vasya)
        assert_that(
            response,
            contains_inanyorder(
                global_permissions.add_groups,
                global_permissions.invite_users,
                global_permissions.use_connect,
            )
        )

    def test_admin_can_edit_any_group(self):
        # проверяем, что у админа есть право редактировать любую группу
        # по умолчанию, мы идем туда от имени админа

        group = self.create_group(org_id=self.organization['id'], type='generic')

        url = '/permissions/?type=group&id={0}'.format(group['id'])
        response = self.get_json(url)
        perms = copy(self.admin_permissions_free_org)
        perms.remove(global_permissions.leave_organization)
        assert_that(response,
                    contains_inanyorder(*perms)
                    )

        # у простого пользователя этих прав быть не должно
        # но он может создавать группы
        response = self.get_json(url, as_uid=self.vasya)
        assert_that(
            response,
            contains_inanyorder(
                global_permissions.add_groups,
                global_permissions.invite_users,
                global_permissions.use_connect,
            )
        )

    def test_owner_can_edit_his_group(self):
        # админ группы может её редактировать
        # и создавать новые группы
        group = self.create_group(
            org_id=self.organization['id'],
            type='generic',
            admins=[self.vasya],
        )

        url = '/permissions/?type=group&id={0}'.format(group['id'])
        response = self.get_json(url, as_uid=self.vasya)
        assert_that(
            response,
            contains_inanyorder(
                global_permissions.add_groups,
                global_permissions.invite_users,
                group_permissions.edit,
                global_permissions.use_connect,
            )
        )

    def test_not_owner_cant_edit_group(self):
        # обычный пользователь группы не может её редактировать
        # но может добавлять группы
        group = self.create_group(
            org_id=self.organization['id'],
            type='generic',
            admins=[self.vasya],
        )

        # пытаемся посмотреть, какие права на эту группу есть у Пети
        url = '/permissions/?type=group&id={0}'.format(group['id'])
        response = self.get_json(url, as_uid=self.petya)
        assert_that(
            response,
            contains_inanyorder(
                global_permissions.add_groups,
                global_permissions.invite_users,
                global_permissions.use_connect,
            )
        )


    def test_user_cant_edit_yourself_in_yandex_team(self):
        # обычный пользователь не может редактировать свои данные в ЯТ
        # но может создавать группы
        org_yandex_team = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex-team',
            name={'ru': 'Яндекс Тим'}
        )['organization']

        settings.YA_TEAM_ORG_IDS = {org_yandex_team['id']}

        petya_yandex_team_id = self.create_user(
            nickname='petya_yandex_team',
            org_id=org_yandex_team['id'],
        )['id']

        # пытаемся посмотреть, какие права есть у юзера Пети из ЯТ
        url = '/permissions/?type=user&id={0}'.format(petya_yandex_team_id)
        response = self.get_json(url, as_uid=petya_yandex_team_id)
        assert_that(response, equal_to([global_permissions.add_groups, global_permissions.use_connect]))

    def test_user_can_edit_himself(self):
        # обычный пользователь не из ЯТ-организации может
        # редактировать свои данные, менять пароль и менять аватарку
        # и создавать группы

        url = '/permissions/?type=user&id={0}'.format(self.petya)
        response = self.get_json(url, as_uid=self.petya)
        assert_that(
            response,
            contains_inanyorder(
                user_permissions.edit_contacts,
                user_permissions.edit_birthday,
                user_permissions.change_avatar,
                user_permissions.edit_info,
                global_permissions.add_groups,
                global_permissions.invite_users,
                global_permissions.use_connect,
            ),
        )

    def test_outer_admin_permissions(self):
        # проверяем, что у внешнего админа есть все права
        headers = get_auth_headers(as_outer_admin={
            'id': self.outer_admin['id'],
            'org_id': self.organization['id'],
        })
        response = self.get_json('/permissions/', headers=headers)
        assert_that(
            response,
            contains_inanyorder(*self.outer_admin_permissions_free_org)
        )

    def test_outer_admin_permissions_for_org_with_billing_info(self):
        # проверяем, что у внешнего админа есть право удалить организацию с биллинговой информацией
        self.enable_paid_mode()
        headers = get_auth_headers(as_outer_admin={
            'id': self.outer_admin['id'],
            'org_id': self.organization['id'],
        })
        # url = '/permissions/?type=organization&id={0}'.format(self.organization['id'])
        response = self.get_json('/permissions/', headers=headers)
        outer_admin_permissions = get_outer_admin_permissions()
        assert_that(
            response,
            contains_inanyorder(*outer_admin_permissions)
        )

    def test_permissions_outer_admin_with_incorrect_org(self):
        # проверяем, что если передали внешнего админа,
        # но с неверной организацией,
        # то возникает ошибка
        headers = get_auth_headers(as_outer_admin={
            'id': self.outer_admin['id'],
            'org_id': 54444546456,
        })
        self.get_json('/permissions/', headers=headers, expected_code=403)

    def test_permissions_for_admin_himself(self):
        # проверяем, что админ не может сам себя блокировать
        OrganizationModel(self.main_connection).\
            filter(id=self.organization['id']).\
            update(subscription_plan='paid')
        user = self.create_user(
            nickname='admin_user',
            department_id=self.department['id'],
        )
        user_model = UserModel(self.main_connection)

        user_model.make_admin_of_organization(
            org_id=self.organization['id'],
            user_id=user['id']
        )
        is_admin = user_model.is_admin(self.organization['id'], user['id'])
        self.assertTrue(is_admin)

        headers = get_auth_headers(as_outer_admin={
            'id': user['id'],
            'org_id': self.organization['id'],
        })
        response = self.get_json(
            '/permissions/',
            query_string={'id': user['id'], 'type': TYPE_USER},
            headers=headers,
        )
        expected = get_all_permissions_without_current_user()
        assert_that(
            response,
            contains_inanyorder(*expected)
        )

    def test_permissions_for_admin_himself_last_admin(self):
        # проверяем, что админ не может сам себя блокировать или убирать из админов, если он владелец организации
        OrganizationModel(self.main_connection).\
            filter(id=self.organization['id']).\
            update(subscription_plan='paid')

        # создадим еще одного админа, чтобы владелец не был единственным админом
        user = self.create_user(
            nickname='admin_user',
            department_id=self.department['id'],
        )
        user_model = UserModel(self.main_connection)

        user_model.make_admin_of_organization(
            org_id=self.organization['id'],
            user_id=user['id']
        )
        is_admin = user_model.is_admin(self.organization['id'], user['id'])
        self.assertTrue(is_admin)

        headers = get_auth_headers(as_outer_admin={
            'id': self.user['id'],
            'org_id': self.organization['id'],
        })
        response = self.get_json(
            '/permissions/',
            query_string={'id': self.user['id'], 'type': TYPE_USER},
            headers=headers,
        )
        expected = get_all_permissions_without_current_user()
        expected.remove(user_permissions.change_role)
        assert_that(
            response,
            contains_inanyorder(*expected)
        )

    def test_permissions_for_free_org(self):
        # проверяем, что админы не могут менять лого у бесплатной организации
        assert_that(
            self.organization['subscription_plan'],
            equal_to('free')
        )

        # внешний админ не может менять лого
        headers = get_auth_headers(as_outer_admin={
            'id': self.outer_admin['id'],
            'org_id': self.organization['id'],
        })
        response = self.get_json('/permissions/', headers=headers)
        assert_that(
            response,
            contains_inanyorder(*self.outer_admin_permissions_free_org)
        )

        # админ организации тоже не может менять лого
        response = self.get_json('/permissions/')
        perms = copy(self.admin_permissions_free_org)
        perms.remove(global_permissions.leave_organization)
        assert_that(
            response,
            contains_inanyorder(*perms)
        )

    def test_has_no_master_domain(self):
        # проверяем, что у внешнего админа в организации нет мастер-домена, и выключена фича ЯОрг,
        # у него есть add_domains право
        org_id = self.organization['id']
        set_feature_value_for_organization(
            self.meta_connection,
            org_id,
            CAN_WORK_WITHOUT_OWNED_DOMAIN,
            False
        )

        DomainModel(self.main_connection).update(
            update_data={'master': False},
            filter_data={'org_id': org_id},
        )

        headers = get_auth_headers(as_outer_admin={
            'id': self.outer_admin['id'],
            'org_id': self.organization['id']
        })
        response = self.get_json('/permissions/', headers=headers)
        assert_that(
            response,
            has_items(
                global_permissions.add_domains,
                organization_permissions.delete,
                global_permissions.remove_domain,
                global_permissions.change_logo,
            )
        )
