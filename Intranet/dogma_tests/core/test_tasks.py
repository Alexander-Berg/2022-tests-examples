# coding: utf-8

from mock import patch, Mock
import pytest
from pretend import stub
from datetime import datetime
import pytz
import sh
import logging

from django.conf import settings
from django.test import override_settings

from dir_data_sync.models import Organization, OperatingMode

from ..utils import test_vcr
from intranet.dogma.dogma.core.models import (
    Node, Clone, Source,
    Repo, PushedCommit, User,
    ChangedFile, UserFileStatistics,
    OrganisationsToClone, Credential,
)
from intranet.dogma.dogma.core.backends.svn import Repo as SvnRepo
from intranet.dogma.dogma.core.backends.git import Repo as GitRepo
from intranet.dogma.dogma.core.crawlers.github import GithubCrawler
from intranet.dogma.dogma.core.crawlers.web_svn import WebSvnCrawler
from intranet.dogma.dogma.core.crawlers.base import RepoTuple
from intranet.dogma.dogma.core.crawlers.stash import StashCrawler
from intranet.dogma.dogma.core import tasks
from intranet.dogma.dogma.core.hg.models import Repository
from intranet.dogma.dogma.core.git.models import Repository as GitRepository
from intranet.dogma.dogma.core.utils import get_current_node
from .hg.utils import HG_ROOT
from .git.utils import GIT_ROOT, GIT_ML_ROOT
from intranet.dogma.dogma.core.dao.commits import create_commits_objects
from intranet.dogma.dogma.core.logic.commits import (get_diff_data_for_batch, get_batch_diff, )
from intranet.dogma.dogma.core.logic.users import EmailGuesser
from intranet.dogma.dogma.core.logic.tasks_processing import MIN_SYNC_DELAY
from intranet.dogma.dogma.core.tasks import update_files_changed_statistics
from intranet.dogma.dogma.core.logic.changed_file import get_changed_files_map
from intranet.dogma.dogma.core.errors import (
    NoRepositoryOnNodeError,
    BaseError,
    PermissionError,
)
from github3.exceptions import AuthenticationFailed

pytestmark = pytest.mark.django_db(transaction=True)


def hg_repo():
    return Repository.discover(HG_ROOT)


@pytest.fixture
def clone(transactional_db):
    source = Source.objects.create(code='testsource',
                                   web_type='github',
                                   vcs_type='git',
                                   rate=0.5)
    repo = Repo.objects.create(
        source=source,
        vcs_name='testrepo',
        name='testrepo',
        owner='testuser',
    )
    return Clone.objects.create(
        repo=repo,
        path=GIT_ROOT,
        node=get_current_node(create_missing=True),
        status=Clone.STATUSES.active,
        modified='2016-07-20 21:02:00',
    )


@pytest.fixture
def source_to_fail(transactional_db):
    return Source.objects.create(code='testsource',
                                 web_type='github',
                                 vcs_type='git',
                                 rate=0.5,
                                 status=Source.SYNC_STATUSES.success,
                                 )


@pytest.fixture
def source_success(transactional_db):
    return Source.objects.create(code='svn',
                                 web_type='websvn',
                                 vcs_type='svn',
                                 web_url='https://svn.yandex-team.ru/websvn/wsvn',
                                 host='svn.yandex-team.ru',
                                 web_auth='basic',
                                 vcs_protocol='https',
                                 rate=0.5,
                                 status=Source.SYNC_STATUSES.success,
                                 )


@pytest.fixture
def operating_mode():
    return OperatingMode.objects.create(
        name='test mode'
    )


@pytest.fixture
def org(operating_mode):
    return Organization.objects.create(
        id=1,
        name='yandex', dir_id='1', label='1',
        mode=operating_mode,
    )


@pytest.fixture
def repo_fail(source_to_fail):
    return Repo.objects.create(
        source=source_to_fail,
        vcs_name='testrepo',
        name='testrepo',
        owner='testuser',
    )


