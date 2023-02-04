from unittest.mock import patch

import pytest

from intranet.compositor_processors.src.clients.connect import ConnectInteractionException
from intranet.compositor_processors.src.processors.connect import (
    disable_service,
    enable_service,
    create_org,
)


def test_enable_service(test_vcr):
    with patch('intranet.compositor_processors.src.clients.base.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some-ticket'
        with test_vcr.use_cassette('directory_enable_service.yaml'):
            enable_service(1, '1', 'wiki')


def test_disable_service(test_vcr):
    with patch('intranet.compositor_processors.src.clients.base.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some-ticket'
        with test_vcr.use_cassette('directory_disable_service.yaml'):
            disable_service(1, '1', 'wiki')


def test_enable_service_error(test_vcr):
    with patch('intranet.compositor_processors.src.clients.base.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some-ticket'
        with test_vcr.use_cassette('directory_enable_service_error.yaml'), pytest.raises(ConnectInteractionException):
            enable_service(1, '1', 'wiki')


def test_create_org_with_domain(test_vcr):
    with patch('intranet.compositor_processors.src.clients.base.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some-ticket'
        with test_vcr.use_cassette('directory_create_org_w_domain.yaml'):
            create_org(
                uid='1',
                org_name='Test Meta Org',
                org_domain_name='johnnycash.com',
                org_language='en',
            )


def test_create_org_without_name(test_vcr):
    with patch('intranet.compositor_processors.src.clients.base.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some-ticket'
        with test_vcr.use_cassette('directory_create_org_wo_name.yaml'):
            create_org(
                uid='1',
                org_domain_name='test.ru',
            )


def test_create_org_without_domain(test_vcr):
    with patch('intranet.compositor_processors.src.clients.base.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some-ticket'
        with test_vcr.use_cassette('directory_create_org_wo_domain.yaml'):
            create_org(
                uid='1',
                org_name='Test Meta Org',
            )


def test_create_org_error(test_vcr):
    with patch('intranet.compositor_processors.src.clients.base.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some-ticket'
        with test_vcr.use_cassette('directory_create_org_error.yaml'):
            with pytest.raises(ConnectInteractionException):
                create_org(
                    uid='1',
                    org_name='Test Meta Org',
                    org_domain_name='yandex.ru',
                    org_language='com',
                )


def test_create_org_with_existing(test_vcr):
    with patch('intranet.compositor_processors.src.clients.base.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some-ticket'
        with test_vcr.use_cassette('directory_create_org_with_existing.yaml'):
            result = create_org(
                uid='4034386806',
            )
            assert result == {'org_id': 103432}
