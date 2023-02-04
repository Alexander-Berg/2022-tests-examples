# coding: utf-8

import pytest


from .utils import HG_ROOT
from intranet.dogma.dogma.core.hg.models import Repository, Diff

pytestmark = pytest.mark.django_db(transaction=True)


def hg_diff():
    repo = Repository.discover(HG_ROOT)
    commit = repo.branch_head(b'default')._initial
    return Diff((commit, commit.parents()[0]))


def test_detailed_patches():
    diff = hg_diff()
    result = diff.detailed_patches()
    assert len(result.keys()) == 2
    assert 'auto-shard/debian/changelog' in result


def test_stats():
    diff = hg_diff()
    additions, deletions = diff.stats
    assert additions == 12
    assert deletions == 0


def test_changed_files():
    diff = hg_diff()
    changed_files = diff.changed_files
    assert changed_files == {
        'a/auto-shard/debian/changelog': {'additions': 6, 'deletions': 0,
                                          'extension': 'changelog'},
        'a/auto-wizard/debian/changelog': {'additions': 6, 'deletions': 0,
                                           'extension': 'changelog'},
    }


def test_patches():
    diff = hg_diff()
    patches = diff.patches
    assert len(patches) == 2
    assert patches[0].additions == 6
    assert patches[0].deletions == 0
    assert patches[0].is_binary is False
    assert patches[0].status == 'M'
    assert patches[0].status_name == 'modified'
    assert patches[0].old_file_path == 'auto-shard/debian/changelog'
    assert patches[0].new_file_path == 'auto-shard/debian/changelog'
    assert patches[0].old_hex == b'ab983f65566352121695a8e391fb40b281a801b7'
    assert patches[0].new_hex == b'c6af10aa9840c6123b574a574d620e64aa434f4b'
