import pytest

import library.python.svn_version as svn_version

import irt.utils


@pytest.fixture
def mock_branched_component(monkeypatch):
    monkeypatch.setattr(svn_version, 'svn_branch', lambda: 'multik_web/stable-6-2')


@pytest.fixture
def mock_tagged_component(monkeypatch):
    monkeypatch.setattr(svn_version, 'svn_branch', lambda: 'multik_web/stable-3')


@pytest.mark.usefixtures("mock_branched_component")
def test_release_info_branched():
    release_info = irt.utils.ReleaseInfo()
    assert release_info.component == 'multik_web'
    assert release_info.prefix == 'stable'
    assert release_info.major == 6
    assert release_info.minor == 2


@pytest.mark.usefixtures("mock_tagged_component")
def test_release_info_tagged():
    irt.utils.ReleaseInfo.set_template(irt.utils.ReleaseInfo.TAGGED_TEMPLATE)
    release_info = irt.utils.ReleaseInfo()
    assert release_info.component == 'multik_web'
    assert release_info.prefix == 'stable'
    assert release_info.major == 3
    assert release_info.minor is None


def test_trunk():
    release_info = irt.utils.ReleaseInfo()
    assert release_info.component is None
    assert release_info.prefix == 'trunk'
    assert release_info.major == svn_version.svn_revision()
    assert release_info.minor is None
