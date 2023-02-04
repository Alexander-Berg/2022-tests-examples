# -*- coding: utf-8 -*-
import datetime
import dateutil.parser
import pytest
import unittest.mock
from unittest.mock import Mock, patch, ANY

from intranet.yandex_directory.src.yandex_directory.common.billing.client import WORKSPACE_SERVICE_ID
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    utcnow,
    format_date,
    format_datetime,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationModel,
    GroupModel,
    ServiceModel,
    UserServiceLicenses,
    EventModel,
    PromocodeModel,
    OrganizationLicenseConsumedInfoModel,
    OrganizationPromocodeModel,
    RequestedUserServiceLicenses,
    DepartmentModel,
    ResourceModel,
    ResourceRelationModel,
    UserModel,
    ActionModel,
    TaskModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.organization import organization_type
from intranet.yandex_directory.src.yandex_directory.core.views.subscription import (
    get_next_act_date,
    get_users_count_from_relations,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import enable_service, trial_status, OrganizationServiceModel
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.models.organization import (
    promocode_type,
)
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShardsCommand
from intranet.yandex_directory.src.yandex_directory.core.utils import (
    prepare_requests_for_licenses,
)

from testutils import (
    TestCase,
    override_settings,
    get_auth_headers,
    create_organization,
    fake_userinfo,
    assert_called_once,
)

from hamcrest import (
    all_of,
    assert_that,
    equal_to,
    none,
    is_not,
    has_entries,
    contains,
    contains_inanyorder,
    not_none,
    has_length,
    has_item,
)
from intranet.yandex_directory.src.yandex_directory.connect_services.partner_disk.tasks import (
    AddSpacePartnerDiskTask,
    DeleteSpacePartnerDiskTask,
)
from intranet.yandex_directory.src.yandex_directory.auth import tvm


class TestOrganizationSubscriptionInfoView(TestCase):
    def test_get_subscription_info(self):
        # проверяем, что ручка вернет информацию о подписке организации с балансом и датой задолженности
        self.enable_paid_mode()

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'subscription_plan'],
        )
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))
        assert_that(
            fresh_organization['billing_info'],
            is_not(
                none()
            )
        )
        contract_id = 10
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '0',
            'ReceiptSum': '0',
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/')

        exp_response = {
            'balance': 0,
            'person_type': 'natural',
            'subscription_plan': 'paid',
            'debt_start_date': None,
            'payment_due_date': None,
            'has_contract': True,
            'next_act_date': get_next_act_date().isoformat(),
            'subscription_plan_change_requirements': {
                'why_not_changeable': [],
            },
            'days_until_disable_by_debt': None,
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

    def test_get_subscription_info_with_debt_date(self):
        # проверяем, что ручка вернет информацию о подписке организации с непустой задолженностью
        payment_term = 5
        today = dateutil.parser.parse('2017-03-08').date()
        self.enable_paid_mode(first_debt_act_date=today, balance=-100)

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'subscription_plan'],
        )
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))
        assert_that(
            fresh_organization['billing_info'],
            is_not(
                none()
            )
        )
        contract_id = 10
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '100',
            'ReceiptSum': '0',
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            with override_settings(BILLING_PAYMENT_TERM=payment_term):
                response = self.get_json('/subscription/')

        exp_response = {
            'balance': -100,
            'person_type': 'natural',
            'subscription_plan': 'paid',
            'debt_start_date': today.isoformat(),
            'has_contract': True,
            'next_act_date': get_next_act_date().isoformat(),
            'payment_due_date': (today + datetime.timedelta(days=payment_term)).isoformat(),
            'subscription_plan_change_requirements': {
                'why_not_changeable': [],
            },
            'days_until_disable_by_debt': 0,
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

    def test_get_subscription_info_for_free_organization(self):
        # проверяем, что ручка вернет информацию о подписке организации с балансом и датой задолженности
        # даже если организация бесплатная

        # включаем и сразу выключаем платный режим, чтобы у организации была принята оферта
        # но она работала на бесплатной подписке
        self.enable_paid_mode()
        self.disable_paid_mode()

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'subscription_plan'],
        )
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))
        assert_that(
            fresh_organization['billing_info'],
            is_not(
                none()
            )
        )
        contract_id = 10
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '0',
            'ReceiptSum': '0',
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/')

        exp_response = {
            'balance': 0,
            'person_type': 'natural',
            'subscription_plan': 'free',
            'debt_start_date': None,
            'has_contract': True,
            'next_act_date': get_next_act_date().isoformat(),
            'payment_due_date': None,
            'subscription_plan_change_requirements': {
                'why_not_changeable': [],
            },
            'days_until_disable_by_debt': None,
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

    def test_get_subscription_info_for_free_organization_without_contract(self):
        # проверяем, что ручка вернет информацию о подписке организации с балансом и датой задолженности
        # и has_contract == False если организация не подписала контракт/не приняла оферту

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'subscription_plan'],
        )
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))
        assert_that(fresh_organization['billing_info'], none())

        response = self.get_json('/subscription/')

        exp_response = {
            'balance': None,
            'person_type': None,
            'subscription_plan': 'free',
            'debt_start_date': None,
            'has_contract': False,
            'next_act_date': None,
            'payment_due_date': None,
            'subscription_plan_change_requirements': {
                'why_not_changeable': [],
            },
            'days_until_disable_by_debt': None,
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

    @override_settings(BILLING_PAYMENT_TERM=10)
    def test_get_subscription_info_for_free_organization_with_debt(self):
        # проверяем, что ручка вернет информацию о подписке организации с балансом, датой задолженности и
        # 'why_not_changeable' == ['organization_has_debt'] для бесплатной организации с задолженностью

        # включаем и сразу выключаем платный режим, чтобы у организации была принята оферта
        # но она работала на бесплатной подписке
        first_debt_act_date = datetime.datetime(year=2017, month=0o1, day=0o7).date()
        self.enable_paid_mode(first_debt_act_date=first_debt_act_date, balance=-100)
        self.disable_paid_mode()

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'subscription_plan'],
        )
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))
        assert_that(
            fresh_organization['billing_info'],
            is_not(
                none()
            )
        )
        contract_id = 10
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '100',
            'ReceiptSum': '0',
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.get_json('/subscription/')

        exp_response = {
            'balance': -100,
            'person_type': 'natural',
            'subscription_plan': 'free',
            'debt_start_date': first_debt_act_date.isoformat(),
            'has_contract': True,
            'next_act_date': get_next_act_date().isoformat(),
            'payment_due_date': (first_debt_act_date + datetime.timedelta(days=10)).isoformat(),
            'subscription_plan_change_requirements': {
                'why_not_changeable': ['organization_has_debt'],
            },
            'days_until_disable_by_debt': 0
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )

    def test_get_subscription_info_with_days_until_disable_by_debt(self):
        # проверяем, что ручка вернет сколько дней осталось до отключения платных сервисов
        payment_term = 5
        debt_start_date = utcnow().date() - datetime.timedelta(days=2)
        self.enable_paid_mode(first_debt_act_date=debt_start_date, balance=-100)

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'subscription_plan'],
        )
        assert_that(fresh_organization['subscription_plan'], equal_to('paid'))
        assert_that(
            fresh_organization['billing_info'],
            is_not(
                none()
            )
        )
        contract_id = 10
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '100',
            'ReceiptSum': '0',
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            with override_settings(BILLING_PAYMENT_TERM=payment_term):
                    response = self.get_json('/subscription/')

        exp_response = {
            'balance': -100,
            'person_type': 'natural',
            'subscription_plan': 'paid',
            'debt_start_date': debt_start_date.isoformat(),
            'has_contract': True,
            'next_act_date': get_next_act_date().isoformat(),
            'payment_due_date': (debt_start_date + datetime.timedelta(days=payment_term)).isoformat(),
            'subscription_plan_change_requirements': {
                'why_not_changeable': [],
            },
            'days_until_disable_by_debt': 4,
        }

        assert_that(
            response,
            equal_to(
                exp_response,
            ),
        )


class Test__test_get_next_act_date(TestCase):

    def test_get_next_act_date_simple(self):
        self.assertEqual(get_next_act_date(dateutil.parser.parse('2017-03-08')),
                         dateutil.parser.parse('2017-04-01'))

    def test_get_next_act_date_if_today_first(self):
        today = dateutil.parser.parse('2017-12-01')
        self.assertEqual(get_next_act_date(today), today)

    def test_get_next_act_date_if_today_december_not_first(self):
        self.assertEqual(get_next_act_date(dateutil.parser.parse('2017-12-12')),
                         dateutil.parser.parse('2018-01-01'))


