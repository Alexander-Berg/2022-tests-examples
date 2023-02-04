import datetime

import mock
import pytest
from infra.dist.cacus.lib.dbal import errors
from infra.dist.cacus.lib.dbal import mongo_connection
from infra.dist.cacus.lib.dbal import package_repository
from infra.dist.cacus.lib.dbal import package


class TestConfig(object):
    @pytest.fixture
    def mock_db(self):
        self.repo1 = {'repo1': {
            "skip_gpg": False,
            "repo_root": "/opt/repo/repo1",
            "incoming_dir": "/opt/repo/repo1/mini-dinstall/incoming",
            "description": "mock",
            "consider_distribution_string": True,
            "loose_upload_checks": False,
            "generate_torrent": False,
        }}
        self.repo2 = {'repo2': {
            "skip_gpg": False,
            "repo_root": "/opt/repo/repo2",
            "incoming_dir": "/opt/repo/repo2/mini-dinstall/incoming",
            "description": "mock",
            "consider_distribution_string": False,
            "loose_upload_checks": False,
            "generate_torrent": False,
        }}
        config_mock = {}
        config_mock.update(self.repo1)
        config_mock.update(self.repo2)
        mongo_connection.cacus()['__config__'].insert(config_mock)

    @pytest.mark.usefixtures("mock_db")
    def test_find(self):
        r1 = package_repository.Config.find('repo1')
        assert r1.to_dict() == self.repo1
        r2 = package_repository.Config.find('repo2')
        assert r2.to_dict() == self.repo2

    @pytest.mark.usefixtures("mock_db")
    def test_save(self):
        conf = package_repository.Config.find('repo1')
        conf.description = 'new'
        conf.save()
        self.repo1['repo1']['description'] = 'new'
        config_mock = {}
        config_mock.update(self.repo1)
        config_mock.update(self.repo2)
        updated = mongo_connection.cacus()['__config__'].find()[0]
        updated.pop('_id')
        assert updated == config_mock

    @pytest.mark.usefixtures("mock_db")
    def test_all(self):
        configs = package_repository.Config.all()
        assert configs['repo1'].to_dict() == self.repo1
        assert configs['repo2'].to_dict() == self.repo2

    def test_from_dict(self):
        d = {
            "skip_gpg": False,
            "repo_root": "/opt/repo/repo1",
            "incoming_dir": "/opt/repo/repo1/mini-dinstall/incoming",
            "description": "mock",
            "consider_distribution_string": True,
            "loose_upload_checks": False,
            "generate_torrent": False,
        }
        r = package_repository.Config.from_dict({'repo1': d})
        assert r.incoming_dir == d['incoming_dir']
        assert r.skip_gpg == d['skip_gpg']
        assert r.description == d['description']
        assert r.repo_root == d['repo_root']
        assert r.repo == 'repo1'
        assert r.loose_upload_checks == d['loose_upload_checks']
        assert r.generate_torrent == d['generate_torrent']

    @pytest.mark.usefixtures("mock_db")
    def test_list_all_repos(self):
        repos = package_repository.list_all()
        assert sorted(repos) == ['repo1', 'repo2']

    @pytest.mark.usefixtures("mock_db")
    def test_delete(self):
        conf = package_repository.Config.find('repo1')
        conf.delete()
        with pytest.raises(errors.RepositoryNotFound):
            package_repository.Config.find('repo1')


def test_add():
    update_sources = mock.Mock()
    update_packages = mock.Mock()
    package_repository.add('repo3', 'descr', '/repo/repo3/mini-dinstall/incoming', '/repo/repo3', update_sources, update_packages)
    conf = package_repository.Config.find('repo3')
    assert conf.repo == 'repo3'
    assert conf.description == 'descr'
    assert conf.incoming_dir == '/repo/repo3/mini-dinstall/incoming'
    assert conf.repo_root == '/repo/repo3'
    update_sources.assert_called()
    update_packages.assert_called()
    assert package_repository.list_all() == ['repo3']


def test_list_envs():
    p = package.Package.empty("repo1")
    p.source = 's1'
    p.env = 'e1'
    p.version = 'v1'
    p.save()
    p = package.Package.empty("repo1")
    p.source = 's2'
    p.env = 'e2'
    p.version = 'v2'
    p.save()
    assert sorted(package_repository.list_envs('repo1')) == sorted(['e1', 'e2'])


def test_drop():
    package_repository.add('repo1', 'descr', 'incoming', 'root', mock.Mock(), mock.Mock())
    package_repository.add('repo2', 'descr', 'incoming', 'root', mock.Mock(), mock.Mock())
    package_repository.drop('repo1')
    assert package_repository.list_all() == ['repo2']
    package_repository.drop('repo2')
    assert package_repository.list_all() == []


def test_find_recycle_branches():
    r_a = datetime.datetime(2000, 1, 1, 0, 0, 0)
    p = package.Package.empty('repo1')
    p.source = 's1'
    p.env = 'e12remove'
    p.version = 'v1'
    p.recycle_after = r_a
    p.save()
    p = package.Package.empty('repo1')
    p.source = 's2'
    p.env = 'e22remove'
    p.version = 'v2'
    p.recycle_after = r_a
    p.save()
    r_b = package_repository.find_recycle_branches('repo1', datetime.datetime.utcnow())
    assert sorted(r_b) == sorted(['e12remove', 'e22remove'])
