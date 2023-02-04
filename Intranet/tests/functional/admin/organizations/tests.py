# -*- coding: utf-8 -*-
import datetime
import responses

from .... import webmaster_responses

from dateutil.relativedelta import relativedelta
from hamcrest import (
    assert_that,
    equal_to,
    contains_inanyorder,
    has_entries,
    contains,
    not_,
    has_key,
    has_item,
    has_items,
    empty,
)
from unittest.mock import (
    patch,
    ANY,
    Mock,
)
from xmlrpc.client import Fault

from testutils import (
    TestCase,
    create_outer_admin,
    create_organization,
    tvm2_auth_success,
    override_settings,
    fake_userinfo, mocked_blackbox,
)
from intranet.yandex_directory.src.yandex_directory.admin.views.organizations import (
    _prepare_organization,
    _prepare_action,
    _prepare_domain,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    format_date,
    utcnow,
    Ignore,
)
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
    UserMetaModel,
    OrganizationModel,
    OrganizationMetaModel,
    GroupModel,
    DepartmentModel,
    ResourceRelationModel,
    OrganizationServiceModel,
    ServiceModel,
    UserServiceLicenses,
    PromocodeModel,
    OrganizationPromocodeModel,
    OrganizationBillingConsumedInfoModel,
    ActionModel,
    OrganizationBillingInfoModel,
    DomainModel,
    TaskModel,
    SupportActionMetaModel,
    MaillistCheckModel,
    OrganizationRevisionCounterModel,
    EventModel,
    ResourceModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.organization import (
    organization_type,
    vip_reason,
)
from intranet.yandex_directory.src.yandex_directory.core.models.group import relation_name
from intranet.yandex_directory.src.yandex_directory.core.models.action import SupportActions
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
    disable_service,
    trial_status)
from intranet.yandex_directory.src.yandex_directory.core.permission import assessor_internal_pemissions
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    create_user,
    prepare_user,
)
from intranet.yandex_directory.src.yandex_directory.auth.scopes import scope
from intranet.yandex_directory.src.yandex_directory.core.actions import action
from intranet.yandex_directory.src.yandex_directory.core.utils.tasks import ChangeOrganizationOwnerTask
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.features import (
    DOMAIN_AUTO_HANDOVER,
    CHANGE_ORGANIZATION_OWNER,
    USE_DOMENATOR,
)
from intranet.yandex_directory.src.yandex_directory.core.features.utils import (
    is_feature_enabled,
    set_feature_value_for_organization,
)
from intranet.yandex_directory.src.yandex_directory.auth import tvm
from intranet.yandex_directory.src.yandex_directory.connect_services.domenator import setup_domenator_client

TVM2_HEADERS = {'X-Ya-Service-Ticket': 'qqq'}


