import argparse
import os
import textwrap
import tempfile

import pytest

from sepelib.core import config


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


def test_config_patched_with_jinja():
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
    config_fd = defaults_fd = None
    config_file_name = defaults_file_name = None
    try:
        config_fd, config_file_name = tempfile.mkstemp()
        defaults_fd, defaults_file_name = tempfile.mkstemp()
        # Write config files
        os.write(config_fd, text)
        os.write(defaults_fd, defaults)

        config.load(config_file_name, defaults=defaults_file_name, config_context=context)
        assert config.get_value('web.http.port') == 8080
        assert config.get_value('log.file_path') == '/usr/local/www/logs/google_borg_8080.log'
    finally:
        if config_fd is not None:
            os.close(config_fd)
        if config_file_name:
            os.remove(config_file_name)
        if defaults_fd is not None:
            os.close(defaults_fd)
        if defaults_file_name:
            os.remove(defaults_file_name)


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
