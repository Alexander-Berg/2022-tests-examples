# coding: utf-8



import pytest

from mock import patch, call, Mock

from django.conf import settings

from intranet.dogma.dogma.core.models import Source, Repo
from intranet.dogma.dogma.core.backends.hg import Repo as HgRepo


pytestmark = pytest.mark.django_db(transaction=True)


@pytest.fixture
def hg_source(transactional_db,):
    source = Source(vcs_type='hg', vcs_protocol='https')
    source.save()
    return source


@pytest.fixture
def hg_repo(hg_source):
    return Repo(source=hg_source, owner='foo', name='bar', vcs_name='foo/bar')


def test_hg_protocol():
    def get_repo(protocol):
        source = Source(vcs_type='svn', host='testhost', vcs_protocol=protocol)
        source.save()
        return HgRepo(Repo(
            source=source,
            owner='foo',
            vcs_name='bar'))

    assert get_repo('https').get_url() == 'https://%s:%s@testhost/bar' % (settings.DOGMA_FETCH_USERNAME,
                                                                          settings.DOGMA_FETCH_PASSWORD)


def test_hg_clone(hg_repo):
    repo = HgRepo(hg_repo)

    with patch.object(repo, 'run_hg_command') as mock_run_hg_command:
        repo.clone()

        assert (mock_run_hg_command.mock_calls ==
                [call('clone', '--uncompressed',
                      'https://%s:%s@/foo/bar' % (settings.DOGMA_FETCH_USERNAME, settings.DOGMA_FETCH_PASSWORD), '/foo/bar')])


def test_hg_fetch(hg_repo):
    repo = HgRepo(hg_repo)
    repo._remove_lock_if_exists = lambda *args: None

    with patch.object(repo, 'run_hg_command') as mock_run_hg_command,\
            patch.object(repo, 'has_new_commits'):
        repo.fetch()

        assert (mock_run_hg_command.mock_calls == [
            call('pull', '-u', 'https://%s:%s@/foo/bar' % (settings.DOGMA_FETCH_USERNAME, settings.DOGMA_FETCH_PASSWORD), _cwd='/foo/bar')])


def test_hg_check_fetch_with_new_commits(hg_repo):
    repo = HgRepo(hg_repo)
    repo._remove_lock_if_exists = lambda *args: None

    with patch.object(repo, 'run_hg_command') as mock_run_hg_command,\
            patch.object(repo, 'has_new_commits') as mock_has_new_commits:
        mock_has_new_commits.return_value = True
        has_new_commits = repo.fetch()

        assert mock_run_hg_command.called is True
        assert has_new_commits is True


def test_hg_check_fetch_without_new_commits(hg_repo):
    repo = HgRepo(hg_repo)
    repo._remove_lock_if_exists = lambda *args: None

    with patch.object(repo, 'run_hg_command') as mock_run_hg_command,\
            patch.object(repo, 'has_new_commits') as mock_has_new_commits:
        mock_has_new_commits.return_value = None
        has_new_commits = repo.fetch()

        assert mock_run_hg_command.called is False
        assert has_new_commits is None


def test_get_commit_native_id(hg_repo):
    repo = HgRepo(hg_repo)

    raw_repo = Mock()

    commit = Mock()
    commit.hex = 'foobar'
    commit._initial = Mock(extra=lambda *args: [])

    assert repo.get_commit_native_id(raw_repo, commit) == """foobar"""
