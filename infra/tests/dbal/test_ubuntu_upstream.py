import copy
import pytest

from infra.dist.cacus.lib.dbal import mongo_connection
from infra.dist.cacus.lib.dbal import ubuntu_upstream

PACKAGES = [
    {"dist": "precise", "package": "gcc"},
    {"dist": "yakkety", "package": "gcc"},
    {"dist": "wily", "package": "gcc"},
    {"dist": "xenial", "package": "gcc"},
    {"dist": "trusty", "package": "gcc"},
    {"dist": "devel", "package": "gcc"},
    {"dist": "vivid", "package": "gcc"},
    {"dist": "zesty", "package": "gcc"},
    {"dist": "artful", "package": "gcc"},
    {"dist": "bionic", "package": "gcc"},
    {"dist": "cosmic", "package": "gcc"},
    {"dist": "disco", "package": "gcc"},
    {"dist": "eoan", "package": "gcc"},
    {"dist": "focal", "package": "gcc"},
    {"dist": "groovy", "package": "gcc"},
]


@pytest.fixture
def mock_db():
    mongo_connection.cacus()['__upstream_packages__'].insert_many(copy.deepcopy(PACKAGES))


def test_from_dict():
    d = {"dist": "focal", "package": "gcc"}
    up = ubuntu_upstream.UpstreamPackage.from_dict(d)
    assert up.dist == d['dist']
    assert up.package == d['package']


def test_to_dict():
    d = {"dist": "focal", "package": "gcc"}
    up = ubuntu_upstream.UpstreamPackage('focal', 'gcc')
    assert up.to_dict() == d


def test_find_query():
    q = {'dist': 'focal'}
    assert ubuntu_upstream.UpstreamStore._find_query(dist='focal') == q
    q['package'] = 'gcc'
    assert ubuntu_upstream.UpstreamStore._find_query(dist='focal', package='gcc') == q


@pytest.mark.usefixtures("mock_db")
def test_find_one():
    up = ubuntu_upstream.default_store.find_one(dist='squeeze', package='plasma-desktop')
    assert up is None
    up = ubuntu_upstream.default_store.find_one(dist='focal', package='gcc')
    assert up.dist == 'focal'
    assert up.package == 'gcc'
    up = ubuntu_upstream.default_store.find_one(dist='focal')
    assert up.dist == 'focal'
    assert up.package == 'gcc'


@pytest.mark.usefixtures("mock_db")
def test_find():
    up = ubuntu_upstream.default_store.find(package='gcc')
    assert len(up) == len(PACKAGES)
    up = ubuntu_upstream.default_store.find()
    assert len(up) == len(PACKAGES)


@pytest.mark.usefixtures("mock_db")
def test_list_dists():
    expected_dists = sorted(d['dist'] for d in PACKAGES)
    assert sorted(ubuntu_upstream.default_store.list_dists()) == expected_dists
