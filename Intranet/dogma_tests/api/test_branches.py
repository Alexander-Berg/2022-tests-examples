# coding: utf-8
import pytest
pytestmark = pytest.mark.django_db(transaction=True)

from json import loads

from django.core.urlresolvers import reverse

from .utils import skipif_no_git


@skipif_no_git
def test_list_branches(client, settings, users, repo, clone):
    client.login(username='vasya')
    response = loads(client.get(
        reverse('api:branches',
                args=[repo.source.code, repo.owner, repo.name]
                )
    ).content)
    assert any(b['name'] == 'develop' for b in response)
    assert len(response) == 11
