import copy
import datetime
import mock

import pytest
from infra.dist.cacus.lib import loader
from infra.dist.cacus.lib.dbal import mongo_connection
from infra.dist.cacus.lib.dbal import package_index
from infra.dist.cacus.lib.dbal import package_index_history

REPO = 'cacus'
ENTRY1 = {
    "architecture": "amd64",
    "environment": "unstable",
    "storage_key": "2493082/cacus/unstable/amd64/Packages_4c4316d82382e299610c39d9b8e11895",
    "sha256": "f7e35d60c54a9ce0d70155dcdc309ce9bb7a608230db6f578593cf4227a2a322",
    "valid_before": datetime.datetime.utcnow() + datetime.timedelta(seconds=600),
    "updated_at": "2020-07-27T15:17:22.300Z",
}
ENTRY2 = {
    "architecture": "amd64",
    "environment": "unstable",
    "valid_before": datetime.datetime.utcnow() - datetime.timedelta(seconds=600),
    "storage_key": "2365016/cacus/unstable/amd64/Packages.gz_9747a570d2b19f3ad17ac4cf704fcdb4",
    "sha256": "08da2311afb3f308162684db698a43e9df7490b54ccb38d1d45f735f55e2f940",
    "updated_at": "2020-07-27T15:17:22.300Z",
}
ENTRY3 = {
    "architecture": "amd64",
    "environment": "unstable",
    "valid_before": datetime.datetime.utcnow() + datetime.timedelta(seconds=600),
    "storage_key": "2758044/cacus/unstable/amd64/Packages.bz2_b1aa8e314842de3492e44086796550a2",
    "sha256": "7c2c45b56ea552c4e3cad1c7e8cbac7610539666f729302a3aa54f3946fc6972",
    "updated_at": "2020-07-27T15:17:22.300Z",
}
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
    "architecture": "amd64",
    "environment": "unstable",
    "dirty_bit": False,
    "bziped_packages": "2758442/cacus/unstable/source/Sources.bz2_7785c5438fe648f317cba5acbca4199e",
    "release_file": "rel_file",
    "lastupdated": "2020-07-27T15:17:24.749Z",
    "packages_file": "1478014/cacus/unstable/source/Sources_f3926c0a5ed0954cdd7620472db1b8e3",
    "release_gpg": "rel.gpg",
    "gziped_packages": "1220126/cacus/unstable/source/Sources.gz_706ca02302cbb333019cac0c70d3c10d",
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


EMPTY_ENTRY = {
    "architecture": "amd64",
    "environment": "unstable",
    "valid_before": datetime.datetime.utcnow() - datetime.timedelta(seconds=600),
    "storage_key": "<empty>",
    "sha256": "08da2311afb3f308162684db698a43e9df7490b54ccb38d1d45f735f55e2f940",
    "updated_at": "2020-07-27T15:17:22.300Z",
}


@pytest.fixture
def mock_db():
    mongo_connection.history()[REPO].insert_many(copy.deepcopy([ENTRY1, ENTRY2, ENTRY3]))


def test_from_dict():
    h = package_index_history.PackageIndexHistoryEntry.from_dict(REPO, ENTRY1)
    assert h.sha256 == ENTRY1['sha256']
    assert h.arch == ENTRY1['architecture']
    assert h.env == ENTRY1['environment']
    assert h.valid_before == ENTRY1['valid_before']
    assert h.storage_key == ENTRY1['storage_key']
    assert h.updated_at == ENTRY1['updated_at']
    assert h.repo == REPO


def test_to_dict():
    h = package_index_history.PackageIndexHistoryEntry.from_dict(REPO, ENTRY1)
    assert h.to_dict() == ENTRY1


