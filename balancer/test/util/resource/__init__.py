# -*- coding: utf-8 -*-
from abc import ABCMeta, abstractmethod


class AbstractResource(object):
    __metaclass__ = ABCMeta

    def __init__(self):
        super(AbstractResource, self).__init__()
        self.__finished = False

    def finish(self):
        if not self.__finished:
            try:
                self._finish()
            finally:
                self.__finished = True

    def set_finished(self):
        self.__finished = True

    @abstractmethod
    def _finish(self):
        raise NotImplementedError()

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.finish()
        return False


class AbstractResourceManager(object):
    def __init__(self):
        super(AbstractResourceManager, self).__init__()
        self.__resources = list()

    def register(self, resource):
        self.__resources.append(resource)

    def _finish_all(self):
        first_exc = None
        while self.__resources:
            res = self.__resources.pop()
            try:
                res.finish()
            except Exception, exc:  # pylint: disable=broad-except
                if first_exc is None:
                    first_exc = exc
        if first_exc is not None:
            raise first_exc  # pylint: disable=raising-bad-type
