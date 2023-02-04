# -*- coding: utf-8 -*-
import datetime
from decimal import Decimal
from xmlrpc.client import Fault

import pytest
import pytz
from dateutil.relativedelta import relativedelta
from flask import g
from hamcrest import (
    assert_that,
    equal_to,
    contains_inanyorder,
    contains,
    calling,
    raises,
    has_entries,
    none,
    not_none,
    is_,
)
from unittest.mock import patch, Mock
from sqlalchemy.exc import IntegrityError

from testutils import (
    TestCase as BaseTestCase,
    assert_not_called,
    is_same,
    create_organization,
    override_settings,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.common.billing.client import (
    BANK_IDS,
    WORKSPACE_SERVICE_ID,
)
from intranet.yandex_directory.src.yandex_directory.common.db import mogrify
from intranet.yandex_directory.src.yandex_directory.common.utils import (
    time_in_past,
    ensure_date,
    utcnow,
    Ignore,
)
from intranet.yandex_directory.src.yandex_directory.core.billing.tasks import CheckOrganizationBalanceTask
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShards
from intranet.yandex_directory.src.yandex_directory.core.events import event
from intranet.yandex_directory.src.yandex_directory.core.exceptions import (
    BillingInvalidField,
    BillingMissingField,
    BillingUidNotFoundInPassport,
    BillingClientIdMismatch,
)
from intranet.yandex_directory.src.yandex_directory.core.exceptions import (
    OrganizationIsWithoutContract,
    OrganizationHasDebt,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    trial_status,
    set_trial_status_to_expired,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    DepartmentModel,
    EventModel,
    OrganizationModel,
    OrganizationMetaModel,
    OrganizationRevisionCounterModel,
    OrganizationBillingInfoModel,
    OrganizationBillingConsumedInfoModel,
    UserModel,
    UserDismissedModel,
    ActionModel,
    ImageModel,
    OrganizationPromocodeModel,
    PromocodeModel,
    ServiceModel,
    OrganizationLicenseConsumedInfoModel,
    UserServiceLicenses,
    ExcludedShardModel,
)
from intranet.yandex_directory.src.yandex_directory.core.models.organization import (
    SEND_DEBT_MAIL_DAYS,
    get_price_and_product_id_for_service,
    check_has_debt,
)
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    OrganizationServiceModel,
    enable_service,
    disable_service,
)
from intranet.yandex_directory.src.yandex_directory.core.utils.robots import create_robot_for_service_and_org_id
from intranet.yandex_directory.src.yandex_directory import app


class TestCase(BaseTestCase):
    """ Отклюаем создание организации для всех тестов этого модуля
    """
    create_organization = False


class TestOrganizationModel_create(TestCase):
    def test_simple(self):
        name = {
            'ru': 'НеЯндекс',
            'en': 'NotYandex',
        }
        # return value
        organization = OrganizationModel(self.main_connection).create(
            id=123,
            name=name,
            language='ru',
            label='yandex-money',
            admin_uid=321
        )
        self.assertEqual(organization['id'], 123)
        self.assertEqual(organization['name'], name)
        self.assertEqual(organization['label'], 'yandex-money')
        self.assertEqual(organization['language'], 'ru')
        self.assertEqual(organization['admin_uid'], 321)
        self.assertEqual(organization['environment'], app.config['ENVIRONMENT'])

        # test data in database
        organization_from_db = OrganizationModel(self.main_connection).get(organization['id'])
        self.assertIsNotNone(organization_from_db)
        self.assertEqual(organization['id'], 123)
        self.assertEqual(organization['name'], name)
        self.assertEqual(organization['label'], 'yandex-money')
        self.assertEqual(organization['language'], 'ru')
        self.assertEqual(organization['admin_uid'], 321)
        self.assertEqual(organization['environment'], app.config['ENVIRONMENT'])

    def test_double_insert(self):
        name = {
            'ru': 'АльфаБанк',
            'en': 'AlfaBank',
        }
        organization = OrganizationModel(self.main_connection).create(
            id=123,
            name=name,
            label='alfa',
            language='ru',
            admin_uid=321
        )
        self.assertEqual(organization['id'], 123)

        with self.assertRaises(IntegrityError):
            OrganizationModel(self.main_connection).create(
                id=123,
                name=name,
                label='alfa',
                language='ru',
                admin_uid=14181,
            )


class TestOrganizationModel_exists(BaseTestCase):
    def test_existent(self):
        assert_that(
            OrganizationModel(self.main_connection)
                .exists(self.organization['id']),
            is_(True),
        )

    def test_non_existent(self):
        assert_that(
            OrganizationModel(self.main_connection).exists(100500),
            is_(False),
        )


class TestOrganizationModel_find(BaseTestCase):
    def test_find_by_id(self):
        yandex = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'НеЯндекс',
                'en': 'NotYandex'
            },
            label='not_yandex',
            language='ru',
            admin_uid=123
        )
        google = OrganizationModel(self.main_connection).create(
            id=2,
            name={
                'ru': 'Гугл',
                'en': 'Google'
            },
            label='google',
            language='en',
            admin_uid=321
        )

        response = OrganizationModel(self.main_connection).find({'id': google['id']})
        # Так как поле revision требует join, то
        # get не возвращает его по-умолчанию.
        del google['revision']
        # Это поле мы убрали в 0.185.5, но пока не удалили из базы,
        # чтобы не сломать прод при выкатывании в QA.
        # В 0.186 релизе надо будет убрать совсем.
        google.pop('company_logo', None)
        assert_that(
            response,
            contains(
                has_entries(google),
            )
        )

    def test_search(self):
        # поиск по пользователю
        organizations = OrganizationModel(self.main_connection).find({
            'text': self.user['nickname'][:-1],
            'type': 'internal_admin',
        })

        assert_that(
            organizations,
            contains(
                has_entries(id=self.organization['id'])
            )
        )

        # поиск label организации
        organizations = OrganizationModel(self.main_connection).find({
            'text': self.organization['label'][:-2]
        })
        assert_that(
            organizations,
            contains(
                has_entries(id=self.organization['id'])
            )
        )

        # поиск названию организации
        organizations = OrganizationModel(self.main_connection).find({
            'text': 'яндек'
        })
        assert_that(
            organizations,
            contains(
                has_entries(id=self.organization['id'])
            )
        )

        # поиск под домену
        organizations = OrganizationModel(self.main_connection).find({
            'text': self.organization_domain[:-2]
        })
        assert_that(
            organizations,
            contains(
                has_entries(id=self.organization['id'])
            )
        )


    def test_service_responsible(self):
        # Проверяем, что можно запросить поле services.responsible.name
        # и моделька отдаст нам id и name сотрудника.
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
            responsible_id=self.user['id'],
        )
        result = OrganizationModel(self.main_connection) \
                 .filter(id=self.organization['id']) \
                 .fields('services.responsible.name') \
                 .all()

        assert_that(
            result,
            contains(
                has_entries(
                    services=contains(
                        has_entries(
                            responsible=has_entries(
                                id=self.user['id'],
                                name=self.user['name'],
                            )
                        )
                    )
                )
            )
        )

    def test_service_without_responsible(self):
        # Проверяем, что можно запросить поле services.responsible.name
        # и моделька отдаст нам None если ответственного нет
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            self.service['slug'],
        )
        result = OrganizationModel(self.main_connection) \
                 .filter(id=self.organization['id']) \
                 .fields('services.responsible.name') \
                 .all()

        assert_that(
            result,
            contains(
                has_entries(
                    services=contains(
                        has_entries(
                            responsible=none(),
                        )
                    )
                )
            )
        )



    def test_service_with_root_departments(self):
        # Проверяем, что можно запросить поле root_departments
        org_id = self.organization['id']
        outstaff = DepartmentModel(self.main_connection).get_or_create_outstaff(org_id)
        usual_department = self.create_department()

        result = OrganizationModel(self.main_connection) \
                 .filter(id=self.organization['id']) \
                 .fields('root_departments.name') \
                 .all()

        assert_that(
            result,
            contains(
                has_entries(
                    root_departments=contains(
                        has_entries(
                            id=1,
                            name=has_entries(
                                en='All employees',
                            )
                        ),
                        has_entries(
                            id=outstaff['id'],
                            name=has_entries(
                                en='Outstaff',
                            )
                        ),
                    )
                )
            )
        )


class TestOrganizationModel_get(TestCase):
    def test_simple(self):
        model = OrganizationModel(self.main_connection)
        model.create(
            id=1,
            name={
                'ru': 'НеЯндекс',
                'en': 'NotYandex'
            },
            label='not_yandex',
            language='ru',
            admin_uid=123
        )
        google = model.create(
            id=2,
            name={
                'ru': 'Гугл',
                'en': 'Google'
            },
            label='google',
            language='en',
            admin_uid=321
        )

        from_db = model.get(google['id'])
        # Так как поле revision требует join, то
        # get не возвращает его по-умолчанию.
        del google['revision']
        # Это поле мы убрали в 0.185.5, но пока не удалили из базы,
        # чтобы не сломать прод при выкатывании в QA.
        # В 0.186 релизе надо будет убрать совсем.
        google.pop('company_logo', None)

        assert_that(
            from_db,
            has_entries(**google)
        )

    def test_get_should_retrieve_revision(self):
        # проверяем, что ревизия будет получена именно из счетчика, а не из пока что существующего поля revision
        organization_model = OrganizationModel(self.main_connection)
        yandex = organization_model.create(
            id=1,
            name={
                'ru': 'Еще один Яндекс',
                'en': 'Yet another Yandex'
            },
            label='test_yay_test',
            language='ru',
            admin_uid=123,
        )

        counter_model = OrganizationRevisionCounterModel(self.main_connection)
        for i in range(5):
            counter_model.increment_revision(org_id=yandex['id'])

        revision_from_counter = counter_model.get(id=yandex['id'])['revision']
        fresh_organization = organization_model.get(
            yandex['id'],
            fields=['revision'],
        )

        # ревизия организации должна быть равна ревизии счетчика
        assert_that(revision_from_counter, equal_to(fresh_organization['revision']))

    def test_has_owned_domains(self):
        meta_org = OrganizationMetaModel(self.meta_connection).create('testtest', 1, True)
        org = OrganizationModel(self.main_connection).create(
            id=meta_org['id'],
            name={
                'ru': 'НеЯндекс',
                'en': 'NotYandex'
            },
            label='not_yandex',
            language='ru',
            admin_uid=123
        )
        response = OrganizationModel(self.main_connection).filter(id=org['id']).fields('has_owned_domains').one()
        assert_that(
            response,
            has_entries(
                has_owned_domains=False,
            )
        )


class TestOrganizationMetaModel_create(TestCase):
    def test_simple(self):
        organization_meta = OrganizationMetaModel(self.meta_connection).create(
            label='not_yandex',
            shard=1,
        )
        self.assertIsNotNone(organization_meta.get('id'))
        self.assertEqual(organization_meta['label'], 'not_yandex')
        self.assertEqual(organization_meta['shard'], 1)

        # test data in database
        organization_meta_from_db = OrganizationMetaModel(self.meta_connection).get(organization_meta['id'])
        self.assertIsNotNone(organization_meta_from_db)
        self.assertEqual(organization_meta['id'], organization_meta['id'])
        self.assertEqual(organization_meta['label'], 'not_yandex')
        self.assertEqual(organization_meta['shard'], 1)


class TestOrganizationMetaModel_find(TestCase):
    def setUp(self):
        self.yandex = OrganizationMetaModel(self.meta_connection).create(
            label='not_yandex',
            shard=1,
            ready=True,
        )
        self.google = OrganizationMetaModel(self.meta_connection).create(
            label='google',
            shard=2,
            ready=False,
        )

    def test_find_by_label(self):
        response = OrganizationMetaModel(self.meta_connection).find({'label': 'google'})
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.google)

    def test_find_by_id(self):
        response = OrganizationMetaModel(self.meta_connection).find({'id': self.google['id']})
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.google)

    def test_find_by_ready(self):
        # в базе 2 организации
        # `not_yandex` - ready=True
        # `google` - ready=False
        # ищем с ready=False и ожидаем `google`
        response = OrganizationMetaModel(self.meta_connection).find({'ready': False})
        self.assertEqual(len(response), 1)
        self.assertEqual(response[0], self.google)


class TestOrganizationMetaModel_get(TestCase):

    def test_me(self):
        self.yandex = OrganizationMetaModel(self.meta_connection).create(
            label='not_yandex',
            shard=1,
        )
        self.google = OrganizationMetaModel(self.meta_connection).create(
            label='google',
            shard=2,
        )

        self.assertEqual(OrganizationMetaModel(self.meta_connection).get(id=self.google['id']), self.google)


class TestOrganizationMetaModel__get_shard_for_new_organization(TestCase):
    def test_get_shard_for_new_organization__raise_runtime_error(self):
        ExcludedShardModel(self.meta_connection).create(1)
        assert_that(
            calling(OrganizationMetaModel.get_shard_for_new_organization),
            raises(RuntimeError),
        )

    def test_get_shard_for_new_organization_type_portal(self):
        # тест для порталов DIR-5335
        # для организаций типа portal мы выбираем 4й шард
        with patch('intranet.yandex_directory.src.yandex_directory.common.db.get_shard_numbers', return_value=[1, 2, 3, 4]):
            with patch('intranet.yandex_directory.src.yandex_directory.core.models.organization.get_shards_with_weight',
                       return_value=[(1.0, 4)]):
                assert_that(
                    OrganizationMetaModel.get_shard_for_new_organization(organization_type='portal'),
                    equal_to(4)
                )