class TestAdminOrganizationsListView(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationsListView, self).setUp(*args, **kwargs)

        self.organization_1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        self.organization_2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_should_return_list_of_organizations(self):
        # проверим, что ручка отдает список организаций
        response = self.get_json(
            '/admin/organizations/',
            headers=TVM2_HEADERS,
            process_tasks=False,
        )

        organizations = OrganizationModel(self.main_connection).find()
        for o in organizations:
            o['shard'] = 1
        organizations = [_prepare_organization(o, shard=1) for o in organizations]

        assert_that(
            response['result'],
            contains_inanyorder(
                *organizations
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_filter_owner_id(self):
        # проверим, что работает фильтр по id владельца организации
        response = self.get_json(
            '/admin/organizations/?owner_id=%s' % self.organization_1['admin_uid'],
            headers=TVM2_HEADERS,
        )

        assert_that(
            response['result'],
            contains(
                has_entries(
                    id=self.organization_1['id'],
                )
            )
        )


class TestAdminOrganizationsSearchListView(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationsSearchListView, self).setUp(*args, **kwargs)

        self.organization_1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        self.organization_2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_should_return_list_of_organizations(self):
        # проверим, что ручка отдает список организаций
        terms = [
            self.organization['label'],
            self.organization_domain,
            'яндекс',
        ]
        for term in terms:
            response = self.get_json(
                '/admin/organizations/?text=%s' % term,
                TVM2_HEADERS,
            )

            organizations = OrganizationModel(self.main_connection).find({'text': term, 'type': 'organization'})
            organizations = [_prepare_organization(o, shard=1) for o in organizations]

            assert_that(
                response['result'],
                contains_inanyorder(
                    *organizations
                )
            )

        terms = [
            self.organization_info['admin_user']['nickname'],
            self.organization_info['admin_user']['name']['first']['ru'],
            self.organization_info['admin_user']['name']['last']['ru'],
        ]
        for term in terms:
            response = self.get_json(
                '/admin/organizations/?text=%s&type=internal_admin' % term,
                TVM2_HEADERS,
            )

            organizations = OrganizationModel(self.main_connection).find({'text': term, 'type': 'internal_admin'})
            organizations = [_prepare_organization(o, shard=1) for o in organizations]

            assert_that(
                response['result'],
                contains_inanyorder(
                    *organizations
                )
            )

        outer_admin = self.create_user(org_id=self.organization_1['id'], nickname='outer_admin', is_outer=True)
        patcher_bb_uid = patch('intranet.yandex_directory.src.yandex_directory.core.models.organization.get_user_id_from_passport_by_login')
        mock_bb_uid = patcher_bb_uid.start()
        mock_bb_uid.return_value = outer_admin['id']

        response = self.get_json(
            '/admin/organizations/?text=outer_admin&type=outer_admin',
            TVM2_HEADERS,
        )

        organizations = OrganizationModel(self.main_connection).find(
            {
                'text': 'outer_admin',
                'type': 'outer_admin'
            }
        )
        organizations = [_prepare_organization(o, shard=1) for o in organizations]

        assert_that(
            response['result'],
            contains_inanyorder(
                *organizations
            )
        )

        # если ББ ничего не знает про внешнего админа
        mock_bb_uid.return_value = None
        response = self.get_json(
            '/admin/organizations/?text=outer_admin&type=outer_admin',
            TVM2_HEADERS,
        )
        assert_that(
            response['result'],
            equal_to([]),
        )


class TestAdminOrganizationDetailView(TestCase):
    enable_admin_api = True

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_unknown_organization(self):
        # для несуществующих организаций должно быть 404
        self.get_json(
            '/admin/organizations/273569/',
            expected_code=404,
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_cloud_org_id(self):
        OrganizationMetaModel(self.meta_connection).update(
            update_data={'cloud_org_id': 'wow-1'},
            filter_data={'id': self.organization['id']},
        )
        response = self.get_json(
            f'/admin/organizations/{self.organization["id"]}/',
        )
        assert response['cloud_org_id'] == 'wow-1'

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_users_count(self):
        # проверим, что в подсчет пользователей не входят роботы
        # проверим, что роботы считаются правильно

        UserModel(self.main_connection).create(
            id=self.user['id'] + 1,
            nickname='robot',
            name=self.name,
            email='robot',
            gender='female',
            org_id=self.organization['id'],
            user_type='robot',
        )
        response = self.get_json(
            '/admin/organizations/{}/'.format(
                self.organization['id']
            )
        )
        assert response['users_count'] == 1
        assert response['robots_count'] == 1

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_billing_error(self):
        # проверим, что вернем None дла баланса, если биллинг вернул ошибку
        self.enable_paid_mode()

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.side_effect = Fault(-1, 'error')
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json(
                '/admin/organizations/{}/'.format(
                    self.organization['id']
                )
            )
        assert response['billing']['balance'] is None
        assert response['billing']['first_debt_act_date'] is None
        assert response['billing']['client_id'] is not None
        assert response['billing']['person_id'] is not None


class TestAdminOrganizationAdminsListView(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationAdminsListView, self).setUp(*args, **kwargs)
        self.outer_uid, self.orgs, revision = create_outer_admin(
            self.meta_connection,
            self.main_connection,
            num_organizations=1
        )

        self.all_types_admins_org_id = self.orgs[0]

        # Создадим 2х внутренний админов вместе с существующим внешним
        user_data = {
            'name': {
                'first': {'ru': 'admin1'},
                'last': {'ru': ''},
            },
            'gender': 'male',
            'department_id': 1,
        }
        uid1 = int(self.get_next_uid())
        user_data['id'] = uid1
        create_user(
            self.meta_connection,
            self.main_connection,
            self.all_types_admins_org_id,
            user_data,
            nickname='admin1',
            password='051292'
        )

        uid2 = int(self.get_next_uid())
        user_data['id'] = uid2
        create_user(
            self.meta_connection,
            self.main_connection,
            self.all_types_admins_org_id,
            user_data,
            nickname='admin2',
            password='051292'
        )

        user_m = UserModel(self.main_connection)
        user_m.make_admin_of_organization(
            org_id=self.all_types_admins_org_id,
            user_id=uid1,
        )
        user_m.make_admin_of_organization(
            org_id=self.all_types_admins_org_id,
            user_id=uid2,
        )

        # создадим организацию без внешнего админа
        self.inner_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='inner-org'
        )['organization']
        self.inner_organization_id = self.inner_organization['id']

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_should_return_list_of_admins(self):
        # проверим, что ручка отдает список внутренних и внешних админов

        outer_user = UserMetaModel(self.meta_connection).find(
            filter_data=dict(
                org_id=self.all_types_admins_org_id,
                user_type='outer_admin',
            )
        )[0]
        del outer_user['created']

        users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.all_types_admins_org_id
            }
        )

        users = [
            prepare_user(
                self.main_connection,
                u,
                expand_contacts=True,
                api_version=1
            )
            for u in users
        ] + [outer_user]
        print(users)
        with patch('intranet.yandex_directory.src.yandex_directory.admin.views.organizations.get_user_data_from_blackbox_by_uid') as mock_account:
            mock_account.return_value = outer_user
            response = self.get_json(
                '/admin/organizations/%d/admins/' % self.all_types_admins_org_id,
                TVM2_HEADERS,
            )

            assert_that(
                response,
                contains_inanyorder(
                    *users
                )
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_should_return_only_inner_admins(self):
        # проверим, что ручка отдает список только из внутренних админов
        response = self.get_json(
            '/admin/organizations/%d/admins/' % self.inner_organization_id,
            TVM2_HEADERS,
        )

        users = UserModel(self.main_connection).find(
            filter_data={
                'org_id': self.inner_organization_id
            }
        )
        users = [
            prepare_user(
                self.main_connection,
                u,
                expand_contacts=True,
                api_version=1
            )
            for u in users
        ]
        assert_that(
            response,
            contains_inanyorder(
                *users
            )
        )


class TestAdminOrganizationServicesView(TestCase):
    enable_admin_api = True

    def setUp(self):
        super(TestAdminOrganizationServicesView, self).setUp()

        OrganizationModel(self.main_connection).change_organization_type(
            self.organization['id'],
            organization_type.partner_organization,
            partner_id=self.partner['id'],
        )

        paid_service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='paid_service',
            name='Service',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        common_service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id2',
            slug='common_service',
            name='CommonService',
            robot_required=False,
        )
        another_paid_service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id3',
            slug='another_paid_service',
            name='AnotherService',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )

        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            paid_service['slug'],
        )

        OrganizationServiceModel(self.main_connection).change_responsible(
            service_id=paid_service['id'],
            org_id=self.organization['id'],
            responsible_id=self.user['id'],
        )

        ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            external_id='smth',
            service=paid_service['slug'],
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            common_service['slug'],
        )
        disable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            common_service['slug'],
            'by_user',
        )

        UserServiceLicenses(self.main_connection).create(
            user_id=self.user['id'],
            org_id=self.organization['id'],
            service_id=paid_service['id'],
        )

        self.exp_paid_service = {
            'enabled': True,
            'ready': True,
            'ready_at': ANY,
            'trial_expires': format_date(
                (utcnow() + relativedelta(months=1)).date(), allow_none=True),
            'trial_expired': False,
            'disable_reason': None,
            'enabled_at': ANY,
            'disabled_at': None,
            'last_mail_sent_at': None,
            'user_count': 1,
            'user_limit': None,
            'expires_at': None,
            'responsible_id': self.user['id'],
            'resources_count': 1,
        }
        self.exp_common_service = {
            'enabled': False,
            'ready': True,
            'ready_at': ANY,
            'trial_expires': None,
            'trial_expired': None,
            'disable_reason': 'by_user',
            'enabled_at': ANY,
            'disabled_at': ANY,
            'last_mail_sent_at': None,
            'user_count': None,
            'user_limit': None,
            'expires_at': None,
            'responsible_id': None,
            'resources_count': 0,
        }
        self.exp_another_paid_service = {
            'enabled': True,
            'ready': True,
            'ready_at': ANY,
            'trial_expires': None,
            'trial_expired': None,
            'disable_reason': None,
            'enabled_at': ANY,
            'disabled_at': None,
            'last_mail_sent_at': None,
            'user_count': 0,
            'user_limit': None,
            'expires_at': None,
        }

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_services_list(self):
        response = self.get_json(
            '/admin/organizations/%d/services/' % self.organization['id'],
            TVM2_HEADERS,
        )

        assert_that(
            response,
            has_entries(
                paid_service=has_entries(**self.exp_paid_service),
                common_service=has_entries(**self.exp_common_service),
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_service_details_paid(self):
        response = self.get_json(
            '/admin/organizations/%d/services/paid_service/' % self.organization['id'],
            TVM2_HEADERS,
        )

        assert_that(
            response,
            has_entries(**self.exp_paid_service),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_service_details_common(self):
        response = self.get_json(
            '/admin/organizations/%d/services/common_service/' % self.organization['id'],
            TVM2_HEADERS,
        )

        assert_that(
            response,
            has_entries(**self.exp_common_service),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_enable_service_limits(self):
        expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365)
        update_data = {
            'user_limit': 1000,
            'expires_at': format_date(expires_at.date(), only_date=True),
            'comment': 'lorem ipsum dolorem',
        }

        response = self.post_json(
            '/admin/organizations/%d/services/paid_service/enable/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
        )

        exp_paid_service = self.exp_paid_service.copy()
        exp_paid_service.update(update_data)
        exp_paid_service.pop('comment')
        exp_paid_service['trial_expires'] = format_date(
            date=(utcnow() - relativedelta(days=1)).date(),
            only_date=True,
        )
        exp_paid_service['trial_expired'] = True

        assert_that(
            response,
            has_entries(**exp_paid_service),
        )

        service = OrganizationServiceModel(self.main_connection) \
            .get_by_slug(self.organization['id'], 'paid_service', fields=['**'])

        assert_that(
            service,
            has_entries(
                enabled=True,
                trial_status=trial_status.expired,
                user_limit=1000,
                expires_at=expires_at.date(),
            ),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_enable_non_created_service_limits(self):
        expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365)
        update_data = {
            'user_limit': 1000,
            'expires_at': format_date(expires_at.date(), only_date=True),
            'comment': 'lorem ipsum dolorem',
        }

        response = self.post_json(
            '/admin/organizations/%d/services/another_paid_service/enable/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
        )

        exp_another_paid_service = self.exp_another_paid_service.copy()
        exp_another_paid_service.update(update_data)
        exp_another_paid_service.pop('comment')
        exp_another_paid_service['trial_expires'] = format_date(
            date=(utcnow() - relativedelta(days=1)).date(),
            only_date=True,
        )
        exp_another_paid_service['trial_expired'] = True

        assert_that(
            response,
            has_entries(**exp_another_paid_service),
        )

        service = OrganizationServiceModel(self.main_connection) \
            .get_by_slug(self.organization['id'], 'another_paid_service', fields=['**'])

        assert_that(
            service,
            has_entries(
                enabled=True,
                trial_status=trial_status.expired,
                user_limit=1000,
                expires_at=expires_at.date(),
            ),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_enable_service_without_user_limit_partner(self):
        expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365)
        update_data = {
            'expires_at': format_date(expires_at.date(), only_date=True),
            'comment': 'test without user limit',
        }

        self.post_json(
            '/admin/organizations/%d/services/another_paid_service/enable/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
            expected_code=422,
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_enable_service_without_user_limit_not_partner(self):
        OrganizationModel(self.main_connection).change_organization_type(
            org_id=self.organization['id'],
            new_org_type=organization_type.common,
            partner_id=self.partner['id'],
        )
        update_data = {
            'comment': 'test without user limit',
        }

        response = self.post_json(
            '/admin/organizations/%d/services/another_paid_service/enable/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
        )

        exp_another_paid_service = self.exp_another_paid_service.copy()
        exp_another_paid_service.update(update_data)
        exp_another_paid_service.pop('comment')
        exp_another_paid_service['trial_expires'] = format_date(
            date=(utcnow() - relativedelta(days=1)).date(),
            only_date=True,
        )
        exp_another_paid_service['trial_expired'] = True

        assert_that(
            response,
            has_entries(**exp_another_paid_service),
        )

        service = OrganizationServiceModel(self.main_connection) \
            .get_by_slug(self.organization['id'], 'another_paid_service', fields=['**'])

        assert_that(
            service,
            has_entries(
                enabled=True,
                trial_status=trial_status.expired,
            ),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_disable_service_limits_not_partner(self):
        self.test_enable_service_limits()

        OrganizationModel(self.main_connection).change_organization_type(
            org_id=self.organization['id'],
            new_org_type=organization_type.common,
            partner_id=self.partner['id'],
        )

        update_data = {
            'comment': 'lorem ipsum dolorem',
        }

        response = self.post_json(
            '/admin/organizations/%d/services/paid_service/disable/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
        )

        exp_paid_service = self.exp_paid_service.copy()
        exp_paid_service.update(update_data)
        exp_paid_service.pop('comment')
        exp_paid_service['user_limit'] = None
        exp_paid_service['expires_at'] = None
        exp_paid_service['trial_expires'] = format_date(
            date=(utcnow() - relativedelta(days=1)).date(),
            only_date=True,
        )
        exp_paid_service['trial_expired'] = True

        assert_that(
            response,
            has_entries(**exp_paid_service),
        )

        service = OrganizationServiceModel(self.main_connection) \
            .get_by_slug(self.organization['id'], 'paid_service', fields=['**'])

        assert_that(
            service,
            has_entries(
                enabled=False,
                trial_status=trial_status.expired,
                user_limit=None,
                expires_at=None,
            ),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_disable_service_limits(self):
        self.test_enable_service_limits()

        update_data = {
            'comment': 'lorem ipsum dolorem',
        }

        response = self.post_json(
            '/admin/organizations/%d/services/paid_service/disable/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
        )

        exp_paid_service = self.exp_paid_service.copy()
        exp_paid_service.update(update_data)
        exp_paid_service.pop('comment')
        exp_paid_service['user_limit'] = None
        exp_paid_service['expires_at'] = None
        exp_paid_service['trial_expires'] = format_date(
            date=(utcnow() - relativedelta(days=1)).date(),
            only_date=True,
        )
        exp_paid_service['trial_expired'] = True

        assert_that(
            response,
            has_entries(**exp_paid_service),
        )

        service = OrganizationServiceModel(self.main_connection) \
            .get_by_slug(self.organization['id'], 'paid_service', fields=['**'])

        assert_that(
            service,
            has_entries(
                enabled=False,
                trial_status=trial_status.expired,
                user_limit=None,
                expires_at=None,
            ),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_restrict_enable_common_service_limits(self):
        expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365)
        update_data = {
            'user_limit': 1000,
            'expires_at': format_date(expires_at.date(), only_date=True),
            'comment': 'lorem ipsum dolorem',
        }

        self.post_json(
            '/admin/organizations/%d/services/common_service/enable/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
            expected_code=422,
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_service_limits(self):
        self.test_enable_service_limits()

        expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365)
        update_data = {
            'user_limit': 1000,
            'expires_at': format_date(expires_at.date(), only_date=True),
            'comment': 'lorem ipsum dolorem',
        }

        response = self.patch_json(
            '/admin/organizations/%d/services/paid_service/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
        )

        exp_paid_service = self.exp_paid_service.copy()
        exp_paid_service.update(update_data)
        exp_paid_service.pop('comment')
        exp_paid_service['trial_expires'] = format_date(
            date=(utcnow() - relativedelta(days=1)).date(),
            only_date=True,
        )
        exp_paid_service['trial_expired'] = True

        assert_that(
            response,
            has_entries(**exp_paid_service),
        )

        service = OrganizationServiceModel(self.main_connection) \
            .get_by_slug(self.organization['id'], 'paid_service', fields=['**'])

        assert_that(
            service,
            has_entries(
                enabled=True,
                trial_status=trial_status.expired,
                user_limit=1000,
                expires_at=expires_at.date(),
            ),
        )


    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_restrict_change_common_service_limits(self):
        expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365)
        update_data = {
            'user_limit': 1000,
            'expires_at': format_date(expires_at.date(), only_date=True),
            'comment': 'lorem ipsum dolorem',
        }

        self.patch_json(
            '/admin/organizations/%d/services/common_service/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
            expected_code=422,
        )


class TestAdminOrganizationChangeSubscriptionPlan(TestCase):
    enable_admin_api = True

    def setUp(self):
        super(TestAdminOrganizationChangeSubscriptionPlan, self).setUp()
        OrganizationModel(self.main_connection).change_organization_type(
            org_id=self.organization['id'],
            new_org_type=organization_type.partner_organization,
            partner_id=self.partner['id'],
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_plan_to_paid(self):
        expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365)
        update_data = {
            'subscription_plan': 'paid',
            'subscription_plan_expires_at': format_date(expires_at.date(), only_date=True),
            'comment': 'lorem ipsum dolorem',
        }

        response = self.patch_json(
            '/admin/organizations/%d/subscription_plan/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
        )

        organization = update_data.copy()
        organization.pop('comment')

        assert_that(
            response,
            has_entries(**organization),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_plan_expiration_date(self):
        expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365)
        OrganizationModel(self.main_connection).enable_paid_mode_for_partner_organization(
            org_id=self.organization['id'],
            author_id=100700,
            expires_at=expires_at.date(),
        )

        new_expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365*2)
        update_data = {
            'subscription_plan_expires_at': format_date(new_expires_at.date(), only_date=True),
            'comment': 'lorem ipsum dolorem',
        }

        response = self.patch_json(
            '/admin/organizations/%d/subscription_plan/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
        )

        organization = update_data.copy()
        organization.pop('comment')
        organization['subscription_plan'] = 'paid'

        assert_that(
            response,
            has_entries(**organization),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_plan_to_free(self):
        expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365)
        OrganizationModel(self.main_connection).enable_paid_mode_for_partner_organization(
            org_id=self.organization['id'],
            author_id=100700,
            expires_at=expires_at.date(),
        )

        update_data = {
            'subscription_plan': 'free',
            'comment': 'lorem ipsum dolorem',
        }

        response = self.patch_json(
            '/admin/organizations/%d/subscription_plan/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
        )

        organization = update_data.copy()
        organization.pop('comment')

        assert_that(
            response,
            has_entries(**organization),
        )


class TestAdminOrganizationChangeSubscriptionPlanFail(TestCase):
    enable_admin_api = True

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_plan_to_paid(self):
        expires_at = datetime.datetime.utcnow() + datetime.timedelta(days=365)
        update_data = {
            'subscription_plan': 'paid',
            'subscription_plan_expires_at': format_date(expires_at.date(), only_date=True),
            'comment': 'lorem ipsum dolorem',
        }

        self.patch_json(
            '/admin/organizations/%d/subscription_plan/' % self.organization['id'],
            update_data,
            TVM2_HEADERS,
            expected_code=422,
        )


class TestAdminOrganizationChangeType(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationChangeType, self).setUp(*args, **kwargs)
        edu_promocode = PromocodeModel(self.meta_connection).create(
            id='edu_org_promocode',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 'free',
                    2: 'free',
                    3: 'free',
                },
            },
        )
        yndx_org_promocode = PromocodeModel(self.meta_connection).create(
            id='yndx_org_promocode',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 'free',
                    2: 'free',
                    3: 'free',
                },
            },
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_org_type_to_education(self):
        # проверяем, что применяется промокод и появляется биллинговая информация
        common_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='common_org'
        )['organization']
        assert_that(
            common_org['organization_type'],
            equal_to('common')
        )

        common_org_with_billing_info = create_organization(
            self.meta_connection,
            self.main_connection,
            label='common_org_with_billing'
        )['organization']
        self.enable_paid_mode(common_org_with_billing_info['id'])

        response = self.patch_json(
            '/admin/organizations/%d/type/' % common_org['id'],
            headers=TVM2_HEADERS,
            data={
                'org_type': 'education', 'client_id': 123,
                'comment': 'test', 'person_id': 1234,
            },
        )
        assert_that(
            response,
            has_entries(
                organization_type='education'
            )
        )
        assert_that(
            OrganizationPromocodeModel(self.main_connection).filter(org_id=common_org['id']).one(),
            has_entries(
                promocode_id='edu_org_promocode',
                active=True
            )
        )

        # проверим, что появилась биллинговая информация
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).filter(org_id=common_org['id']).one(),
            has_entries(
                client_id=123,
                contract_type='offer',
                person_type='legal',
            ),
        )

        # проверим, что создалось событие
        assert_that(
            ActionModel(self.main_connection).filter(org_id=common_org['id'], name='organization_type_change').one(),
            has_entries(
                author_id=100700,
                object=has_entries(
                    organization_type='education',
                    id=common_org['id'],
                ),
            )
        )

        # записали запись в лог
        assert_that(
            SupportActionMetaModel(self.meta_connection).all(),
            has_item(
                has_entries(
                    org_id=common_org['id'],
                    name=SupportActions.change_organization_type,
                    comment='test'
                )
            )

        )
        # проверим, что обновляется client_id, если биллинговая информация уже была
        response = self.patch_json(
            '/admin/organizations/%d/type/' % common_org_with_billing_info['id'],
            headers=TVM2_HEADERS,
            data={'org_type': 'education', 'client_id': 111, 'comment': 'test', 'person_id': 344},
        )
        assert_that(
            response,
            has_entries(
                organization_type='education'
            )
        )

        # проверим, что обновилась биллинговая информация
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).filter(org_id=common_org_with_billing_info['id']).one(),
            has_entries(
                client_id=111,
                contract_type='offer',
                person_type='natural',
            ),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_org_type_to_charity(self):
        # проверяем, что применяется промокод и появляется биллинговая информация
        common_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='common_org'
        )['organization']
        assert_that(
            common_org['organization_type'],
            equal_to('common')
        )

        common_org_with_billing_info = create_organization(
            self.meta_connection,
            self.main_connection,
            label='common_org_with_billing'
        )['organization']
        self.enable_paid_mode(common_org_with_billing_info['id'])

        response = self.patch_json(
            '/admin/organizations/%d/type/' % common_org['id'],
            headers=TVM2_HEADERS,
            data={
                'org_type': 'charity', 'client_id': 123,
                'comment': 'test', 'person_id': 1234,
            },
        )

        assert_that(
            response,
            has_entries(
                organization_type='charity'
            )
        )
        assert_that(
            OrganizationPromocodeModel(self.main_connection).filter(org_id=common_org['id']).one(),
            has_entries(
                promocode_id='edu_org_promocode',
                active=True
            )
        )

        # проверим, что появилась биллинговая информация
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).filter(org_id=common_org['id']).one(),
            has_entries(
                client_id=123,
                contract_type='offer',
                person_type='legal',
            ),
        )

        # проверим, что создалось событие
        assert_that(
            ActionModel(self.main_connection).filter(org_id=common_org['id'], name='organization_type_change').one(),
            has_entries(
                author_id=100700,
                object=has_entries(
                    organization_type='charity',
                    id=common_org['id'],
                ),
            )
        )

        # записали запись в лог
        assert_that(
            SupportActionMetaModel(self.meta_connection).all(),
            has_item(
                has_entries(
                    org_id=common_org['id'],
                    name=SupportActions.change_organization_type,
                    comment='test'
                )
            )

        )
        # проверим, что обновляется client_id, если биллинговая информация уже была
        response = self.patch_json(
            '/admin/organizations/%d/type/' % common_org_with_billing_info['id'],
            headers=TVM2_HEADERS,
            data={'org_type': 'charity', 'client_id': 111, 'comment': 'test', 'person_id': 344},
        )
        assert_that(
            response,
            has_entries(
                organization_type='charity'
            )
        )

        # проверим, что обновилась биллинговая информация
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).filter(org_id=common_org_with_billing_info['id']).one(),
            has_entries(
                client_id=111,
                contract_type='offer',
                person_type='natural',
            ),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_org_type_to_common(self):
        # проверяем, что образовательный промокод должен деактивироваться
        education_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='edu_org'
        )['organization']
        # сделаем организацию образовательной
        OrganizationModel(self.main_connection).change_organization_type(education_org['id'], 'education')
        assert_that(
            OrganizationModel(self.main_connection).get(education_org['id'])['organization_type'],
            equal_to('education')
        )
        assert_that(
            OrganizationPromocodeModel(self.main_connection).find({'org_id': education_org['id']}, one=True),
            has_entries(
                promocode_id='edu_org_promocode'
            )
        )

        response = self.patch_json(
            '/admin/organizations/%d/type/' % education_org['id'],
            headers=TVM2_HEADERS,
            data={'org_type': 'common', 'comment': 'test'},
        )
        assert_that(
            response,
            has_entries(
                organization_type='common'
            )
        )
        assert_that(
            OrganizationPromocodeModel(self.main_connection).find({'org_id': education_org['id']}, one=True),
            has_entries(
                promocode_id='edu_org_promocode',
                active=False
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_org_type_to_yandex_project(self):
        # проверяем, что применяется промокод
        common_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='common_org'
        )['organization']
        assert_that(
            common_org['organization_type'],
            equal_to('common')
        )
        response = self.patch_json(
            '/admin/organizations/%d/type/' % common_org['id'],
            headers=TVM2_HEADERS,
            data={'org_type': 'yandex_project', 'comment': 'test'},
        )
        assert_that(
            response,
            has_entries(
                organization_type='yandex_project'
            )
        )
        assert_that(
            OrganizationPromocodeModel(self.main_connection).find({'org_id': common_org['id']}, one=True),
            has_entries(
                promocode_id='yndx_org_promocode',
                active=True
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_org_type_to_test(self):
        # проверяем, что поменялся тип организации
        common_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='common_org'
        )['organization']
        assert_that(
            common_org['organization_type'],
            equal_to('common')
        )
        response = self.patch_json(
            '/admin/organizations/%d/type/' % common_org['id'],
            headers=TVM2_HEADERS,
            data={'org_type': 'test', 'comment': 'test'},
        )
        assert_that(
            response,
            has_entries(
                organization_type='test'
            )
        )
        assert_that(
            OrganizationPromocodeModel(self.main_connection).find({'org_id': common_org['id']}),
            equal_to([]),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_org_type_to_partner_organization(self):
        # меняем тип организации на партнерскую

        common_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='common_org'
        )['organization']

        partner_id = self.partner['id']
        response = self.patch_json(
            '/admin/organizations/%d/type/' % common_org['id'],
            headers=TVM2_HEADERS,
            data={'org_type': 'partner_organization', 'partner_id': partner_id, 'comment': 'test'},
        )
        assert_that(
            response,
            has_entries(
                organization_type=organization_type.partner_organization
            )
        )

        # проверим, что создалось событие
        assert_that(
            ActionModel(self.main_connection).filter(org_id=common_org['id'], name='organization_type_change').one(),
            has_entries(
                author_id=100700,
                object=has_entries(
                    organization_type=organization_type.partner_organization,
                    id=common_org['id'],
                ),
            )
        )

        # записали запись в лог
        assert_that(
            SupportActionMetaModel(self.meta_connection).all(),
            has_item(
                has_entries(
                    org_id=common_org['id'],
                    name=SupportActions.change_organization_type,
                    comment='test'
                )
            )

        )
        assert_that(
            OrganizationModel(self.main_connection).get(common_org['id']),
            has_entries(
                partner_id=None,
                organization_type=organization_type.partner_organization,
            )
        )


class TestAdminOrganizationVerifyDomain(TestCase):
    enable_admin_api = True

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_verify_domain(self):
        self.clean_actions_and_events()

        domain_name = 'foo.com'

        # добавим неподтвержденный домен организации
        org_id = self.organization['id']
        DomainModel(self.main_connection).create(domain_name, org_id, owned=False)

        # добавим организацию, где данный домен подтвержден
        other_org = OrganizationModel(self.main_connection).create(org_id + 1, '', '', self.get_next_uid(), '')
        DomainModel(self.main_connection).create(domain_name, other_org['id'], owned=True)

        self.post_json(
            '/admin/organizations/%s/domains/%s/verify/' % (org_id, domain_name),
            data={'comment': 'test'},
        )

        # проверим, что домен первой организации стал подтвержденным
        domain = DomainModel(self.main_connection).get(domain_name, org_id)
        assert domain['owned'] is True

        # проверим, что домен второй организации стал неподтвержденным
        domain = DomainModel(self.main_connection).get(domain_name, other_org['id'])
        assert domain['owned'] is False

        # записали запись в лог
        assert_that(
            SupportActionMetaModel(self.meta_connection).all(),
            has_item(
                has_entries(
                    org_id=org_id,
                    name=SupportActions.verify_domain,
                    comment='test'
                )
            )

        )

        assert_that(
            ActionModel(self.main_connection).find({'org_id': org_id}, one=True),
            has_entries(
                object=has_entries(
                    name=domain_name,
                ),
                name='domain_occupy'
            ),
        )
        assert_that(
            ActionModel(self.main_connection).find({'org_id': other_org['id']}, one=True),
            has_entries(
                object=has_entries(
                    name=domain_name,
                ),
                name='domain_alienate'
            ),
        )


class TestAdminOrganizationTotalCount(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationTotalCount, self).setUp(*args, **kwargs)

        self.organization_1 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        self.organization_2 = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']

        for _ in range(10):
            self.create_user(org_id=self.organization_1['id'])

        for _ in range(5):
            self.create_user(org_id=self.organization_2['id'])

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_get_counts(self):
        response = self.get_json(
            '/admin/organizations/total_count/',
            TVM2_HEADERS,
        )

        organizations_total_count = OrganizationModel(self.main_connection).count()
        users_total_count = UserModel(self.main_connection).count(filter_data={'is_robot': False})

        assert_that(
            response,
            has_entries(
                organizations_total_count=organizations_total_count,
                users_total_count=users_total_count,
            )
        )


class TestAdminOrganizationMaillistsSync(TestCase):
    enable_admin_api = True

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_successful_sync(self):
        last_revision = OrganizationRevisionCounterModel(self.main_connection).get(self.organization['id'])['revision']
        with patch('intranet.yandex_directory.src.yandex_directory.admin.views.organizations.MaillistsCheckTask.delay') as delay_task:
            self.post_json(
                '/admin/organizations/%s/maillists/sync/' % (
                    self.organization['id'],
                ),
                data=None,
            )
            delay_task.assert_called_once_with(
                org_id=self.organization['id'],
            )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_get_last_status(self):
        check_data = {
            'revision': 10,
            'ml_is_ok': False,
            'problems': 'Some problems here',
        }
        MaillistCheckModel(self.main_connection).insert_or_update(
            org_id=self.organization['id'],
            data=check_data,
        )

        response = self.get_json(
            '/admin/organizations/%s/maillists/sync/' % (
                self.organization['id'],
            ),
        )

        assert_that(
            response,
            has_entries(
                org_id=self.organization['id'],
                updated_at=ANY,
                **check_data
            )
        )


class TestAdminOrganizationStaffList(TestCase):
    enable_admin_api = True

    def setUp(self):
        super(TestAdminOrganizationStaffList, self).setUp()

        # добавим дополнительную организацию с группой и отделом
        # и дальше будем добавлять группы и отделы в обычную организациию с такими же id
        self.other_org_id = create_organization(
            self.meta_connection,
            self.main_connection,
            name={'ru': 'Орг 2'},
            label='org2',
            domain_part='.ws.autotest.yandex.ru',
        )['organization']['id']

        self.department_id = self.create_department(org_id=self.other_org_id)['id']
        admin = UserModel(self.main_connection) \
                .filter(org_id=self.other_org_id, nickname='admin') \
                .one()
        self.group_id = self.create_group(org_id=self.other_org_id, author_id=admin['id'])['id']

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_no_filters(self):
        # проверяем без фильтров, должны вернуться все контейнеры + поиск по текстовым полям
        group = GroupModel(self.main_connection).create(
            id=self.group_id,
            name={'ru': 'Группа 1', 'en': 'Group 1'},
            org_id=self.organization['id'],
            label='group dep',
        )
        DepartmentModel(self.main_connection) \
            .filter(id=self.user['department_id']) \
            .update(label='department')
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_data_from_blackbox_by_uids') as mock_user_data:
            mock_user_data.return_value = {
                self.user['id']: {
                    'aliases': [],
                    'birth_date': '2000-01-01',
                    'default_email': 'only_passport_user@khunafin.xyz',
                    'first_name': 'user',
                    'is_maillist': False,
                    'language': 'ru',
                    'last_name': 'user',
                    'login': 'only_passport_user@khunafin.xyz',
                    'sex': '1',
                    'avatar_id': '123445',
                    'uid': self.user['id'],
                },
            }
            response = self.get_json(
                '/admin/organizations/%s/staff/' % (
                    self.organization['id'],
                ),
            )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=self.user['id'],
                        org_id=self.organization['id'],
                        avatar_id='123445',
                    )
                ),
                has_entries(
                    type='department',
                    object=has_entries(
                        id=self.user['department_id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=group['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

        response = self.get_json(
            '/admin/organizations/%s/staff/?text=%s' % (
                self.organization['id'],
                'dep',
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='department',
                    object=has_entries(
                        id=self.user['department_id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=group['id'],
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_type_user(self):
        # проверяем фильтр по пользователям с указанной ролью (по разным ролям)
        user = self.create_user(org_id=self.organization['id'])
        deputy_admin = self.create_deputy_admin(org_id=self.organization['id'], is_outer=False)
        outer_deputy_admin = self.create_deputy_admin(org_id=self.organization['id'], is_outer=True)
        robot = UserModel(self.main_connection).create(
            id=user['id'] + 1,
            nickname='znick',
            name={
                'first': {
                    'ru': 'Пользователь',
                    'en': ''
                },
                'last': {
                    'ru': 'Автотестовый',
                    'en': ''
                },
            },
            email='email',
            gender='male',
            org_id=self.organization['id'],
            user_type='robot',
        )
        yamb_bot = UserModel(self.main_connection).create(
            id=robot['id'] + 1,
            nickname='zznick',
            name={
                'first': {
                    'ru': 'Пользователь',
                    'en': ''
                },
                'last': {
                    'ru': 'Автотестовый',
                    'en': ''
                },
            },
            email='email',
            gender='male',
            org_id=self.organization['id'],
            user_type='yamb_bot',
        )
        # добавим админа в еще одну группу
        self.create_group(members=[{'type': 'user', 'id': self.user['id']}])

        response = self.get_json(
            '/admin/organizations/%s/staff/?type=user' % (
                self.organization['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=self.user['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='user',
                    object=has_entries(
                        id=deputy_admin['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='user',
                    object=has_entries(
                        id=user['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='user',
                    object=has_entries(
                        id=robot['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='user',
                    object=has_entries(
                        id=yamb_bot['id'],
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

        response = self.get_json(
            '/admin/organizations/%s/staff/?role=user&type=user' % (
                self.organization['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=user['id'],
                        org_id=self.organization['id'],
                    )
                )
            )
        )

        with patch('intranet.yandex_directory.src.yandex_directory.admin.views.organizations.get_user_data_from_blackbox_by_uid') as mock_user_data:
            mock_user_data.return_value = {
                'uid': self.outer_admin['id'],
                'login': 'outer_admin',
                'last_name': 'admin',
                'first_name': 'admin',
                'is_maillist': False,
                'sex': '1',
                'karma': '10',
                'default_email': 'admin@yandex.ru',
                'language': 'ru',
                'birth_date': '2000-01-01',
                'aliases': [],
            }
            self.mocked_blackbox.batch_userinfo.side_effect = lambda **kwargs: [
                fake_userinfo(
                    uid=self.user['id'],
                    karma='20'
                ),
                fake_userinfo(
                    uid=self.outer_admin['id'],
                    karma='10'
                )
            ]

            response = self.get_json(
                '/admin/organizations/%s/staff/?role=admin&type=user' % (
                    self.organization['id'],
                ),
            )
            assert_that(
                response['result'],
                contains(
                    has_entries(
                        type='user',
                        object=has_entries(
                            id=self.user['id'],
                            org_id=self.organization['id'],
                            karma=20,
                        )
                    ),
                    has_entries(
                        type='user',
                        object=has_entries(
                            id=self.outer_admin['id'],
                            org_id=self.organization['id'],
                            karma=10,
                        )
                    ),
                )
            )

            # проверим текстовый фильтр и фильтр по id для внешних админов
            response = self.get_json(
                '/admin/organizations/%s/staff/?role=admin&type=user&text=%s' % (
                    self.organization['id'],
                    'outer_admin',
                ),
            )
            assert_that(
                response['result'],
                contains(
                    has_entries(
                        type='user',
                        object=has_entries(
                            id=self.outer_admin['id'],
                            org_id=self.organization['id'],
                        )
                    ),
                )
            )

            response = self.get_json(
                '/admin/organizations/%s/staff/?role=admin&type=user&id=%s' % (
                    self.organization['id'],
                    self.outer_admin['id'],
                ),
            )
            assert_that(
                response['result'],
                contains(
                    has_entries(
                        type='user',
                        object=has_entries(
                            id=self.outer_admin['id'],
                            org_id=self.organization['id'],
                        )
                    ),
                )
            )

        with patch('intranet.yandex_directory.src.yandex_directory.admin.views.organizations.get_user_data_from_blackbox_by_uid') as mock_user_data:
            mock_user_data.return_value = {
                'uid': outer_deputy_admin['id'],
                'login': 'outer',
                'last_name': 'outer',
                'first_name': 'outer',
            }

            response = self.get_json(
                '/admin/organizations/%s/staff/?role=deputy_admin&type=user' % (
                    self.organization['id'],
                ),
            )
            assert_that(
                response['result'],
                contains(
                    has_entries(
                        type='user',
                        object=has_entries(
                            id=deputy_admin['id'],
                            org_id=self.organization['id'],
                        )
                    ),
                    has_entries(
                        type='user',
                        object=has_entries(
                            id=outer_deputy_admin['id'],
                            org_id=self.organization['id'],
                        )
                    ),
                )
            )

            # проверим текстовый фильтр и фильтр по id для внешних заместителей админов
            response = self.get_json(
                '/admin/organizations/%s/staff/?role=deputy_admin&type=user&text=%s' % (
                    self.organization['id'],
                    'outer',
                ),
            )
            assert_that(
                response['result'],
                contains(
                    has_entries(
                        type='user',
                        object=has_entries(
                            id=outer_deputy_admin['id'],
                            org_id=self.organization['id'],
                        )
                    ),
                )
            )

            response = self.get_json(
                '/admin/organizations/%s/staff/?role=deputy_admin&type=user&id=%s' % (
                    self.organization['id'],
                    outer_deputy_admin['id'],
                ),
            )
            assert_that(
                response['result'],
                contains(
                    has_entries(
                        type='user',
                        object=has_entries(
                            id=outer_deputy_admin['id'],
                            org_id=self.organization['id'],
                        )
                    ),
                )
            )

        # протестируем случай когда нет внутренних админов, но есть внешние
        UserModel(self.main_connection).delete(
            filter_data={
                'org_id': self.organization['id'],
                'id': deputy_admin['id'],
            }
        )
        with patch('intranet.yandex_directory.src.yandex_directory.admin.views.organizations.get_user_data_from_blackbox_by_uid') as mock_user_data:
            mock_user_data.return_value = {
                'uid': outer_deputy_admin['id'],
                'login': 'outer_deputy_admin',
            }

            response = self.get_json(
                '/admin/organizations/%s/staff/?role=deputy_admin&type=user' % (
                    self.organization['id'],
                ),
            )
            assert_that(
                response['result'],
                contains(
                    has_entries(
                        type='user',
                        object=has_entries(
                            id=outer_deputy_admin['id'],
                            org_id=self.organization['id'],
                        )
                    ),
                )
            )

        response = self.get_json(
            '/admin/organizations/%s/staff/?role=robot&type=user' % (
                self.organization['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=robot['id'],
                        org_id=self.organization['id'],
                    )
                )
            )
        )

        response = self.get_json(
            '/admin/organizations/%s/staff/?role=yamb_bot&type=user' % (
                self.organization['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=yamb_bot['id'],
                        org_id=self.organization['id'],
                    )
                )
            )
        )

        # проверим дополнительный фильтр по текстовым полям
        response = self.get_json(
            '/admin/organizations/%s/staff/?role=user&type=user&text=%s' % (
                self.organization['id'],
                'user',
            ),
        )
        assert len(response['result']) == 0

        # проверим фильтр по id
        response = self.get_json(
            '/admin/organizations/%s/staff/?type=user&id=%s' % (
                self.organization['id'],
                self.user['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=self.user['id'],
                        org_id=self.organization['id'],
                    )
                )
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_type_department(self):
        # проверим фильтр по контейнерам с типом департамент + поиск по текстовым полям
        dep = self.create_department(id=self.department_id, org_id=self.organization['id'], label='aaa')

        response = self.get_json(
            '/admin/organizations/%s/staff/?type=department' % (
                self.organization['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='department',
                    object=has_entries(
                        id=dep['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='department',
                    object=has_entries(
                        id=self.user['department_id'],
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

        response = self.get_json(
            '/admin/organizations/%s/staff/?type=department&text=%s' % (
                self.organization['id'],
                'notext',
            ),
        )
        assert len(response['result']) == 0

        # проверим фильтр по id
        response = self.get_json(
            '/admin/organizations/%s/staff/?type=department&id=%s' % (
                self.organization['id'],
                self.user['department_id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='department',
                    object=has_entries(
                        id=self.user['department_id'],
                        org_id=self.organization['id'],
                    )
                )
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_type_group(self):
        # проверим фильтр по контейнерам с типом группа + поиск по текстовым полям

        # обновим label всех групп, чтобы понятно было как они должны сортироваться
        for id in [1, 2, 3, 4]:
            GroupModel(self.main_connection) \
                .filter(id=id) \
                .update(label=str(id))

        response = self.get_json(
            '/admin/organizations/%s/staff/?type=group' % (
                self.organization['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='group',
                    object=has_entries(
                        id=1,
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=2,
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=3,
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=4,
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

        response = self.get_json(
            '/admin/organizations/%s/staff/?type=group&groups.type=organization_admin' % (
                self.organization['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='group',
                    object=has_entries(
                        type='organization_admin',
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

        # проверим фильтр по id
        response = self.get_json(
            '/admin/organizations/%s/staff/?type=group&id=%s' % (
                self.organization['id'],
                2,
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='group',
                    object=has_entries(
                        id=2,
                        org_id=self.organization['id'],
                    )
                )
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_does_not_exist(self):
        # несуществующий контейнер: пользователь, отдел, группа
        # несуществующий сервис
        self.get_json(
            '/admin/organizations/%s/staff/?parent_id=%s&parent_type=department' % (
                self.organization['id'],
                0,
            ),
            expected_code=404,
        )
        self.get_json(
            '/admin/organizations/%s/staff/?parent_id=%s&parent_type=group' % (
                self.organization['id'],
                0,
            ),
            expected_code=404,
        )
        self.get_json(
            '/admin/organizations/%s/staff/?member_id=%s&member_type=user' % (
                self.organization['id'],
                0,
            ),
            expected_code=404,
        )
        self.get_json(
            '/admin/organizations/%s/staff/?member_id=%s&member_type=group' % (
                self.organization['id'],
                0,
            ),
            expected_code=404,
        )
        self.get_json(
            '/admin/organizations/%s/staff/?member_id=%s&member_type=department' % (
                self.organization['id'],
                0,
            ),
            expected_code=404,
        )
        self.get_json(
            '/admin/organizations/%s/staff/?services.slug=frfrfrfr' % (
                self.organization['id'],
            ),
            expected_code=404,
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_parent_id_parent_type_group(self):
        # проверяем контейнеры, вложенные в группу
        group_model = GroupModel(self.main_connection)
        group1 = group_model.create(
            id=self.group_id,
            name={'ru': 'Группа 1', 'en': 'Group admin'},
            org_id=self.organization['id'],
            label='group admin',
        )
        group2 = group_model.create(
            name={'ru': 'Группа 2', 'en': 'Group 2'},
            org_id=self.organization['id'],
            members=[
                {
                    'type': 'user',
                    'id': self.user['id']
                },
                {
                    'type': 'department',
                    'id': self.user['department_id']
                },
                {
                    'type': 'group',
                    'id': group1['id']
                },
            ]
        )
        response = self.get_json(
            '/admin/organizations/%s/staff/?parent_id=%s&parent_type=group' % (
                self.organization['id'],
                group2['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=self.user['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=group1['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='department',
                    object=has_entries(
                        id=self.department['id'],
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

        # проверим дополнительно фильтр по текстовым полям
        response = self.get_json(
            '/admin/organizations/%s/staff/?parent_id=%s&parent_type=group&text=%s' % (
                self.organization['id'],
                group2['id'],
                'adm',
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=self.user['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=group1['id'],
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_parent_id_parent_type_department(self):
        # проверяем контейнеры, вложенные в department
        department = DepartmentModel(self.main_connection).create(
            id=self.department_id,
            name={'ru': 'Отдел', 'en': 'Department'},
            org_id=self.organization['id'],
            parent_id=self.user['department_id'],
            label='dep',
        )

        response = self.get_json(
            '/admin/organizations/%s/staff/?parent_id=%s&parent_type=department' % (
                self.organization['id'],
                self.user['department_id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=self.user['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='department',
                    object=has_entries(
                        id=department['id'],
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

        # проверим дополнительный фильтр по текстовым полям
        response = self.get_json(
            '/admin/organizations/%s/staff/?parent_id=%s&parent_type=department&text=%s' % (
                self.organization['id'],
                self.user['department_id'],
                'dep',
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='department',
                    object=has_entries(
                        id=department['id'],
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_member_id_member_type_user(self):
        # проверяем контейнеры, членом которых является данный пользователь
        group_model = GroupModel(self.main_connection)
        group = group_model.create(
            id=self.group_id,
            name={'ru': 'Группа 1', 'en': 'Group 1'},
            org_id=self.organization['id'],
            label='group',
        )
        group_model.add_member(org_id=self.organization['id'], group_id=group['id'], member={
            'type': 'user',
            'id': self.user['id']
        })
        DepartmentModel(self.main_connection) \
            .filter(id=self.user['department_id']) \
            .update(label='department')

        response = self.get_json(
            '/admin/organizations/%s/staff/?member_id=%s&member_type=user' % (
                self.organization['id'],
                self.user['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='department',
                    object=has_entries(
                        id=self.user['department_id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=group['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        org_id=self.organization['id'],
                    )
                ),
            ),
        )

        # проверим дополнительно фильтр по текстовым полям
        response = self.get_json(
            '/admin/organizations/%s/staff/?member_id=%s&member_type=user&text=%s' % (
                self.organization['id'],
                self.user['id'],
                'group',
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='group',
                    object=has_entries(
                        id=group['id'],
                        org_id=self.organization['id'],
                    )
                ),
            ),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_member_id_member_type_department(self):
        # проверяем контейнеры, членом которых является данный отдел
        group_model = GroupModel(self.main_connection)
        group = group_model.create(
            id=self.group_id,
            name={'ru': 'Группа 1', 'en': 'Group 1'},
            org_id=self.organization['id'],
            label='group',
        )
        group_model.add_member(org_id=self.organization['id'], group_id=group['id'], member={
            'type': 'department',
            'id': self.user['department_id']
        })
        department_model = DepartmentModel(self.main_connection)
        department = department_model.create(
            id=self.department_id,
            name={'ru': 'Отдел', 'en': 'Department'},
            org_id=self.organization['id'],
            label='department',
        )
        department_model\
            .filter(id=self.user['department_id'])\
            .update(parent_id=department['id'])

        response = self.get_json(
            '/admin/organizations/%s/staff/?member_id=%s&member_type=department' % (
                self.organization['id'],
                self.user['department_id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='department',
                    object=has_entries(
                        id=department['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=group['id'],
                        org_id=self.organization['id'],
                    )
                ),
            ),
        )

        # проверим дополнительно фильтр по текстовым полям
        response = self.get_json(
            '/admin/organizations/%s/staff/?member_id=%s&member_type=department&text=%s' % (
                self.organization['id'],
                self.user['department_id'],
                'admin',
            ),
        )
        assert len(response['result']) == 0

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_member_id_member_type_group(self):
        # проверяем контейнеры, членом которых является данная группа

        group_model = GroupModel(self.main_connection)
        # создадим две группы и добавим вторую в первую
        group1 = group_model.create(
            id=self.group_id,
            name={'ru': 'Группа 1', 'en': 'Group 1'},
            org_id=self.organization['id'],
        )
        group2 = group_model.create(
            name={'ru': 'Группа 2', 'en': 'Group 2'},
            org_id=self.organization['id'],
        )
        group_model.add_member(org_id=self.organization['id'], group_id=group1['id'], member={
            'type': 'group',
            'id': group2['id']
        })

        response = self.get_json(
            '/admin/organizations/%s/staff/?member_id=%s&member_type=group' % (
                self.organization['id'],
                group2['id'],
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='group',
                    object=has_entries(
                        id=group1['id'],
                        org_id=self.organization['id'],
                    )
                )
            )
        )

        # проверим дополнительно фильтр по текстовым полям
        response = self.get_json(
            '/admin/organizations/%s/staff/?member_id=%s&member_type=group&text=%s' % (
                self.organization['id'],
                group2['id'],
                'notext'
            ),
        )
        assert len(response['result']) == 0

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_services_slug(self):
        # проверяем контейнеры, подписанные на указанный сервис

        # добавим сервис и активируем его
        slug = 'tracker'
        ServiceModel(self.meta_connection).create(
            slug=slug,
            name=slug,
            client_id='client_id',
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            slug,
        )

        group = GroupModel(self.main_connection).create(
            id=self.group_id,
            org_id=self.organization['id'],
            name={'ru': 'Group'},
            label='group admin',
        )
        resource_id = OrganizationServiceModel(self.main_connection)\
            .get_licensed_service_resource_id(self.organization['id'], slug)

        # добавим для группы, департамента и пользователя подписку на сервис
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=resource_id,
            name=relation_name.member,
            user_id=self.user['id'],
        )
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=resource_id,
            name=relation_name.member,
            department_id=self.user['department_id'],
        )
        ResourceRelationModel(self.main_connection).create(
            org_id=self.organization['id'],
            resource_id=resource_id,
            name=relation_name.member,
            group_id=group['id'],
        )

        response = self.get_json(
            '/admin/organizations/%s/staff/?services.slug=%s' % (
                self.organization['id'],
                slug,
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=self.user['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=group['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='department',
                    object=has_entries(
                        id=self.user['department_id'],
                        org_id=self.organization['id'],
                    )
                ),
            )
        )

        # проверим дополнительно фильтр по текстовым полям
        response = self.get_json(
            '/admin/organizations/%s/staff/?services.slug=%s&text=%s' % (
                self.organization['id'],
                slug,
                'adm',
            ),
        )
        assert_that(
            response['result'],
            contains(
                has_entries(
                    type='user',
                    object=has_entries(
                        id=self.user['id'],
                        org_id=self.organization['id'],
                    )
                ),
                has_entries(
                    type='group',
                    object=has_entries(
                        id=group['id'],
                        org_id=self.organization['id'],
                    )
                ),
            )
        )


class TestAdminActionsListView(TestCase):
    enable_admin_api = True

    def setUp(self):
        super(TestAdminActionsListView, self).setUp()

        self.service_license_change_action = ActionModel(self.main_connection).create(
            org_id=self.organization['id'],
            name=action.service_license_change,
            author_id=self.admin_uid,
            object_type='organization',
            object_value={},
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_should_return_list_of_actions(self):
        # проверим, что ручка отдает список действий
        response = self.get_json('/admin/organizations/{}/actions/'.format(self.organization['id']), TVM2_HEADERS)

        actions = ActionModel(self.main_connection).find()
        actions = [_prepare_action(a) for a in actions]

        assert_that(
            response['result'],
            contains_inanyorder(
                *actions
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_should_order_by_reverse_revision(self):
        # проверим, что работает обратная сортировка по ревизии

        # добавим еще один action
        ActionModel(self.main_connection).create(
           org_id=self.organization['id'],
           name='action',
           author_id=self.user['id'],
           object_value='',
           object_type='',
        )

        response = self.get_json('/admin/organizations/{}/actions/?ordering=-revision'.format(self.organization['id']), TVM2_HEADERS)

        actions = ActionModel(self.main_connection).find()
        actions = sorted([_prepare_action(a) for a in actions], key=lambda a: -a['revision'])

        assert_that(
            response['result'],
            contains(
                *actions
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_should_filter_by_name(self):
        # проверим, что ручка фильтрует действия по типу
        response = self.get_json(
            '/admin/organizations/{}/actions/?name={}'.format(self.organization['id'], action.service_license_change),
            TVM2_HEADERS,
        )

        expected_action = _prepare_action(self.service_license_change_action)
        assert_that(
            response['result'],
            contains(
                expected_action
            )
        )


class TestAdminOrganizationServicesSubscribersView(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationServicesSubscribersView, self).setUp(*args, **kwargs)
        self.service = ServiceModel(self.meta_connection).create(
            name='service',
            slug='service',
            client_id=123,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id=self.organization['id'],
            service_slug=self.service['slug'],
        )

        self.user1 = self.create_user(nickname='user1')
        self.user2 = self.create_user(name={'first': {'ru': 'User2'}})
        self.user3 = self.create_user(nickname='user_three')
        self.user4 = self.create_user(name={'first': {'ru': 'cool'}, 'last': {'ru': 'guy'}})

        self.create_licenses_for_service(
            self.service['id'],
            user_ids=[self.user3['id'], self.user4['id']]
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_simple_get(self):
        response = self.get_json(
            '/admin/organizations/{}/services/{}/subscribers/'.format(
                self.organization['id'],
                self.service['slug']
            ),
            TVM2_HEADERS,
        )

        assert_that(
            response['result'],
            contains_inanyorder(
                has_entries(id=self.user3['id']),
                has_entries(id=self.user4['id']),
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_get_with_filter(self):
        response = self.get_json(
            '/admin/organizations/{}/services/{}/subscribers/?query=user'.format(
                self.organization['id'],
                self.service['slug']
            ),
            TVM2_HEADERS,
        )

        assert_that(
            response['result'],
            contains_inanyorder(
                has_entries(id=self.user3['id']),
            )
        )

        response = self.get_json(
            '/admin/organizations/{}/services/{}/subscribers/?query=cool'.format(
                self.organization['id'],
                self.service['slug']
            ),
            TVM2_HEADERS,
        )

        assert_that(
            response['result'],
            contains_inanyorder(
                has_entries(id=self.user4['id']),
            )
        )


class TestAdminOrganizationDomainsView(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationDomainsView, self).setUp(*args, **kwargs)
        self.org_id = self.organization['id']
        self.alias1 = DomainModel(self.main_connection).create(
            name='alias1',
            org_id=self.org_id,
            owned=True,
            via_webmaster=False,
        )
        self.alias2 = DomainModel(self.main_connection).create(
            name='alias2',
            org_id=self.org_id,
            owned=False,
            via_webmaster=True
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_get_simple(self):
        # получаем все домены организации
        response = self.get_json(
            '/admin/organizations/{}/domains/'.format(self.org_id),
            TVM2_HEADERS,
        )

        expected = [_prepare_domain(d) for d in DomainModel(self.main_connection).find({'org_id': self.org_id})]
        assert_that(
            response['result'],
            contains_inanyorder(*expected)
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_get_with_filter(self):
        # получаем все домены организации
        response = self.get_json(
            '/admin/organizations/{}/domains/?master=True'.format(self.org_id),
            TVM2_HEADERS,
        )

        assert_that(
            response['result'],
            contains(
                has_entries(
                    name=self.organization_domain
                )
            )
        )

        response = self.get_json(
            '/admin/organizations/{}/domains/?owned=True'.format(self.org_id),
            TVM2_HEADERS,
        )
        assert_that(
            response['result'],
            contains_inanyorder(
                has_entries(
                    name=self.organization_domain
                ),
                has_entries(
                    name=self.alias1['name']
                ),
            )
        )

        response = self.get_json(
            '/admin/organizations/{}/domains/?via_webmaster=True'.format(self.org_id),
            TVM2_HEADERS,
        )
        assert_that(
            response['result'],
            contains_inanyorder(
                has_entries(
                    name=self.organization_domain
                ),
                has_entries(
                    name=self.alias2['name']
                ),
            )
        )


class TestAdminOrganizationChangeOwnerView(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationChangeOwnerView, self).setUp(*args, **kwargs)
        self.org_id = self.organization['id']

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_change_owner(self):
        # передача прав внешнему админу
        comment = 'some comment'
        new_owner_uid = 7777
        with patch('intranet.yandex_directory.src.yandex_directory.admin.views.organizations.get_user_id_from_passport_by_login',
                   return_value=new_owner_uid):
            self.post_json(
                '/admin/organizations/{}/owner/'.format(self.org_id),
                {'comment': comment, 'new_owner_login': 'new-ownder@yandex.ru'},
                TVM2_HEADERS,
                expected_code=202,
            )

        # создали задачу
        task = TaskModel(self.main_connection).filter(
            task_name=ChangeOrganizationOwnerTask.get_task_name(),
        ).one()
        assert_that(
            task['params'],
            has_entries(
                org_id=self.org_id,
                new_owner_uid=new_owner_uid
            )
        )
        # записали запись в лог
        assert_that(
            SupportActionMetaModel(self.meta_connection).all(),
            has_item(
                has_entries(
                    org_id=self.org_id,
                    name=SupportActions.change_organization_admin,
                    comment=comment
                )
            )

        )


class TestAdminOrganizationsDomainOwnershipView(TestCase):
    enable_admin_api = True

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_ownership_info_when_verification_failed(self):
        # Проверим, что если процедуру подтверждения запустили, то ручка
        # отдаст способ подтверждения, выбранный пользователем и дату
        # последней проверки.

        # Сделаем вид, что домен owned=False
        domain_name = 'example.com'
        org_id = self.organization['id']
        DomainModel(self.main_connection).create(domain_name, org_id, owned=False)

        # Сделаем вид, что запрос в вебмастер вернул список возможных способов подтверждения
        self.mocked_webmaster_inner_list_applicable.side_effect = webmaster_responses.applicable()
        # После нескольких попыток подтвердить через DNS не удалось, и
        # нужно чтобы пользователь снова нажал кнопку.
        self.mocked_webmaster_inner_info.side_effect = webmaster_responses.verification_failed()

        response = self.get_json('/admin/organizations/{}/domains/{}/'.format(org_id, domain_name))

        assert_that(
            response,
            has_entries(
                status='need-validation',
                last_check=has_entries(
                    method='webmaster.dns',
                    date='2018-03-02T13:40:10.167Z',
                    fail_type='dns_record_not_found',
                    fail_reason='some_error',
                ),
            )
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_ownership_info_when_verification_in_progress(self):
        # Проверим, что если процедуру подтверждения запустили, но она еще не закончена

        # Сделаем вид, что домен owned=False
        domain_name = 'example.com'
        org_id = self.organization['id']
        DomainModel(self.main_connection).create(domain_name, org_id, owned=False)

        # Сделаем вид, что запрос в вебмастер вернул список возможных способов подтверждения
        self.mocked_webmaster_inner_list_applicable.side_effect = webmaster_responses.applicable()
        # После нескольких попыток подтвердить через DNS не удалось, и
        # нужно чтобы пользователь снова нажал кнопку.
        self.mocked_webmaster_inner_info.side_effect = webmaster_responses.verification_in_progress()

        response = self.get_json('/admin/organizations/{}/domains/{}/'.format(org_id, domain_name))

        assert_that(
            response,
            has_entries(
                status='in-progress',
            )
        )


class TestAdminPartnersList(TestCase):
    enable_admin_api = True

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_me(self):
        # получаем спискок партнеров
        response = self.get_json(
            '/admin/partners/',
            headers=TVM2_HEADERS,
        )
        assert_that(
            response['result'],
            contains_inanyorder(
                has_entries(
                   **self.partner
                )
            )
        )


class TestAdminOrganizationFeaturesList(TestCase):
    enable_admin_api = True

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_me(self):
        # получаем список фичей для организации
        response = self.get_json(
            '/admin/organizations/{}/features/'.format(self.organization['id']),
            headers=TVM2_HEADERS,
        )

        assert_that(
            response,
            equal_to(self.features_list)
        )


class TestAdminOrganizationFeaturesManage(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationFeaturesManage, self).setUp(*args, **kwargs)
        self.comment = 'some text'

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_enable_feature(self):
        self.post_json(
            '/admin/organizations/{}/features/{}/enable/'.format(
                self.organization['id'],
                CHANGE_ORGANIZATION_OWNER
            ),
            data={'comment': self.comment},
            headers=TVM2_HEADERS,
        )

        assert_that(
            is_feature_enabled(self.meta_connection, self.organization['id'], CHANGE_ORGANIZATION_OWNER),
            equal_to(True)
        )

        assert_that(
            SupportActionMetaModel(self.meta_connection).all(),
            has_item(
                has_entries(
                    org_id=self.organization['id'],
                    name=SupportActions.manage_features,
                    object={
                        CHANGE_ORGANIZATION_OWNER: 'enable',
                    },
                    comment=self.comment,
                )
            )
        )

        # повторное включение ничего не ломает
        self.post_json(
            '/admin/organizations/{}/features/{}/enable/'.format(
                self.organization['id'],
                CHANGE_ORGANIZATION_OWNER
            ),
            data={'comment': self.comment},
            headers=TVM2_HEADERS,
        )

        assert_that(
            SupportActionMetaModel(self.meta_connection).filter(
                org_id=self.organization['id'],
                name=SupportActions.manage_features,
            ).count(),
            equal_to(1)
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_disable_feature(self):
        # DOMAIN_AUTO_HANDOVER включается в базовом сетапе
        self.post_json(
            '/admin/organizations/{}/features/{}/disable/'.format(
                self.organization['id'],
                DOMAIN_AUTO_HANDOVER
            ),
            data={'comment': self.comment},
            headers=TVM2_HEADERS,
        )

        assert_that(
            is_feature_enabled(self.meta_connection, self.organization['id'], DOMAIN_AUTO_HANDOVER),
            equal_to(False)
        )

        assert_that(
            SupportActionMetaModel(self.meta_connection).all(),
            has_item(
                has_entries(
                    org_id=self.organization['id'],
                    name=SupportActions.manage_features,
                    object={
                        DOMAIN_AUTO_HANDOVER: 'disable',
                    },
                    comment=self.comment
                )
            )
        )

        # повторное выключение ничего не ломает
        self.post_json(
            '/admin/organizations/{}/features/{}/disable/'.format(
                self.organization['id'],
                DOMAIN_AUTO_HANDOVER
            ),
            data={'comment': self.comment},
            headers=TVM2_HEADERS,
        )

        assert_that(
            SupportActionMetaModel(self.meta_connection).filter(
                org_id=self.organization['id'],
                name=SupportActions.manage_features,
            ).count(),
            equal_to(1)
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_incorrect(self):
        # проверяем нехорошие сценарии
        self.post_json(
            '/admin/organizations/{}/features/{}/disable/'.format(
                self.organization['id'],
                'some-feature'
            ),
            data={'comment': self.comment},
            headers=TVM2_HEADERS,
            expected_code=422
        )

        self.post_json(
            '/admin/organizations/{}/features/{}/run/'.format(
                self.organization['id'],
                DOMAIN_AUTO_HANDOVER
            ),
            data={'comment': self.comment},
            headers=TVM2_HEADERS,
            expected_code=422
        )

        # создадим фичу, включенную для всех
        query = """
            INSERT INTO features (slug, enabled_default, description)
            VALUES ('always_enable_feature', True, 'description');
        """
        self.meta_connection.execute(query)

        assert_that(
            is_feature_enabled(self.meta_connection, self.organization['id'], 'always_enable_feature'),
            equal_to(True)
        )
        response = self.post_json(
            '/admin/organizations/{}/features/{}/disable/'.format(
                self.organization['id'],
                'always_enable_feature'
            ),
            data={'comment': self.comment},
            headers=TVM2_HEADERS,
            expected_code=422
        )

        assert_that(
            response,
            has_entries(
                code='invalid_value',
                message='Feature enabled by default for everyone'
            )
        )


class TestAdminOrganizationWhitelistManage(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestAdminOrganizationWhitelistManage, self).setUp(*args, **kwargs)
        self.comment = 'some text'
        self.org_id = self.organization['id']

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_add_to_whitelist(self):
        assert_that(
            OrganizationModel(self.main_connection).filter(id=self.org_id).fields('vip').one()['vip'],
            empty()
        )

        self.post_json(
            '/admin/organizations/{}/whitelist/'.format(
                self.organization['id'],
            ),
            data={'comment': self.comment},
            headers=TVM2_HEADERS,
        )

        assert_that(
            OrganizationModel(self.main_connection).filter(id=self.org_id).fields('vip').one()['vip'],
            equal_to([vip_reason.whitelist])
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_delete_from_whitelist(self):
        assert_that(
            OrganizationModel(self.main_connection).filter(id=self.org_id).fields('vip').one()['vip'],
            empty()
        )

        OrganizationModel(self.main_connection).update_one(
            self.org_id,
            {'vip': [vip_reason.whitelist]}
        )

        assert_that(
            OrganizationModel(self.main_connection).filter(id=self.org_id).fields('vip').one()['vip'],
            equal_to([vip_reason.whitelist])
        )

        self.delete_json(
            '/admin/organizations/{}/whitelist/'.format(
                self.organization['id'],
            ),
            data={'comment': self.comment},
            headers=TVM2_HEADERS,
        )

        assert_that(
            OrganizationModel(self.main_connection).filter(id=self.org_id).fields('vip').one()['vip'],
            empty()
        )


class TestOrganizationBlock(TestCase):
    enable_admin_api = True

    def setUp(self, *args, **kwargs):
        super(TestOrganizationBlock, self).setUp(*args, **kwargs)
        self.org_id = self.organization['id']

    @tvm2_auth_success(100700, scopes=[])
    def test_without_permission(self):
        self.post_json(
            '/admin/organizations/{org_id}/block/'.format(org_id=self.organization['id']),
            data={
                'comment': 'comment-for-block'
            },
            expected_code=403,
            headers=TVM2_HEADERS,
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_without_comment(self):
        self.post_json(
            '/admin/organizations/{org_id}/block/'.format(org_id=self.org_id),
            data={},
            expected_code=422,
            headers=TVM2_HEADERS,
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_block_organization(self):
        self.post_json(
            '/admin/organizations/{org_id}/block/'.format(org_id=self.org_id),
            data={
                'comment': 'comment-for-block'
            },
            expected_code=200,
            headers=TVM2_HEADERS,
        )

        assert_that(
            OrganizationModel(self.main_connection).get(self.org_id),
            has_entries(
                is_blocked=True,
            )
        )

        assert_that(
            SupportActionMetaModel(self.meta_connection).filter(
                org_id=self.organization['id'],
                name=SupportActions.block_organization,
            ).count(),
            equal_to(1),
        )

        assert_that(
            ActionModel(self.main_connection) \
                .filter(org_id=self.org_id, name=action.organization_block) \
                .count(),
            equal_to(1),
        )

        assert_that(
            EventModel(self.main_connection) \
                .filter(name=event.organization_blocked) \
                .count(),
            equal_to(1),
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_block_paid_organization(self):
        # проверяем, что платные сервисы выключаются при блокировке
        self.enable_paid_mode(self.org_id)
        paid_service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            paid_by_license=True,
            ready_default=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.org_id,
            paid_service['slug']
        )

        self.post_json(
            '/admin/organizations/{org_id}/block/'.format(org_id=self.org_id),
            data={
                'comment': 'comment-for-block'
            },
            expected_code=200,
            headers=TVM2_HEADERS,
        )

        assert_that(
            OrganizationModel(self.main_connection).get(self.org_id),
            has_entries(
                is_blocked=True,
                subscription_plan='free'
            )
        )

        assert_that(
            OrganizationServiceModel(self.main_connection).filter(
                org_id=self.org_id,
                service_id=paid_service['id'],
                enabled=Ignore,
            ).one(),
            has_entries(
                enabled=False
            )
        )


class TestOrganizationUnblock(TestCase):
    enable_admin_api = True

    @tvm2_auth_success(100700, scopes=[])
    def test_without_permission(self):
        self.post_json(
            '/admin/organizations/{org_id}/unblock/'.format(org_id=self.organization['id']),
            data={
                'comment': 'comment-for-unblock'
            },
            expected_code=403,
            headers=TVM2_HEADERS,
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_without_comment(self):
        org_id = self.organization['id']

        self.post_json(
            '/admin/organizations/{org_id}/unblock/'.format(org_id=org_id),
            data={},
            expected_code=422,
            headers=TVM2_HEADERS,
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    def test_unblock_organization(self):
        org_id = self.organization['id']

        OrganizationModel(self.main_connection).update(
            update_data={'is_blocked': True},
            filter_data={'id': org_id},
        )

        self.post_json(
            '/admin/organizations/{org_id}/unblock/'.format(org_id=org_id),
            data={
                'comment': 'comment-for-unblock'
            },
            expected_code=200,
            headers=TVM2_HEADERS,
        )

        assert_that(
            OrganizationModel(self.main_connection).get(org_id),
            has_entries(
                is_blocked=False,
            )
        )

        assert_that(
            SupportActionMetaModel(self.meta_connection).filter(
                org_id=self.organization['id'],
                name=SupportActions.unblock_organization,
            ).count(),
            equal_to(1),
        )

        assert_that(
            ActionModel(self.main_connection) \
                .filter(org_id=org_id, name=action.organization_unblock) \
                .count(),
            equal_to(1),
        )

        assert_that(
            EventModel(self.main_connection) \
                .filter(name=event.organization_unblocked) \
                .count(),
            equal_to(1),
        )


class TestGetDomainsFromDomenatorInCatalog(TestCase):
    enable_admin_api = True

    def setUp(self):
        super().setUp()
        tvm.tickets['domenator'] = 'tvm-ticket-domenator'
        setup_domenator_client(app)
        app.domenator.sync_connect = lambda *args, **kwargs: None
        set_feature_value_for_organization(
            self.meta_connection,
            self.organization['id'],
            USE_DOMENATOR,
            True,
        )

    @tvm2_auth_success(100700, scopes=[scope.internal_admin])
    @responses.activate
    def test_get_domains(self):
        org_id = self.organization['id']
        responses.add(
            responses.GET,
            f'https://domenator-test.yandex.net/api/private/domains/?org_id={org_id}',
            json=[{
                "org_id": org_id,
                "name": "dfmk.auto.connect-tk.tk",
                "owned": True,
                "master": False,
                "display": None,
                "blocked_at": None,
                "created_at": "2020-10-19T13:08:20.367123+00:00",
                "delegated": False,
                "mx": False,
                "validated": False,
                "validated_at": "2020-10-19T13:08:26.888722+00:00",
                "via_webmaster": True
            }, {
                "org_id": org_id,
                "name": "hfhjidhj.auto.connect-tk.tk",
                "owned": True,
                "master": False,
                "display": None,
                "blocked_at": None,
                "created_at": "2020-08-31T14:56:42.908081+00:00",
                "delegated": False,
                "mx": False,
                "validated": False,
                "validated_at": "2020-08-31T14:56:55.987860+00:00",
                "via_webmaster": True
            }],
            status=200,
        )

        response = self.get_json(
            f'/admin/organizations/{org_id}/domains/',
            expected_code=200,
            headers=TVM2_HEADERS,
        )

        assert response == {
            "page": 1,
            "per_page": 2,
            "pages": 1,
            "total": 2,
            "links": {},
            "multishard": False,
            'result': [{
                "org_id": org_id,
                "name": "dfmk.auto.connect-tk.tk",
                "owned": True,
                "master": False,
                "display": None,
                "blocked_at": None,
                "created_at": "2020-10-19T13:08:20.367123+00:00",
                "delegated": False,
                "mx": False,
                "validated": False,
                "validated_at": "2020-10-19T13:08:26.888722+00:00",
                "via_webmaster": True
            }, {
                "org_id": org_id,
                "name": "hfhjidhj.auto.connect-tk.tk",
                "owned": True,
                "master": False,
                "display": None,
                "blocked_at": None,
                "created_at": "2020-08-31T14:56:42.908081+00:00",
                "delegated": False,
                "mx": False,
                "validated": False,
                "validated_at": "2020-08-31T14:56:55.987860+00:00",
                "via_webmaster": True
            }]
        }
