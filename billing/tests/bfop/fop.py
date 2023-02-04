# -*- coding: utf-8 -*-
"""
    #############################################################################
    Balance FOP server python wrapper was moved to balance project.
    If you intend to edit files in the directory, please consider updating both
    #############################################################################
"""

import httplib as http
import os
import os.path as path
import re
import subprocess

import util
from errors import *


class Env(object):
    DEFAULT = 'default'
    LOCAL = 'local'
    LOCAL_DEV = 'local_dev'
    DEVELOPMENT = 'development'
    TESTING = 'testing'
    PRODUCTION = 'production'


LOGGER = util.get_logger('fop')
FOP_ENV = os.environ.get('FOP_ENV', None) or util.get_env()
FOP_COMMAND = path.realpath(os.environ.get('FOP_COMMAND', None) or '/usr/bin/fop')
FOP_RESOURCES = path.realpath(os.environ.get('FOP_RESOURCES', None) or '/usr/share/fop-resources')
FOP_CONF = '{}/conf/fop.xconf'.format(FOP_RESOURCES)
EXCEPTION_KEYWORDS = (
    'SEVERE',
    'WARNING: Glyph',
    'No such file',
)
RENDER_ENV_PARAMS = {
    Env.DEFAULT: {
        'name': FOP_ENV,
        'i18n-path': '{}/xsl/i18n/i18n.xml'.format(FOP_RESOURCES),
        # чтобы поменять работу в ветках, отредактировать
        # https://a.yandex-team.ru/arcadia/billing/balance/balance/publisher/bfop/fop.py?rev=r7915733#L33
        'images-host': 'https://user-balance.greed-tm.paysys.yandex.ru'
    },
    Env.PRODUCTION: {
        'images-host': 'https://balance.yandex.ru'
    },
    # локальная разработка, должно отличаться от LOCAL, т.к. c LOCAL запускаются тесты удаленно
    Env.LOCAL_DEV: {
        'name': FOP_ENV,
        'i18n-path': '{}/xsl/i18n/i18n.xml'.format(FOP_RESOURCES),
        'images-host': 'http://localhost:8899'
    },
}
SERVER_PARAMS = {
    Env.DEFAULT: {
        'scheme': 'http',
        'host': 'localhost',
        'port': 30101,
        'timeout': 10000,
        'verify': False,
    },
    Env.TESTING: {
        'scheme': 'https',
        'host': 'balance-fop-server-test.paysys.yandex.net',
        'port': 443
    },
    Env.PRODUCTION: {
        'scheme': 'https',
        'host': 'balance-fop-server.paysys.yandex.net',
        'port': 443
    }
}


# TODO[ashchek]: check "http://www.renderx.com/tools/fo2html.html"
class OutputFormat(object):
    PDF = 'pdf'
    RTF = 'rtf'
    FO_OUT = 'foout'


class XslPath(object):
    DEFAULT = '{}/xsl/main.xsl'.format(FOP_RESOURCES)
    RIT = '{}/xsl/rit/rit.xsl'.format(FOP_RESOURCES)


def render(xml_data, xsl_path=XslPath.DEFAULT, output_format=OutputFormat.PDF, conf=FOP_CONF, strict=False):
    LOGGER.info('============== FOP ==============')
    LOGGER.info('env:')
    LOGGER.info('FOP_ENV = {}'.format(FOP_ENV))
    LOGGER.info('FOP_COMMAND = {}'.format(FOP_COMMAND))
    LOGGER.info('FOP_CONF = {}'.format(FOP_CONF))
    LOGGER.info('arguments:')
    LOGGER.debug('xml_data = {}'.format(xml_data))
    LOGGER.info('xsl_path = {}'.format(xsl_path))
    LOGGER.info('output_format = {}'.format(output_format))
    LOGGER.info('conf = {}'.format(conf))
    LOGGER.info('strict = {}'.format(strict))
    LOGGER.info('=================================')

    LOGGER.info('start rendering...')

    assert xml_data
    assert xsl_path

    # TODO[ashchek]: use temp files (https://docs.python.org/2/library/tempfile.html) for passing xsl as string
    assert path.isfile(xsl_path)

    try:
        return _render_with_server(xml_data, xsl_path, output_format)
    except ServerFopException, e:
        LOGGER.warn('bad response from server: {}, rendering with subprocess'.format(e))
    except IOError, e:
        LOGGER.warn('server is not available due to "{}", so rendering with subprocess'.format(e))

    if FOP_ENV == Env.TESTING:
        raise FopException('rendering with subprocess is DEPRECATED in "testing" env')

    return _render_with_subprocess(xml_data, xsl_path, output_format, conf, strict)


