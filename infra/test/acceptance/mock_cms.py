import logging

from gevent.pywsgi import WSGIServer


class MockCMS(object):
    def __init__(self, listen_address, selectors_to_hosts):
        self.selectors_to_hosts = {
            selector: '\n'.join(host for host in hosts).encode('latin1')
            for selector, hosts in selectors_to_hosts.items()
        }
        self.server = WSGIServer(listen_address, self._handle_request,
                                 log=logging.getLogger('mockcms'))

    def _handle_request(self, env, start_response):
        if env['PATH_INFO'] != '/' or env['REQUEST_METHOD'] != 'POST':
            start_response('400 Bad Request', [])
            return [b'Bad Request']
        selector = env['wsgi.input'].read().decode('latin1')
        hosts = self.selectors_to_hosts.get(selector)
        if not hosts:
            start_response('404 Not Found', [])
            return [b'Selector not found']
        start_response('200 OK', [('Content-Type', 'text/plain')])
        return [hosts, b'\n']

    def start(self):
        self.server.start()

    def stop(self):
        self.server.stop()

    def serve_forever(self):
        self.server.serve_forever()


if __name__ == '__main__':
    print('Serving MockCMS on 127.0.0.1:7080')
    mock_cms = MockCMS(('127.0.0.1', 7080), {'': ['host1', 'host2']})
    try:
        mock_cms.serve_forever()
    except KeyboardInterrupt:
        pass