class TestOrganizationChangeServiceLicensesView(TestCase):
    def setUp(self):
        super(TestOrganizationChangeServiceLicensesView, self).setUp()
        # заголовок для авторизации по токенам
        self.token_auth_header = get_auth_headers(as_uid=self.admin_uid)
        self.service = ServiceModel(self.meta_connection).create(
            slug='tracker',
            name='tracker',
            client_id='client_id',
            paid_by_license=True,
        )
        self.org_service = enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )

    def test_put_get_simple(self):
        # выдадим лицензии на группу, в которой есть подргуппа с 10 пользователями, и на отдел с одним пользователем
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 0)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 0)

        group_users = [{'type': 'user', 'id': self.create_user()['id']} for _ in range(10)]
        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            },
            members=group_users
        )
        main_group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            },
            members=[{'type': 'group', 'id': group['id']}]
        )
        department = self.create_department()
        dep_user = self.create_user(department_id=department['id'])

        EventModel(self.main_connection).delete(force_remove_all=True)
        person_id = 1
        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@yandex-team.ru'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': main_group['id']
                },
                {
                    'type': 'department',
                    'id': department['id']
                }
            ],
            headers=self.token_auth_header,
        )

        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    object_type='group',
                    object=has_entries(
                        id=main_group['id'],
                        name=main_group['name'],
                        type=main_group['type'],
                        members_count=10,
                    )
                ),
                has_entries(
                    object_type='department',
                    object=has_entries(
                        id=department['id'],
                        name=department['name'],
                        parent_id=department['parent_id'],
                        members_count=1,
                    )
                )
            )
        )
        # проверим, что обновилась таблица с лицензиями и у всех пользователей подргуппы и отдела есть лицензии
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 11)

        # в таблице с платаженой информацией также 11 записей
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 11)

        # сгенерировалось событие
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.service_license_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.service_license_changed,
                    content={}
                )
            )
        )

        resp_get = self.get_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            headers=self.token_auth_header,
        )

        assert_that(resp_get, equal_to(response))

    def test_put_group_with_subgroup(self):
        # выдадим лицензии на группу с подгруппой и меням состав подгруппы
        # проверяем, что состав лицензий меняется
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 0)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 0)

        group_users = [{'type': 'user', 'id': self.create_user()['id']} for _ in range(10)]
        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            },
            members=group_users[:-1]
        )
        main_group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            },
            members=[{'type': 'group', 'id': group['id']}]
        )
        person_id = 1
        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@yandex-team.ru'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': main_group['id']
                },
            ],
            headers=self.token_auth_header,
        )

        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    object_type='group',
                    object=has_entries(
                        id=main_group['id'],
                        name=main_group['name'],
                        type=main_group['type'],
                        members_count=9,
                    )
                ),
            )
        )
        # проверим, что обновилась таблица с лицензиями и у всех пользователей подргуппы и отдела есть лицензии
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 9)

        # добавляем пользователя в подгруппу
        self.patch_json(
            '/groups/%s/' % group['id'],
            data={
                'members': group_users
            }
        )
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 10)

    def test_put_department_with_subdepartment(self):
        # выдадим лицензии на отдел с подотделом и меням состав подотдела
        # проверяем, что состав лицензий меняется
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 0)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 0)

        main_dep = self.create_department()
        sub_dep = self.create_department(parent_id=main_dep['id'])
        user_ids = [self.create_user(department_id=sub_dep['id'])['id'] for _ in range(10)]
        dep_users = [{'type': 'user', 'id': uid} for uid in user_ids]
        person_id = 1
        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@yandex-team.ru'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'department',
                    'id': main_dep['id']
                },
            ],
            headers=self.token_auth_header,
        )

        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    object_type='department',
                    object=has_entries(
                        id=main_dep['id'],
                        name=main_dep['name'],
                        members_count=10,
                    )
                ),
            )
        )
        # проверим, что обновилась таблица с лицензиями и у всех пользователей подргуппы и отдела есть лицензии
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 10)

        # перемещаем пользователя из подотдела
        self.patch_json('/users/%s/' % user_ids[0], data={'department_id': self.department['id']})
        assert_that(
            DepartmentModel(self.main_connection).get(sub_dep['id'], self.organization['id']),
            has_entries(
                members_count=9,
            )
        )

        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 9)

    def test_invalid_service(self):
        # Проверяем, что ручка вернет ошибки, если передан некорректный сервис
        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            }
        )
        EventModel(self.main_connection).delete(force_remove_all=True)
        # несуществующий сервис
        response = self.put_json(
            '/subscription/services/%s/licenses/' % '123',
            data=[
                {
                    'type': 'group',
                    'id': group['id']
                }
            ],
            headers=self.token_auth_header,
            expected_code=404,
        )

        assert_that(
            response,
            equal_to({
                'message': 'Service not found',
                'code': 'service_not_found',
            })
        )
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.service_license_changed,
                }
            ),
            equal_to([])
        )

        # создадим сервис, но без лицензий
        another_service = ServiceModel(self.meta_connection).create(
            slug='my_service',
            name='service',
            client_id='my_client_id',
        )

        response = self.put_json(
            '/subscription/services/%s/licenses/' % another_service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': group['id']
                }
            ],
            headers=self.token_auth_header,
            expected_code=422,
        )

        assert_that(
            response,
            equal_to({
                'message': 'Service is not licensed',
                'code': 'service_not_licensed',
            })
        )
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.service_license_changed,
                }
            ),
            equal_to([])
        )

        # включим лицензии, но не привяжем к организации
        another_service = ServiceModel(self.meta_connection).update(
            filter_data={'slug': 'my_service'},
            update_data={'paid_by_license': True},
        )

        response = self.put_json(
            '/subscription/services/%s/licenses/' % another_service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': group['id']
                }
            ],
            headers=self.token_auth_header,
            expected_code=403,
        )
        assert_that(
            response,
            equal_to({
                'message': 'Service is not enabled',
                'code': 'service_is_not_enabled',
            })
        )
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.service_license_changed,
                }
            ),
            equal_to([])
        )

    def test_post_relation(self):
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 0)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 0)

        user = self.create_user()

        resp_get = self.get_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            headers=self.token_auth_header,
        )

        assert_that(resp_get, equal_to([]))

        EventModel(self.main_connection).delete(force_remove_all=True)
        response = self.post_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data={
                'type': 'user',
                'id': user['id'],
            },
            headers=self.token_auth_header,
        )

        assert_that(
            len(response),
            equal_to(1),
        )

        assert_that(
            response[0],
            has_entries(
                object_type='user',
                object=has_entries(
                    id=user['id'],
                    nickname=user['nickname'],
                    gender=user['gender'],
                    position=user['position'],
                )
            )
        )

        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.service_license_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.service_license_changed,
                    content={}
                )
            )
        )

        # проверим, что обновились таблицы с лицензиями и платежной информацией
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 1)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 1)

    def test_delete_relation(self):
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 0)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 0)

        user = self.create_user()
        self.post_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data={
                'type': 'user',
                'id': user['id'],
            },
            headers=self.token_auth_header,
        )
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 1)
        self.delete_json(
            f'/subscription/services/{self.service["slug"]}/licenses/user/{user["id"]}/',
            headers=self.token_auth_header,
        )

        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 0)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 1)

    def test_put_multiple_relations(self):
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 0)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 0)

        group_users = [{'type': 'user', 'id': self.create_user()['id']} for _ in range(4)]
        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            },
            members=group_users
        )
        user = self.create_user()

        resp_get = self.get_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            headers=self.token_auth_header,
        )

        assert_that(resp_get, equal_to([]))
        person_id = 1
        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@yandex-team.ru'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        EventModel(self.main_connection).delete(force_remove_all=True)
        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': group['id']
                },
                {
                    'type': 'user',
                    'id': user['id']
                }
            ],
            headers=self.token_auth_header,
        )

        assert_that(
            len(response),
            equal_to(2),
        )

        assert_that(
            response[0],
            has_entries(
                object_type='group',
                object=has_entries(
                    id=group['id'],
                    name=group['name'],
                    type=group['type'],
                    members_count=4,
                )
            )
        )

        assert_that(
            response[1],
            has_entries(
                object_type='user',
                object=has_entries(
                    id=user['id'],
                    nickname=user['nickname'],
                    gender=user['gender'],
                    position=user['position'],
                )
            )
        )

        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.service_license_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.service_license_changed,
                    content={}
                )
            )
        )

        # проверим, что обновились таблицы с лицензиями и платежной информацией
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 5)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 5)

    def test_add_delete_relations(self):
        # выдадим лицензии на группу и отберем их в тот же день,
        # в таблице с платежной информацией должны сохраниться все лицензии
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 0)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 0)

        group_users = [{'type': 'user', 'id': self.create_user()['id']} for _ in range(10)]
        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            },
            members=group_users
        )
        person_id = 1
        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@yandex-team.ru'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': group['id']
                }
            ],
            headers=self.token_auth_header,
        )

        # проверим, что обновилась таблица с лицензиями и у всех пользователей лицензии
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 10)

        # в таблице с платаженой информацией также 10 записей
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 10)

        # создадим новую группу и обновим лицензии
        new_group_users = [{'type': 'user', 'id': self.create_user()['id']} for _ in range(5)]
        new_group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            },
            members=new_group_users
        )
        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': new_group['id']
                }
            ],
            headers=self.token_auth_header,
        )

        # обновилась таблица с лицензиями
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 5)

        # в таблице с платеженой информацией есть все лицензии за сегодня
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 15)

    def test_add_to_user_license(self):
        OrganizationModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            update_data={
                'organization_type': organization_type.partner_organization,
                'partner_id': self.partner['id']
            }
        )

        group_users = [{'type': 'user', 'id': self.create_user()['id']} for _ in range(10)]
        group = GroupModel(self.main_connection).create(
            org_id=self.organization['id'],
            name={
                'ru': 'Group'
            },
            members=group_users[:-1]
        )

        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'department',
                    'id': self.department['id']
                },

            ],
            headers=self.token_auth_header,
            expected_code=422
        )
        assert_that(
            response,
            has_entries(
                code='subscription.incompatible_container_type',
            )
        )

        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': group['id']
                },

            ],
            headers=self.token_auth_header,
            expected_code=422
        )
        assert_that(
            response,
            has_entries(
                code='subscription.incompatible_container_type',
            )
        )

    def test_try_give_over_licenses_for_partner_organization(self):
        # нельзя выдать лицензии сверх лимита для партнерских организаций
        OrganizationModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            update_data={
                'organization_type': organization_type.partner_organization,
                'partner_id': self.partner['id']
            }
        )

        update_data = {
            'user_limit': 10,
            'expires_at': datetime.date.today() + datetime.timedelta(days=10),
            'trial_status': trial_status.inapplicable,
        }

        OrganizationServiceModel(self.main_connection) \
            .filter(service_id=self.org_service['id']).update(**update_data)

        users = [{'type': 'user', 'id': self.create_user()['id']} for _ in range(11)]

        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=users,
            headers=self.token_auth_header,
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='subscription.limit_exceeded',
            )
        )

    def test_try_give_licenses_for_partner_organization(self):
        # удачано выдаем партнерским организация лицензии
        OrganizationModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            update_data={
                'organization_type': organization_type.partner_organization,
                'partner_id': self.partner['id']
            }
        )

        update_data = {
            'user_limit': 100,
            'expires_at': datetime.date.today() + datetime.timedelta(days=10),
            'trial_status': trial_status.inapplicable,
        }

        OrganizationServiceModel(self.main_connection) \
            .filter(service_id=self.org_service['id']).update(**update_data)

        users = [{'type': 'user', 'id': self.create_user()['id']} for _ in range(3)]

        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=users,
            headers=self.token_auth_header,
            expected_code=200,
        )
        assert_that(
            response,
            has_length(3)
        )


