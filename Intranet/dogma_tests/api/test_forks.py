# coding: utf-8
import pytest
pytestmark = pytest.mark.django_db(transaction=True)

from json import loads

from django.core.urlresolvers import reverse


def test_forks_smoke(client, users, repo):
    client.login(username='vasya')

    response = loads(client.get(
        reverse('api:forks',
                args=[repo.source.code, repo.owner, repo.name])).content)

    assert len(response) >= 0