@pytest.fixture
def repo_success(source_success):
    return Repo.objects.create(
        source=source_success,
        vcs_name='adv',
        name='adv',
        owner='root',
    )


@pytest.fixture
def clone_fail(repo_fail):
    return Clone.objects.create(
        repo=repo_fail,
        path=GIT_ROOT,
        node=get_current_node(create_missing=True),
        status=Clone.STATUSES.active,
        modified='2016-07-20 21:02:00',
    )


@pytest.fixture
def clone_success(repo_success):
    return Clone.objects.create(
        repo=repo_success,
        path=GIT_ROOT,
        node=get_current_node(create_missing=True),
        status=Clone.STATUSES.active,
        modified='2016-07-20 21:02:00',
    )


@pytest.fixture
def node(transactional_db):
    return Node.objects.create(
        hostname='dogma.yandex.net',
        space_total=1000,
        space_available=700,
    )


@pytest.fixture
def repo_tuple(transactional_db):
    return RepoTuple(name='zlatabox',
                     vcs_name='zlatabox',
                     owner='root',
                     description='',
                     default_branch='master',
                     is_public=True
                     )


@pytest.fixture
def user(transactional_db,):
    return User.objects.create(
        uid='12345',
        login='vsem_privet',
        email='test@ya.ru',
        name='Someone',
        other_emails='test@ya.ru,test_email@ya.ru,anotheremail@test.com',
        from_staff=True,
    )


@pytest.fixture
def another_user(transactional_db,):
    return User.objects.create(
        login='smosker',
        email='test_email@ya.ru',
        name='Anotherone',
    )


@pytest.fixture
def commit(transactional_db, repo_success, another_user):
    return PushedCommit.objects.create(
        repo=repo_success,
        author=another_user,
        committer=another_user,
        commit='edf1d67f432003d3d84d3d7b46af7fc518eec475',
        commit_time='1996-12-19T10:39:57-08:00',
        lines_added=43,
        lines_deleted=50,
        message='some message text',
        branch_name='master',
        tree_hex='ec13a4bd6500ab81a11b148d08a5a6983e58d665',
        commit_id='17092ba4edd863ada20009558b9b55e194ffbe56',
        tickets=['TEST-5'],
        queues=['TEST'],
        create_changed_files=True,
    )


@pytest.fixture
def changed_file_author_without_uid(transactional_db, commit):
    return ChangedFile.objects.create(
        commit=commit,
        name='test_tasks.py',
        extension='py',
        lines_added=100,
        lines_deleted=50,
    )


@pytest.fixture
def commit_with_author_with_uid(transactional_db, repo_success, user):
    return PushedCommit.objects.create(
        repo=repo_success,
        author=user,
        committer=user,
        commit='edf1d67f432003d3d84d3d7b46af7fc518eec473',
        commit_time='1996-12-19T12:39:57-08:00',
        lines_added=43,
        lines_deleted=50,
        message='some text',
        branch_name='master',
        tree_hex='ec13a4bd6500ab81a11b148d08a5a6983e58d365',
        commit_id='17092ba4edd863ada10009558b9b55e194ffbe56',
        tickets=['TEST-456'],
        queues=['TEST'],
        create_changed_files=True,
    )


@pytest.fixture
def changed_file(transactional_db, commit_with_author_with_uid):
    return ChangedFile.objects.create(
        commit=commit_with_author_with_uid,
        name='test_tasks.py',
        extension='py',
        lines_added=100,
        lines_deleted=50,
    )


@pytest.fixture
def another_changed_file(transactional_db, commit_with_author_with_uid):
    return ChangedFile.objects.create(
        commit=commit_with_author_with_uid,
        name='smth.txt',
        extension='txt',
        lines_added=56,
        lines_deleted=30,
    )


@pytest.fixture
def py_file_statistics(user):
    return UserFileStatistics.objects.create(
        user=user,
        extension='py',
        lines_added=100,
        lines_deleted=50,
        period='1996-12-01',
    )