class TestOrganizationChangeLicensesView_Disk(TestCase):
    def setUp(self):
        super(TestOrganizationChangeLicensesView_Disk, self).setUp()
        # заголовок для авторизации по токенам
        self.token_auth_header = get_auth_headers(as_uid=self.admin_uid)
        tvm.tickets['partner_disk'] = 'disk-tvm-ticket'
        self.service = ServiceModel(self.meta_connection).create(
            slug='disk',
            name='disk',
            client_id='client_id',
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )
        UpdateServicesInShardsCommand().try_run()
        self.org_id = self.organization['id']

    def test_put_get_simple(self):
        # выдадим лицензии пользователям
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 0)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 0)

        EventModel(self.main_connection).delete(force_remove_all=True)
        user1 = self.create_user()
        user2 = self.create_user()
        person_id = 1
        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@yandex-team.ru'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        with patch.object(app, 'partner_disk') as mocked_disk:
            mocked_disk.add_disk_space.return_value = {'product_id': 'intranet.yandex_directory.src.yandex_directory_1tb',
                                                       'service_id': 'dea2b5b08ec438d96b60357a87f078df'}

            response = self.put_json(
                '/subscription/services/%s/licenses/' % self.service['slug'],
                data=[
                    {
                        'type': 'user',
                        'id': user1['id']
                    },
                    {
                        'type': 'user',
                        'id': user2['id']
                    }
                ],
                headers=self.token_auth_header,
            )

        assert_that(
            response,
            contains_inanyorder(
                has_entries(
                    object_type='user',
                    object=has_entries(
                        id=user1['id'],
                    )
                ),
                has_entries(
                    object_type='user',
                    object=has_entries(
                        id=user2['id'],
                    )
                )
            )
        )

        # проверим, что обновилась таблица с лицензиями и у всех пользователей есть лицензии
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 2)

        # в таблице с платежной информацией также 2 записи
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 2)

        # сгенерировалось событие
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.org_id,
                    'name': event.service_license_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.org_id,
                    name=event.service_license_changed,
                    content={}
                )
            )
        )

        # создались задачи на поход в диск
        disk_tasks = TaskModel(self.main_connection).filter(
            task_name=AddSpacePartnerDiskTask.get_task_name(),
        ).all()

        assert_that(
            disk_tasks,
            contains_inanyorder(
                has_entries(
                    params=has_entries(
                        org_id=self.org_id,
                        uid=user1['id'],
                        resource_id=ANY,
                    )
                ),
                has_entries(
                    params=has_entries(
                        org_id=self.org_id,
                        uid=user2['id'],
                        resource_id=ANY,
                    )
                ),
            )
        )

        resp_get = self.get_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            headers=self.token_auth_header,
        )

        assert_that(resp_get, equal_to(response))

    def test_update_lic(self):
        # обновляем лицензии, должны создасться таски на поход в диск
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 0)
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 0)

        user1 = self.create_user()
        user2 = self.create_user()

        with patch.object(app, 'partner_disk') as mocked_disk:
            mocked_disk.add_disk_space.return_value = {'product_id': 'intranet.yandex_directory.src.yandex_directory_1tb',
                                                       'service_id': 'dea2b5b08ec438d96b60357a87f078df'}

            response = self.put_json(
                '/subscription/services/%s/licenses/' % self.service['slug'],
                data=[
                    {
                        'type': 'user',
                        'id': user1['id']
                    },
                    {
                        'type': 'user',
                        'id': user2['id']
                    }
                ],
                headers=self.token_auth_header,
            )

        # проверим, что обновилась таблица с лицензиями и у всех пользователей есть лицензии
        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 2)

        # в таблице с платежной информацией также 2 записи
        consumed_licenses = OrganizationLicenseConsumedInfoModel(self.main_connection).count()
        self.assertEqual(consumed_licenses, 2)

        # отбираем лицензию у пользователя user1
        with patch.object(app, 'partner_disk'):
            response = self.put_json(
                '/subscription/services/%s/licenses/' % self.service['slug'],
                data=[
                    {
                        'type': 'user',
                        'id': user2['id']
                    }
                ],
                headers=self.token_auth_header,
            )

        licenses = UserServiceLicenses(self.main_connection).count()
        self.assertEqual(licenses, 1)

        # создались задачи на поход в диск
        disk_tasks = TaskModel(self.main_connection).filter(
            task_name=DeleteSpacePartnerDiskTask.get_task_name(),
        ).all()
        assert_that(
            disk_tasks,
            contains_inanyorder(
                has_entries(
                    params=has_entries(
                        org_id=self.org_id,
                        uid=user1['id'],
                        resource_id=ANY,
                    )
                ),
            )
        )

        resp_get = self.get_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            headers=self.token_auth_header,
        )

        assert_that(resp_get, equal_to(response))

    def test_incorrect_containers(self):
        # на группы/отделы лицензии выдавать нельзя
        group = self.create_group()
        department = self.create_department()

        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': group['id']
                },
                {
                    'type': 'department',
                    'id': department['id']
                }
            ],
            headers=self.token_auth_header,
            expected_code=422,
        )

    def test_organization_with_debt(self):
        # организация с долгом не может выдать лицензии
        # включаем платный режим с задолженностью
        first_debt_act_date = datetime.datetime(year=2017, month=1, day=7).date()
        self.enable_paid_mode(first_debt_act_date=first_debt_act_date, balance=-100)

        user1 = self.create_user()

        # пытаемся выдать лицензии
        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'user',
                    'id': user1['id']
                },
            ],
            headers=self.token_auth_header,
            expected_code=422,
        )

        assert_that(
            response,
            equal_to({
                'message': 'Organization has debt',
                'code': 'organization_has_debt',
            })
        )


