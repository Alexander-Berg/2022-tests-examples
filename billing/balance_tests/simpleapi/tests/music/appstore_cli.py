"""HTTP Parking Payment Proxy Client
"""
import json

from simpleapi.common import logger
from simpleapi.tests.music.http_util import http_post, split_url

log = logger.get_logger()


class AppStoreClient(object):
    '''A HTTP client for app store emulator.
    '''

    def __init__(self, url):
        self.url = url
        self.url_parts = split_url(self.url)

    def _call_method(self, method,
                     request_method='POST',
                     params=None,
                     headers=None,
                     payload=None,
                     protobuf=False):
        try:
            (code, desc, body) = http_post(self.url_parts,
                                           params=params or [],
                                           method=request_method,
                                           path_add=method,
                                           user_headers=headers or [],
                                           body=payload)
        except Exception, exc:
            log.exception('')
            return {'method': 'AppStoreProxyCli.%s' % method,
                    'status': 'error',
                    'status_desc': '%s(%s)' % (
                        exc.__class__.__name__, str(exc))}
        if code != 200:
            return {'method': 'AppStoreProxyCli.%s' % method,
                    'status': 'error',
                    'status_desc': 'http error: %s: %s' % (code, desc)}
        try:
            parse_json = getattr(json, 'loads', None) or getattr(json, 'read')
            parsed_body = parse_json(body)
        except ValueError, exc:
            return {'method': 'AppStoreCli.%s' % method,
                    'status': 'error',
                    'status_desc': 'JSON parsing error'}

        if not isinstance(parsed_body, dict):
            return {'method': 'AppStoreCli.%s' % method,
                    'status': 'error',
                    'status_desc': 'invalid response structure'}
        return parsed_body

    def register_user(self, user):
        '''Registers user.
        '''
        resp = self._call_method('users/register', request_method='POST',
                                 payload=json.dumps({'user': user}),
                                 headers=[('Content-Type', 'application/json'),
                                          ('Accept', 'application/json')])
        return resp

    def inapps(self):
        '''Retrieves inapp list.
        '''
        resp = self._call_method('inapps', request_method='GET')
        return resp

    def create_inapp(self, inapp):
        '''Retrieves inapp list.
        '''
        resp = self._call_method('inapps', request_method='POST',
                                 payload=json.dumps(inapp),
                                 headers=[('Content-Type', 'application/json'),
                                          ('Accept', 'application/json')])
        return resp

    def subscribe(self, uid, product_id, subs_plan):
        '''Retrieves inapp list.
        '''
        resp = self._call_method('inapps/subs/subscribe',
                                 request_method='POST',
                                 payload=json.dumps(
                                     {'userId': uid,
                                      'productId': product_id,
                                      'subscriptionPlan': subs_plan}),
                                 headers=[('Content-Type', 'application/json'),
                                          ('Accept', 'application/json')])
        return resp

    def koga(self, sum, shop_id, scid, trust_payment_id, external_id, customerNumber):
        '''Retrieves inapp list.
        '''
        resp = self._call_method('https://koga.yandex.ru/eshop.xml',
                                 request_method='GET',
                                 payload=json.dumps(
                                     {'Sum': sum,
                                      'ShopId': shop_id,
                                      'scid': scid,
                                      'trust_payment_id': trust_payment_id,
                                      'external_id': external_id,
                                      'customerNumber': customerNumber}),
                                 )
        return resp

    def restore(self, uid, product_id):
        '''Restore receipts.
        '''
        resp = self._call_method('inapps/receipts/restore',
                                 request_method='POST',
                                 payload=json.dumps(
                                     {'uid': uid,
                                      'pid': product_id}),
                                 headers=[('Content-Type', 'application/json'),
                                          ('Accept', 'application/json')])
        return resp

    def verify(self, receipt):
        '''Verifies receipt.
        '''
        resp = self._call_method('inapps/receipts/verifyReceipt',
                                 request_method='POST',
                                 payload=json.dumps({'receipt': receipt}),
                                 headers=[('Content-Type', 'application/json'),
                                          ('Accept', 'application/json')])
        return resp
