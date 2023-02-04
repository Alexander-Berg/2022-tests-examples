import copy
import pytest
from infra.dist.cacus.lib.dbal import mongo_connection
from infra.dist.cacus.lib.dbal import package_index
from infra.dist.cacus.lib.dbal import package_index_history

INDEX1 = {
    "architecture": "amd64",
    "environment": "unstable",
    "size": 51266,
    "packages_file": "2493082/cacus/unstable/amd64/Packages_4c4316d82382e299610c39d9b8e11895",
    "gziped_packages": "2365016/cacus/unstable/amd64/Packages.gz_9747a570d2b19f3ad17ac4cf704fcdb4",
    "sha1": "uMoSoCv43FAyVSSk+OF9HrcGWvA=",
    "release_gpg": "rel.gpg",
    "bziped_packages": "2758044/cacus/unstable/amd64/Packages.bz2_b1aa8e314842de3492e44086796550a2",
    "sha256": "9+NdYMVKnODXAVXc3DCc6bt6YIIw229XhZPPQieioyI=",
    "release_file": "rel_file",
    "lastupdated": "2020-07-27T15:17:22.300Z",
    "md5": "TEMW2COC4plhDDnZuOEYlQ==",
    "dirty_bit": False,
    "dirty_bit_set_at": "2020-07-27T15:17:18.701Z",
    "force_index": False,
    "in_release_file": "in_rel_file",
    "release_gpg_by_hash": "rel_gpg_hash",
    "gzipped_sha256": "08da2311afb3f308162684db698a43e9df7490b54ccb38d1d45f735f55e2f940",
    "bzipped_sha256": "7c2c45b56ea552c4e3cad1c7e8cbac7610539666f729302a3aa54f3946fc6972",
    "in_release_gpg_by_hash": "in_rel_gpg_hash",
    "release_by_hash": "rel_hash",
    "plain_sha256": "f7e35d60c54a9ce0d70155dcdc309ce9bb7a608230db6f578593cf4227a2a322"
}
INDEX2 = {
    "architecture": "source",
    "environment": "unstable",
    "dirty_bit": False,
    "bziped_sources": "2758442/cacus/unstable/source/Sources.bz2_7785c5438fe648f317cba5acbca4199e",
    "release_file": "rel_file",
    "lastupdated": "2020-07-27T15:17:24.749Z",
    "sources_file": "1478014/cacus/unstable/source/Sources_f3926c0a5ed0954cdd7620472db1b8e3",
    "release_gpg": "rel.gpg",
    "gziped_sources": "1220126/cacus/unstable/source/Sources.gz_706ca02302cbb333019cac0c70d3c10d",
    "dirty_bit_set_at": "2020-07-27T15:17:22.445Z",
    "in_release_file": "in_rel_file",
    "force_index": True,
    "bzipped_sha256": "ed4819e9a9ad26512690bf790d14607c7cf943ef1171d226ebb9f7a563f69427",
    "release_by_hash": "rel_hash",
    "gzipped_sha256": "d57e47a33f659708a786b23b9045033b53e2570d043122118f319b3fc09fcb0b",
    "release_gpg_by_hash": "rel_gpg_hash",
    "plain_sha256": "de532a2f2f93337285c9082b5a0be8d27aa3f529b7e4f18b122dc26b392707af",
    "in_release_gpg_by_hash": "in_rel_gpg_hash",
}


@pytest.fixture
def mock_db():
    mongo_connection.cacus()['cacus'].insert_many(copy.deepcopy([INDEX1, INDEX2]))


def drop_none(d):
    return {k: v for k, v in d.items() if v is not None}


def test_from_dict_sources():
    s = package_index.PackageIndex.from_dict('cacus', INDEX2)
    assert s.arch == INDEX2['architecture']
    assert s.env == INDEX2['environment']
    assert s.dirty_bit == INDEX2['dirty_bit']
    assert s.bzipped == INDEX2['bziped_sources']
    assert s.release_file == INDEX2['release_file']
    assert s.lastupdated == INDEX2['lastupdated']
    assert s.plain == INDEX2['sources_file']
    assert s.release_gpg == INDEX2['release_gpg']
    assert s.gzipped == INDEX2['gziped_sources']
    assert s.dirty_bit_set_at == INDEX2['dirty_bit_set_at']
    assert s.in_release_file == INDEX2['in_release_file']
    assert s.force_index == INDEX2['force_index']
    assert s.bzipped_sha256 == INDEX2['bzipped_sha256']
    assert s.release_by_hash == INDEX2['release_by_hash']
    assert s.gzipped_sha256 == INDEX2['gzipped_sha256']
    assert s.release_gpg_by_hash == INDEX2['release_gpg_by_hash']
    assert s.plain_sha256 == INDEX2['plain_sha256']
    assert s.in_release_gpg_by_hash == INDEX2['in_release_gpg_by_hash']
    assert s.md5 is None
    assert s.sha1 is None
    assert s.sha256 is None
    assert s.size is None
    assert s.repo == 'cacus'


