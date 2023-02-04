# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    equal_to,
    contains_inanyorder,
    has_item,
    is_not,
    greater_than,
    not_,
    contains,
    is_in,
    has_items,
)
from unittest.mock import (
    patch,
    ANY,
)

from testutils import (
    TestCase,
    TestOuterAdmin,
    create_organization,
    get_auth_headers,
    TestOrganizationWithoutDomainMixin,
    get_yandex_team_permissions,
    get_all_permissions_without_current_user,
    get_all_admin_permssions,
    get_outer_admin_permissions,
    get_deputy_admin_permissions,
    get_yandex_organization_permissions_with_domain,
    assert_called,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.features import (
    set_feature_value_for_organization,
    CAN_WORK_WITHOUT_OWNED_DOMAIN,
    CHANGE_ORGANIZATION_OWNER,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DomainModel,
    DepartmentModel,
    OrganizationMetaModel,
    OrganizationModel,
    ServiceModel,
    PresetModel,
    UserModel,
)
from intranet.yandex_directory.src.yandex_directory.common.models import types as OBJECT_TYPES
from intranet.yandex_directory.src.yandex_directory.core.permission.internal_permissions import (
    all_internal_assessor_permissions,
    all_internal_support_permissions,
    all_internal_bizdev_permissions,
    all_internal_admin_permissions,
    get_admin_permissions,
)
from intranet.yandex_directory.src.yandex_directory.core.permission.permissions import (
    get_permissions,
    group_permissions,
    user_permissions, sso_permissions,
)
from intranet.yandex_directory.src.yandex_directory.core.permission.permissions import (
    global_permissions,
    organization_permissions,
    department_permissions,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import create_robot_for_service_and_org_id
from intranet.yandex_directory.src import settings

yandex_team_domain = 'yandex-team.ru'
from ..... import webmaster_responses
from intranet.yandex_directory.src.yandex_directory.core.dependencies import (
    Service,
    Setting,
)


class TestPromoPermissions(TestCase):
    def setUp(self):
        super(TestPromoPermissions, self).setUp()

        # id админа организации в паспорте (создан при регистрации через паспорт)
        self.admin_uid = 1
        self.domain_name = 'test-domain.qqq'
        # домен новой организации
        self.domain = 'test-org{}'.format(app.config['DOMAIN_PART'])
        # userinfo из ЧЯ
        self.mocked_blackbox.userinfo.return_value = {
            'fields': {
                'country': 'ru',
                'login': 'admin',
            },
            'domain': 'camomile.yaserv.biz',
            'uid': self.admin_uid,
            'default_email': 'default@ya.ru',
        }
        # Сделаем сервис, который надо включить в пресет
        self.service_slug = 'the-service'
        ServiceModel(self.meta_connection).create(
            slug=self.service_slug,
            name='The Service',
        )

        # Сделаем сервис dashboard, который надо включить до подтверждения домена
        self.dashboard_slug = 'dashboard'
        ServiceModel(self.meta_connection).create(
            slug=self.dashboard_slug,
            name='Dashboard',
        )

        # Создадим пресет
        PresetModel(self.meta_connection).create(
            'only-the-service',
            service_slugs=[self.service_slug, self.dashboard_slug],
            settings={}
        )

        # И пресет no-owned-domain с сервисов dashboard
        PresetModel(self.meta_connection).create(
            'no-owned-domain',
            service_slugs=[self.dashboard_slug],
            settings={}
        )

        self.dependencies = {
            Service(self.service_slug): [
                Setting('shared-contacts', True)
            ],
        }

    def test_can_remove_domain_when_it_more_that_one_for_org_with_promo(self):
        # в организации без подтвержденных доменов можно удалять все домены
        self.mocked_blackbox.userinfo.reset_mock()
        with patch('intranet.yandex_directory.src.yandex_directory.core.views.domains.create_domain_in_passport') as add_domain, \
                patch('intranet.yandex_directory.src.yandex_directory.core.dependencies.dependencies', new=self.dependencies):
            self.mocked_webmaster_inner_info.side_effect = webmaster_responses.ok(owned=False)
            some_org = self.post_json(
                '/organization/with-domain/',
                data={
                    'domain_name': self.domain_name,
                    'preset': 'only-the-service',
                    'source': 'pdd_new_promo',
                    'tld': 'com',
                },
                headers=get_auth_headers(as_uid=self.admin_uid),
            )
            assert_called(
                self.mocked_blackbox.userinfo,
                3,
            )
        some_org_id = some_org['org_id']

        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.admin_uid,
            org_id=some_org_id,
        )
        assert_that(
            result,
            has_items(
                department_permissions.edit,
                global_permissions.activate_promocode,
                global_permissions.add_departments,
                global_permissions.add_domains,
                global_permissions.add_groups,
                global_permissions.manage_services,
                global_permissions.remove_departments,
                global_permissions.remove_domain,
                global_permissions.invite_users,
                global_permissions.change_logo,
                group_permissions.edit,
                organization_permissions.delete,
                organization_permissions.edit,
                user_permissions.change_alias,
                user_permissions.change_role,
                user_permissions.dismiss,
                user_permissions.edit,
                user_permissions.make_admin,
            )
        )


class Test_get_permissions_global(TestOuterAdmin, TestCase):
    def setUp(self):
        super(Test_get_permissions_global, self).setUp()
        self.org_admin = self.orginfo_1['admin_user']
        self.org_id = self.orginfo_1['organization']['id']

    def test_get_permission_for_non_admin(self):
        created_user = self.create_user()
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            created_user['id'],
            org_id=self.organization['id'],
        )
        assert_that(
            result,
            contains_inanyorder(
                global_permissions.add_groups,
                global_permissions.invite_users,
                global_permissions.use_connect,
            )
        )

    def test_get_permission_for_admin(self):
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.org_admin['id'],
            org_id=self.org_id,
        )
        result.sort()

        admin_permissions = get_all_admin_permssions()
        admin_permissions.remove(global_permissions.leave_organization)
        assert_that(result, contains_inanyorder(*admin_permissions))

    def test_get_permission_for_admin_yateam(self):
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.org_admin['id'],
            org_id=self.org_id,
        )
        assert_that(
            result,
            has_items(
                *get_yandex_team_permissions()
            ),
        )

    def test_get_permission_for_outer_admin(self):
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.admin_org1['id'],
            org_id=self.org_id,
        )
        admin_permissions = get_all_admin_permssions()
        admin_permissions.remove(global_permissions.manage_licenses)
        admin_permissions.remove(global_permissions.manage_tracker)
        admin_permissions.remove(global_permissions.can_pay)

        assert_that(
            result,
            contains_inanyorder(
                organization_permissions.add_organization,
                *admin_permissions
            )
        )

    def test_get_permission_for_deputy_admin(self):
        outer_deputy_admin = self.create_deputy_admin(org_id=self.orginfo_1['organization']['id'])
        inner_deputy_admin = self.create_deputy_admin(org_id=self.orginfo_1['organization']['id'], is_outer=False)
        outer_result = get_permissions(
            self.meta_connection,
            self.main_connection,
            uid=outer_deputy_admin['id'],
            org_id=self.orginfo_1['organization']['id'],
        )
        inner_result = get_permissions(
            self.meta_connection,
            self.main_connection,
            uid=inner_deputy_admin['id'],
            org_id=self.orginfo_1['organization']['id'],
        )
        all_deputy_admin_permissions = get_deputy_admin_permissions()
        assert_that(all_deputy_admin_permissions, not_(contains(user_permissions.make_admin)))
        assert_that(outer_result, contains_inanyorder(*all_deputy_admin_permissions))
        all_deputy_admin_permissions.remove(global_permissions.leave_organization)
        assert_that(inner_result, contains_inanyorder(*all_deputy_admin_permissions))

    def test_get_change_subscription_plan_permission_for_simple_and_outer_admin(self):
        # Теперь организации создаются с Яорг фичей, а этот тест был написан до неё,
        # поэтому тут сначала надо отключить фичу.
        org_id = self.orginfo_1['organization']['id']

        set_feature_value_for_organization(
            self.meta_connection,
            org_id,
            CAN_WORK_WITHOUT_OWNED_DOMAIN,
            False
        )

        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.admin_org1['id'],
            org_id=org_id,
        )
        # у внешнего админа нет права на смену тарифного плана
        assert_that(
            result,
            is_not(
                has_item(global_permissions.change_subscription_plan),
            ),
        )

        simple_user = self.create_user()
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            simple_user['id'],
            org_id=self.orginfo_1['organization']['id'],
        )
        # у обычного пользователя нет права на смену тарифного плана
        assert_that(
            result,
            is_not(
                has_item(global_permissions.change_subscription_plan),
            ),
        )

        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.user['id'],
            org_id=self.orginfo_1['organization']['id'],
        )
        # у обычного админа есть право оплаты
        assert global_permissions.change_subscription_plan in result


    def test_get_can_pay_permission_for_simple_and_outer_admin(self):
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.admin_org1['id'],
            org_id=self.orginfo_1['organization']['id'],
        )
        # у внешнего админа нет права оплаты
        assert_that(
            result,
            is_not(
                has_item(global_permissions.can_pay),
            ),
        )

        simple_user = self.create_user()
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            simple_user['id'],
            org_id=self.orginfo_1['organization']['id'],
        )
        # у обычного пользователя нет права оплаты
        assert_that(
            result,
            is_not(
                has_item(global_permissions.can_pay),
            ),
        )

        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.user['id'],
            org_id=self.orginfo_1['organization']['id'],
        )
        # у обычного админа есть право оплаты
        assert_that(
            result,
            has_item(global_permissions.can_pay),
        )


