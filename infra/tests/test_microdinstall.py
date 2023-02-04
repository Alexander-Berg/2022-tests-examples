import pytest
import copy
import yatest.common

from infra.dist.cacus.lib.utils.microdinstall import change_file

SHA256 = {
    'cacus_0.5.11.7.dsc': ('f3714ce7b7d64dd291f73b8aff5935c7e9ee1dba68540cb341debc51728c7653', 1338),
    'cacus_0.5.11.7.tar.xz': ('ceedab761ee3c56e206266db99f2f6e3b9cf47e78899ef5a424931d379b78ed3', 81420),
    'cacus_0.5.11.7_all.deb': ('01403f11dc66732e92a0edc942cfa0597457d2f2911448d3be2ffc176524a600', 6912764),
    'cacus_0.5.11.7_amd64.buildinfo': ('56c663406510eb53db24d22ba96fae8838e69fec35e95a442549e6673880060b', 7457),
}


@pytest.fixture
def changes_file():
    changes = change_file.ChangeFile()
    changes.load_from_file(yatest.common.source_path('infra/dist/cacus/tests/test.changes'))
    return changes


def test_microdinstall_parse_changes(changes_file):
    assert changes_file['Version'] == '0.5.11.7'
    assert changes_file['Source'] == 'cacus'
    assert changes_file['Distribution'] == 'unstable'
    assert changes_file['Changed-By'] == 'Anton Suvorov <warwish@yandex-team.ru>'


def test_microdinstall_contains(changes_file):
    assert 'changed-by' in changes_file
    assert 'ChAnGed-By' in changes_file


def test_microdinstall_get_files(changes_file):
    expected = [
        ('4d26d604f1c1aa3d7da6bd1f286786a9', 1338, 'cacus_0.5.11.7.dsc'),
        ('ce4ffd8c0a951e64aab02f0dd1a8d0e9', 81420, 'cacus_0.5.11.7.tar.xz'),
        ('1471fcda4b399fcb86945d65ee1b25aa', 6912764, 'cacus_0.5.11.7_all.deb'),
        ('35d49c91655d515830b345c13fe5bf85', 7457, 'cacus_0.5.11.7_amd64.buildinfo'),
    ]
    assert changes_file.get_files() == expected


def test_microdinstall_verify(changes_file):
    def _hash_func_ok(f):
        f = f.split('/dev/null/')[1]
        return SHA256[f]
    changes_file.verify('/dev/null', _hash_func_ok)
    damaged_sha256 = copy.deepcopy(SHA256)
    damaged_sha256['cacus_0.5.11.7.dsc'] = ('sha100500', 1338)

    def _hash_func_damaged_sha256(f):
        f = f.split('/dev/null/')[1]
        return damaged_sha256[f]
    with pytest.raises(change_file.ChangeFileException, match=r'sha256 differs'):
        changes_file.verify('/dev/null', _hash_func_damaged_sha256)

    damaged_size = copy.deepcopy(SHA256)
    damaged_size['cacus_0.5.11.7.dsc'] = ('f3714ce7b7d64dd291f73b8aff5935c7e9ee1dba68540cb341debc51728c7653', 1337)

    def _hash_func_damaged_size(f):
        f = f.split('/dev/null/')[1]
        return damaged_size[f]
    with pytest.raises(change_file.ChangeFileException, match=r'size differs'):
        changes_file.verify('/dev/null', _hash_func_damaged_size)

    def _hash_func_oserror(f):
        raise OSError('cannot open: {}'.format(f))
    with pytest.raises(change_file.ChangeFileException, match=r'error opening file'):
        changes_file.verify('/dev/null', _hash_func_oserror)