@pytest.fixture
def bitbucket_source():
    return Source.objects.create(
        code='bitbucket',
        web_type='stash',
        vcs_type='git_stash',
        web_url='https://bb.yandex-team.ru',
        host='bb.yandex-team.ru',
        web_auth='x_oauth_token',
        vcs_protocol='https',
        rate=1,
        status=Source.SYNC_STATUSES.success,
    )


@pytest.fixture
def bitbucket_org(bitbucket_source):
    return OrganisationsToClone.objects.create(
        name='METRO',
        source=bitbucket_source,
    )


@pytest.fixture
def bitbucket_credential(org):
    return Credential.objects.create(
        name='test auth',
        auth_type=Credential.AUTH_TYPES.app_password,
        auth_data={
            'login': 'test',
            'app_password': 'test',
        },
        is_success=False,
        connect_organization=org,
    )


@pytest.fixture
def github_org(source_to_fail):
    return OrganisationsToClone.objects.create(
        name='GITHUB_ORG',
        source=source_to_fail,
    )


@pytest.fixture
def github_credential(org):
    return Credential.objects.create(
        name='test auth',
        auth_type=Credential.AUTH_TYPES.token,
        auth_data={
            'token': 'test_token',
        },
        is_success=False,
        connect_organization=org,
    )


@pytest.fixture
def bitbucket_b2b_source():
    return Source.objects.create(
        code='bitbucket',
        web_type='bitbucket_ext',
        vcs_type='git_stash',
        web_url='https://bitbucket.org',
        host='bitbucket.org',
        web_auth='x_oauth_token',
        vcs_protocol='https',
        rate=1,
        status=Source.SYNC_STATUSES.success,
        extra_info={'api_url': 'https://api.bitbucket.org'}
    )


@pytest.fixture
def bitbucket_b2b_org(bitbucket_b2b_source):
    return OrganisationsToClone.objects.create(
        name='dogmatestb2b-NEWTEST',
        source=bitbucket_b2b_source,
    )


def model_repository():
    source = Source(
        name='arcadia',
        code='arcadia',
        vcs_type='git',
        web_type='github',
    )
    source.save()
    repo = Repo(
        source=source,
        owner='root',
        name='arcadia',
    )
    repo.save()
    clone = Clone(
        status='active',
        repo=repo,
        path=HG_ROOT,
        node=get_current_node()
    )
    clone.save()
    Repo.objects.filter(id=repo.id).update(
        modified='2016-07-20 21:02:00', created='2016-07-20 21:02:00')
    return Repo.objects.get(id=repo.id), clone


def model_repository_git():
    source = Source(
        name='arcadia',
        code='arcadia',
        vcs_type='git',
        web_type='github',
    )
    source.save()
    repo = Repo(
        source=source,
        owner='root',
        name='smth',
    )
    repo.save()
    clone = Clone(
        status='active',
        repo=repo,
        path=GIT_ML_ROOT,
        node=get_current_node()
    )
    clone.save()
    Repo.objects.filter(id=repo.id).update(
        modified='2016-07-20 21:02:00', created='2016-07-20 21:02:00')
    return Repo.objects.get(id=repo.id), clone


def hg_commit():
    repo = hg_repo()
    return list(repo.walk(
        b'ab983f65566352121695a8e391fb40b281a801b7', sort_type='tm')
    )


def commits_for_test(repo_raw, hg=True):
    commits_from = datetime(2016, 6, 23, 00, 00, 00, 0, pytz.UTC)
    commits_to = datetime(2016, 7, 4, 00, 00, 00, 0, pytz.UTC)
    for commit in repo_raw.all_commits(exclude=[]):
        if not hg:
            yield commit
        else:
            if commits_from < commit.commit_time < commits_to:
                yield commit


def test_node_info(node):
    st = stub(
        f_bavail=5,
        f_blocks=10,
        f_frsize=100,
    )

    with patch('intranet.dogma.dogma.core.tasks.get_current_node',
               lambda create_missing=True, no_cache=True: node):
        with patch('intranet.dogma.dogma.core.tasks.os', stub(statvfs=lambda *a: st)):
            tasks.node_info()

    node = Node.objects.get(pk=node.pk)
    assert node.space_total == 1000
    assert node.space_available == 500


