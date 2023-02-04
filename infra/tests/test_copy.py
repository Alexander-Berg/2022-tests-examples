import os
import unittest

from api.copier import Copier
from api.poll import Poller


class SimpleCopyTest(unittest.TestCase):
    def test_copy(self):
        copier = Copier()
        task1 = copier.copy('localhost', '/etc/passwd', 'localhost', '/var/tmp/passwd12345')  # download existing
        task2 = copier.copy('wniowfbww', '/etc/bebebe', 'localhost', '/var/tmp/passwd123456')  # download nonexisting
        task3 = copier.copy('localhost', '/etc/passwd', 'localhost', '/etc/passwd2')  # download with error
        task5 = copier.copy('localhost', '/etc/bebebe', 'wniowfbww', '/var/tmp/passwd123456')  # upload nonexisting
        task4 = copier.copy('localhost', '/etc/passwd', 'localhost', '/var/tmp/passwd12345')  # download existing
        task6 = copier.copy('localhost', '/etc/shadow', 'localhost', '/var/tmp/passwd123456')  # download with error

        poll = Poller([task1, task2, task3, task4, task5, task6])
        tries = 0
        while not poll.finished() and tries < 5:
            poll.poll(5)
            tries += 1  # during the test I suppose that all the tasks should finish in a reasonable time
        self.assertTrue(poll.finished())
        self.assertTrue(task1.reason is None, task1.reason)
        self.assertTrue(task2.reason is not None)
        self.assertTrue(task3.reason is not None)
        self.assertTrue(task4.reason is None, (task4.reason, task4.product))
        self.assertTrue(task5.reason is not None)
        self.assertTrue(task6.reason is not None)

        os.unlink('/var/tmp/passwd12345')

if __name__ == '__main__':
    unittest.main()
