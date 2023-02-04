import re
import json
import urllib.parse
import logging

from gevent.pywsgi import WSGIServer


class MockSandbox(object):
    def __init__(self, listen_address,
                 resource_types_to_releases,
                 resource_id_to_info,):
        self.resource_id_to_info = {
            rid: json.dumps(info).encode('latin1')
            for rid, info in resource_id_to_info.items()
        }
        self.resource_types_to_releases = {
            rtype: json.dumps({'items': [
                {'description': release['description'],
                 'id': release['resource_id']}
                for release in releases
            ]}).encode('latin1')
            for rtype, releases in resource_types_to_releases.items()
        }
        self.resource_types_to_descriptions = {
            rtype: json.dumps([{'description': '<skipped>',
                                'type': rtype}]).encode('latin1')
            for rtype, releases in resource_types_to_releases.items()
        }
        self.server = WSGIServer(listen_address, self._handle_request,
                                 log=logging.getLogger('mocksandbox'))

    def _handle_request(self, env, start_response):
        if env['REQUEST_METHOD'] == 'GET':
            qs = {k: v[-1]
                  for k, v in urllib.parse.parse_qs(env['QUERY_STRING']).items()
                  if v}
            m = re.match(r'\/resource\/(\d+)', env['PATH_INFO'])
            if m is not None:
                [resource_id] = m.groups()
                rinfo = self.resource_id_to_info.get(int(resource_id))
                if rinfo:
                    start_response('200 OK', [])
                    return [rinfo]
                assert False
                start_response('404 Not Found', [])
                return [b'Resource not found']
            elif env['PATH_INFO'] == '/suggest/resource' \
                    and 'type' in qs:
                    rtype = qs['type']
                    descr = self.resource_types_to_descriptions.get(rtype)
                    if descr:
                        start_response('200 OK', [])
                        return [descr]
                    assert False
                    start_response('404 Not Found', [])
                    return [b'Resource type not found']
            elif env['PATH_INFO'] == '/resource' \
                    and set(qs) == set(('type', 'limit', 'state',
                                        'attrs', 'order')) \
                    and qs['state'] == 'READY' \
                    and qs['attrs'] == '{"released": "stable"}' \
                    and qs['order'] == '-time.created':
                releases = self.resource_types_to_releases.get(qs['type'])
                if not releases:
                    assert False
                    start_response('404 Not Found', [])
                    return [b'Resource releases not found']
                start_response('200 OK', [])
                return [releases]
        assert False
        start_response('400 Bad Request', [])
        return [b'Bad Request']

    def start(self):
        self.server.start()

    def stop(self):
        self.server.stop()

    def serve_forever(self):
        self.server.serve_forever()


if __name__ == '__main__':
    print('Serving MockSandbox on 127.0.0.1:7081')
    resource = {
        "skynet_id": "rbtorrent:1478423bab725b38ee68397a9c1d20cee88bce2e",
        "rsync": {"links": ["rsync://sandbox-storage11.search.yandex.net/sandbox-tasks/6/9/39211596/dist/skynet.bin",
                            "rsync://sandbox-storage8.search.yandex.net/sandbox-tasks/6/9/39211596/dist/skynet.bin"]},
        "task": {"url": "https://sandbox.yandex-team.ru/api/v1.0/task/39211596",
                 "status": "RELEASED",
                 "id": 39211596},
        "http": {"proxy": "http://proxy.sandbox.yandex-team.ru/79690886",
                 "links": ["http://sandbox-storage11.search.yandex.net:13578/6/9/39211596/dist/skynet.bin",
                           "http://sandbox-storage8.search.yandex.net:13578/6/9/39211596/dist/skynet.bin"]},
        "description": "skynet.bin (14.5.20 (tc:1897))",
        "rights": "write",
        "url": "https://sandbox.yandex-team.ru/api/v1.0/resource/79690886",
        "type": "SKYNET_BINARY",
        "file_name": "dist/skynet.bin",
        "sources": ["sandbox-storage11", "sandbox-storage8"],
        "state": "READY",
        "time": {"accessed": "2015-09-01T17:36:07.768000Z",
                 "expires": None,
                 "created": "2015-09-01T14:03:38Z"},
        "owner": "SKYNET",
        "attributes": {"mds": "25154/79690886.tar.gz",
                       "released": "stable",
                       "version": "14.5.20",
                       "backup_task": 39214474,
                       "ttl": "inf"},
        "md5": "0387edc48ad1d3eef545da81fbba2c6d",
        "arch": "any",
        "id": 1,
        "size": 142797824
    }
    mock_sandbox = MockSandbox(
        listen_address=('127.0.0.1', 7081),
        resource_types_to_releases={'SKYNET_BINARY': [
            {'resource_id': 1, 'description': '<skipped>'}
        ]},
        resource_id_to_info={1: resource}
    )
    try:
        mock_sandbox.serve_forever()
    except KeyboardInterrupt:
        pass
