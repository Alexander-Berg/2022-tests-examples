# coding: utf-8
import pytest

from json import loads
from django.core.urlresolvers import reverse

from .utils import skipif_no_git

pytestmark = pytest.mark.django_db(transaction=True)


@skipif_no_git
def test_get_file_content(client, users, repo, clone):
    client.login(username='vasya')
    path = 'src/requirements.txt'
    sha = '92bd418e5398eaaa3e51edf2f03891c19915f61e'

    response = loads(client.get(
        reverse('api:contents', args=[repo.source.code, repo.owner, repo.name, path]) +
        '?ref=' + sha
    ).content)

    assert response['type'] == 'file'
    assert response['path'] == path
    assert response['sha'] == '551a1bc268335d4d1aec9cb6926ca40a453b880e'
    assert response['encoding'] == 'utf-8'
    assert response['content'] == '-r ../deps/python-main.txt\n-r ../deps/python-dev.txt\n'


@skipif_no_git
def test_get_dir_content(client, users, repo, clone):
    client.login(username='vasya')
    path = 'src'
    sha = '085009dcf3fcf9c1b0d192c92f8e261f8be6da87'

    response = loads(client.get(
        reverse('api:contents', args=[repo.source.code, repo.owner, repo.name, path]) +
        '?ref=' + sha
    ).content)

    assert len(response) == 6
    assert response[0]['type'] == 'file'
    assert 'content' not in response[0]
    assert response[1]['type'] == 'dir'
    assert response[1]['path'] == 'src/magiclinks'