class TestOrganizationModel__save_paid_subscription_plan(BaseTestCase):
    def test_save_paid_subscription_plan_should_raise_error_if_organization_is_not_initiated_in_billing(self):
        # Если для организации нет записи OrganizationBillingInfoModel, метод _save_paid_subscription_plan
        # должен вызвать ошибку и не переводить организацию в платный режим.
        # Также, не должно генерироваться никаких событий.
        organization_model = OrganizationModel(self.main_connection)

        # Проверим, что организация в бесплатном режиме
        fresh_organization = organization_model.get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )

        with self.assertRaises(OrganizationIsWithoutContract):
            organization_model._save_paid_subscription_plan(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
            )

        # Проверим, что организация всё еще бесплатном режиме
        fresh_organization = organization_model.get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )

        # Проверяем, что действия и события не сгенерировались
        actions = ActionModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
            },
        )
        events = EventModel(self.main_connection).find()

        assert_that(
            [x['name'] for x in actions],
            equal_to([]),
        )
        assert_that(
            [x['name'] for x in events],
            equal_to([]),
        )


class TestOrganizationModel__enable_paid_mode_for_natural_person(TestCase):
    def test_enabling_paid_mode(self):
        # проверим, что включение платного режима создаст плательщика и клиента в Биллинге
        # запишет их id в OrganizationBillingInfoModel и создаст нужные события по action_organization_subscription_plan_change
        organization = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'Какая-то организация',
                'en': 'Some organization'
            },
            label='some_organization',
            language='ru',
            admin_uid=123,
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

        # до начала теста нет никакой информации о платных организациях
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).enable_paid_mode_for_natural_person(
                org_id=organization['id'],
                author_id=organization['admin_uid'],
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )

        billing_info = OrganizationBillingInfoModel(self.main_connection).find()
        assert_that(len(billing_info), equal_to(1))
        billing_info = billing_info[0]

        assert_that(billing_info['client_id'], equal_to(client_id))
        assert_that(billing_info['person_id'], equal_to(person_id))
        assert_that(billing_info['org_id'], equal_to(organization['id']))
        assert_that(billing_info['is_contract_active'], equal_to(True))  # оферта сразу активна

        mocked_xmlrpc.Balance.CreateClient.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'currency': 'RUR',
                'email': email,
                'phone': phone,
                'name': organization['name']['ru'],
            },
        )
        mocked_xmlrpc.Balance.CreatePerson.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'lname': last_name,
                'client_id': client_id,
                'mname': middle_name,
                'type': 'ph',
                'phone': phone,
                'fname': first_name,
                'email': email,
            }
        )
        mocked_xmlrpc.Balance.CreateOffer.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'bank_details_id': BANK_IDS['resident'],
                'person_id': person_id,
                'currency': 'RUR',
                'start_dt': None,
                'payment_type': 3,
                'services': [202],
                'firm_id': 1,
                'manager_uid': app.billing_client.manager_uid,
                'client_id': client_id,
                'payment_term': 30,
            }
        )

        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='paid',
                    )
                )
            )
        )

    def test_enabling_paid_mode_raise_exception_if_incorrect_email(self):
        # проверим, что включение платного режима вызовет исключение, если поле email некорректно
        organization = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'Какая-то организация',
                'en': 'Some organization'
            },
            label='some_organization',
            language='ru',
            admin_uid=123,
        )

        client_id = 2
        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov'

        fault_msg = """
        <error><msg>Email address "akhmetov" is invalid</msg><email>akhmetov</email>
        <wo-rollback>0</wo-rollback><method>Balance.CreatePerson</method><code>WRONG_EMAIL</code></error>
        """

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        # до начала теста нет никакой информации о платных организациях
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             pytest.raises(BillingInvalidField) as err:
            OrganizationModel(self.main_connection).enable_paid_mode_for_natural_person(
                org_id=organization['id'],
                author_id=organization['admin_uid'],
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        self.assertEqual(err.value.code, 'invalid_email')

        # проверим, что платность не включилась
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

    def test_enabling_paid_mode_raise_exception_if_missing_filed(self):
        # проверим, что включение платного режима вызовет исключение, если пропущено обязательное поле
        organization = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'Какая-то организация',
                'en': 'Some organization'
            },
            label='some_organization',
            language='ru',
            admin_uid=123,
        )

        client_id = 2
        first_name = 'Alexander'
        last_name = ''
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov'

        fault_msg = """
        <error><msg>Missing mandatory person field 'lname' for person type ur</msg>
        <field>lname</field><wo-rollback>0</wo-rollback><person-type>ur</person-type>
        <method>Balance.CreatePerson</method><code>MISSING_MANDATORY_PERSON_FIELD</code></error>
        """

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        # до начала теста нет никакой информации о платных организациях
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             pytest.raises(BillingMissingField) as err:
            OrganizationModel(self.main_connection).enable_paid_mode_for_natural_person(
                org_id=organization['id'],
                author_id=organization['admin_uid'],
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        self.assertEqual(err.value.params['field'], 'last_name')

        # проверим, что платность не включилась
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

    def test_enabling_paid_mode_raise_exception_if_uid_not_found(self):
        # проверим, что включение платного режима вызовет исключение, если биллинг не нашел uid
        organization = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'Какая-то организация',
                'en': 'Some organization'
            },
            label='some_organization',
            language='ru',
            admin_uid=123,
        )

        first_name = 'Alexander'
        last_name = 'Akhmetov'
        middle_name = 'R'
        phone = '+7'
        email = 'akhmetov@mail.ru'

        fault_msg = """
        <error><msg>Passport with ID 1130000000621392 not found in DB</msg>
        <wo-rollback>0</wo-rollback><code>2</code><object-id>1130000000621392</object-id><object >
        </object><method>Balance.CreateClient</method><code>PASSPORT_NOT_FOUND</code><parent-codes>
        <code>NOT_FOUND</code><code>EXCEPTION</code></parent-codes><contents>Passport with ID 1130000000621392
        not found in DB</contents></error>
        """
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.side_effect = Fault(-1, fault_msg)

        # до начала теста нет никакой информации о платных организациях
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             pytest.raises(BillingUidNotFoundInPassport) as err:
            OrganizationModel(self.main_connection).enable_paid_mode_for_natural_person(
                org_id=organization['id'],
                author_id=organization['admin_uid'],
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )
        self.assertEqual(err.value.params['uid'], '1130000000621392')

        # проверим, что платность не включилась
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))


class TestOrganizationModel__enable_paid_mode_for_initiated_in_billing_organization(BaseTestCase):
    def test_enabling_paid_mode_with_not_initiated_in_billing_organization(self):
        # проверим, что включение платного режима для организации, которой нет в Биллинге
        # сгенерирует ошибку OrganizationIsWithoutContract
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({'org_id': self.organization['id']}),
            equal_to(0),
        )

        with self.assertRaises(OrganizationIsWithoutContract):
            OrganizationModel(self.main_connection).enable_paid_mode_for_initiated_in_billing_organization(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
            )

        # Проверим, что организация всё еще бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )

        # Проверяем, что действия и события не сгенерировались
        actions = ActionModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
            },
        )
        events = EventModel(self.main_connection).find()

        assert_that(
            [x['name'] for x in actions],
            equal_to([]),
        )
        assert_that(
            [x['name'] for x in events],
            equal_to([]),
        )

    def test_enabling_paid_mode_for_initiated_on_billing_organization(self):
        # проверяем включение платности для заведенной в Биллинге организации
        self.enable_paid_mode()
        self.disable_paid_mode()

        # организация должна быть в бесплатном режиме, но у неё должна быть запись OrganizationBillingInfoModel
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({'org_id': self.organization['id']}),
            equal_to(1),
        )

        # удалим все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        contract_id = 123

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '10',
            'ReceiptSum': '200',
        }]

        # включаем платный режим
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).enable_paid_mode_for_initiated_in_billing_organization(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
            )

        # организация перешла в платный режим
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('paid'),
        )

        # и сгенерировались нужные события
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='paid',
                    )
                )
            )
        )

    def test_enabling_paid_mode_for_initiated_on_billing_organization_with_unpaid_act(self):
        # проверяем включение платности для заведенной в Биллинге организации, у которой
        # есть неоплаченный акт, но еще есть время на оплату
        self.enable_paid_mode()
        self.disable_paid_mode()

        # организация должна быть в бесплатном режиме, но у неё должна быть запись OrganizationBillingInfoModel
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({'org_id': self.organization['id']}),
            equal_to(1),
        )

        # удалим все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        contract_id = 123
        first_debt_act_date = utcnow() - datetime.timedelta(days=14)

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '10',
            'ReceiptSum': '200',
            'FirstDebtFromDT': first_debt_act_date.isoformat(),
        }]

        # включаем платный режим
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).enable_paid_mode_for_initiated_in_billing_organization(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
            )

        # организация перешла в платный режим
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('paid'),
        )

        # и сгенерировались нужные события
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=self.organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='paid',
                    )
                )
            )
        )

    def test_enabling_paid_mode_for_organization_with_debt(self):
        # проверим, что включение платного режима для организации, у которой есть задолженность
        # вызовет исключение OrganizationHasDebt
        self.enable_paid_mode()
        self.disable_paid_mode()

        # организация должна быть в бесплатном режиме, но у неё должна быть запись OrganizationBillingInfoModel
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )
        assert_that(
            OrganizationBillingInfoModel(self.main_connection).count({'org_id': self.organization['id']}),
            equal_to(1),
        )

        # удалим все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        contract_id = 123
        first_debt_act_date = utcnow() - datetime.timedelta(days=app.config['BILLING_PAYMENT_TERM'] + 1)

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '300',
            'ReceiptSum': '200',
            'FirstDebtFromDT': first_debt_act_date.isoformat(),
        }]

        # пытаемся включить платный режим
        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             self.assertRaises(OrganizationHasDebt):
            OrganizationModel(self.main_connection).enable_paid_mode_for_initiated_in_billing_organization(
                org_id=self.organization['id'],
                author_id=self.admin_uid,
            )

        # Проверим, что организация всё еще бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )

        # Проверяем, что cобытия не сгенерировались
        events = EventModel(self.main_connection).find()
        assert_that(
            [x['name'] for x in events],
            equal_to([]),
        )


class TestOrganizationModel__get_price_info_for_service(BaseTestCase):
    def test_should_return_pricing_for_free_organization(self):
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '570'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '285'}],
                'ProductID': app.config['BILLING_CONNECT_TWO_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            },
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            for user_count, price_per_user in [(1, 570), (2, 570), (3, 570), (4, 570)]:
                UserModel(self.main_connection).delete(
                    filter_data={'org_id': self.organization['id']},
                    force_remove_all=True,
                )

                for _ in range(user_count):
                    self.create_user(org_id=self.organization['id'])

                assert_that(
                    UserModel(self.main_connection).count(filter_data={'org_id': self.organization['id']}),
                    equal_to(user_count),
                )

                price_info = OrganizationModel(self.main_connection).get_price_info_for_service(
                    org_id=self.organization['id'],
                    service_slug='connect',
                    promocode_id=None,
                )

                assert_that(
                    price_info,
                    has_entries(
                        per_user=price_per_user,
                        total=0,
                        users_count=0,
                        per_user_with_discount=None,
                        total_with_discount=None,
                    )
                )

    def test_should_return_pricing_for_free_organization_and_connect_upgrade_service(self):
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '570'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '285'}],
                'ProductID': app.config['BILLING_CONNECT_TWO_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            },
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            for user_count, price_per_user in [(1, 570), (2, 285), (3, 190), (4, 190)]:
                UserModel(self.main_connection).delete(
                    filter_data={'org_id': self.organization['id']},
                    force_remove_all=True,
                )

                for _ in range(user_count):
                    self.create_user(org_id=self.organization['id'])

                assert_that(
                    UserModel(self.main_connection).count(filter_data={'org_id': self.organization['id']}),
                    equal_to(user_count),
                )

                price_info = OrganizationModel(self.main_connection).get_price_info_for_service(
                    org_id=self.organization['id'],
                    service_slug='connect_upgrade',
                    promocode_id=None,
                )

                assert_that(
                    price_info,
                    has_entries(
                        per_user=price_per_user,
                        total=user_count * price_per_user,
                        users_count=user_count,
                        per_user_with_discount=None,
                        total_with_discount=None,
                    )
                )

    def test_should_return_pricing_with_promocode_for_no_licenses(self):
        # проверяем, что total_with_discount = 0, если нет лицензий на сервис, либо тарифный план 'free'
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )
        tracker = ServiceModel(self.meta_connection).create(
            name='tracker',
            slug='tracker',
            client_id='client11',
            paid_by_license=True
        )

        promocode_connect_product_id = 2
        promocode_tracker_product_id = 3
        promocode_connect_price = 200
        promocode_tracker_price = 300
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: promocode_connect_product_id,
                },
                'tracker': {
                    5: promocode_tracker_product_id,
                },
            },
        )
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        for _ in range(10):
            self.create_user(org_id=self.organization['id'])

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '790'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '400'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': str(promocode_connect_price)}],
                'ProductID': promocode_connect_product_id
            },
            {
                'Prices': [{'Price': str(promocode_tracker_price)}],
                'ProductID': promocode_tracker_product_id
            },
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            price_info = OrganizationModel(self.main_connection).get_price_info_for_service(
                org_id=self.organization['id'],
                service_slug='connect',
                promocode_id=promocode['id'],
            )

            assert_that(
                price_info,
                has_entries(
                    per_user=790,
                    total=0,
                    users_count=0,
                    per_user_with_discount=promocode_connect_price,
                    total_with_discount=0,
                )
            )

            price_info = OrganizationModel(self.main_connection).get_price_info_for_service(
                org_id=self.organization['id'],
                service_slug='tracker',
                promocode_id=promocode['id'],
            )

            assert_that(
                price_info,
                has_entries(
                    per_user=0,
                    total=0,
                    users_count=0,
                    per_user_with_discount=None,
                    total_with_discount=None,
                )
            )

    def test_correct_user_count_for_paid_service(self):
        # проверяем, что вернется правильное количество пользователей, если лицензия им выдана несколькими способами
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )
        tracker = ServiceModel(self.meta_connection).create(
            name='tracker',
            slug='tracker',
            client_id='client11',
            paid_by_license=True
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            tracker['slug'])

        department = self.create_department()
        user1 = self.create_user(nickname='user1')
        user2 = self.create_user(name={'first': {'ru': 'User2'}})
        user3 = self.create_user(department_id=department['id'], nickname='user_three')
        other_users = []
        for _ in range(4):
            other_users.append(self.create_user()['id'])

        members = [
            {'type': 'user', 'object': user2},
            {'type': 'user', 'object': user3},
        ]
        group = self.create_group(members=members)
        self.create_licenses_for_service(
            tracker['id'],
            department_ids=[department['id']],
            group_ids=[group['id']],
            user_ids=[user3['id']] + other_users
        )

        # пользователю user3 лицензия выдана тремя способами + лицензия у user2 и других 4х пользователей
        licenses = UserServiceLicenses(self.main_connection).find()
        self.assertEqual(len(licenses), 8)

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '400'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID'],
            },
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            price_info = OrganizationModel(self.main_connection).get_price_info_for_service(
                org_id=self.organization['id'],
                service_slug='tracker',
                promocode_id=None,
            )

            # лицензии есть только у 3 человек
            assert_that(
                price_info,
                has_entries(
                    per_user=0,
                    total=0,
                    users_count=6,
                    per_user_with_discount=None,
                    total_with_discount=None,
                )
            )


