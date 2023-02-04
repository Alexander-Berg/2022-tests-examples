# coding: utf-8
from __future__ import unicode_literals

import pytest
from mock import Mock, patch

from staff_api.middleware import PingMiddleware


def test_ping_middleware_response_200_when_db_is_available():
    middleware = PingMiddleware()

    request = Mock()
    request.path = '/ping_db/'
    with patch('staff_api.middleware.check_host', return_value=True):
        assert middleware.process_request(request).status_code == 200


def test_ping_middleware_response_500_when_db_is_not_available():
    middleware = PingMiddleware()

    request = Mock()
    request.path = '/ping_db/'
    middleware.db_hosts_to_check = []
    assert middleware.process_request(request).status_code == 500


