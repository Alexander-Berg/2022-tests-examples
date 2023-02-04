import pytest
from unittest.mock import patch

from intranet.search.core.sources.utils import get_popularity_factors
from intranet.search.tests.helpers import models_helpers as mh


pytestmark = pytest.mark.django_db


def test_get_popularity_factors_for_moved_to_page():
    moved_page = mh.MovedPage()
    factors = get_popularity_factors(moved_page.old_url)
    assert factors == {'is_moved': True, 'links': {}, 'search_queries': {}}


@patch('intranet.search.core.sources.utils.get_page_links')
@patch('intranet.search.core.sources.utils.get_page_clicked_marked_texts')
def test_get_popularity_factors_for_moved_from_page(patched_texts, patched_links):
    patched_links.return_value = {'links': 'test'}
    patched_texts.return_value = {'popular': 'none'}
    moved_page = mh.MovedPage()

    factors = get_popularity_factors(moved_page.new_url)

    expected = {
        'is_moved': False,
        'links': patched_links.return_value,
        'search_queries': patched_texts.return_value
    }
    assert factors == expected
    patched_links.assert_called_once_with([moved_page.new_url, moved_page.old_url])
    patched_texts.assert_called_once_with([moved_page.new_url, moved_page.old_url])


@patch('intranet.search.core.sources.utils.get_page_links')
@patch('intranet.search.core.sources.utils.get_page_clicked_marked_texts')
def test_get_popularity_factors_for_not_moved_page(patched_texts, patched_links):
    patched_links.return_value = {'links': 'test'}
    patched_texts.return_value = {'popular': 'none'}
    url = 'https://some.url.org'

    factors = get_popularity_factors(url)

    expected = {
        'is_moved': False,
        'links': patched_links.return_value,
        'search_queries': patched_texts.return_value
    }
    assert factors == expected
    patched_links.assert_called_once_with([url])
    patched_texts.assert_called_once_with([url])
