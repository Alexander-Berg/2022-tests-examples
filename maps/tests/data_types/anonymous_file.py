import random
import rstr
import string
import os
from data_types.head_unit import HeadUnit


class AnonymousFile:
    def __init__(self, file_id=None, contents=None, head_unit=None):
        self.file_id = file_id or AnonymousFile.file_id()
        self.head_unit = head_unit or HeadUnit()
        self.contents = contents or AnonymousFile.contents()

    @staticmethod
    def contents(length=None):
        length = length or random.randint(128, 4 * 1024 * 1024)
        return os.urandom(length)

    @staticmethod
    def file_id():
        return rstr.rstr(string.ascii_letters, 5)
