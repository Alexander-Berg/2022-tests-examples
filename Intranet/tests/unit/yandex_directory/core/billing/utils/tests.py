# -*- coding: utf-8 -*-
import datetime
import unittest
import unittest.mock
import dateutil.parser
import pytest

from intranet.yandex_directory.src.yandex_directory.core.billing.utils import (
    _get_table_path,
    get_yt_clusters_without_billing_data,
    _prepare_consumed_products,
    save_organizations_consumed_products_to_yt,
    calculate_licensed_users,
    calculate_base_licenses,
    ensure_date,
    _get_tracker_billing_data,
    _mark_billing_info_as_sended,
    _set_next_payment,
)
from intranet.yandex_directory.src.yandex_directory.core.features.utils import set_feature_value_for_organization
from intranet.yandex_directory.src.yandex_directory.core.billing.tasks import (
    SaveBillingInfoToYTTask,
)
from intranet.yandex_directory.src.yandex_directory.core.models.license import TrackerLicenseLogModel
from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationModel,
    OrganizationBillingConsumedInfoModel,
    PromocodeModel,
    ServiceModel,
    UserServiceLicenses,
    OrganizationLicenseConsumedInfoModel,
    OrganizationPromocodeModel,
    UserModel,
    OrganizationBillingInfoModel,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import utcnow
from intranet.yandex_directory.src.yandex_directory.core.models.service import (
    enable_service,
    disable_service,
    TrackerBillingStatusModel,
)
from intranet.yandex_directory.src.yandex_directory import app
from intranet.yandex_directory.src.yandex_directory.core.commands.update_services_in_shards import Command as UpdateServicesInShards

from unittest.mock import (
    patch,
    Mock,
    MagicMock,
    ANY,
)
from hamcrest import (
    assert_that,
    equal_to,
    is_not,
    contains_inanyorder,
    empty,
)

from testutils import (
    TestCase,
    override_settings,
    create_organization,
)


BILLING_YT_TABLES_PATH = '//tmp/yandex-connect-autotests/billing/'


class Test__get_table_path(TestCase):
    @override_settings(BILLING_YT_TABLES_PATH=BILLING_YT_TABLES_PATH)
    def test_should_return_table_path(self):
        for_date = '1990-07-25'
        table_path = _get_table_path(for_date=for_date)
        exp_table_path = '//tmp/yandex-connect-autotests/billing/1990-07-25'

        assert_that(table_path, equal_to(exp_table_path))


class Test_get_yt_clusters_without_billing_data(TestCase):
    @override_settings(BILLING_YT_TABLES_PATH=BILLING_YT_TABLES_PATH)
    def test_should_check_if_yt_table_exists_in_all_clusters(self):
        # get_yt_clusters_without_billing_data должен вызвать метод is_table_exists
        # с каждым YT-клиентом из словаря billing_yt_clients
        for_date = '1990-07-25'
        mocked_billing_yt_clients = self._get_mocked_yt_clients('hahn', 'arnold', 'some_cluster')
        mocked_is_table_exists = Mock(return_value=True)

        with patch('intranet.yandex_directory.src.yandex_directory.core.billing.utils.yt_utils.billing_yt_clients', mocked_billing_yt_clients), \
             patch('intranet.yandex_directory.src.yandex_directory.core.billing.utils.yt_utils.is_table_exists', mocked_is_table_exists):
            get_yt_clusters_without_billing_data(for_date)

        exp_calls = []
        for client in list(mocked_billing_yt_clients.values()):
            exp_calls.append(unittest.mock.call('%s%s' % (BILLING_YT_TABLES_PATH, for_date), client))

        assert_that(
            mocked_is_table_exists.call_args_list,
            equal_to(exp_calls),
        )

    @override_settings(BILLING_YT_TABLES_PATH=BILLING_YT_TABLES_PATH)
    def test_should_return_clusters_without_requested_tables(self):
        # get_yt_clusters_without_billing_data должен вернуть те кластера,
        # для которых переданных таблиц нет
        for_date = '1990-07-25'

        def mocked_is_table_exists(_, client):
            if client.config['proxy']['url'] == 'hahn':
                return True
            else:
                return False

        mocked_billing_yt_clients = self._get_mocked_yt_clients('hahn', 'arnold', 'some_cluster')

        with patch('intranet.yandex_directory.src.yandex_directory.core.billing.utils.yt_utils.is_table_exists', mocked_is_table_exists), \
             patch('intranet.yandex_directory.src.yandex_directory.core.billing.utils.yt_utils.billing_yt_clients', mocked_billing_yt_clients):
            response = get_yt_clusters_without_billing_data(for_date)

        exp_response = [
            'arnold',
            'some_cluster',
        ]

        assert_that(
            sorted(response),
            equal_to(sorted(exp_response)),
        )

    def _get_mocked_yt_clients(self, *clusters):
        mocked_billing_yt_clients = {}
        for cluster in clusters:
            client = Mock()
            client.config = {
                'proxy': {
                    'url': cluster,
                },
            }
            mocked_billing_yt_clients[cluster] = client
        return mocked_billing_yt_clients

    @override_settings(BILLING_YT_TABLES_PATH=BILLING_YT_TABLES_PATH)
    def test_should_return_clusters_without_rows_with_empty_tables_checking(self):
        # get_yt_clusters_without_billing_data должен вернуть те кластера,
        # для которых нет таблиц когда включена проверка пустых строк (DIR-3945)
        for_date = '1990-07-25'

        mocked_is_table_exists = Mock(return_value=False)
        mocked_billing_yt_clients = self._get_mocked_yt_clients('hahn', 'arnold')
        mocked_billing_yt_clients['hahn'].row_count.return_value = 1
        mocked_billing_yt_clients['arnold'].row_count.return_value = 0

        with patch('intranet.yandex_directory.src.yandex_directory.core.billing.utils.yt_utils.is_table_exists', mocked_is_table_exists), \
             patch('intranet.yandex_directory.src.yandex_directory.core.billing.utils.yt_utils.billing_yt_clients', mocked_billing_yt_clients):
            response = get_yt_clusters_without_billing_data(for_date, check_empty_tables=True)

        exp_response = [
            'arnold',
            'hahn',
        ]

        assert_that(
            sorted(response),
            equal_to(sorted(exp_response)),
        )

    @override_settings(BILLING_YT_TABLES_PATH=BILLING_YT_TABLES_PATH)
    def test_should_return_clusters_without_rows(self):
        # get_yt_clusters_without_billing_data должен вернуть те кластера,
        # для которых в таблице нет строк
        for_date = '1990-07-25'

        mocked_is_table_exists = Mock(return_value=True)
        mocked_billing_yt_clients = self._get_mocked_yt_clients('hahn', 'arnold')
        mocked_billing_yt_clients['hahn'].row_count.return_value = 1
        mocked_billing_yt_clients['arnold'].row_count.return_value = 0

        with patch('intranet.yandex_directory.src.yandex_directory.core.billing.utils.yt_utils.billing_yt_clients', mocked_billing_yt_clients), \
             patch('intranet.yandex_directory.src.yandex_directory.core.billing.utils.yt_utils.is_table_exists', mocked_is_table_exists):
            response = get_yt_clusters_without_billing_data(for_date, check_empty_tables=True)

        exp_response = [
            'arnold',
        ]

        assert_that(
            sorted(response),
            equal_to(sorted(exp_response)),
        )

