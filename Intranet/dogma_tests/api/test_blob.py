# coding: utf-8
import pytest
pytestmark = pytest.mark.django_db(transaction=True)

from json import loads

from django.core.urlresolvers import reverse

from .utils import skipif_no_git


@skipif_no_git
def test_get_blob(client, users, repo, clone):
    client.login(username='vasya')
    sha = 'e01174e88a8ef6e9bb0ad29839608c8f7718ebe7'

    response = loads(client.get(reverse('api:blob', args=[repo.source.code, repo.owner, repo.name, sha])).content)

    assert response['size'] == 71
    assert response['sha'] == sha
    assert response['encoding'] == 'utf-8'
    assert response['content'] == '# magiclinks\n\nhttps://wiki.yandex-team.ru/wiki/components/magic-links/\n'
