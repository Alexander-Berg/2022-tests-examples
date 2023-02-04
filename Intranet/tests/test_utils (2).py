# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import pytest

import app.utils.otrs as otrs_utils


@pytest.fixture
def patch_otrs_response(monkeypatch):

    class ResponseMock(object):

        def json(self):
            return {'result': {'list': [{'TicketNumber': 123}]}}

    def post_mock(url, data):
        return ResponseMock()

    monkeypatch.setattr('requests.post', post_mock)


def test_get_ticket_number(patch_otrs_response):
    client = otrs_utils.OTRSClient('url', 'key')
    assert client.get_ticket_number(123) == 123
