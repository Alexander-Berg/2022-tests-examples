from billing.agency_rewards.tests_platform.common import TestBase
from .platform import prepare_yt_tables

BASE_YT_PATH = "//home/balance/dev/yb-ar/regress/cashback-differ"

SCHEMA = [
    dict(name='client_id', type='int64'),
    dict(name='currency', type='string'),
    dict(name='reward', type='double'),
    dict(name='d', type='boolean'),
    dict(name='e', type='int64'),
    dict(name='f', type='int64'),
]


class TestCashBackDiffer(TestBase):
    """
    У 2 таблиц имеются совпадающие и расходящиеся данные
    """

    correct_table_path = f'{BASE_YT_PATH}/correct-table'
    wrong_table_path = f'{BASE_YT_PATH}/wrong-table'

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        result_table = {'path': cls.correct_table_path, 'schema': SCHEMA}
        correct_table = {'path': cls.wrong_table_path, 'schema': SCHEMA}
        prepare_yt_tables(yt_client, result_table, correct_table)

        result_table_data, correct_table_data = [], []

        for i in range(1, 12):
            if i <= 3:
                # одинаковые contract_id, discount_type,reward, но разные reward
                result_table_data.append(dict(client_id=i, currency='USD', d=True, e=i, f=i, reward=2.0 * i))
                correct_table_data.append(dict(client_id=i, currency='USD', d=True, e=i, f=i, reward=3.0 * i))

            elif 4 <= i <= 6:
                result_table_data.append(dict(client_id=i, currency='USD', d=True, e=i, f=i, reward=2.0 * i))
            elif 7 <= i <= 9:
                correct_table_data.append(dict(client_id=i, currency='USD', d=True, e=i, f=i, reward=3.0 * i))

            else:
                # совпадающие данные
                result_table_data.append(dict(client_id=i, currency='USD', d=True, e=i, f=i, reward=4.0 * i))
                correct_table_data.append(dict(client_id=i, currency='USD', d=True, e=i, f=i, reward=4.0 * i))

        yt_client.write_table(result_table['path'], result_table_data)
        yt_client.write_table(correct_table['path'], correct_table_data)


class TestCashBackDifferSameTables(TestBase):
    """
    Таблицы полностью совпадают. Строки в разном порядке
    """

    result_table_path = f'{BASE_YT_PATH}/same_tables_1'
    correct_table_path = f'{BASE_YT_PATH}/same_tables_2'

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        results_table = {'path': cls.result_table_path, 'schema': SCHEMA}
        correct_table = {'path': cls.correct_table_path, 'schema': SCHEMA}

        prepare_yt_tables(yt_client, results_table, correct_table)

        results_table_data = [
            dict(client_id=1, currency='USD', d=True, e=1, f=1, reward=1.0),
            dict(client_id=2, currency='USD', d=True, e=2, f=2, reward=2.0),
            dict(client_id=3, currency='USD', d=True, e=3, f=3, reward=3.0),
        ]

        correct_table_data = [
            dict(client_id=3, currency='USD', d=True, e=3, f=3, reward=3.0),
            dict(client_id=1, currency='USD', d=True, e=1, f=1, reward=1.0),
            dict(client_id=2, currency='USD', d=True, e=2, f=2, reward=2.0),
        ]

        yt_client.write_table(results_table['path'], results_table_data)
        yt_client.write_table(correct_table['path'], correct_table_data)


class TestCashBackDifferMissingRequiredColumns(TestBase):
    """
    Проверяет случай когда у таблиц отсутствуют необходимые колонки
    """

    t1 = f'{BASE_YT_PATH}/missing_columns_1'
    t2 = f'{BASE_YT_PATH}/missing_columns_2'

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        t1_scheme = [e for e in SCHEMA if e["name"] != "currency"]
        t2_scheme = [e for e in SCHEMA if e["name"] not in ("f", "reward")]

        results_table = {'path': cls.t1, 'schema': t1_scheme}
        correct_table = {'path': cls.t2, 'schema': t2_scheme}

        prepare_yt_tables(yt_client, results_table, correct_table)


class TestCashBackDifferRounding(TestBase):
    """
    Проверяет округление при вычислении расхождения
    BALANCE-31965
    """

    t1 = f'{BASE_YT_PATH}/round_tables_1'
    t2 = f'{BASE_YT_PATH}/round_tables_2'

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        results_table = {'path': cls.t1, 'schema': SCHEMA}
        correct_table = {'path': cls.t2, 'schema': SCHEMA}

        prepare_yt_tables(yt_client, results_table, correct_table)

        results_table_data = [
            dict(client_id=1, currency='USD', d=True, e=1, f=1, reward=1.214999),
            dict(client_id=2, currency='USD', d=True, e=1, f=1, reward=1.215999),
            dict(client_id=3, currency='USD', d=True, e=1, f=1, reward=1.215),
        ]

        correct_table_data = [
            dict(client_id=1, currency='USD', d=True, e=1, f=1, reward=1.214),
            dict(client_id=2, currency='USD', d=True, e=1, f=1, reward=1.214999),
            dict(client_id=3, currency='USD', d=True, e=1, f=1, reward=1.214999),
        ]

        yt_client.write_table(results_table['path'], results_table_data)
        yt_client.write_table(correct_table['path'], correct_table_data)
