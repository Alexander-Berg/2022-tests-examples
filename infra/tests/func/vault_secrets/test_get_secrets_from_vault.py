from __future__ import unicode_literals

import json
import base64
import subprocess
from ConfigParser import SafeConfigParser

import pytest
import gevent
from flask import Flask, request as flask_request
from sepelib.flask.server import WebServer
from yp_proto.yp.client.hq.proto import hq_pb2, types_pb2

from instancectl import common
from utils import must_start_instancectl, must_stop_instancectl


def get_env():
    return {
        "BSCONFIG_INAME": "sas1-1956.search.yandex.net:17319",
        "BSCONFIG_IHOST": "sas1-1956.search.yandex.net",
        "BSCONFIG_IPORT": "17319",
        "BSCONFIG_SHARDDIR": "rlsfacts-000-1393251816",
        "BSCONFIG_SHARDNAME": "rlsfacts-000-1393251816",
        "BSCONFIG_ITAGS": "newstyle_upload a_dc_sas a_geo_sas parallel_autofacts a_ctype_isstest enable_hq_poll",
        "tags": "newstyle_upload a_dc_sas a_geo_sas parallel_autofacts a_ctype_isstest enable_hq_poll",
        "annotated_ports": "{\"main\": 8080, \"extra\": 8081}",
        "NANNY_SERVICE_ID": "parallel_rlsfacts_iss_test"
    }


FILE_FIELD = 'file-field'
ENV_FIELD = 'fake-secret-field'
SECRET = {
    'fake-secret-field': 'SECRET_CONTENT',
    'other-field': 'OTHER_CONTENT'
}
NEW_ENV_VALUE = 'new-secret-value'
ONLY_ONE_KEY_VALUE = 'one-key-secret-value'
BASE64_ENCODED_VALUE = ('AAECAwQFBgcICQoLDA0ODxAREh'
                        'MUFRYXGBkaGxwdHh8gISIjJCUm'
                        'JygpKissLS4vMDEyMzQ1Njc4OT'
                        'o7PD0+P0BBQkNERUZHSElKS0xN'
                        'Tk9QUVJTVFVWV1hZWltcXV5fYG'
                        'FiY2RlZmdoaWprbG1ub3BxcnN0'
                        'dXZ3eHl6e3x9fn+AgYKDhIWGh4'
                        'iJiouMjY6PkJGSk5SVlpeYmZqb'
                        'nJ2en6ChoqOkpaanqKmqq6ytrq'
                        '+wsbKztLW2t7i5uru8vb6/wMHC'
                        'w8TFxsfIycrLzM3Oz9DR0tPU1d'
                        'bX2Nna29zd3t/g4eLj5OXm5+jp'
                        '6uvs7e7v8PHy8/T19vf4+fr7/P'
                        '3+')
NEW_SECRET = {
    "content": {
        "entries": [
            {
                "content": {
                    "key": FILE_FIELD,
                    "value": BASE64_ENCODED_VALUE
                },
                "meta": {
                    "format": "BASE64"
                }
            },
            {
                "content": {
                    "key": ENV_FIELD,
                    "value": base64.b64encode(NEW_ENV_VALUE)
                },
                "meta": {
                    "format": "BASE64"
                }
            }
        ]
    }
}

ONLY_ONE_KEY_SECRET = {
    "content": {
        "entries": [
            {
                "content": {
                    "key": ENV_FIELD,
                    "value": base64.b64encode(ONLY_ONE_KEY_VALUE)
                },
                "meta": {
                    "format": "BASE64"
                }
            }
        ]
    }
}


