# -*- coding: utf-8 -*-

from billing.dcs.tests.utils import BaseTestCase

from billing.dcs.dcs.utils.sql import lock


class AcquireLockTestCase(BaseTestCase):
    name = 'DCS_TEST_LOCK'

    def test_lock_acquired(self):
        nl = lock.NamedLock(self.name)

        nl.acquire()
        nl.release()

        with nl:
            pass

        with lock.NamedLock(self.name):
            pass

    def test_lock_timeout(self):
        with lock.NamedLock(self.name):
            nl = lock.NamedLock(self.name, )
            self.assertRaises(lock.TimeoutException, nl.acquire)

    def test_already_acquired(self):
        nl = lock.NamedLock(self.name)
        nl.acquire()
        self.assertRaises(lock.LockException, nl.acquire)

# vim:ts=4:sts=4:sw=4:tw=79:et:
