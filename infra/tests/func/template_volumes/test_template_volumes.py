from __future__ import unicode_literals

import json
import os
import socket
import subprocess
import time
import grpc

from ConfigParser import SafeConfigParser

import pytest
import gevent
from yatest.common import network
from flask import Flask, request as flask_request
from sepelib.flask.server import WebServer
from sepelib.subprocess import util
from yp_proto.yp.client.hq.proto import hq_pb2, federated_pb2, types_pb2

from infra.yp_service_discovery.api import api_pb2
from infra.yp_service_discovery.python.resolver import resolver

from utils import must_start_sd_server, must_start_instancectl, must_stop_instancectl
from instancectl import common


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


TEMPLATE_VOLUME_NAME = 'volume_name'
TEMPLATE = {
    'src_path': 'template',
    'dst_path': 'rendered_template'
}

ABS_TEMPLATE_VOLUME_NAME = 'abs_volume_name'
ABS_TEMPLATE = {
    'src_path': 'template',
    'dst_path': 'abs_rendered_template'
}

NET_TEMPLATE_VOLUME_NAME = 'net_volume_name'
NET_TEMPLATE = {
    'src_path': 'net_template',
    'dst_path': 'net_rendered_template'
}

SD_TEMPLATE_VOLUME_NAME = 'sd_volume_name'
SD_TEMPLATE = {
    'src_path': 'sd_template',
    'dst_path': 'sd_rendered_template'
}