class Test_get_permissions_for_organization_without_domain(TestOrganizationWithoutDomainMixin, TestCase):
    def test_get_admins_permission(self):
        # У админа есть права добавлять домен, приглашать пользователя,
        # создавать группы и отделы, удалять организацию, управлять трекером
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            uid=self.yandex_admin['id'],
            org_id=self.yandex_organization['id'],
        )
        assert_that(
            result,
            has_items(
                global_permissions.add_domains,
                organization_permissions.delete,
                organization_permissions.edit,
                global_permissions.add_departments,
                global_permissions.add_groups,
                global_permissions.invite_users,
                global_permissions.remove_domain,
                global_permissions.manage_tracker,
                global_permissions.manage_services,
                global_permissions.remove_departments,
                # TODO: DIR-6085 тест должен срабатывать если задана группа
                group_permissions.edit,
                user_permissions.edit,
                user_permissions.change_role,
                user_permissions.make_admin,
                department_permissions.edit,
                global_permissions.can_pay,
                global_permissions.change_logo,
                global_permissions.activate_promocode,
                user_permissions.dismiss,
                user_permissions.change_alias,
            )
        )

    def test_users_permission(self):
        # У пользователя есть право создавать группу
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.yandex_user['id'],
            org_id=self.yandex_organization['id'],
        )
        assert_that(
            result,
            contains_inanyorder(
                global_permissions.add_groups,
                global_permissions.invite_users,
                global_permissions.use_connect,
                global_permissions.leave_organization,
            )
        )

    def test_admin_permissions_for_portal_account(self):
        # У админа яндекс организации нет права редактировать и блокировать портальные аккаунты
        # Добавляем подтвержденный домен в яндекс организацию
        DomainModel(self.main_connection).create(
            'not-yandex-team',
            self.yandex_organization['id'],
            True,
            True,
        )

        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            uid=self.yandex_admin['id'],
            object_type=OBJECT_TYPES.TYPE_USER,
            object_id=self.yandex_user['id'],
            org_id=self.yandex_organization['id'],
        )
        expected = get_yandex_organization_permissions_with_domain()

        excluded = [
            user_permissions.change_avatar,
            user_permissions.edit_birthday,
            user_permissions.edit_contacts,
            user_permissions.change_password,
            user_permissions.block,
            user_permissions.edit_info,
            user_permissions.change_alias,
            user_permissions.move_to_staff,
        ]
        for p in excluded:
            expected.remove(p)
        assert_that(
            result,
            contains_inanyorder(*expected),
        )

    def test_portal_user_permissions_for_yourself(self):
        # У пользователя яндекс организации нет права менять себе аватар, пароль, контакты, день рождения
        DomainModel(self.main_connection).create(
            'not-yandex-team',
            self.yandex_organization['id'],
            True,
            True,
        )

        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.yandex_user['id'],
            'user',
            self.yandex_user['id'],
            self.yandex_organization['id'],
        )
        expected = [
            global_permissions.add_groups,
            global_permissions.invite_users,
            global_permissions.use_connect,
            global_permissions.leave_organization,
        ]
        assert_that(
            result,
            contains_inanyorder(*expected)
        )


