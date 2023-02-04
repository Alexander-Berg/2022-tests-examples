# -*- coding: utf-8 -*-
"""
This is not a test, it's a collection of mock objects for tests
"""
import unittest
import time
import re

import util as ut

class MockLog(object):
    def __init__(self):
        self.lines = []
    def debug(self, s):
        self.lines.append(('D', s))
    def info(self, s):
        self.lines.append(('I', s))
    def error(self, s):
        self.lines.append(('E', s))

def func_raises(e = Exception):
    raise e

class MockCursor(object):
    def __init__(self, rows = None):
        self.rows = rows
        self.invalid = False
        self.callproc__called = 0
        self.execute__called = 0
        self.fetchall__called = 0
    def check_valid(self):
        if self.invalid:
            raise Exception()
    def callproc(self, name, params):
        self.check_valid()
        self.callproc__called += 1
        self.callproc__name = name
        self.callproc__params = params
    def execute(self, sql, args):
        self.check_valid()
        if sql == 'RAISE':
            raise Exception()
        self.execute__called += 1
        self.execute__sql = sql
        self.execute__args = args
    def fetchall(self):
        self.check_valid()
        if self.execute__called == 0 or self.fetchall__called != 0:
            raise Exception()
        self.fetchall__called += 1
        return self.rows

class MockConnection(object):
    def __init__(self, rows = None):
        self.closed = False
        self.rows = rows
        self.cursors = []
        self.trans = []
    def check_valid(self):
        if self.closed:
            raise Exception()
    def flush_cursors(self):
        for c in self.cursors:
            c.invalid = True
    def commit(self):
        self.check_valid()
        self.flush_cursors()
        self.trans.append('C')
    def rollback(self):
        self.check_valid()
        self.flush_cursors()
        self.trans.append('R')
    def close(self):
        self.check_valid()
        self.flush_cursors()
        self.closed = True
    def cursor(self):
        self.check_valid()
        cursor = MockCursor(self.rows)
        self.cursors.append(cursor)
        return cursor

class MockError(Exception):
    pass

def incr(x):
    x[0] += 1
    return x[0]

class MockCallable(object):
    def __init__(self):
        self.cnt = 0
    def user_action(self):
        pass
    def call(self):
        self.user_action()
        self.cnt += 1

