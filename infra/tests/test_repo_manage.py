import mock
import datetime
from infra.dist.cacus.lib import repo_manage
from infra.dist.cacus.lib.dbal import package_repository


def test_generate_release_file(monkeypatch):
    monkeypatch.setattr(package_repository.Config, 'find', classmethod(lambda cls, repo: package_repository.Config.default('repo')))
    now = datetime.datetime.utcnow()
    m = mock.Mock()
    release = repo_manage.generate_release_file('repo', 'env', 'arch', now, m)
    assert release is not None
    release = repo_manage.generate_release_file('repo', 'env', 'arch', now, m, {'Field': 'Value'})
    assert release is not None
    assert 'Field: Value' in release