@pytest.mark.django_db(transaction=True)
def test_create_commits():
    repo, clone = model_repository()
    repo_hg = Repository.discover(HG_ROOT)
    assert PushedCommit.objects.filter(repo=repo).count() == 0
    assert ChangedFile.objects.filter(commit__repo=repo).count() == 0
    with patch('intranet.dogma.dogma.core.tasks.get_repository_model') as get_repository_mock,\
            patch('intranet.dogma.dogma.core.tasks.get_all_repo_commits', lambda *args, **kwargs: commits_for_test(repo_hg)),\
            patch('intranet.dogma.dogma.core.logic.users.get_user_data_by_email_from_staff') as get_user_mock:
                get_user_mock.return_value = None
                get_repository_mock.return_value = repo_hg
                tasks.create_commits(clone.id)

    assert PushedCommit.objects.filter(repo=repo).count() == 61
    assert PushedCommit.objects.filter(repo=repo, create_changed_files=False).count() == 1
    assert PushedCommit.objects.filter(repo=repo, create_changed_files=True).count() == 60
    assert ChangedFile.objects.filter(commit__repo=repo).count() == 523
    commit = PushedCommit.objects.get(commit_id='7ab86dc3f813723f189d5651628eb931d35cf4c6')
    assert commit.lines_added == 30
    assert commit.lines_deleted == 0
    assert commit.message == ('released auto2-shard, '
                              'yandex-auto2-config-tr-testing,'
                              ' yandex-auto2-config-ru-testing, '
                              'yandex-auto2-config-tr-stable,'
                              ' yandex-auto2-config-ru-stable')


@pytest.mark.django_db(transaction=True)
def test_create_commits_git():
    repo, clone = model_repository_git()
    repo_git = GitRepository.discover(GIT_ML_ROOT)
    assert PushedCommit.objects.filter(repo=repo).count() == 0
    assert ChangedFile.objects.filter(commit__repo=repo).count() == 0
    with patch('intranet.dogma.dogma.core.tasks.get_repository_model') as get_repository_mock,\
            patch('intranet.dogma.dogma.core.logic.users.get_user_data_by_email_from_staff') as get_user_mock:
                get_user_mock.return_value = None
                get_repository_mock.return_value = repo_git
                tasks.create_commits(clone.id)

    assert PushedCommit.objects.filter(repo=repo).count() == 419
    assert PushedCommit.objects.filter(repo=repo, create_changed_files=False).count() == 1
    assert PushedCommit.objects.filter(repo=repo, create_changed_files=True).count() == 418
    assert ChangedFile.objects.filter(commit__repo=repo).count() == 2408

    commit = PushedCommit.objects.get(commit_id='a9b145fbb941cc6792ba71474b8bef37b9ee8b60')
    assert commit.lines_added == 59
    assert commit.lines_deleted == 0
    assert commit.message == 'Initial commit\n'

    commit = PushedCommit.objects.get(commit_id='348e19ae7137c2b8273121368053adb8e07debcf')
    assert commit.lines_added == 1610
    assert commit.lines_deleted == 2


def test_source_update_fail(source_to_fail):
    with patch.object(GithubCrawler, 'get_repos',
                      side_effect=BaseError(ValueError('get repo error'))) as mock_crawler_get_repos:
        tasks.update_source(source_to_fail.id)
        assert mock_crawler_get_repos.called is True

    source_to_fail.refresh_from_db()
    assert source_to_fail.status == source_to_fail.SYNC_STATUSES.fail
    assert source_to_fail.last_sync_success_time is None
    assert source_to_fail.last_sync_fail_time is not None
    assert source_to_fail.last_sync_fail_trace is not None
    assert 'get repo error' in source_to_fail.last_sync_fail_trace
    assert source_to_fail.last_sync_fail_error_code == BaseError.db_value
    assert source_to_fail.error_value == BaseError.help


