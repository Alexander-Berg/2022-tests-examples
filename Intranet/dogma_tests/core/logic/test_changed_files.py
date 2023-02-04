# coding: utf-8



from intranet.dogma.dogma.core.logic.changed_file import get_changed_files_map
from ..hg.utils import HG_ROOT
from intranet.dogma.dogma.core.hg.models import Repository, Diff
from intranet.dogma.dogma.core.logic.commits import get_batch_diff
from intranet.dogma.dogma.core.abstract_repository import ChangedFilesMixin


def test_create_changed_files_map_success():
    repo = Repository.discover(HG_ROOT)
    commit = repo.branch_head(b'default')
    diff_batch = get_batch_diff([commit], repo)
    assert diff_batch == {
        b'c6af10aa9840c6123b574a574d620e64aa434f4b':
            Diff((commit._initial, commit._initial.parents()[0])),
    }
    changed_files_map = get_changed_files_map(diff_batch)
    assert len(changed_files_map) == 1
    changed_files_for_commit = changed_files_map[b'c6af10aa9840c6123b574a574d620e64aa434f4b']
    assert len(changed_files_for_commit) == 2
    assert [
        (
            changed_file_name, changed_file_data['extension'],
            changed_file_data['additions'], changed_file_data['deletions'],
        ) for changed_file_name, changed_file_data in changed_files_for_commit.items()
    ] == [
        ('a/auto-shard/debian/changelog', 'changelog', 6, 0),
        ('a/auto-wizard/debian/changelog', 'changelog', 6, 0),
    ]


def test_get_extension():
    changed_files_mixin_intance = ChangedFilesMixin()
    extension_map = {
        'something.py': 'py',
        '/test.txt': 'txt',
        'a/something.sh': 'sh',
        '/path/to/some/file.cc': 'cc',
        'a/debian/changelog': 'changelog',
        'path/to/file/without/an/extension': 'unknown extension',
        'a/test/.gitignore': 'unknown extension',
        None: 'unknown extension',
        '': 'unknown extension',
        'yandex-search-yamo/yandex-search-yamo-1.0/debian/noconffiles': 'unknown extension',
    }
    for file_name, extension in extension_map.items():
        assert changed_files_mixin_intance.get_file_extension(file_name) == extension
