# encoding: utf-8
from __future__ import unicode_literals

from mock import Mock
from datetime import datetime, timedelta

from django.db import models
from django.test import TestCase

from emission.django.emission_master.models import MasterLog
from emission.django.emission_master.storage import MasterOrmStorage


class MasterOrmStorageTest(TestCase):
    TRANSACTION_WAIT_DELTA = 0

    def test_get_filtered_qs_wo_delta(self):
        # """Проверка фильтации qs, если задана 0 дельта"""
        MasterLog.objects.create(id=10, data='[]')
        MasterLog.objects.create(id=5, data='[]')

        storage = MasterOrmStorage(transaction_wait_delta=0)

        full_log_qs = (
            MasterLog.objects
            .order_by('id')
            .values_list('id', flat=True)
        )
        storage.get_queryset = Mock(return_value=full_log_qs)

        logs = list(storage.get_filtered_qs())

        self.assertEqual(logs, [5, 10])

    def test_get_filtered_qs_w_delta(self):
        # """Проверка фильтации qs, если задана существенная дельта"""
        MasterLog.objects.create(
            id=10, data='[]',
            creation_time=datetime.now() - timedelta(1)
        )
        MasterLog.objects.create(id=5, data='[]')

        storage = MasterOrmStorage(transaction_wait_delta=30)

        full_log_qs = (
            MasterLog.objects
            .order_by('id')
            .values_list('id', flat=True)
        )
        storage.get_queryset = Mock(return_value=full_log_qs)

        logs = list(storage.get_filtered_qs())

        self.assertEqual(logs, [10])

    def test_get_slice_till_end(self):
        # """Получение обрезанного по start итератора."""
        for id_ in range(1, 11):
            MasterLog.objects.create(id=id_)

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)

        full_log_qs = MasterLog.objects.all().values('id')
        storage.get_filtered_qs = Mock(return_value=full_log_qs)

        logs = storage.get_slice(start=8, max_rows=100500)

        self.assertEqual([r['id'] for r in logs], [8, 9, 10])

    def test_get_slice_max_rows_limited(self):
        # """Получение обрезанного по start и max_rows итератора."""
        for id_ in range(1, 11):
            MasterLog.objects.create(id=id_)

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)
        full_log_qs = MasterLog.objects.all().values('id')
        storage.get_filtered_qs = Mock(return_value=full_log_qs)

        logs = storage.get_slice(start=3, max_rows=3)
        self.assertEqual([r['id'] for r in logs], [3, 4, 5])

    def test_get_slice_by_start_and_stop(self):
        # """Получить кусок лога по границам, не выходящим за лог."""
        for id_ in range(1, 11):
            MasterLog.objects.create(id=id_)

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)

        full_log_qs = MasterLog.objects.all().values('id')
        storage.get_filtered_qs = Mock(return_value=full_log_qs)

        logs = storage.get_slice(start=3, stop=5)

        self.assertEqual([r['id'] for r in logs], [3, 4, 5])

    def test_get_slice_by_start_and_stop_out_of_range(self):
        # """Получить кусок лога по границам, выходящим за лог."""
        for id in range(3, 6):
            MasterLog.objects.create(id=id)

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)

        full_log_qs = MasterLog.objects.all().values('id')
        storage.get_filtered_qs = Mock(return_value=full_log_qs)

        logs = storage.get_slice(start=-10, stop=500)

        self.assertEqual([r['id'] for r in logs], [3, 4, 5])

    def test_get_slice_by_start_only(self):
        # """Получить кусок лога по границам, выходящим за лог."""
        for id_ in range(3, 6):
            MasterLog.objects.create(id=id_)

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)

        full_log_qs = MasterLog.objects.all().values('id')
        storage.get_filtered_qs = Mock(return_value=full_log_qs)

        logs = storage.get_slice(start=4)

        self.assertEqual([r['id'] for r in logs], [4, 5])


#    Тут нужно freezegun подключить или что-то такое, нужно патчить дату
#    def test_delete_by_date(self):
#      # """Удаление из лога записей до конкретной даты."""
#        fixed_time = datetime(2012, 12, 21, 12, 0, 0)
#        TestEmissionLog.objects.create(id=1,
#            creation_time=fixed_time - timedelta(days=1))
#        TestEmissionLog.objects.create(id=2,
#            creation_time=fixed_time)
#        TestEmissionLog.objects.create(id=3,
#            creation_time=fixed_time + timedelta(days=1))
#
#        backend = OrmBackend(app_name='tests', log_name=self.log_model_path)
#
#        backend.cut(date=date(2012, 12, 21))
#
#        self.assertEqual(
#            set(TestEmissionLog.objects.values_list('id', flat=True)),
#            set([3])
#        )

    def test_append_log_record(self):
        # """Добавление записи в конец лога"""
        MasterLog.objects.create(id=1, data='[]')
        MasterLog.objects.create(id=5, data='[]')

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)

        storage.append(data='my_data', action='modify')

        self.assertTrue(
            MasterLog.objects.filter(data='my_data').exists()
        )

        new_record = MasterLog.objects.get(data='my_data')

        self.assertEqual(
            MasterLog.objects.order_by('-id')[0],
            new_record
        )

    def test_insert_by_id(self):
        # """Добавление записи по нужному id"""
        MasterLog.objects.create(id=1, data='[]')

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)

        storage.insert(msg_id=10, data='my_data', action='modify')

        self.assertTrue(
            MasterLog.objects.filter(id=10).exists()
        )

    def test_update_data_by_id(self):
        # """Обновление записи по нужному id"""
        MasterLog.objects.create(id=1, data='[]')

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)

        storage.insert(msg_id=1, data='my_data', action='modify')

        updated = MasterLog.objects.get(id=1)
        self.assertEqual(updated.data, 'my_data')

    def test_get_record_by_id(self):
        # """Получение записи по нужному id"""
        MasterLog.objects.create(id=1, data='[]')

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)

        record = storage.get_one(msg_id=1)

        self.assertTrue('creation_time' in record)
        del record['creation_time']
        self.assertEqual(record, {'id': 1, 'data': '[]', 'action': 'modify'})

    def test_get_last_id(self):
        # """Получение последней записи в логе."""
        MasterLog.objects.create(id=1, data='[]')
        MasterLog.objects.create(id=50, data='[]')

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)

        biggest_id = storage.get_last_id()

        self.assertEqual(biggest_id, 50)

    def test_delete_record_by_id(self):
        # """Удаление записи в логе по id."""
        MasterLog.objects.create(id=1, data='[]')
        MasterLog.objects.create(id=50, data='[]')

        storage = MasterOrmStorage(self.TRANSACTION_WAIT_DELTA)

        storage.delete_one(msg_id=50)

        self.assertEqual(MasterLog.objects.count(), 1)
        self.assertTrue(MasterLog.objects.filter(id=1).exists())