class Test__prepare_consumed_products(TestCase):
    def test_prepare_consumed_products_should_return_prepared_data(self):
        self.create_feature(False, 'disable_billing_tracker')

        # проверим, что _prepare_consumed_products формирует данные в правильном формате
        promocode_product_id = 12345
        promocode_price = 99
        promocode = PromocodeModel(self.meta_connection).create(
            id='TRACKER_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'tracker': {
                    100: promocode_product_id,
                },
            },
        )

        date = datetime.date(year=2017, day=1, month=1)
        consumed_products_info = [
            {
                'organization_billing_info': {
                    'client_id': 100,
                },
                'total_users_count': 8,
                'org_id': 1,
                'for_date': date,
                'promocode_id': None,
                'service': 'tracker',
                'organization_type': 'common',
            },
            {
                'organization_billing_info': {
                    'client_id': 200,
                },
                'total_users_count': 80,
                'org_id': 2,
                'for_date': date,
                'promocode_id': None,
                'service': 'tracker',
                'organization_type': 'common',
            },
            {
                'organization_billing_info': {
                    'client_id': 300,
                },
                'total_users_count': 10000,
                'org_id': 3,
                'for_date': date,
                'service': 'tracker',
                'promocode_id': None,
                'organization_type': 'common',
            },
            {
                'organization_billing_info': {
                    'client_id': 400,
                },
                'total_users_count': 12,
                'org_id': 4,
                'for_date': date,
                'promocode_id': promocode['id'],
                'service': 'tracker',
                'organization_type': 'common',
            },
        ]
        exp_response = [
            {
                'client_id': 300,
                'quantity': 10000,
                'product_id': app.config['BILLING_PRODUCT_IDS_FOR_SERVICES']['tracker'][2000],
                'date': date.strftime('%Y-%m-%d'),
                'promocode_id': None,
                'service': 'tracker',
                'org_id': ANY,
            },
        ]

        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '285'}],
                'ProductID': app.config['BILLING_TRACKER_HUNDRED_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '570'}],
                'ProductID': app.config['BILLING_TRACKER_TWO_THOUSAND_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': str(promocode_price)}],
                'ProductID': promocode_product_id,
            },
            {
                'Prices': [{'Price': str(promocode_price)}],
                'ProductID': promocode_product_id,
            },
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            response = _prepare_consumed_products(consumed_products_info)

        assert_that(
            response,
            equal_to(exp_response),
        )