def _render_with_subprocess(xml_data, xsl_path, output_format, conf, strict):
    assert conf

    assert path.isfile(FOP_COMMAND)
    assert path.isfile(conf)

    LOGGER.warning('rendering with subprocess is DEPRECATED and will be removed soon')

    args = [FOP_COMMAND, '-c', conf, '-xml', '-', '-xsl', xsl_path, '-{}'.format(output_format), '-']

    if not strict:
        args.append('-r')

    render_env_params = _get_params(RENDER_ENV_PARAMS, FOP_ENV, {
        'creator-tool': 'CLI'
    })

    for k, v in render_env_params.iteritems():
        args.extend(['-param', 'env-{}'.format(k), v])

    LOGGER.info('args: {}'.format(args))

    try:
        # See "https://docs.python.org/2/library/subprocess.html" for more information
        process = subprocess.Popen(args, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        data, error = process.communicate(input=xml_data)
    except Exception as e:
        raise SubprocessFopException(str(e))

    _check_render_with_subprocess_result(error)

    LOGGER.info('successfully rendered')

    return data


def _render_with_server(xml_data, xsl_path, output_format):
    import requests

    LOGGER.info('rendering with server (ignoring conf & strict args)')

    server_params = _get_params(SERVER_PARAMS, FOP_ENV)
    # Ugly reverse mapping
    entrypoint = {
        XslPath.DEFAULT: 'default',
        XslPath.RIT: 'rit',
    }[xsl_path]

    LOGGER.info('ready to send ~{} bytes to {}:{}'.format(
        len(xml_data),
        server_params['host'],
        server_params['port'])
    )

    url = '{}://{}:{}/render?entrypoint={}&outputformat={}'.format(
        server_params['scheme'],
        server_params['host'],
        server_params['port'],
        entrypoint,
        output_format.replace('foout', 'fo_out')
    )
    response = requests.post(
        url=url,
        data=xml_data,
        timeout=server_params['timeout'],
        verify=server_params['verify']
    )

    if response.status_code != http.OK:
        raise ServerFopException('call to fop server failed, status code: {}'.format(response.status_code))

    return response.content


def _get_params(params, env, extra=None):
    """
    :param dict params:
    :param str env:
    :param dict [extra]:
    :rtype: dict
    """
    result = {}

    result.update(params.get(Env.DEFAULT, {}))
    result.update(params.get(env, {}))

    if extra is not None:
        result.update(extra)

    return result


def _check_render_with_subprocess_result(error):
    """
    :param str error:
    """
    LOGGER.info('error: {}'.format(error))

    template_not_found_regex = 'TEMPLATE_NOT_FOUND:invoice_id=(\d+)'
    template_not_found_match = re.search(template_not_found_regex, error)

    if template_not_found_match:
        raise TemplateNotFoundFopException('Template for invoice with id {} not found'.format(
            template_not_found_match.group(1)
        ))

    if any(keyword in error for keyword in EXCEPTION_KEYWORDS):
        raise RenderFopException(error)


if __name__ == '__main__':
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument('xml_path')
    parser.add_argument('format', default=OutputFormat.PDF, nargs='?')
    parsed_args = parser.parse_args()

    print render(
        open(parsed_args.xml_path, 'rb').read(),
        XslPath.DEFAULT,
        conf='tests/conf/fop.xconf',
        output_format=parsed_args.format
    )