def test_from_index():
    idx = package_index.PackageIndex.from_dict(REPO, INDEX1)
    h = package_index_history.PackageIndexHistoryEntry.from_index(idx)
    assert len(h) == 3
    assert h[0].storage_key == idx.plain
    assert h[1].storage_key == idx.gzipped
    assert h[2].storage_key == idx.bzipped
    assert h[0].sha256 == idx.plain_sha256
    assert h[1].sha256 == idx.gzipped_sha256
    assert h[2].sha256 == idx.bzipped_sha256
    assert h[0].repo == REPO
    assert h[1].repo == REPO
    assert h[2].repo == REPO
    assert h[0].env == idx.env
    assert h[1].env == idx.env
    assert h[2].env == idx.env
    assert h[0].arch == idx.arch
    assert h[1].arch == idx.arch
    assert h[2].arch == idx.arch
    assert h[0].valid_before is None
    assert h[1].valid_before is None
    assert h[2].valid_before is None
    assert h[0].updated_at == idx.lastupdated
    assert h[1].updated_at == idx.lastupdated
    assert h[2].updated_at == idx.lastupdated


def test_find_query():
    q = {'environment': 'env'}
    assert q == package_index_history.PackageIndexHistoryStore._find_query('env')
    q['architecture'] = 'arch'
    assert q == package_index_history.PackageIndexHistoryStore._find_query('env', 'arch')
    q['valid_before'] = 'validity'
    assert q == package_index_history.PackageIndexHistoryStore._find_query('env', 'arch', 'validity')
    q['sha256'] = 'sha256'
    assert q == package_index_history.PackageIndexHistoryStore._find_query('env', 'arch', 'validity', 'sha256')


@pytest.mark.usefixtures("mock_db")
def test_save():
    h = package_index_history.PackageIndexHistoryEntry('repo', 'env', 'arch', 'sha256', 'storage', 'validity', 'updated')
    package_index_history.default_store.save(h)
    r = mongo_connection.history()['repo'].find_one({'environment': 'env', 'architecture': 'arch', 'sha256': 'sha256'})
    assert h.to_dict() == package_index_history.PackageIndexHistoryEntry.from_dict('repo', r).to_dict()

    h.storage_key = 's-key'
    package_index_history.default_store.save(h)
    r = mongo_connection.history()['repo'].find_one({'environment': 'env', 'architecture': 'arch', 'sha256': 'sha256'})
    assert h.to_dict() == package_index_history.PackageIndexHistoryEntry.from_dict('repo', r).to_dict()

    h.env = 'env2'
    package_index_history.default_store.save(h)
    r = mongo_connection.history()['repo'].find_one({'environment': 'env2', 'architecture': 'arch', 'sha256': 'sha256'})
    assert h.to_dict() == package_index_history.PackageIndexHistoryEntry.from_dict('repo', r).to_dict()

    indexes = [x['name'] for x in mongo_connection.history()['repo'].list_indexes()]
    assert 'env-arch' in indexes
    assert 'env-arch-sha256' in indexes
    assert 'env-arch-validity' in indexes


@pytest.mark.usefixtures("mock_db")
def test_cleanup(monkeypatch):
    mds_mock = mock.Mock()
    mds_mock.delete = mock.Mock(return_value=None)
    monkeypatch.setattr(loader, 'get_plugin', lambda *args, **kwargs: mds_mock)
    now = datetime.datetime.utcnow()
    package_index_history.default_store.cleanup(REPO, 'unstable', 'amd64')
    r = [package_index_history.PackageIndexHistoryEntry.from_dict(REPO, x) for x in
         mongo_connection.history()[REPO].find(
             package_index_history.PackageIndexHistoryStore._find_query('unstable', 'amd64'))]
    assert len(r) == 2
    for i in r:
        assert i.valid_before > now
    mds_mock.delete.assert_called_with(ENTRY2['storage_key'])