class Test__get_consumed_products_info_for_shard(TestCase):
    @pytest.mark.skip("DIR-8565")
    def test_should_return_all_consumed_products_info(self):
        # проверяем, что _get_consumed_products_info_for_shard
        # вернет данные для всех организаций

        # создадим две организации
        self.yandex = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex'
        )['organization']
        self.google = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']


        yesterday = utcnow() - datetime.timedelta(days=1)

        # и включим в них платный режим
        for organization in (self.yandex, self.google):
            self.enable_paid_mode(
                org_id=organization['id'],
                subscription_plan_changed_at=yesterday,
            )
        # создадим несколько пользователей так, чтобы дата создания была чуть позже включения
        # платного режима
        yesterday_5min = yesterday + datetime.timedelta(minutes=5)
        for i in range(3):
            self.create_user(org_id=self.yandex['id'], created_at=yesterday_5min)
        for i in range(5):
            self.create_user(org_id=self.google['id'], created_at=yesterday_5min)

        # Обновляем дату создания у дефолтного админа, который создается при заведении организации
        self.main_connection.execute("UPDATE users SET created = %s", yesterday)

        # выполним подсчет потребленных услуг
        OrganizationBillingConsumedInfoModel(self.main_connection).calculate(rewrite=True)

        yesterday = (utcnow() - datetime.timedelta(days=1)).date().strftime('%Y-%m-%d')
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            }
        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            consumed_products_info = _prepare_consumed_products(
                OrganizationBillingConsumedInfoModel(self.main_connection).find(
                    fields=[
                        'org_id',
                        'for_date',
                        'service',
                        'total_users_count',
                        'organization_billing_info.client_id',
                        'promocode_id',
                        'organization_type',
                    ],
                    filter_data={'for_date': yesterday},
                    limit=10,
                )
            )

        exp_products = [
            {
                'quantity': 4,
                'date': yesterday,
                'product_id': app.config['BILLING_PRODUCT_IDS_FOR_SERVICES']['connect'][3],
                'client_id': ANY,
                'promocode_id': None,
                'service': 'connect',
                'org_id': ANY,
            },
            {
                'quantity': 6,
                'date': yesterday,
                'product_id': app.config['BILLING_PRODUCT_IDS_FOR_SERVICES']['connect'][3],
                'client_id': ANY,
                'promocode_id': None,
                'service': 'connect',
                'org_id': ANY,
            },
        ]

        assert_that(
            consumed_products_info,
            equal_to(exp_products),
        )

        # у обеих организаций разные client_id
        assert_that(
            consumed_products_info[0]['client_id'],
            is_not(equal_to(consumed_products_info[1]['client_id'])),
        )

    @pytest.mark.skip("DIR-8565")
    def test_should_return_all_consumed_products_info_small_organisations(self):
        # проверяем, что _get_consumed_products_info_for_shard
        # вернет правильные данные для маленьких организаций

        # создадим три организации
        self.yandex = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex',
        )['organization']
        self.google = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google'
        )['organization']
        self.mailru = create_organization(
            self.meta_connection,
            self.main_connection,
            label='mailru',
        )['organization']

        yesterday = utcnow() - datetime.timedelta(days=1)

        # и включим в них платный режим
        for organization in (self.yandex, self.google, self.mailru):
            self.enable_paid_mode(
                org_id=organization['id'],
                subscription_plan_changed_at=yesterday,
            )

        # создадим несколько пользователей
        for i in range(2):
            self.create_user(org_id=self.yandex['id'], created_at=yesterday)
        self.create_user(org_id=self.google['id'], created_at=yesterday)

        assert_that(
            OrganizationModel(self.main_connection).get_users_count_for_billing(self.yandex['id']),
            equal_to(3),
        )
        assert_that(
            OrganizationModel(self.main_connection).get_users_count_for_billing(self.google['id']),
            equal_to(2),
        )
        assert_that(
            OrganizationModel(self.main_connection).get_users_count_for_billing(self.mailru['id']),
            equal_to(1),
        )

        # Обновляем дату создания у дефолтного админа, который создается при заведении организации
        self.main_connection.execute("UPDATE users SET created = TIMESTAMP 'yesterday'")

        # выполним подсчет потребленных услуг
        OrganizationBillingConsumedInfoModel(self.main_connection).calculate(rewrite=True)

        yesterday = (utcnow().date() - datetime.timedelta(days=1)).strftime('%Y-%m-%d')
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '285'}],
                'ProductID': app.config['BILLING_CONNECT_TWO_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '570'}],
                'ProductID': app.config['BILLING_CONNECT_ONE_PRODUCT_ID']
            }

        ]
        with patch.object(app.billing_client, 'server', mocked_xmlrpc):
            consumed_products_info = _prepare_consumed_products(
                OrganizationBillingConsumedInfoModel(self.main_connection).find(
                    fields=[
                        'org_id',
                        'for_date',
                        'service',
                        'total_users_count',
                        'organization_billing_info.client_id',
                        'promocode_id',
                        'organization_type',
                    ],
                    filter_data={'for_date': yesterday},
                    limit=10,
                )
            )

        exp_products = [
            {
                'quantity': 3,
                'date': yesterday,
                'product_id': app.config['BILLING_PRODUCT_IDS_FOR_SERVICES']['connect'][3],
                'client_id': ANY,
                'service': 'connect',
                'org_id': ANY,
                'promocode_id': None,
            },
            {
                'quantity': 2,
                'date': yesterday,
                'product_id': app.config['BILLING_PRODUCT_IDS_FOR_SERVICES']['connect'][2],
                'client_id': ANY,
                'service': 'connect',
                'org_id': ANY,
                'promocode_id': None,
            },
            {
                'quantity': 1,
                'date': yesterday,
                'product_id': app.config['BILLING_PRODUCT_IDS_FOR_SERVICES']['connect'][1],
                'client_id': ANY,
                'service': 'connect',
                'org_id': ANY,
                'promocode_id': None,
            },
        ]

        assert_that(
            consumed_products_info,
            equal_to(exp_products),
        )

        # у организаций разные client_id
        assert_that(
            consumed_products_info[0]['client_id'],
            is_not(equal_to(consumed_products_info[1]['client_id'])),
        )
        assert_that(
            consumed_products_info[1]['client_id'],
            is_not(equal_to(consumed_products_info[2]['client_id'])),
        )


