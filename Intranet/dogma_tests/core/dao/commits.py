# coding: utf-8


import pytest

from intranet.dogma.dogma.core.models import User, PushedCommit, Repo, Source
from intranet.dogma.dogma.core.dao.commits import attach_commits_to_uid_by_emails

@pytest.fixture()
def github(transactional_db,):
    return Source.objects.create(
        name='Github Enterprise',
        code='github',
        vcs_type='git',
        web_type='github',
        web_url='https://github.example.com/',
        host='github.example.com',
    )

@pytest.fixture()
def repo(transactional_db, github):
    return Repo.objects.create(
        source=github,
        name='dogma',
        owner='tools',
        vcs_name='tools/dogma',
        description='Догма',
    )


@pytest.fixture
def user(transactional_db,):
    return User.objects.create(
        uid='12345',
        login='vsem_privet',
        email='test@ya.ru',
        name='Someone',
        other_emails='test@ya.ru,anotheremail@test.com'
    )


@pytest.fixture
def another_user(transactional_db,):
    return User.objects.create(
        uid='123456',
        login='smosker',
        email='smosker@ya.ru',
        name='Anotherone',
        other_emails='smosker@ya.ru, email@email.com'
    )


@pytest.fixture
def commit(transactional_db, repo, another_user):
    return PushedCommit.objects.create(
        repo=repo,
        author=another_user,
        committer=another_user,
        commit='edf1d67f432003d3d84d3d7b46af7fc518eec475',
        commit_time='1996-12-19T16:39:57-08:00',
        lines_added=43,
        lines_deleted=50,
        message='some message text',
        branch_name='master',
        tree_hex='ec13a4bd6500ab81a11b148d08a5a6983e58d665',
        commit_id='17092ba4edd863ada20009558b9b55e194ffbe56',
        tickets=['TEST-5'],
        queues=['TEST'],
    )


def test_attach_commits_to_uid_by_emails(user, another_user, commit, ):
    assert User.objects.count() == 2
    assert PushedCommit.objects.filter(author=user).count() == 0
    assert PushedCommit.objects.filter(author=another_user).count() == 1
    attach_commits_to_uid_by_emails(user.uid, another_user.email)
    assert PushedCommit.objects.filter(author=user).count() == 1
    assert PushedCommit.objects.filter(author=another_user).count() == 0
    assert User.objects.count() == 1
    assert User.objects.first().id == user.id
