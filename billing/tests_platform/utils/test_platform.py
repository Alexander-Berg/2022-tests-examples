from agency_rewards.rewards.config import Config
from agency_rewards.rewards.utils.yt import YtTablesDiffer
from agency_rewards.rewards.utils.yql_crutches import create_yt_client, create_yql_client
from billing.agency_rewards.tests_platform.common import TestBase


def prepare_yt_tables(yt_client, *paths):
    for d in paths:
        yt_client.remove(d['path'], force=True)
        yt_client.create('table', d['path'], recursive=True, attributes={'schema': d['schema']})


class TestYtTablesDiffer(TestBase):
    """
    У 2 таблиц имеются совпадающие и расходящиеся данные
    """

    def test_yt_tables_diff(self):
        yt_client = create_yt_client(cluster=Config.clusters[0])
        yql_client = create_yql_client()
        wrong_table_path = '//home/balance/dev/tamirok/wrong_table'
        correct_table_path = '//home/balance/dev/tamirok/correct_table'
        differ = YtTablesDiffer(
            correct_table_path=correct_table_path,
            result_table_path=wrong_table_path,
            yt_client=yt_client,
            yql_client=yql_client,
            is_need_tci=False,
        )

        result = differ.get_yt_tables_diff()
        self.assertEqual(result['status'], 'error')
        self.assertIsNotNone(result['message'])

        # проверка несовпадающих объектов
        expected_objs = [
            # расходятся amt
            dict(
                correct_contract_id=1,
                correct_discount_type=1,
                correct_amt=3.0,
                correct_reward=2.0,
                calc_contract_id=1,
                calc_discount_type=1,
                calc_amt=2.0,
                calc_reward=2.0,
            ),
            dict(
                correct_contract_id=2,
                correct_discount_type=2,
                correct_amt=6.0,
                correct_reward=4.0,
                calc_contract_id=2,
                calc_discount_type=2,
                calc_amt=4.0,
                calc_reward=4.0,
            ),
            dict(
                correct_contract_id=3,
                correct_discount_type=3,
                correct_amt=9.0,
                correct_reward=6.0,
                calc_contract_id=3,
                calc_discount_type=3,
                calc_amt=6.0,
                calc_reward=6.0,
            ),
            # этих данных нет в таблице с эталонными данными
            dict(
                correct_contract_id=None,
                correct_discount_type=None,
                correct_amt=None,
                correct_reward=None,
                calc_contract_id=4,
                calc_discount_type=4,
                calc_amt=8.0,
                calc_reward=8.0,
            ),
            dict(
                correct_contract_id=None,
                correct_discount_type=None,
                correct_amt=None,
                correct_reward=None,
                calc_contract_id=5,
                calc_discount_type=5,
                calc_amt=10.0,
                calc_reward=10.0,
            ),
            dict(
                correct_contract_id=None,
                correct_discount_type=None,
                correct_amt=None,
                correct_reward=None,
                calc_contract_id=6,
                calc_discount_type=6,
                calc_amt=12.0,
                calc_reward=12.0,
            ),
            # этих данных нет в таблице с результатами
            dict(
                correct_contract_id=7,
                correct_discount_type=7,
                correct_amt=21.0,
                correct_reward=21.0,
                calc_contract_id=None,
                calc_discount_type=None,
                calc_amt=None,
                calc_reward=None,
            ),
            dict(
                correct_contract_id=8,
                correct_discount_type=8,
                correct_amt=24.0,
                correct_reward=24.0,
                calc_contract_id=None,
                calc_discount_type=None,
                calc_amt=None,
                calc_reward=None,
            ),
            dict(
                correct_contract_id=9,
                correct_discount_type=9,
                correct_amt=27.0,
                correct_reward=27.0,
                calc_contract_id=None,
                calc_discount_type=None,
                calc_amt=None,
                calc_reward=None,
            ),
        ]

        self.assertEqual(len(differ._inner), len(expected_objs))
        for obj in expected_objs:
            self.assertIn(obj, differ._inner)
        for obj in differ._inner:
            self.assertIn(obj, expected_objs)


