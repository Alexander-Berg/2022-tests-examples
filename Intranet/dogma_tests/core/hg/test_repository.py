# coding: utf-8

import pytest
from os import path

from .utils import HG_ROOT
from intranet.dogma.dogma.core.hg.models import Repository, dtfromts

pytestmark = pytest.mark.django_db(transaction=True)


def hg_repo():
    return Repository.discover(HG_ROOT)


def test_it_lists_branches():
    repo = hg_repo()
    assert [b'AUTO-8759', b'default', b'g1', b'yoctodb-0.0.13'] == repo.list_branches()
    base_path = path.abspath('.')  # корень проекта!
    assert base_path + '/hg/auto/.hg' == repo.path.decode('utf-8')
    assert base_path + '/hg/auto' == repo.workdir.decode('utf-8')


def test_it_returns_branch_head():
    repo = hg_repo()
    commit = repo.branch_head(b'default')
    assert commit.author.name == 'Svyatoslav Demidov'
    assert commit.committer.name == 'Svyatoslav Demidov'
    assert commit.message == 'released auto2-wizard'
    assert commit.commit_time == dtfromts(1467628870)  # 2016, 7, 4, 10, 41, 10
    assert commit.parent_ids == ["b'ab983f65566352121695a8e391fb40b281a801b7'"]


def test_it_returns_branch():
    repo = hg_repo()
    branch = repo.get_branch(b'default')
    assert branch.name == b'default'
    assert branch.target == b'c6af10aa9840c6123b574a574d620e64aa434f4b'


def test_it_returns_diff():
    repo = hg_repo()
    commit = repo.branch_head(b'default')
    diff = repo.commit_diff(commit)
    assert diff.stats == (12, 0)  # просто хоть что-нибудь проверить


def test_walker():
    repo = hg_repo()
    commit = repo.branch_head(b'default')
    walker = list(repo.walk(commit.hex, sort_type='tm'))
    assert len(walker) == 29831
    assert walker[0].hex == commit.hex
    assert walker[1].hex == b'ab983f65566352121695a8e391fb40b281a801b7'

    walker = list(repo.walk(b'ab983f65566352121695a8e391fb40b281a801b7', sort_type='tm'))
    assert walker[0].hex == b'ab983f65566352121695a8e391fb40b281a801b7'
    assert walker[-1].hex == b'40ef6c80f0079b18109901a93022ac45949bde33'
    assert len(walker) == 29830

    walker = list(repo.walk(b'ab983f65566352121695a8e391fb40b281a801b7', sort_type='tm+rv'))
    assert walker[0].hex == b'40ef6c80f0079b18109901a93022ac45949bde33'
    assert walker[-1].hex == b'ab983f65566352121695a8e391fb40b281a801b7'
    assert len(walker) == 29830
