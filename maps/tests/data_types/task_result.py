import random
import os
from data_types.head_unit import HeadUnit


class TaskResult:
    def __init__(self, task_id=None, result=None, head_unit=None, error=False):
        self.task_id = task_id
        self.head_unit = head_unit or HeadUnit()
        self.result = result or TaskResult.result()
        self.error = error

    @staticmethod
    def result(length=None):
        length = length or random.randint(128, 4 * 1024 * 1024)
        return os.urandom(length)

    @staticmethod
    def task_id():
        return random.randint(1, 1000000)
