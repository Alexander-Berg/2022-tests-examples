from datetime import date
import pytest
from paysys.sre.balance_notifier.yql_over_yt import yql_over_yt
from unittest import mock
import json
import pandas as pd


class TestYqlOverYt:
    class TestDailyResultTableName:
        @pytest.fixture
        def prefix_name(self):
            return ''

        @pytest.fixture
        def url(self):
            return ''

        @pytest.mark.parametrize('prefix_name', ['SDMD6N14-6J61'])
        @pytest.mark.parametrize('url', ['https://test.sender.yandex-team.ru/api/0/yandex.balance/transactional/SDMD6N14-6J61'])
        def test_make_daily_result_table_name_url(self, prefix_name, url):
            expected_table_name = prefix_name+'.'+date.today().strftime('%Y%m%d')
            result_table_name = yql_over_yt.make_daily_result_table_name(url)
            assert expected_table_name == result_table_name

        @pytest.mark.parametrize('prefix_name', ['empty_notify_id'])
        def test_make_daily_result_table_name_empty_url(self, prefix_name, url):
            expected_table_name = prefix_name+'.'+date.today().strftime('%Y%m%d')
            result_table_name = yql_over_yt.make_daily_result_table_name(url)
            assert expected_table_name == result_table_name

    class TestQueryProcessing:
        @pytest.fixture
        def query_type(self):
            return 'YQL'

        @pytest.fixture
        def query(self, query_type):
            return [{'query': '',
                     'notify_url': '',
                     'query_type': query_type,
                     'to': '',
                     'cc': '',
                     'bcc': ''}]

        @pytest.fixture
        def file_with_query(self):
            return 'file_with_query.txt'

        @pytest.fixture
        def yql_token(self):
            return 'fake_token'

        @pytest.fixture
        def yt_token(self):
            return 'fake_token'

        @pytest.fixture
        def out_file(self):
            return 'out_file'

        @pytest.fixture
        def cluster(self):
            return 'hahn'

        @pytest.fixture
        def yql_table_return_value(self):
            return ''

        @pytest.fixture
        def yql_client_mock_check_if_table_exist(self, mocker, yql_table_return_value):
            yql_mock = mocker.patch('paysys.sre.balance_notifier.yql_over_yt.yql_over_yt.YqlClientWrapper.check_if_table_exist')
            yql_mock.return_value = yql_table_return_value
            return yql_mock

        @pytest.fixture
        def yql_client_mock_get_data_from_yt(self, mocker):
            return mocker.patch(
                'paysys.sre.balance_notifier.yql_over_yt.yql_over_yt.YqlClientWrapper.get_data_from_yt')

        @pytest.fixture
        def save_data_to_yt(self, mocker):
            return mocker.patch(
                'paysys.sre.balance_notifier.yql_over_yt.yql_over_yt.save_data_to_yt')

        @pytest.fixture
        def path(self):
            return f"home/balance/prod/tmp/empty_notify_id.{date.today().strftime('%Y%m%d')}"

        @pytest.fixture
        def mock_open(self, query):
            with mock.patch('paysys.sre.balance_notifier.yql_over_yt.yql_over_yt.open',
                            mock.mock_open(read_data=json.dumps(query))) as mock_yql_over_yt_open:
                yield mock_yql_over_yt_open

        @pytest.mark.parametrize('query_type', ['SQL'])
        @pytest.mark.parametrize('yql_table_return_value', 'Y')
        @pytest.mark.usefixtures('mock_open')
        def test_wrong_query_type(self, yql_client_mock_check_if_table_exist, file_with_query, yql_token, yt_token,
                                  out_file, cluster, query_type, yql_table_return_value):
            yql_over_yt.query_processing(file_with_query, yql_token, yt_token, out_file, cluster)
            yql_client_mock_check_if_table_exist.assert_not_called()

        @pytest.mark.parametrize('yql_table_return_value', 'Y')
        @pytest.mark.usefixtures('mock_open')
        def test_correct_query_type(self, yql_client_mock_check_if_table_exist, file_with_query, yql_token, yt_token,
                                    out_file, cluster, path, yql_table_return_value):
            yql_over_yt.query_processing(file_with_query, yql_token, yt_token, out_file, cluster)
            yql_client_mock_check_if_table_exist.assert_called_once_with(path, yql_token, cluster)

        @pytest.mark.usefixtures('mock_open')
        def test_correct_query_type_and_table_not_exists(self, yql_client_mock_get_data_from_yt,
                                                         yql_client_mock_check_if_table_exist, query,
                                                         file_with_query, yql_token, yt_token, out_file, cluster):
            yql_over_yt.query_processing(file_with_query, yql_token, yt_token, out_file, cluster)
            yql_client_mock_get_data_from_yt.assert_called_once_with(query[0], yql_token, cluster)

        @pytest.mark.usefixtures('mock_open')
        def test_correct_query_type_empty_result(self, yql_client_mock_get_data_from_yt,
                                                 yql_client_mock_check_if_table_exist, save_data_to_yt, file_with_query,
                                                 yql_token, yt_token, out_file, cluster):
            yql_client_mock_get_data_from_yt.return_value = pd.DataFrame()
            yql_over_yt.query_processing(file_with_query, yql_token, yt_token, out_file, cluster)
            save_data_to_yt.assert_not_called()

        @pytest.mark.usefixtures('mock_open')
        @pytest.mark.usefixtures('yql_client_mock_check_if_table_exist')
        def test_correct_query_type_save_result(self, yql_client_mock_get_data_from_yt, save_data_to_yt, query,
                                                file_with_query, yql_token, yt_token, out_file, cluster, path):
            df = pd.DataFrame(query)
            yql_client_mock_get_data_from_yt.return_value = df
            yql_over_yt.query_processing(file_with_query, yql_token, yt_token, out_file, cluster)
            save_data_to_yt.assert_called_once_with(df, yql_token, yt_token, cluster, path)