class TestOrganizationCalculatePriceServiceLicensesView(TestCase):
    def setUp(self):
        super(TestOrganizationCalculatePriceServiceLicensesView, self).setUp()
        # заголовок для авторизации по токенам
        self.token_auth_header = get_auth_headers(as_uid=self.admin_uid)
        self.service = ServiceModel(self.meta_connection).create(
            slug='tracker',
            name='tracker',
            client_id='client_id',
            paid_by_license=True,
            trial_period_months=2,
        )
        self.org_service = enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )

    def test_calculate_licences_price_for_group_with_admin_only(self):
        # Считаем цену сервиса для пустой группы с одним администратором
        # администраторы группы не должны считаться при выдаче лицензий
        user = self.create_user()
        group = self.create_group(
            admins=[
                user['id'],
            ],
            members=[],
        )
        tracker_price = 100

        fresh_group = GroupModel(self.main_connection).get(
            group_id=group['id'],
            org_id=self.organization['id'],
            fields=['members', 'admins'],
        )
        assert_that(
            len(fresh_group['members']),
            equal_to(0),
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': str(tracker_price)}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/services/%s/licenses/calculate-price/' % self.service['slug'],
                data=[
                    {
                        'type': 'group',
                        'id': group['id']
                    },
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

        assert_that(
            response,
            has_entries(
                per_user=0,
                total=0,
                user_count=0,
            )
        )

    def test_calculate_licences_price_simple(self):
        # Считаем цену сервиса для простых объектов без вложенности
        users = [self.create_user() for _ in range(6)]
        group1 = self.create_group(members=[
            {'type': 'user', 'object': users[0]},
            {'type': 'user', 'object': users[1]},
            {'type': 'user', 'object': users[2]},
            {'type': 'user', 'object': users[3]},
        ])
        user_count = 4

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '100'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/services/%s/licenses/calculate-price/' % self.service['slug'],
                data=[
                    {
                        'type': 'group',
                        'id': group1['id']
                    },
                    {
                        'type': 'user',
                        'id': users[4]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[5]['id']
                    }
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

        assert_that(
            response,
            has_entries(
                per_user=0,
                total=0,
                user_count=6,
            )
        )

    def test_calculate_licences_price_for_empty_data(self):
        # Проверяем, что вернется ноль, если считаем цену для пустого контейнера и у сервиса уже есть лицензии
        users = [self.create_user() for _ in range(2)]
        group1 = self.create_group(members=[
            {'type': 'user', 'object': users[0]},
            {'type': 'user', 'object': users[1]}
        ])

        person_id = 1
        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@yandex-team.ru'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        # выдаем лицензии на сервис
        response = self.put_json(
            '/subscription/services/%s/licenses/' % self.service['slug'],
            data=[
                {
                    'type': 'group',
                    'id': group1['id']
                }
            ],
            headers=self.token_auth_header,
        )

        assert_that(
            response,
            contains(
                has_entries(
                    object_type='group',
                    object=has_entries(
                        id=group1['id'],
                        name=group1['name'],
                        type=group1['type'],
                    )
                )
            )
        )
        assert_that(
            UserServiceLicenses(self.main_connection).count(),
            equal_to(2)
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '100'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/services/%s/licenses/calculate-price/' % self.service['slug'],
                data=[],
                headers=self.token_auth_header,
                expected_code=200,
            )

        assert_that(
            response,
            has_entries(
                per_user=0,
                total=0,
                user_count=0,
            )
        )

    @pytest.mark.skip('DIR-8844')
    def test_calculate_licences_price_with_promocode(self):
        # Считаем цену сервиса для простых объектов без вложенности с промокодом
        promocode_product_id = 12345
        promocode_price = 500
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=3000, month=1, day=1),
            expires_at=datetime.date(year=3000, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'tracker': {
                    10: promocode_product_id,
                },
            },
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        user_count = 6
        users = [self.create_user() for _ in range(user_count)]
        group1 = self.create_group(members=[
            {'type': 'user', 'object': users[0]},
            {'type': 'user', 'object': users[1]}
        ])

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '100'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': str(promocode_price)}],
                'ProductID': promocode_product_id,
            },
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/services/%s/licenses/calculate-price/' % self.service['slug'],
                data=[
                    {
                        'type': 'group',
                        'id': group1['id']
                    },
                    {
                        'type': 'user',
                        'id': users[2]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[3]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[4]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[5]['id']
                    },
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

        assert_that(
            response,
            has_entries(
                per_user=100,
                per_user_with_discount=promocode_price,
                total=user_count * 100,
                total_with_discount=promocode_price * user_count,
                user_count=user_count,
            )
        )

    @override_settings(NEW_TRACKER_PRICING=True)
    def test_calculate_licences_price_with_promocode_new_pricing(self):
        # Считаем цену сервиса для простых объектов без вложенности с промокодом
        promocode_product_id = 12345
        promocode_price = 500
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=3000, month=1, day=1),
            expires_at=datetime.date(year=3000, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'tracker': {
                    1: promocode_product_id,
                },
            },
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        user_count = 6
        users = [self.create_user() for _ in range(user_count)]
        group1 = self.create_group(members=[
            {'type': 'user', 'object': users[0]},
            {'type': 'user', 'object': users[1]}
        ])

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '1'}],
                'ProductID': app.config['TRACKER_PRODUCT_ID_1']
            },
            {
                'Prices': [{'Price': '100'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': str(promocode_price)}],
                'ProductID': promocode_product_id,
            },
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/services/%s/licenses/calculate-price/' % self.service['slug'],
                data=[
                    {
                        'type': 'group',
                        'id': group1['id']
                    },
                    {
                        'type': 'user',
                        'id': users[2]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[3]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[4]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[5]['id']
                    },
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

    @pytest.mark.skip('DIR-8844')
    def test_calculate_licences_price_with_free_promocode(self):
        # Считаем цену сервиса для простых объектов с бесплатным промокодом
        promocode = PromocodeModel(self.meta_connection).create(
            id='TRACKER_FREE',
            activate_before=datetime.date(year=3000, month=1, day=1),
            expires_at=datetime.date(year=3000, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'tracker': {
                    10: app.config['PRODUCT_ID_FREE'],
                },
            },
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        user_count = 6
        users = [self.create_user() for _ in range(user_count)]
        group1 = self.create_group(members=[
            {'type': 'user', 'object': users[0]},
            {'type': 'user', 'object': users[1]}
        ])

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '100'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            },
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/services/%s/licenses/calculate-price/' % self.service['slug'],
                data=[
                    {
                        'type': 'group',
                        'id': group1['id']
                    },
                    {
                        'type': 'user',
                        'id': users[2]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[3]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[4]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[5]['id']
                    },
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

        assert_that(
            response,
            has_entries(
                per_user=100,
                per_user_with_discount=0,
                total=user_count * 100,
                total_with_discount=0,
                user_count=user_count,
            )
        )

    def test_calculate_licences_for_one_user_and_group_with_this_user(self):
        # если попросили посчитать для одного пользователя и одной группы, в участниках которой только этот пользователь,
        # мы считаем что это один и тот же человек
        user = self.create_user()
        group = self.create_group(members=[
            {'type': 'user', 'object': user},
        ])

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '100'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/services/%s/licenses/calculate-price/' % self.service['slug'],
                data=[
                    {
                        'type': 'group',
                        'id': group['id']
                    },
                    {
                        'type': 'user',
                        'id': user['id']
                    },
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

        assert_that(
            response,
            has_entries(
                per_user=0,
                total=0,
                user_count=1,
            )
        )

    @pytest.mark.skip('DIR-8844')
    def test_calculate_licences_price_all_objects(self):
        # Считаем цену сервиса для всех типов объектов и их потомков
        users = [self.create_user() for _ in range(10)]
        members = []
        for user in users:
            members.append({'type': 'user', 'object': user})
        group1 = self.create_group(members=members)
        department1 = self.create_department()
        user11, user12 = self.create_user(department1['id']), self.create_user(department1['id'])

        members = [
            {'type': 'user', 'object': users[1]},
            {'type': 'user', 'object': users[2]},
            {'type': 'user', 'object': users[3]},
            {'type': 'user', 'object': user11},
            {'type': 'group', 'object': group1}
        ]
        group2 = self.create_group(members=members)

        department2 = self.create_department(
            parent_id=department1['id']
        )
        user13 = self.create_user(department2['id'])

        data = [
            {
                'type': 'group',
                'id': group2['id'],
            },
            {
                'type': 'department',
                'id': department1['id'],
            },
            {
                'type': 'user',
                'id': self.user['id'],
            },
            {
                'type': 'user',
                'id': users[0]['id'],
            },
            {
                'type': 'group',
                'id': group1['id'],
            }
        ]

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '50'}],
                'ProductID': app.config['BILLING_TRACKER_HUNDRED_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/services/%s/licenses/calculate-price/' % self.service['slug'],
                data=data,
                headers=self.token_auth_header,
                expected_code=200,
            )

        assert_that(
            response,
            has_entries(
                per_user=50,
                total=700,
                user_count=14,
            )
        )

    def test_calculate_licences_price_with_internal_promocode(self):
        # проверяем, что цена с внутренним промокодом отдается в обычном поле per_user
        promocode = PromocodeModel(self.meta_connection).create(
            id='TRACKER_FREE',
            activate_before=datetime.date(year=3000, month=1, day=1),
            expires_at=datetime.date(year=3000, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'tracker': {
                    10: app.config['PRODUCT_ID_FREE'],
                },
            },
            promocode_type=promocode_type.internal,
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        user_count = 4
        users = [self.create_user() for _ in range(user_count)]
        group1 = self.create_group(members=[
            {'type': 'user', 'object': users[0]},
            {'type': 'user', 'object': users[1]}
        ])

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '100'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            },
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = self.post_json(
                '/subscription/services/%s/licenses/calculate-price/' % self.service['slug'],
                data=[
                    {
                        'type': 'group',
                        'id': group1['id']
                    },
                    {
                        'type': 'user',
                        'id': users[2]['id']
                    },
                    {
                        'type': 'user',
                        'id': users[3]['id']
                    }
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

        assert_that(
            response,
            has_entries(
                per_user=0,
                per_user_with_discount=None,
                total=0,
                total_with_discount=None,
                user_count=user_count,
                promocode=None,
            )
        )


class TestOrganizationPromocodeActivateView(TestCase):
    def test_should_return_error_for_unknown_promocode(self):
        response = self.post_json(
            '/subscription/promocodes/activate/',
            data={
                'id': 'unknown',
            },
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='invalid_promocode',
                message='Invalid promo code',
            ),
        )

    def test_should_return_error_for_expired_promocode(self):
        # если промокод истек - вернем 422
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=3000, month=1, day=1),
            expires_at=datetime.date(year=1000, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )

        response = self.post_json(
            '/subscription/promocodes/activate/',
            data={
                'id': promocode['id'],
            },
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='expired_promocode',
                message='Expired promo code',
            ),
        )

    def test_should_not_return_error_for_expired_promocode_if_valid_for(self):
        # если промокод истек, но указан  valid_for - игнорируем это
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=3000, month=1, day=1),
            expires_at=datetime.date(year=1000, month=1, day=1),
            valid_for=10,
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )

        response = self.post_json(
            '/subscription/promocodes/activate/',
            data={
                'id': promocode['id'],
            },
            expected_code=200,
        )
        assert_that(
            response,
            has_entries(
                id=promocode['id'],
                expires=format_date(utcnow() + datetime.timedelta(10), only_date=True),
            ),
        )

    def test_should_return_error_for_expired_by_activation_date_promocode(self):
        # если истекла дата до которой надо активировать промокод - вернем 422
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=1000, month=1, day=1),
            expires_at=datetime.date(year=3000, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )

        response = self.post_json(
            '/subscription/promocodes/activate/',
            data={
                'id': promocode['id'],
            },
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='expired_promocode',
                message='Expired promo code',
            ),
        )

    def test_should_activate_promocode(self):
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=3000, month=1, day=1),
            expires_at=datetime.date(year=3000, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )

        response = self.post_json(
            '/subscription/promocodes/activate/',
            data={
                'id': promocode['id'],
            },
            expected_code=200,
        )
        assert_that(
            response,
            has_entries(
                id=promocode['id'],
                expires=format_date(promocode['expires_at']),
            ),
        )

    def test_outer_admin_can_activate_promocode(self):
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=3000, month=1, day=1),
            expires_at=datetime.date(year=3000, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
        )

        response = self.post_json(
            '/subscription/promocodes/activate/',
            data={
                'id': promocode['id'],
            },
            headers=get_auth_headers(as_uid=self.outer_admin['id']),
            expected_code=200,
        )
        assert_that(
            response,
            has_entries(
                id=promocode['id'],
                expires=format_date(promocode['expires_at']),
            ),
        )

    def test_should_return_error_for_internal_promocode(self):
        # нельзя активировать внутренние промокоды с помощью портальной ручки
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=3000, month=1, day=1),
            expires_at=datetime.date(year=3000, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
            promocode_type=promocode_type.internal
        )

        response = self.post_json(
            '/subscription/promocodes/activate/',
            data={
                'id': promocode['id'],
            },
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='invalid_promocode',
                message='Invalid promo code',
            ),
        )

    def test_activate_duplicate_promocode(self):
        # проверим, что если повторно активировать промокод для организации, то лимит для промокода не уменьшается
        promocode_id = 'connect_promo'
        activation_limit = 10

        promocode = PromocodeModel(self.meta_connection).create(
            id=promocode_id,
            activate_before=datetime.date(year=3000, month=1, day=1),
            expires_at=datetime.date(year=3000, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: 12345,
                },
            },
            activation_limit=activation_limit,
        )

        # активируем промокод и проверим, что лимит активаций для промокода уменьшился на один
        self.post_json(
            '/subscription/promocodes/activate/',
            data={
                'id': promocode['id'],
            },
            expected_code=200,
        )
        promocode = PromocodeModel(self.meta_connection).filter(id=promocode_id).one()
        assert promocode['activation_limit'] == activation_limit - 1

        # активируем еще раз и проверим, что лимит активаций для промокода не уменьшился в этот раз
        self.post_json(
            '/subscription/promocodes/activate/',
            data={
                'id': promocode['id'],
            },
            expected_code=200,
        )
        promocode = PromocodeModel(self.meta_connection).filter(id=promocode_id).one()
        assert promocode['activation_limit'] == activation_limit - 1


