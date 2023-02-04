# coding: utf-8



import pytest

from mock import patch, call, Mock

from django.conf import settings

from intranet.dogma.dogma.core.models import Source, Repo

from intranet.dogma.dogma.core.backends.git import Repo as GitRepo


pytestmark = pytest.mark.django_db(transaction=True)


@pytest.fixture
def git_source(transactional_db,):
    source = Source(vcs_type='git', vcs_protocol='native', host='testhost')
    source.save()
    return source


@pytest.fixture
def git_repo(git_source):
    return Repo(source=git_source, owner='foo', name='bar', vcs_name='foo/bar.git')


def test_git_protocol():
    def get_repo(protocol):
        source = Source(vcs_type='git', host='testhost', vcs_protocol=protocol)
        source.save()
        return GitRepo(Repo(source=source,
                            owner='foo', name='bar', vcs_name='foo/bar.git'))

    assert get_repo('native').get_url() == 'git://testhost/foo/bar.git'
    assert get_repo('ssh').get_url() == 'ssh://%s@testhost/foo/bar.git' % settings.DOGMA_FETCH_USERNAME
    assert get_repo('https').get_url() == 'https://testhost/foo/bar.git'


def test_git_protocol_with_gitlab_source():
    def get_repo(protocol):
        source = Source(vcs_type='git', web_type='gitlab', host='testhost', vcs_protocol=protocol)
        source.save()
        return GitRepo(Repo(source=source,
                            owner='foo', name='bar', vcs_name='foo/bar'))

    assert get_repo('native').get_url() == 'git://testhost/foo/bar.git'
    assert get_repo('ssh').get_url() == 'ssh://%s@testhost/foo/bar.git' % settings.DOGMA_FETCH_USERNAME
    assert get_repo('https').get_url() == 'https://testhost/foo/bar.git'


def test_git_clone(git_repo):
    repo = GitRepo(git_repo)

    with patch.object(repo, 'run_git_command') as mock_run_git_command:
        repo.clone()

        assert mock_run_git_command.mock_calls == [call('clone', '--bare', 'git://testhost/foo/bar.git', '/foo/bar',
                                                        _in=settings.DOGMA_FETCH_PASSWORD),
                                                   call('config', 'remote.origin.fetch', '+refs/heads/*:refs/heads/*',
                                                        )]


def test_git_fetch(git_repo):
    repo = GitRepo(git_repo)
    repo._remove_lock_if_exists = lambda *args: None

    with patch.object(repo, 'run_git_command') as mock_run_git_command,\
            patch.object(repo, 'has_new_commits'):
        repo.fetch()

        assert mock_run_git_command.mock_calls == [call('remote', 'set-url', 'origin', 'git://testhost/foo/bar.git',),
                                                   call('fetch', 'origin', '-v',
                                                        _in=settings.DOGMA_FETCH_PASSWORD)]


def test_git_has_new_commits_with_new_commits(git_repo):
    repo = GitRepo(git_repo)

    result = Mock()
    result.process.stderr = b'''
        some other things
        = [up to date]      master -> master
        b796438..6335e69      some_staff -> some_staff
        some other things'''
    has_new_commits = repo.has_new_commits(result)
    assert has_new_commits is True


def test_git_has_new_commits_without_new_commits(git_repo):
    repo = GitRepo(git_repo)

    result = Mock()
    result.process.stderr = b'''
        some other things
        = [up to date]      master -> master
        = [up to date]      some_staff -> some_staff
        some other things'''
    has_new_commits = repo.has_new_commits(result)
    assert has_new_commits is False
