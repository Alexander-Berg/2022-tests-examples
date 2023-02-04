# -*- coding: utf-8 -*-

import re
import decimal

import pickle

# NOTE: данный код требует выполенения на раннем этапе кода из balance/usercustomize.py
# (прочтите документацию к стандартному модулю site)


class pytest_regex(object):
    """Assert that a given string meets some expectations."""

    def __init__(self, pattern, flags=0):
        self._regex = re.compile(pattern, flags)

    def __eq__(self, actual):
        return bool(self._regex.match(actual))

    def __repr__(self):
        return self._regex.pattern


class TestCdecimal(object):
    def test_decimal_is_cdecimal(self):
        assert pytest_regex('.*cdecimal.*\.(so|pyd)$') == decimal.__file__

        import cdecimal
        assert id(decimal) == id(cdecimal)

    def test_cdecimal_pickle(self):
        self.test_decimal_is_cdecimal()
        D = decimal.Decimal
        p = pickle.dumps(D(666, context=decimal.Inexact))
        # NOTE: c -- это опкод pickle'а, модуль cdecimal будет сериализован как ccdecimal,
        # смотри ниже тест balance_tests.test_cdecimal.TestCdecimal#test_unpickle_from_cdecimal
        assert pytest_regex('^cdecimal$', re.MULTILINE) == p

        assert pickle.loads(p) == D(666)

        p = pickle.dumps(pickle.PickleError)
        pickle.loads(p)

        p = pickle.dumps(pickle.PickleError('66666'))
        pickle.loads(p)

        p = pickle.dumps(pickle.dumps)
        pickle.loads(p)

        p = pickle.dumps(id)
        pickle.loads(p)

    def test_cdecimal_opt_bug(self):
        self.test_decimal_is_cdecimal()

        class L(decimal.Decimal):
            def __new__(cls, value=None):
                self = super(L, cls).__new__(cls, value)
                self.xxx = 666
                return self

        D = decimal.Decimal
        d = D(666)
        x = L(666)
        assert d == D('666.000')
        assert id(d) != id(x)
        assert x.xxx == 666

    def test_unpickle_from_cdecimal(self):
        self.test_decimal_is_cdecimal()
        p = '''ccdecimal
Decimal
p0
(S'6666'
p1
tp2
Rp3
.'''
        d = pickle.loads(p)
        D = decimal.Decimal
        assert d == D(6666)
        assert isinstance(d, D)
