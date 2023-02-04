# encoding: utf-8
from __future__ import unicode_literals

import json

import unittest

from mock import Mock, patch, ANY

from emission.core.controller import Controller


class ControllerTest(unittest.TestCase):
    def test_get_by_id_successfull(self):
        # """get_one делегируется storage.get_one."""
        ctl = Controller(storage=Mock())

        MSG_ID = 100500
        ctl.get_one(msg_id=MSG_ID)

        ctl.storage.get_one.assert_called_once_with(MSG_ID)

    def test_delete_by_id_successfull(self):
        # """delete_one делегируется storage.delete_one."""
        ctl = Controller(storage=Mock())

        MSG_ID = 100500
        ctl.delete_one(msg_id=MSG_ID)

        ctl.storage.delete_one.assert_called_once_with(MSG_ID)

    def test_getitem_by_positive_int_key(self):
        # """Индексация по положительному целочисленному ключу."""
        ctl = Controller(storage=None)
        ctl.get_one = Mock()

        MSG_ID = 1
        msg = ctl[MSG_ID]

        ctl.get_one.assert_called_once_with(MSG_ID)

    def test_getitem_by_not_positive_int_key(self):
        # """Индексация по неположительному целочисленному ключу."""
        ctl = Controller(storage=None)

        with self.assertRaises(KeyError):
            msg = ctl[0]

        with self.assertRaises(KeyError):
            msg = ctl[-10]

    def test_getitem_by_start_stop_slice(self):
        # """Получение среза по двум границами."""
        ctl = Controller(storage=None)
        ctl.get_slice = Mock()

        MSG_START_ID, MSG_STOP_ID = 1, 10
        msg = ctl[MSG_START_ID:MSG_STOP_ID]

        ctl.get_slice.assert_called_once_with(MSG_START_ID, MSG_STOP_ID)

    def test_getitem_by_start_slice(self):
        # """Получение среза по левой границе."""
        ctl = Controller(storage=None)
        ctl.get_slice = Mock()

        MSG_START_ID = 1
        msg = ctl[MSG_START_ID:]

        ctl.get_slice.assert_called_once_with(MSG_START_ID, None)

    def test_getitem_by_start_stop_slice_stop_lt_start(self):
        # """Получение среза, если правая граница меньше левой."""
        ctl = Controller(storage=None)

        with self.assertRaises(KeyError):
            msg = ctl[10:5]

    def test_getitem_by_no_start(self):
        # """Получение среза, если правая граница не задана."""
        ctl = Controller(storage=None)

        with self.assertRaises(KeyError):
            msg = ctl[:100]

    def test_getitem_by_negative_bounds(self):
        # """Получение среза, если границы отрицательные."""
        ctl = Controller(storage=None)

        with self.assertRaises(KeyError):
            msg = ctl[-1:]

        with self.assertRaises(KeyError):
            msg = ctl[-50:-10]

        with self.assertRaises(KeyError):
            msg = ctl[-50:10]


    def test_limit_right_boundary_normal_case(self):
        # """Ограничение среза. Обычный случай"""
        storage = Mock(**{'get_last_id.return_value': 5})
        ctl = Controller(storage=storage)

        right_bound = ctl._limit_right_bound(start=1, stop=2, max_rows=None)

        self.assertEqual(right_bound, 2)

    def test_limit_right_boundary_last_id_limitation(self):
        # """Ограничение среза. Запрошено больше, чем есть в логе."""
        storage = Mock(**{'get_last_id.return_value': 5})
        ctl = Controller(storage=storage)

        right_bound = ctl._limit_right_bound(start=1, stop=100, max_rows=None)

        self.assertEqual(right_bound, 5)

    def test_limit_right_boundary_max_rows_limitation(self):
        # """Ограничение среза. Явно ограничено параметром max_rows"""
        storage = Mock(**{'get_last_id.return_value': 5})
        ctl = Controller(storage=storage)

        right_bound = ctl._limit_right_bound(start=1, stop=None, max_rows=2)

        self.assertEqual(right_bound, 2)

    def test_get_slice_wo_gaps(self):
        # """Получить от бэкенда срез без пустышек"""
        slice = [{'id': 1}, {'id': 2}]
        storage = Mock(**{'get_slice.return_value': slice})
        ctl = Controller(storage=storage)
        ctl._limit_right_bound = Mock(return_value=2)

        messages = ctl.get_slice(start=1, stop=ANY)

        self.assertEqual(list(messages), slice)

    def test_get_slice_with_gap_in_middle(self):
        # """Получить от бэкенда срез c пустышкой в середине"""
        slice = [{'id': 1}, {'id': 3}]
        storage = Mock(**{'get_slice.return_value': slice})
        ctl = Controller(storage=storage)
        ctl._limit_right_bound = Mock(return_value=3)

        messages = ctl.get_slice(start=1, stop=ANY)

        slice.insert(1, {'id': 2, 'data': '[]'})
        self.assertEqual(list(messages), slice)

    def test_get_slice_with_gap_in_end(self):
        # """Получить от бэкенда срез c пустышкой в конце"""
        slice = [{'id': 1}, {'id': 2}]
        storage = Mock(**{'get_slice.return_value': slice})
        ctl = Controller(storage=storage)
        ctl._limit_right_bound = Mock(return_value=3)  # в бэкенде есть записи id > 3

        messages = ctl.get_slice(start=1, stop=ANY)

        slice.append({'id': 3, 'data': '[]'})
        self.assertEqual(list(messages), slice)

    def test_get_slice_with_gap_in_beginning(self):
        # """Получить от бэкенда срез c пустышкой в начале"""
        slice = [{'id': 2}, {'id': 3}]
        storage = Mock(**{'get_slice.return_value': slice})
        ctl = Controller(storage=storage)
        ctl._limit_right_bound = Mock(return_value=3)  # в бэкенде есть записи id > 3

        messages = ctl.get_slice(start=1, stop=ANY)

        slice.insert(0, {'id': 1, 'data': '[]'})
        self.assertEqual(list(messages), slice)

    def test_get_slice_long(self):
        # """Получить от бэкенда срез до конца лога."""
        slice = [{'id': 2}, {'id': 3}, {'id': 4}]
        storage = Mock(**{'get_slice.return_value': slice})
        ctl = Controller(storage=storage)
        ctl._limit_right_bound = Mock(return_value=3)  # в бэкенде есть записи id > 3

        messages = ctl.get_slice(start=2, stop=None, max_rows=0)

        self.assertEqual(list(messages), slice)


    def test_save_new_object_into_log(self):
        # """Сохранить новую запись в лог"""
        data = '[{"id": 100500}]'
        storage = Mock()
        storage.serialize_objects = Mock(return_value=data)

        ctl = Controller(storage=storage)

        ctl.append(obj=object(), action='modify')

        ctl.storage.append.assert_called_once_with(data, 'modify')

    def test_insert_object_into_log(self):
        # """Вставить запись в лог по id"""
        ctl = Controller(storage=Mock())
        data = '[{id: 100500}]'
        ctl.insert(msg_id=100500, data=data, action='modify')

        ctl.storage.insert.assert_called_once_with(100500, data, 'modify')


    def test_get_iterator_with_rows(self):
        # """Получить от бэкенда итератор два раза и поелдить его"""
        def storage_get_slice(start, *args, **kwargs):
            if start > 4:
                return iter([])
            return iter([{'id': start}, {'id': start + 1}])

        ctl = Controller(storage=Mock())
        ctl.storage.get_slice = storage_get_slice

        collected_messages = ctl.get_iterator(from_id=1)

        self.assertEqual(
            list(collected_messages),
            [{'id': 1}, {'id': 2}, {'id': 3}, {'id': 4}]
        )

    def test_get_iterator_with_one_row(self):
        # """Получить от бэкенда итератор один раз и поелдить его"""
        def storage_get_slice(start, *args, **kwargs):
            if start > 1:
                # один раз отдаем результат
                return iter([])
            return iter([{'id': start}, {'id': start + 1}])

        ctl = Controller(storage=Mock())
        ctl.storage.get_slice = storage_get_slice

        collected_messages = ctl.get_iterator(from_id=1)

        self.assertEqual(
            list(collected_messages),
            [{'id': 1}, {'id': 2}]
        )

    def test_get_iterator_without_rows(self):
        # """Получить от бэкенда пустой итератор и поелдить его"""
        def storage_get_slice(start, *args, **kwargs):
            return iter([])

        ctl = Controller(storage=Mock())
        ctl.storage.get_slice = storage_get_slice

        collected_messages = ctl.get_iterator(from_id=1)

        self.assertEqual(
            list(collected_messages), []
        )

    # controllers utils

    def test_empty_messages(self):
        # """Пустышкогенератор"""
        ctl = Controller(storage=None)

        messages = ctl._generate_empty_messages(start=1, stop=5)

        self.assertEqual(len(list(messages)), 5)



