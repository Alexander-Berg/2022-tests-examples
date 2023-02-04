import pytest

from mock import patch

from intranet.compositor_processors.src.processors.wiki import create_wiki_page
from intranet.compositor_processors.src.clients.exceptions import WikiInteractionException


def test_create_wiki_page_success(test_vcr):
    with patch('intranet.compositor_processors.src.clients.base.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some_ticket'
        with test_vcr.use_cassette('wiki_create_page_success.yaml'):
            create_wiki_page(
                uid='1130000001559746',
                website='yandex.ru',
                org_id=100322,
            )


def test_create_wiki_page_fail(test_vcr):
    with patch('intranet.compositor_processors.src.clients.base.get_service_ticket') as mock_service_ticket:
        mock_service_ticket.return_value = 'some_ticket'
        with test_vcr.use_cassette('wiki_create_page_fail.yaml'):
            with pytest.raises(WikiInteractionException):
                create_wiki_page(
                    uid='1130000001559746',
                    website='yandex.ru',
                    org_id=100322,
                )
