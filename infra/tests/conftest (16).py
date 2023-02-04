# -*- coding: utf-8 -*-
import gevent
import gevent.monkey

gevent.monkey.patch_all()

import json
import httplib

import pytest
from flask import Flask, request as flask_request, jsonify, Response
from sepelib.flask.server import WebServer
from sepelib.util.log import setup_logging_to_stdout


ITAGS = ['a_tag1', 'a_tag2', 'a_tag3']


setup_logging_to_stdout()


@pytest.fixture
def its_params():
    return {
        'expect_200_ok': True,
        'controls': {},
        'wait_before_response': 0,
        'max_age': 0.5,
        'statistics_period': 0,
        'config_version': 0,
    }


@pytest.fixture
def its_server(request, its_params):

    app = Flask('its-client-fake-server')

    @app.route('/v1/process/', methods=['POST'])
    def main():
        req = flask_request.json
        assert req == ITAGS or ('i' in req and req['i'] == ITAGS)
        assert flask_request.headers.get('If-None-Match') == its_params.get('etag')

        gevent.sleep(its_params['wait_before_response'])

        if its_params['expect_200_ok']:
            assert flask_request.headers.get('Expect') == '200-ok'
            its_params['expect_200_ok'] = False
        else:
            assert flask_request.headers.get('Expect') is None

        its_params['etag'] = '{}:{}:{}'.format(its_params['statistics_period'],
                                               json.dumps(its_params['controls'], sort_keys=True),
                                               its_params['config_version'])

        response = jsonify(its_params['controls'])

        if flask_request.headers.get('Expect') != '200-ok':
            if flask_request.headers.get('If-None-Match') == its_params['etag']:
                response = Response(status=httplib.NOT_MODIFIED)

        response.headers['ETag'] = its_params['etag']
        response.headers['Cache-Control'] = 'max-age={0}'.format(its_params['max_age'])

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


