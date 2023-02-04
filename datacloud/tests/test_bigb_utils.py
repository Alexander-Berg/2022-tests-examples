import os
import mock
import requests
import responses
from hamcrest import assert_that, is_, equal_to
from datacloud.dev_utils.tvm import tvm_utils
from datacloud.dev_utils.bigb import bigb_utils


RESP_200_NO_ID = {
    u'data': [{
        u'is_full': u'1',
        u'segment': [{u'data': {}, u'id': u'730', u'name': u'indevice-identifiers'}],
        u'id': u'1',
        u'name': u'big brother'
    }],
    u'id':
    u'123'
}

RESP_200_WITH_ID = {
    u'data': [{
        u'is_full': u'1',
        u'segment': [{
            u'data': {u'CryptaId': 22222222222222222222L},
            u'id': u'730',
            u'name': u'indevice-identifiers'}
        ],
        u'id': u'1',
        u'name': u'big brother'
    }],
    u'id': u'1111111111111111111'
}


def _fake_tvm_get_request(url, src, dst, timeout=1):
    return requests.get(url)


@mock.patch('datacloud.dev_utils.tvm.tvm_utils.TVMManager.tvm_get_request',
            side_effect=_fake_tvm_get_request)
class TestBigbUtils(object):

    @classmethod
    def setup_class(cls):
        os.environ['TVM_SECRET'] = 'so-tvm-such-secret'

    @responses.activate
    def test_return_cid_when_found(self, _):
        yuid = '1111111111111111111'
        responses.add(
            responses.GET,
            bigb_utils._URL.format(yuid=yuid, client='datacloud'),
            json=RESP_200_WITH_ID,
            status=200)
        manager = tvm_utils.TVMManager()
        cid = bigb_utils.yuid_to_cid(manager, yuid)
        expected = 22222222222222222222
        assert_that(cid, equal_to(expected))

    @responses.activate
    def test_return_none_when_cid_not_found(self, _):
        yuid = '123'
        responses.add(
            responses.GET,
            bigb_utils._URL.format(yuid=yuid, client='datacloud'),
            json=RESP_200_NO_ID,
            status=200)
        manager = tvm_utils.TVMManager()
        cid = bigb_utils.yuid_to_cid(manager, yuid)
        assert_that(cid, is_(None))

    @responses.activate
    def test_return_none_when_not_200(self, _):
        yuid = '123'
        responses.add(
            responses.GET,
            bigb_utils._URL.format(yuid=yuid, client='datacloud'),
            json={'fake': 'data'},
            status=403)
        manager = tvm_utils.TVMManager()
        cid = bigb_utils.yuid_to_cid(manager, yuid)
        assert_that(cid, is_(None))

    @responses.activate
    def test_return_none_when_empty_response(self, _):
        yuid = '123'
        responses.add(
            responses.GET,
            bigb_utils._URL.format(yuid=yuid, client='datacloud'),
            json={},
            status=200)
        manager = tvm_utils.TVMManager()
        cid = bigb_utils.yuid_to_cid(manager, yuid)
        assert_that(cid, is_(None))