class TestOrganizationModel__enable_paid_mode_for_legal_person(TestCase):
    def test_enabling_paid_mode_with_offer(self):
        # проверим, что включение платного по оферте режима создаст плательщика и клиента в Биллинге
        # запишет их id в OrganizationBillingInfoModel и создаст нужные события по action_organization_subscription_plan_change
        organization = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'Какая-то организация',
                'en': 'Some organization'
            },
            label='some_organization',
            language='ru',
            admin_uid=123,
        )

        person_id = 1
        client_id = 2
        long_name = 'OOO Yandex'
        phone = '+7916'
        email = 'akhmetov@yandex-team.ru'
        postal_code = 123456
        postal_address = 'Moscow, 1'
        legal_address = 'Moscow, 2'
        inn = 123456789012
        kpp = 100
        bik = 101
        account = 110

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        # до начала теста нет никакой информации о платных организациях
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).enable_paid_mode_for_legal_person(
                org_id=organization['id'],
                author_id=organization['admin_uid'],
                long_name=long_name,
                phone=phone,
                email=email,
                postal_code=postal_code,
                postal_address=postal_address,
                legal_address=legal_address,
                inn=inn,
                kpp=kpp,
                bik=bik,
                account=account,
                contract=False,
            )

        billing_info = OrganizationBillingInfoModel(self.main_connection).find()
        assert_that(len(billing_info), equal_to(1))
        billing_info = billing_info[0]

        assert_that(billing_info['client_id'], equal_to(client_id))
        assert_that(billing_info['person_id'], equal_to(person_id))
        assert_that(billing_info['org_id'], equal_to(organization['id']))
        assert_that(billing_info['is_contract_active'], equal_to(True))  # оферта сразу активна

        mocked_xmlrpc.Balance.CreateClient.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'currency': 'RUR',
                'email': email,
                'phone': phone,
                'name': organization['name']['ru'],
            },
        )
        mocked_xmlrpc.Balance.CreatePerson.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'type': 'ur',
                'email': email,
                'postcode': postal_code,
                'inn': inn,
                'phone': phone,
                'postaddress': postal_address,
                'name': organization['name']['ru'],
                'legaladdress': legal_address,
                'account': account,
                'longname': long_name,
                'client_id': client_id,
                'bik': bik,
                'kpp': kpp,
            }
        )
        mocked_xmlrpc.Balance.CreateOffer.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'bank_details_id': BANK_IDS['resident'],
                'payment_term': 30,
                'services': [202],
                'manager_uid': app.billing_client.manager_uid,
                'client_id': client_id,
                'firm_id': 1,
                'start_dt': None,
                'payment_type': 3,
                'person_id': person_id,
                'currency': 'RUR',
            }
        )

        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='paid',
                    )
                )
            )
        )

    def test_enabling_paid_mode_with_contract(self):
        # проверим, что включение платного режима по договору создаст плательщика и клиента в Биллинге
        # запишет их id в OrganizationBillingInfoModel и создаст нужные события по action_organization_subscription_plan_change
        organization = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'Какая-то организация',
                'en': 'Some organization'
            },
            label='some_organization',
            language='ru',
            admin_uid=123,
        )

        person_id = 100
        client_id = 101
        long_name = 'OOO Yandex'
        phone = '+7916'
        email = 'akhmetov@yandex-team.ru'
        postal_code = 123456
        postal_address = 'Moscow, 1'
        legal_address = 'Moscow, 2'
        inn = 123456789012
        kpp = 111
        bik = 1000
        account = 1001

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        # до начала теста нет никакой информации о платных организациях
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).enable_paid_mode_for_legal_person(
                org_id=organization['id'],
                author_id=organization['admin_uid'],
                long_name=long_name,
                phone=phone,
                email=email,
                postal_code=postal_code,
                postal_address=postal_address,
                legal_address=legal_address,
                inn=inn,
                kpp=kpp,
                bik=bik,
                account=account,
                contract=True,
            )

        billing_info = OrganizationBillingInfoModel(self.main_connection).find()
        assert_that(len(billing_info), equal_to(1))
        billing_info = billing_info[0]

        assert_that(billing_info['client_id'], equal_to(client_id))
        assert_that(billing_info['person_id'], equal_to(person_id))
        assert_that(billing_info['org_id'], equal_to(organization['id']))
        assert_that(billing_info['is_contract_active'], equal_to(False))  # договор не должен быть активен

        mocked_xmlrpc.Balance.CreateClient.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'currency': 'RUR',
                'email': email,
                'phone': phone,
                'name': organization['name']['ru'],
            },
        )
        mocked_xmlrpc.Balance.CreatePerson.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'type': 'ur',
                'email': email,
                'postcode': postal_code,
                'inn': inn,
                'phone': phone,
                'postaddress': postal_address,
                'name': organization['name']['ru'],
                'legaladdress': legal_address,
                'account': account,
                'longname': long_name,
                'client_id': client_id,
                'bik': bik,
                'kpp': kpp,
            }
        )
        mocked_xmlrpc.Balance.CreateCommonContract.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'bank_details_id': BANK_IDS['resident'],
                'payment_term': 30,
                'services': [202],
                'manager_uid': app.billing_client.manager_uid,
                'client_id': client_id,
                'firm_id': 1,
                'start_dt': None,
                'payment_type': 3,
                'person_id': person_id,
                'currency': 'RUR',
            }
        )

        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='paid',
                    )
                )
            )
        )

    def test_enabling_paid_mode_raise_exception_if_incorrect_inn(self):
        # проверим, что включение платного режима вызовет исключение, если поле ИНН некорректно
        organization = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'Какая-то организация',
                'en': 'Some organization'
            },
            label='some_organization',
            language='ru',
            admin_uid=123,
        )

        client_id = 2
        long_name = 'OOO Yandex'
        phone = '+7916'
        email = 'akhmetov@yandex-team.ru'
        postal_code = 123456
        postal_address = 'Moscow, 1'
        legal_address = 'Moscow, 2'
        inn = 12345
        kpp = 100
        bik = 101
        account = 110

        fault_msg = """
        <error><msg>Invalid INN for ur or ua person</msg><wo-rollback>0</wo-rollback>
        <method>Balance.CreatePerson</method><code>INVALID_INN</code><parent-codes>
        <code>INVALID_PARAM</code><code>EXCEPTION</code></parent-codes>
        <contents>Invalid INN for ur or ua person</contents></error>
        """

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        # до начала теста нет никакой информации о платных организациях
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             pytest.raises(BillingInvalidField) as err:
            OrganizationModel(self.main_connection).enable_paid_mode_for_legal_person(
                org_id=organization['id'],
                author_id=organization['admin_uid'],
                long_name=long_name,
                phone=phone,
                email=email,
                postal_code=postal_code,
                postal_address=postal_address,
                legal_address=legal_address,
                inn=inn,
                kpp=kpp,
                bik=bik,
                account=account,
                contract=False,
            )
        self.assertEqual(err.value.code, 'invalid_inn')

        # проверим, что платность не включилась
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

    def test_enabling_paid_mode_raise_exception_if_account_mismatch_bik(self):
        # проверим, что включение платного режима вызовет исключение, если аккаунт не соотвествует бику
        organization = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'Какая-то организация',
                'en': 'Some organization'
            },
            label='some_organization',
            language='ru',
            admin_uid=123,
        )

        client_id = 2
        long_name = 'OOO Yandex'
        phone = '+7916'
        email = 'akhmetov@yandex-team.ru'
        postal_code = 123456
        postal_address = 'Moscow, 1'
        legal_address = 'Moscow, 2'
        inn = 12345
        kpp = 100
        bik = 101
        account = 110

        fault_msg = """
        <error><msg>Account 30101810400000000225 doesn't match bank with BIK=044525225</msg>
        <account>30101810400000000225</account><bik>044525225</bik><wo-rollback>0</wo-rollback>
        <method>Balance.CreatePerson</method><code>WRONG_ACCOUNT</code><parent-codes><code>INVALID_PARAM</code>
        <code>EXCEPTION</code></parent-codes><contents>Account 30101810400000000225 doesn't match bank
        with BIK=044525225</contents></error>
        """

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.side_effect = Fault(-1, fault_msg)

        # до начала теста нет никакой информации о платных организациях
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             pytest.raises(BillingInvalidField) as err:
            OrganizationModel(self.main_connection).enable_paid_mode_for_legal_person(
                org_id=organization['id'],
                author_id=organization['admin_uid'],
                long_name=long_name,
                phone=phone,
                email=email,
                postal_code=postal_code,
                postal_address=postal_address,
                legal_address=legal_address,
                inn=inn,
                kpp=kpp,
                bik=bik,
                account=account,
                contract=False,
            )
        self.assertEqual(err.value.code, 'invalid_account')

        # проверим, что платность не включилась
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))


