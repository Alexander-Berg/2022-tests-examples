import random
import time

from wiki.async_process.backend.handlers.base import BaseJobHandler
from wiki.async_process.serializers import AsyncResponse


class TestJobHandler(BaseJobHandler):
    @classmethod
    def process_task(cls, task_data):
        if random.randrange(0, 4) == 0:
            raise ValueError('Test exception')

        fake_calc_time_seconds = random.randrange(1, 61)
        time.sleep(fake_calc_time_seconds)
        task_data['result'] = fake_calc_time_seconds
        return task_data

    @classmethod
    def transform_result(cls, result):
        return AsyncResponse.from_success({'wrapped': result})