class Test_get_permissions_for_object(TestCase):
    def setUp(self):
        super(Test_get_permissions_for_object, self).setUp()

        self.org_admin = self.organization_info['admin_user']
        self.org_id = self.organization_info['organization']['id']

    def test_get_permission_for_non_group_admin(self):
        created_user = self.create_user()
        group = self.create_group(
            members=[{'type': 'user', 'object': created_user}]
        )
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            created_user['id'],
            'group',
            group['id'],
            org_id=self.org_id,
        )
        assert_that(
            result,
            contains_inanyorder(
                global_permissions.add_groups,
                global_permissions.invite_users,
                global_permissions.use_connect,
            )
        )

    def test_get_permission_for_not_existed_group(self):
        user = self.create_user()

        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            user['id'],
            'group',
            1234355456,
            org_id=self.org_id,
        )
        assert_that(
            result,
            contains_inanyorder(
                global_permissions.add_groups,
                global_permissions.invite_users,
                global_permissions.use_connect,
            )
        )

    def test_get_permission_for_group_admin(self):
        user = self.create_user()
        group = self.create_group(admins=[user['id']])

        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            user['id'],
            'group',
            group['id'],
            org_id=self.org_id,
        )
        # может редактировать свою группу
        # может создавать группы, как и любой пользователь
        admin_permissions = [
            group_permissions.edit,
            global_permissions.add_groups,
            global_permissions.invite_users,
            global_permissions.use_connect,
        ]
        assert_that(result, contains_inanyorder(*admin_permissions))

    def test_get_user_permission_for_oneself(self):
        user = self.create_user()
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            user['id'],
            'user',
            user['id'],
            org_id=self.org_id,
        )
        expected_permissions = [
            user_permissions.edit_info,
            user_permissions.edit_birthday,
            user_permissions.edit_contacts,
            user_permissions.change_avatar,
            global_permissions.add_groups,
            global_permissions.invite_users,
            global_permissions.use_connect,
        ]
        assert_that(result, contains_inanyorder(*expected_permissions))

    def test_get_user_permission_for_yourself_yateam(self):
        settings.YA_TEAM_ORG_IDS = {self.organization['id']}

        user = self.create_user()
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            user['id'],
            'user',
            user['id'],
            org_id=self.org_id,
        )
        assert_that(
            result,
            equal_to(
                [
                    global_permissions.add_groups,
                    global_permissions.use_connect,
                ]
            )
        )

    def test_get_user_permission_for_admin(self):
        another_admin = self.create_user()
        UserModel(self.main_connection).make_admin_of_organization(
            org_id=self.organization['id'],
            user_id=another_admin['id']
        )
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            another_admin['id'],
            'user',
            another_admin['id'],
            org_id=self.org_id,
        )
        expected = get_all_permissions_without_current_user()
        assert_that(result, contains_inanyorder(*expected))

    def test_get_user_permission_for_owner(self):
        # Владелец не может уволить или заблокировать себя
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.org_admin['id'],
            'user',
            # запрашиваем права на операции над самим собой
            self.org_admin['id'],
            org_id=self.org_id,
        )
        expected = get_all_permissions_without_current_user()
        # нельзя менять роль у владельца
        expected.remove(user_permissions.change_role)
        assert_that(result, contains_inanyorder(*expected))

    def test_get_deputy_admin_permission_for_admin(self):
        # зам не может ничего сделать с админом, но у него есть все глобальные права
        deputy_admin = self.create_deputy_admin()
        result = get_permissions(
            self.meta_connection,
            self.main_connection,
            deputy_admin['id'],
            object_type=OBJECT_TYPES.TYPE_USER,
            object_id=self.org_admin['id'],
            org_id=self.org_id,
        )
        expected = get_deputy_admin_permissions()
        expected.remove(user_permissions.change_avatar)
        expected.remove(user_permissions.block)
        expected.remove(user_permissions.dismiss)
        expected.remove(user_permissions.change_password)
        expected.remove(user_permissions.edit_contacts)
        expected.remove(user_permissions.edit_birthday)
        expected.remove(user_permissions.edit)
        expected.remove(user_permissions.edit_info)
        expected.remove(user_permissions.change_alias)
        assert_that(result, contains_inanyorder(*expected))

    def test_get_user_permissions_for_robot_user(self):
        # права у роботного пользователя
        self.mocked_passport.account_add.side_effect = lambda *args, **kwargs: self.get_next_uid(outer_admin=False)
        robot_uid = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id']
        )

        # у роботного пользователя нет никаких прав на редактирование себя,
        # но есть право создавать группы
        result = get_permissions(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            uid=robot_uid,
            object_type='user',
            object_id=robot_uid,
            org_id=self.org_id,
        )
        assert_that(
            result,
            contains_inanyorder(
                global_permissions.add_groups,
                global_permissions.invite_users,
                global_permissions.use_connect,
            )
        )

    def test_get_admin_permissions_for_robot_support_user(self):
        # у админа нет никаких прав на роботного пользователя
        # Но есть все остальные права
        robot_uid = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id']
        )
        result = get_permissions(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            uid=self.admin_uid,
            object_type='user',
            object_id=robot_uid,
            org_id=self.org_id,
        )
        expected = get_all_admin_permssions()
        expected.remove(user_permissions.change_avatar)
        expected.remove(user_permissions.block)
        expected.remove(user_permissions.dismiss)
        expected.remove(user_permissions.change_password)
        expected.remove(user_permissions.edit_contacts)
        expected.remove(user_permissions.edit_birthday)
        expected.remove(user_permissions.edit)
        expected.remove(user_permissions.make_admin)
        expected.remove(user_permissions.change_role)
        expected.remove(user_permissions.edit_info)
        expected.remove(global_permissions.leave_organization)
        assert_that(result, contains_inanyorder(*expected))

    def test_get_outer_admin_permissions_for_free_organization(self):
        # внешний админ может удалить бесплатную организацию, но не может менять логотип
        result = get_permissions(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            uid=self.outer_admin['id'],
            object_type='organization',
            object_id=self.organization['id'],
            org_id=self.organization['id'],
        )

        outer_admin_permissions = get_outer_admin_permissions()
        assert_that(
            result,
            contains_inanyorder(*outer_admin_permissions),
        )

    def test_get_permissions_for_education_organization(self):
        # образовательным организациям нельзя применять промокоды
        education_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='edu_org',

        )['organization']
        OrganizationModel(self.main_connection).update(
            filter_data={'id': education_org['id']},
            update_data={'organization_type': 'education'}
        )
        result = get_permissions(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            uid=education_org['admin_uid'],
            org_id=education_org['id'],
        )
        admin_permissions = get_all_admin_permssions()
        admin_permissions.remove(global_permissions.activate_promocode)
        admin_permissions.remove(global_permissions.leave_organization)
        assert_that(
            result,
            contains_inanyorder(*admin_permissions)
        )

    def test_get_permissions_for_portal_organization(self):
        # portal (порталы) не могут активировать/деактивировать сервисы
        portal_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='portal_org',

        )['organization']
        OrganizationModel(self.main_connection).update(
            filter_data={'id': portal_org['id']},
            update_data={'organization_type': 'portal'}
        )
        result = get_permissions(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            uid=portal_org['admin_uid'],
            org_id=portal_org['id'],
        )
        admin_permissions = get_all_admin_permssions()
        admin_permissions.remove(global_permissions.manage_services)
        admin_permissions.remove(global_permissions.manage_tracker)
        admin_permissions.remove(global_permissions.migrate_emails)
        admin_permissions.remove(global_permissions.leave_organization)
        assert_that(
            result,
            contains_inanyorder(*admin_permissions)
        )

    def test_get_permission_change_owner(self):
        self.set_feature_value_for_organization(CHANGE_ORGANIZATION_OWNER, True)

        # проверим, что владелец организации может выпилить себя
        result = get_permissions(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            uid=self.admin_uid,
            object_type='organization',
            object_id=self.organization['id'],
            org_id=self.organization['id'],
        )
        admin_permissions = get_all_admin_permssions()
        admin_permissions.append(organization_permissions.change_owner)
        admin_permissions.remove(global_permissions.leave_organization)
        assert_that(
            result,
            contains_inanyorder(*admin_permissions)
        )

        # проверим, что другой админ не может менять владельца
        other_admin = self.create_user()
        UserModel(self.main_connection).make_admin_of_organization(
            org_id=self.organization['id'],
            user_id=other_admin['id']
        )
        result = get_permissions(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            uid=other_admin['id'],
            object_type='organization',
            object_id=self.organization['id'],
            org_id=self.organization['id'],
        )
        other_admin_permissions = get_all_admin_permssions()
        other_admin_permissions.remove(global_permissions.leave_organization)
        assert_that(
            result,
            contains_inanyorder(*other_admin_permissions)
        )

    def test_admin_can_move_to_staff(self):
        # Проверим, что админ имеет право переместить внештатника в штат.
        # При этом на штатном сотруднике такого права быть не должно.
        # И это всё должно работать только для админов.
        org_id = self.organization['id']
        # штатный сотрудник
        user = self.create_user()

        outstaff = DepartmentModel(self.main_connection) \
                   .get_or_create_outstaff(org_id)
        outstaffer = self.create_user(department_id=outstaff['id'])

        is_not_in = lambda result: is_not(is_in(result))

        params = [
            ('На аутстаффере право быть должно',
             self.admin_uid, outstaffer['id'], is_in),
            ('А на штатном сотруднике - нет',
             self.admin_uid, user['id'], is_not_in),
            ('И если текущий сотрудник не админ, то права тоже быть не должно',
             user['id'], outstaffer['id'], is_not_in),
        ]
        for reason, admin_id, user_id, predicate in params:
            result = get_permissions(
                meta_connection=self.meta_connection,
                main_connection=self.main_connection,
                uid=admin_id,
                object_type='user',
                object_id=user_id,
                org_id=org_id,
            )

            assert_that(
                user_permissions.move_to_staff,
                predicate(result),
                reason
            )