@pytest.fixture
def hq(request, ctl, cwd):

    with ctl.dirpath('dump.json').open() as fd:
        dump_json = json.load(fd)

    port = dump_json['properties']['BSCONFIG_IPORT']
    service, conf = dump_json['configurationId'].rsplit('#')
    hostname = socket.getfqdn()
    expected_instance_id = '{}:{}@{}'.format(hostname, port, service)

    app = Flask('instancectl-test-fake-hq')
    app.processed_requests = []

    web_cfg = {'web': {'http': {
        'host': '::1',
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

    parser.set('defaults', 'federated_url', 'http://[::1]:{}/'.format(port))

    with open(conf_file, 'w') as fd:
        parser.write(fd)

    @app.route('/rpc/instances/GetInstanceRev/', methods=['POST'])
    def get_instance():
        try:
            req = hq_pb2.GetInstanceRevRequest()
            req.ParseFromString(flask_request.data)
            assert req.id.split(':')[1] == expected_instance_id.split(':')[1]
            assert req.rev == conf
            resp = hq_pb2.GetInstanceRevResponse()
            resp.revision.id = conf

            # Template Volume
            v = resp.revision.volume.add()
            v.name = TEMPLATE_VOLUME_NAME
            v.type = types_pb2.Volume.TEMPLATE
            t = v.template_volume.template.add()
            t.src_path = TEMPLATE['src_path']
            t.dst_path = TEMPLATE['dst_path']

            # Template volume which make localhost resolving to ip addresses
            v = resp.revision.volume.add()
            v.name = NET_TEMPLATE_VOLUME_NAME
            v.type = types_pb2.Volume.TEMPLATE
            t = v.template_volume.template.add()
            t.src_path = NET_TEMPLATE['src_path']
            t.dst_path = NET_TEMPLATE['dst_path']

            # Template Volume with absolute path
            v = resp.revision.volume.add()
            v.name = ABS_TEMPLATE_VOLUME_NAME
            v.type = types_pb2.Volume.TEMPLATE
            t = v.template_volume.template.add()
            t.src_path = '{}/{}'.format(cwd, ABS_TEMPLATE['src_path'])
            t.dst_path = ABS_TEMPLATE['dst_path']

            # Template Volume with sd resolve instances
            v = resp.revision.volume.add()
            v.name = SD_TEMPLATE_VOLUME_NAME
            v.type = types_pb2.Volume.TEMPLATE
            t = v.template_volume.template.add()
            t.src_path = '{}/{}'.format(cwd, SD_TEMPLATE['src_path'])
            t.dst_path = SD_TEMPLATE['dst_path']

            return resp.SerializeToString(), 200
        except Exception:
            import traceback
            traceback.print_exc()

    @app.route('/sas/rpc/instances/FindInstances/', methods=['POST'])
    def find_instances_sas():
        try:
            resp = hq_pb2.FindInstancesResponse()
            i = resp.instance.add()
            i.spec.node_name = 'sas1-1112.search.yandex.net'
            i.spec.hostname = 'hostname'
            p = i.spec.allocation.port.add()
            p.port = 8082
            r = i.status.revision.add()
            r.id = 'parallel_rlsfacts_iss_test-999999'
            r = i.spec.revision.add()
            r.id = 'parallel_rlsfacts_iss_test-999999'

            i = resp.instance.add()
            i.spec.node_name = 'sas1-1111.search.yandex.net'
            p = i.spec.allocation.port.add()
            p.port = 8082
            r = i.status.revision.add()
            r.id = 'parallel_rlsfacts_iss_test-123457'
            r = i.spec.revision.add()
            r.id = 'parallel_rlsfacts_iss_test-123457'
            i.status.ready.status = 'True'

            i = resp.instance.add()
            i.spec.node_name = 'sas1-1113.search.yandex.net'
            p = i.spec.allocation.port.add()
            p.port = 8082
            r = i.status.revision.add()
            r.id = 'parallel_rlsfacts_iss_test-123457'
            r = i.spec.revision.add()
            r.id = 'parallel_rlsfacts_iss_test-123457'
            r = i.status.revision.add()
            r.id = 'parallel_rlsfacts_iss_test-123456'
            r = i.spec.revision.add()
            r.id = 'parallel_rlsfacts_iss_test-123456'
            return resp.SerializeToString(), 200
        except Exception:
            import traceback
            traceback.print_exc()

    @app.route('/man/rpc/instances/FindInstances/', methods=['POST'])
    def find_instances_man():
        try:
            resp = hq_pb2.FindInstancesResponse()
            i = resp.instance.add()
            i.spec.node_name = 'man1-1111.search.yandex.net'
            p = i.spec.allocation.port.add()
            p.port = 8082
            r = i.status.revision.add()
            r.id = 'parallel_rlsfacts_iss_test-999999'
            r = i.spec.revision.add()
            r.id = 'parallel_rlsfacts_iss_test-999999'
            return resp.SerializeToString(), 200
        except Exception:
            import traceback
            traceback.print_exc()

    @app.route('/rpc/federated/FindClusters/', methods=['POST'])
    def find_clusters():
        try:
            resp = federated_pb2.FindClustersResponse()
            cluster = resp.value.add()
            cluster.meta.name = 'sas_prod'
            cluster.spec.endpoint.url = 'http://[::1]:{}/sas'.format(port)

            cluster = resp.value.add()
            cluster.meta.name = 'man_prod'
            cluster.spec.endpoint.url = 'http://[::1]:{}/man'.format(port)

            return resp.SerializeToString(), 200
        except Exception:
            import traceback
            traceback.print_exc()

    return web_server


def _get_port(web_server):
    return web_server.wsgi.socket.getsockname()[1]


def test_template_processing_good(ctl, sd_bin, request, hq):
    port = _get_port(hq)
    with network.PortManager() as pm:
        sd_bind_addr = "127.0.0.1:{}".format(pm.get_port())
        ctl_environment = get_env()
        ctl_environment['SD_URL'] = sd_bind_addr

        endpoints_responses = {
            'sas': {
                'endpoint_set': {
                    'endpoint_set_id': 'any-endpoint-set-id',
                    'endpoints': [
                            {'id': 'sas-1', 'protocol': 'TCP', 'fqdn': 'fqdn', 'ip6_address': 'ip6_address',
                             'port': 80, 'ready': True}
                    ]
                }
            },
            'man': {
                'endpoint_set': {
                    'endpoint_set_id': 'any-endpoint-set-id',
                    'endpoints': [
                            {'id': 'man-1', 'protocol': 'TCP', 'fqdn': 'fqdn', 'ip6_address': 'ip6_address',
                             'port': 80, 'ready': False}
                    ]
                }
            },
        }
        pods_responses = {
            'sas': {
                'pod_set': {
                    'pod_set_id': 'any-pod-set-id',
                    'pods': [
                        {'id': 'sas-1', 'node_id': 'sas-node-1',
                         'dns': {'persistent_fqdn': 'sas-pers-fqdn', 'transient_fqdn': 'sas-trans-fqdn'}
                         }
                    ]
                }
            },
            'man': {
                'pod_set': {
                    'pod_set_id': 'any-pod-set-id',
                    'pods': [
                        {'id': 'man-1', 'node_id': 'man-node-1', 'dns': {
                            'persistent_fqdn': 'man-pers-fqdn',
                            'transient_fqdn': 'man-trans-fqdn',
                        }}
                    ]
                }
            },
        }
        must_start_sd_server(sd_bin, request, sd_bind_addr,
                             endpoints_responses=endpoints_responses,
                             pods_responses=pods_responses)
        must_start_instancectl(ctl, request, ctl_environment=ctl_environment,
                               add_args=['--hq-url', 'http://[::1]:{}/'.format(port)])
        rendered_path = os.path.join(TEMPLATE_VOLUME_NAME, TEMPLATE['dst_path'])
        assert ctl.dirpath(rendered_path).read() == ctl.dirpath('correct_template').read()
        net_rendered_path = os.path.join(NET_TEMPLATE_VOLUME_NAME, NET_TEMPLATE['dst_path'])
        lines = ctl.dirpath(net_rendered_path).readlines()
        assert len(lines) == 2
        assert '127.0.0.1' in lines[0].strip().split(',')
        assert '::1' in lines[1].strip().split(',')
        abs_rendered_path = os.path.join(ABS_TEMPLATE_VOLUME_NAME, ABS_TEMPLATE['dst_path'])
        assert ctl.dirpath(abs_rendered_path).read() == ctl.dirpath('correct_template').read()

        sd_rendered_path = os.path.join(SD_TEMPLATE_VOLUME_NAME, SD_TEMPLATE['dst_path'])
        assert ctl.dirpath(sd_rendered_path).read().strip() == ctl.dirpath('correct_sd_template').read().strip()

        must_stop_instancectl(ctl, check_loop_err=False)


def test_template_processing_fail(ctl, request, hq):
    # Test that instancectl will fail if there is no source template given
    ctl.dirpath(TEMPLATE['src_path']).remove()
    port = _get_port(hq)
    p = subprocess.Popen([ctl.strpath, 'start', '--hq-url', 'http://localhost:{}/'.format(port)],
                         cwd=ctl.dirname, env=get_env())
    request.addfinalizer(lambda: util.terminate(p))
    s = time.time()
    while p.poll() is None and time.time() - s < 20.0:
        gevent.sleep(0.1)
    assert p.poll() == common.INSTANCE_CTL_CANNOT_INIT


def test_sd_bin_server(sd_bin, request):
    with network.PortManager() as pm:
        bind_addr = "127.0.0.1:{}".format(pm.get_port())
        endpoints_responses = {
            "man": {"resolve_status": "NOT_CHANGED", "anyString": "string-passed-to-args" * 400}
        }
        pods_responses = {
            "man": {"resolve_status": "NOT_CHANGED", "anyString": "string-passed-to-args" * 400}
        }

        must_start_sd_server(sd_bin, request, bind_addr,
                             endpoints_responses=endpoints_responses,
                             pods_responses=pods_responses)

        client = resolver.Resolver(client_name='testing', grpc_address=bind_addr)

        req = api_pb2.TReqResolveEndpoints(cluster_name='man')
        resp = client.resolve_endpoints(req)
        assert resp.resolve_status == api_pb2.EResolveStatus.NOT_CHANGED
        req = api_pb2.TReqResolveEndpoints(cluster_name='sas')
        resp = client.resolve_endpoints(req)
        assert resp.resolve_status == api_pb2.EResolveStatus.NOT_EXISTS

        req = api_pb2.TReqResolvePods(cluster_name='man')
        resp = client.resolve_endpoints(req)
        assert resp.resolve_status == api_pb2.EResolveStatus.NOT_CHANGED
        req = api_pb2.TReqResolvePods(cluster_name='sas')
        resp = client.resolve_endpoints(req)
        assert resp.resolve_status == api_pb2.EResolveStatus.NOT_EXISTS
