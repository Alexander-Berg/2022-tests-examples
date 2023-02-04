# -*- coding: utf-8 -*-
from unittest.mock import patch, Mock

from intranet.yandex_directory.src.yandex_directory.common.yt.utils import (
    create_table_if_needed,
    append_rows_to_table,
)

from testutils import TestCase, assert_not_called


class Test__create_table_if_needed(TestCase):
    def test_should_not_create_table_if_it_is_already_exists(self):
        # проверяем, что create_table_if_needed не создаёт таблицу, если она уже есть в yt
        table_name = 'test_table'

        mocked_yt_client = Mock()
        mocked_yt_client.exists.return_value = True

        with patch('intranet.yandex_directory.src.yandex_directory.common.yt.utils.yt_client', mocked_yt_client):
            create_table_if_needed(table=table_name, schema={})

        mocked_yt_client.exists.assert_called_once_with(table_name)
        assert_not_called(mocked_yt_client.create_table)

    def test_should_use_custom_yt_client(self):
        # проверяем, что create_table_if_needed будет использовать кастомный yt-клиент, если его передать
        table_name = 'test_table'

        mocked_yt_client = Mock()
        custom_yt_client = Mock()
        custom_yt_client.exists.return_value = True

        with patch('intranet.yandex_directory.src.yandex_directory.common.yt.utils.yt_client', mocked_yt_client):
            create_table_if_needed(table=table_name, schema={}, client=custom_yt_client)

        custom_yt_client.exists.assert_called_once_with(table_name)
        assert_not_called(mocked_yt_client.exists)

    def test_should_create_table_if_it_is_not_exists(self):
        # проверяем, что create_table_if_needed создаёт таблицу рекурсивно, если её нет в yt
        table_name = 'test_table'
        table_schema = [
            {'name': 'id', 'type': 'int64', 'sort_order': 'ascending'},
            {'name': 'name', 'type': 'string'},
        ]

        mocked_yt_client = Mock()
        mocked_yt_client.exists.return_value = False

        with patch('intranet.yandex_directory.src.yandex_directory.common.yt.utils.yt_client', mocked_yt_client):
            create_table_if_needed(table=table_name, schema=table_schema)

        mocked_yt_client.exists.assert_called_once_with(table_name)

        attributes = {
            'dynamic': False,
            'schema': table_schema,
        }
        mocked_yt_client.create.assert_called_once_with(
            'table',
            table_name,
            attributes=attributes,
            recursive=True,
        )


class Test__append_rows_to_table(TestCase):
    def test_should_append_rows_to_table(self):
        # проверяем, что вызывается метод write_table у yt-клиента с правильным именем таблицы,
        # в которое должен добавиться атрибут append=true
        table_name = 'test_table'
        rows_data = [{'id': 1, 'name': 'Alexander'}]
        mocked_yt_client = Mock()

        with patch('intranet.yandex_directory.src.yandex_directory.common.yt.utils.yt_client', mocked_yt_client):
            append_rows_to_table(table=table_name, rows_data=rows_data)

        exp_table_name = '<append=%%true>%s' % table_name
        mocked_yt_client.write_table.assert_called_once_with(exp_table_name, rows_data)

    def test_should_use_custom_client(self):
        # проверяем, что если передадим кастомный yt-клиент, функция будет использовать его
        table_name = 'test_table'
        rows_data = {'id': 1, 'name': 'Alexander'}
        mocked_yt_client = Mock()
        custom_mocked_client = Mock()

        with patch('intranet.yandex_directory.src.yandex_directory.common.yt.utils.yt_client', mocked_yt_client):
            append_rows_to_table(table=table_name, rows_data=rows_data, client=custom_mocked_client)

        exp_table_name = '<append=%%true>%s' % table_name
        custom_mocked_client.write_table.assert_called_once_with(exp_table_name, rows_data)
        assert_not_called(mocked_yt_client)