class TestPermissions(TestOuterAdmin, TestCase):
    def setUp(self):
        super(TestPermissions, self).setUp()
        self.org_admin = self.orginfo_1['admin_user']
        self.org_id = self.orginfo_1['organization']['id']

        self.admin_permissions =  get_permissions(
            self.meta_connection,
            self.main_connection,
            self.org_admin['id'],
            org_id=self.org_id,
        )
        self.outer_permissions = get_permissions(
            self.meta_connection,
            self.main_connection,
            self.admin_org1['id'],
            org_id=self.org_id,
        )

    def test_outer_admin_permissions_should_be_equal_to_admin_permissions_exclude_changing_subscription(self):
        # удалим у обоих админов те права, которые есть только у них и сравним оставшиеся - они должны быть равны
        self.admin_permissions.remove(global_permissions.manage_licenses)
        self.admin_permissions.remove(global_permissions.manage_tracker)
        self.admin_permissions.remove(global_permissions.can_pay)

        # внешний админ может удалять организацию, если у неё нет биллинговой информации
        self.outer_permissions.remove(organization_permissions.add_organization)
        # внешний админ может выйти из организации
        self.outer_permissions.remove(global_permissions.leave_organization)

        assert_that(len(self.admin_permissions), greater_than(0))
        assert_that(self.admin_permissions, contains_inanyorder(*self.outer_permissions))

    def test_admin_permissions_should_include_manage_licenses(self):
        assert_that(self.admin_permissions, has_item(global_permissions.manage_licenses))

    def test_admin_permissions_should_include_can_pay(self):
        assert_that(self.admin_permissions, has_item(global_permissions.can_pay))


