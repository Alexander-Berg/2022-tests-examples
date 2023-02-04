import collections
from builtins import object, range, str
from uuid import uuid4

import requests
from mock import MagicMock

from django.conf import settings
from django.db import connection

from kelvin.reports.reports import ClickHouseIterator, SqlIterator


class TestIterators(object):
    COLS_COUNT = 2
    ROWS_COUNT = 3

    @staticmethod
    def _sample_row():
        return tuple(str(uuid4()) for _ in range(TestIterators.COLS_COUNT))

    @staticmethod
    def _sample_data():
        return tuple(
            TestIterators._sample_row()
            for _ in range(TestIterators.ROWS_COUNT)
        )

    def test_sql(self, mocker):
        mocker.patch.object(connection, 'cursor')

        header = TestIterators._sample_row()
        description = tuple((name,) + (None,) * 6 for name in header)
        data = TestIterators._sample_data()

        connection.cursor().description = description
        connection.cursor().fetchone.side_effect = data + (None,)

        query = str(uuid4())
        iterator = SqlIterator(query=query)
        assert isinstance(iterator, collections.Iterator)

        cursor = iterator.cursor
        cursor.execute.assert_called_once_with(query)

        result = tuple(iterator)
        assert cursor.fetchone.call_count == TestIterators.ROWS_COUNT + 1
        assert result == (header,) + data

    def test_clickhouse(self, mocker):
        query = str(uuid4())
        filename = str(uuid4())
        response_text = str(uuid4())

        def mock_post(url, auth, params, timeout):
            assert url == settings.REPORTS_CLICKHOUSE_HOST

            user, pwd = auth
            assert user == settings.REPORTS_CLICKHOUSE_USER
            assert pwd == settings.REPORTS_CLICKHOUSE_PASSWORD

            assert 'query' in params
            assert params['query'] == query

            result = MagicMock(text=response_text)
            return result

        mocker.patch.object(
            requests,
            'post',
            new=MagicMock(side_effect=mock_post)
        )

        iterator = ClickHouseIterator([(filename, query)])
        assert isinstance(iterator, collections.Iterator)

        result = tuple(iterator)
        # no assert_called_once_with as user can pass extra
        # parameters to `params` and change `timeout`
        assert requests.post.call_count == 1
        assert result == ((filename, response_text),)
