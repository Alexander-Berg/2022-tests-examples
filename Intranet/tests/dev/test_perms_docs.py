# coding: utf-8
import pytest

from review.dev import docs

from tests import helpers


def test_wiki_docs_build_docs_roles_wiki_markup():
    assert docs.build_docs_roles_wiki_markup()


@pytest.mark.skip
@pytest.mark.parametrize('format', [
    'wiki',
    'html',
    None,
])
def test_docs_are_rendered(client, format):
    request = {}
    if format is not None:
        request['format'] = format
    helpers.get(
        client=client,
        path='/dev/perms/',
        request=request
    )
