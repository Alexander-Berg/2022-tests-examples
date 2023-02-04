from django.test import TestCase

from staff.emission.logbroker.models import LogbrokerLog, NotSent, Sent
from staff.emission.logbroker.storage import LogbrokerOrmStorage


class LogbrokerOrmStorageTest(TestCase):
    TRANSACTION_WAIT_DELTA = 0

    def test_bulk_create(self):
        storage = LogbrokerOrmStorage(self.TRANSACTION_WAIT_DELTA)
        storage.bulk_create([
            ('[1]', 'modify'),
            ('[1]', 'modify'),
            ('[1]', 'modify'),
        ])
        assert LogbrokerLog.objects.count() == 3

    def test_get_unsent(self):
        ml = LogbrokerLog.objects.create(id=1, data='[]')
        LogbrokerLog.objects.create(id=2, data='[]')
        NotSent.objects.create(entry=ml)

        storage = LogbrokerOrmStorage(self.TRANSACTION_WAIT_DELTA)
        self.assertEqual(storage.get_unsent_queryset().count(), 1)

    def test_mark_sent(self):
        # given
        ml = LogbrokerLog.objects.create(id=1, data='[]')
        NotSent.objects.create(entry=ml)

        storage = LogbrokerOrmStorage(self.TRANSACTION_WAIT_DELTA)

        # when
        self.assertEqual(Sent.objects.count(), 0)
        storage.mark_sent(LogbrokerLog.objects.filter(id=1))

        # then
        self.assertEqual(storage.get_unsent_queryset().count(), 0)
        self.assertEqual(Sent.objects.count(), 1)