class Test__save_organizations_consumed_products_to_yt(TestCase):

    @pytest.mark.skip("DIR-8565")
    def test_should_save_organizations_consumed_products_to_yt(self):
        # проверяем, что save_organizations_consumed_products_to_yt
        # сохранит данные для организаций
        #
        # Если вчера одна организация в один день выключила и снова включила себе платный режим,
        # для нее запись в OrganizationBillingConsumedInfoModel уже есть.
        #
        # При стандартном подсчете по крону на следующий день, мы должны посчитать
        # потребленные услуги и для нее и для остальных организаций (DIR-3716) и добавить в OrganizationBillingConsumedInfoModel

        # создадим две организации
        yandex = create_organization(
            self.meta_connection,
            self.main_connection,
            label='yandex',
        )['organization']
        google = create_organization(
            self.meta_connection,
            self.main_connection,
            label='google',
        )['organization']

        yesterday = utcnow() - datetime.timedelta(days=1)

        # и включим в них платный режим
        for organization in (yandex, google):
            self.enable_paid_mode(
                org_id=organization['id'],
                subscription_plan_changed_at=yesterday,
            )

        yandex = OrganizationModel(self.main_connection).find(filter_data={'id': yandex['id']}, fields=['billing_info.*'], one=True)
        google = OrganizationModel(self.main_connection).find(filter_data={'id': google['id']}, fields=['billing_info.*'], one=True)

        # создадим разное количество пользователей в организациях, чтобы посмотреть что в YT улетают разные числа
        # в yandex будет 4 пользователя (3 + 1 админ)
        for i in range(10):
            user = self.create_user(org_id=yandex['id'], created_at=yesterday)
        # в google будет 6 пользователей (5 + 1 админ)
        for i in range(5):
            self.create_user(org_id=google['id'], created_at=yesterday)

        # апдейтим время создания пользователей в базе, чтобы сделать вид, будто они завелись вчера
        # делаем через SQL чтобы это проставилось для всех пользователей, включая админов созданных при заведении организации
        self.main_connection.execute("UPDATE users SET created = TIMESTAMP 'yesterday'")

        # создадим сервис и включим его для одной организации
        tracker = ServiceModel(self.meta_connection).create(
            name='tracker',
            slug='tracker',
            client_id='client11',
            ready_default=True,
            paid_by_license=True,
        )
        # синхронизируем таблицу services
        UpdateServicesInShards().try_run()
        enable_service(
            self.meta_connection,
            self.main_connection,
            yandex['id'],
            tracker['slug']
        )
        # выдаем лицензии
        self.create_licenses_for_service(
            tracker['id'],
            user_ids=UserModel(self.main_connection).filter(org_id=yandex['id']).fields('id').scalar()[:6],
            org_id=yandex['id'],
        )
        OrganizationLicenseConsumedInfoModel(self.main_connection).save_user_service_licenses(
            yandex['id'], tracker['id'], yesterday)

        # для одной организации данные уже должны быть посчитаны
        OrganizationBillingConsumedInfoModel(self.main_connection).calculate(
            rewrite=False,
            org_id=yandex['id'],
        )

        # выполним подсчет потребленных услуг
        mocked_cluster = MagicMock()
        mocked_yt_utils = Mock()
        mocked_yt_utils.billing_yt_clients = {'mocked_cluster': mocked_cluster}
        mocked_xmlrpc = Mock()
        mocked_xmlrpc.Balance.GetProducts.return_value = [
            {
                'Prices': [{'Price': '190'}],
                'ProductID': app.config['BILLING_CONNECT_PRODUCT_ID']
            },
            {
                'Prices': [{'Price': '570'}],
                'ProductID': app.config['BILLING_TRACKER_TEN_PRODUCT_ID']
            }
        ]
        with patch('intranet.yandex_directory.src.yandex_directory.core.billing.utils.get_yt_clusters_without_billing_data', Mock(return_value=['mocked_cluster'])):
            with patch('intranet.yandex_directory.src.yandex_directory.core.billing.utils.yt_utils', mocked_yt_utils):
                with patch.object(app.billing_client, 'server', mocked_xmlrpc):
                    save_organizations_consumed_products_to_yt()

        yesterday_str = yesterday.strftime('%Y-%m-%d')
        table_path = _get_table_path(for_date=yesterday_str)
        exp_calls = [
            unittest.mock.call(
                table=table_path,
                rows_data=ANY,
                client=mocked_cluster
            ),
        ]

        assert_that(
            mocked_yt_utils.append_rows_to_table.mock_calls,
            equal_to(exp_calls),
        )

        rows_data = [
            {
                'client_id': yandex['billing_info']['client_id'],
                'product_id': app.config['BILLING_PRODUCT_IDS_FOR_SERVICES']['tracker'][10],
                'quantity': 6,
                'date': yesterday_str,
                'org_id': yandex['id'],
                'service': 'tracker',
                'promocode_id': None,
            },
            {
                'client_id': yandex['billing_info']['client_id'],
                'product_id': app.config['BILLING_PRODUCT_IDS_FOR_SERVICES']['connect'][3],
                'quantity': 11,
                'date': yesterday_str,
                'org_id': yandex['id'],
                'service': 'connect',
                'promocode_id': None,
            },
            {
                'client_id': google['billing_info']['client_id'],
                'product_id': app.config['BILLING_PRODUCT_IDS_FOR_SERVICES']['connect'][3],
                'quantity': 6,
                'date': yesterday_str,
                'org_id': google['id'],
                'service': 'connect',
                'promocode_id': None,
            },
        ]
        _, args, kwargs = mocked_yt_utils.append_rows_to_table.mock_calls[0]
        assert_that(
            kwargs['rows_data'],
            contains_inanyorder(*rows_data)
        )

    def test_tasks_starting(self):
        # Проверим, что мета-таск можно запустить и он не требует org_id
        with patch.object(SaveBillingInfoToYTTask, 'do'):
            result = SaveBillingInfoToYTTask(self.main_connection).delay()
            assert result