class TestYtTableDiffSameTables(TestBase):
    """
    Таблицы полностью совпадают. Строки в разном порядке
    """

    def test_yt_tables_diff(self):
        yt_client = create_yt_client(cluster=Config.clusters[0])
        yql_client = create_yql_client()
        result_table_path = '//home/balance/dev/tamirok/same_tables_1'
        correct_table_path = '//home/balance/dev/tamirok/same_tables_2'
        differ = YtTablesDiffer(result_table_path, correct_table_path, yt_client, yql_client, False)
        result = differ.get_yt_tables_diff()

        self.assertEqual(result['status'], 'ok', result)
        self.assertEqual(differ._inner, [])


class TestMissingRequiredColumns(TestBase):
    """
    Проверяет случай когда у таблиц отсутствуют необходимые колонки
    """

    result_table_path = '//home/balance/dev/tamirok/missing_columns_1'
    correct_table_path = '//home/balance/dev/tamirok/missing_columns_2'

    yt_client = create_yt_client(cluster=Config.clusters[0])
    yql_client = create_yql_client()

    def test_yt_tables_diff(self):
        differ = YtTablesDiffer(self.result_table_path, self.correct_table_path, self.yt_client, self.yql_client, False)
        result = differ.get_yt_tables_diff()

        self.assertEqual(result['status'], 'error', result)
        error_message = (
            'Table //home/balance/dev/tamirok/missing_columns_1 does not have such columns: amt\n'
            'Table //home/balance/dev/tamirok/missing_columns_2 does not have such columns: reward\n'
        )

        self.assertEqual(result['message'], error_message)

    def test_yt_tables_diff_monthly(self):
        differ = YtTablesDiffer(
            self.result_table_path, self.correct_table_path, self.yt_client, self.yql_client, False, True
        )
        result = differ.get_yt_tables_diff()

        self.assertEqual(result['status'], 'error', result)
        error_message = (
            f'Table {self.result_table_path} does not have such columns: amt, invoice_cnt, invoice_prep_cnt\n'
            f'Table {self.correct_table_path} does not have such columns: invoice_cnt, invoice_prep_cnt, reward\n'
        )

        self.assertEqual(result['message'], error_message)


class TestYtTablesDifferRounding(TestBase):
    """
    Проверяет округление при вычислении расхождения
    BALANCE-31965
    """

    def test_yt_tables_differ_rounding(self):
        yt_client = create_yt_client(cluster=Config.clusters[0])
        yql_client = create_yql_client()
        wrong_table_path = '//home/balance/dev/tamirok/round_tables_1'
        correct_table_path = '//home/balance/dev/tamirok/round_tables_2'
        differ = YtTablesDiffer(
            correct_table_path=correct_table_path,
            result_table_path=wrong_table_path,
            yt_client=yt_client,
            yql_client=yql_client,
            is_need_tci=False,
        )

        result = differ.get_yt_tables_diff()
        self.assertEqual(result['status'], 'error')
        self.assertIsNotNone(result['message'])

        # проверка несовпадающих объектов
        expected_objs = [
            # расходятся amt
            dict(
                correct_contract_id=2,
                correct_discount_type=2,
                correct_amt=1.21,
                correct_reward=2.26,
                calc_contract_id=2,
                calc_discount_type=2,
                calc_amt=1.22,
                calc_reward=2.26,
            ),
            dict(
                correct_contract_id=3,
                correct_discount_type=2,
                correct_amt=1.21,
                correct_reward=2.26,
                calc_contract_id=3,
                calc_discount_type=2,
                calc_amt=1.21,
                calc_reward=2.25,
            ),
        ]
        self.assertEqual(len(differ._inner), len(expected_objs))

        for exp_obj in expected_objs:
            self.assertIn(exp_obj, differ._inner)

        self.assertEqual(differ._inner, expected_objs)
