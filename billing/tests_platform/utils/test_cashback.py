from agency_rewards.rewards.config import Config
from agency_rewards.cashback.utils import CashBackDiffer
from agency_rewards.rewards.utils.yql_crutches import create_yt_client, create_yql_client
from billing.agency_rewards.tests_platform.common import TestBase
from billing.agency_rewards.tests_platform.generators.utils.cashback import BASE_YT_PATH


class TestCashBackDiffer(TestBase):
    """
    У 2 таблиц имеются совпадающие и расходящиеся данные
    """

    correct_table_path = f'{BASE_YT_PATH}/correct-table'
    wrong_table_path = f'{BASE_YT_PATH}/wrong-table'

    def test_yt_tables_diff(self):
        yt_client = create_yt_client(cluster=Config.clusters[0])
        yql_client = create_yql_client()
        differ = CashBackDiffer(
            correct_table_path=self.wrong_table_path,
            result_table_path=self.correct_table_path,
            yt_client=yt_client,
            yql_client=yql_client,
        )

        result = differ.get_yt_tables_diff()
        self.assertEqual(result['status'], 'error')
        self.assertIsNotNone(result['message'])

        # проверка несовпадающих объектов
        expected = [
            # расходятся amt
            dict(
                correct_client_id=1,
                correct_currency='USD',
                correct_reward=3.0,
                calc_client_id=1,
                calc_currency='USD',
                calc_reward=2.0,
            ),
            dict(
                correct_client_id=2,
                correct_currency='USD',
                correct_reward=6.0,
                calc_client_id=2,
                calc_currency='USD',
                calc_reward=4.0,
            ),
            dict(
                correct_client_id=3,
                correct_currency='USD',
                correct_reward=9.0,
                calc_client_id=3,
                calc_currency='USD',
                calc_reward=6.0,
            ),
            # этих данных нет в таблице с эталонными данными
            dict(
                correct_client_id=None,
                correct_currency=None,
                correct_reward=None,
                calc_client_id=4,
                calc_currency='USD',
                calc_reward=8.0,
            ),
            dict(
                correct_client_id=None,
                correct_currency=None,
                correct_reward=None,
                calc_client_id=5,
                calc_currency='USD',
                calc_reward=10.0,
            ),
            dict(
                correct_client_id=None,
                correct_currency=None,
                correct_reward=None,
                calc_client_id=6,
                calc_currency='USD',
                calc_reward=12.0,
            ),
            # этих данных нет в таблице с результатами
            dict(
                correct_client_id=7,
                correct_currency='USD',
                correct_reward=21.0,
                calc_client_id=None,
                calc_currency=None,
                calc_reward=None,
            ),
            dict(
                correct_client_id=8,
                correct_currency='USD',
                correct_reward=24.0,
                calc_client_id=None,
                calc_currency=None,
                calc_reward=None,
            ),
            dict(
                correct_client_id=9,
                correct_currency='USD',
                correct_reward=27.0,
                calc_client_id=None,
                calc_currency=None,
                calc_reward=None,
            ),
        ]

        self.assertEqual(len(differ._inner), len(expected))
        for obj in expected:
            self.assertIn(obj, differ._inner)
        for obj in differ._inner:
            self.assertIn(obj, expected)


class TestCashBackDifferSameTables(TestBase):
    """
    Таблицы полностью совпадают. Строки в разном порядке
    """

    result_table_path = f'{BASE_YT_PATH}/same_tables_1'
    correct_table_path = f'{BASE_YT_PATH}/same_tables_2'

    def test_yt_tables_diff(self):
        yt_client = create_yt_client(cluster=Config.clusters[0])
        yql_client = create_yql_client()

        differ = CashBackDiffer(self.result_table_path, self.correct_table_path, yt_client, yql_client)
        result = differ.get_yt_tables_diff()

        self.assertEqual(result['status'], 'ok', result)
        self.assertEqual(differ._inner, [])


class TestCashBackDifferMissingRequiredColumns(TestBase):
    """
    Проверяет случай когда у таблиц отсутствуют необходимые колонки
    """

    t1 = f'{BASE_YT_PATH}/missing_columns_1'
    t2 = f'{BASE_YT_PATH}/missing_columns_2'

    def test_yt_tables_diff(self):
        yt_client = create_yt_client(cluster=Config.clusters[0])
        yql_client = create_yql_client()

        differ = CashBackDiffer(self.t1, self.t2, yt_client, yql_client)
        result = differ.get_yt_tables_diff()

        self.assertEqual(result['status'], 'error', result)
        error_message = (
            f'Table {self.t1} does not have such columns: currency\n'
            f'Table {self.t2} does not have such columns: reward\n'
        )

        self.assertEqual(result['message'], error_message)


class TestCashBackDifferRounding(TestBase):
    """
    Проверяет округление при вычислении расхождения
    BALANCE-31965
    """

    t1 = f'{BASE_YT_PATH}/round_tables_1'
    t2 = f'{BASE_YT_PATH}/round_tables_2'

    def test_yt_tables_differ_rounding(self):
        yt_client = create_yt_client(cluster=Config.clusters[0])
        yql_client = create_yql_client()

        differ = CashBackDiffer(
            correct_table_path=self.t2, result_table_path=self.t1, yt_client=yt_client, yql_client=yql_client
        )

        result = differ.get_yt_tables_diff()
        self.assertEqual(result['status'], 'error')
        self.assertIsNotNone(result['message'])

        # проверка несовпадающих объектов
        expected = [
            # расходятся reward
            dict(
                correct_client_id=2,
                correct_currency='USD',
                correct_reward=1.21,
                calc_client_id=2,
                calc_currency='USD',
                calc_reward=1.22,
            ),
            dict(
                correct_client_id=3,
                correct_currency='USD',
                correct_reward=1.21,
                calc_client_id=3,
                calc_currency='USD',
                calc_reward=1.22,
            ),
        ]
        self.assertEqual(len(differ._inner), len(expected))

        for obj in expected:
            self.assertIn(obj, differ._inner)

        self.assertEqual(differ._inner, expected)
