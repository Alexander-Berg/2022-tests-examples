import os
import signal
import time
from time import sleep

from django.test import SimpleTestCase as TestCase

from catalogue.worker import PartsWorker
from django_dbq.models import Job, JobType


def locking_func():
    while 1:
        sleep(1)


class TestPartsWorker(TestCase):
    def test_handle_warm_shutdown_request(self):
        worker = PartsWorker.get_worker()
        pid = os.fork()
        queue, *_ = worker.queues
        Job.save = lambda *_: ...
        job = queue.enqueue(locking_func, args=(Job(name="TestFunc", settings=JobType()),))
        if pid == 0:
            worker.work()
            print("bye")
            os._exit(0)

        while not (job.is_started or job.is_failed, job.is_finished):
            time.sleep(1)
        os.kill(pid, signal.SIGTERM)
        os.wait()
        self.assertTrue(job.is_queued)