@pytest.fixture
def hq_and_vault(request, ctl):

    keychain_id = 'fake-keychain-id'
    secret_id = 'fake-secret-id'
    rev_id = 'fake-rev-id'
    new_rev_id = 'new-rev-id'
    only_one_key_rev_id = 'only-one-key-rev-id'
    full_secret_id = '{}/{}/{}'.format(keychain_id, secret_id, rev_id)
    new_secret_id = '{}/{}/{}'.format(keychain_id, secret_id, new_rev_id)
    only_one_secret_id = '{}/{}/{}'.format(keychain_id, secret_id, only_one_key_rev_id)
    token = 'fake-token'

    with ctl.dirpath('dump.json').open() as fd:
        dump_json = json.load(fd)

    service, conf = dump_json['configurationId'].rsplit('#')

    app = Flask('instancectl-test-fake-its')
    app.processed_requests = []

    @app.route('/rpc/instances/GetInstanceRev/', methods=['POST'])
    def get_instance():
        try:
            req = hq_pb2.GetInstanceRevRequest()
            req.ParseFromString(flask_request.data)
            assert req.rev == conf
            resp = hq_pb2.GetInstanceRevResponse()
            resp.revision.id = conf
            c = resp.revision.container.add()
            c.name = 'test_vault'

            # Legacy secret without schema
            e = c.env.add()
            e.name = 'FAKE_SECRET'
            e.value_from.type = types_pb2.EnvVarSource.SECRET_ENV
            e.value_from.secret_env.keychain_secret.keychain_id = keychain_id
            e.value_from.secret_env.keychain_secret.secret_id = secret_id
            e.value_from.secret_env.keychain_secret.secret_revision_id = rev_id
            e.value_from.secret_env.field = ENV_FIELD

            # New secret schema
            e = c.env.add()
            e.name = 'FAKE_SECRET_WITH_SCHEMA'
            e.value_from.type = types_pb2.EnvVarSource.SECRET_ENV
            e.value_from.secret_env.keychain_secret.keychain_id = keychain_id
            e.value_from.secret_env.keychain_secret.secret_id = secret_id
            e.value_from.secret_env.keychain_secret.secret_revision_id = new_rev_id
            e.value_from.secret_env.field = ENV_FIELD

            # Secret containing only one key-value pair should be retrieved even if no field given
            e = c.env.add()
            e.name = 'ONLY_ONE_KEY_SECRET'
            e.value_from.type = types_pb2.EnvVarSource.SECRET_ENV
            e.value_from.secret_env.keychain_secret.keychain_id = keychain_id
            e.value_from.secret_env.keychain_secret.secret_id = secret_id
            e.value_from.secret_env.keychain_secret.secret_revision_id = only_one_key_rev_id
            e.value_from.secret_env.field = ENV_FIELD

            # Legacy secret without schema
            v = resp.revision.volume.add()
            v.type = types_pb2.Volume.SECRET
            v.name = 'mount_path'
            v.secret_volume.keychain_secret.keychain_id = keychain_id
            v.secret_volume.keychain_secret.secret_id = secret_id
            v.secret_volume.keychain_secret.secret_revision_id = rev_id

            # New secret schema
            v = resp.revision.volume.add()
            v.type = types_pb2.Volume.SECRET
            v.name = 'new_mount_path'
            v.secret_volume.keychain_secret.keychain_id = keychain_id
            v.secret_volume.keychain_secret.secret_id = secret_id
            v.secret_volume.keychain_secret.secret_revision_id = new_rev_id
            return resp.SerializeToString(), 200
        except Exception:
            import traceback
            traceback.print_exc()

    @app.route('/v1/auth/blackbox/service/login/{}'.format(service), methods=['POST'])
    def auth():
        try:
            data = flask_request.get_json()
            assert data['keychain'] == keychain_id
            return json.dumps({
                'auth': {'client_token': token}
            }), 200
        except BaseException:
            import traceback
            traceback.print_exc()

    @app.route('/v1/secret/{}'.format(full_secret_id), methods=['GET'])
    def secret():
        assert flask_request.headers.get('X-Vault-Token') == token
        return json.dumps({'data': SECRET}), 200

    @app.route('/v1/secret/{}'.format(new_secret_id), methods=['GET'])
    def secret_with_schema():
        assert flask_request.headers.get('X-Vault-Token') == token
        return json.dumps({'data': NEW_SECRET}), 200

    @app.route('/v1/secret/{}'.format(only_one_secret_id), methods=['GET'])
    def secret_with_only_key():
        assert flask_request.headers.get('X-Vault-Token') == token
        return json.dumps({'data': ONLY_ONE_KEY_SECRET}), 200

    web_cfg = {'web': {'http': {
        'host': 'localhost',
        'port': 0,
    }}}

    web_server = WebServer(web_cfg, app, version='test')
    web_thread = gevent.spawn(web_server.run)

    request.addfinalizer(web_server.stop)
    request.addfinalizer(web_thread.kill)

    conf_file = ctl.dirpath('loop.conf').strpath
    parser = SafeConfigParser()
    parser.read(conf_file)
    port = web_server.wsgi.socket.getsockname()[1]

    parser.set('defaults', 'hq_default_url_sas', 'http://localhost:{}/'.format(port))
    parser.set('defaults', 'vault_url', 'http://localhost:{}/'.format(port))

    with open(conf_file, 'w') as fd:
        parser.write(fd)

    return web_server


def _get_port(web_server):
    return web_server.wsgi.socket.getsockname()[1]


def test_get_secrets_from_vault(ctl, request, hq_and_vault):
    port = _get_port(hq_and_vault)
    must_start_instancectl(ctl, request, ctl_environment=get_env(), console_logging=True,
                           add_args=['--hq-url', 'http://localhost:{}/'.format(port)])
    gevent.sleep(10)

    content = SECRET[ENV_FIELD]

    # Check env secrets
    assert ctl.dirpath('secret.txt').read().strip() == content
    assert ctl.dirpath('secret_with_schema.txt').read().strip() == NEW_ENV_VALUE
    assert ctl.dirpath('secret_from_install.txt').read().strip() == content
    assert ctl.dirpath('secret_with_only_key.txt').read().strip() == ONLY_ONE_KEY_VALUE

    # Check secrets from volumes
    assert ctl.dirpath('mount_path').stat().mode % 0o1000 == 0o700
    for f in ctl.dirpath('mount_path').listdir():
        assert f.stat().mode % 0o1000 == 0o600
        assert f.read().strip() == SECRET[f.basename]

    d = {e['content']['key']: e['content']['value'] for e in NEW_SECRET['content']['entries']}
    assert ctl.dirpath('new_mount_path').stat().mode % 0o1000 == 0o700
    for f in ctl.dirpath('new_mount_path').listdir():
        assert f.stat().mode % 0o1000 == 0o600
        assert f.read().strip() == base64.b64decode(d[f.basename])

    must_stop_instancectl(ctl, check_loop_err=False)

    content = ctl.dirpath('loop.conf').read()
    content = content.replace('[test_vault]', '[some_fake_section]')
    ctl.dirpath('loop.conf').write(content)
    ctl.dirpath('state', 'instance.conf').remove()
    p = subprocess.Popen([ctl.strpath, 'start', '--hq-url', 'http://localhost:{}/'.format(port)],
                         cwd=ctl.dirname, env=get_env())
    gevent.sleep(10)
    assert p.poll() == common.INSTANCE_CTL_CANNOT_INIT