def test_update_source_with_org_bitbucket_success(bitbucket_org, bitbucket_source, org, ):
    assert Repo.objects.filter(source=bitbucket_source).count() == 0
    with test_vcr.use_cassette('update_bitbucket_orgs.yaml'):
        tasks.update_source(bitbucket_source.id)

    assert Repo.objects.filter(source=bitbucket_source).count() == 6
    repo = Repo.objects.get(name='metro-content')
    assert repo.owner == bitbucket_org.name.lower()
    assert repo.organisation == bitbucket_org
    assert repo.is_public is False

    repo = Repo.objects.get(name='tanker-tool')
    assert repo.is_public is False


def test_update_source_with_org_bitbucket_b2b_success(bitbucket_b2b_org,
                                                      bitbucket_b2b_source,
                                                      org,
                                                      bitbucket_credential,
                                                      ):
    bitbucket_b2b_org.credentials.add(bitbucket_credential)
    bitbucket_credential.is_success = False
    bitbucket_credential.save()
    assert Repo.objects.filter(source=bitbucket_b2b_source).count() == 0
    with test_vcr.use_cassette('update_bitbucket_b2b_orgs.yaml'):
        with override_settings(IS_BUSINESS=True):
            tasks.update_source(bitbucket_b2b_source.id)

    assert Repo.objects.filter(source=bitbucket_b2b_source).count() == 1
    repo = Repo.objects.get(name='CRM_system')
    team, project = bitbucket_b2b_org.name.split('-')
    assert repo.owner == team
    assert repo.organisation == bitbucket_b2b_org
    bitbucket_credential.refresh_from_db()
    assert bitbucket_credential.is_success is True


def test_update_source_with_org_bitbucket_credentials_fail(bitbucket_org, bitbucket_source,
                                                           org, bitbucket_credential,
                                                           ):
    bitbucket_org.credentials.add(bitbucket_credential)
    bitbucket_credential.is_success = True
    bitbucket_credential.save()
    assert bitbucket_source.status == bitbucket_source.SYNC_STATUSES.success
    assert Repo.objects.filter(organisation=bitbucket_org).count() == 0
    with override_settings(IS_BUSINESS=True):
        with patch.object(StashCrawler, 'repos_by_organisation',
                          side_effect=BaseError(ValueError('get repo error'))):
            tasks.update_source(bitbucket_source.id)

    bitbucket_credential.refresh_from_db()
    bitbucket_source.refresh_from_db()
    assert bitbucket_source.status == bitbucket_source.SYNC_STATUSES.fail
    assert Repo.objects.filter(organisation=bitbucket_org).count() == 0
    assert bitbucket_credential.is_success is True


def test_update_source_with_org_bitbucket_no_auth_credentials_fail(bitbucket_org, bitbucket_source,
                                                                   org, bitbucket_credential,
                                                                   ):
    bitbucket_org.credentials.add(bitbucket_credential)
    bitbucket_credential.is_success = True
    bitbucket_credential.save()
    assert bitbucket_source.status == bitbucket_source.SYNC_STATUSES.success
    assert Repo.objects.filter(organisation=bitbucket_org).count() == 0
    with override_settings(IS_BUSINESS=True):
        with patch.object(StashCrawler, 'repos_by_organisation',
                          side_effect=AuthenticationFailed(Mock())):
            tasks.update_source(bitbucket_source.id)

    bitbucket_credential.refresh_from_db()
    bitbucket_source.refresh_from_db()
    assert bitbucket_source.status == bitbucket_source.SYNC_STATUSES.fail
    assert Repo.objects.filter(organisation=bitbucket_org).count() == 0
    assert bitbucket_credential.is_success is False


