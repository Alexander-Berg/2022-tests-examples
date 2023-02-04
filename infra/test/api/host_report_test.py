import json
import msgpack

from .base import ApiWebTestCase


class HostReportTestCase(ApiWebTestCase):
    def test(self):
        data = [
            {'host': 'h1', 'timestamp': 123555, 'reports': {
                'k1': 'v1.1', 'k2': 'v2.1'
             }},
            {'host': 'h2', 'timestamp': 123111, 'reports': {
                'k1': 'v1.2', 'k2': 'v2.1',
             }},
        ]
        data = msgpack.dumps(data, encoding='utf8')
        response = self.client.post('/v1/hostreport', data=data)
        self.assertEquals(response.status_code, 200)
        self.assertEquals(response.content_type, 'application/json')
        self.assertEquals(json.loads(response.data.decode('ascii')),
                          {'result': 'ok'})

        self.assertEquals(list(self.redis.zscan_iter('expires')), [
            ('h2\x00k1', 123111.0),
            ('h2\x00k2', 123111.0),
            ('h1\x00k1', 123555.0),
            ('h1\x00k2', 123555.0)
        ])

        self.assertEquals(self.redis.get('hk2v\0h1\0k1'), 'v1.1')
        self.assertEquals(self.redis.get('hk2v\0h1\0k2'), 'v2.1')
        self.assertEquals(self.redis.get('hk2v\0h2\0k1'), 'v1.2')
        self.assertEquals(self.redis.get('hk2v\0h2\0k2'), 'v2.1')
