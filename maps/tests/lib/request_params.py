import base64
import hashlib
import hmac


class RequestParams(object):
    HEAD_ID = 'ABCDEF123456'
    DEVICE_ID = '0123456789abcdef0123456789abcdef'
    UUID = '0123456789abcdef0123456789abcdef'
    TASK_ID = '42'

    AUTOMOTIVE_HTTP_SIGNATURE_KEY = 'AAECAwQFBgcICQoLDA0ODw=='

    def __init__(self, request_method, uri, body=''):
        host = 'auto-proxy.maps.yandex.net'
        user_agent = 'Mozilla/5.0'
        request_timestamp = '1585671515'

        request_uri = "http://{host}{uri}".format(
            host=host,
            uri=uri,
        )
        data = "{user_agent}{request_method} {request_uri}{request_timestamp}{body}".format(
            user_agent=user_agent,
            request_method=request_method,
            request_uri=request_uri,
            request_timestamp=request_timestamp,
            body=body,
        ).encode('utf-8')

        self.signature = base64.b64encode(hmac.new(
            key=base64.b64decode(self.AUTOMOTIVE_HTTP_SIGNATURE_KEY),
            msg=data,
            digestmod=hashlib.sha1
        ).digest()).rstrip(b'\n')

        self.uri = uri
        self.headers = {
            'Host': host,
            'User-Agent': user_agent,
            'X-YRuntime-Signature': self.signature,
            'X-YRuntime-Timestamp': request_timestamp,
        }
        self.body = body

    def invalidate_signature(self):
        self.headers['X-YRuntime-Signature'] = 'ZmZmZmZmZmZmZmZmZmZmZmZmZmY='

    def pop_timestamp(self):
        self.headers.pop('X-YRuntime-Timestamp')
