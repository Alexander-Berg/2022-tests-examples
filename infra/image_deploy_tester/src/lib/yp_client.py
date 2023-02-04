import json

import logging
import requests


class YpClient(object):
    DEFAULT_TIMEOUT = 60

    def __init__(self, cluster, base_url, oauth_token, timeout=None):
        self.oauth_token = oauth_token
        self.timeout = timeout or self.DEFAULT_TIMEOUT
        self.headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'OAuth %s' % (self.oauth_token,)
        }
        self.log = logging.getLogger('yp_client')
        self.cluster = cluster
        self.base_url = base_url

    def _api_call(self, url, data):
        result = requests.post(url, data=data, headers=self.headers, timeout=self.timeout)
        if result.status_code != 200:
            self.log.warning('yp request failed with code: %d', result.status_code)
            self.log.debug('response headers: %r', result.headers)

            if 'x-yt-response-message' in result.headers:
                raise Exception('YP Error: %s' % (result.headers['x-yt-response-message'],))
            else:
                raise Exception('YP request error (status code %d)' % (result.status_code,))
        return result

    def get_nodes_from_dev(self, node_filter):
        walle_project = 'yp-iss-{}-dev'.format(self.cluster.lower())
        query = '[/labels/extras/walle/project] = "{}"'.format(walle_project)
        if node_filter:
            query = '{} and {}'.format(query, node_filter)
        request = {
            'object_type': 'node',
            'filter': {
                'query': query
            },
            'selector': {
                'paths': ['/meta/id']
            },
            'format': 1
        }
        url = '{}/ObjectService/SelectObjects'.format(self.base_url)
        result = self._api_call(url, json.dumps(request))
        return [item['value_payloads'][0]['yson'] for item in result.json()['results']]
