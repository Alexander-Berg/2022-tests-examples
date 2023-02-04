import argparse
import os
import textwrap
import tempfile
from contextlib import contextmanager
from functools import partial

import pytest
import six

from sepelib.core import config
from sepelib.core.exceptions import Error


def test_key_value_parsing():
    wrong_values = [
        'asddsd',  # no '='
        '1aaaa=1111',  # starts with number
        ',aaaa=1111',  # starts with comma,
        'aaaa aaaa=1111'  # key has spaces
    ]
    for i in wrong_values:
        with pytest.raises(ValueError):
            config._parse_key_value(i)
    good_values = [
        ('_A=B', ('_A', 'B')),
        ('port=23333', ('port', '23333')),
        (' port=8080', ('port', '8080'))
    ]
    for input_, output in good_values:
        assert config._parse_key_value(input_) == output


def test_augment_args_parser():
    parser = argparse.ArgumentParser()
    config.augment_args_parser(parser)
    namespace = parser.parse_args(args=[])
    assert namespace.config_context == []
    namespace = parser.parse_args(['-V', 'key=value', '-V', 'key1=value1'])
    assert namespace.config_context == [('key', 'value'), ('key1', 'value1')]
    # Now check that error values fail
    with pytest.raises(SystemExit):
        parser.parse_args(['-V', '111=value'])


@contextmanager
def mock_config_loader(text, defaults, context):
    config_fd = defaults_fd = None
    config_file_name = defaults_file_name = None

    try:
        config_fd, config_file_name = tempfile.mkstemp()
        defaults_fd, defaults_file_name = tempfile.mkstemp()
        # Write config files
        os.write(config_fd, six.ensure_binary(str(text), 'utf-8'))
        os.write(defaults_fd, six.ensure_binary(str(defaults), 'utf-8'))

        yield partial(config.load, path=config_file_name, defaults=defaults_file_name, config_context=context)

    finally:
        if config_fd is not None:
            os.close(config_fd)
        if config_file_name:
            os.remove(config_file_name)
        if defaults_fd is not None:
            os.close(defaults_fd)
        if defaults_file_name:
            os.remove(defaults_file_name)


class TestConfigPatchedWithJinja(object):
    def test_loads_values_from_context(self):
        text = textwrap.dedent("""
        run:
            production: false
            auth: true
        web:
            http:
                port: {{ port }}
                ip: 0.0.0.0
        """)
        defaults = textwrap.dedent("""
        log:
            file_path: /usr/local/www/logs/google_borg_{{ port }}.log
        """)
        context = {
            'port': '8080'
        }

        with mock_config_loader(text, defaults, context) as config_loader:
            loaded_config = config_loader()
            assert config.get_value('web.http.port', config=loaded_config) == 8080
            assert config.get_value('log.file_path', config=loaded_config) == '/usr/local/www/logs/google_borg_8080.log'

    def test_raises_exception_on_missing_values_in_strict_mode(self):
        text = textwrap.dedent("""
        web:
            http:
                port: {{ port }}
                ip: 0.0.0.0
        """)

        with mock_config_loader(text, defaults="", context={}) as config_loader:
            with pytest.raises(Error):
                config_loader(strict=True)

    def test_does_not_raise_exception_on_missing_values_in_non_strict_mode(self):
        text = textwrap.dedent("""
        web:
            http:
                port: {{ port }}
                ip: 0.0.0.0
        """)
        defaults = textwrap.dedent("""
        log:
            file_path: /usr/local/www/logs/google_borg_{{ port }}.log
        """)

        with mock_config_loader(text, defaults, context={}) as config_loader:
            loaded_config = config_loader()
            assert config.get_value("web.http.port", config=loaded_config) is None

    def test_does_not_raise_exception_on_missing_defaults_in_strict_mode(self):
        text = textwrap.dedent("""
        web:
            http:
                port: 8080
                ip: 0.0.0.0
        """)
        defaults = textwrap.dedent("""
        web:
            http:
                port: {{ port }}
                ip: {{ ip_address }}
        """)

        with mock_config_loader(text, defaults, context={}) as config_loader:
            loaded_config = config_loader(strict=True)
            assert config.get_value("web.http.port", config=loaded_config) == 8080


def test_get_context_from_env():
    with pytest.raises(ValueError):
        config.get_context_from_env(prefix=('SEPELIB', 'BSCONFIG'))

    env = {
        'PATH': '/bin',
        'SEPELIB_port': '8080',
        'SEPELIB_logpath': '/usr/local/www/logs/sepelib-service.log'
    }
    context = {
        'port': '8080',
        'logpath': '/usr/local/www/logs/sepelib-service.log'
    }
    assert config.get_context_from_env(env_getter=lambda: env) == context
    bad_env = {
        'SEPELIB_port': '8080',
        'SEPELIB_log path': '/usr/local/www/logs/sepelib-service.log'  # Has space in key
    }
    bad_context = {
        'port': '8080'
    }
    with pytest.raises(ValueError):
        config.get_context_from_env(env_getter=lambda: bad_env)
    assert config.get_context_from_env(env_getter=lambda: bad_env, ignore_errors=True) == bad_context
