import copy
import datetime

import pytest

from infra.dist.cacus.lib.dbal import errors
from infra.dist.cacus.lib.dbal import mongo_connection
from infra.dist.cacus.lib.dbal import package

PACKAGE = {
    "Source": "cacus",
    "audit_meta": [
        {
            "timestamp": "2020-07-17T11:19:53.433Z",
            "event": "upload",
            "user": "robot-admins"
        }
    ],
    "environment": "unstable",
    "sources": [
        {
            "size": 1256,
            "sha1": "6148mkCj8ux+mSLApajdpRaqYl4=",
            "name": "cacus_0.6.0-7121399_amd64.changes",
            "storage_key": "/storage/372274/cacus/cacus_0.6.0-7121399_amd64.changes",
            "sha256": "geejkcr7h/JLWT/aPh8fBBnYFMp95uEUjLlncievXlo=",
            "sha512": "s1RXVHmf17RiczU7jjifibaVGh9jw+CIGoj5oQUm9GHYdIump2279BEj90qvxxfzsTE5Cgs2eKiyY6kIkQFHaA==",
            "md5": "Bv2Nhj6JN9YpC7CUlxmn4Q=="
        }
    ],
    "Version": "0.6.0-7121399",
    "debs": [
        {
            "size": 11012684,
            "Priority": "optional",
            "sha1": "ABV/39zAEPb1wHyZ5ZtykbWyFrc=",
            "Maintainer": "Anton Suvorov <warwish@yandex-team.ru>",
            "Description": "Yandex.Search host manager",
            "Package": "cacus",
            "Homepage": "http://www.yandex.ru",
            "Section": "misc",
            "storage_key": "/storage/1743814/cacus/cacus_0.6.0-7121399_amd64.deb",
            "Installed-Size": "24170",
            "Version": "0.6.0-7121399",
            "Architecture": "amd64",
            "sha256": "ppZNyEeJMVQ/3bW0r0nUJD2SBT4oxtFzuEeahOg0VTk=",
            "sha512": "3n1ixwpsp2S/OvoTyR4nAySc48/miFe3p8JpqqkP6ZHdTT6Y0o47zaiKZAF1UX0ug4DUVu4cv7JujfT2t9NzqA==",
            "md5": "wJ1BV05hhAvGijkcYHTE0A=="
        }
    ],
    "dsc": {
        "Binary": "cacus",
        "Maintainer": "Anton Suvorov <warwish@yandex-team.ru>",
        "Format": "3.0 (native)",
        "Build-Depends": "debhelper (>= 9), spotify-dh-virtualenv | dh-virtualenv, python-setuptools, python-dev, build-essential",
        "Source": "cacus",
        "Version": "0.5.11.7",
        "Standards-Version": "3.9.5",
        "Architecture": "all",
        "Package-List": "\n cacus deb net optional arch=all",
        "Vcs-Git": "https://github.yandex-team.ru/InfraComponents/cacus.git",
        "Vcs-Browser": "https://github.yandex-team.ru/InfraComponents/cacus",
        "Homepage": "https://wiki.yandex-team.ru/dist/"
    }
}
PACKAGE2 = {
    "_id": "5f0f410f4f1944018146a7bc",
    "Source": "cacus",
    "audit_meta": [
        {
            "timestamp": "2020-07-15T17:46:54.662Z",
            "event": "upload",
            "user": "robot-admins"
        }
    ],
    "environment": "unstable",
    "sources": [
        {
            "size": 1256,
            "sha1": "Man2pyjNBYcC7WBCeLwdiIBntuE=",
            "name": "cacus_0.6.0-7113449_amd64.changes",
            "storage_key": "/storage/996597/cacus/cacus_0.6.0-7113449_amd64.changes",
            "sha256": "m65V31DqkZAxz8tr4MWm3X31k+vLhf8dDSN8cwGatVo=",
            "sha512": "X7MzItJog/A5age8fMsfDw9FAXKN6Xp9wX4fJbxcMRuRjP2inv/En7tvriOWpVfVuVo3lBGMHhJH2cFy5KAUXg==",
            "md5": "Q7fBZwGgk5m9NUTCz4bHnQ=="
        }
    ],
    "Version": "0.6.0-7113449",
    "debs": [
        {
            "size": 11010464,
            "Priority": "optional",
            "sha1": "goCNId6wbyHgCC8DyW/GVjx/T44=",
            "Maintainer": "Anton Suvorov <warwish@yandex-team.ru>",
            "Description": "Yandex.Search host manager",
            "Package": "cacus",
            "Homepage": "http://www.yandex.ru",
            "Section": "misc",
            "storage_key": "/storage/119702/cacus/cacus_0.6.0-7113449_amd64.deb",
            "Installed-Size": "24162",
            "Version": "0.6.0-7113449",
            "Architecture": "amd64",
            "sha256": "B2Z9C/QIOEWMvcx3pD5PLMKPoL2iWhNvA8GsFBrBVuc=",
            "sha512": "EfDBrJpBoHiI2VSiQbMcmA6lNUn6lskGupSyH5L8doPes4hUcnSGejf7laPrxw56cxACOKPp5fQ7Szo4eOaqXQ==",
            "md5": "pj/KwgfiGl9Ni0jcMmnPRQ=="
        }
    ]
}