class TestOrganizationBillingConsumedInfoModel(BaseTestCase):
    def test_find_gaps_for_organization(self):
        # проверяем, что метод найдет все даты, для которых у организации нет подсчитанных потребленных продуктов
        dates = [
            datetime.datetime(2017, 1, 1, tzinfo=pytz.UTC),
            datetime.datetime(2017, 1, 2, tzinfo=pytz.UTC),
            datetime.datetime(2017, 1, 3, tzinfo=pytz.UTC),
            datetime.datetime(2017, 1, 4, tzinfo=pytz.UTC),
            datetime.datetime(2017, 1, 5, tzinfo=pytz.UTC),
        ]
        exp_gaps = [
            datetime.datetime(2017, 1, 3, tzinfo=pytz.UTC),
            datetime.datetime(2017, 1, 5, tzinfo=pytz.UTC),
        ]

        expected_gaps = [gap.date() for gap in exp_gaps]

        model = OrganizationBillingConsumedInfoModel(self.main_connection)

        query = '''
            INSERT INTO {table}(org_id, total_users_count, for_date) VALUES(%(org_id)s, %(users)s, (%(date)s At TIME ZONE 'UTC'))
        '''.format(table=OrganizationBillingConsumedInfoModel.table)

        for date in dates:
            if date not in exp_gaps:
                self.main_connection.execute(
                    mogrify(
                        self.main_connection,
                        query,
                        vars={
                            'org_id': self.organization['id'],
                            'users': 100,
                            'date': date,
                        },
                    )
                )

        # посчитаем дыры от первой до последней даты, должны быть все дыры из expected_gaps
        gaps = model.find_gaps_for_organization(
            from_date=dates[0],
            to_date=dates[-1],
            org_id=self.organization['id'],
        )
        assert_that(
            gaps,
            equal_to(expected_gaps),
        )

        # посчитаем дыры от (dates[0]+1день) до последней даты, должна быть одна дата exp_gaps[1]
        gaps = model.find_gaps_for_organization(
            from_date=exp_gaps[0] + datetime.timedelta(days=1),
            to_date=dates[-1],
            org_id=self.organization['id'],
        )
        assert_that(
            gaps,
            equal_to([expected_gaps[1]]),
        )

        # если дыр нет, нужно вернуть пустой массив
        gaps = model.find_gaps_for_organization(
            from_date=dates[0],
            to_date=dates[1],
            org_id=self.organization['id'],
        )
        assert_that(
            gaps,
            equal_to([]),
        )

    def test_find_gaps_for_organization_with_less_than_24h_interval(self):
        # проверяем, что метод вернет две даты, даже если разницы между датами < 24 часов,
        # но фактически это два разных дня

        midnight = utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
        yesterday = midnight - datetime.timedelta(hours=1)
        model = OrganizationBillingConsumedInfoModel(self.main_connection)

        # посчитаем дыры от первой до последней даты, должны быть все дыры из exp_gaps
        gaps = model.find_gaps_for_organization(
            from_date=yesterday,
            to_date=midnight,
            org_id=self.organization['id'],
        )
        exp_gaps = [midnight.date(), yesterday.date()]
        assert_that(
            gaps,
            contains_inanyorder(*exp_gaps),
        )

    def test_calculate_should_calculate_all_consumed_products_info(self):
        # метод должен посчитать потребленные услуги для всех платных организаций
        # с учетом начала их платного периода

        # создадим две платные организации
        first_paid_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='first_paid_organization',
        )['organization']

        # платность в первой была включена 5 дней назад
        self.enable_paid_mode(
            org_id=first_paid_organization['id'],
            subscription_plan_changed_at=utcnow() - datetime.timedelta(days=5),
        )
        first_paid_organization = OrganizationModel(self.main_connection).get(first_paid_organization['id'])

        second_paid_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_paid_organization',
        )['organization']

        # платность во второй была включена 2 дня назад
        self.enable_paid_mode(
            org_id=second_paid_organization['id'],
            subscription_plan_changed_at=utcnow() - datetime.timedelta(days=2),
        )
        second_paid_organization = OrganizationModel(self.main_connection).get(second_paid_organization['id'])

        # создадим в первой и второй организации по 2 пользователя
        # которые были заведены сразу после включения платного режима
        for i in range(2):
            for organization in (first_paid_organization, second_paid_organization):
                self.create_user(
                    org_id=organization['id'],
                    created_at=organization['subscription_plan_changed_at'],
                )

        # в первой организации создадим еще одного пользователя,
        # который был заведен на день позже включения платного режима
        # это поможет протестировать подсчет количества потребленных услуг на день включения платности,
        # так как этот пользователь там не должен фигурировать
        self.create_user(
            org_id=first_paid_organization['id'],
            created_at=first_paid_organization['subscription_plan_changed_at'] + datetime.timedelta(days=1)
        )

        # и создадим одну бесплатную организацию
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='free_organization',
        )['organization']

        model = OrganizationBillingConsumedInfoModel(self.main_connection)

        # до подсчета не должно быть данных о потребленных услугах
        assert_that(
            model.count(),
            equal_to(0),
        )

        # считаем данные за 10 дней
        # чтобы гарантированно учесть все время платной работы организаций
        for date in [utcnow() - datetime.timedelta(days=i) for i in range(10)]:
            model.calculate(for_date=date)

        # после подсчета будет несколько потребленных услуг
        assert_that(
            model.count(),
            equal_to(9),
        )

        exp_first_org_result = []
        for date in [utcnow() - datetime.timedelta(days=i) for i in range(6)]:
            diff = (utcnow() - date)
            if diff.days >= 5:  # сначала было два пользователя (которых мы создали сами при включении платного режима)
                users_count = 2
            elif diff.days >= 1:  # потом три (мы добавили еще одного пользователя на день позже включения платности)
                users_count = 3
            else:
                users_count = 4  # после - 4 (из-за такого ручного заведения админ был создан сегодняшним днем)
            exp_first_org_result.append({
                'org_id': first_paid_organization['id'],
                'total_users_count': users_count,
                'for_date': date.date(),
            })

        assert_that(
            model.find(
                fields=[
                    'total_users_count',
                    'for_date',
                ],
                filter_data={
                    'org_id': first_paid_organization['id'],
                },
            ),
            contains_inanyorder(*exp_first_org_result),
        )

        exp_second_org_result = []
        for date in [utcnow() - datetime.timedelta(days=i) for i in range(3)]:
            diff = (utcnow() - date)
            if diff.days >= 1:  # сначала было два пользователя (которых мы создали сами при включении платного режима)
                users_count = 2
            else:
                users_count = 3  # после - 3 (из-за такого ручного заведения админ был создан сегодняшним днем)
            exp_second_org_result.append({
                'org_id': second_paid_organization['id'],
                'total_users_count': users_count,
                'for_date': date.date(),
            })

        assert_that(
            model.find(
                fields=[
                    'total_users_count',
                    'for_date',
                ],
                filter_data={
                    'org_id': second_paid_organization['id'],
                },
            ),
            contains_inanyorder(*exp_second_org_result),
        )

    def test_calculate_should_calculate_all_consumed_products_info_if_subscription_plan_changed_on_same_day(self):
        # метод должен посчитать потребленные услуги для всех платных организаций
        # проверяем, что если мы 1 числа включили платный режим в середине дня,
        # а 2 числа считаем количество потребленных услуг за вчера
        # метод правильно их посчитает

        # создадим платную организацию
        paid_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='first_paid_organization',
        )['organization']

        midnight = utcnow().replace(hour=0, minute=0, second=0, microsecond=0)
        afternoon = utcnow().replace(hour=12, minute=0, second=0, microsecond=0)

        # платность в первой была включена сегодня в 12 часов
        self.enable_paid_mode(
            org_id=paid_organization['id'],
            subscription_plan_changed_at=afternoon,
        )
        paid_organization = OrganizationModel(self.main_connection).get(paid_organization['id'])

        # создадим в организации пользователя
        self.create_user(
            org_id=paid_organization['id'],
            created_at=afternoon,
        )

        model = OrganizationBillingConsumedInfoModel(self.main_connection)

        # до подсчета не должно быть данных о потребленных услугах
        assert_that(
            model.count(),
            equal_to(0),
        )

        model.calculate(for_date=midnight)
        users_count = UserModel(self.main_connection).count(filter_data={
            'org_id': paid_organization['id'],
        })

        # после подсчета будет несколько потребленных услуг с учетом всех пользователей организации
        assert_that(
            model.count(),
            equal_to(1),
        )

        exp_result = [
            {'total_users_count': 2, 'for_date': midnight.date(), 'org_id': paid_organization['id']}
        ]

        assert_that(
            model.find(
                fields=[
                    'total_users_count',
                    'for_date',
                ],
                filter_data={
                    'org_id': paid_organization['id'],
                },
            ),
            equal_to(exp_result),
        )

    def test_calculate_should_calculate_all_consumed_products_info_with_org_id_filter(self):
        # метод должен посчитать потребленные услуги для всех платных организаций
        # с учетом начала их платного периода

        # создадим две платные организации
        first_paid_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='first_paid_organization',
        )['organization']
        self.enable_paid_mode(
            org_id=first_paid_organization['id'],
            subscription_plan_changed_at=utcnow() - datetime.timedelta(days=1),
        )
        first_paid_organization = OrganizationModel(self.main_connection).get(first_paid_organization['id'])

        second_paid_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_paid_organization',
        )['organization']
        self.enable_paid_mode(
            org_id=second_paid_organization['id'],
            subscription_plan_changed_at=utcnow() - datetime.timedelta(days=1),
        )
        second_paid_organization = OrganizationModel(self.main_connection).get(second_paid_organization['id'])

        # создадим в первой и второй организации по пользователю
        for organization in (first_paid_organization, second_paid_organization):
            self.create_user(
                org_id=organization['id'],
                created_at=utcnow() - datetime.timedelta(days=1),
            )

        # и создадим одну бесплатную организацию
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='free_organization',
        )['organization']

        model = OrganizationBillingConsumedInfoModel(self.main_connection)

        # до подсчета не должно быть данных о потребленных услугах
        assert_that(
            model.count(),
            equal_to(0),
        )

        model.calculate(org_id=first_paid_organization['id'])

        # после подсчета будет несколько потребленных услуг
        assert_that(
            model.count(),
            equal_to(1),
        )
        # мы считали с фильтром org_id=first_paid_organization['id'], поэтому должна была посчитаться только она
        assert_that(
            model.find(
                fields=[
                    'total_users_count',
                    'for_date',
                ],
            ),
            contains_inanyorder(*[{
                'for_date': (utcnow() - datetime.timedelta(days=1)).date(),
                'org_id': first_paid_organization['id'],
                'total_users_count': 1,
            }]),
        )

        model.calculate(rewrite=True)

        # после подсчета будет несколько потребленных услуг и добавится вторая организация
        assert_that(
            model.count(),
            equal_to(2),
        )
        assert_that(
            model.find(
                fields=[
                    'total_users_count',
                    'for_date',
                ],
            ),
            contains_inanyorder(*[
                {
                    'for_date': (utcnow() - datetime.timedelta(days=1)).date(),
                    'org_id': first_paid_organization['id'],
                    'total_users_count': 1,
                },
                {
                    'for_date': (utcnow() - datetime.timedelta(days=1)).date(),
                    'org_id': second_paid_organization['id'],
                    'total_users_count': 1,
                }
            ]),
        )

    def test_calculate_should_not_calculate_consumed_products_info_for_robots(self):
        # метод не должен посчитать потребленные услуги для роботных пользователей
        model = OrganizationBillingConsumedInfoModel(self.main_connection)

        calculate_for = utcnow()
        # включаем платный режим
        self.enable_paid_mode(
            subscription_plan_changed_at=calculate_for,
        )

        # у нас должен посчитаться только администратор организации
        self.assert_consumed_total_users_count(expected=1)

        # создадим в организации робота
        robot_user_id = create_robot_for_service_and_org_id(
            meta_connection=self.meta_connection,
            main_connection=self.main_connection,
            service_slug=self.service['slug'],
            org_id=self.organization['id']
        )
        robot_created_at = calculate_for
        self.main_connection.execute(
            mogrify(
                self.main_connection,
                'UPDATE users SET created = %(created_at)s WHERE id=%(user_id)s',
                {
                    'created_at': robot_created_at,
                    'user_id': robot_user_id,
                },
            )
        )

        # считаем данные за тот день, когда был создан робот
        # и ничего не меняется, у нас всё равно должен посчитаться только один администратор организации
        self.assert_consumed_total_users_count(expected=1)

        # создадим еще одного пользователя,
        # который был заведен в день включения платного режима
        self.create_user(
            created_at=calculate_for
        )

        # теперь платных пользователей два: администратор и новый пользователь
        self.assert_consumed_total_users_count(expected=2)

    def test_calculate_should_not_calculate_consumed_products_info_for_dismissed_users(self):
        # метод не должен посчитать потребленные услуги для уволенных пользователей
        model = OrganizationBillingConsumedInfoModel(self.main_connection)

        today = utcnow()
        yesterday = today - datetime.timedelta(days=1)

        # в базе проапдейтим всех пользователей организации, будто они созданы вчера
        UserModel(self.main_connection).update(
            update_data={'created': yesterday},
            filter_data={'org_id': self.organization['id']},
        )

        # вчера был включен платный режим
        self.enable_paid_mode(subscription_plan_changed_at=yesterday)

        # За вчера у нас должен посчитаться только администратор организации
        self.assert_consumed_total_users_count(expected=1, calculate_for=yesterday)

        # создадим в организации пользователя вчерашним днем
        user = self.create_user(created_at=yesterday)
        # и уволим его
        UserModel(self.main_connection).dismiss(
            org_id=self.organization['id'],
            user_id=user['id'],
            author_id=user['id'],
        )
        # в базе проапдейтим дату увольнения, чтобы он был уволен вчера
        UserDismissedModel(self.main_connection).update(
            update_data={'dismissed_date': yesterday},
            filter_data={'user_id': user['id']}
        )

        # проверяем, что за вчера теперь было бы подсчитано два человека
        self.assert_consumed_total_users_count(expected=2, calculate_for=yesterday)

        # а за сегодня - снова один администратор
        self.assert_consumed_total_users_count(expected=1, calculate_for=today)

    def assert_consumed_total_users_count(self, expected, calculate_for=None, org_id=None, promocode_id=None):
        """
        Проверяет, что подсчет количества людей за которых берем деньги на указанную дату
        совпадает с ожидаемым значением
        """
        if not calculate_for:
            calculate_for = utcnow()

        if org_id is None:
            org_id = self.organization['id']

        model = OrganizationBillingConsumedInfoModel(self.main_connection)
        model.delete(force_remove_all=True)
        model.calculate(for_date=calculate_for, rewrite=True)
        data = model.find(
            fields=[
                'total_users_count',
                'for_date',
                'promocode_id',
            ],
            filter_data={
                'org_id': org_id,
            },
        )
        assert_that(
            len(data),
            equal_to(1)
        )
        assert_that(
            data[0],
            has_entries(
                total_users_count=expected,
                for_date=calculate_for.date(),
                org_id=org_id,
                promocode_id=promocode_id,
            ),
        )

    def test_calculate_should_save_active_promocode_id(self):
        # при подсчете потребленных услуг мы должны сохранить id активного промокода

        # включаем платный режим
        self.enable_paid_mode()

        # у организации будет один активный промокод
        inactive_promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
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
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_70',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
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
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=inactive_promocode['id'],
            author_id=None,
        )
        # когда мы активировали второй промокод, первый деактивировался
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )

        self.assert_consumed_total_users_count(1, promocode_id=promocode['id'])

    def test_calculate_should_should_not_save_inactive_promocode_id(self):
        # при подсчете потребленных услуг мы должны сохранить id активного промокода

        # включаем платный режим
        self.enable_paid_mode()

        # у организации будет один неактивный промокод
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
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
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )
        OrganizationPromocodeModel(self.main_connection).deactivate_for_organization(
            org_id=self.organization['id'],
            author_id=None,
        )

        self.assert_consumed_total_users_count(1, promocode_id=None)

    def test_calculate_should_not_save_active_but_expired_promocode(self):
        # при подсчете потребленных услуг если вдруг промокод активен, ну срок его действия уже истек - мы его не сохраняем

        # включаем платный режим
        self.enable_paid_mode()

        # у организации будет один активный промокод
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2050, month=1, day=1),
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
        OrganizationPromocodeModel(self.main_connection).activate_for_organization(
            org_id=self.organization['id'],
            promocode_id=promocode['id'],
            author_id=None,
        )
        # делаем вид будто промокод истек
        OrganizationPromocodeModel(self.main_connection).update(
            update_data={'expires_at': utcnow() - datetime.timedelta(days=1)}
        )

        self.assert_consumed_total_users_count(1, promocode_id=None)

    def test_calculate_license_consumed_info(self):
        # проверяем, что метод правильно сохраняет информацию о используемых лицензиях
        org_id = self.organization['id']
        service = ServiceModel(self.meta_connection).create(
            name='service11',
            slug='service11',
            client_id='client11',
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            service['slug']
        )
        dates = [
            datetime.date(year=2017, month=1, day=1),
            datetime.date(year=2017, month=1, day=2),
            datetime.date(year=2017, month=1, day=3),
            datetime.date(year=2017, month=1, day=4),
        ]
        # 10 лицензий в первый день
        for _ in range(10):
            user = self.create_user(org_id=org_id)
            UserServiceLicenses(self.main_connection).create(user['id'], org_id, service['id'])
        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            org_id, service['id'], dates[0])
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).count(),
            equal_to(10),
        )

        # еще +5 лицензий во второй день
        for _ in range(5):
            user = self.create_user(org_id=org_id)
            UserServiceLicenses(self.main_connection).create(user['id'], org_id, service['id'])
        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            org_id, service['id'], dates[1])
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).count(),
            equal_to(25),
        )

        # удаляем одну лицензию
        UserServiceLicenses(self.main_connection).delete(filter_data={'user_id': user['id']})
        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            org_id, service['id'], dates[2])
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).count(),
            equal_to(39),
        )

        # выключаем сервис
        disable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            service['slug'],
            'reason',
        )

        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            org_id, service['id'], dates[3])

        # количество записей не поменялось, потому что сервис выключили
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).count(),
            equal_to(39),
        )

        model = OrganizationBillingConsumedInfoModel(self.main_connection)

        # до подсчета не должно быть данных о потребленных услугах
        assert_that(
            model.count(),
            equal_to(0),
        )

        # считаем данные за 4 дня
        for date in dates:
            model.calculate(for_date=date)

        # записей будет 3, потому что в 4 день сервис уже выключен
        assert_that(
            model.count(),
            equal_to(3),
        )
        assert_that(
            model.count(filter_data={'for_date': dates[-1]}),
            equal_to(0),
        )

        exp_result = []
        for date in dates[:-1]:
            exp_result.append({
                'total_users_count': OrganizationLicenseConsumedInfoModel(self.main_connection).count(
                    filter_data={'for_date': date}
                ),
                'for_date': date,
                'org_id': org_id,
            })

        assert_that(
            model.find(
                fields=[
                    'total_users_count',
                    'for_date',
                    'org_id',
                ],
            ),
            contains_inanyorder(*exp_result),
        )

    def test_calculate_consumed_billing_and_license_info(self):
        # метод должен посчитать потребленные услуги для коннекта и сервиса по лицензиям
        yesterday = utcnow() - datetime.timedelta(days=1)
        paid_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='paid_organization',
        )['organization']

        org_id = paid_organization['id']
        self.enable_paid_mode(
            org_id=org_id,
            subscription_plan_changed_at=yesterday,
        )

        # создадим двух пользователей
        for _ in range(2):
            user = self.create_user(
                org_id=org_id,
                created_at=yesterday,
            )

        # выдаем лицензию одному пользователю
        service = ServiceModel(self.meta_connection).create(
            name='service11',
            slug='service11',
            client_id='client11',
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            service['slug']
        )

        UserServiceLicenses(self.main_connection).create(user['id'], org_id, service['id'])
        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            org_id, service['id'], yesterday)

        model = OrganizationBillingConsumedInfoModel(self.main_connection)

        # до подсчета не должно быть данных о потребленных услугах
        assert_that(
            model.count(),
            equal_to(0),
        )

        model.calculate(org_id=org_id)

        # после подсчета будет несколько потребленных услуг
        assert_that(
            model.count(),
            equal_to(2),
        )
        exp_result = [
            {
                'for_date': yesterday.date(),
                'org_id': org_id,
                'service': service['slug'],
                'total_users_count': 1,
            },
            {
                'for_date': yesterday.date(),
                'org_id': org_id,
                'service': 'connect',
                'total_users_count': 2,
            },
        ]
        assert_that(
            model.find(
                fields=[
                    'for_date',
                    'org_id',
                    'service',
                    'total_users_count',
                ],
            ),
            contains_inanyorder(*exp_result),
        )


