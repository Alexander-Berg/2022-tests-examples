# -*- coding: utf-8 -*-
import functools


class Memoize(object):
    def __init__(self, function):
        super(Memoize, self).__init__()
        self.__function = function
        self.__cache = dict()

    def __call__(self, *args, **kwargs):
        key = args, tuple(sorted(kwargs.iteritems()))
        if key not in self.__cache:
            self.__cache[key] = self.__function(*args, **kwargs)
        return self.__cache[key]

    def __get__(self, obj, objtype):
        """Support instance methods"""
        return functools.partial(self.__call__, obj)