class Test__get_users_count_from_relations(TestCase):
    def test_get_users_count_from_relations_with_all_objects(self):
        users = [self.create_user() for _ in range(4)]
        group1 = self.create_group(members=[
            {'type': 'user', 'object': users[0]},
            {'type': 'user', 'object': users[1]}
        ])
        department1 = self.create_department()
        user5, user6 = self.create_user(department1['id']), self.create_user(department1['id'])

        members = [
            {'type': 'user', 'object': users[1]},
            {'type': 'user', 'object': users[2]},
            {'type': 'user', 'object': users[3]},
            {'type': 'user', 'object': user5},
            {'type': 'group', 'object': group1}
        ]
        group2 = self.create_group(members=members)

        department2 = self.create_department(
            parent_id=department1['id']
        )
        user7 = self.create_user(department2['id'])

        relations = [
            {
                'name': 'member',
                'group_id': group2['id'],
            },
            {
                'name': 'member',
                'department_id': department1['id'],
            },
            {
                'name': 'member',
                'user_id': self.user['id'],
            },
            {
                'name': 'member',
                'user_id': users[0]['id'],
            },
            {
                'name': 'member',
                'group_id': group1['id'],
            }
        ]
        users_count = get_users_count_from_relations(
            self.main_connection,
            self.organization['id'],
            relations=relations,
        )
        assert_that(
            users_count,
            equal_to(8)
        )

    def test_get_users_count_for_different_organizations(self):
        # пользователи в разных организациях должны считаться корректно
        users = [self.create_user() for _ in range(3)]
        members = [
            {'type': 'user', 'object': users[0]},
            {'type': 'user', 'object': users[1]},
            {'type': 'user', 'object': users[2]},
        ]
        group = self.create_group(id=15, members=members)
        department = self.create_department(id=45)
        self.create_user(department['id'])
        relations = [
            {
                'name': 'member',
                'group_id': group['id'],
            },
            {
                'name': 'member',
                'department_id': department['id'],
            }
        ]

        # создадим вторую организацию
        my_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='my-org'
        )['organization']

        users_my_org = [self.create_user(org_id=my_organization['id']) for _ in range(2)]
        members = [
            {'type': 'user', 'object': users_my_org[0]},
            {'type': 'user', 'object': users_my_org[1]},
        ]
        # создадим группу и отдел с такими же id, как в первой организации
        my_group = self.create_group(
            org_id=my_organization['id'],
            id=15,
            members=members,
            author_id=users_my_org[0]['id'],
        )
        my_department = self.create_department(org_id=my_organization['id'], id=45)
        my_org_relations = [
            {
                'name': 'member',
                'group_id': my_group['id'],
            },
            {
                'name': 'member',
                'department_id': my_department['id'],
            }
        ]

        users_count = get_users_count_from_relations(
            self.main_connection,
            self.organization['id'],
            relations=relations
        )
        assert_that(
            users_count,
            equal_to(4)
        )

        users_count_my_org = get_users_count_from_relations(
            self.main_connection,
            my_organization['id'],
            relations=my_org_relations
        )
        assert_that(
            users_count_my_org,
            equal_to(2)
        )


class TestOrganizationRequestLicensesView(TestCase):
    def setUp(self):
        super(TestOrganizationRequestLicensesView, self).setUp()
        self.service = ServiceModel(self.meta_connection).create(
            slug='tracker',
            name='tracker',
            client_id='client_id',
            paid_by_license=True,
        )
        # Обновляем сервисы на шарде.
        UpdateServicesInShardsCommand().try_run()

        self.org_service = enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )

    def test_create_request(self):
        # создаем заявки на лицензии
        assert_that(
            RequestedUserServiceLicenses(self.main_connection).count(),
            equal_to(0)
        )
        new_group = self.create_group()
        new_dep = self.create_department()
        new_user = self.create_user()

        response = self.post_json(
            '/subscription/services/tracker/licenses/request/',
            data={
                'objects':
                    [
                        {
                            'type': 'group',
                            'id': new_group['id']
                        },
                        {
                            'type': 'user',
                            'id': new_user['id']
                        },
                        {
                            'type': 'department',
                            'id': new_dep['id']
                        }
                    ],
                'comment': 'some text',
            },
            headers=get_auth_headers(as_uid=self.user['id']),
            expected_code=200,
        )

        requested_licenses = RequestedUserServiceLicenses(self.main_connection).find()
        assert_that(
            len(requested_licenses),
            equal_to(3)
        )
        assert_that(
            requested_licenses,
            contains_inanyorder(
                has_entries(
                    department_id=None,
                    group_id=None,
                    user_id=new_user['id'],
                    org_id=self.organization['id'],
                    service_slug='tracker',
                    author_id=self.user['id'],
                    created_at=not_none(),
                    comment='some text',
                ),
                has_entries(
                    department_id=new_dep['id'],
                    group_id=None,
                    user_id=None,
                    org_id=self.organization['id'],
                    service_slug='tracker',
                    author_id=self.user['id'],
                    created_at=not_none(),
                    comment='some text',
                ),
                has_entries(
                    department_id=None,
                    group_id=new_group['id'],
                    user_id=None,
                    org_id=self.organization['id'],
                    service_slug='tracker',
                    author_id=self.user['id'],
                    created_at=not_none(),
                    comment='some text',
                ),
            )
        )

    def test_get_requests(self):
        # получем все заявки на лицензии сервиса
        users = [self.create_user() for _ in range(5)]
        for user in users:
            self.post_json(
                '/subscription/services/tracker/licenses/request/',
                data={'objects': [{
                    'type': 'user',
                    'id': user['id']
                }]},
                headers=get_auth_headers(as_uid=self.user['id']),
                expected_code=200,
            )

        # проверим, что повторное создание заявки ничего не ломает и новую завяку не создает
        self.post_json(
            '/subscription/services/tracker/licenses/request/',
            data={'objects': [{
                'type': 'user',
                'id': users[0]['id']
            }]},
            headers=get_auth_headers(as_uid=self.user['id']),
            expected_code=200,
        )

        assert_that(
            RequestedUserServiceLicenses(self.main_connection).count(),
            equal_to(5)
        )
        response = self.get_json(
            '/subscription/services/tracker/licenses/request/',
            headers=get_auth_headers(as_uid=self.user['id']),
            expected_code=200,
        )['result']

        assert_that(
            len(response),
            equal_to(5)
        )

        assert_that(
            response[0],
            has_entries(
                object=has_entries(
                    id=users[0]['id'],
                ),
                object_type='user',
                service_slug='tracker',
            )
        )

        fields = ['user.nickname', 'author.nickname', 'user.position', 'author.position',
                  'created_at', 'comment', 'service_slug']
        lic_requests = RequestedUserServiceLicenses(self.main_connection).find(
            fields=fields,
            filter_data={'user_id': users[1]['id']},
        )
        expected_response = [prepare_requests_for_licenses(req) for req in lic_requests]
        for result in expected_response:
            result['object']['external'] = False

        response = self.get_json(
            '/subscription/services/tracker/licenses/request/?user_id={}&fields={}'.format(
                users[1]['id'],
                ','.join(fields)
            ),
            headers=get_auth_headers(as_uid=self.user['id']),
            expected_code=200,
        )['result']

        assert_that(
            response,
            equal_to(expected_response)
        )

        assert_that(
            response[0],
            has_entries(
                object=has_entries(
                    id=users[1]['id'],
                    nickname=users[1]['nickname'],
                    position=users[1]['position'],
                ),
                object_type='user',
                author=has_entries(
                    id=self.user['id'],
                    nickname=self.user['nickname'],
                    position=self.user['position'],
                ),
                comment=None,
                service_slug='tracker',
                created_at=format_datetime(lic_requests[0]['created_at']),
            )
        )


