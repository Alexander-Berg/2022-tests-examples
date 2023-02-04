import pytest


def patch_gevent_for_coverage():
    import sys as __sys
    from gevent.hub import Hub as __hub

    __switch = __hub.switch

    def switch(self):
        try:
            trace = __sys.gettrace()
            __sys.settrace(None)
            return __switch(self)
        finally:
            __sys.settrace(trace)

    __hub.switch = switch


def patch_pyjack_connect():
    import pyjack as _pyjack
    import types as _types
    _PyjackFuncCode = _pyjack._PyjackFuncCode

    class __PyjackFuncCode(_PyjackFuncCode):
        def __init__(self, fn, proxyfn):
            fn_hash = hash(fn.func_code)

            _PyjackFuncCode.__init__(self, fn, proxyfn)

            code = [key for key, value in _pyjack._func_code_map.items() if value == self][0]
            new_code = _types.CodeType(
                code.co_argcount,
                code.co_nlocals,
                code.co_stacksize,
                code.co_flags,
                code.co_code,
                code.co_consts,
                code.co_names,
                code.co_varnames,
                code.co_filename,
                '_pyjacked_func__%s' % (fn_hash, ),
                code.co_firstlineno,
                code.co_lnotab
            )
            _pyjack._func_code_map[new_code] = self
            del _pyjack._func_code_map[code]
            self._proxy_func_code = new_code
            fn.func_code = new_code

    _pyjack._PyjackFuncCode = __PyjackFuncCode


def patch_coverage_for_gevent():
    from coverage import collector as _collector, control as _control
    import collections as _collections
    import gevent as _gevent
    _PyTracer = _collector.PyTracer

    class DataStack(object):
        def __init__(self):
            self.__data = _collections.defaultdict(list)

        def __idx(self):
            return hash(_gevent.getcurrent())

        def pop(self):
            return self.__data[self.__idx()].pop()

        def append(self, value):
            return self.__data[self.__idx()].append(value)

    class PyTracer(_PyTracer):
        def __init__(self):
            _PyTracer.__init__(self)
            self.data_stack = DataStack()

    _collector.PyTracer = PyTracer

    _coverage = _control.coverage

    class coverage(_coverage):
        def __init__(self, *args, **kwargs):
            kwargs.setdefault('timid', True)
            super(coverage, self).__init__(*args, **kwargs)
            #return _coverage.__init__(self, *args, **kwargs)

    __import__('pyjack').replace_all_refs(_coverage, coverage)


#patch_gevent_for_coverage()
patch_pyjack_connect()
patch_coverage_for_gevent()


benchmark = pytest.mark.benchmark


def pytest_addoption(parser):
    parser.addoption('--benchmark', action='store_true', help='run benchmark')


def pytest_runtest_setup(item):
    bench = item.config.getoption('--benchmark')
    if 'benchmark' in item.keywords:
        if not bench:
            pytest.skip('need --benchmark option to run')
    elif bench:
        pytest.skip('run only benchmarks')
