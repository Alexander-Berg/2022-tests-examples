# coding: utf-8

# coding: utf-8
import pytest
pytestmark = pytest.mark.django_db(transaction=True)

from json import loads

from django.core.urlresolvers import reverse

from .utils import skipif_no_git


@skipif_no_git
def test_get_commit(client, users, repo):
    client.login(username='vasya')
    sha1 = 'master'
    sha2 = 'orphan_branch_for_tests'

    resp = client.get(reverse('api:compare', args=[repo.source.code, repo.owner,
                                                   repo.name, sha1, sha2])
    )
    resp.status_code == 404
