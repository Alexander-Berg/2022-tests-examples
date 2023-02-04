# -*- coding: utf-8 -*-

from autodasha.core.config import AbstractConfig


class TestConfig(AbstractConfig):
    def __init__(self, items=None):
        self._items = items or dict()

    def __getitem__(self, item):
        return self._items[item]

    def __setitem__(self, key, value):
        self._items[key] = value

    def __getattr__(self, item):
        try:
            return self._items[item]
        except KeyError as e:
            raise AttributeError(*e.args)

    def get(self, key, default=None):
        return self._items.get(key, default)
