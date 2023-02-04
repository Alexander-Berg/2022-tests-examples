from contextlib import contextmanager

import maps.pylibs.yandex_environment.environment as yenv

import maps.analyzer.pylibs.envkit as envkit


@contextmanager
def with_env(env):
    old_env = envkit.config.YENV_BASE
    envkit.config.init_env(env=env)
    yield
    envkit.config.init_env(env=old_env)


@contextmanager
def dev_env():
    with with_env(env=yenv.Environment.DEVELOPMENT):
        yield


@contextmanager
def tst_env():
    with with_env(env=yenv.Environment.TESTING):
        yield


@contextmanager
def prod_env():
    with with_env(env=yenv.Environment.STABLE):
        yield