class TestOrganizationManageRequestedLicensesView(TestCase):
    def setUp(self):
        super(TestOrganizationManageRequestedLicensesView, self).setUp()
        self.token_auth_header = get_auth_headers(as_uid=self.admin_uid)
        self.service = ServiceModel(self.meta_connection).create(
            slug='tracker',
            name='tracker',
            client_id='client_id',
            paid_by_license=True,
        )
        # Обновляем сервисы на шарде.
        UpdateServicesInShardsCommand().try_run()
        self.org_service = enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )
        self.users = [self.create_user() for _ in range(5)]
        self.expected_response = []
        for user in self.users:
            RequestedUserServiceLicenses(self.main_connection).create(
                org_id=self.organization['id'],
                service_slug='tracker',
                user_id=user['id'],
                author_id=self.user['id'],
                department_id=None,
                group_id=None,
                comment=None,
            )
        lic_requests = RequestedUserServiceLicenses(self.main_connection).find(
            fields=[
                '*',
                'author.*',
                'group.*',
                'department.*',
                'user.*',
            ],
            order_by=['-created_at'],
        )
        self.expected_response = [prepare_requests_for_licenses(req) for req in lic_requests]

    def test_confirm_requested_licenses(self):
        # проверяем, что подтверждение заявок на лицензии создает лицензии и удаляет записи из таблицы заявок
        assert_that(
            RequestedUserServiceLicenses(self.main_connection).count(),
            equal_to(5)
        )

        assert_that(
            UserServiceLicenses(self.main_connection).count(),
            equal_to(0)
        )

        response = self.post_json(
            '/subscription/services/tracker/licenses/request/confirm/',
            data=[
                {
                    'type': 'user',
                    'id': self.users[0]['id']
                },
                {
                    'type': 'user',
                    'id': self.users[2]['id']
                },
            ],
            headers=self.token_auth_header,
            expected_code=200,
        )
        # лицензии создались
        licenses = UserServiceLicenses(self.main_connection).find(filter_data={'org_id': self.organization['id']})
        assert_that(
            len(licenses),
            equal_to(2)
        )

        assert_that(
            licenses,
            contains_inanyorder(
                has_entries(
                    user_id=self.users[0]['id'],
                    service_id=self.org_service['id']
                ),
                has_entries(
                    user_id=self.users[2]['id'],
                    service_id=self.org_service['id']
                )
            )
        )
        # в таблице с заявками осталось 3 записи
        assert_that(
            RequestedUserServiceLicenses(self.main_connection).count(),
            equal_to(3)
        )

        assert_that(
            response['result'],
            contains_inanyorder(
                self.expected_response[1],
                self.expected_response[3],
                self.expected_response[4]
            )
        )

    def test_confirm_requested_licenses_for_partner_organization(self):
        OrganizationModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            update_data={
                'organization_type': organization_type.partner_organization,
                'partner_id': self.partner['id']
            }
        )
        update_data = {
            'user_limit': 100,
            'expires_at': datetime.date.today() + datetime.timedelta(days=10),
            'trial_status': trial_status.inapplicable,
        }

        OrganizationServiceModel(self.main_connection) \
            .filter(service_id=self.org_service['id']).update(**update_data)
        self.test_confirm_requested_licenses()

    def test_requested_over_licenses_for_partner_organization(self):
        OrganizationModel(self.main_connection).update_one(
            org_id=self.organization['id'],
            update_data={
                'organization_type': organization_type.partner_organization,
                'partner_id': self.partner['id']
            }
        )
        update_data = {
            'user_limit': 1,
            'expires_at': datetime.date.today() + datetime.timedelta(days=10),
            'trial_status': trial_status.inapplicable,
        }

        OrganizationServiceModel(self.main_connection) \
            .filter(service_id=self.org_service['id']).update(**update_data)

        # пока еще есть лимит
        self.post_json(
            '/subscription/services/tracker/licenses/request/confirm/',
            data=[
                {
                    'type': 'user',
                    'id': self.users[0]['id']
                }
            ],
            headers=self.token_auth_header,
            expected_code=200,
        )

        # сверх лимита выдать нельзя
        response = self.post_json(
            '/subscription/services/tracker/licenses/request/confirm/',
            data=[

                {
                    'type': 'user',
                    'id': self.users[2]['id']
                },
            ],
            headers=self.token_auth_header,
            expected_code=422,
        )
        assert_that(
            response,
            has_entries(
                code='subscription.limit_exceeded',
            )
        )

    def test_deny_requested_licenses(self):
        # проверяем, что отклонение заявок на лицензии не лицензии и удаляет записи из таблицы заявок
        assert_that(
            RequestedUserServiceLicenses(self.main_connection).count(),
            equal_to(5)
        )

        assert_that(
            UserServiceLicenses(self.main_connection).count(),
            equal_to(0)
        )

        response = self.post_json(
            '/subscription/services/tracker/licenses/request/deny/',
            data=[
                {
                    'type': 'user',
                    'id': self.users[2]['id']
                },
                {
                    'type': 'user',
                    'id': self.users[3]['id']
                },
                {
                    'type': 'user',
                    'id': self.users[4]['id']
                },
            ],
            headers=self.token_auth_header,
            expected_code=200,
        )
        # лицензии не создались
        licenses = UserServiceLicenses(self.main_connection).find(filter_data={'org_id': self.organization['id']})
        assert_that(
            len(licenses),
            equal_to(0)
        )
        # в таблице с заявками осталось 2 записи
        license_requests = RequestedUserServiceLicenses(self.main_connection).find()
        assert_that(
            len(license_requests),
            equal_to(2)
        )
        assert_that(
            license_requests,
            contains_inanyorder(
                has_entries(
                    user_id=self.users[0]['id']
                ),
                has_entries(
                    user_id=self.users[1]['id']
                )
            )
        )

        assert_that(
            response['result'],
            contains_inanyorder(
                self.expected_response[0],
                self.expected_response[1]
            )
        )

    def test_incorrect_action(self):
        # проверяем, что вернется ошибка, если передан некоррекный action
        response = self.post_json(
            '/subscription/services/tracker/licenses/request/some_action/',
            data=[
                {
                    'type': 'user',
                    'id': self.users[2]['id']
                },
            ],
            headers=self.token_auth_header,
            expected_code=422,
        )

        assert_that(
            response,
            has_entries(
                code='invalid_value',
                params={'field': 'action'}
            )
        )