def test_source_update_success(source_success, repo_tuple, org, ):
    with patch.object(WebSvnCrawler, 'get_repos', return_value=(repo_tuple,)) as mock_crawler_get_repos:
        tasks.update_source(source_success.id)
        assert mock_crawler_get_repos.called is True

    source_success.refresh_from_db()
    assert source_success.status == source_success.SYNC_STATUSES.success
    assert source_success.last_sync_success_time is not None
    assert source_success.last_sync_fail_time is None
    assert source_success.last_sync_fail_trace is None
    assert source_success.last_sync_fail_error_code is None
    assert Repo.objects.count() == 1
    repo = Repo.objects.first()
    assert repo.connect_organization.count() == 1
    assert repo.connect_organization.first() == Organization.objects.get(dir_id=settings.INTERNAL_DIR_ID)


def test_clone_repo_fail(repo_fail):
    assert repo_fail.clone_attempt == 0
    assert repo_fail.sync_delay == 1
    with patch.object(GitRepo, 'clone',
                      side_effect=sh.ErrorReturnCode(full_cmd=b'clone repo',
                                                     stdout=b'',
                                                     stderr=b'Got 403 Forbidden while accessing source',
                                                     )) as mock_repo_clone:
        tasks.clone_repo(repo_fail.id)
        assert mock_repo_clone.called is True

    repo_fail.refresh_from_db()
    assert repo_fail.status == repo_fail.SYNC_STATUSES.fail
    assert repo_fail.last_sync_success_time is None
    assert repo_fail.last_sync_fail_time is not None
    assert repo_fail.last_sync_fail_trace is not None
    assert 'Got 403 Forbidden while accessing source' in repo_fail.last_sync_fail_trace
    assert repo_fail.last_sync_fail_error_code == PermissionError.db_value
    assert repo_fail.error_value == PermissionError.help
    assert repo_fail.clone_attempt == 1
    assert repo_fail.sync_delay == 2


def test_clone_repo_with_credential_fail(repo_fail, github_credential, org, ):
    repo_fail.credentials.add(github_credential)
    assert github_credential.is_success is False
    with patch.object(GitRepo, 'base_clone_command',
                      side_effect=sh.ErrorReturnCode(full_cmd=b'clone repo',
                                                     stdout=b'',
                                                     stderr=b'Got 403 Forbidden while accessing source',
                                                     )) as mock_repo_clone:
        with override_settings(IS_BUSINESS=True):
            tasks.clone_repo(repo_fail.id)
        assert mock_repo_clone.called is True

    repo_fail.refresh_from_db()
    github_credential.refresh_from_db()
    assert repo_fail.status == repo_fail.SYNC_STATUSES.fail
    assert github_credential.is_success is False


def test_clone_repo_with_credential_success(repo_fail, github_credential, org, ):
    repo_fail.credentials.add(github_credential)
    assert github_credential.is_success is False
    with patch.object(GitRepo, 'base_clone_command') as mock_repo_clone, \
            patch('intranet.dogma.dogma.core.tasks.create_commits'), \
            patch.object(GitRepo, 'commits_in_default_branch') as mock_repo_commits:
        mock_repo_commits.return_value = 5
        with override_settings(IS_BUSINESS=True):
            tasks.clone_repo(repo_fail.id, force=True)
        assert mock_repo_clone.called is True

    repo_fail.refresh_from_db()
    github_credential.refresh_from_db()
    assert repo_fail.clones.all()
    assert repo_fail.status == repo_fail.SYNC_STATUSES.success
    assert github_credential.is_success is True


def test_clone_repo_with_org_credential_success(repo_fail, github_org,
                                                github_credential, org,
                                                ):
    github_org.credentials.add(github_credential)
    repo_fail.organisation = github_org
    repo_fail.save()

    assert github_credential.is_success is False
    with patch.object(GitRepo, 'base_clone_command') as mock_repo_clone, \
            patch('intranet.dogma.dogma.core.tasks.create_commits'), \
            patch.object(GitRepo, 'commits_in_default_branch') as mock_repo_commits:
        mock_repo_commits.return_value = 5
        with override_settings(IS_BUSINESS=True):
            tasks.clone_repo(repo_fail.id, force=True)
        assert mock_repo_clone.called is True

    repo_fail.refresh_from_db()
    github_credential.refresh_from_db()
    assert repo_fail.clones.all()
    assert repo_fail.status == repo_fail.SYNC_STATUSES.success
    assert github_credential.is_success is True


