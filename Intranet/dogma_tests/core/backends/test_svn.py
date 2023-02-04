# coding: utf-8



import pytest

from mock import patch, call, Mock

from django.conf import settings

from intranet.dogma.dogma.core.models import Source, Repo
from intranet.dogma.dogma.core.backends.svn import Repo as SvnRepo


pytestmark = pytest.mark.django_db(transaction=True)


@pytest.fixture
def svn_source(transactional_db,):
    source = Source(vcs_type='svn', host='testhost', vcs_protocol='https')
    source.save()
    return source


@pytest.fixture
def svn_repo(svn_source):
    return Repo(source=svn_source, owner='foo', name='bar', vcs_name='foo/bar')


def test_svn_protocol():
    def get_repo(protocol):
        source = Source(vcs_type='svn', host='testhost', vcs_protocol=protocol)
        source.save()
        return SvnRepo(Repo(source=source,
                            owner='foo', name='bar', vcs_name='foo/bar'))

    assert get_repo('ssh').get_url() == 'svn+ssh://testhost/foo/bar'
    assert get_repo('https').get_url() == 'https://testhost/foo/bar'


def test_svn_clone(svn_repo):
    repo = SvnRepo(svn_repo)

    with patch.object(repo, 'run_git_command') as mock_run_git_command,\
            patch.object(repo, 'has_new_commits'):
        repo.clone()

        assert (mock_run_git_command.mock_calls ==
                [call('svn', 'init', '--stdlayout',
                      '--username', settings.DOGMA_FETCH_USERNAME,
                      'https://testhost/foo/bar', '/foo/bar',
                      _in=settings.DOGMA_FETCH_PASSWORD),
                 call('config', '--local', 'svn.authorsfile', '/storage/dogma/svn_users.txt'),
                 call('config', '--local', '--bool', 'core.bare', 'true'),
                 call('config', '--local', 'svn-remote.svn.fetch', 'trunk:refs/remotes/origin/trunk'),
                 call('config', '--local', 'svn-remote.svn.branches', 'branches/*:refs/remotes/origin/*'),
                 call('config', '--local', '--unset', 'svn-remote.svn.tags', _ok_code=[0, 5]),
                 call('config', '--local', '--unset', 'svn-remote.svn.branches', _ok_code=[0, 5]),
                 call('svn', 'fetch', '-q', '--authors-prog', '/usr/bin/dogma-userinfo',
                      '--username', settings.DOGMA_FETCH_USERNAME,
                      _in=settings.DOGMA_FETCH_PASSWORD)])


def test_svn_fetch(svn_repo):
    repo = SvnRepo(svn_repo)

    with patch.object(repo, 'run_git_command') as mock_run_git_command,\
            patch.object(repo, 'has_new_commits'):
        repo.fetch()

        assert (mock_run_git_command.mock_calls ==
                [call('svn', 'fetch', '-q', '--authors-prog', '/usr/bin/dogma-userinfo',
                      '--username', settings.DOGMA_FETCH_USERNAME,
                      _in=settings.DOGMA_FETCH_PASSWORD)])


def test_svn_has_new_commits_with_new_commits(svn_repo):
    repo = SvnRepo(svn_repo)

    result = Mock()
    result.process.stdout = 'some fetch info, new revisions...'
    has_new_commits = repo.has_new_commits(result)
    assert has_new_commits is True


def test_svn_has_new_commits_without_new_commits(svn_repo):
    repo = SvnRepo(svn_repo)

    result = Mock()
    result.process.stdout = ''
    has_new_commits = repo.has_new_commits(result)
    assert has_new_commits is False


def test_git_get_commit_native_id(svn_repo):
    repo = SvnRepo(svn_repo)

    raw_repo = Mock()

    commit = Mock()
    commit.hex = 'foobar'
    commit.message = """Fixed logging and memory increased in starting scripts.

    git-svn-id: svn+ssh://svn.yandex-team.ru/abar/trunk@277 aede67fe-2447-0410-a7fd-859f749f696c

    """

    assert repo.get_commit_native_id(raw_repo, commit) == """277"""
    assert commit.message == """Fixed logging and memory increased in starting scripts."""