class TestOrganizationModel_disable_paid_mode(BaseTestCase):
    def test_should_not_generate_events_for_free_organization(self):
        # проверяем, что для бесплатной организации ничего не произойдет
        # удалим все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        # выключаем платный режим
        OrganizationModel(self.main_connection).disable_paid_mode(
            org_id=self.organization['id'],
            author_id=1,
        )

        # при выключении никаких данных посчитаться не должно было, так как их нет
        assert_that(
            OrganizationBillingConsumedInfoModel(self.main_connection).count(),
            equal_to(0),
        )
        # Проверим, что организация всё еще бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )

        # Проверяем, что действия и события не сгенерировались
        actions = ActionModel(self.main_connection).find(
            filter_data={
                'org_id': self.organization['id'],
            },
        )
        events = EventModel(self.main_connection).find()

        assert_that(
            [x['name'] for x in actions],
            equal_to([]),
        )
        assert_that(
            [x['name'] for x in events],
            equal_to([]),
        )

    def test_disable_paid_mode_should_fill_gaps_in_consumed_info(self):
        # проверяем, что выключение платного режима действительно его выключает и заполняет
        # таблицу потребленных услуг необходимыми данными
        #
        # создадим платную организации
        paid_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='paid_organization',
        )['organization']

        # платность была включена вчера
        self.enable_paid_mode(
            org_id=paid_organization['id'],
            subscription_plan_changed_at=utcnow() - datetime.timedelta(days=1),
        )
        # и вчера был заведено два пользователя
        for i in range(2):
            self.create_user(
                org_id=paid_organization['id'],
                created_at=utcnow() - datetime.timedelta(days=1),
            )
        paid_organization = OrganizationModel(self.main_connection).get(paid_organization['id'])

        # делаем вид, что вообще не считали информацию о потребленных услугах
        assert_that(
            OrganizationBillingConsumedInfoModel(self.main_connection).count(),
            equal_to(0),
        )

        # удалим все события
        EventModel(self.main_connection).delete(force_remove_all=True)

        # выключаем платный режим
        OrganizationModel(self.main_connection).disable_paid_mode(
            org_id=paid_organization['id'],
            author_id=1,
        )

        # при выключении должны были посчитаться данные о потребленных услугах за два дня
        assert_that(
            OrganizationBillingConsumedInfoModel(self.main_connection).count(),
            equal_to(2),
        )
        # проверяем содержимое этих данных
        assert_that(
            OrganizationBillingConsumedInfoModel(self.main_connection).find(
                fields=[
                    'total_users_count',
                    'for_date',
                ],
            ),
            contains_inanyorder(*[
                {
                    'for_date': (utcnow() - datetime.timedelta(days=1)).date(),
                    'org_id': paid_organization['id'],
                    'total_users_count': 2,
                },
                {
                    'for_date': utcnow().date(),
                    'org_id': paid_organization['id'],
                    'total_users_count': 3,
                }
            ]),
        )

        fresh_paid_organization = OrganizationModel(self.main_connection).get(paid_organization['id'])
        assert_that(
            fresh_paid_organization['subscription_plan'],
            equal_to('free'),
        )
        assert_that(
            ensure_date(fresh_paid_organization['subscription_plan_changed_at']),
            equal_to(ensure_date(utcnow())),
        )

        # должны были сгенерироваться события выключения платного режима
        assert_that(
            EventModel(self.main_connection).find(
                filter_data={
                    'org_id': fresh_paid_organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            contains(
                has_entries(
                    org_id=fresh_paid_organization['id'],
                    name=event.organization_subscription_plan_changed,
                    content=has_entries(
                        subscription_plan='free',
                    )
                )
            )
        )

    def test_disable_paid_mode_should_not_disable_licensed_service(self):
        # проверяем, что выключение платного режима не выключает платные сервисы
        self.enable_paid_mode()

        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service['slug'],
        )
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'enabled': Ignore,
                }
            ),
            contains(
                has_entries(
                    service_id=service['id'],
                    enabled=True,
                )
            )
        )
        OrganizationModel(self.main_connection).disable_paid_mode(
            org_id=self.organization['id'],
            author_id=1,
        )
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                    'enabled': Ignore,
                }
            ),
            contains(
                has_entries(
                    service_id=service['id'],
                    enabled=True,
                )
            )
        )


class TestOrganizationModel__get_url_for_paying(BaseTestCase):
    def test_pay_should_raise_error_for_free_organization_without_billing_info(self):
        # если организация бесплатная и информации о клиенте в Биллинге нет,
        # они НЕ могут платить
        with self.assertRaises(OrganizationIsWithoutContract):
            OrganizationModel(self.main_connection).get_url_for_paying(
                org_id=self.organization['id'],
                author_id=1,
                amount=1,
            )

    def test_pay_should_return_payment_url(self):
        # проверяем, что get_url_for_paying вернет ссылку на оплату
        # и сделает человека, который хочет заплатить, представителем клиента в Биллинге
        amount = 999
        exp_url_for_paying = 'https://billing.connect.yandex.ru/test/'

        self.enable_paid_mode()

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'id'],
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'UserPath': exp_url_for_paying}
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            with patch.object(CheckOrganizationBalanceTask, 'place_into_the_queue') as place_into_the_queue:
                url_for_paying = OrganizationModel(self.main_connection).get_url_for_paying(
                    org_id=self.organization['id'],
                    author_id=self.admin_uid,
                    amount=amount,
                )

                assert_that(
                    url_for_paying,
                    equal_to(exp_url_for_paying),
                )
                assert_that(
                    place_into_the_queue.call_count,
                    equal_to(2),
                )

        # пользователь должен быть представителем клиента в Биллинге,
        # иначе он не сможет оплатить
        mocked_xmlrpc.Balance.CreateUserClientAssociation.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            str(self.admin_uid),
        )

        # в Биллинге создался заказ
        mocked_xmlrpc.Balance.CreateOrUpdateOrdersBatch.assert_called_once_with(
            str(self.admin_uid),
            [
                {
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                    'ProductID': app.config['BILLING_PRODUCT_ID_RUB'],
                }
            ],
        )

        # в Биллинге создался счет на определенное количество продукта
        mocked_xmlrpc.Balance.CreateRequest2.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            [
                {
                    'Qty': amount,
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                },
            ],
            {
                'InvoiceDesireType': 'charge_note',
                'ReturnPath': None,
            }
        )

    def test_pay_with_return_path_should_return_payment_url(self):
        # проверяем, что get_url_for_paying вернет ссылку на оплату и вызовет методы биллинга с переданным ReturnPath
        amount = 999
        exp_url_for_paying = 'https://billing.connect.yandex.ru/test/'
        return_path = 'https://return_path_for_billing.yandex'

        self.enable_paid_mode()

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'id'],
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'UserPath': exp_url_for_paying}
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            with patch.object(CheckOrganizationBalanceTask, 'place_into_the_queue') as place_into_the_queue:
                url_for_paying = OrganizationModel(self.main_connection).get_url_for_paying(
                    org_id=self.organization['id'],
                    author_id=self.admin_uid,
                    amount=amount,
                    return_path=return_path,
                )

                assert_that(
                    url_for_paying,
                    equal_to(exp_url_for_paying),
                )
                assert_that(
                    place_into_the_queue.call_count,
                    equal_to(2),
                )

        # пользователь должен быть представителем клиента в Биллинге,
        # иначе он не сможет оплатить
        mocked_xmlrpc.Balance.CreateUserClientAssociation.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            str(self.admin_uid),
        )

        # в Биллинге создался заказ
        mocked_xmlrpc.Balance.CreateOrUpdateOrdersBatch.assert_called_once_with(
            str(self.admin_uid),
            [
                {
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                    'ProductID': app.config['BILLING_PRODUCT_ID_RUB'],
                }
            ],
        )

        # в Биллинге создался счет на определенное количество продукта
        mocked_xmlrpc.Balance.CreateRequest2.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            [
                {
                    'Qty': amount,
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                },
            ],
            {
                'InvoiceDesireType': 'charge_note',
                'ReturnPath': return_path,
            }
        )

    def test_pay_should_return_payment_url_for_free_organization_with_billing_info(self):
        # если организация бесплатная, но информация о клиенте в Биллинге есть,
        # они могут платить
        amount = 999
        exp_url_for_paying = 'https://billing.connect.yandex.ru/test/'

        # включаем и выключаем платный режим, чтобы организация завелась в Биллинге, но потом была бесплатной
        self.enable_paid_mode()
        OrganizationModel(self.main_connection).disable_paid_mode(
            org_id=self.organization['id'],
            author_id=self.admin_uid,
        )

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'id', 'subscription_plan'],
        )

        # проверяем, что организация бесплатная
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'UserPath': exp_url_for_paying}
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}  # не возвращаем информации о привязанных клиентах

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            with patch.object(CheckOrganizationBalanceTask, 'place_into_the_queue') as place_into_the_queue:
                url_for_paying = OrganizationModel(self.main_connection).get_url_for_paying(
                    org_id=self.organization['id'],
                    author_id=self.admin_uid,
                    amount=amount,
                )

                # но она все равно может оплатить услуги
                assert_that(
                    url_for_paying,
                    equal_to(exp_url_for_paying),
                )
                assert_that(
                    place_into_the_queue.call_count,
                    equal_to(2),
                )

        # пользователь не был привязан ни к одному клиенту, поэтому не отвязываем его
        assert_not_called(mocked_xmlrpc.Balance.RemoveUserClientAssociation)

        # пользователь должен быть представителем клиента в Биллинге,
        # иначе он не сможет оплатить
        mocked_xmlrpc.Balance.CreateUserClientAssociation.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            str(self.admin_uid),
        )

        # в Биллинге создался заказ
        mocked_xmlrpc.Balance.CreateOrUpdateOrdersBatch.assert_called_once_with(
            str(self.admin_uid),
            [
                {
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                    'ProductID': app.config['BILLING_PRODUCT_ID_RUB'],
                }
            ],
        )

        # в Биллинге создался счет на определенное количество продукта
        mocked_xmlrpc.Balance.CreateRequest2.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            [
                {
                    'Qty': amount,
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                },
            ],
            {
                'InvoiceDesireType': 'charge_note',
                'ReturnPath': None,
            }
        )

    def test_get_url_for_paying_should_reassign_user_to_client_if_needed(self):
        # проверяем, что get_url_for_paying привяжет пользователя методом create_client_user_association_if_needed
        # к клиенту в Биллинге
        amount = 999.5
        exp_url_for_paying = 'https://billing.connect.yandex.ru/test/'

        self.enable_paid_mode()

        fresh_organization = OrganizationModel(self.main_connection).get(
            self.organization['id'],
            fields=['billing_info.*', 'id'],
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'UserPath': exp_url_for_paying}
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'ClientId': None}

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            with patch.object(CheckOrganizationBalanceTask, 'place_into_the_queue') as place_into_the_queue:
                url_for_paying = OrganizationModel(self.main_connection).get_url_for_paying(
                    org_id=self.organization['id'],
                    author_id=self.admin_uid,
                    amount=amount,
                )

                assert_that(
                    url_for_paying,
                    equal_to(exp_url_for_paying),
                )
                assert_that(
                    place_into_the_queue.call_count,
                    equal_to(2),
                )

        # пользователь должен быть представителем клиента в Биллинге,
        # иначе он не сможет оплатить
        mocked_xmlrpc.Balance.CreateUserClientAssociation.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            str(self.admin_uid),
        )

        # в Биллинге создался заказ
        mocked_xmlrpc.Balance.CreateOrUpdateOrdersBatch.assert_called_once_with(
            str(self.admin_uid),
            [
                {
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                    'ProductID': app.config['BILLING_PRODUCT_ID_RUB'],
                }
            ],
        )

        # в Биллинге создался счет на определенное количество продукта
        mocked_xmlrpc.Balance.CreateRequest2.assert_called_once_with(
            str(self.admin_uid),
            fresh_organization['billing_info']['client_id'],
            [
                {
                    'Qty': amount,
                    'ServiceOrderID': fresh_organization['billing_info']['client_id'],
                    'ServiceID': 202,
                    'ClientID': fresh_organization['billing_info']['client_id'],
                },
            ],
            {
                'InvoiceDesireType': 'charge_note',
                'ReturnPath': None,
            }
        )

    def test_get_url_for_paying_should_raise_exception(self):
        # проверяем, что get_url_for_paying вернет ошибку, если пользователь привязан к другому client_id в биллинге
        amount = 999.5
        exp_url_for_paying = 'https://billing.connect.yandex.ru/test/'

        self.enable_paid_mode()

        # старый клиент, к которому был привязан пользователь
        # при вызове OrganizationModel(self.main_connection).get_url_for_paying
        # мы должны будем проверить что тот кто платит - привязан именно к клиенту текщей организации
        # и если это не так, вернем исключение
        old_client_id = 'some-client-id'

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.CreateRequest2.return_value = {'UserPath': exp_url_for_paying}
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {'ClientId': old_client_id}

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            with self.assertRaises(BillingClientIdMismatch):
                OrganizationModel(self.main_connection).get_url_for_paying(
                    org_id=self.organization['id'],
                    author_id=self.admin_uid,
                    amount=amount,
                )


