# -*- coding: utf-8 -*-
import os
import uuid

from butils import logger as logger

log = logger.get_logger('test-snout-app')


def _fix_cors(request, response):
    """
    See https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS for more details.
    :param flask.wrappers.Request request:
    :param flask.wrappers.Response response:
    """
    referrer = request.referrer

    if referrer:
        origin = '/'.join(referrer.split('/')[:3])

        log.debug('Origin: %s', origin)

        response.headers['Access-Control-Allow-Origin'] = origin
        response.headers['Access-Control-Allow-Credentials'] = 'true'

        log.warn('"Allow origin" header is now unsafe')


def patch_proxy_start(uid):
    from brest.core.auth.direct import DirectAuthMethod
    from yb_snout_proxy.servant import flask_app as app

    app.auth.methods = [DirectAuthMethod(uid)]
    log.debug('New auth methods: %s', app.auth.methods)

    @app.after_request
    def set_headers(response):
        response.headers['X-Request-Id'] = uuid.uuid4().hex

        return response


def patch_api_start(uid):
    from balance.binary_main import patch_cdecimal_unpickling

    patch_cdecimal_unpickling()

    from brest.core.auth.direct import DirectAuthMethod
    from yb_snout_api.servant import flask_app as app

    app.auth.methods = [DirectAuthMethod(uid)]
    log.debug('New auth methods: %s', app.auth.methods)

    @app.after_request
    def make_unsafe_allow_origin(response):
        import flask

        _fix_cors(flask.request, response)

        return response

    @app.route('/favicon.ico')
    def get_favicon():
        import http.client as http

        return '', http.NOT_FOUND


def start_proxy():
    from yb_snout_proxy.uwsgi_run import SnoutProxyRunner
    uid = os.environ['PASSPORT_UID']
    helper = SnoutProxyRunner()
    patch_proxy_start(uid)
    helper.run()


def start_api():
    from yb_snout_api.uwsgi_run import SnoutApiRunner
    uid = os.environ['PASSPORT_UID']
    helper = SnoutApiRunner()
    patch_api_start(uid)
    helper.run()
