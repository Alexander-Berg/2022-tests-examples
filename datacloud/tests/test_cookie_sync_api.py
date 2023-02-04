import os
import requests
import responses
from hamcrest import assert_that, equal_to, is_
import mock
from datacloud.dev_utils.bigb import bigb_utils
from datacloud.score_api.storage.cookie_sync.cookie_sync_api import CookieSyncApi
from datacloud.score_api.storage.cookie_sync.optimistic import OptimisticCookieSyncSet


GET_MATCHES_200 = {
    'header': {'status': 'OK'},
    'body': {'crypta_id': '123', 'yuid': '456'}
}
GET_CID_200 = {
    u'data': [{
        u'is_full': u'1',
        u'segment': [{
            u'data': {u'CryptaId': 22222222222222222222L},
            u'id': u'730',
            u'name': u'indevice-identifiers'
        }],
        u'id': u'1',
        u'name': u'big brother'
    }],
    u'id': u'1111111111111111111'
}


def _fake_tvm_get_request(url, src, dst, timeout=1):
    return requests.get(url)


@mock.patch('datacloud.dev_utils.tvm.tvm_utils.TVMManager.tvm_get_request',
            side_effect=_fake_tvm_get_request)
class TestCookieSyncApi(object):

    def setup_class(cls):
        os.environ['TVM_SECRET'] = 'some-fake-secret'

    def test_loockup_yuids(self, _):
        pass

    @responses.activate
    def test_get_matches_ok(self, _):
        api = CookieSyncApi(OptimisticCookieSyncSet())
        responses.add(
            responses.GET,
            api._url,
            json=GET_MATCHES_200,
            status=200)
        yuid, cid = api.get_matches({'cookie_vendor': 'fake', 'cookie': 'fake'})
        assert_that(yuid, equal_to('456'))
        assert_that(cid, equal_to('123'))

    @responses.activate
    def test_get_matches_none(self, _):
        api = CookieSyncApi(OptimisticCookieSyncSet())
        responses.add(
            responses.GET,
            api._url,
            status=403)
        yuid, cid = api.get_matches({'cookie_vendor': 'fake', 'cookie': 'fake'})
        assert_that(yuid, is_(None))
        assert_that(cid, is_(None))

    @responses.activate
    def test_lookup_yuids(self, _):
        api = CookieSyncApi(OptimisticCookieSyncSet())
        responses.add(
            responses.GET,
            api._url,
            json=GET_MATCHES_200,
            status=200)
        yuids = api._lookup_yuids([{'cookie_vendor': 'some', 'cookie': 'value'}])
        assert_that(yuids, equal_to(set(['456'])))

    @responses.activate
    def test_lookup_yuids_empty(self, _):
        api = CookieSyncApi(OptimisticCookieSyncSet())
        responses.add(
            responses.GET,
            api._url,
            status=403)
        yuids = api._lookup_yuids([{'cookie_vendor': 'some', 'cookie': 'value'}])
        assert_that(yuids, equal_to(set()))

    @responses.activate
    def test_yuid_to_cid(self, _):
        api = CookieSyncApi(OptimisticCookieSyncSet())
        yuid = '456'
        responses.add(
            responses.GET,
            bigb_utils._URL.format(yuid=yuid, client='datacloud'),
            json=GET_CID_200,
            status=200)
        cid = api._yuid_to_cid('456')
        assert_that(cid, equal_to(22222222222222222222))

    @responses.activate
    def test_yuid_to_cid_empty(self, _):
        api = CookieSyncApi(OptimisticCookieSyncSet())
        yuid = '456'
        responses.add(
            responses.GET,
            bigb_utils._URL.format(yuid=yuid, client='datacloud'),
            status=403)
        cid = api._yuid_to_cid('456')
        assert_that(cid, is_(None))

    @responses.activate
    def test_lookup_cids(self, _):
        api = CookieSyncApi(OptimisticCookieSyncSet())
        responses.add(
            responses.GET,
            api._url,
            json=GET_MATCHES_200,
            status=200)
        yuid = 456
        responses.add(
            responses.GET,
            bigb_utils._URL.format(yuid=yuid, client='datacloud'),
            json=GET_CID_200,
            status=200)
        cid = api.lookup_cids([{'cookie_vendor': 'fake', 'cookie': 'fake'}])
        hashed_cid = 16677664979511863544L
        assert_that(cid, equal_to(set([hashed_cid])))

    @responses.activate
    def test_lookup_cids_empty(self, _):
        api = CookieSyncApi(OptimisticCookieSyncSet())
        responses.add(
            responses.GET,
            api._url,
            status=403)
        cid = api.lookup_cids([{'cookie_vendor': 'fake', 'cookie': 'fake'}])
        assert_that(cid, is_(set()))
