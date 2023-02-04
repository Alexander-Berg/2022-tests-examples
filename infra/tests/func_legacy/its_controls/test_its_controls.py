# coding=utf-8
from __future__ import unicode_literals

import os
import re

import pytest
import gevent
from flask import Flask, request as flask_request, jsonify
from sepelib.flask.server import WebServer

from utils import must_start_instancectl, must_stop_instancectl
from utils import wait_condition_is_true


FAKE_ITS_SERVER = os.path.join(os.path.dirname(__file__), 'its_stub.py')


@pytest.fixture
def its_params(ctl_environment):
    return {
        'controls': None,
        'etag': None,
        'expect_etag': None,
        'first_poll': True,
        'tags': [t for t in ctl_environment['BSCONFIG_ITAGS'].split() if t.startswith('a_')],
        'wait_before_response': 0,
        'max_age': 0.5,
    }


@pytest.fixture
def its_server(request, its_params):
    app = Flask('instancectl-test-fake-its')

    @app.route('/v1/process/', methods=['POST'])
    def main():
        req = flask_request.json
        assert req == its_params['tags'] or ('i' in req and req['i'] == its_params['tags'])
        assert flask_request.headers.get('If-None-Match') == its_params['expect_etag']
        if its_params['first_poll']:
            assert flask_request.headers.get('Expect') == '200-ok'
        else:
            assert flask_request.headers.get('Expect') is None

        gevent.sleep(its_params['wait_before_response'])

        if flask_request.headers.get('Expect') != '200-ok':
            if its_params['etag'] == flask_request.headers.get('If-None-Match'):
                return '', 304

        response = jsonify(its_params['controls'])
        try:
            response.headers[b'ETag'] = its_params['etag'].encode('utf8')
            response.headers[b'Cache-Control'] = b'max-age={}'.format(its_params['max_age'])
        except Exception as e:
            print e

        return response

    web_cfg = {'web': {'http': {
        'host': 'localhost',
        'port': 0,
    }}}

    web_server = WebServer(web_cfg, app, version='test')
    web_thread = gevent.spawn(web_server.run)

    request.addfinalizer(web_server.stop)
    request.addfinalizer(web_thread.kill)

    return web_server


def _put_its_port_to_loop_conf(conf_file, port):

    with open(conf_file) as fd:
        data = fd.read()

    with open(conf_file, 'w') as fd:
        fd.write(re.sub(r'localhost:\d+', 'localhost:{}'.format(port), data))


def test_its_controls(cwd, ctl, patch_loop_conf, its_server, its_params, request, ctl_environment):
    """
    Проверяем поллинг ручек из ITS
    """
    its_port = its_server.wsgi.socket.getsockname()[1]
    _put_its_port_to_loop_conf(cwd.join('loop.conf').strpath, its_port)

    # возвращаемые сервером названия и значения ручек
    controls = {
        'first_key': ['first_value'],
        'second_key': ['second_value1', 'second_value2']
    }

    p = must_start_instancectl(ctl, request, ctl_environment)

    for step in xrange(3):
        if step > 0:
            its_params['expect_etag'] = str(step - 1)
        its_params['first_poll'] = step == 0
        its_params['etag'] = str(step)
        its_params['controls'] = {k: v[0] for k, v in controls.iteritems() if v}

        for key, values in controls.iteritems():
            if values:
                assert wait_condition_is_true(
                    lambda: cwd.join('controls', key).exists(), 9, 0.1
                )
                assert wait_condition_is_true(
                    lambda: str(values[0]) == cwd.join('controls', key).read().strip(), 9, 0.1
                )
                assert wait_condition_is_true(
                    lambda: str(values[0]) == cwd.join('{}.txt'.format(key)).read().strip(), 9, 0.1
                )
                assert wait_condition_is_true(
                    lambda: cwd.join('its_shared_storage', key).exists(), 9, 0.1
                )
                assert wait_condition_is_true(
                    lambda: str(values[0]) == cwd.join('its_shared_storage', key).read().strip(), 9, 0.1
                )
                values.pop(0)
            else:
                assert wait_condition_is_true(
                    lambda: not cwd.join('controls', key).check(), 9, 0.1
                )
                assert wait_condition_is_true(
                    lambda: not cwd.join('its_shared_storage', key).check(), 9, 0.1
                )

    must_stop_instancectl(ctl, process=p)

    # проверяем подгрузку кешированной версии ручек и отправку заголовка Expect: 200-ok
    p = must_start_instancectl(ctl, request, ctl_environment)

    for step in xrange(2):

        its_params['expect_etag'] = '2'
        its_params['first_poll'] = step == 0
        its_params['etag'] = '2'
        its_params['controls'] = {'first_key': 'value'}

        assert wait_condition_is_true(
            lambda: os.path.exists(os.path.join(cwd.strpath, 'controls/first_key')), 9, 0.1
        )
        assert wait_condition_is_true(
            lambda: 'value' == cwd.join('controls/first_key').read().strip(), 9, 0.1
        )
        assert wait_condition_is_true(
            lambda: 'value' == cwd.join('first_key.txt').read().strip(), 9, 0.1
        )

    must_stop_instancectl(ctl, process=p)


def test_its_timeout(cwd, ctl, patch_loop_conf, its_server, its_params, request, ctl_environment):
    # проверяем таймаут подключения к ITS
    its_params['wait_before_response'] = 1000

    p = must_start_instancectl(ctl, request, ctl_environment)

    # если таймаут обращения к ITS не сработает, то
    # проверять наличие run.flag луп-скрипт не станет и не завершится
    # пока ITS не ответит
    must_stop_instancectl(ctl, process=p)


# def test_shared_controls_reading(cwd, ctl, patch_loop_conf, request, ctl_environment):
#     local_controls_dir = cwd.join('controls')
#     local_controls_dir.mkdir().join('missed_control').write('missed_value')
#
#     shared_controls_dir = cwd.join('its_shared_storage')
#     shared_controls_dir.mkdir().join('shared_control').write('shared_value')
#
#     p = must_start_instancectl(ctl, request, ctl_environment)
#     gevent.sleep(3)
#     assert local_controls_dir.join('missed_control').exists()
#     assert not local_controls_dir.join('shared_control').exists()
#     must_stop_instancectl(ctl, process=p)
#
#     shared_controls_dir.join('.its_client', 'version').write('1', ensure=True)
#
#     p = must_start_instancectl(ctl, request, ctl_environment)
#     assert not local_controls_dir.join('missed_control').exists()
#     assert local_controls_dir.join('shared_control').read() == 'shared_value'
#     must_stop_instancectl(ctl, process=p)


def test_its_unicode_controls(cwd, ctl, patch_loop_conf, its_server, its_params, request, ctl_environment):
    its_params['controls'] = {'control': 'русское_значение'}
    its_params['etag'] = 'test'

    its_port = its_server.wsgi.socket.getsockname()[1]
    _put_its_port_to_loop_conf(str(cwd.join('loop.conf')), its_port)

    p = must_start_instancectl(ctl, request, ctl_environment)
    assert cwd.join('controls', 'control').read().decode('utf-8') == 'русское_значение'
    must_stop_instancectl(ctl, process=p)