class TestOrganizationBillingInfoModel__filter_by_organization_subscription_plan(BaseTestCase):
    def test_should_filter_by_organizations_subscription_plan(self):
        # создадим две организации, одну платную, одну бесплатную, но с биллинговой информацией
        paid_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='first_organization',
        )['organization']
        free_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_organization',
        )['organization']

        # включаем платный режим в первой организации
        self.enable_paid_mode(org_id=paid_organization['id'])

        # во второй организации мы включим и выключим платность, чтобы она была бесплатной,
        # но запись OrganizationBillingInfoModel была в базе
        self.enable_paid_mode(org_id=free_organization['id'])
        self.disable_paid_mode(org_id=free_organization['id'], admin_uid=free_organization['admin_uid'])

        model = OrganizationBillingInfoModel(self.main_connection)
        all_data = model.find(fields=['org_id'])
        paid_data = model.find(fields=['org_id'], filter_data={'organization__subscription_plan': 'paid'})
        free_data = model.find(fields=['org_id'], filter_data={'organization__subscription_plan': 'free'})

        assert_that(
            all_data,
            equal_to([{'org_id': paid_organization['id']}, {'org_id': free_organization['id']}]),
        )
        assert_that(
            paid_data,
            equal_to([{'org_id': paid_organization['id']}]),
        )
        assert_that(
            free_data,
            equal_to([{'org_id': free_organization['id']}]),
        )


class TestOrganizationBillingInfoModel__check_balance_and_debt(BaseTestCase):
    def enable_licensed_service(self):
        # вспомогательная функция, чтобы создать и подлючить лицензионный сервис
        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service['slug'],
        )

        # обновляем дату окончания триала
        last_week_date = (utcnow() - relativedelta(weeks=1)).date()
        self.update_service_trial_expires_date(
            self.organization['id'],
            service['id'],
            last_week_date
        )

        # выдаем лицензии на service
        new_license = UserServiceLicenses(self.main_connection).create(
            user_id=self.user['id'],
            org_id=self.organization['id'],
            service_id=service['id'],
        )
        return service['id']

    def test_check_balance(self):
        # проверяем, что баланс обновляется в базе и дата задолженности обнуляется, если он стал положительный

        # создаем организацию с непустой датой задолженности
        today = utcnow().date()
        self.enable_paid_mode(first_debt_act_date=today)
        fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            self.organization['id']
        )

        assert_that(
            fresh_organization_billing_info,
            has_entries(
                first_debt_act_date=today,
                balance=0,
                receipt_sum=0,
                act_sum=0,
            ),
        )

        act_sum = '1000.2'  # сумма по актам
        receipt_sum = '10.1'  # сумма поступлений
        contract_id = 123
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': act_sum,
            'ReceiptSum': receipt_sum,
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationBillingInfoModel(self.main_connection).check_balance_and_debt(
                self.organization['id']
            )

        mocked_xmlrpc.Balance.GetPartnerBalance.assert_called_once_with(
            202,
            [contract_id],
        )

        fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            self.organization['id']
        )

        # данные обновились, дата начала задолженности сбросилась
        assert_that(
            fresh_organization_billing_info,
            has_entries(
                first_debt_act_date=None,
                balance=Decimal('-990.1'),
                receipt_sum=Decimal(receipt_sum),
                act_sum=Decimal(act_sum),
            ),
        )

    def test_should_save_first_debt_act_date_even_if_balance_doesnt_change(self):
        # даже если баланс не изменился, но изменилась дата задолженности - нужно менять её в базе

        # создаем организацию с датой задолженности
        today = utcnow().date()
        yesterday = today - datetime.timedelta(days=1)
        self.enable_paid_mode(first_debt_act_date=yesterday)
        fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            self.organization['id']
        )

        # баланс организации нулевой
        assert_that(
            fresh_organization_billing_info['balance'],
            equal_to(0),
        )
        # дата задолженности - вчера
        assert_that(
            fresh_organization_billing_info['first_debt_act_date'],
            equal_to(yesterday),
        )

        contract_id = 123

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
            'FirstDebtFromDT': today.isoformat(),
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationBillingInfoModel(self.main_connection).check_balance_and_debt(
                self.organization['id'],
            )

        fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            self.organization['id']
        )

        # баланс не обновился, дата начала задолженности обновилась на сегодняшнюю
        assert_that(
            fresh_organization_billing_info,
            has_entries(
                first_debt_act_date=today,
                balance=Decimal(0),
                receipt_sum=Decimal(0),
                act_sum=Decimal(0),
            ),
        )

    def test_check_debts_not_send_mails(self):
        # проверяем, что письма не отправляются, если количество дней с начала
        # задолженности не попадает в интервал отпавки писем, либо баланс >= 0

        # создаем организацию с датой задолженности
        today = utcnow().date()
        debt_date = today - datetime.timedelta(days=10)
        self.enable_paid_mode(first_debt_act_date=debt_date)
        fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            self.organization['id']
        )

        # дата задолженности - 10 дней назад
        assert_that(
            fresh_organization_billing_info['first_debt_act_date'],
            equal_to(debt_date),
        )

        contract_id = 123
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
            'FirstDebtFromDT': debt_date.isoformat(),
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.send_email_to_admins') as mock_send_email:
            OrganizationBillingInfoModel(self.main_connection).check_balance_and_debt(
                self.organization['id'],
            )

        fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            self.organization['id']
        )

        # письма не отправились
        assert_not_called(mock_send_email)

        # дата начала задолженности не обновилась
        assert_that(
            fresh_organization_billing_info['first_debt_act_date'],
            equal_to(debt_date),
        )

        # дата отправки письма не обновилась
        assert_that(
            fresh_organization_billing_info['last_mail_sent_at'],
            equal_to(None),
        )

    def test_check_debts_not_send_mails_has_money(self):
        # проверяем, что письма не отправляются, если баланс >= 0

        # создаем организацию с датой задолженности, которая попадает в интервал отправки писем
        today = utcnow().date()
        debt_date = today - datetime.timedelta(days=SEND_DEBT_MAIL_DAYS[0])
        self.enable_paid_mode(first_debt_act_date=debt_date)
        fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            self.organization['id']
        )

        assert_that(
            fresh_organization_billing_info['first_debt_act_date'],
            equal_to(debt_date),
        )

        contract_id = 123
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        # баланс положительный
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '100',
            'ReceiptSum': '300',
            'FirstDebtFromDT': debt_date.isoformat(),
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.send_email_to_admins') as mock_send_email:
            OrganizationBillingInfoModel(self.main_connection).check_balance_and_debt(
                self.organization['id'],
            )

        fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            self.organization['id']
        )

        # письма не отправились
        mock_send_email.assert_not_called()

        # дата отправки письма не обновилась
        assert_that(
            fresh_organization_billing_info['last_mail_sent_at'],
            equal_to(None),
        )

    def test_check_debts_send_mails(self):
        # проверяем, что при наличии задолженности обновляется дата начала задолженности
        # и отправляются письма администраторам
        self.enable_paid_mode()
        fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            self.organization['id']
        )

        # дата начала задолженности пустая
        assert_that(
            fresh_organization_billing_info['first_debt_act_date'],
            equal_to(None),
        )

        # дата отправки письма пустая
        assert_that(
            fresh_organization_billing_info['last_mail_sent_at'],
            equal_to(None),
        )

        for days in SEND_DEBT_MAIL_DAYS[:-1]:
            first_debt_act_date = (utcnow() - datetime.timedelta(days=days)).date()
            contract_id = 123

            mocked_xmlrpc = Mock()
            mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
                'ID': contract_id,
                'SERVICES': [WORKSPACE_SERVICE_ID],
                'IS_ACTIVE': True,
            }]
            mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
                'ContractID': contract_id,
                'ActSum': '1000',
                'ReceiptSum': '200',
                'FirstDebtFromDT': first_debt_act_date.isoformat(),
            }]

            with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
                 patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.send_email_to_admins') as mock_send_email, \
                    patch('intranet.yandex_directory.src.yandex_directory.core.models.organization.get_meta_connection') as get_meta_connection:
                get_meta_connection.return_value = self.meta_connection
                OrganizationBillingInfoModel(self.main_connection).check_balance_and_debt(
                    self.organization['id']
                )

            # отправили письма администраторам
            mock_send_email.assert_called_once_with(
                self.meta_connection,
                self.main_connection,
                self.organization['id'],
                app.config['SENDER_CAMPAIGN_SLUG']['CONNECT_DEBT_EMAILS'][days],
                invoice_month=first_debt_act_date.month,
                tld='ru',
                lang='ru',
                downgrade_time_msk='23:59',
            )

            fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
                self.organization['id']
            )

            # дата задолженности обновилась
            assert_that(
                fresh_organization_billing_info['first_debt_act_date'],
                equal_to(first_debt_act_date),
            )

            # дата отправки письма обновилась
            assert_that(
                fresh_organization_billing_info['last_mail_sent_at'],
                equal_to(utcnow().date()),
            )

            # сбрасываем дату отправки письма
            OrganizationBillingInfoModel(self.main_connection).update(
                {'last_mail_sent_at': None},
                {'org_id': self.organization['id']}
            )

    def test_check_debts_and_disable_paid_mode(self):
        # проверяем, что платный режим и платные сервисы отключаются, если задолженность не погашена за 30 дней
        self.enable_paid_mode()
        fresh_organization_billing_info = OrganizationBillingInfoModel(self.main_connection).get(
            self.organization['id']
        )

        first_debt_act_date = utcnow() - datetime.timedelta(days=app.config['BILLING_PAYMENT_TERM'] + 1)
        contract_id = 123

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '300',
            'ReceiptSum': '200',
            'FirstDebtFromDT': first_debt_act_date.isoformat(),
        }]

        service_id = self.enable_licensed_service()
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                }
            ),
            contains_inanyorder(
                has_entries(service_id=service_id)
            )
        )
        with patch.object(app.billing_client, 'server', mocked_xmlrpc), \
             patch('intranet.yandex_directory.src.yandex_directory.core.mailer.utils.send_email_to_admins') as mock_send_email, \
                patch('intranet.yandex_directory.src.yandex_directory.core.models.organization.get_meta_connection') as get_meta_connection:
            get_meta_connection.return_value = self.meta_connection
            OrganizationBillingInfoModel(self.main_connection).check_balance_and_debt(
                self.organization['id']
            )

        # отправили письма администраторам
        mock_send_email.assert_called_once_with(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            app.config['SENDER_CAMPAIGN_SLUG']['CONNECT_DEBT_EMAILS'][31],
            invoice_month=first_debt_act_date.month,
            tld='ru',
            lang='ru',
            downgrade_time_msk='23:59',
        )

        # платный режим выключен
        fresh_organization_billing_info = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization_billing_info['subscription_plan'],
            equal_to('free'),
        )
        # платные сервисы отключены
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                }
            ),
            equal_to([])
        )


