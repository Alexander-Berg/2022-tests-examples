# coding=utf-8

import json
from httplib import INTERNAL_SERVER_ERROR

import mock
import pytest
import httpretty
from requests import HTTPError

from balance import mapper
from balance.constants import TVMToolAliases, TVM2_SERVICE_TICKET_HEADER
from balance.actions.fetch_restricted_domains import fetch, insert

from tests import object_builder as ob


URL = 'https://direct-api-dev.yandex.net/GetHostings'


@pytest.fixture()
def get_service_ticket_mock():
    with mock.patch('balance.actions.fetch_restricted_domains.get_service_ticket') as get_service_ticket:
        yield get_service_ticket


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_fetch_ok(get_service_ticket_mock):
    ticket = ob.generate_character_string(5)
    get_service_ticket_mock.return_value = ticket

    expected_domains = ['ya.ru', 'vk.com']
    body = json.dumps({'hostings': expected_domains})
    httpretty.register_uri(httpretty.GET, URL, body)

    actual_domains = fetch(URL)
    assert actual_domains == expected_domains

    latest_requests = httpretty.httpretty.latest_requests
    assert len(latest_requests) == 1, latest_requests
    request = httpretty.httpretty.latest_requests[0]
    headers = dict(request.headers)
    assert TVM2_SERVICE_TICKET_HEADER.lower() in headers
    assert headers[TVM2_SERVICE_TICKET_HEADER.lower()] == ticket

    get_service_ticket_mock.assert_called_once_with(TVMToolAliases.YB_MEDIUM, TVMToolAliases.DIRECT)


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_fetch_fail(get_service_ticket_mock):
    get_service_ticket_mock.return_value = ob.generate_character_string(5)
    httpretty.register_uri(httpretty.GET, URL, 'fail', status=INTERNAL_SERVER_ERROR)
    with pytest.raises(HTTPError):
        fetch(URL)


def test_insert(session):
    initial = [
        ('ya.ru', 'Direct'),
        ('mail.ru', 'Direct'),
        (u'путин.рф', 'Billing'),
        (u'президент.рф', 'Billing'),
    ]
    for domain, source in initial:
        ob.RestrictedDomainBuilder.construct(session, domain=domain, restrict_source=source)

    insert(session, ['ya.ru', u'путин.рф', 'fb.com', u'собянин.рф'])
    actual = [(row.domain, row.restrict_source)
              for row in session.query(mapper.RestrictedDomain)]
    expected = initial + [('fb.com', 'Direct'),
                          (u'собянин.рф', 'Direct')]
    assert actual == expected
