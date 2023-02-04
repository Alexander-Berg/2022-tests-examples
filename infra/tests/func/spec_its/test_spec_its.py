from __future__ import print_function
from __future__ import unicode_literals

import gevent
from flask import Flask
from flask import jsonify
from flask import request as flask_request
from yp_proto.yp.client.hq.proto import hq_pb2
from yp_proto.yp.client.hq.proto import types_pb2
from sepelib.flask import server

import utils


def test_spec_stop_handler(ctl, request):
    env = utils.get_spec_env()
    app = Flask('instancectl-test-fake-its')
    reqs = []

    @app.route('/v1/process/', methods=['POST'])
    def main():
        try:
            reqs.append(flask_request.json)
            return jsonify({'key': 'value'})
        except Exception:
            import traceback
            traceback.print_exc()

    web_cfg = {'web': {'http': {
        'host': 'localhost',
        'port': 0,
    }}}

    web_server = server.WebServer(web_cfg, app, version='test')
    web_thread = gevent.spawn(web_server.run)
    request.addfinalizer(web_server.stop)
    request.addfinalizer(web_thread.kill)

    resp = hq_pb2.GetInstanceRevResponse()
    c = resp.revision.container.add()
    c.name = 'test_spec_its'
    c.readiness_probe.initial_delay_seconds = 3
    c.command.extend(['/bin/sleep', '100'])
    v = resp.revision.volume.add()
    v.type = types_pb2.Volume.ITS
    v.its_volume.its_url = 'http://localhost:{}/v1'.format(web_server.wsgi.socket.getsockname()[1])
    v.its_volume.period_seconds = 1

    hq = utils.start_hq_mock(ctl, env, request, resp)
    utils.must_start_instancectl(ctl, request, ctl_environment=env, add_args=utils.make_hq_args(hq))

    gevent.sleep(10)

    assert len(reqs) > 0
    assert reqs[-1] == {
        'f': ['fake_instancectl_service'],
        'i': ['a_ctype_isstest', 'a_dc_sas', 'a_geo_sas', 'a_itype_fake_type']
    }
    assert ctl.dirpath('controls', 'key').read() == 'value'

    utils.must_stop_instancectl(ctl, check_loop_err=False)
    hq.stop()