class Test__get_price_and_product_id_for_service(BaseTestCase):
    def test_get_price_and_product_id_for_tracker(self):
        # проверяем что для трекера будут возвращаться правильные цены для разного количества людей
        prices = {
            'free': 0,
            app.config['BILLING_TRACKER_FIVE_HUNDRED_PRODUCT_ID']: 5,
            app.config['BILLING_TRACKER_TWO_THOUSAND_PRODUCT_ID']: 7,
        }

        experiments = {
            'free': (0, 1, 5),
            app.config['BILLING_TRACKER_FIVE_HUNDRED_PRODUCT_ID']: (101, 499, 500),
            app.config['BILLING_TRACKER_TWO_THOUSAND_PRODUCT_ID']: (501, 2000, 5000),
        }

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': str(price)}],
                'ProductID': product_id,
            } for product_id, price in list(prices.items())
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            for product_id, user_counts in list(experiments.items()):
                for user_count in user_counts:
                    assert_that(
                        get_price_and_product_id_for_service(user_count, 'tracker', None),
                        has_entries(
                            product_id=product_id,
                            price=prices[product_id],
                        )
                    )

    def test_get_price_with_promocode(self):
        # проверяем что промокод будет переписывать product_id если нужно
        promocode_product_id = 2
        promocode_price = 200
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: promocode_product_id,
                },
            },
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '10'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '100'}],
                'ProductID': app.config['BILLING_CONNECT_TWO_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': str(promocode_price)}],
                'ProductID': promocode_product_id,
            },
        ]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            assert_that(
                get_price_and_product_id_for_service(
                    users_count=1,
                    service='connect',
                    promocode_id=promocode['id'],
                ),
                has_entries(
                    product_id=promocode_product_id,
                    price=10,
                    price_with_discount=promocode_price,
                )
            )

            # но для двух пользователей цена должна быть стандартной, т.к. указана в настройках
            assert_that(
                get_price_and_product_id_for_service(
                    users_count=2,
                    service='connect',
                    promocode_id=promocode['id'],
                ),
                has_entries(
                    product_id=app.config['BILLING_CONNECT_TWO_PRODUCT_ID'],
                    price=100,
                    price_with_discount=None,
                )
            )

            # и для другого сервиса цена не должна быть переписанной
            assert_that(
                get_price_and_product_id_for_service(
                    users_count=1,
                    service='tracker',
                    promocode_id=None,
                ),
                has_entries(
                    product_id=app.config['PRODUCT_ID_FREE'],
                    price=0,
                    price_with_discount=None,
                )
            )

    def test_get_price_with_free_promocode(self):
        # проверяем что 'free' промокод показывает нулевую цену
        promocode_free_id = app.config['PRODUCT_ID_FREE']
        promocode_price = 0
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    1: promocode_free_id,
                    2: promocode_free_id,
                    3: promocode_free_id
                },
            },
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '10'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '100'}],
                'ProductID': app.config['BILLING_CONNECT_TWO_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '50'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID'],
            },
            {
                'Prices': [{'Price': '400'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID'],
            },

        ]

        user_count_price = {1: 10, 2: 100, 3: 50, 10: 50}
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            for users_count, price in user_count_price.items():
                assert_that(
                    get_price_and_product_id_for_service(
                        users_count=users_count,
                        service='connect',
                        promocode_id=promocode['id'],
                    ),
                    has_entries(
                        product_id=promocode_free_id,
                        price=price,
                        price_with_discount=promocode_price,
                    )
                )


class Test__get_price_and_product_id_for_connect(BaseTestCase):
    def test_get_price_and_product_id_for_connect_one(self):
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '570'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            assert_that(
                get_price_and_product_id_for_service(1, 'connect', None),
                has_entries(
                    product_id=app.config['BILLING_CONNECT_ONE_PRODUCT_ID'],
                    price=570,
                )
            )

    def test_get_price_and_product_id_for_connect_two(self):
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '285'}],
                'ProductID': app.config['BILLING_CONNECT_TWO_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            assert_that(
                get_price_and_product_id_for_service(2, 'connect', None),
                has_entries(
                    product_id=app.config['BILLING_CONNECT_TWO_PRODUCT_ID'],
                    price=285,
                )
            )

    def test_get_price_and_product_id_for_connect_three(self):
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            assert_that(
                get_price_and_product_id_for_service(3, 'connect', None),
                has_entries(
                    product_id=app.config['BILLING_CONNECT_PRODUCT_ID'],
                    price=190,
                )
            )

    def test_get_price_and_product_id_for_connect_large(self):
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            assert_that(
                get_price_and_product_id_for_service(300, 'connect', None),
                has_entries(
                    product_id=app.config['BILLING_CONNECT_PRODUCT_ID'],
                    price=190,
                )
            )

    def test_get_price_and_product_id_for_connect_large_with_promocode(self):
        promocode_product_id = 10000
        promocode_price = 10
        promocode = PromocodeModel(self.meta_connection).create(
            id='CONNECT_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'connect': {
                    3: promocode_product_id,
                },
            },
        )

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': str(promocode_price)}],
                'ProductID': promocode_product_id
            },
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            assert_that(
                get_price_and_product_id_for_service(300, 'connect', promocode['id']),
                has_entries(
                    product_id=promocode_product_id,
                    price=190,
                    price_with_discount=promocode_price,
                )
            )


class Test__get_price_and_product_id_for_disk(BaseTestCase):
    def test_get_price_and_product_id_for_disk_one(self):
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '500'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            assert_that(
                get_price_and_product_id_for_service(1, 'disk', None),
                has_entries(
                    product_id=app.config['BILLING_PARTNER_DISK_PRODUCT_ID'],
                    price=500,
                )
            )

    def test_get_price_and_product_id_for_disk_large(self):
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '500'}],
                'ProductID': app.config['BILLING_PARTNER_DISK_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            assert_that(
                get_price_and_product_id_for_service(1000000, 'disk', None),
                has_entries(
                    product_id=app.config['BILLING_PARTNER_DISK_PRODUCT_ID'],
                    price=500,
                )
            )


class TestOrganizationLogoCase(BaseTestCase):
    def setUp(self):
        super(TestOrganizationLogoCase, self).setUp()
        org_id = self.organization['id']

        # Смоделируем ситуацию, когда у организации есть загруженное лого
        image = ImageModel(self.main_connection).create(
            org_id,
            meta={
                'sizes': {
                    'orig': {
                        'height': 960,
                        'path': '/get-connect/5201/some-img/orig',
                        'width': 640
                    }
                }
            }
        )
        OrganizationModel(self.main_connection).update(
            update_data={'logo_id': image['id']},
            filter_data={'id': org_id},
        )

    def test_paid_organization_have_logo(self):
        # Проверим, что если организация на платном тарифе,
        # то лого у неё отдаётся
        self.enable_paid_mode()

        org = OrganizationModel(self.main_connection).find(
            {'id': self.organization['id']},
            fields=['logo'],
            one=True,
        )
        assert_that(
            org,
            has_entries(
                logo=not_none(),
            )
        )

    def test_free_organization_have_logo(self):
        # Проверим, что в организация на бесплатном тарифе, есть лого.
        org = OrganizationModel(self.main_connection).find(
            {'id': self.organization['id']},
            fields=['logo'],
            one=True,
        )
        assert_that(
            org,
            has_entries(
                logo=not_none(),
            )
        )


class TestOrganizationCreateContractInfoView(TestCase):
    valid_natural_person_data = {
        'person_type': 'natural',
        'first_name': 'Alexander',
        'last_name': 'Akhmetov',
        'middle_name': 'R',
        'email': 'akhmetov@yandex-team.ru',
        'phone': '+79160000000',
    }

    valid_legal_person_data = {
        'person_type': 'legal',
        'long_name': 'ООО Яндекс',
        'postal_code': '119021',
        'postal_address': 'Москва, Льва Толстого 18Б',
        'legal_address': 'Москва, Льва Толстого 16',
        'inn': '666',
        'kpp': '777',
        'bik': '888',
        'account': '999',
        'phone': '+79160000000',
        'email': 'akhmetov@yandex-team.ru',
    }

    def test_create_billing_info_for_natural_person(self):
        # проверим, что заведение биллинговой информации для физических лиц создаст плательщика и клиента в Биллинге
        # запишет их id в OrganizationBillingInfoModel, но тарифный план не поменяется
        organization = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'Какая-то организация',
                'en': 'Some organization'
            },
            label='some_organization',
            language='ru',
            admin_uid=123,
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

        # до начала теста нет никакой информации о платных организациях
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_natural_person(
                org_id=organization['id'],
                author_id=organization['admin_uid'],
                first_name=first_name,
                last_name=last_name,
                middle_name=middle_name,
                phone=phone,
                email=email,
            )

        billing_info = OrganizationBillingInfoModel(self.main_connection).find()
        assert_that(len(billing_info), equal_to(1))
        billing_info = billing_info[0]

        assert_that(billing_info['client_id'], equal_to(client_id))
        assert_that(billing_info['person_id'], equal_to(person_id))
        assert_that(billing_info['org_id'], equal_to(organization['id']))
        assert_that(billing_info['is_contract_active'], equal_to(True))  # оферта сразу активна

        mocked_xmlrpc.Balance.CreateClient.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'currency': 'RUR',
                'email': email,
                'phone': phone,
                'name': organization['name']['ru'],
            },
        )
        mocked_xmlrpc.Balance.CreatePerson.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'lname': last_name,
                'client_id': client_id,
                'mname': middle_name,
                'type': 'ph',
                'phone': phone,
                'fname': first_name,
                'email': email,
            }
        )
        mocked_xmlrpc.Balance.CreateOffer.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'bank_details_id': BANK_IDS['resident'],
                'person_id': person_id,
                'currency': 'RUR',
                'start_dt': None,
                'payment_type': 3,
                'services': [202],
                'firm_id': 1,
                'manager_uid': app.billing_client.manager_uid,
                'client_id': client_id,
                'payment_term': 30,
            }
        )

        # проверим, что организация всё еще в бесплатном режиме
        organization = OrganizationModel(self.main_connection).get(organization['id'])
        assert_that(organization['subscription_plan'], equal_to('free'))

        # и никакие события не сгенерировались
        assert_that(
            EventModel(self.main_connection).count(
                filter_data={
                    'org_id': organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            equal_to(0),
        )

    def test_create_billing_info_for_legal_person(self):
        # проверим, что заведение биллинговой информации для юридических лиц создаст плательщика и клиента в Биллинге
        # запишет их id в OrganizationBillingInfoModel, но тарифный план не поменяется
        organization = OrganizationModel(self.main_connection).create(
            id=1,
            name={
                'ru': 'Какая-то организация',
                'en': 'Some organization'
            },
            label='some_organization',
            language='ru',
            admin_uid=123,
        )

        person_id = 1
        client_id = 2
        long_name = 'OOO Yandex'
        phone = '+7916'
        email = 'akhmetov@yandex-team.ru'
        postal_code = 123456
        postal_address = 'Moscow, 1'
        legal_address = 'Moscow, 2'
        inn = 123456789012
        kpp = 100
        bik = 101
        account = 110

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetPassportByUid.return_value = {}
        mocked_xmlrpc.Balance.CreateClient.return_value = (None, None, client_id)
        mocked_xmlrpc.Balance.CreatePerson.return_value = person_id

        # до начала теста нет никакой информации о платных организациях
        assert_that(len(OrganizationBillingInfoModel(self.main_connection).find()), equal_to(0))
        assert_that(EventModel(self.main_connection).count(), equal_to(0))

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationModel(self.main_connection).create_contract_info_for_legal_person(
                org_id=organization['id'],
                author_id=organization['admin_uid'],
                long_name=long_name,
                phone=phone,
                email=email,
                postal_code=postal_code,
                postal_address=postal_address,
                legal_address=legal_address,
                inn=inn,
                kpp=kpp,
                bik=bik,
                account=account,
                contract=False,
            )

        billing_info = OrganizationBillingInfoModel(self.main_connection).find()
        assert_that(len(billing_info), equal_to(1))
        billing_info = billing_info[0]

        assert_that(billing_info['client_id'], equal_to(client_id))
        assert_that(billing_info['person_id'], equal_to(person_id))
        assert_that(billing_info['org_id'], equal_to(organization['id']))
        assert_that(billing_info['is_contract_active'], equal_to(True))  # оферта сразу активна

        mocked_xmlrpc.Balance.CreateClient.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'currency': 'RUR',
                'email': email,
                'phone': phone,
                'name': organization['name']['ru'],
            },
        )
        mocked_xmlrpc.Balance.CreatePerson.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'type': 'ur',
                'email': email,
                'postcode': postal_code,
                'inn': inn,
                'phone': phone,
                'postaddress': postal_address,
                'name': organization['name']['ru'],
                'legaladdress': legal_address,
                'account': account,
                'longname': long_name,
                'client_id': client_id,
                'bik': bik,
                'kpp': kpp,
            }
        )
        mocked_xmlrpc.Balance.CreateOffer.assert_called_once_with(
            str(organization['admin_uid']),
            {
                'bank_details_id': BANK_IDS['resident'],
                'payment_term': 30,
                'services': [202],
                'manager_uid': app.billing_client.manager_uid,
                'client_id': client_id,
                'firm_id': 1,
                'start_dt': None,
                'payment_type': 3,
                'person_id': person_id,
                'currency': 'RUR',
            }
        )

        # проверим, что организация всё еще в бесплатном режиме
        fresh_organization = OrganizationModel(self.main_connection).get(organization['id'])
        assert_that(fresh_organization['subscription_plan'], equal_to('free'))

        # и никакие события не сгенерировались
        assert_that(
            EventModel(self.main_connection).count(
                filter_data={
                    'org_id': organization['id'],
                    'name': event.organization_subscription_plan_changed,
                }
            ),
            equal_to(0),
        )


