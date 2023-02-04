# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from builtins import str as text
from future import standard_library

standard_library.install_aliases()

from hamcrest import assert_that

from brest.core.exception import SnoutException
from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class Exception1(SnoutException):
    pass


class Exception2(SnoutException):
    pass


class TestCaseErrorHandler(TestCaseApiAppBase):
    def test_caused_by(self):
        def func1():
            try:
                raise Exception1('func1 error')
            except SnoutException:
                func2()

        def func2():
            raise Exception2('func2 error')

        try:
            func1()
        except SnoutException as e:
            assert_that(isinstance(e, Exception2))
            assert_that(all(e.caused_by))
            assert_that('Caused by:' in text(e))

            caused_by_exc = e.caused_by[0]

            assert_that(isinstance(caused_by_exc, Exception1))
            assert_that(not all(caused_by_exc.caused_by))
            assert_that('Caused by:' not in text(caused_by_exc))