def test_clone_repo_success(repo_success):
    repo_success.clone_attempt = 10
    repo_success.sync_delay = 24
    repo_success.save()
    with patch.object(SvnRepo, 'clone') as mock_repo_clone,\
            patch('intranet.dogma.dogma.core.tasks.create_commits'), \
            patch.object(SvnRepo, 'commits_in_default_branch') as mock_repo_commits:
        mock_repo_commits.return_value = 5
        tasks.clone_repo(repo_success.id, force=True)
        assert mock_repo_clone.called is True

    repo_success.refresh_from_db()
    assert repo_success.clones.all()
    assert repo_success.status == repo_success.SYNC_STATUSES.success
    assert repo_success.last_sync_success_time is not None
    assert repo_success.last_sync_fail_time is None
    assert repo_success.last_sync_fail_trace is None
    assert repo_success.clones.first().commits_count == 5
    assert repo_success.clone_attempt == 1
    assert repo_success.sync_delay == MIN_SYNC_DELAY


def test_fetch_clone_fail(clone_fail):
    with patch('intranet.dogma.dogma.core.tasks.locked_context'),\
         patch.object(
             GitRepo,
             'fetch',
             side_effect=OSError('OSError: [Errno 2] No such file or directory'),
         ) as mock_repo_fetch:
        tasks.fetch_clone(clone_fail.id)
        assert mock_repo_fetch.called is True

    repo = clone_fail.repo
    repo.refresh_from_db()
    assert repo.status == repo.SYNC_STATUSES.fail
    assert repo.last_sync_success_time is None
    assert repo.last_sync_fail_time is not None
    assert repo.last_sync_fail_trace is not None
    assert 'No such file or directory' in repo.last_sync_fail_trace
    assert repo.last_sync_fail_error_code == NoRepositoryOnNodeError.db_value
    assert repo.error_value == NoRepositoryOnNodeError.help


def test_fetch_clone_success(clone_success):
    with patch('intranet.dogma.dogma.core.tasks.locked_context'), \
         patch('intranet.dogma.dogma.core.tasks.create_commits'), \
         patch.object(SvnRepo, 'fetch') as mock_repo_fetch:
        tasks.fetch_clone(clone_success.id)
        assert mock_repo_fetch.called is True

    repo = clone_success.repo
    repo.refresh_from_db()
    assert repo.status == repo.SYNC_STATUSES.success
    assert repo.last_sync_success_time is not None
    assert repo.last_sync_fail_time is None
    assert repo.last_sync_fail_trace is None
    assert repo.last_sync_fail_error_code is None


def test_merge_commits_creation():
    repo, clone = model_repository()
    batch = hg_commit()[:5]
    repo_raw = hg_repo()
    guesser = EmailGuesser()
    commit_map = {}
    batch_diff = get_batch_diff(batch=batch, repo_raw=repo_raw)
    diff_data = get_diff_data_for_batch(batch_diff, {})
    changed_files_map = get_changed_files_map(batch_diff=batch_diff)
    tracker_data = {comm.hex: (tuple(), tuple()) for comm in batch}
    with patch('intranet.dogma.dogma.core.logic.users.get_user_data_by_email_from_staff') as get_user_mock:
        get_user_mock.return_value = None
        create_commits_objects(commits=batch, diff_data=diff_data,
                               commit_map=commit_map, repo_raw=repo_raw,
                               guesser=guesser, repository=repo,
                               tracker_data=tracker_data,
                               changed_files_map=changed_files_map,
                               )
    assert PushedCommit.objects.filter(repo=repo).count() == 5