@pytest.fixture
def package_mock():
    return package.Package(
        'mock',
        'mock',
        'mock',
        'mock',
        [{'field': 'mock'}],
        [{'field': 'mock'}],
        {'field': 'mock'},
        [{'event': 'mock'}],
        datetime.datetime(2000, 1, 1, 0, 0, 0)
    )


@pytest.fixture
def mock_db():
    mongo_connection.repos()['repo1'].insert(PACKAGE)
    mongo_connection.repos()['repo1'].insert(PACKAGE2)


def test_from_dict():
    p = package.Package.from_dict('cacus', PACKAGE)
    assert p.source == PACKAGE['Source']
    assert p.version == PACKAGE['Version']
    assert p.env == PACKAGE['environment']
    assert p.sources == PACKAGE['sources']
    assert p.debs == PACKAGE['debs']
    assert p.audit_meta == PACKAGE['audit_meta']
    assert p.recycle_after is None


def test_mongo_update_only_set(package_mock):
    d = package_mock._mongo_update()
    assert len(d) == 1
    assert d['$set']['Source'] == 'mock'
    assert d['$set']['Version'] == 'mock'
    assert d['$set']['environment'] == 'mock'
    assert d['$set']['recycle_after'] == datetime.datetime(2000, 1, 1, 0, 0, 0)
    assert d['$set']['audit_meta'] == [{'event': 'mock'}]
    assert d['$set']['dsc'] == {'field': 'mock'}
    assert d['$set']['sources'] == [{'field': 'mock'}]
    assert d['$set']['debs'] == [{'field': 'mock'}]


def test_mongo_update_append(package_mock):
    package_mock.append_audit({'event': 'mock'})
    package_mock.append_debs({'field': 'mock'})
    package_mock.append_sources({'field': 'mock'})
    d = package_mock._mongo_update()
    assert d['$set']['Source'] == 'mock'
    assert d['$set']['Version'] == 'mock'
    assert d['$set']['environment'] == 'mock'
    assert d['$set']['recycle_after'] == datetime.datetime(2000, 1, 1, 0, 0, 0)
    assert d['$set']['dsc'] == {'field': 'mock'}
    assert d['$push']['audit_meta'] == {'$each': [{'event': 'mock'}]}
    assert d['$push']['sources'] == {'$each': [{'field': 'mock'}]}
    assert d['$push']['debs'] == {'$each': [{'field': 'mock'}]}


def test_mongo_update_extend(package_mock):
    package_mock.extend_audit([{'event': 'mock'}])
    package_mock.extend_debs([{'field': 'mock'}])
    package_mock.extend_sources([{'field': 'mock'}])
    d = package_mock._mongo_update()
    assert d['$set']['Source'] == 'mock'
    assert d['$set']['Version'] == 'mock'
    assert d['$set']['environment'] == 'mock'
    assert d['$set']['recycle_after'] == datetime.datetime(2000, 1, 1, 0, 0, 0)
    assert d['$set']['dsc'] == {'field': 'mock'}
    assert d['$push']['audit_meta'] == {'$each': [{'event': 'mock'}]}
    assert d['$push']['sources'] == {'$each': [{'field': 'mock'}]}
    assert d['$push']['debs'] == {'$each': [{'field': 'mock'}]}


def test_transaction_conflict_audit_meta(package_mock):
    package_mock.audit_meta = []
    with pytest.raises(errors.TransactionConflict):
        package_mock.append_audit({'event': 'mock'})
    with pytest.raises(errors.TransactionConflict):
        package_mock.extend_audit([{'event': 'mock'}])