class TestOrganizationLicenseConsumedInfoModel(BaseTestCase):
    def setUp(self):
        super(TestOrganizationLicenseConsumedInfoModel, self).setUp()
        self.services = [
            ServiceModel(self.meta_connection).create(
                name='service1',
                slug='service1',
                client_id='client1',
            ),
            ServiceModel(self.meta_connection).create(
                name='service2',
                slug='service2',
                client_id='client2',
            ),
            ServiceModel(self.meta_connection).create(
                name='service3',
                slug='service3',
                client_id='client3',
            ),
        ]
        self.second_organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='second_organization',
        )['organization']
        for serv in self.services:
            for org_id in [self.organization['id'], self.second_organization['id']]:
                enable_service(
                    self.meta_connection,
                    self.main_connection,
                    org_id,
                    serv['slug']
                )
        for _ in range(10):
            for org_id in [self.organization['id'], self.second_organization['id']]:
                user = self.create_user(
                    org_id=org_id
                )
                for serv in self.services:
                    UserServiceLicenses(self.main_connection).create(user['id'], org_id, serv['id'])

    def test_should_not_save_licenses_for_trial_service(self):
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='my_org',
        )['organization']

        # создадим два сервиса и включим их
        paid_service = ServiceModel(self.meta_connection).create(
            name='paid',
            slug='paid',
            client_id='paid',
            paid_by_license=True,
        )
        trial_service = ServiceModel(self.meta_connection).create(
            name='trial',
            slug='trial',
            client_id='trial',
            paid_by_license=True,
            trial_period_months=12,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            organization['id'],
            paid_service['slug']
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            organization['id'],
            trial_service['slug']
        )

        # выдадим пользователю лицензию на сервис
        user = self.create_user(org_id=organization['id'])
        UserServiceLicenses(self.main_connection).create(user['id'], organization['id'], paid_service['id'])
        UserServiceLicenses(self.main_connection).create(user['id'], organization['id'], trial_service['id'])

        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).find(),
            equal_to([])
        )

        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses()

        # проверим что в OrganizationLicenseConsumedInfoModel создалась только
        # запись о потребленной лицензии для сервиса без триала
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).count(
                filter_data={'org_id': organization['id']}),
            1,
        )
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).find(
                filter_data={'org_id': organization['id']},
                fields=['service_id'],
            ),
            equal_to([
                {'service_id': paid_service['id'], 'org_id': organization['id']},
            ]),
        )

    def test_should_save_licenses_for_trial_service_without_trial(self):
        organization = create_organization(
            self.meta_connection,
            self.main_connection,
            label='my_org',
        )['organization']

        # создадим сервис и включим
        paid_service = ServiceModel(self.meta_connection).create(
            name='paid',
            slug='paid',
            client_id='paid',
            paid_by_license=True,
            trial_period_months=-1,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            organization['id'],
            paid_service['slug']
        )

        # выдадим пользователю лицензию на сервис
        user = self.create_user(org_id=organization['id'])
        UserServiceLicenses(self.main_connection).create(user['id'], organization['id'], paid_service['id'])

        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).find(),
            equal_to([])
        )

        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses()

        # проверим что в OrganizationLicenseConsumedInfoModel создалась
        # запись о потребленной лицензии для сервиса
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).count(
                filter_data={'org_id': organization['id']}),
            1,
        )
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).find(
                filter_data={'org_id': organization['id']},
                fields=['service_id'],
            ),
            equal_to([
                {'service_id': paid_service['id'], 'org_id': organization['id']},
            ]),
        )

    def test_save_user_service_licenses(self):
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).find(),
            equal_to([])
        )

        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses()

        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).count(),
            equal_to(UserServiceLicenses(self.main_connection).count())
        )

    def test_save_user_service_licenses_with_ord_service_id(self):
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).find(),
            equal_to([])
        )

        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            org_id=self.second_organization['id'], service_id=self.services[0]['id']
        )

        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).count(),
            equal_to(UserServiceLicenses(self.main_connection).count(
                filter_data={'org_id': self.second_organization['id'], 'service_id': self.services[0]['id']}
            ))
        )

    def test_save_user_service_licenses_should_save_changed_licenses(self):
        # при изменении лицензий в таблице должы сохраняться все пользователи,
        # у которых были лицензии на сегодня, даже если их отобрали в течение дня,
        # и должны добавляться новые пользователи, которым выдали лицензии

        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).find(),
            equal_to([])
        )

        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses()
        old_user_count = OrganizationLicenseConsumedInfoModel(self.main_connection).count()

        assert_that(
            old_user_count,
            equal_to(UserServiceLicenses(self.main_connection).count())
        )

        # удалим все лицензии для сервиса service2
        UserServiceLicenses(self.main_connection).delete(filter_data={'service_id': self.services[1]['id']})

        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses()
        # количество записей при этом не должно поменяться
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).count(),
            equal_to(old_user_count)
        )

        # добавим лицензий на сервис service1
        for _ in range(2):
            user = self.create_user(
                org_id=self.organization['id']
            )
            UserServiceLicenses(self.main_connection).create(user['id'], self.organization['id'],
                                                             self.services[0]['id'])
        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses()
        # в таблицу должны добавится 2 новых пользователя
        assert_that(
            OrganizationLicenseConsumedInfoModel(self.main_connection).count(),
            equal_to(old_user_count + 2)
        )

    def test_set_trial_to_expired_for_disabled_service(self):
        # Если сервис выключили, и после этого триал истёк, мы должны
        # переводить статус триала из in-progress в expired. Раньше
        # этого не происходило. Подробности в тикете: DIR-7160
        # создадим два сервиса и включим их
        trial_service = ServiceModel(self.meta_connection).create(
            name='trial',
            slug='trial',
            client_id='trial',
            paid_by_license=True,
            trial_period_months=1,
        )
        org_id = self.organization['id']
        # Смоделируем включение и выключение сервиса
        enable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            trial_service['slug']
        )
        disable_service(
            self.meta_connection,
            self.main_connection,
            org_id,
            trial_service['slug'],
            'reason',
        )

        query = OrganizationServiceModel(self.main_connection) \
            .filter(org_id=org_id, service_id=trial_service['id'], enabled=Ignore)

        # Притворимся, будто триал истёк вчера
        query.update(trial_expires=time_in_past(25 * 60 * 60))

        # Запустим отключение триала в организациях, где он истёк
        set_trial_status_to_expired(self.main_connection)

        service_info = query.one()

        # запись о потребленной лицензии для сервиса без триала
        assert_that(
            service_info,
            has_entries(
                trial_status=trial_status.expired,
            )
        )


class TestOrganizationModel__get_tld_for_email(TestCase):
    def test_should_return_tld_for_organization(self):
        exp_tld = 'earth'
        model = OrganizationModel(self.main_connection)
        organization = model.create(
            id=1,
            name={
                'ru': 'Яндекс',
                'en': 'Yandex'
            },
            label='yandex',
            language='ru',
            admin_uid=123,
            tld=exp_tld,
        )

        with override_settings(PORTAL_TLDS=[exp_tld]):
            tld = model.get_tld_for_email(org_id=organization['id'])

        assert_that(
            tld,
            equal_to(exp_tld),
        )
        assert_that(
            g._tld_for_email_1,
            equal_to(exp_tld),
        )

    def test_should_return_default_tld_if_organizations_is_not_supported(self):
        exp_tld = 'earth'
        default_tld = 'tr'
        model = OrganizationModel(self.main_connection)
        organization = model.create(
            id=1,
            name={
                'ru': 'Яндекс',
                'en': 'Yandex'
            },
            label='yandex',
            language='ru',
            admin_uid=123,
            tld=exp_tld,
        )

        with override_settings(PORTAL_TLDS=[], DEFAULT_TLD=default_tld):
            tld = model.get_tld_for_email(org_id=organization['id'])

        assert_that(
            tld,
            equal_to(default_tld),
        )
        assert_that(
            g._tld_for_email_1,
            equal_to(default_tld),
        )

    def test_should_get_tld_for_organization_from_cache_if_its_exists(self):
        g._tld_for_email_1 = 'earth'
        model = OrganizationModel(self.main_connection)
        organization = model.create(
            id=1,
            name={
                'ru': 'Яндекс',
                'en': 'Yandex'
            },
            label='yandex',
            language='ru',
            admin_uid=123,
            tld='sometld',
        )

        with override_settings(PORTAL_TLDS=[g._tld_for_email_1]):
            tld = model.get_tld_for_email(org_id=organization['id'])

        assert_that(
            tld,
            equal_to(g._tld_for_email_1),
        )


class TestOrganizationBillingInfoModel__check_contract_status_in_billing(BaseTestCase):
    def test_disable_paid_mode_and_services_by_inactive_contracts(self):
        # проверяем, что платный режим и платные сервисы отключатся, если нет активных договоров

        self.enable_paid_mode()
        service = ServiceModel(self.meta_connection).create(
            client_id='some-client-id',
            slug='new_service',
            name='Service',
            robot_required=False,
            trial_period_months=1,
            paid_by_license=True,
        )
        enable_service(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
            service['slug'],
        )

        # обновляем дату окончания триала
        last_week_date = (utcnow() - relativedelta(weeks=1)).date()
        self.update_service_trial_expires_date(
            self.organization['id'],
            service['id'],
            last_week_date
        )

        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('paid')
        )
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                }
            ),
            contains_inanyorder(
                has_entries(service_id=service['id'])
            )
        )

        contract_id = 123
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': 0,
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            OrganizationBillingInfoModel(self.main_connection).check_contract_status_in_billing(
                self.organization['id']
            )

        # платный режим выключен
        fresh_organization = OrganizationModel(self.main_connection).get(self.organization['id'])
        assert_that(
            fresh_organization['subscription_plan'],
            equal_to('free'),
        )
        # платные сервисы отключены
        assert_that(
            OrganizationServiceModel(self.main_connection).find(
                filter_data={
                    'org_id': self.organization['id'],
                }
            ),
            equal_to([])
        )


class Test__check_has_debt(BaseTestCase):
    def assert_has_debt(self, has_debt, days_since_debt, expected_days_since_debt):
        assert_that(
            has_debt,
            equal_to(True)
        )
        assert_that(
            days_since_debt,
            equal_to(expected_days_since_debt)
        )

    def assert_has_no_debt(self, has_debt, days_since_debt, expected_days_since_debt):
        assert_that(
            has_debt,
            equal_to(False)
        )
        assert_that(
            days_since_debt,
            equal_to(expected_days_since_debt)
        )

    def test_has_debt(self):
        # считаем, что есть задолженность, если есть непогашенные акты и баланс отрицательный
        contract_id = 123
        first_debt_act_date = (utcnow() - datetime.timedelta(days=app.config['BILLING_PAYMENT_TERM'] + 1)).date()

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '300',
            'ReceiptSum': '200',
            'FirstDebtFromDT': first_debt_act_date.isoformat(),
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            has_debt, days_since_debt = check_has_debt(client_id=333)
        self.assert_has_debt(has_debt, days_since_debt, expected_days_since_debt=31)

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            has_debt, days_since_debt = check_has_debt(first_debt_act_date=first_debt_act_date, balance=-100)
        self.assert_has_debt(has_debt, days_since_debt, expected_days_since_debt=31)

    def test_has_no_debt(self):
        # задолженности нет, если нет неоплаченных актов, либо баланс положительный
        contract_id = 123
        first_debt_act_date = (utcnow() - datetime.timedelta(days=app.config['BILLING_PAYMENT_TERM'] + 1)).date()

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetClientContracts.return_value = [{
            'ID': contract_id,
            'SERVICES': [WORKSPACE_SERVICE_ID],
            'IS_ACTIVE': True,
        }]

        # есть просроченный акт, но баланс нулевой
        mocked_xmlrpc.Balance.GetPartnerBalance.return_value = [{
            'ContractID': contract_id,
            'ActSum': '200',
            'ReceiptSum': '200',
            'FirstDebtFromDT': first_debt_act_date.isoformat(),
        }]

        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            has_debt, days_since_debt = check_has_debt(client_id=333)
        self.assert_has_no_debt(has_debt, days_since_debt, expected_days_since_debt=31)

        # есть просроченный акт и баланс положительный
        has_debt, days_since_debt = check_has_debt(first_debt_act_date=first_debt_act_date, balance=100)
        self.assert_has_no_debt(has_debt, days_since_debt, expected_days_since_debt=31)

        # нет неоплаченных актов
        has_debt, days_since_debt = check_has_debt(first_debt_act_date=None, balance=0)
        self.assert_has_no_debt(has_debt, days_since_debt, expected_days_since_debt=0)

        # баланс отрицательный, но акт еще не просрочен
        first_act_date = (utcnow() - datetime.timedelta(days=app.config['BILLING_PAYMENT_TERM'] - 10)).date()
        has_debt, days_since_debt = check_has_debt(first_debt_act_date=first_act_date, balance=-100)
        self.assert_has_no_debt(has_debt, days_since_debt, expected_days_since_debt=20)
