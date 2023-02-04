# -*- coding: utf-8 -*-
import time
import exceptions


class Multirun(object):
    def __init__(self, max_retries=7, sum_delay=None, plan=None, exc_type=None):
        super(Multirun, self).__init__()
        if plan is not None:
            self.__plan = plan
        elif sum_delay is not None:
            self.__plan = [1.0 * sum_delay / max_retries] * max_retries
        else:
            self.__plan = [0.1 * 2 ** cnt for cnt in range(max_retries)]
        if exc_type is not None:
            self.__exc_type = exc_type
        else:
            self.__exc_type = (AssertionError, exceptions.AssertionError)
        self.__counter = 0
        self.__passed = False

    def __iter__(self):
        return self

    def next(self):
        if not self.__passed and self.__counter < self.__max_retries:
            time.sleep(self.__plan[self.__counter])
            self.__counter += 1
            return self

        else:
            raise StopIteration()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        if exc_type is None:
            self.__passed = True
            return True
        if issubclass(exc_type, self.__exc_type) and self.__counter < self.__max_retries:
            return True
        else:
            return False

    @property
    def __max_retries(self):
        return len(self.__plan)