def test_common_commits_creation():
    repo, clone = model_repository()
    batch = hg_commit()[:5]
    repo_raw = hg_repo()
    guesser = EmailGuesser()
    commit_map = {}
    batch_diff = get_batch_diff(batch=batch, repo_raw=repo_raw)
    diff_data = get_diff_data_for_batch(batch_diff, {})
    changed_files_map = get_changed_files_map(batch_diff=batch_diff)
    tracker_data = {comm.hex: (tuple(), tuple()) for comm in batch}
    with patch('intranet.dogma.dogma.core.logic.users.get_user_data_by_email_from_staff') as get_user_mock:
        get_user_mock.return_value = None
        create_commits_objects(commits=batch, diff_data=diff_data,
                               commit_map=commit_map, repo_raw=repo_raw,
                               guesser=guesser, repository=repo,
                               tracker_data=tracker_data,
                               changed_files_map=changed_files_map,
                               )
    assert PushedCommit.objects.filter(repo=repo).count() == 5


def test_validate_users(user, another_user, commit):
    assert User.objects.count() == 2
    assert PushedCommit.objects.filter(author=another_user).count() == 1
    assert PushedCommit.objects.filter(author=user).count() == 0
    assert commit.author == another_user
    assert commit.committer == another_user
    tasks.validate_users()
    commit = PushedCommit.objects.get(id=commit.id)
    assert commit.author == user
    assert commit.committer == user
    assert PushedCommit.objects.filter(author=another_user).count() == 0
    assert PushedCommit.objects.filter(author=user).count() == 1
    assert User.objects.count() == 1


def test_update_files_changed_statistics_success(commit_with_author_with_uid,
                                                 another_changed_file, changed_file,
                                                 ):
    assert ChangedFile.objects.count() == 2
    assert PushedCommit.objects.filter(aggregated=True).count() == 0
    assert UserFileStatistics.objects.count() == 0
    update_files_changed_statistics(commit_with_author_with_uid.commit_time)
    assert UserFileStatistics.objects.filter(user=commit_with_author_with_uid.author).count() == 2
    assert PushedCommit.objects.filter(aggregated=True).count() == 1
    py_file = UserFileStatistics.objects.get(extension=changed_file.extension)
    txt_file = UserFileStatistics.objects.get(extension=another_changed_file.extension)
    assert py_file.lines_added == changed_file.lines_added
    assert py_file.lines_deleted == changed_file.lines_deleted
    assert txt_file.lines_added == another_changed_file.lines_added
    assert txt_file.lines_deleted == another_changed_file.lines_deleted


def test_update_files_changed_statistics_with_existing_success(commit_with_author_with_uid,
                                                               another_changed_file, changed_file,
                                                               py_file_statistics,
                                                               ):
    assert ChangedFile.objects.count() == 2
    assert PushedCommit.objects.filter(aggregated=True).count() == 0
    assert UserFileStatistics.objects.count() == 1

    update_files_changed_statistics(commit_with_author_with_uid.commit_time)
    assert UserFileStatistics.objects.filter(user=commit_with_author_with_uid.author).count() == 2
    assert PushedCommit.objects.filter(aggregated=True).count() == 1
    py_file = UserFileStatistics.objects.get(extension=changed_file.extension)
    txt_file = UserFileStatistics.objects.get(extension=another_changed_file.extension)
    assert py_file.lines_added == changed_file.lines_added + py_file_statistics.lines_added
    assert py_file.lines_deleted == changed_file.lines_deleted + py_file_statistics.lines_deleted
    assert txt_file.lines_added == another_changed_file.lines_added
    assert txt_file.lines_deleted == another_changed_file.lines_deleted


def test_update_files_changed_statistics_success_no_main_user(commit, changed_file_author_without_uid,
                                                              caplog,
                                                              ):
    caplog.set_level(logging.INFO)
    assert ChangedFile.objects.count() == 1
    assert UserFileStatistics.objects.count() == 0
    update_files_changed_statistics(commit.commit_time)
    assert UserFileStatistics.objects.count() == 0
    assert 'No main user found, skipping' in caplog.text
