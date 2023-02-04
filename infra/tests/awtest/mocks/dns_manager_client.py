import mock

from awacs.lib import dns_resolver


class DnsResolverMock(dns_resolver.DnsResolver):
    get_address_record = mock.Mock()
    get_address_record.side_effect = lambda *a, **kwa: {'127.0.0.1', }


class DnsManagerClientMock(mock.Mock):
    create_request = mock.Mock()
    create_request.side_effect = lambda *a, **kwa: {u'uuid': u'test-req-id'}
    get_request_status = mock.Mock()
    get_request_status.side_effect = lambda *a, **kwa: {u'meta': {u'state': u'done'}}
    list_requests = mock.Mock()
    list_requests.side_effect = lambda *a, **kwa: [{u'uuid': '123', u'owner': None}]
    cancel_request = mock.Mock()