class Test__calculate_license_count(TestCase):
    def test_calculate_base_success(self):
        tracker_id = ServiceModel(self.main_connection).create(
            id=444,
            slug='tracker',
            name='tracker',
        )['id']
        for_date = ensure_date('2020-04-05')

        for i in range(14):
            OrganizationLicenseConsumedInfoModel(self.main_connection).insert_into_db(
                org_id=self.organization['id'],
                user_id=i,
                for_date=for_date,
                service_id=tracker_id,
            )

        result = calculate_base_licenses(
            self.main_connection,
            self.organization['id'],
            for_date
        )
        self.assertEqual(result, [i for i in range(14)])


    def test_calculate_by_log_success(self):
        # выдадим лицензию раньше срока расчета она не должна быть учтена
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=123,
            action='add',
            created_at=dateutil.parser.parse('2020-09-02')
        )

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1234,
            action='add',
            created_at=dateutil.parser.parse('2020-09-04 15:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1235,
            action='add',
            created_at=dateutil.parser.parse('2020-09-04 15:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1236,
            action='add',
            created_at=dateutil.parser.parse('2020-09-04 15:00')
        )

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1234,
            action='delete',
            created_at=dateutil.parser.parse('2020-09-07 19:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1235,
            action='delete',
            created_at=dateutil.parser.parse('2020-09-04 15:20')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1238,
            action='add',
            created_at=dateutil.parser.parse('2020-09-08 16:00')
        )

        # эта лицензия тоже не должна быть учтена так как выдана позже даты расчета
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1239,
            action='add',
            created_at=dateutil.parser.parse('2020-10-04 15:00')
        )

        billing_value = calculate_licensed_users(
            self.meta_connection,
            org_id=self.organization['id'],
            to_date='2020-10-04',
        )

        # в результате должно быть 2 так как выдали в этом месяце 4 лицензии
        # одну из которых отозвали меньше чем через 30 минут
        # и еще одну лицензию мы выдали после того как отозвали одну из
        # выданных изначально
        self.assertEqual(billing_value, 2)

    def test_calculate_by_log_with_base(self):

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1234,
            action='add',
            created_at=dateutil.parser.parse('2020-09-04 15:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1235,
            action='add',
            created_at=dateutil.parser.parse('2020-09-04 15:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1236,
            action='add',
            created_at=dateutil.parser.parse('2020-09-04 15:00')
        )

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1234,
            action='delete',
            created_at=dateutil.parser.parse('2020-09-04 15:23')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1234,
            action='add',
            created_at=dateutil.parser.parse('2020-09-04 15:30')
        )

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1235,
            action='delete',
            created_at=dateutil.parser.parse('2020-09-07 19:20')
        )

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1238,
            action='add',
            created_at=dateutil.parser.parse('2020-09-08 16:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1239,
            action='add',
            created_at=dateutil.parser.parse('2020-09-08 16:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1240,
            action='add',
            created_at=dateutil.parser.parse('2020-09-08 16:00')
        )

        base_licenses = {
            1234,
            1235,
            1236,
        }

        billing_value = calculate_licensed_users(
            self.meta_connection,
            org_id=self.organization['id'],
            to_date='2020-10-04',
            base_licenses=base_licenses
        )
        self.assertEqual(billing_value, 5)

        # теперь удалим одну из лицензий менее чем через 30 минут
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1240,
            action='delete',
            created_at=dateutil.parser.parse('2020-09-08 16:20')
        )

        # теперь расчет должен вернуть четыре
        billing_value = calculate_licensed_users(
            self.meta_connection,
            org_id=self.organization['id'],
            to_date='2020-10-04',
            base_licenses=base_licenses,
        )
        self.assertEqual(billing_value, 4)

        # а если удалить лицензию больше чем через 30 минут
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1239,
            action='delete',
            created_at=dateutil.parser.parse('2020-09-08 16:40')
        )

        # расчет не должен поменяться
        billing_value = calculate_licensed_users(
            self.meta_connection,
            org_id=self.organization['id'],
            to_date='2020-10-04',
            base_licenses=base_licenses,
        )
        self.assertEqual(billing_value, 4)

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1240,
            action='add',
            created_at=dateutil.parser.parse('2020-09-08 18:00')
        )

        # теперь снова добавим пользователю лицензию, расчет опять не должен
        # поменяться так как на текущий момент у нас 4 лицензии
        billing_value = calculate_licensed_users(
            self.meta_connection,
            org_id=self.organization['id'],
            to_date='2020-10-04',
            base_licenses=base_licenses,
        )
        self.assertEqual(billing_value, 4)


        # выдадим лицензию еще одному из удаленных - должно стать 5
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1239,
            action='add',
            created_at=dateutil.parser.parse('2020-09-08 18:00')
        )

        billing_value = calculate_licensed_users(
            self.meta_connection,
            org_id=self.organization['id'],
            to_date='2020-10-04',
            base_licenses=base_licenses,
        )
        self.assertEqual(billing_value, 5)


    def test_calculate_by_log_success_add_to_billing(self):

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1234,
            action='add',
            created_at=dateutil.parser.parse('2020-09-04 15:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1235,
            action='add',
            created_at=dateutil.parser.parse('2020-09-04 15:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1236,
            action='add',
            created_at=dateutil.parser.parse('2020-09-04 15:00')
        )

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1234,
            action='delete',
            created_at=dateutil.parser.parse('2020-09-07 19:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1235,
            action='delete',
            created_at=dateutil.parser.parse('2020-09-07 19:20')
        )

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1238,
            action='add',
            created_at=dateutil.parser.parse('2020-09-08 16:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1239,
            action='add',
            created_at=dateutil.parser.parse('2020-09-08 16:00')
        )
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1240,
            action='add',
            created_at=dateutil.parser.parse('2020-09-08 16:00')
        )

        # тут должно быть 4 так как выдали три лицензии, потом 2 удалили
        # и после этого события выдали еще 2
        billing_value = calculate_licensed_users(
            self.meta_connection,
            org_id=self.organization['id'],
            to_date='2020-10-04',
        )
        self.assertEqual(billing_value, 4)

        # теперь удалим одну из лицензий менее чем через 30 минут
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1240,
            action='delete',
            created_at=dateutil.parser.parse('2020-09-08 16:20')
        )
        # теперь расчет должен вернуть три
        billing_value = calculate_licensed_users(
            self.meta_connection,
            org_id=self.organization['id'],
            to_date='2020-10-04',
        )
        self.assertEqual(billing_value, 3)

        # а если удалить лицензию больше чем через 30 минут
        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=1239,
            action='delete',
            created_at=dateutil.parser.parse('2020-09-08 16:40')
        )

        # расчет не должен поменяться
        billing_value = calculate_licensed_users(
            self.meta_connection,
            org_id=self.organization['id'],
            to_date='2020-10-04',
        )
        self.assertEqual(billing_value, 3)