@pytest.mark.usefixtures("mock_db")
def test_cleanup_skip_empty_mds_deletion(monkeypatch):
    mongo_connection.history()[REPO].insert_many(copy.deepcopy([EMPTY_ENTRY]))
    mds_mock = mock.Mock()
    mds_mock.delete = mock.Mock(return_value=None)
    monkeypatch.setattr(loader, 'get_plugin', lambda *args, **kwargs: mds_mock)
    now = datetime.datetime.utcnow()
    package_index_history.default_store.cleanup(REPO, 'unstable', 'amd64')
    r = [package_index_history.PackageIndexHistoryEntry.from_dict(REPO, x) for x in
         mongo_connection.history()[REPO].find(
             package_index_history.PackageIndexHistoryStore._find_query('unstable', 'amd64'))]
    assert len(r) == 2
    for i in r:
        assert i.valid_before > now
    mds_mock.delete.assert_called_once_with(ENTRY2['storage_key'])


def test_put_empty(monkeypatch):
    mds_mock = mock.Mock()
    mds_mock.delete = mock.Mock(return_value=None)
    monkeypatch.setattr(loader, 'get_plugin', lambda *args, **kwargs: mds_mock)
    idx = package_index.PackageIndex.from_dict(REPO, INDEX1)
    package_index_history.default_store.put(idx)
    r = [package_index_history.PackageIndexHistoryEntry.from_dict(REPO, x) for x in
         mongo_connection.history()[REPO].find(
             package_index_history.PackageIndexHistoryStore._find_query('unstable', 'amd64'))]
    assert len(r) == 3
    mds_mock.delete.assert_not_called()
    now = datetime.datetime.utcnow()
    for i in r:
        assert i.valid_before > now
        assert i.arch == 'amd64'
        assert i.env == 'unstable'
        assert i.repo == REPO
        assert i.sha256 in (INDEX1['plain_sha256'], INDEX1['gzipped_sha256'], INDEX1['bzipped_sha256'])
        assert i.storage_key in (INDEX1['packages_file'], INDEX1['gziped_packages'], INDEX1['bziped_packages'])


@pytest.mark.usefixtures("mock_db")
def test_put_with_cleanup(monkeypatch):
    mds_mock = mock.Mock()
    mds_mock.delete = mock.Mock(return_value=None)
    monkeypatch.setattr(loader, 'get_plugin', lambda *args, **kwargs: mds_mock)
    idx = package_index.PackageIndex.from_dict(REPO, INDEX2)
    package_index_history.default_store.put(idx)
    r = [package_index_history.PackageIndexHistoryEntry.from_dict(REPO, x) for x in
         mongo_connection.history()[REPO].find(
             package_index_history.PackageIndexHistoryStore._find_query('unstable', 'amd64'))]
    assert len(r) == 5
    mds_mock.delete.assert_called_with(ENTRY2['storage_key'])
    now = datetime.datetime.utcnow()
    for i in r:
        assert i.valid_before > now
        assert i.arch == 'amd64'
        assert i.env == 'unstable'
        assert i.repo == REPO
        assert i.sha256 in (INDEX1['plain_sha256'], INDEX1['bzipped_sha256'],
                            INDEX2['plain_sha256'], INDEX2['gzipped_sha256'], INDEX2['bzipped_sha256'],)
        assert i.storage_key in (INDEX1['packages_file'], INDEX1['bziped_packages'],
                                 INDEX2['packages_file'], INDEX2['gziped_packages'], INDEX2['bziped_packages'])


@pytest.mark.usefixtures("mock_db")
def test_cleanup_with_protected_hashes(monkeypatch):
    mds_mock = mock.Mock()
    mds_mock.delete = mock.Mock(return_value=None)
    monkeypatch.setattr(loader, 'get_plugin', lambda *args, **kwargs: mds_mock)
    idx = package_index.PackageIndex.from_dict(REPO, INDEX1)
    package_index_history.default_store.cleanup(idx.repo, idx.env, idx.arch, (idx.bzipped_sha256, idx.gzipped_sha256, idx.plain_sha256))
    r = [package_index_history.PackageIndexHistoryEntry.from_dict(REPO, x) for x in
         mongo_connection.history()[REPO].find(
             package_index_history.PackageIndexHistoryStore._find_query('unstable', 'amd64'))]
    assert len(r) == 2
    mds_mock.delete.assert_not_called()