def test_from_dict_packages():
    s = package_index.PackageIndex.from_dict('cacus', INDEX1)
    assert s.arch == INDEX1['architecture']
    assert s.env == INDEX1['environment']
    assert s.size == INDEX1['size']
    assert s.plain == INDEX1['packages_file']
    assert s.gzipped == INDEX1['gziped_packages']
    assert s.sha1 == INDEX1['sha1']
    assert s.release_gpg == INDEX1['release_gpg']
    assert s.bzipped == INDEX1['bziped_packages']
    assert s.sha256 == INDEX1['sha256']
    assert s.release_file == INDEX1['release_file']
    assert s.lastupdated == INDEX1['lastupdated']
    assert s.md5 == INDEX1['md5']
    assert s.dirty_bit == INDEX1['dirty_bit']
    assert s.dirty_bit_set_at == INDEX1['dirty_bit_set_at']
    assert s.force_index == INDEX1['force_index']
    assert s.in_release_file == INDEX1['in_release_file']
    assert s.release_gpg_by_hash == INDEX1['release_gpg_by_hash']
    assert s.gzipped_sha256 == INDEX1['gzipped_sha256']
    assert s.bzipped_sha256 == INDEX1['bzipped_sha256']
    assert s.in_release_gpg_by_hash == INDEX1['in_release_gpg_by_hash']
    assert s.release_by_hash == INDEX1['release_by_hash']
    assert s.plain_sha256 == INDEX1['plain_sha256']
    assert s.repo == 'cacus'


def test_to_dict_sources():
    s = package_index.PackageIndex.from_dict('cacus', INDEX2)
    result = drop_none(s.to_dict())
    assert result == INDEX2


def test_to_dict_packages():
    s = package_index.PackageIndex.from_dict('cacus', INDEX1)
    assert s.to_dict() == INDEX1


def test_from_index():
    s = package_index.PackageIndex.from_dict('cacus', INDEX1)
    new = package_index.PackageIndex.from_index(s)
    assert new.to_dict() == INDEX1
    assert new.repo == 'cacus'


def test_find_query():
    expected = {'environment': 'cacus', 'architecture': 'amd64'}
    assert package_index.PackageIndexStore._find_query('cacus', 'amd64', None) == expected
    expected['dirty_bit'] = False
    assert package_index.PackageIndexStore._find_query('cacus', 'amd64', False) == expected
    expected['dirty_bit'] = True
    assert package_index.PackageIndexStore._find_query('cacus', 'amd64', True) == expected


@pytest.mark.usefixtures("mock_db")
def test_all():
    indices = package_index.default_store.all('cacus')
    assert len(indices) == 2
    assert isinstance(indices[0], package_index.PackageIndex) and isinstance(indices[1], package_index.PackageIndex)
    indices = package_index.default_store.all('non-existent')
    assert len(indices) == 0


@pytest.mark.usefixtures("mock_db")
def test_find_one():
    idx = package_index.default_store.find_one('cacus', 'unstable', 'amd64')
    assert idx.to_dict() == INDEX1


@pytest.mark.usefixtures("mock_db")
def test_set_dirty():
    package_index.default_store.set_dirty('cacus', 'unstable', 'source')
    p = package_index.default_store.find_one('cacus', 'unstable', 'source')
    assert p.dirty_bit is True


@pytest.mark.usefixtures("mock_db")
def test_set_clean():
    package_index.default_store.set_clean('cacus', 'unstable', 'amd64')
    p = package_index.default_store.find_one('cacus', 'unstable', 'amd64')
    assert p.dirty_bit is False


@pytest.mark.usefixtures("mock_db")
def test_save():
    p = package_index.default_store.find_one('cacus', 'unstable', 'amd64')
    p.sha256 = 'sha265'
    package_index.default_store.save(p)
    p = package_index.default_store.find_one('cacus', 'unstable', 'amd64')
    assert p.sha256 == 'sha265'
    assert p.to_dict() != INDEX1


@pytest.mark.usefixtures("mock_db")
def test_save_with_history():
    p = package_index.default_store.find_one('cacus', 'unstable', 'amd64')
    p.plain_sha256 = 'psha256'
    p.gzipped_sha256 = 'gsha256'
    p.bzipped_sha256 = 'bsha256'
    package_index.default_store.save(p)
    p.plain_sha256 = 'psha512'
    p.gzipped_sha256 = 'gsha512'
    p.bzipped_sha256 = 'bsha512'
    package_index.default_store.save(p)
    r = [package_index_history.PackageIndexHistoryEntry.from_dict('cacus', x) for x in
         mongo_connection.history()['cacus'].find(
             package_index_history.PackageIndexHistoryStore._find_query('unstable', 'amd64'))]
    assert len(r) == 6


@pytest.mark.usefixtures("mock_db")
def test_delete():
    p = package_index.default_store.find_one('cacus', 'unstable', 'amd64')
    package_index.default_store.delete(p)
    assert len(package_index.default_store.all('cacus')) == 1


@pytest.mark.usefixtures("mock_db")
def test_drop():
    package_index.default_store.drop('cacus')
    assert len(package_index.default_store.all('cacus')) == 0