class TestRequestAccessToMetrikaResource(TestCase):
    def setUp(self):
        super(TestRequestAccessToMetrikaResource, self).setUp()
        self.service = ServiceModel(self.meta_connection).create(
            slug='metrika',
            name='metrika',
            client_id='client_id',
            paid_by_license=True,
        )
        # Обновляем сервисы на шарде.
        UpdateServicesInShardsCommand().try_run()

        self.responsible_user = self.create_user(
            uid=999111,
            email='responsible@user.com',
        )

        self.org_service = enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
            responsible_id=self.responsible_user['id'],
        )

        self.resource_1 = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='metrika',
            external_id='1',
        )
        self.resource_2 = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service='metrika',
            external_id='2',
        )

    def test_create_access_requests(self):
        assert_that(
            RequestedUserServiceLicenses(self.main_connection).count(),
            equal_to(0)
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.mailer.send') as mailer_send:
            self.post_json(
                '/subscription/services/metrika/licenses/request/',
                data={
                    'objects': [{
                        'type': 'user',
                        'id': self.user['id'],
                    }],
                    'comment': 'some text',
                    'resource_ids': [
                        self.resource_1['external_id'],
                        self.resource_2['external_id'],
                    ],
                },
                headers=get_auth_headers(as_uid=self.user['id']),
                expected_code=200,
            )

        requested_licenses = RequestedUserServiceLicenses(self.main_connection).find()
        assert_that(
            len(requested_licenses),
            equal_to(2)
        )

        assert_called_once(
            mailer_send,
            self.main_connection,
            app.config['SENDER_CAMPAIGN_SLUG']['REQUEST_ACCESS_TO_RESOURCE'],
            self.organization['id'],
            self.responsible_user['email'],
            {
                'lang': self.organization['language'],
                'tld': self.organization['tld'],
                'uid': self.user['id'],
                'resource_ids': [
                    self.resource_1['external_id'],
                    self.resource_2['external_id'],
                ],
            }
        )

        # проверим выдачу
        response = self.get_json(
            '/subscription/services/metrika/licenses/request/?resource_id={}'.format(self.resource_1['external_id']),
            headers=get_auth_headers(as_uid=self.user['id']),
            expected_code=200,
        )
        exp_response = [
            {
                'object_type': 'user',
                'object': {
                    'avatar_id': None,
                    'id': self.user['id'],
                    'name': {'first': {'ru': 'Vasya'}, 'last': {'ru': 'Pupkin'}},
                    'nickname': 'user',
                    'gender': 'male',
                    'external': True
                },
                'service_slug': 'metrika',
                'resource_id': self.resource_1['external_id'],
                'comment': None,
                'name': 'view'
            }
        ]

        assert_that(
            response['result'],
            equal_to(
                exp_response,
            ),
        )

    def test_create_with_user_in_another_org(self):
        assert_that(
            RequestedUserServiceLicenses(self.main_connection).count(),
            equal_to(0)
        )
        another_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']

        another_user = self.create_user(org_id=another_organization['id'])

        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.mailer.send') as mailer_send:
            self.post_json(
                '/subscription/services/metrika/licenses/request/',
                data={
                    'objects': [{
                        'type': 'user',
                        'id': another_user['id'],
                    }],
                    'comment': 'some text',
                    'resource_ids': [
                        self.resource_1['external_id'],
                        self.resource_2['external_id'],
                    ],
                },
                headers=get_auth_headers(as_uid=self.user['id']),
                expected_code=409,
            )

        requested_licenses = RequestedUserServiceLicenses(self.main_connection).find()
        assert_that(
            len(requested_licenses),
            equal_to(0)
        )

    def test_get_without_user_in_org(self):
        uid = 1111222333
        role_request = RequestedUserServiceLicenses(self.main_connection).create(
            org_id=self.organization['id'],
            service_slug='metrika',
            user_id=uid,
            group_id=None,
            department_id=None,
            comment='hello',
            author_id=self.user['id'],
            external_resource_id=self.resource_1['external_id'],
        )
        with patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_data_from_blackbox_by_uids') as mock_user_data:
            mock_user_data.return_value = {
                uid: {
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
                    'uid': uid,
                },
            }
            response = self.get_json(
                '/subscription/services/metrika/licenses/request/?resource_id={}&fields=user,user.position'.format(self.resource_1['external_id']),
                headers=get_auth_headers(as_uid=self.user['id']),
                expected_code=200,
            )

        exp_response = [
            {
                'object_type': 'user',
                'object': {
                    'gender': 'male',
                    'external': True,
                    'name': {'first': {'ru': 'user'},
                             'last': {'ru': 'user'}},
                    'nickname': 'only_passport_user@khunafin.xyz',
                    'avatar_id': '123445', 'id': uid,
                },
                'resource_id': self.resource_1['external_id'],
                'service_slug': 'metrika',
                'name': 'view',
                'comment': None,
            },
        ]

        assert_that(
            response['result'],
            equal_to(
                exp_response,
            ),
        )

    def test_create_duplicate_access_requests(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.mailer.send'):
            self.post_json(
                '/subscription/services/metrika/licenses/request/',
                data={
                    'objects': [{
                        'type': 'user',
                        'id': self.user['id'],
                    }],
                    'comment': 'some text',
                    'resource_ids': [
                        self.resource_1['external_id'],
                        self.resource_2['external_id'],
                    ],
                },
                headers=get_auth_headers(as_uid=self.user['id']),
                expected_code=200,
            )
            self.post_json(
                '/subscription/services/metrika/licenses/request/',
                data={
                    'objects': [{
                        'type': 'user',
                        'id': self.user['id'],
                    }],
                    'comment': 'some text',
                    'resource_ids': [
                        self.resource_1['external_id'],
                        self.resource_2['external_id'],
                    ],
                },
                headers=get_auth_headers(as_uid=self.user['id']),
                expected_code=200,
            )

    def test_create_with_not_enabled_service(self):
        OrganizationServiceModel(self.main_connection).update(
            filter_data={'service_id': self.service['id'], 'org_id': self.organization['id']},
            update_data={'enabled': False},
        )

        self.post_json(
            '/subscription/services/metrika/licenses/request/',
            data={
                'objects': [{
                    'type': 'user',
                    'id': self.user['id'],
                }],
                'comment': 'some text',
                'resource_ids': [
                        self.resource_1['external_id'],
                        self.resource_2['external_id'],
                    ],
            },
            headers=get_auth_headers(as_uid=self.user['id']),
            expected_code=403,
        )

    def test_create_without_resources_in_db(self):
        ResourceModel(self.main_connection).delete(
            filter_data={
                'org_id': self.organization['id'],
                'service': 'metrika',
            }
        )
        self.post_json(
            '/subscription/services/metrika/licenses/request/',
            data={
                'objects': [{
                    'type': 'user',
                    'id': self.user['id'],
                }],
                'comment': 'some text',
                'resource_ids': ['1', '2'],
            },
            headers=get_auth_headers(as_uid=self.user['id']),
            expected_code=404,
        )

    def test_create_access_request_with_resources_in_different_organizations(self):
        second_org = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_org'
        )

        ResourceModel(self.main_connection).update(
            update_data={'org_id': second_org['organization']['id']},
            filter_data={'id': self.resource_2['id']},
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.mailer.send'):
            self.post_json(
                '/subscription/services/metrika/licenses/request/',
                data={
                    'objects': [{
                        'type': 'user',
                        'id': self.user['id'],
                    }],
                    'comment': 'some text',
                    'resource_ids': [
                        self.resource_1['external_id'],
                        self.resource_2['external_id'],
                    ],
                },
                headers=get_auth_headers(as_uid=self.user['id']),
                expected_code=422,
            )


class TestManageRequestedLicensesToMetrika(TestCase):
    language = 'en'

    def setUp(self):
        super(TestManageRequestedLicensesToMetrika, self).setUp()
        self.token_auth_header = get_auth_headers(as_uid=self.admin_uid)
        self.service = ServiceModel(self.meta_connection).create(
            slug='metrika',
            name='metrika',
            client_id='client_id',
            paid_by_license=True,
        )

        # Обновляем сервисы на шарде.
        UpdateServicesInShardsCommand().try_run()
        self.org_service = enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )
        self.users = [self.create_user() for _ in range(2)]
        self.expected_response = []

        self.resource = ResourceModel(self.main_connection).create(
            org_id=self.organization['id'],
            service=self.service['slug'],
            external_id=111111,
        )

        for user in self.users:
            RequestedUserServiceLicenses(self.main_connection).create(
                org_id=self.organization['id'],
                service_slug=self.service['slug'],
                user_id=user['id'],
                author_id=self.user['id'],
                department_id=None,
                group_id=None,
                comment=None,
                external_resource_id=self.resource['external_id'],
            )

    def test_confirm(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.models.user.app.blackbox_instance.batch_userinfo') as get_batch_userinfo, \
                patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.mailer.send') as mailer_send, \
                patch('intranet.yandex_directory.src.yandex_directory.core.sms.tasks.app.send_sms') as send_sms:
            blackbox_user_info_list = [
                fake_userinfo(uid=self.users[0]['id'], default_email='user0@yandex.ru', language=None),
                fake_userinfo(uid=self.users[1]['id'], default_email='user1@yandex.ru'),
            ]

            get_batch_userinfo.return_value = blackbox_user_info_list

            self.post_json(
                '/subscription/services/metrika/licenses/request/confirm/?resource_id={resource_id}'.format(
                    resource_id=self.resource['external_id'],
                ),
                data=[
                    {
                        'type': 'user',
                        'id': self.users[0]['id']
                    }, {
                        'type': 'user',
                        'id': self.users[1]['id']
                    }
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

        for user in self.users:
            access_request = RequestedUserServiceLicenses(self.main_connection).filter(
                org_id=self.organization['id'],
                service_slug=self.service['slug'],
                user_id=user['id'],
                external_id=self.resource['external_id'],
            ).one()
            assert_that(access_request, equal_to(None))

            resource_relation = ResourceRelationModel(self.main_connection).filter(
                org_id=self.organization['id'],
                resource_id=self.resource['id'],
                name='view',
                user_id=user['id'],
            ).one()
            assert_that(resource_relation, not_none())

        actions = ActionModel(self.main_connection).filter(
            name='resource_relation_add',
            org_id=self.organization['id'],
        ).all()

        assert_that(len(actions), equal_to(len(self.users)))

        assert_that(
            mailer_send.call_args_list,
            contains_inanyorder(
                unittest.mock.call(
                    self.main_connection,
                    app.config['SENDER_CAMPAIGN_SLUG']['CONFIRM_ACCESS_TO_RESOURCE'],
                    self.organization['id'],
                    blackbox_user_info_list[0]['default_email'],
                    {
                        'lang': self.organization['language'],
                        'tld': self.organization['tld'],
                        'resource_id': self.resource['external_id'],
                    },
                ),
                unittest.mock.call(
                    self.main_connection,
                    app.config['SENDER_CAMPAIGN_SLUG']['CONFIRM_ACCESS_TO_RESOURCE'],
                    self.organization['id'],
                    blackbox_user_info_list[1]['default_email'],
                    {
                        'lang': blackbox_user_info_list[1]['fields']['language'],
                        'tld': self.organization['tld'],
                        'resource_id': self.resource['external_id'],
                    },
                ),
            ),
        )

        assert_that(
            send_sms.call_args_list,
            contains_inanyorder(
                unittest.mock.call(self.users[0]['id'], ANY),
                unittest.mock.call(self.users[1]['id'], ANY),
            )
        )

    def test_deny(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.models.user.app.blackbox_instance.batch_userinfo') as get_batch_userinfo, \
                patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.mailer.send') as mailer_send, \
                patch('intranet.yandex_directory.src.yandex_directory.core.sms.tasks.app.send_sms') as send_sms:
            blackbox_user_info_list = [
                fake_userinfo(uid=self.users[0]['id'], default_email='user0@yandex.ru'),
                fake_userinfo(uid=self.users[1]['id'], default_email='user1@yandex.ru'),
            ]

            get_batch_userinfo.return_value = blackbox_user_info_list

            self.post_json(
                '/subscription/services/metrika/licenses/request/deny/?resource_id={resource_id}'.format(
                    resource_id=self.resource['external_id'],
                ),
                data=[
                    {
                        'type': 'user',
                        'id': self.users[0]['id']
                    }, {
                        'type': 'user',
                        'id': self.users[1]['id']
                    }
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

        for user in self.users:
            access_request = RequestedUserServiceLicenses(self.main_connection).filter(
                org_id=self.organization['id'],
                service_slug=self.service['slug'],
                user_id=user['id'],
                external_id=self.resource['external_id'],
            ).one()
            assert_that(access_request, equal_to(None))

            resource_relation = ResourceRelationModel(self.main_connection).filter(
                org_id=self.organization['id'],
                resource_id=self.resource['id'],
                name='view',
                user_id=user['id'],
            ).one()
            assert_that(resource_relation, equal_to(None))

        assert_that(
            mailer_send.call_args_list,
            contains_inanyorder(
                unittest.mock.call(
                    self.main_connection,
                    app.config['SENDER_CAMPAIGN_SLUG']['DENY_ACCESS_TO_RESOURCE'],
                    self.organization['id'],
                    blackbox_user_info_list[0]['default_email'],
                    {
                        'lang': blackbox_user_info_list[0]['fields']['language'],
                        'tld': self.organization['tld'],
                        'resource_id': self.resource['external_id'],
                    },
                ),
                unittest.mock.call(
                    self.main_connection,
                    app.config['SENDER_CAMPAIGN_SLUG']['DENY_ACCESS_TO_RESOURCE'],
                    self.organization['id'],
                    blackbox_user_info_list[1]['default_email'],
                    {
                        'lang': blackbox_user_info_list[1]['fields']['language'],
                        'tld': self.organization['tld'],
                        'resource_id': self.resource['external_id'],
                    },
                ),
            ),
        )

        assert_that(
            send_sms.call_args_list,
            contains_inanyorder(
                unittest.mock.call(self.users[0]['id'], ANY),
                unittest.mock.call(self.users[1]['id'], ANY),
            )
        )

    def test_confirm_with_new_user(self):
        new_user_uid = 111222333
        RequestedUserServiceLicenses(self.main_connection).create(
            org_id=self.organization['id'],
            service_slug=self.service['slug'],
            user_id=new_user_uid,
            author_id=self.user['id'],
            department_id=None,
            group_id=None,
            comment=None,
            external_resource_id=self.resource['external_id'],
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.models.user.app.blackbox_instance.batch_userinfo') as get_batch_userinfo, \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_info_from_blackbox') as userinfo, \
                patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.mailer.send') as mailer_send, \
                patch('intranet.yandex_directory.src.yandex_directory.core.sms.tasks.app.send_sms') as send_sms:
            blackbox_user_info_list = [
                fake_userinfo(uid=self.users[0]['id'], default_email='user0@yandex.ru'),
                fake_userinfo(uid=new_user_uid, default_email='new_user@yandex.ru'),
            ]

            userinfo.return_value = (
                blackbox_user_info_list[0]['fields']['login'],
                blackbox_user_info_list[0]['fields']['first_name'],
                blackbox_user_info_list[0]['fields']['last_name'],
                0,
                None,
                'new_user@yandex.ru',
                None,
            )

            app.blackbox_instance.userinfo.return_value = fake_userinfo(
                uid=new_user_uid,
            )

            get_batch_userinfo.return_value = blackbox_user_info_list

            self.post_json(
                '/subscription/services/metrika/licenses/request/confirm/?resource_id={resource_id}'.format(
                    resource_id=self.resource['external_id'],
                ),
                data=[
                    {
                        'type': 'user',
                        'id': self.users[0]['id']
                    }, {
                        'type': 'user',
                        'id': new_user_uid,
                    }
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

        for uid in [self.users[0]['id'], new_user_uid]:
            access_request = RequestedUserServiceLicenses(self.main_connection).filter(
                org_id=self.organization['id'],
                service_slug=self.service['slug'],
                user_id=uid,
                external_id=self.resource['external_id'],
            ).one()
            assert_that(access_request, equal_to(None))

            resource_relation = ResourceRelationModel(self.main_connection).filter(
                org_id=self.organization['id'],
                resource_id=self.resource['id'],
                name='view',
                user_id=uid,
            ).one()
            assert_that(resource_relation, not_none())

        new_user = UserModel(self.main_connection).filter(id=new_user_uid, org_id=self.organization['id']).one()
        assert_that(new_user, not_none())

        assert_that(
            mailer_send.call_args_list,
            contains_inanyorder(
                unittest.mock.call(
                    self.main_connection,
                    app.config['SENDER_CAMPAIGN_SLUG']['CONFIRM_ACCESS_TO_RESOURCE'],
                    self.organization['id'],
                    blackbox_user_info_list[0]['default_email'],
                    {
                        'lang': blackbox_user_info_list[0]['fields']['language'],
                        'tld': self.organization['tld'],
                        'resource_id': self.resource['external_id'],
                    },
                ),
                unittest.mock.call(
                    self.main_connection,
                    app.config['SENDER_CAMPAIGN_SLUG']['CONFIRM_ACCESS_TO_RESOURCE'],
                    self.organization['id'],
                    blackbox_user_info_list[1]['default_email'],
                    {
                        'lang': blackbox_user_info_list[1]['fields']['language'],
                        'tld': self.organization['tld'],
                        'resource_id': self.resource['external_id'],
                    },
                ),
            ),
        )

        assert_that(
            send_sms.call_args_list,
            contains_inanyorder(
                unittest.mock.call(self.users[0]['id'], ANY),
                unittest.mock.call(new_user_uid, ANY),
            )
        )


    def test_deny_with_new_user(self):
        new_user_uid = 111222333
        RequestedUserServiceLicenses(self.main_connection).create(
            org_id=self.organization['id'],
            service_slug=self.service['slug'],
            user_id=new_user_uid,
            author_id=self.user['id'],
            department_id=None,
            group_id=None,
            comment=None,
            external_resource_id=self.resource['external_id'],
        )

        with patch('intranet.yandex_directory.src.yandex_directory.core.models.user.app.blackbox_instance.batch_userinfo') as get_batch_userinfo, \
                patch('intranet.yandex_directory.src.yandex_directory.core.utils.get_user_info_from_blackbox') as userinfo, \
                patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.mailer.send') as mailer_send, \
                patch('intranet.yandex_directory.src.yandex_directory.core.sms.tasks.app.send_sms') as send_sms:
            blackbox_user_info_list = [
                fake_userinfo(uid=self.users[0]['id'], default_email='user0@yandex.ru'),
                fake_userinfo(uid=new_user_uid, default_email='new_user@yandex.ru'),
            ]

            userinfo.return_value = (
                blackbox_user_info_list[0]['fields']['login'],
                blackbox_user_info_list[0]['fields']['first_name'],
                blackbox_user_info_list[0]['fields']['last_name'],
                0,
                None,
                None,
            )

            app.blackbox_instance.userinfo.return_value = fake_userinfo(
                uid=new_user_uid,
            )

            get_batch_userinfo.return_value = blackbox_user_info_list

            self.post_json(
                '/subscription/services/metrika/licenses/request/deny/?resource_id={resource_id}'.format(
                    resource_id=self.resource['external_id'],
                ),
                data=[
                    {
                        'type': 'user',
                        'id': self.users[0]['id']
                    }, {
                        'type': 'user',
                        'id': new_user_uid,
                    }
                ],
                headers=self.token_auth_header,
                expected_code=200,
            )

        for uid in [self.users[0]['id'], new_user_uid]:
            access_request = RequestedUserServiceLicenses(self.main_connection).filter(
                org_id=self.organization['id'],
                service_slug=self.service['slug'],
                user_id=uid,
                external_id=self.resource['external_id'],
            ).one()
            assert_that(access_request, equal_to(None))

            resource_relation = ResourceRelationModel(self.main_connection).filter(
                org_id=self.organization['id'],
                resource_id=self.resource['id'],
                name='view',
                user_id=uid,
            ).one()
            assert_that(resource_relation, equal_to(None))

        new_user = UserModel(self.main_connection).filter(id=new_user_uid, org_id=self.organization['id']).one()
        assert_that(new_user, equal_to(None))

        assert_that(
            mailer_send.call_args_list[0][0],
            equal_to(
                (
                    self.main_connection,
                    app.config['SENDER_CAMPAIGN_SLUG']['DENY_ACCESS_TO_RESOURCE'],
                    self.organization['id'],
                    blackbox_user_info_list[0]['default_email'],
                    {
                        'lang': blackbox_user_info_list[0]['fields']['language'],
                        'tld': self.organization['tld'],
                        'resource_id': self.resource['external_id'],
                    },
                ),
            ),
        )

        assert_that(
            mailer_send.call_args_list[1][0],
            equal_to(
                (
                    self.main_connection,
                    app.config['SENDER_CAMPAIGN_SLUG']['DENY_ACCESS_TO_RESOURCE'],
                    self.organization['id'],
                    blackbox_user_info_list[1]['default_email'],
                    {
                        'lang': blackbox_user_info_list[1]['fields']['language'],
                        'tld': self.organization['tld'],
                        'resource_id': self.resource['external_id'],
                    },
                ),
            ),
        )

        assert_that(
            send_sms.call_args_list,
            contains_inanyorder(
                unittest.mock.call(self.users[0]['id'], ANY),
                unittest.mock.call(new_user_uid, ANY),
            )
        )



    def test_with_non_existent_user_in_blackbox(self):
        with patch('intranet.yandex_directory.src.yandex_directory.core.models.user.app.blackbox_instance.batch_userinfo') as get_batch_userinfo:
            get_batch_userinfo.return_value = []

            self.post_json(
                '/subscription/services/metrika/licenses/request/confirm/?resource_id={resource_id}'.format(
                    resource_id=self.resource['external_id'],
                ),
                data=[
                    {
                        'type': 'user',
                        'id': self.users[0]['id']
                    }, {
                        'type': 'user',
                        'id': self.users[1]['id']
                    }
                ],
                headers=self.token_auth_header,
                expected_code=404,
            )

    def test_with_non_existent_resource_id(self):
        self.post_json(
            '/subscription/services/metrika/licenses/request/confirm/?resource_id={resource_id}'.format(
                resource_id='non-existent-resource-id',
            ),
            data=[
                {
                    'type': 'user',
                    'id': self.users[0]['id']
                }, {
                    'type': 'user',
                    'id': self.users[1]['id']
                }
            ],
            headers=self.token_auth_header,
            expected_code=404,
        )

    def test_create_with_not_enabled_service(self):
        OrganizationServiceModel(self.main_connection).update(
            filter_data={'service_id': self.service['id'], 'org_id': self.organization['id']},
            update_data={'enabled': False},
        )

        self.post_json(
            '/subscription/services/metrika/licenses/request/confirm/?resource_id={resource_id}'.format(
                resource_id='non-existent-resource-id',
            ),
            data=[
                {
                    'type': 'user',
                    'id': self.users[0]['id']
                }, {
                    'type': 'user',
                    'id': self.users[1]['id']
                }
            ],
            headers=self.token_auth_header,
            expected_code=403,
        )
