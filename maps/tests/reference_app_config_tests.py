import pytest

from pathlib import Path

import yatest.common

from maps.infra.pycare.scripts.make_configs.lib import PycareConfigsGenerator


@pytest.fixture(scope='session')
def testapp_configs() -> PycareConfigsGenerator:
    return PycareConfigsGenerator.from_app_binary(
        app_binary_path=Path(yatest.common.binary_path('maps/infra/pycare/example/bin/example'))
    )


def test_supervisord(testapp_configs):
    assert testapp_configs.supervisord.path == Path('etc/supervisor/conf-available/example.conf')

    testapp_configs.supervisord.write(yatest.common.test_output_path())
    return yatest.common.canonical_file(
        yatest.common.test_output_path() / testapp_configs.supervisord.path,
        local=True
    )


def test_template_generator(testapp_configs):
    assert testapp_configs.template_generator_yaml.path ==\
           Path('etc/template_generator/config.d/example.yaml')

    testapp_configs.template_generator_yaml.write(yatest.common.test_output_path())
    return yatest.common.canonical_file(
        yatest.common.test_output_path() / testapp_configs.template_generator_yaml.path,
        local=True
    )


def test_nginx(testapp_configs):
    assert testapp_configs.nginx.path == Path('etc/nginx/sites-available/example.conf')

    testapp_configs.nginx.write(yatest.common.test_output_path())
    return yatest.common.canonical_file(
        yatest.common.test_output_path() / testapp_configs.nginx.path,
        local=True
    )


def test_roquefort(testapp_configs):
    assert testapp_configs.roquefort.path == Path('etc/yandex/maps/roquefort/example.conf')

    testapp_configs.roquefort.write(yatest.common.test_output_path())
    return yatest.common.canonical_file(
        yatest.common.test_output_path() / testapp_configs.roquefort.path,
        local=True
    )


def test_syslog_ng(testapp_configs):
    assert testapp_configs.syslog_ng.path == Path('etc/syslog-ng/conf-available/example.conf')

    testapp_configs.syslog_ng.write(yatest.common.test_output_path())
    return yatest.common.canonical_file(
        yatest.common.test_output_path() / testapp_configs.syslog_ng.path,
        local=True
    )


def test_logrotate(testapp_configs):
    assert testapp_configs.logrotate.path == \
           Path('etc/template_generator/templates/etc/logrotate.d/example')

    testapp_configs.logrotate.write(yatest.common.test_output_path())
    return yatest.common.canonical_file(
        yatest.common.test_output_path() / testapp_configs.logrotate.path,
        local=True
    )


def test_unified_agent(testapp_configs):
    assert testapp_configs.unified_agent.path == \
           Path('etc/template_generator/templates/etc/yandex/unified_agent/conf.d/04_pycare_example.yaml')

    testapp_configs.unified_agent.write(yatest.common.test_output_path())
    return yatest.common.canonical_file(
        yatest.common.test_output_path() / testapp_configs.unified_agent.path,
        local=True
    )
