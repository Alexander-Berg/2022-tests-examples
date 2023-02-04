# -*- coding: utf-8 -*-
import pylua


def test_eval_raw():
    assert int(pylua.eval_raw('return 2 + 2')) == 4


def test_lua_vars():
    assert pylua.eval_raw('return global_var', {'global_var': 'hello'}) == 'hello'


def test_lua_int_vars():
    assert int(pylua.eval_raw('return global_var + 2', {'global_var': '2'})) == 4
