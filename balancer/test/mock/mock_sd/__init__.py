import json
import socket
import subprocess
import time

import yatest.common


class MockSD:
    def __init__(self, port, backends):
        backends_json = []
        for k, v in backends.iteritems():
            backends_json.append({
                'cluster_name': k[0],
                'endpoint_set_id': k[1],
                'backends': v,
            })
        sd_mock_config_path = yatest.common.test_output_path('sd_mock_config.json')
        json.dump(backends_json, open(sd_mock_config_path, 'w'))

        self.p = subprocess.Popen([yatest.common.binary_path('balancer/production/x/sd_mock/sd_mock'), str(port), sd_mock_config_path])

        while True:
            try:
                sock = socket.create_connection(('127.0.0.1', port), timeout=1)
                sock.close()
            except:
                pass
            else:
                break
            time.sleep(0.1)

    def stop(self):
        self.p.terminate()
        self.p.wait()
