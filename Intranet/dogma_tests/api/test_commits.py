# coding: utf-8
import pytest
pytestmark = pytest.mark.django_db(transaction=True)

from json import loads

from django.core.urlresolvers import reverse

from .utils import skipif_no_git
from intranet.dogma.dogma.core.logic.commits import PRIVATE_REPO_MESSAGE


@pytest.mark.skip(reason='Сейчас эта ручка не работает так как ничего не знает о тикетах, очередях')
@skipif_no_git
def test_get_commit(client, users, repo):
    client.login(username='vasya')
    sha = 'b56ebec6ec0086eea9b041b7613a47f5e028a4c7'

    response = loads(client.get(reverse('api:commit', args=[sha])).content)
    assert response['sha'] == sha
    assert response['commit']['message'] == 'WIKI-10209 \u0411\u0435\u0440\u0435\u043c \u0438\u043c\u044f \u0438\u0437 \u0434\u0440\u0443\u0433\u043e\u0433\u043e \u043f\u043e\u043b\u044f (#46)\n\n'
    assert response['stats'] == {'additions': 1, 'deletions': 2, 'total': 3}
    assert response['parents'][0]['sha'] == '85a572a80d77203259ce80c60bb60f52a558bc14'


def test_list_commits(client, settings, users, repo, pushedcommits):
    client.login(username='vasya')

    raw_response = client.get(reverse('api:commits', args=[repo.source.code, repo.owner, repo.name]))
    assert raw_response['Link'] == (
        '<http://testserver/api/v3/repos/github:tools/dogma/commits?page=2>; rel="next"'
    )

    response = loads(raw_response.content)
    assert len(response) == settings.DOGMA_API_DEFAULT_PAGINATE_BY

    assert response[0]['sha']
    assert response[0]['parents']
    assert response[0]['commit']['author']['email']
    assert response[0]['commit']['author']['login']
    assert response[0]['commit']['committer']['email']
    assert response[0]['commit']['message']

    response = loads(client.get(reverse('api:commits', args=[repo.source.code, repo.owner, repo.name]) +
                                '?per_page=100500').content)
    assert len(response) == settings.DOGMA_API_MAX_PAGINATE_BY


def test_commits_in_file(client, repo, pushedcommits_dates):
    client.login(username='vasya')

    response = loads(client.get(reverse('api:commits',
                                        args=[repo.source.code, repo.owner, repo.name]) +
                                '?&sha=master').content)
    all_sha = set(i['sha'] for i in response)
    assert 'edf1d67f432003d3d84d3d7b46af7fc518eec475' in all_sha
    assert '0f7153fccd4a40b2b34bcb35fde5695ea2cabc6e' in all_sha
    assert len(all_sha) >= 2


def test_commits_in_file_since_date(client, repo, pushedcommits_dates):
    client.login(username='vasya')

    response = loads(client.get(reverse('api:commits',
                                        args=[repo.source.code, repo.owner, repo.name]) +
                                '?&sha=master&since=2014-12-22T18:26:55Z').content)
    all_sha = set(i['sha'] for i in response)
    assert 'edf1d67f432003d3d84d3d7b46af7fc518eec475' not in all_sha
    assert '0f7153fccd4a40b2b34bcb35fde5695ea2cabc6e' in all_sha
    assert len(all_sha) >= 1


def test_search_commit_by_author(client, users, user, another_user,
                                 repo, commit, commit_duplicate):
    client.login(username='vasya')

    response = loads(client.get('{}?{}'.format(reverse('api:commits_search'), 'q=author:smosker,chapson,vsem_privet')).content)

    commit_response = response[0]
    assert commit_response['commit']['message'] == commit.message
    assert commit_response['stats'] == {'additions': 43, 'deletions': 50, 'total': 93}
    assert len(response) == 1


def test_search_commit_by_author_private(client, users, user, another_user,
                                         repo, commit, commit_duplicate):
    client.login(username='vasya')
    repo.is_public = False
    repo.save()

    response = loads(
        client.get(
            '{}?{}'.format(reverse('api:commits_search'), 'q=author:smosker,chapson,vsem_privet')
        ).content
    )

    commit_response = response[0]
    assert commit_response['commit']['message'] == PRIVATE_REPO_MESSAGE
    assert commit_response['stats'] == {'additions': 43, 'deletions': 50, 'total': 93}
    assert len(response) == 1


def test_get_commit_data(client, users, user, another_user,
                         repo, commit, commit_duplicate):
    client.login(username='vasya')

    response = loads(client.get(reverse('api:commit', args=[commit.commit])).content)

    assert response['commit']['message'] == commit.message
    assert response['stats'] == {'additions': 43, 'deletions': 50, 'total': 93}


def test_search_gerrit_commit_by_author(client, another_user, gerrit_commit):
    client.login(username='vasya')

    response = loads(client.get('{}?{}'.format(reverse('api:commits_search'), 'q=author:smosker')).content)
    assert len(response) == 1

    commit_response = response[0]
    assert commit_response['commit']['message'] == gerrit_commit.message
    assert commit_response['stats'] == {'additions': 20, 'deletions': 10, 'total': 30}
    assert commit_response['_dogma']['html_url'] == 'https://gerrit.yandex-team.ru/c/yandex-phone/device/yandex/+/1989'


def test_search_commit_by_source(client, users, user, another_user,
                                 repo, commit, commit_duplicate):
    client.login(username='vasya')

    response = loads(client.get('{}?{}'.format(reverse('api:commits_search'), 'q=source:github')).content)

    commit_response = response[0]
    assert commit_response['commit']['message'] == commit.message
    assert commit_response['stats'] == {'additions': 43, 'deletions': 50, 'total': 93}
    assert len(response) == 1


def test_search_commit_by_source_not_exists(client, users, user, another_user,
                                            repo, commit, commit_duplicate):
    client.login(username='vasya')

    response = loads(client.get('{}?{}'.format(reverse('api:commits_search'), 'q=source:hg')).content)

    assert len(response) == 0


def test_search_commit_by_commit_time(client, users, user, another_user,
                                 repo, commit, commit_duplicate):
    client.login(username='vasya')

    response = loads(client.get('{}?{}'.format(reverse('api:commits_search'),
                                               'q=commit_time:1996-12-19T10:39:57-08,1996-12-19T21:39:57-08')).content)

    commit_response = response[0]
    assert commit_response['commit']['message'] == commit.message
    assert commit_response['stats'] == {'additions': 43, 'deletions': 50, 'total': 93}
    assert commit_response['commit']['author']['date'] == '1996-12-20T00:39:57'
    assert len(response) == 1
