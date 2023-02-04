from billing.agency_rewards.tests_platform.common import TestBase


def prepare_yt_tables(yt_client, *paths):
    for d in paths:
        yt_client.remove(d['path'], force=True)
        yt_client.create('table', d['path'], recursive=True, attributes={'schema': d['schema']})


class TestYtTablesDiffer(TestBase):
    """
    У 2 таблиц имеются совпадающие и расходящиеся данные
    """

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        common_schema = [
            dict(name='contract_id', type='int64'),
            dict(name='discount_type', type='int64'),
            dict(name='amt', type='double'),
            dict(name='reward', type='double'),
            dict(name='d', type='boolean'),
            dict(name='e', type='int64'),
            dict(name='f', type='int64'),
        ]
        result_table = {'path': '//home/balance/dev/tamirok/wrong_table', 'schema': common_schema}
        correct_table = {'path': '//home/balance/dev/tamirok/correct_table', 'schema': common_schema}

        prepare_yt_tables(yt_client, result_table, correct_table)

        result_table_data, correct_table_data = [], []

        for i in range(1, 12):
            if i <= 3:
                # одинаковые contract_id, discount_type,reward, но разные amt
                result_table_data.append(
                    dict(contract_id=i, discount_type=i, amt=2.0 * i, d=True, e=i, f=i, reward=2.0 * i)
                )
                correct_table_data.append(
                    dict(contract_id=i, discount_type=i, amt=3.0 * i, d=True, e=i, f=i, reward=2.0 * i)
                )

            elif 4 <= i <= 6:
                result_table_data.append(
                    dict(contract_id=i, discount_type=i, amt=2.0 * i, d=True, e=i, f=i, reward=2.0 * i)
                )
            elif 7 <= i <= 9:
                correct_table_data.append(
                    dict(contract_id=i, discount_type=i, amt=3.0 * i, d=True, e=i, f=i, reward=3.0 * i)
                )

            else:
                # совпадающие данные
                result_table_data.append(
                    dict(contract_id=i, discount_type=i, amt=4.0 * i, d=True, e=i, f=i, reward=4.0 * i)
                )
                correct_table_data.append(
                    dict(contract_id=i, discount_type=i, amt=4.0 * i, d=True, e=i, f=i, reward=4.0 * i)
                )

        yt_client.write_table(result_table['path'], result_table_data)
        yt_client.write_table(correct_table['path'], correct_table_data)


class TestYtTableDiffSameTables(TestBase):
    """
    Таблицы полностью совпадают. Строки в разном порядке
    """

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        common_schema = [
            dict(name='contract_id', type='int64'),
            dict(name='discount_type', type='int64'),
            dict(name='amt', type='double'),
            dict(name='reward', type='double'),
            dict(name='d', type='boolean'),
            dict(name='e', type='int64'),
            dict(name='f', type='int64'),
        ]

        results_table = {'path': '//home/balance/dev/tamirok/same_tables_1', 'schema': common_schema}
        correct_table = {'path': '//home/balance/dev/tamirok/same_tables_2', 'schema': common_schema}

        prepare_yt_tables(yt_client, results_table, correct_table)

        results_table_data = [
            dict(contract_id=1, discount_type=1, amt=1.0, d=True, e=1, f=1, reward=1.0),
            dict(contract_id=2, discount_type=2, amt=2.0, d=True, e=2, f=2, reward=2.0),
            dict(contract_id=3, discount_type=3, amt=3.0, d=True, e=3, f=3, reward=3.0),
        ]

        correct_table_data = [
            dict(contract_id=2, discount_type=2, amt=2.0, d=True, e=2, f=2, reward=2.0),
            dict(contract_id=1, discount_type=1, amt=1.0, d=True, e=1, f=1, reward=1.0),
            dict(contract_id=3, discount_type=3, amt=3.0, d=True, e=3, f=3, reward=3.0),
        ]

        yt_client.write_table(results_table['path'], results_table_data)
        yt_client.write_table(correct_table['path'], correct_table_data)


class TestMissingRequiredColumns(TestBase):
    """
    Проверяет случай когда у таблиц отсутствуют необходимые колонки
    """

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        results_table_schema = [
            dict(name='discount_type', type='int64'),
            dict(name='contract_id', type='int64'),
            dict(name='reward', type='double'),
            dict(name='d', type='boolean'),
            dict(name='e', type='int64'),
            dict(name='f', type='int64'),
        ]

        correct_table_schema = [
            dict(name='contract_id', type='int64'),
            dict(name='discount_type', type='int64'),
            dict(name='amt', type='double'),
            dict(name='d', type='boolean'),
            dict(name='e', type='int64'),
        ]

        results_table = {'path': '//home/balance/dev/tamirok/missing_columns_1', 'schema': results_table_schema}
        correct_table = {'path': '//home/balance/dev/tamirok/missing_columns_2', 'schema': correct_table_schema}

        prepare_yt_tables(yt_client, results_table, correct_table)


class TestYtTablesDifferRounding(TestBase):
    """
    Проверяет округление при вычислении расхождения
    BALANCE-31965
    """

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        common_schema = [
            dict(name='contract_id', type='int64'),
            dict(name='discount_type', type='int64'),
            dict(name='amt', type='double'),
            dict(name='reward', type='double'),
            dict(name='d', type='boolean'),
            dict(name='e', type='int64'),
            dict(name='f', type='int64'),
        ]

        results_table = {'path': '//home/balance/dev/tamirok/round_tables_1', 'schema': common_schema}
        correct_table = {'path': '//home/balance/dev/tamirok/round_tables_2', 'schema': common_schema}

        prepare_yt_tables(yt_client, results_table, correct_table)

        results_table_data = [
            dict(contract_id=1, discount_type=2, amt=1.214999, d=True, e=1, f=1, reward=2.256),
            dict(contract_id=2, discount_type=2, amt=1.215999, d=True, e=1, f=1, reward=2.256),
            dict(contract_id=3, discount_type=2, amt=1.214999, d=True, e=1, f=1, reward=2.255),
        ]

        correct_table_data = [
            dict(contract_id=1, discount_type=2, amt=1.214, d=True, e=1, f=1, reward=2.259),
            dict(contract_id=2, discount_type=2, amt=1.214999, d=True, e=1, f=1, reward=2.256),
            dict(contract_id=3, discount_type=2, amt=1.214999, d=True, e=1, f=1, reward=2.256),
        ]

        yt_client.write_table(results_table['path'], results_table_data)
        yt_client.write_table(correct_table['path'], correct_table_data)