class TestAdminPermissions(TestCase):
    def test_get_admin_permission__success(self):
        # если тимный блэкбокс вернул email по uid,
        # и если этот email есть в списке ролей из IDM
        # значит, у пользователя есть соответсвюущая роль
        with patch('intranet.yandex_directory.src.yandex_directory.core.permission.internal_permissions.get_internal_roles_by_uid') as get_roles:
            get_roles.return_value = ['support']
            result = get_admin_permissions(1)

        assert_that(
            result,
            contains_inanyorder(*all_internal_support_permissions)
        )

    def test_get_assessor_permissions__successs(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.permission.internal_permissions.get_internal_roles_by_uid') as get_roles:
            get_roles.return_value = ['assessor']
            result = get_admin_permissions(1)

        assert_that(
            result,
            contains_inanyorder(*all_internal_assessor_permissions)
        )

    def test_get_admin_permission_roles_not_found(self):
        # если ролей для пользователя не нашлось,
        # значит, у пользователя нет прав в админке
        with patch('intranet.yandex_directory.src.yandex_directory.core.permission.internal_permissions.get_internal_roles_by_uid') as get_roles:
            get_roles.return_value = []
            result = get_admin_permissions(1)

        assert_that(
            result,
            equal_to([]),
        )

    def test_get_admin_permissions__successs(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.permission.internal_permissions.get_internal_roles_by_uid') as get_roles:
            get_roles.return_value = ['admin']
            result = get_admin_permissions(1)

        assert_that(
            result,
            contains_inanyorder(*all_internal_admin_permissions)
        )

    def test_get_bizdev_permissions__successs(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.permission.internal_permissions.get_internal_roles_by_uid') as get_roles:
            get_roles.return_value = ['bizdev']
            result = get_admin_permissions(1)

        assert_that(
            result,
            contains_inanyorder(*all_internal_bizdev_permissions)
        )

    def test_get_permissions_for_multi_roles(self):
        # если ролей несколько, то права должны быть объединением множеств
        with patch('intranet.yandex_directory.src.yandex_directory.core.permission.internal_permissions.get_internal_roles_by_uid') as get_roles:
            get_roles.return_value = ['support', 'bizdev']
            result = get_admin_permissions(1)

        expected_roles = list(set(all_internal_bizdev_permissions + all_internal_support_permissions))

        assert_that(
            result,
            contains_inanyorder(*expected_roles)
        )