def test_transaction_conflict_debs(package_mock):
    package_mock.debs = []
    with pytest.raises(errors.TransactionConflict):
        package_mock.append_debs({'field': 'mock'})
    with pytest.raises(errors.TransactionConflict):
        package_mock.extend_debs([{'field': 'mock'}])


def test_transaction_conflict_sources(package_mock):
    package_mock.sources = []
    with pytest.raises(errors.TransactionConflict):
        package_mock.append_sources({'field': 'mock'})
    with pytest.raises(errors.TransactionConflict):
        package_mock.extend_sources([{'field': 'mock'}])


def test_drop_none():
    d = {
        'a': 'b',
        'c': {
            'd': 'e',
            'f': None,
        },
        'g': None,
    }
    expected = {
        'a': 'b',
        'c': {
            'd': 'e',
        },
    }
    package.Package._drop_empty(d)
    assert d == expected


def test_find_query():
    query = {'environment': 'mock'}
    assert query == package.Package._find_query(env='mock')
    query['Source'] = 'mock'
    assert query == package.Package._find_query(env='mock', source='mock')
    query['Version'] = 'mock'
    assert query == package.Package._find_query(env='mock', source='mock', version='mock')
    query['recycle_after'] = 'mock'
    assert query == package.Package._find_query(env='mock', source='mock', version='mock', recycle_after='mock')
    query['debs.Architecture'] = 'mock'
    assert query == package.Package._find_query(env='mock', source='mock', version='mock', recycle_after='mock',
                                                arch='mock')
    query['dsc'] = 'mock'
    assert query == package.Package._find_query(env='mock', source='mock', version='mock', recycle_after='mock',
                                                arch='mock', dsc='mock')

    pq = copy.deepcopy(query)
    pq.pop('Source')
    pq['debs'] = {
        '$elemMatch': {
            'Package': 'mock'
        }
    }
    query = {
        '$or': [
            query,
            pq
        ]
    }
    assert query == package.Package._find_query(env='mock', source='mock', version='mock', recycle_after='mock',
                                                arch='mock', dsc='mock', package='mock')


@pytest.mark.usefixtures("mock_db")
def test_find_one():
    p = package.Package.from_dict('repo1', PACKAGE)
    p_db = package.Package.find_one('repo1', env='unstable', source='cacus', version='0.6.0-7121399')
    assert p._mongo_update() == p_db._mongo_update()


@pytest.mark.usefixtures("mock_db")
def test_find():
    pkgs = package.Package.find('repo1')
    assert len(pkgs) == 2


@pytest.mark.usefixtures("mock_db")
def test_delete_many():
    package.Package.delete_many('repo1', version='0.6.0-7121399')
    assert len(package.Package.find('repo1')) == 1


@pytest.mark.usefixtures("mock_db")
def test_delete():
    p = package.Package.find_one('repo1', version='0.6.0-7121399')
    p.delete()
    assert len(package.Package.find('repo1')) == 1


@pytest.mark.usefixtures("mock_db")
def test_save():
    p = package.Package.find_one('repo1', version='0.6.0-7121399')
    p.repo = 'repo2'
    p.source = 'kekus'
    p.version = 'lol'
    p.save()
    assert len(package.Package.find('repo2')) == 1
    p = package.Package.find_one('repo2')
    assert p.source == 'kekus'
    assert p.version == 'lol'


def test_from_package():
    p = package.Package.from_dict('repo1', PACKAGE)
    p2 = package.Package.from_package(p)
    assert p.source == p2.source
    assert p.version == p2.version
    assert p.repo == p2.repo
    assert p.env == p2.env
    assert p.debs == p2.debs
    assert p.debs is not p2.debs
    assert p.audit_meta == p2.audit_meta
    assert p.audit_meta is not p2.audit_meta
    assert p.sources == p2.sources
    assert p.sources is not p2.sources
    assert p.dsc == p2.dsc
    assert p.dsc is not p2.dsc


def test_from_dict_with_missing_fields():
    pkg = copy.deepcopy(PACKAGE)
    pkg.pop('sources')
    pkg.pop('debs')
    pkg.pop('dsc')
    pkg.pop('audit_meta')
    p = package.Package.from_dict('cacus', pkg)
    assert p.repo == 'cacus'
    assert p.env == pkg['environment']
    assert p.version == pkg['Version']
    assert p.source == pkg['Source']
    assert p.dsc == {}
    assert p.sources == []
    assert p.debs == []
    assert p.audit_meta == []
    assert p.recycle_after is None
