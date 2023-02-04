import pytest
from mock import Mock, patch
from json import loads

from django.test import TestCase

from staff.emission.django.emission_master.models import MasterLog, NotSent, Sent
from staff.emission.django.emission_master.storage import MasterOrmStorage


class MasterOrmStorageTest(TestCase):
    def test_get_filtered_qs(self):
        # """Проверка фильтации qs, если задана 0 дельта"""
        MasterLog.objects.create(id=10, data='[]')
        MasterLog.objects.create(id=5, data='[]')

        storage = MasterOrmStorage()

        full_log_qs = (
            MasterLog.objects
            .order_by('id')
            .values_list('id', flat=True)
        )
        storage.get_queryset = Mock(return_value=full_log_qs)

        logs = list(storage.get_filtered_qs())

        self.assertEqual(logs, [5, 10])

    def test_get_slice_till_end(self):
        # """Получение обрезанного по start итератора."""
        for id_ in range(1, 11):
            MasterLog.objects.create(id=id_)

        storage = MasterOrmStorage()

        full_log_qs = MasterLog.objects.all().values('id')
        storage.get_filtered_qs = Mock(return_value=full_log_qs)

        logs = storage.get_slice(start=8, max_rows=100500)

        self.assertEqual([r['id'] for r in logs], [8, 9, 10])

    def test_get_slice_max_rows_limited(self):
        # """Получение обрезанного по start и max_rows итератора."""
        for id_ in range(1, 11):
            MasterLog.objects.create(id=id_)

        storage = MasterOrmStorage()
        full_log_qs = MasterLog.objects.all().values('id')
        storage.get_filtered_qs = Mock(return_value=full_log_qs)

        logs = storage.get_slice(start=3, max_rows=3)
        self.assertEqual([r['id'] for r in logs], [3, 4, 5])

    def test_get_slice_by_start_and_stop(self):
        # """Получить кусок лога по границам, не выходящим за лог."""
        for id_ in range(1, 11):
            MasterLog.objects.create(id=id_)

        storage = MasterOrmStorage()

        full_log_qs = MasterLog.objects.all().values('id')
        storage.get_filtered_qs = Mock(return_value=full_log_qs)

        logs = storage.get_slice(start=3, stop=5)

        self.assertEqual([r['id'] for r in logs], [3, 4, 5])

    def test_get_slice_by_start_and_stop_out_of_range(self):
        # """Получить кусок лога по границам, выходящим за лог."""
        for id in range(3, 6):
            MasterLog.objects.create(id=id)

        storage = MasterOrmStorage()

        full_log_qs = MasterLog.objects.all().values('id')
        storage.get_filtered_qs = Mock(return_value=full_log_qs)

        logs = storage.get_slice(start=-10, stop=500)

        self.assertEqual([r['id'] for r in logs], [3, 4, 5])

    def test_get_slice_by_start_only(self):
        # """Получить кусок лога по границам, выходящим за лог."""
        for id_ in range(3, 6):
            MasterLog.objects.create(id=id_)

        storage = MasterOrmStorage()

        full_log_qs = MasterLog.objects.all().values('id')
        storage.get_filtered_qs = Mock(return_value=full_log_qs)

        logs = storage.get_slice(start=4)

        self.assertEqual([r['id'] for r in logs], [4, 5])

    def test_insert_by_id(self):
        # """Добавление записи по нужному id"""
        MasterLog.objects.create(id=1, data='[]')

        storage = MasterOrmStorage()

        storage.insert(msg_id=10, data='my_data', action='modify')

        assert MasterLog.objects.filter(id=10).exists()

    def test_update_data_by_id(self):
        # """Обновление записи по нужному id"""
        MasterLog.objects.create(id=1, data='[]')

        storage = MasterOrmStorage()

        storage.insert(msg_id=1, data='my_data', action='modify')

        updated = MasterLog.objects.get(id=1)
        self.assertEqual(updated.data, 'my_data')

    def test_get_record_by_id(self):
        # """Получение записи по нужному id"""
        MasterLog.objects.create(id=1, data='[]')

        storage = MasterOrmStorage()

        record = storage.get_one(msg_id=1)

        self.assertTrue('creation_time' in record)
        del record['creation_time']
        self.assertEqual(record, {'id': 1, 'data': '[]', 'action': 'modify'})

    def test_get_last_id(self):
        # """Получение последней записи в логе."""
        MasterLog.objects.create(id=1, data='[]')
        MasterLog.objects.create(id=50, data='[]')

        storage = MasterOrmStorage()

        biggest_id = storage.get_last_id()

        self.assertEqual(biggest_id, 50)

    def test_delete_record_by_id(self):
        # """Удаление записи в логе по id."""
        MasterLog.objects.create(id=1, data='[]')
        MasterLog.objects.create(id=50, data='[]')

        storage = MasterOrmStorage()

        storage.delete_one(msg_id=50)

        self.assertEqual(MasterLog.objects.count(), 1)
        self.assertTrue(MasterLog.objects.filter(id=1).exists())

    def test_bulk_create(self):
        storage = MasterOrmStorage()
        storage.bulk_create([
            ('[1]', 'modify'),
            ('[1]', 'modify'),
            ('[1]', 'modify'),
        ])
        assert MasterLog.objects.count() == 3

    def test_get_unsent(self):
        ml = MasterLog.objects.create(id=1, data='[]')
        MasterLog.objects.create(id=2, data='[]')
        NotSent.objects.create(entry=ml)

        storage = MasterOrmStorage()
        self.assertEqual(storage.get_unsent_queryset().count(), 1)

    def test_mark_sent(self):
        # given
        ml = MasterLog.objects.create(id=1, data='[]')
        NotSent.objects.create(entry=ml)

        storage = MasterOrmStorage()

        # when
        self.assertEqual(Sent.objects.count(), 0)
        storage.mark_sent(MasterLog.objects.filter(id=1))

        # then
        self.assertEqual(storage.get_unsent_queryset().count(), 0)
        self.assertEqual(Sent.objects.count(), 1)


@pytest.mark.django_db
def test_transaction():
    from staff.emission.django.emission_master.controller import controller
    from staff.lib.testing import CountryFactory
    from staff.map.models import Country

    with patch('staff.emission.django.emission_master.models.logged_models', {Country}):
        CountryFactory()
        last_emission_data = list(controller.cached_objects.values())[-1][-1][0]
        last_emission_data = loads(last_emission_data)[0]
        assert last_emission_data['pk']
