# coding: utf-8

import pytest

from mock import patch
from django.test.utils import override_settings
from dir_data_sync.models import Organization, OperatingMode

from intranet.dogma.dogma.core.utils import get_current_node
from intranet.dogma.dogma.core.models import Source, Repo, PushedCommit, Clone, User
from ...utils import test_vcr
from intranet.dogma.dogma.core.tasks import update_source, fetch_clone, clone_repo
from intranet.dogma.dogma.core.logic.users import EmailGuesser

pytestmark = pytest.mark.django_db(transaction=True)


@pytest.fixture
def gerrit_source(transactional_db,):
    return Source.objects.create(
        vcs_type='gerrit', web_type='gerrit',
        host='testhost', web_url='https://gerrit.yandex-team.ru',
        status='success',
    )


@pytest.fixture
def gerrit_repo(gerrit_source):
    return Repo.objects.create(
        source=gerrit_source, owner='yandex-phone',
        name='yandex-phone/device/yandex',
        vcs_name='yandex-phone/device/yandex',
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
def gerrit_clone_fail(transactional_db, gerrit_repo):
    return Clone.objects.create(
        repo=gerrit_repo,
        path='my/path',
        node=get_current_node(create_missing=True),
        status=Clone.STATUSES.fail,
        modified='2016-07-20 21:02:00',
    )


@pytest.fixture
def gerrit_user(transactional_db,):
    return User.objects.create(
        login='smosker',
        email='test_email@ya.ru',
        name='Anotherone',
    )


@pytest.fixture
def gerrit_commit(transactional_db, gerrit_repo, gerrit_user):
    return PushedCommit.objects.create(
        repo=gerrit_repo,
        author=gerrit_user,
        committer=gerrit_user,
        commit='Ib602700dc9103241fd420d5f4e8903b15eac658d_1989',
        commit_time='2019-04-01T07:15:30-03:00',
        lines_added=0,
        lines_deleted=0,
        message='Revert "PHONE-4641: update default wallpaper"',
        branch_name='master',
        tree_hex='',
        commit_id='1989',
        create_changed_files=True,
    )


def _mock_get_tracker_data(batch):
    return {
        commit['change_id']: (None, None)
        for commit in batch
    }


def test_update_source_success(gerrit_source, org):
    assert gerrit_source.status == 'success'
    assert Repo.objects.filter(source=gerrit_source).count() == 0
    with override_settings(GERRIT_IN_BATCH=10):
        with test_vcr.use_cassette('gerrit_fetch_repos_success.yaml'):
            update_source(gerrit_source.id)

    assert Repo.objects.filter(source=gerrit_source).count() == 10
    gerrit_source.refresh_from_db()
    assert gerrit_source.status == 'success'


def test_update_source_fail(gerrit_source, org):
    assert gerrit_source.status == 'success'
    assert Repo.objects.filter(source=gerrit_source).count() == 0
    with override_settings(GERRIT_IN_BATCH=10):
        with test_vcr.use_cassette('gerrit_fetch_repos_fail.yaml'):
            update_source(gerrit_source.id)

    assert Repo.objects.filter(source=gerrit_source).count() == 0
    gerrit_source.refresh_from_db()
    assert gerrit_source.status == 'fail'


def test_clone_repo_success(gerrit_repo, org):
    assert Clone.objects.count() == 0
    assert PushedCommit.objects.filter(repo=gerrit_repo).count() == 0
    with override_settings(GERRIT_IN_BATCH=10):
        with patch.object(EmailGuesser, 'guess_user_by') as mock_guess_user:
            with patch('intranet.dogma.dogma.core.backends.gerrit.get_tracker_data', side_effect=_mock_get_tracker_data):
                with test_vcr.use_cassette('gerrit_fetch_commits_success.yaml'):
                    mock_guess_user.return_value = (None, None)
                    clone_repo(gerrit_repo.id)
    assert PushedCommit.objects.filter(repo=gerrit_repo).count() == 10
    assert Clone.objects.count() == 1
    clone = Clone.objects.first()
    assert clone.repo == gerrit_repo
    assert clone.status == 'active'
    gerrit_repo.refresh_from_db()
    assert gerrit_repo.status == 'success'


def test_fetch_clone_success(gerrit_clone_fail, org):
    assert PushedCommit.objects.filter(repo=gerrit_clone_fail.repo_id).count() == 0
    with override_settings(GERRIT_IN_BATCH=10):
        with patch.object(EmailGuesser, 'guess_user_by') as mock_guess_user:
            with patch('intranet.dogma.dogma.core.backends.gerrit.get_tracker_data', side_effect=_mock_get_tracker_data):
                with test_vcr.use_cassette('gerrit_fetch_commits_success.yaml'):
                    mock_guess_user.return_value = (None, None)
                    fetch_clone(gerrit_clone_fail.id)
    assert PushedCommit.objects.filter(repo=gerrit_clone_fail.repo_id).count() == 10
    repo = Repo.objects.get(pk=gerrit_clone_fail.repo_id)
    assert repo.status == 'success'
    gerrit_clone_fail.refresh_from_db()
    assert gerrit_clone_fail.status == 'active'
    commit = PushedCommit.objects.get(commit='Ib602700dc9103241fd420d5f4e8903b15eac658d_1988')
    assert commit.message == 'Revert "PHONE-4641: update default wallpaper"'
    assert commit.commit_id == '1988'
    assert commit.author.email == 'evve@yandex-team.ru'


def test_fetch_clone_with_existed_commits_success(gerrit_clone_fail, org, gerrit_commit):
    assert PushedCommit.objects.filter(repo=gerrit_clone_fail.repo_id).count() == 1
    with override_settings(GERRIT_IN_BATCH=10):
        with patch.object(EmailGuesser, 'guess_user_by') as mock_guess_user:
            with patch('intranet.dogma.dogma.core.backends.gerrit.get_tracker_data', side_effect=_mock_get_tracker_data):
                with test_vcr.use_cassette('gerrit_fetch_with_existed_commits_success.yaml'):
                    mock_guess_user.return_value = (None, None)
                    fetch_clone(gerrit_clone_fail.id)
    assert PushedCommit.objects.filter(repo=gerrit_clone_fail.repo_id).count() == 2
    repo = Repo.objects.get(pk=gerrit_clone_fail.repo_id)
    assert repo.status == 'success'
    gerrit_clone_fail.refresh_from_db()
    assert gerrit_clone_fail.status == 'active'