class Test_calculate_tracker_price(TestCase):
    def setUp(self):
        super().setUp()
        self.create_feature(False, 'disable_billing_tracker')
        ServiceModel(self.main_connection).create(
            id=444,
            slug='tracker',
            name='tracker',
        )

    def test_calculate_success(self):
        for_date = ensure_date('2020-04-05')

        for uid in range(8):
            TrackerLicenseLogModel(self.meta_connection).insert_into_db(
                org_id=self.organization['id'],
                uid=uid,
                action='add',
                created_at=dateutil.parser.parse('2020-03-20 15:00')
            )

        TrackerLicenseLogModel(self.meta_connection).insert_into_db(
            org_id=self.organization['id'],
            uid=123,
            action='add',
            created_at=dateutil.parser.parse('2020-02-20 15:00')
        )

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )
        client_id = OrganizationBillingInfoModel(self.main_connection).get(self.organization['id'])['client_id']

        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )

        result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
        _mark_billing_info_as_sended(billing_ids)

        self.assertEqual(
            result,
            [
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                 'quantity': 240, 'date': '2020-04-05', 'promocode_id': None,
                 'service': 'tracker', 'org_id': self.organization['id']},
            ]
        )
        billing_status = TrackerBillingStatusModel(self.main_connection).filter(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        ).one()

        self.assertTrue(
            billing_status['payment_status']
        )

    def test_calculate_270_success(self):
        for_date = ensure_date('2020-04-05')

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )
        client_id = OrganizationBillingInfoModel(self.main_connection).get(self.organization['id'])['client_id']

        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.billing.utils.calculate_licensed_users',
            return_value=270
        ):
            result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
            _mark_billing_info_as_sended(billing_ids)

        self.assertEqual(
            result,
            [
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                 'quantity': 3000, 'date': '2020-04-05', 'promocode_id': None,
                 'service': 'tracker', 'org_id': self.organization['id']},
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_100'],
                 'quantity': 4500, 'date': '2020-04-05', 'promocode_id': None,
                 'service': 'tracker', 'org_id': self.organization['id']},
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_250'],
                 'quantity': 600, 'date': '2020-04-05', 'promocode_id': None,
                 'service': 'tracker', 'org_id': self.organization['id']}
            ]
        )

    def test_calculate_55_success(self):
        for_date = ensure_date('2020-04-05')

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )
        client_id = OrganizationBillingInfoModel(self.main_connection).get(self.organization['id'])['client_id']

        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.billing.utils.calculate_licensed_users',
            return_value=55
        ):
            result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
            _mark_billing_info_as_sended(billing_ids)
        self.assertEqual(
            result,
            [
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                 'quantity': 1650, 'date': '2020-04-05', 'promocode_id': None,
                 'service': 'tracker', 'org_id': self.organization['id']},
            ]
        )

    def test_calculate_6_success(self):
        for_date = ensure_date('2020-04-05')

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )
        client_id = OrganizationBillingInfoModel(self.main_connection).get(self.organization['id'])['client_id']

        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.billing.utils.calculate_licensed_users',
            return_value=6
        ):
            result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
            _mark_billing_info_as_sended(billing_ids)

        self.assertEqual(
            result,
            [
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                 'quantity': 180, 'date': '2020-04-05', 'promocode_id': None,
                 'service': 'tracker', 'org_id': self.organization['id']},
            ]
        )

    def test_calculate_free_success(self):
        for_date = ensure_date('2020-04-05')

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )

        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.billing.utils.calculate_licensed_users',
            return_value=5
        ):
            result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
            _mark_billing_info_as_sended(billing_ids)

        self.assertEqual(
            result,
            []
        )

    def test_calculate_feature_enable(self):
        for_date = ensure_date('2020-04-05')

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )
        set_feature_value_for_organization(self.meta_connection, self.organization['id'], 'disable_billing_tracker', True)

        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.billing.utils.calculate_licensed_users',
            return_value=55
        ):
            result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
            _mark_billing_info_as_sended(billing_ids)

        self.assertEqual(
            result,
            []
        )


    def test_calculate_multiple_success(self):
        for_date = ensure_date('2020-04-05')

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )
        client_id = OrganizationBillingInfoModel(self.main_connection).get(self.organization['id'])['client_id']

        org2 = self.create_organization()
        self.enable_paid_mode(
            org_id=org2['id'],
        )
        client_id2 = OrganizationBillingInfoModel(self.main_connection).get(org2['id'])['client_id']

        org3 = self.create_organization()
        self.enable_paid_mode(
            org_id=org3['id'],
        )
        OrganizationBillingInfoModel(self.main_connection).filter(org_id=org3['id']).delete()

        TrackerBillingStatusModel(self.main_connection).create(
            org_id=org2['id'],
            payment_date='2020-04-04',
        )
        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )
        TrackerBillingStatusModel(self.main_connection).create(
            org_id=org3['id'],
            payment_date='2020-04-04',
        )
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.billing.utils.calculate_licensed_users',
            return_value=110
        ):
            result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
            _mark_billing_info_as_sended(billing_ids)

        assert_that(
            result,
            contains_inanyorder(
                    {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                     'quantity': 3000, 'date': '2020-04-05', 'promocode_id': None,
                     'service': 'tracker', 'org_id': self.organization['id']},
                    {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_100'],
                     'quantity': 300, 'date': '2020-04-05', 'promocode_id': None,
                     'service': 'tracker', 'org_id': self.organization['id']},
                    {'client_id': client_id2, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                     'quantity': 3000, 'date': '2020-04-05', 'promocode_id': None,
                     'service': 'tracker', 'org_id': org2['id']},
                    {'client_id': client_id2, 'product_id': app.config['TRACKER_PRODUCT_ID_100'],
                     'quantity': 300, 'date': '2020-04-05', 'promocode_id': None,
                     'service': 'tracker', 'org_id': org2['id']},
            )
        )

    def test_calculate_multiple_past_date_already_payed_success(self):
        for_date = ensure_date('2020-04-05')

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )
        client_id = OrganizationBillingInfoModel(self.main_connection).get(self.organization['id'])['client_id']

        org2 = self.create_organization()
        self.enable_paid_mode(
            org_id=org2['id'],
        )

        # рассчет не должен быть произведен если дата в прошлом и payment_status - True
        TrackerBillingStatusModel(self.main_connection).insert_into_db(
            org_id=org2['id'],
            payment_date='2020-06-04',
            payment_status=True,
        )

        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.billing.utils.calculate_licensed_users',
            return_value=110
        ):
            result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
            _mark_billing_info_as_sended(billing_ids)
        self.assertEqual(
            result,
            [
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                 'quantity': 3000, 'date': '2020-04-05', 'promocode_id': None,
                 'service': 'tracker', 'org_id': self.organization['id']},
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_100'],
                 'quantity': 300, 'date': '2020-04-05', 'promocode_id': None,
                 'service': 'tracker', 'org_id': self.organization['id']},
            ]
        )


    def test_calculate_multiple_future_date_success(self):
        for_date = ensure_date('2020-04-05')

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )
        client_id = OrganizationBillingInfoModel(self.main_connection).get(self.organization['id'])['client_id']

        org2 = self.create_organization()
        self.enable_paid_mode(
            org_id=org2['id'],
        )

        # рассчет не должен быть произведен если дата в будущем
        TrackerBillingStatusModel(self.main_connection).create(
            org_id=org2['id'],
            payment_date='2020-06-04',
        )
        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.billing.utils.calculate_licensed_users',
            return_value=110
        ):
            result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
            _mark_billing_info_as_sended(billing_ids)
        self.assertEqual(
            result,
            [
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                 'quantity': 3000, 'date': '2020-04-05', 'promocode_id': None,
                 'service': 'tracker', 'org_id': self.organization['id']},
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_100'],
                 'quantity': 300, 'date': '2020-04-05', 'promocode_id': None,
                 'service': 'tracker', 'org_id': self.organization['id']},
            ]
        )

    def test_calculate_multiple_past_date_success(self):
        for_date = ensure_date('2020-04-05')

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )
        client_id = OrganizationBillingInfoModel(self.main_connection).get(self.organization['id'])['client_id']

        org2 = self.create_organization()
        self.enable_paid_mode(
            org_id=org2['id'],
        )
        client_id2 = OrganizationBillingInfoModel(self.main_connection).get(org2['id'])['client_id']

        # рассчет должен быть произведен если дата в прошлом
        TrackerBillingStatusModel(self.main_connection).create(
            org_id=org2['id'],
            payment_date='2020-03-01',
        )
        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )
        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.billing.utils.calculate_licensed_users',
            return_value=110
        ):
            result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
            _mark_billing_info_as_sended(billing_ids)
        assert_that(
            result,
            contains_inanyorder(
                    {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                     'quantity': 3000, 'date': '2020-04-05', 'promocode_id': None,
                     'service': 'tracker', 'org_id': self.organization['id']},
                    {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_100'],
                     'quantity': 300, 'date': '2020-04-05', 'promocode_id': None,
                     'service': 'tracker', 'org_id': self.organization['id']},
                    {'client_id': client_id2, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                     'quantity': 3000, 'date': '2020-04-05', 'promocode_id': None,
                     'service': 'tracker', 'org_id': org2['id']},
                    {'client_id': client_id2, 'product_id': app.config['TRACKER_PRODUCT_ID_100'],
                     'quantity': 300, 'date': '2020-04-05', 'promocode_id': None,
                     'service': 'tracker', 'org_id': org2['id']},
            )
        )

    def test_with_promocode(self):
        for_date = ensure_date('2020-04-05')

        self.enable_paid_mode(
            org_id=self.organization['id'],
        )
        client_id = OrganizationBillingInfoModel(self.main_connection).get(self.organization['id'])['client_id']


        TrackerBillingStatusModel(self.main_connection).create(
            org_id=self.organization['id'],
            payment_date='2020-04-04',
        )

        promocode_product_id = 12345
        promocode = PromocodeModel(self.meta_connection).create(
            id='TRACKER_50',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'tracker': {
                    100: promocode_product_id,
                },
            },
        )
        promocode2_product_id = 111
        promocode2 = PromocodeModel(self.meta_connection).create(
            id='TRACKER_500',
            activate_before=datetime.date(year=2050, month=1, day=1),
            expires_at=datetime.date(year=2060, month=1, day=1),
            description={
                'ru': 'промо код',
                'en': 'promo code',
            },
            product_ids={
                'tracker': {
                    100: promocode2_product_id,
                },
            },
        )
        # применим один промокод к 5 дням, а второй к 10
        # в результате должен использоваться второй
        for i in range(5):
            OrganizationBillingConsumedInfoModel(self.main_connection).insert_into_db(
                org_id=self.organization['id'],
                for_date='2020-03-1{}'.format(i),
                service='tracker',
                total_users_count=1,
                organization_type='common',
                promocode_id=promocode['id'],
            )

        for i in range(10):
            OrganizationBillingConsumedInfoModel(self.main_connection).insert_into_db(
                org_id=self.organization['id'],
                for_date='2020-03-2{}'.format(i),
                service='tracker',
                total_users_count=1,
                organization_type='common',
                promocode_id=promocode2['id'],
            )

        with patch(
            'intranet.yandex_directory.src.yandex_directory.core.billing.utils.calculate_licensed_users',
            return_value=110
        ):
            result, billing_ids, orgs_by_shard = _get_tracker_billing_data(for_date)
            _mark_billing_info_as_sended(billing_ids)

        self.assertEqual(
            result,
            [
                {'client_id': client_id, 'product_id': app.config['TRACKER_PRODUCT_ID_1'],
                 'quantity': 3000, 'date': '2020-04-05', 'promocode_id': 'TRACKER_500',
                 'service': 'tracker', 'org_id': self.organization['id']},
                {'client_id': client_id, 'product_id': promocode2_product_id,
                 'quantity': 300, 'date': '2020-04-05', 'promocode_id': 'TRACKER_500',
                 'service': 'tracker', 'org_id': self.organization['id']},
            ]
        )
        self.assertEqual(
            TrackerBillingStatusModel(self.main_connection).filter(
                org_id=self.organization['id'],
                payment_status=False,
            ).count(),
            0
        )
        _set_next_payment(orgs_by_shard)
        self.assertEqual(
            TrackerBillingStatusModel(self.main_connection).filter(
                org_id=self.organization['id'],
                payment_status=False,
            ).count(),
            1
        )
        billing_status = TrackerBillingStatusModel(self.main_connection).filter(
            org_id=self.organization['id'],
            payment_status=False,
        ).one()
        self.assertEqual(billing_status['payment_date'], ensure_date('2020-05-04'))


